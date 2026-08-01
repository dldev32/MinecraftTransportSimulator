package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.DamageXRaySystem;
import minecrafttransportsimulator.systems.DamageXRaySystem.Analysis;

/**
 * Moving procedurally-generated projectile for the x-ray hitcam replay.
 */
public class GUIComponentDamageXRayBullet extends AGUIComponent {
    private static final int BULLET_SEGMENTS = 16;
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_SEGMENT = 12;
    private static final double BODY_BASE_Z = -1.0D;
    private static final double BODY_SHOULDER_Z = 0.35D;
    private static final double BULLET_TIP_Z = 1.0D;
    private static final ColorRGB BULLET_COLOR = new ColorRGB(255, 137, 36);

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
    private final RenderableData bulletRenderable = new RenderableData(createBulletVertices(), DamageXRaySystem.XRAY_SOLID_TEXTURE);

    public GUIComponentDamageXRayBullet(int x, int y) {
        super(x, y, 0, 0);
        bulletRenderable.setColor(BULLET_COLOR);
        bulletRenderable.setAlpha(0.98F);
        bulletRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
    }

    @Override
    public int getZOffset() {
        return MODEL_DEFAULT_ZOFFSET;
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
        if (!blendingEnabled || analysis == null || analysis.targetEntity == null || !analysis.targetEntity.isValid) {
            return;
        }

        GUIComponentDamageXRay.setViewRotation(analysis, viewRotation, flightVector);
        GUIComponentDamageXRay.setProjectileFocus(analysis, partialTicks, targetPosition, focusPosition);
        currentProjectileWorld.set(analysis.startPosition).interpolate(analysis.endPosition, analysis.getPlaybackProgress());
        GUIComponentDamageXRay.projectPathPoint(analysis, currentProjectileWorld, partialTicks, panelWidth, panelHeight, viewRotation, targetPosition, focusPosition, projectedPoint);

        localFlightVector.set(analysis.endPosition).subtract(analysis.startPosition).reOrigin(analysis.targetEntity.orientation).normalize();
        bulletRotation.setToVector(localFlightVector, false);
        viewBulletRotation.set(viewRotation).multiply(bulletRotation);

        double displayRadius = Math.max(1.25D, Math.min(6.0D, Math.sqrt(Math.max(analysis.bulletDiameter, 1.0F)) * 0.55D)) * analysis.getCameraZoom();
        double displayLength = displayRadius * 4.5D;
        position.set(panelX + projectedPoint.x, -panelY + projectedPoint.y, MODEL_DEFAULT_ZOFFSET + 35);
        bulletRenderable.transform.resetTransforms();
        bulletRenderable.transform.setTranslation(position);
        bulletRenderable.transform.applyRotation(viewBulletRotation);
        bulletRenderable.transform.applyScaling(displayRadius, displayRadius, displayLength / (BULLET_TIP_Z - BODY_BASE_Z));
        bulletRenderable.setAlpha(0.98F * analysis.getAlpha());
        bulletRenderable.render();
    }

    private static RenderableVertices createBulletVertices() {
        FloatBuffer vertices = FloatBuffer.allocate(BULLET_SEGMENTS * VERTICES_PER_SEGMENT * FLOATS_PER_VERTEX);
        double noseLength = BULLET_TIP_Z - BODY_SHOULDER_Z;
        double coneNormalLength = Math.sqrt(noseLength * noseLength + 1.0D);
        for (int segment = 0; segment < BULLET_SEGMENTS; ++segment) {
            double angle1 = Math.PI * 2.0D * segment / BULLET_SEGMENTS;
            double angle2 = Math.PI * 2.0D * (segment + 1) / BULLET_SEGMENTS;
            double middleAngle = (angle1 + angle2) * 0.5D;
            double x1 = Math.cos(angle1);
            double y1 = Math.sin(angle1);
            double x2 = Math.cos(angle2);
            double y2 = Math.sin(angle2);

            addVertex(vertices, x1, y1, BODY_BASE_Z, x1, y1, 0.0D);
            addVertex(vertices, x2, y2, BODY_SHOULDER_Z, x2, y2, 0.0D);
            addVertex(vertices, x1, y1, BODY_SHOULDER_Z, x1, y1, 0.0D);
            addVertex(vertices, x1, y1, BODY_BASE_Z, x1, y1, 0.0D);
            addVertex(vertices, x2, y2, BODY_BASE_Z, x2, y2, 0.0D);
            addVertex(vertices, x2, y2, BODY_SHOULDER_Z, x2, y2, 0.0D);

            addVertex(vertices, x1, y1, BODY_SHOULDER_Z, x1 * noseLength / coneNormalLength, y1 * noseLength / coneNormalLength, 1.0D / coneNormalLength);
            addVertex(vertices, x2, y2, BODY_SHOULDER_Z, x2 * noseLength / coneNormalLength, y2 * noseLength / coneNormalLength, 1.0D / coneNormalLength);
            addVertex(vertices, 0.0D, 0.0D, BULLET_TIP_Z, Math.cos(middleAngle) * noseLength / coneNormalLength, Math.sin(middleAngle) * noseLength / coneNormalLength, 1.0D / coneNormalLength);

            addVertex(vertices, 0.0D, 0.0D, BODY_BASE_Z, 0.0D, 0.0D, -1.0D);
            addVertex(vertices, x2, y2, BODY_BASE_Z, 0.0D, 0.0D, -1.0D);
            addVertex(vertices, x1, y1, BODY_BASE_Z, 0.0D, 0.0D, -1.0D);
        }
        vertices.flip();
        return new RenderableVertices("XRAY_GENERATED_PROJECTILE", vertices, false);
    }

    private static void addVertex(FloatBuffer vertices, double x, double y, double z, double normalX, double normalY, double normalZ) {
        vertices.put((float) normalX);
        vertices.put((float) normalY);
        vertices.put((float) normalZ);
        vertices.put(0.5F);
        vertices.put(0.5F);
        vertices.put((float) x);
        vertices.put((float) y);
        vertices.put((float) z);
    }
}
