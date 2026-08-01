package minecrafttransportsimulator.systems;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Client-side holder for the short-lived bullet damage x-ray replay.
 */
public class DamageXRaySystem {
    public static final int MAX_EVENTS = 12;
    public static final int MAX_FRAGMENT_EVENTS = 48;
    public static final String XRAY_SOLID_TEXTURE = "mts:textures/rendering/light.png";
    public static final ColorRGB XRAY_MODEL_COLOR = new ColorRGB(96, 96, 96);
    public static final ColorRGB XRAY_DETAIL_COLOR = new ColorRGB(255, 255, 255);
    public static final ColorRGB XRAY_DAMAGED_COLOR = new ColorRGB(255, 210, 48);
    public static final ColorRGB XRAY_DESTROYED_COLOR = new ColorRGB(255, 64, 64);
    public static final ColorRGB XRAY_DAMAGE_FLASH_COLOR = new ColorRGB(255, 244, 190);
    public static final ColorRGB XRAY_DAMAGED_FLASH_COLOR = new ColorRGB(255, 242, 132);
    public static final ColorRGB XRAY_DESTROYED_FLASH_COLOR = new ColorRGB(255, 142, 142);
    private static final long DISPLAY_DURATION_MS = 7000;
    private static final long PLAYBACK_DURATION_MS = 3000;
    private static final long FADE_DURATION_MS = 650;
    private static final long CAMERA_TURN_DURATION_MS = 3000;
    private static final long FRAGMENT_REPLAY_DURATION_MS = 1000;
    private static final long DAMAGE_FLASH_DURATION_MS = 900;
    private static final long DAMAGE_FLASH_STEP_MS = 110;
    private static final double CAMERA_TURN_ANGLE_DEGREES = 45.0D;
    private static final double CAMERA_IMPACT_ZOOM = 1.22D;
    private static final double CAMERA_FINAL_ZOOM = 1.45D;
    private static final double CAMERA_ZOOM_APPROACH_PROGRESS = 0.30D;
    private static final long CAMERA_ZOOM_DURATION_MS = 1800;

    private static Analysis activeAnalysis;
    private static int activeModelRenderDepth;
    private static int activeProjectionDepth;
    private static float activeModelRenderAlpha = 1.0F;
    private static ColorRGB activeModelRenderColor = ColorRGB.WHITE;
    private static boolean activeModelRenderOriginalTexture;
    private static LightingMode activeModelRenderLightingMode = LightingMode.IGNORE_ALL_LIGHTING;
    private static double activeModelRenderDepthOffset;
    private static double activeProjectionCenterX;
    private static double activeProjectionCenterY;
    private static double activeProjectionBaseZ;
    private static double activeProjectionCameraDistance = 1.0D;
    private static final Point3D projectionPoint = new Point3D();
    private static final Map<RenderableData, RenderableData> projectedRenderables = new IdentityHashMap<>();

    public static void displayAnalysis(AEntityE_Interactable<?> targetEntity, UUID gunID, int bulletNumber, String bulletName, String targetName, float bulletDiameter, Point3D startPosition, Point3D endPosition, ResultType resultType, List<HitEvent> hitEvents, List<FragmentEvent> fragmentEvents) {
        if (InterfaceManager.clientInterface == null || targetEntity == null || !targetEntity.world.isClient() || hitEvents == null || hitEvents.isEmpty()) {
            return;
        }
        if (activeAnalysis != null && activeAnalysis.matches(gunID, bulletNumber, targetEntity.uniqueUUID) && !activeAnalysis.isExpired()) {
            return;
        }
        activeAnalysis = new Analysis(targetEntity, gunID, bulletNumber, bulletName, targetName, bulletDiameter, startPosition, endPosition, resultType, hitEvents, fragmentEvents);
    }

    public static Analysis getActiveAnalysis() {
        if (activeAnalysis != null && activeAnalysis.isExpired()) {
            activeAnalysis = null;
        }
        return activeAnalysis;
    }

    public static void beginModelRender(ColorRGB color, float alpha) {
        beginModelRender(color, alpha, false, LightingMode.IGNORE_ALL_LIGHTING, 0.0D);
    }

    public static void beginModelRender(ColorRGB color, float alpha, boolean useOriginalTexture) {
        beginModelRender(color, alpha, useOriginalTexture, LightingMode.IGNORE_ALL_LIGHTING, 0.0D);
    }

