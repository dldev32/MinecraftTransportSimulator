package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Server-side fragment rays distributed uniformly over a cone around the impact direction.
 * Each ray damages its first target. Terrain and vehicle armor block targets behind them.
 */
public class Fragmentation {
    private final PartGun gun;
    private final JSONBullet definition;
    private final Point3D position;
    private final RotationMatrix orientation;
    private final Random random = new Random();

    public Fragmentation(PartGun gun, JSONBullet definition, Point3D position, RotationMatrix orientation) {
        this.gun = gun;
        this.definition = definition;
        this.position = position.copy();
        this.orientation = orientation;
    }

    public void spawnFragments() {
        AWrapperWorld world = gun.world;
        JSONBullet.Bullet bulletDef = definition.bullet;
        if (world.isClient() || bulletDef.types.contains(BulletType.WATER) || bulletDef.fragmentCount <= 0 || !(bulletDef.fragmentDamage > 0) || !(bulletDef.fragmentDistance > 0) || !Float.isFinite(bulletDef.fragmentDamage) || !Float.isFinite(bulletDef.fragmentDistance) || !Float.isFinite(bulletDef.fragmentMaxAngle)) {
            return;
        }

        BoundingBox bounds = new BoundingBox(position, bulletDef.fragmentDistance);
        List<AEntityF_Multipart<?>> multiparts = new ArrayList<>();
        world.populateWithEntitiesInBounds(multiparts, bounds);
        //Unlike direct gunfire, fragments can also hit riders on the vehicle that fired them.
        List<IWrapperEntity> entities = world.getEntitiesWithin(bounds);
        double amount = bulletDef.fragmentDamage * ConfigSystem.settings.damage.bulletDamageFactor.value * ConfigSystem.settings.damage.packBulletDamageFactors.value.getOrDefault(definition.packID, 1D);
        LanguageEntry language = gun.lastController != null ? LanguageSystem.DEATH_BULLET_PLAYER : LanguageSystem.DEATH_BULLET_NULL;

        for (int i = 0; i < bulletDef.fragmentCount; ++i) {
            Point3D ray = getRayDirection(orientation, bulletDef.fragmentMaxAngle, random).scale(bulletDef.fragmentDistance);
            Point3D endPoint = position.copy().add(ray);
            BlockHitResult blockHit = world.getBlockHit(position, ray);
            double closestDistance = blockHit != null ? position.distanceTo(blockHit.hitPosition) : bulletDef.fragmentDistance;
            BoundingBoxHitResult closestHit = null;
            AEntityF_Multipart<?> hitMultipart = null;
            IWrapperEntity hitEntity = null;

            for (AEntityF_Multipart<?> multipart : multiparts) {
                if (!multipart.isValid) {
                    continue;
                }
                for (BoundingBox box : multipart.allCollisionBoxes) {
                    if (box.collisionTypes == null || !(box.collisionTypes.contains(CollisionType.ATTACK) || box.collisionTypes.contains(CollisionType.BULLET))) {
                        continue;
                    }
                    APart part = multipart.getPartWithBox(box);
                    if (part != null && !part.isValid) {
                        continue;
                    }
                    BoundingBoxHitResult hit = box.getIntersection(position, endPoint);
                    if (hit != null) {
                        double distance = position.distanceTo(hit.position);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestHit = hit;
                            hitMultipart = multipart;
                        }
                    }
                }
            }
            for (IWrapperEntity entity : entities) {
                if (!entity.isValid()) {
                    continue;
                }
                BoundingBoxHitResult hit = entity.getBounds().getIntersection(position, endPoint);
                if (hit != null) {
                    double distance = position.distanceTo(hit.position);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestHit = hit;
                        hitEntity = entity;
                    }
                }
            }

            if (hitEntity != null) {
                hitEntity.attack(new Damage(amount, closestHit.box, gun, gun.lastController, language).ignoreCooldown());
            } else if (hitMultipart != null) {
                damageMultipart(hitMultipart, closestHit.box, amount, language);
            }
        }
    }

    /**
     * Sampling cos(theta), rather than theta, gives equal probability per solid angle.
     */
    static Point3D getRayDirection(RotationMatrix orientation, double maxAngle, Random random) {
        double cosTheta = 1D - random.nextDouble() * (1D - Math.cos(Math.toRadians(Math.max(0D, Math.min(180D, maxAngle)))));
        double sinTheta = Math.sqrt(Math.max(0D, 1D - cosTheta * cosTheta));
        double phi = random.nextDouble() * Math.PI * 2D;
        return new Point3D(Math.cos(phi) * sinTheta, Math.sin(phi) * sinTheta, cosTheta).rotate(orientation);
    }

    private void damageMultipart(AEntityF_Multipart<?> multipart, BoundingBox box, double amount, LanguageEntry language) {
        JSONCollisionGroup group = box.groupDef;
        if (group != null && group.damageMultiplier != 0) {
            amount *= group.damageMultiplier;
        }
        boolean healthBox = group != null && group.health > 0;
        boolean armorBox = group != null && (group.armorThickness > 0 || group.heatArmorThickness > 0);
        if (healthBox) {
            multipart.damageCollisionBox(box, amount);
        }
        if (group != null && group.forwardsDamageMultiplier > 0) {
            amount *= group.forwardsDamageMultiplier;
        } else if (healthBox || armorBox) {
            return;
        }
        //APart.attack applies the part's forwarding multiplier when passing damage to the vehicle.
        multipart.attack(new Damage(amount, box, gun, gun.lastController, language).ignoreCooldown());
    }
}
