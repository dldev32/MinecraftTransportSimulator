package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.systems.DamageXRaySystem;
import minecrafttransportsimulator.systems.DamageXRaySystem.Analysis;

/**
 * Renders the real hit target entity and its installed parts into the x-ray hitcam.
 */
public class GUIComponentDamageXRayEntity extends AGUIComponent {
    private final TransformationMatrix baseTransform = new TransformationMatrix();
    private final TransformationMatrix entityTransform = new TransformationMatrix();
    private final RotationMatrix viewRotation = new RotationMatrix();
    private final RotationMatrix targetOrientation = new RotationMatrix();
    private final RotationMatrix entityOrientation = new RotationMatrix();
    private final RotationMatrix relativeOrientation = new RotationMatrix();
    private final Point3D targetPosition = new Point3D();
    private final Point3D focusPosition = new Point3D();
    private final Point3D entityPosition = new Point3D();
    private final Point3D relativePosition = new Point3D();
    private final Point3D healthBoxOffset = new Point3D();
    private final Point3D flightVector = new Point3D();
    private final Point3D interpolatedScale = new Point3D();
    private final BoundingBox healthBoxRenderBounds = new BoundingBox(new Point3D(), 1, 1, 1);

    public GUIComponentDamageXRayEntity(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public int getZOffset() {
        return MODEL_DEFAULT_ZOFFSET;
    }

    public void setPanelBounds(int x, int y, int width, int height) {
        position.set(x, -y, getZOffset());
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        Analysis analysis = DamageXRaySystem.getActiveAnalysis();
        if (blendingEnabled || analysis == null || analysis.targetEntity == null || !analysis.targetEntity.isValid) {
            return;
        }

        setTargetFrame(analysis.targetEntity, partialTicks);
        double maxRadius = Math.max(analysis.targetEntity.encompassingBox.widthRadius, Math.max(analysis.targetEntity.encompassingBox.heightRadius, analysis.targetEntity.encompassingBox.depthRadius));
        double modelScale = Math.min(width / Math.max(maxRadius * 2.35D, 1.0D), height / Math.max(maxRadius * 1.95D, 1.0D)) * analysis.getCameraZoom();

        GUIComponentDamageXRay.setViewRotation(analysis, viewRotation, flightVector);
        GUIComponentDamageXRay.setProjectileFocus(analysis, partialTicks, targetPosition, focusPosition);
        baseTransform.resetTransforms();
        baseTransform.setTranslation(position.x + width / 2D, position.y - height / 2D - 5, getZOffset());
        baseTransform.applyRotation(viewRotation);
        baseTransform.applyScaling(modelScale, modelScale, modelScale);
        baseTransform.applyTranslation(-focusPosition.x, -focusPosition.y, -focusPosition.z);

        DamageXRaySystem.beginPerspectiveProjection(position.x + width / 2D, position.y - height / 2D - 5, getZOffset(), Math.max(width, height) * 1.35D);
        try {
            DamageXRaySystem.beginModelRender(DamageXRaySystem.XRAY_MODEL_COLOR, 1.0F, false, LightingMode.IGNORE_ALL_LIGHTING, -Math.max(width, height) * 2.0D);
            try {
                renderEntity(analysis.targetEntity, analysis.targetEntity, baseTransform, partialTicks, blendingEnabled, true, false, analysis);
                if (analysis.targetEntity instanceof AEntityF_Multipart) {
                    for (APart part : ((AEntityF_Multipart<?>) analysis.targetEntity).allParts) {
                        if (part.isValid) {
                            renderEntity(analysis.targetEntity, part, baseTransform, partialTicks, blendingEnabled, true, false, analysis);
                        }
                    }
                }
            } finally {
                DamageXRaySystem.endModelRender();
            }

            if (analysis.targetEntity instanceof AEntityF_Multipart) {
                for (APart part : ((AEntityF_Multipart<?>) analysis.targetEntity).allParts) {
                    if (part.isValid && isXRayModule(part)) {
                        renderDetailModel(analysis.targetEntity, part, baseTransform, partialTicks, blendingEnabled, analysis);
                    }
                }
            }
            renderEntity(analysis.targetEntity, analysis.targetEntity, baseTransform, partialTicks, blendingEnabled, false, true, analysis);
            if (analysis.targetEntity instanceof AEntityF_Multipart) {
                for (APart part : ((AEntityF_Multipart<?>) analysis.targetEntity).allParts) {
                    if (part.isValid) {
                        renderEntity(analysis.targetEntity, part, baseTransform, partialTicks, blendingEnabled, false, true, analysis);
                    }
                }
            }
        } finally {
            DamageXRaySystem.endPerspectiveProjection();
        }
    }

    private void renderDetailModel(AEntityE_Interactable<?> targetEntity, AEntityE_Interactable<?> entity, TransformationMatrix baseTransform, float partialTicks, boolean blendingEnabled, Analysis analysis) {
        ColorRGB detailColor = getDetailColor(entity.damageVar.currentValue, entity.definition.general.health, analysis.isEntityDamageFlashActive(entity.uniqueUUID));
        DamageXRaySystem.beginModelRender(detailColor, 1.0F, false, LightingMode.IGNORE_WORLD_LIGHTING, 0.0D);
        try {
            renderEntity(targetEntity, entity, baseTransform, partialTicks, blendingEnabled, true, false, analysis);
        } finally {
            DamageXRaySystem.endModelRender();
        }
    }

    private static ColorRGB getDetailColor(double damage, double health, boolean damageFlashActive) {
        if (damage <= 0.0D || health <= 0.0D) {
            return damageFlashActive ? DamageXRaySystem.XRAY_DAMAGE_FLASH_COLOR : DamageXRaySystem.XRAY_DETAIL_COLOR;
        }
        if (damage >= health) {
            return damageFlashActive ? DamageXRaySystem.XRAY_DESTROYED_FLASH_COLOR : DamageXRaySystem.XRAY_DESTROYED_COLOR;
        }
        return damageFlashActive ? DamageXRaySystem.XRAY_DAMAGED_FLASH_COLOR : DamageXRaySystem.XRAY_DAMAGED_COLOR;
    }

    private static boolean isXRayModule(APart part) {
        return !isTurretPart(part) && (part instanceof PartEngine || part instanceof PartGun || part instanceof PartInteractable || part instanceof PartGroundDevice);
    }

    private static boolean isTurretPart(APart part) {
        if (part.placementDefinition.types != null) {
            for (String type : part.placementDefinition.types) {
                if (type.contains("gun_turret") || type.contains("generic_turret")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void renderEntity(AEntityE_Interactable<?> targetEntity, AEntityE_Interactable<?> entity, TransformationMatrix baseTransform, float partialTicks, boolean blendingEnabled, boolean renderModel, boolean renderHealthBoxes, Analysis analysis) {
        entityTransform.set(baseTransform);
        if (entity == targetEntity) {
            entityPosition.set(targetPosition);
            entityOrientation.set(targetOrientation);
        } else {
            setEntityFrame(entity, partialTicks);
            relativePosition.set(entityPosition).subtract(targetPosition).reOrigin(targetOrientation);
            setRelativeOrientation(relativeOrientation, targetOrientation, entityOrientation);
            entityTransform.applyTranslation(relativePosition);
            entityTransform.applyRotation(relativeOrientation);
        }
        interpolatedScale.set(entity.scale).subtract(entity.prevScale).scale(partialTicks).add(entity.prevScale);
        entityTransform.applyScaling(interpolatedScale);
        if (renderModel) {
            entity.renderModelWithTransform(entityTransform, blendingEnabled, partialTicks);
        }
        if (renderHealthBoxes) {
            renderHealthBoxes(entity, analysis);
        }
    }

    private void renderHealthBoxes(AEntityE_Interactable<?> entity, Analysis analysis) {
        if (entity.definition.collisionGroups == null) {
            return;
        }
        for (int groupIndex = 0; groupIndex < entity.definition.collisionGroups.size(); ++groupIndex) {
            JSONCollisionGroup group = entity.definition.collisionGroups.get(groupIndex);
            if (group.health <= 0 || groupIndex >= entity.definitionCollisionBoxes.size()) {
                continue;
            }
            double damage = entity.getOrCreateVariable("collision_" + (groupIndex + 1) + "_damage").currentValue;
            boolean destroyed = damage >= group.health;
            ColorRGB detailColor = getDetailColor(damage, group.health, analysis.isCollisionDamageFlashActive(entity.uniqueUUID, groupIndex + 1));
            DamageXRaySystem.beginModelRender(detailColor, 1.0F, false, LightingMode.IGNORE_WORLD_LIGHTING, 0.0D);
            try {
                for (BoundingBox box : entity.definitionCollisionBoxes.get(groupIndex)) {
                    if (!destroyed && !entity.collisionBoxes.contains(box)) {
                        continue;
                    }
                    if (destroyed) {
                        healthBoxOffset.set(box.localCenter);
                        healthBoxRenderBounds.widthRadius = box.definition.width / 2.0D;
                        healthBoxRenderBounds.heightRadius = box.definition.height / 2.0D;
                        healthBoxRenderBounds.depthRadius = box.definition.width / 2.0D;
                    } else {
                        healthBoxOffset.set(box.globalCenter).subtract(entityPosition).reOrigin(entityOrientation);
                        healthBoxOffset.x /= Math.max(Math.abs(interpolatedScale.x), 0.001D);
                        healthBoxOffset.y /= Math.max(Math.abs(interpolatedScale.y), 0.001D);
                        healthBoxOffset.z /= Math.max(Math.abs(interpolatedScale.z), 0.001D);
                        healthBoxRenderBounds.widthRadius = box.widthRadius / Math.max(Math.abs(interpolatedScale.x), 0.001D);
                        healthBoxRenderBounds.heightRadius = box.heightRadius / Math.max(Math.abs(interpolatedScale.y), 0.001D);
                        healthBoxRenderBounds.depthRadius = box.depthRadius / Math.max(Math.abs(interpolatedScale.z), 0.001D);
                    }
                    healthBoxRenderBounds.renderHolographic(entityTransform, healthBoxOffset, detailColor);
                }
            } finally {
                DamageXRaySystem.endModelRender();
            }
        }
    }

    private void setTargetFrame(AEntityE_Interactable<?> targetEntity, float partialTicks) {
        targetPosition.set(targetEntity.prevPosition).interpolate(targetEntity.position, partialTicks);
        targetEntity.getInterpolatedOrientation(targetOrientation, partialTicks);
    }

    private void setEntityFrame(AEntityC_Renderable entity, float partialTicks) {
        entityPosition.set(entity.prevPosition).interpolate(entity.position, partialTicks);
        entity.getInterpolatedOrientation(entityOrientation, partialTicks);
    }

    private static void setRelativeOrientation(RotationMatrix result, RotationMatrix origin, RotationMatrix entity) {
        double r00 = origin.m00 * entity.m00 + origin.m10 * entity.m10 + origin.m20 * entity.m20;
        double r01 = origin.m00 * entity.m01 + origin.m10 * entity.m11 + origin.m20 * entity.m21;
        double r02 = origin.m00 * entity.m02 + origin.m10 * entity.m12 + origin.m20 * entity.m22;
        double r10 = origin.m01 * entity.m00 + origin.m11 * entity.m10 + origin.m21 * entity.m20;
        double r11 = origin.m01 * entity.m01 + origin.m11 * entity.m11 + origin.m21 * entity.m21;
        double r12 = origin.m01 * entity.m02 + origin.m11 * entity.m12 + origin.m21 * entity.m22;
        double r20 = origin.m02 * entity.m00 + origin.m12 * entity.m10 + origin.m22 * entity.m20;
        double r21 = origin.m02 * entity.m01 + origin.m12 * entity.m11 + origin.m22 * entity.m21;
        double r22 = origin.m02 * entity.m02 + origin.m12 * entity.m12 + origin.m22 * entity.m22;
        result.m00 = r00;
        result.m01 = r01;
        result.m02 = r02;
        result.m10 = r10;
        result.m11 = r11;
        result.m12 = r12;
        result.m20 = r20;
        result.m21 = r21;
        result.m22 = r22;
        result.bypassAngles();
    }
}