    public static void beginModelRender(ColorRGB color, float alpha, boolean useOriginalTexture, LightingMode lightingMode, double depthOffset) {
        ++activeModelRenderDepth;
        activeModelRenderColor = color;
        activeModelRenderAlpha = alpha;
        activeModelRenderOriginalTexture = useOriginalTexture;
        activeModelRenderLightingMode = lightingMode;
        activeModelRenderDepthOffset = depthOffset;
    }

    public static void beginPerspectiveProjection(double centerX, double centerY, double baseZ, double cameraDistance) {
        ++activeProjectionDepth;
        activeProjectionCenterX = centerX;
        activeProjectionCenterY = centerY;
        activeProjectionBaseZ = baseZ;
        activeProjectionCameraDistance = Math.max(cameraDistance, 1.0D);
    }

    public static void endModelRender() {
        if (activeModelRenderDepth > 0) {
            --activeModelRenderDepth;
        }
        if (activeModelRenderDepth == 0) {
            activeModelRenderColor = ColorRGB.WHITE;
            activeModelRenderAlpha = 1.0F;
            activeModelRenderOriginalTexture = false;
            activeModelRenderLightingMode = LightingMode.IGNORE_ALL_LIGHTING;
            activeModelRenderDepthOffset = 0.0D;
        }
    }

    public static void endPerspectiveProjection() {
        if (activeProjectionDepth > 0) {
            --activeProjectionDepth;
        }
    }

    public static boolean isRenderingXRayModel() {
        return activeModelRenderDepth > 0;
    }

    public static float getActiveModelRenderAlpha() {
        return activeModelRenderAlpha;
    }

    public static ColorRGB getActiveModelRenderColor() {
        return activeModelRenderColor;
    }

    public static boolean isActiveModelRenderUsingOriginalTexture() {
        return activeModelRenderOriginalTexture;
    }

    public static LightingMode getActiveModelRenderLightingMode() {
        return activeModelRenderLightingMode;
    }

    public static boolean isPerspectiveProjectionActive() {
        return activeProjectionDepth > 0;
    }

    public static void projectPoint(Point3D point) {
        double depth = point.z - activeProjectionBaseZ;
        double perspective = activeProjectionCameraDistance / Math.max(activeProjectionCameraDistance - depth, activeProjectionCameraDistance * 0.30D);
        point.x = activeProjectionCenterX + (point.x - activeProjectionCenterX) * perspective;
        point.y = activeProjectionCenterY + (point.y - activeProjectionCenterY) * perspective;
    }

