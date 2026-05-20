package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.DamageXRaySystem;
import minecrafttransportsimulator.systems.DamageXRaySystem.Analysis;

/**
 * Moving projectile model for the x-ray hitcam replay.
 */
public class GUIComponentDamageXRayBullet extends GUIComponent3DModel {
    private static final ColorRGB BULLET_COLOR = new ColorRGB(255, 219, 156);

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private final Point3D currentProjectileWorld = new Point3D();
    private final Point3D projectedPoint = new Point3D();
    private final Point3D targetPosition = new Point3D();
    private final Point3D focusPosition = new Point3D();
    private final Point3D flightVector = new Point3D();
    private final Point3D localFlightVector = new Point3D();
    private final RotationMatrix viewRotation = new RotationMatrix();
    private final RotationMatrix bulletRotation = new RotationMatrix();
    private final RotationMatrix viewBulletRotation = new RotationMatrix();
    private final BoundingBox fallbackBulletBounds = new BoundingBox(new Point3D(), 0.15D, 0.15D, 0.42D);
    private final RenderableData fallbackRenderable = new RenderableData(new RenderableVertices(true), DamageXRaySystem.XRAY_SOLID_TEXTURE);

    public GUIComponentDamageXRayBullet(int x, int y) {
        super(x, y, 22.0F, false, false, false);
        this.renderColor = BULLET_COLOR;
        fallbackRenderable.setColor(BULLET_COLOR);
        fallbackRenderable.setAlpha(0.95F);
        fallbackRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        fallbackRenderable.setBoxBounds(fallbackBulletBounds, false);
    }

    public void setPanelBounds(int x, int y, int width, int height) {
        this.panelX = x;
        this.panelY = y;
        this.panelWidth = width;
        this.panelHeight = height;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        Analysis analysis = DamageXRaySystem.getActiveAnalysis();
        if (!blendingEnabled || analysis == null) {
            return;
        }

        GUIComponentDamageXRay.setViewRotation(analysis, viewRotation, flightVector);
        GUIComponentDamageXRay.setProjectileFocus(analysis, partialTicks, targetPosition, focusPosition);
        currentProjectileWorld.set(analysis.startPosition).interpolate(analysis.endPosition, analysis.getPlaybackProgress());
        GUIComponentDamageXRay.projectPathPoint(analysis, currentProjectileWorld, partialTicks, panelWidth, panelHeight, viewRotation, targetPosition, focusPosition, projectedPoint);

        localFlightVector.set(analysis.endPosition).subtract(analysis.startPosition).reOrigin(analysis.targetEntity.orientation).normalize();
        bulletRotation.setToVector(localFlightVector, false);
        viewBulletRotation.set(viewRotation).multiply(bulletRotation);

        modelLocation = analysis.bulletModelLocation;
        textureLocation = DamageXRaySystem.XRAY_SOLID_TEXTURE;
        rotationOverride = viewBulletRotation;
        renderColor = BULLET_COLOR;
        alpha = 0.95F * analysis.getAlpha();
        position.x = panelX + projectedPoint.x;
        position.y = -panelY + projectedPoint.y;
        position.z = MODEL_DEFAULT_ZOFFSET + 35;

        if (modelLocation != null) {
            super.render(gui, mouseX, mouseY, renderBright, renderLitTexture, blendingEnabled, partialTicks);
        } else {
            fallbackRenderable.transform.resetTransforms();
            fallbackRenderable.transform.setTranslation(position);
            fallbackRenderable.transform.applyRotation(viewBulletRotation);
            fallbackRenderable.transform.applyScaling(30.0D, 30.0D, 30.0D);
            fallbackRenderable.setTexture(DamageXRaySystem.XRAY_SOLID_TEXTURE);
            fallbackRenderable.setColor(BULLET_COLOR);
            fallbackRenderable.setAlpha(alpha);
            fallbackRenderable.render();
        }
    }
}
