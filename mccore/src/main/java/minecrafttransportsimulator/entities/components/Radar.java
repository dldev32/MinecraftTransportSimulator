package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.AJSONItem.General;
import minecrafttransportsimulator.jsondefs.JSONPart.TargetType;

/**
 * Radar returns for vehicles and approaching guided missiles.
 */
public final class Radar {
    private static final int FIXED_SCAN_TICKS = 5;

    private final AEntityD_Definable<?> receiver;
    private final Map<AEntityD_Definable<?>, Track> tracks = new HashMap<>();
    private final List<Contact> aircraft = new ArrayList<>();
    private final List<Contact> ground = new ArrayList<>();
    private final Point3D antennaPosition = new Point3D();
    private final Point3D targetOffset = new Point3D();
    private final Point3D targetAngles = new Point3D();
    private final Comparator<Contact> byDistance = Comparator.comparingDouble((Contact contact) -> contact.distance)
            .thenComparing(Radar::compareTargets);

    Radar(AEntityD_Definable<?> receiver) {
        this.receiver = receiver;
    }

    void update() {
        if (!isOperational()) {
            clear();
            return;
        }

        General settings = receiver.definition.general;
        double rotationSpeed = settings.radarRotationSpeed;
        double legacyWidth = settings.radarWidth > 0 ? Math.min(360, 2 * settings.radarWidth) : 360;
        double horizontalAngle = settings.radarHorizontalAngle != null ? Math.max(0, Math.min(360, settings.radarHorizontalAngle)) : legacyWidth;
        double verticalAngle = settings.radarVerticalAngle != null ? Math.max(0, Math.min(360, settings.radarVerticalAngle)) : legacyWidth;
        boolean legacyCone = settings.radarHorizontalAngle == null && settings.radarVerticalAngle == null && rotationSpeed == 0;
        boolean sweeping = rotationSpeed != 0 && horizontalAngle < 360;
        long tick = receiver.world.getTickCount();
        double revisitTicks = sweeping ? Math.ceil(360 / Math.abs(rotationSpeed)) : FIXED_SCAN_TICKS;

        if (sweeping || receiver.ticksExisted == 1 || tick % FIXED_SCAN_TICKS == 0) {
            //Cover the whole arc swept since the last tick, including a beam narrower than one rotation step.
            double sweptWidth = Math.min(360, horizontalAngle + Math.abs(rotationSpeed));
            double sweptCenter = rotation(receiver, 0) - Math.copySign(Math.min(360, Math.abs(rotationSpeed)) / 2, rotationSpeed);
            double legacyCosine = Math.cos(Math.toRadians(settings.radarWidth));
            antennaPosition.set(receiver.position);
            if (settings.radarPosition != null) {
                antennaPosition.set(settings.radarPosition).multiply(receiver.scale).rotate(receiver.orientation).add(receiver.position);
            }
            List<AEntityD_Definable<?>> candidates = new ArrayList<>(receiver.world.getEntitiesOfType(EntityVehicleF_Physics.class));
            for (EntityBullet bullet : receiver.world.getEntitiesOfType(EntityBullet.class)) {
                if (bullet.definition.bullet.turnRate > 0) {
                    candidates.add(bullet);
                }
            }
            for (AEntityD_Definable<?> target : candidates) {
                if (!canTrack(target)) {
                    continue;
                }
                Track track = tracks.get(target);
                if (track != null) {
                    track.receive(tick);
                    //Keep at most one return in flight per target.
                    if (track.pending != null) {
                        continue;
                    }
                }
                targetOffset.set(target.position).subtract(antennaPosition);
                double distance = targetOffset.length();
                if (!Double.isFinite(distance) || distance > settings.radarRange) {
                    continue;
                }
                targetAngles.set(targetOffset).reOrigin(receiver.orientation);
                if (legacyCone) {
                    if (settings.radarWidth <= 0 || (settings.radarWidth <= 180 && targetAngles.z <= distance * legacyCosine)) {
                        continue;
                    }
                } else {
                    targetAngles.getAngles(true);
                    if (Math.abs(targetAngles.x) > verticalAngle / 2 || Math.abs(targetAngles.getClampedYDelta(sweptCenter)) > sweptWidth / 2) {
                        continue;
                    }
                }
                if (settings.radarObstacleAttenuation > 0 && !passesTerrain(distance, settings)) {
                    continue;
                }
                double delay = settings.radarSignalSpeed > 0 ? Math.ceil(2 * distance / settings.radarSignalSpeed) : 0;
                if (Double.isFinite(delay)) {
                    tracks.computeIfAbsent(target, entity -> new Track()).pending = new Contact(this, target, distance, tick, tick + delay);
                }
            }
        }

        aircraft.clear();
        ground.clear();
        Iterator<Entry<AEntityD_Definable<?>, Track>> iterator = tracks.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<AEntityD_Definable<?>, Track> entry = iterator.next();
            AEntityD_Definable<?> target = entry.getKey();
            Track track = entry.getValue();
            track.receive(tick);
            Contact contact = track.received;
            if (!canTrack(target) || (track.pending == null && (contact == null || tick >= contact.arrivalTick + revisitTicks))) {
                if (target instanceof EntityVehicleF_Physics) {
                    ((EntityVehicleF_Physics) target).radarsTracking.remove(receiver);
                }
                iterator.remove();
            } else if (contact != null) {
                if (contact.vehicle != null && !contact.vehicle.radarsTracking.contains(receiver)) {
                    contact.vehicle.radarsTracking.add(receiver);
                }
                (contact.isMissile() || contact.vehicle.definition.motorized.isAircraft ? aircraft : ground).add(contact);
            }
        }
        aircraft.sort(byDistance);
        ground.sort(byDistance);
    }

    public Contact getContact(AEntityD_Definable<?> target) {
        Track track = tracks.get(target);
        return track != null && canTrack(target) && isOperational() ? track.received : null;
    }

    public Contact getContact(boolean isAircraft, int index) {
        List<Contact> contacts = isAircraft ? aircraft : ground;
        return index >= 0 && index < contacts.size() && canTrack(contacts.get(index).target) && isOperational() ? contacts.get(index) : null;
    }

    public Contact getClosestContact(Point3D origin, double range, TargetType type, Contact closest) {
        if (!isOperational() || type == TargetType.SOFT) {
            return closest;
        }
        double closestDistance = closest != null ? origin.distanceTo(closest.position) : range;
        for (Track track : tracks.values()) {
            Contact contact = track.received;
            if (contact == null || !canTrack(contact.target)) {
                continue;
            }
            boolean isAircraft = contact.isMissile() || contact.vehicle.definition.motorized.isAircraft;
            if ((type == TargetType.AIRCRAFT && !isAircraft) || (type == TargetType.GROUND && isAircraft) || (type == TargetType.MISSILE && !contact.isMissile())) {
                continue;
            }
            double distance = origin.distanceTo(contact.position);
            if (distance < closestDistance || (distance == closestDistance && (closest == null || compareTargets(contact, closest) < 0))) {
                closest = contact;
                closestDistance = distance;
            }
        }
        return closest;
    }

    static double rotation(AEntityD_Definable<?> receiver, float partialTicks) {
        double speed = receiver.definition.general.radarRotationSpeed;
        double angle = (receiver.world.getTickCount() * (speed % 360) + (partialTicks * speed) % 360) % 360;
        return angle < 0 ? angle + 360 : angle;
    }

    void clear() {
        tracks.keySet().forEach(target -> {
            if (target instanceof EntityVehicleF_Physics) {
                ((EntityVehicleF_Physics) target).radarsTracking.remove(receiver);
            }
        });
        tracks.clear();
        aircraft.clear();
        ground.clear();
    }

    private static int compareTargets(Contact first, Contact second) {
        EntityBullet firstMissile = first.isMissile() ? (EntityBullet) first.target : null;
        EntityBullet secondMissile = second.isMissile() ? (EntityBullet) second.target : null;
        //Bullet UUIDs are local to each world; gun UUID and shot number identify the same shot on both sides.
        int result = (firstMissile != null ? firstMissile.gun.uniqueUUID : first.target.uniqueUUID)
                .compareTo(secondMissile != null ? secondMissile.gun.uniqueUUID : second.target.uniqueUUID);
        return result != 0 ? result : Integer.compare(firstMissile != null ? firstMissile.bulletNumber : -1, secondMissile != null ? secondMissile.bulletNumber : -1);
    }

    private boolean canTrack(AEntityD_Definable<?> target) {
        AEntityD_Definable<?> platform = receiver instanceof APart ? ((APart) receiver).masterEntity : receiver;
        if (!target.isValid || target == platform) {
            return false;
        }
        if (target instanceof EntityVehicleF_Physics) {
            return !((EntityVehicleF_Physics) target).outOfHealth;
        }
        EntityBullet missile = (EntityBullet) target;
        if (!missile.isInFlight() || missile.definition.bullet.turnRate <= 0 || missile.gun.masterEntity == platform) {
            return false;
        }
        //Use relative motion, so a moving platform does not mistake a receding missile for an approach.
        double dx = missile.position.x - platform.position.x;
        double dy = missile.position.y - platform.position.y;
        double dz = missile.position.z - platform.position.z;
        double vx = missile.motion.x - (platform.position.x - platform.prevPosition.x);
        double vy = missile.motion.y - (platform.position.y - platform.prevPosition.y);
        double vz = missile.motion.z - (platform.position.z - platform.prevPosition.z);
        return dx * vx + dy * vy + dz * vz < 0;
    }

    private boolean isOperational() {
        double range = receiver.definition.general.radarRange;
        if (!receiver.isValid || !(range > 0) || !Double.isFinite(range) || (receiver instanceof AEntityE_Interactable && ((AEntityE_Interactable<?>) receiver).outOfHealth)) {
            return false;
        }
        if (receiver instanceof APart) {
            APart part = (APart) receiver;
            return !part.isSpare && part.isActiveVar.isActive && !part.masterEntity.outOfHealth;
        }
        return true;
    }

    private boolean passesTerrain(double distance, General settings) {
        double range = settings.radarRange;
        double transmission = 1 - Math.min(1, settings.radarObstacleAttenuation);
        Point3D origin = antennaPosition;
        Point3D block = new Point3D(Math.floor(origin.x), Math.floor(origin.y), Math.floor(origin.z));
        int stepX = targetOffset.x > 0 ? 1 : -1;
        int stepY = targetOffset.y > 0 ? 1 : -1;
        int stepZ = targetOffset.z > 0 ? 1 : -1;
        double intervalX = 1 / Math.abs(targetOffset.x);
        double intervalY = 1 / Math.abs(targetOffset.y);
        double intervalZ = 1 / Math.abs(targetOffset.z);
        double nextX = firstBoundary(origin.x, block.x, stepX, intervalX);
        double nextY = firstBoundary(origin.y, block.y, stepY, intervalY);
        double nextZ = firstBoundary(origin.z, block.z, stepZ, intervalZ);

        int lastChunkX = Integer.MIN_VALUE;
        int lastChunkZ = Integer.MIN_VALUE;
        //Walk the intersected blocks, without loading chunks along the ray.
        while (true) {
            double next = Math.min(nextX, Math.min(nextY, nextZ));
            if (next >= 1) {
                return true;
            }
            if (nextX == next) {
                block.x += stepX;
                nextX += intervalX;
            }
            if (nextY == next) {
                block.y += stepY;
                nextY += intervalY;
            }
            if (nextZ == next) {
                block.z += stepZ;
                nextZ += intervalZ;
            }
            int chunkX = (int) block.x >> 4;
            int chunkZ = (int) block.z >> 4;
            if (chunkX != lastChunkX || chunkZ != lastChunkZ) {
                if (!receiver.world.chunkLoaded(block)) {
                    return false;
                }
                lastChunkX = chunkX;
                lastChunkZ = chunkZ;
            }
            if (receiver.world.isBlockSolid(block, Axis.UP)) {
                range *= transmission;
                if (range < distance) {
                    return false;
                }
            }
        }
    }

    private static double firstBoundary(double origin, double block, int step, double interval) {
        return Double.isInfinite(interval) ? interval : (step > 0 ? block + 1 - origin : origin - block) * interval;
    }

    private static final class Track {
        private Contact received;
        private Contact pending;

        private void receive(long tick) {
            if (pending != null && pending.arrivalTick <= tick) {
                received = pending;
                pending = null;
            }
        }
    }

    public static final class Contact {
        public final Radar radar;
        public final AEntityD_Definable<?> target;
        public final EntityVehicleF_Physics vehicle;
        public final Point3D position;
        public final double speed;
        private final Point3D motion;
        private final double distance;
        private final long sampleTick;
        private final double arrivalTick;

        private Contact(Radar radar, AEntityD_Definable<?> target, double distance, long sampleTick, double arrivalTick) {
            this.radar = radar;
            this.target = target;
            this.vehicle = target instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) target : null;
            this.position = target.position.copy();
            this.speed = target.velocity;
            this.motion = target.motion.copy().scale(vehicle != null ? vehicle.speedFactor : 1);
            this.distance = distance;
            this.sampleTick = sampleTick;
            this.arrivalTick = arrivalTick;
        }

        public boolean isMissile() {
            return target instanceof EntityBullet;
        }

        public Point3D getLeadPoint(Point3D origin, double projectileSpeed) {
            Point3D predicted = position.copy().addScaled(motion, Math.max(0, radar.receiver.world.getTickCount() - sampleTick));
            if (!(projectileSpeed > 0)) {
                return predicted;
            }
            Point3D offset = predicted.copy().subtract(origin);
            double a = motion.dotProduct(motion, false) - projectileSpeed * projectileSpeed;
            double b = 2 * offset.dotProduct(motion, false);
            double c = offset.dotProduct(offset, false);
            double time = Double.POSITIVE_INFINITY;
            if (Math.abs(a) < 1.0E-9) {
                if (b < 0) {
                    time = -c / b;
                }
            } else {
                double discriminant = b * b - 4 * a * c;
                if (discriminant >= 0) {
                    double root = Math.sqrt(discriminant);
                    double first = (-b - root) / (2 * a);
                    double second = (-b + root) / (2 * a);
                    if (first >= 0) {
                        time = first;
                    }
                    if (second >= 0) {
                        time = Math.min(time, second);
                    }
                }
            }
            return Double.isFinite(time) ? predicted.addScaled(motion, time) : predicted;
        }
    }
}