    public static void renderProjected(RenderableData source) {
        RenderableData projected = projectedRenderables.get(source);
        int vertexCapacity = source.vertexObject.vertices.capacity();
        if (projected == null || projected.vertexObject.vertices.capacity() != vertexCapacity) {
            projected = new RenderableData(new RenderableVertices("XRAY_PERSPECTIVE", FloatBuffer.allocate(vertexCapacity), false, source.vertexObject.isErrorPlaceholder), source.texture);
            projectedRenderables.put(source, projected);
        }

        FloatBuffer sourceVertices = source.vertexObject.vertices;
        FloatBuffer projectedVertices = projected.vertexObject.vertices;
        double normalM00 = source.transform.m11 * source.transform.m22 - source.transform.m12 * source.transform.m21;
        double normalM01 = source.transform.m12 * source.transform.m20 - source.transform.m10 * source.transform.m22;
        double normalM02 = source.transform.m10 * source.transform.m21 - source.transform.m11 * source.transform.m20;
        double normalM10 = source.transform.m02 * source.transform.m21 - source.transform.m01 * source.transform.m22;
        double normalM11 = source.transform.m00 * source.transform.m22 - source.transform.m02 * source.transform.m20;
        double normalM12 = source.transform.m01 * source.transform.m20 - source.transform.m00 * source.transform.m21;
        double normalM20 = source.transform.m01 * source.transform.m12 - source.transform.m02 * source.transform.m11;
        double normalM21 = source.transform.m02 * source.transform.m10 - source.transform.m00 * source.transform.m12;
        double normalM22 = source.transform.m00 * source.transform.m11 - source.transform.m01 * source.transform.m10;
        double normalDeterminant = source.transform.m00 * normalM00 + source.transform.m01 * normalM01 + source.transform.m02 * normalM02;
        boolean transformNormals = !source.lightingMode.disableTextureShadows && Math.abs(normalDeterminant) > 1.0E-8D;
        if (transformNormals) {
            double inverseDeterminant = 1.0D / normalDeterminant;
            normalM00 *= inverseDeterminant;
            normalM01 *= inverseDeterminant;
            normalM02 *= inverseDeterminant;
            normalM10 *= inverseDeterminant;
            normalM11 *= inverseDeterminant;
            normalM12 *= inverseDeterminant;
            normalM20 *= inverseDeterminant;
            normalM21 *= inverseDeterminant;
            normalM22 *= inverseDeterminant;
        }
        projectedVertices.clear();
        while (sourceVertices.hasRemaining()) {
            float normalX = sourceVertices.get();
            float normalY = sourceVertices.get();
            float normalZ = sourceVertices.get();
            float textureU = sourceVertices.get();
            float textureV = sourceVertices.get();
            projectionPoint.set(sourceVertices.get(), sourceVertices.get(), sourceVertices.get()).transform(source.transform);
            projectPoint(projectionPoint);
            projectionPoint.z += activeModelRenderDepthOffset;

            if (transformNormals) {
                double transformedNormalX = normalM00 * normalX + normalM01 * normalY + normalM02 * normalZ;
                double transformedNormalY = normalM10 * normalX + normalM11 * normalY + normalM12 * normalZ;
                double transformedNormalZ = normalM20 * normalX + normalM21 * normalY + normalM22 * normalZ;
                double normalLength = Math.sqrt(transformedNormalX * transformedNormalX + transformedNormalY * transformedNormalY + transformedNormalZ * transformedNormalZ);
                if (normalLength > 1.0E-8D) {
                    normalX = (float) (transformedNormalX / normalLength);
                    normalY = (float) (transformedNormalY / normalLength);
                    normalZ = (float) (transformedNormalZ / normalLength);
                }
            }

            projectedVertices.put(normalX);
            projectedVertices.put(normalY);
            projectedVertices.put(normalZ);
            projectedVertices.put(textureU);
            projectedVertices.put(textureV);
            projectedVertices.put((float) projectionPoint.x);
            projectedVertices.put((float) projectionPoint.y);
            projectedVertices.put((float) projectionPoint.z);
        }
        sourceVertices.rewind();
        projectedVertices.flip();

        projected.transform.resetTransforms();
        projected.setTexture(source.texture);
        projected.setAlpha(source.alpha);
        projected.setColor(source.color);
        projected.setLightValue(source.worldLightValue);
        projected.setLightMode(source.lightingMode);
        projected.setBlending(source.enableBrightBlending);
        InterfaceManager.renderingInterface.renderVertices(projected, true);
    }

    public static enum ResultType {
        PENETRATED("Penetrated"),
        ARMOR_STOP("Stopped by armor"),
        VEHICLE_STOP("Damaged component");

        public final String title;

        private ResultType(String title) {
            this.title = title;
        }
    }

    public static class HitEvent {
        public final Point3D hitPosition;
        public final UUID componentID;
        public final int groupIndex;
        public final int boxIndex;
        public final String componentName;
        public final double armorThickness;
        public final double penetrationPotential;
        public final double armorPenetrated;
        public final double collisionDamage;
        public final double entityDamage;
        public final boolean stopped;
        public final boolean forwardedDamage;
        public double pathProgress;

        public HitEvent(Point3D hitPosition, UUID componentID, int groupIndex, int boxIndex, String componentName, double armorThickness, double penetrationPotential, double armorPenetrated, double collisionDamage, double entityDamage, boolean stopped, boolean forwardedDamage) {
            this.hitPosition = hitPosition.copy();
            this.componentID = componentID;
            this.groupIndex = groupIndex;
            this.boxIndex = boxIndex;
            this.componentName = componentName;
            this.armorThickness = armorThickness;
            this.penetrationPotential = penetrationPotential;
            this.armorPenetrated = armorPenetrated;
            this.collisionDamage = collisionDamage;
            this.entityDamage = entityDamage;
            this.stopped = stopped;
            this.forwardedDamage = forwardedDamage;
        }

        public boolean hasArmor() {
            return armorThickness > 0;
        }

        public boolean hasDamage() {
            return collisionDamage > 0 || entityDamage > 0;
        }

        public String getShortLabel() {
            String boxText = groupIndex > 0 ? "G" + groupIndex + "/B" + boxIndex : "Box";
            if (hasArmor()) {
                return boxText + " armor " + (int) armorThickness + "mm" + (stopped ? " stop" : " pass");
            } else if (hasDamage()) {
                return boxText + " dmg " + String.format("%.1f", Math.max(collisionDamage, entityDamage));
            } else {
                return boxText + " pass";
            }
        }
    }

    public static class FragmentEvent {
        public final Point3D startPosition;
        public final Point3D endPosition;
        public final UUID componentID;
        public double pathProgress;

        public FragmentEvent(Point3D startPosition, Point3D endPosition) {
            this(startPosition, endPosition, null);
        }

        public FragmentEvent(Point3D startPosition, Point3D endPosition, UUID componentID) {
            this.startPosition = startPosition.copy();
            this.endPosition = endPosition.copy();
            this.componentID = componentID;
        }
    }

    public static class Analysis {
        public final AEntityE_Interactable<?> targetEntity;
        public final UUID targetID;
        public final UUID gunID;
        public final int bulletNumber;
        public final String bulletName;
        public final String targetName;
        public final float bulletDiameter;
        public final Point3D startPosition;
        public final Point3D endPosition;
        public final Point3D impactPosition;
        public final ResultType resultType;
        public final List<HitEvent> hitEvents;
        public final List<FragmentEvent> fragmentEvents;
        public final double impactProgress;
        private final double cameraTurnSide;
        private final long createdTime;

        private Analysis(AEntityE_Interactable<?> targetEntity, UUID gunID, int bulletNumber, String bulletName, String targetName, float bulletDiameter, Point3D startPosition, Point3D endPosition, ResultType resultType, List<HitEvent> hitEvents, List<FragmentEvent> fragmentEvents) {
            this.targetEntity = targetEntity;
            this.targetID = targetEntity.uniqueUUID;
            this.gunID = gunID;
            this.bulletNumber = bulletNumber;
            this.bulletName = bulletName;
            this.targetName = targetName;
            this.bulletDiameter = bulletDiameter;
            this.startPosition = startPosition.copy();
            this.endPosition = endPosition.copy();
            this.resultType = resultType;
            this.hitEvents = new ArrayList<>();
            this.fragmentEvents = new ArrayList<>();
            double totalDistance = Math.max(this.startPosition.distanceTo(this.endPosition), 0.001D);
            for (int i = 0; i < hitEvents.size() && i < MAX_EVENTS; ++i) {
                HitEvent event = hitEvents.get(i);
                event.pathProgress = Math.max(0, Math.min(1, this.startPosition.distanceTo(event.hitPosition) / totalDistance));
                this.hitEvents.add(event);
            }
            if (fragmentEvents != null) {
                for (int i = 0; i < fragmentEvents.size() && i < MAX_FRAGMENT_EVENTS; ++i) {
                    FragmentEvent event = fragmentEvents.get(i);
                    event.pathProgress = Math.max(0, Math.min(1, this.startPosition.distanceTo(event.startPosition) / totalDistance));
                    this.fragmentEvents.add(event);
                }
            }
            HitEvent focusEvent = this.hitEvents.isEmpty() ? null : this.hitEvents.get(0);
            for (HitEvent event : this.hitEvents) {
                if (event.hasDamage()) {
                    focusEvent = event;
                    break;
                }
            }
            this.impactProgress = focusEvent != null ? focusEvent.pathProgress : 1.0D;
            this.impactPosition = focusEvent != null ? focusEvent.hitPosition.copy() : this.endPosition.copy();
            Point3D localHitOffset = this.impactPosition.copy().subtract(targetEntity.position).reOrigin(targetEntity.orientation);
            this.cameraTurnSide = localHitOffset.x >= 0 ? -1.0D : 1.0D;
            this.createdTime = System.currentTimeMillis();
        }

        private boolean matches(UUID gunID, int bulletNumber, UUID targetID) {
            return this.bulletNumber == bulletNumber && this.gunID.equals(gunID) && this.targetID.equals(targetID);
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - createdTime > DISPLAY_DURATION_MS;
        }

        public float getPlaybackProgress() {
            return Math.min(1.0F, (System.currentTimeMillis() - createdTime) / (float) PLAYBACK_DURATION_MS);
        }

        public float getFocusProgress() {
            return Math.min(getPlaybackProgress(), (float) impactProgress);
        }

        public double getCameraTurnAngle() {
            long impactAge = System.currentTimeMillis() - (createdTime + (long) (PLAYBACK_DURATION_MS * impactProgress));
            if (impactAge <= 0) {
                return 0.0D;
            }
            double turnProgress = Math.min(1.0D, impactAge / (double) CAMERA_TURN_DURATION_MS);
            double easedProgress = turnProgress * turnProgress * (3.0D - 2.0D * turnProgress);
            return cameraTurnSide * CAMERA_TURN_ANGLE_DEGREES * easedProgress;
        }

        public double getCameraZoom() {
            double approachStart = Math.max(0.0D, impactProgress - CAMERA_ZOOM_APPROACH_PROGRESS);
            double approachProgress = impactProgress <= 0.001D ? 1.0D : (getPlaybackProgress() - approachStart) / Math.max(impactProgress - approachStart, 0.001D);
            double zoom = 1.0D + (CAMERA_IMPACT_ZOOM - 1.0D) * smoothStep(approachProgress);
            long impactAge = System.currentTimeMillis() - (createdTime + (long) (PLAYBACK_DURATION_MS * impactProgress));
            if (impactAge > 0) {
                zoom += (CAMERA_FINAL_ZOOM - CAMERA_IMPACT_ZOOM) * smoothStep(impactAge / (double) CAMERA_ZOOM_DURATION_MS);
            }
            return zoom;
        }

        public boolean isEntityDamageFlashActive(UUID componentID) {
            long currentTime = System.currentTimeMillis();
            for (HitEvent event : hitEvents) {
                if (event.entityDamage > 0.0D && event.componentID.equals(componentID) && isDamageFlashVisible(currentTime, createdTime + (long) (PLAYBACK_DURATION_MS * event.pathProgress))) {
                    return true;
                }
            }
            for (FragmentEvent event : fragmentEvents) {
                if (componentID.equals(event.componentID) && isDamageFlashVisible(currentTime, createdTime + (long) (PLAYBACK_DURATION_MS * event.pathProgress) + FRAGMENT_REPLAY_DURATION_MS)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isCollisionDamageFlashActive(UUID componentID, int groupIndex) {
            long currentTime = System.currentTimeMillis();
            for (HitEvent event : hitEvents) {
                if (event.collisionDamage > 0.0D && event.groupIndex == groupIndex && event.componentID.equals(componentID) && isDamageFlashVisible(currentTime, createdTime + (long) (PLAYBACK_DURATION_MS * event.pathProgress))) {
                    return true;
                }
            }
            return false;
        }

        public float getHitMarkerProgress(HitEvent event) {
            long markerAge = System.currentTimeMillis() - (createdTime + (long) (PLAYBACK_DURATION_MS * event.pathProgress));
            return markerAge >= 0 ? Math.min(1.0F, markerAge / 650.0F) : -1.0F;
        }

        public float getFragmentProgress(FragmentEvent event) {
            long fragmentAge = System.currentTimeMillis() - (createdTime + (long) (PLAYBACK_DURATION_MS * event.pathProgress));
            return fragmentAge >= 0 ? Math.min(1.0F, fragmentAge / (float) FRAGMENT_REPLAY_DURATION_MS) : -1.0F;
        }

        public float getAlpha() {
            long age = System.currentTimeMillis() - createdTime;
            if (age < DISPLAY_DURATION_MS - FADE_DURATION_MS) {
                return 1.0F;
            }
            return Math.max(0.0F, (DISPLAY_DURATION_MS - age) / (float) FADE_DURATION_MS);
        }

        private static boolean isDamageFlashVisible(long currentTime, long damageTime) {
            long damageAge = currentTime - damageTime;
            return damageAge >= 0 && damageAge < DAMAGE_FLASH_DURATION_MS && damageAge / DAMAGE_FLASH_STEP_MS % 2 == 0;
        }

        private static double smoothStep(double value) {
            value = Math.max(0.0D, Math.min(1.0D, value));
            return value * value * (3.0D - 2.0D * value);
        }

        public double getTotalArmor() {
            double totalArmor = 0;
            for (HitEvent event : hitEvents) {
                totalArmor = Math.max(totalArmor, event.armorPenetrated);
            }
            return totalArmor;
        }

        public double getTotalDamage() {
            double totalDamage = 0;
            for (HitEvent event : hitEvents) {
                totalDamage += event.collisionDamage + event.entityDamage;
            }
            return totalDamage;
        }

        public double getPenetrationPotential() {
            double potential = 0;
            for (HitEvent event : hitEvents) {
                potential = Math.max(potential, event.penetrationPotential);
            }
            return potential;
        }
    }
}
