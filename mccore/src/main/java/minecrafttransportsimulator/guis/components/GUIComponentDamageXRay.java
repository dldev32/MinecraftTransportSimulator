package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.DamageXRaySystem;
import minecrafttransportsimulator.systems.DamageXRaySystem.Analysis;
import minecrafttransportsimulator.systems.DamageXRaySystem.FragmentEvent;
import minecrafttransportsimulator.systems.DamageXRaySystem.HitEvent;

/**
 * Corner HUD window that replays bullet damage as an x-ray schematic.
 */
public class GUIComponentDamageXRay extends AGUIComponent {
    private static final int PANEL_Z = 20;
    private static final int MAX_LINES = 36;
    private static final int HIT_MARKER_SEGMENTS = 28;
    private static final int FLOATS_PER_VERTEX = 8;
    private static final ColorRGB LINE_COLOR = new ColorRGB(106, 220, 255);
    private static final ColorRGB FRAGMENT_COLOR = new ColorRGB(255, 174, 74);
    private static final ColorRGB TEXT_COLOR = new ColorRGB(225, 248, 255);
    private static final ColorRGB ALERT_COLOR = new ColorRGB(255, 126, 104);

    private final RenderableData lineRenderable;
    private final RenderableData fragmentRenderable;
    private final RenderableData hitMarkerRenderable;
    private final Point3D textHelper = new Point3D();
    private final Point3D pathStart = new Point3D();
    private final Point3D pathEnd = new Point3D();
    private final Point3D currentProjectile = new Point3D();
    private final Point3D currentProjectileWorld = new Point3D();
    private final Point3D fragmentStart = new Point3D();
    private final Point3D fragmentEnd = new Point3D();
    private final Point3D currentFragmentEnd = new Point3D();
    private final Point3D projectedPoint = new Point3D();
    private final Point3D targetPosition = new Point3D();
    private final Point3D focusPosition = new Point3D();
    private final Point3D flightVector = new Point3D();
    private final RotationMatrix viewRotation = new RotationMatrix();

    public GUIComponentDamageXRay(int x, int y, int width, int height) {
        super(x, y, width, height);
        lineRenderable = new RenderableData(new RenderableVertices(MAX_LINES), null);
        lineRenderable.setColor(LINE_COLOR);
        lineRenderable.setAlpha(0.95F);
        lineRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        fragmentRenderable = new RenderableData(new RenderableVertices(DamageXRaySystem.MAX_FRAGMENT_EVENTS), null);
        fragmentRenderable.setColor(FRAGMENT_COLOR);
        fragmentRenderable.setAlpha(0.72F);
        fragmentRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        hitMarkerRenderable = new RenderableData(new RenderableVertices("XRAY_HIT_MARKERS", FloatBuffer.allocate(DamageXRaySystem.MAX_EVENTS * HIT_MARKER_SEGMENTS * 3 * FLOATS_PER_VERTEX), false), DamageXRaySystem.XRAY_SOLID_TEXTURE);
        hitMarkerRenderable.setColor(ALERT_COLOR);
        hitMarkerRenderable.setAlpha(0.48F);
        hitMarkerRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
    }

    @Override
    public int getZOffset() {
        return PANEL_Z;
    }

    public void setPanelBounds(int x, int y, int width, int height) {
        position.set(x, -y, PANEL_Z);
        textPosition.set(x, -y, PANEL_Z + TEXT_DEFAULT_ZOFFSET);
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        Analysis analysis = DamageXRaySystem.getActiveAnalysis();
        if (analysis == null) {
            return;
        }

        float alpha = analysis.getAlpha();
        if (blendingEnabled) {
            renderTrace(analysis, alpha, partialTicks);
        } else {
            renderLabels(gui, analysis, alpha);
        }
    }

    private void renderTrace(Analysis analysis, float alpha, float partialTicks) {
        FloatBuffer vertices = lineRenderable.vertexObject.vertices;
        FloatBuffer fragmentVertices = fragmentRenderable.vertexObject.vertices;
        FloatBuffer hitMarkerVertices = hitMarkerRenderable.vertexObject.vertices;
        vertices.clear();
        fragmentVertices.clear();
        hitMarkerVertices.clear();
        int linesUsed = 0;
        int fragmentLinesUsed = 0;
        setViewRotation(analysis, viewRotation, flightVector);
        setProjectileFocus(analysis, partialTicks, targetPosition, focusPosition);

        linesUsed = addLine(vertices, linesUsed, 1, -1, width - 1, -1);
        linesUsed = addLine(vertices, linesUsed, width - 1, -1, width - 1, -height + 1);
        linesUsed = addLine(vertices, linesUsed, width - 1, -height + 1, 1, -height + 1);
        linesUsed = addLine(vertices, linesUsed, 1, -height + 1, 1, -1);

        float playbackProgress = analysis.getPlaybackProgress();
        projectPathPoint(analysis, analysis.startPosition, partialTicks, width, height, viewRotation, targetPosition, focusPosition, pathStart);
        projectPathPoint(analysis, analysis.endPosition, partialTicks, width, height, viewRotation, targetPosition, focusPosition, pathEnd);
        currentProjectileWorld.set(analysis.startPosition).interpolate(analysis.endPosition, playbackProgress);
        projectPathPoint(analysis, currentProjectileWorld, partialTicks, width, height, viewRotation, targetPosition, focusPosition, currentProjectile);
        linesUsed = addLine(vertices, linesUsed, (float) pathStart.x, (float) pathStart.y, (float) currentProjectile.x, (float) currentProjectile.y);

        for (FragmentEvent event : analysis.fragmentEvents) {
            float fragmentProgress = analysis.getFragmentProgress(event);
            if (fragmentProgress <= 0.0F) {
                continue;
            }
            projectPathPoint(analysis, event.startPosition, partialTicks, width, height, viewRotation, targetPosition, focusPosition, fragmentStart);
            currentFragmentEnd.set(event.startPosition).interpolate(event.endPosition, fragmentProgress);
            projectPathPoint(analysis, currentFragmentEnd, partialTicks, width, height, viewRotation, targetPosition, focusPosition, fragmentEnd);
            fragmentLinesUsed = addLine(fragmentVertices, fragmentLinesUsed, (float) fragmentStart.x, (float) fragmentStart.y, (float) fragmentEnd.x, (float) fragmentEnd.y);
        }

        for (HitEvent event : analysis.hitEvents) {
            if (event.pathProgress > playbackProgress + 0.02F) {
                continue;
            }
            float markerProgress = analysis.getHitMarkerProgress(event);
            if (markerProgress < 0 || markerProgress >= 1.0F) {
                continue;
            }
            projectPathPoint(analysis, event.hitPosition, partialTicks, width, height, viewRotation, targetPosition, focusPosition, projectedPoint);
            addCircle(hitMarkerVertices, (float) projectedPoint.x, (float) projectedPoint.y, 13.0F * (1.0F - markerProgress));
        }
        while (linesUsed < MAX_LINES) {
            linesUsed = addLine(vertices, linesUsed, 0, 0, 0, 0);
        }
        while (fragmentLinesUsed < DamageXRaySystem.MAX_FRAGMENT_EVENTS) {
            fragmentLinesUsed = addLine(fragmentVertices, fragmentLinesUsed, 0, 0, 0, 0);
        }
        vertices.flip();
        fragmentVertices.flip();
        hitMarkerVertices.flip();

        lineRenderable.transform.setTranslation(position.x, position.y, MODEL_DEFAULT_ZOFFSET + 30);
        lineRenderable.setAlpha(0.95F * alpha);
        lineRenderable.render();
        fragmentRenderable.transform.setTranslation(position.x, position.y, MODEL_DEFAULT_ZOFFSET + 34);
        fragmentRenderable.setAlpha(0.72F * alpha);
        fragmentRenderable.render();
        hitMarkerRenderable.transform.setTranslation(position.x, position.y, MODEL_DEFAULT_ZOFFSET + 42);
        hitMarkerRenderable.setAlpha(0.48F * alpha);
        hitMarkerRenderable.render();
    }

    private static int addLine(FloatBuffer vertices, int lineIndex, float x1, float y1, float x2, float y2) {
        vertices.put(x1);
        vertices.put(y1);
        vertices.put(0);
        vertices.put(x2);
        vertices.put(y2);
        vertices.put(0);
        return lineIndex + 1;
    }

    private static void addCircle(FloatBuffer vertices, float centerX, float centerY, float radius) {
        if (radius <= 0.1F) {
            return;
        }
        for (int i = 0; i < HIT_MARKER_SEGMENTS; ++i) {
            double angle1 = Math.PI * 2.0D * i / HIT_MARKER_SEGMENTS;
            double angle2 = Math.PI * 2.0D * (i + 1) / HIT_MARKER_SEGMENTS;
            addCircleVertex(vertices, centerX, centerY);
            addCircleVertex(vertices, centerX + (float) Math.cos(angle1) * radius, centerY + (float) Math.sin(angle1) * radius);
            addCircleVertex(vertices, centerX + (float) Math.cos(angle2) * radius, centerY + (float) Math.sin(angle2) * radius);
        }
    }

    private static void addCircleVertex(FloatBuffer vertices, float x, float y) {
        vertices.put(0);
        vertices.put(0);
        vertices.put(1);
        vertices.put(0.5F);
        vertices.put(0.5F);
        vertices.put(x);
        vertices.put(y);
        vertices.put(0);
    }

    public static void setViewRotation(Analysis analysis, RotationMatrix viewRotation, Point3D flightVector) {
        flightVector.set(analysis.startPosition).subtract(analysis.endPosition).reOrigin(analysis.targetEntity.orientation).normalize();
        viewRotation.setToVector(flightVector, false);
        double m01 = viewRotation.m01;
        double m02 = viewRotation.m02;
        double m12 = viewRotation.m12;
        viewRotation.m01 = viewRotation.m10;
        viewRotation.m02 = viewRotation.m20;
        viewRotation.m10 = m01;
        viewRotation.m12 = viewRotation.m21;
        viewRotation.m20 = m02;
        viewRotation.m21 = m12;
        viewRotation.bypassAngles();
        viewRotation.rotateY(analysis.getCameraTurnAngle());
    }

    public static void setProjectileFocus(Analysis analysis, float partialTicks, Point3D targetPosition, Point3D focusPosition) {
        double targetX = analysis.targetEntity.prevPosition.x + (analysis.targetEntity.position.x - analysis.targetEntity.prevPosition.x) * partialTicks;
        double targetY = analysis.targetEntity.prevPosition.y + (analysis.targetEntity.position.y - analysis.targetEntity.prevPosition.y) * partialTicks;
        double targetZ = analysis.targetEntity.prevPosition.z + (analysis.targetEntity.position.z - analysis.targetEntity.prevPosition.z) * partialTicks;
        targetPosition.set(targetX, targetY, targetZ);
        focusPosition.set(analysis.startPosition).interpolate(analysis.endPosition, analysis.getFocusProgress()).subtract(targetPosition).reOrigin(analysis.targetEntity.orientation);
    }

    public static void projectPathPoint(Analysis analysis, Point3D worldPoint, float partialTicks, int width, int height, RotationMatrix viewRotation, Point3D targetPosition, Point3D focusPosition, Point3D projectedPoint) {
        double targetX = analysis.targetEntity.prevPosition.x + (analysis.targetEntity.position.x - analysis.targetEntity.prevPosition.x) * partialTicks;
        double targetY = analysis.targetEntity.prevPosition.y + (analysis.targetEntity.position.y - analysis.targetEntity.prevPosition.y) * partialTicks;
        double targetZ = analysis.targetEntity.prevPosition.z + (analysis.targetEntity.position.z - analysis.targetEntity.prevPosition.z) * partialTicks;
        targetPosition.set(targetX, targetY, targetZ);
        projectedPoint.set(worldPoint).subtract(targetPosition).reOrigin(analysis.targetEntity.orientation).subtract(focusPosition).rotate(viewRotation);

        double maxRadius = Math.max(analysis.targetEntity.encompassingBox.widthRadius, Math.max(analysis.targetEntity.encompassingBox.heightRadius, analysis.targetEntity.encompassingBox.depthRadius));
        double scale = Math.min(width / Math.max(maxRadius * 2.7D, 1.0D), height / Math.max(maxRadius * 2.2D, 1.0D));
        double depth = projectedPoint.z * scale;
        double cameraDistance = Math.max(width, height) * 1.35D;
        double perspective = cameraDistance / Math.max(cameraDistance - depth, cameraDistance * 0.30D);
        projectedPoint.set(width / 2D + projectedPoint.x * scale * perspective, -height / 2D + projectedPoint.y * scale * perspective, 0);
    }

    private void renderLabels(AGUIBase gui, Analysis analysis, float alpha) {
        float textAlpha = 0.92F * alpha;
        int left = (int) position.x + 8;
        int top = (int) -position.y + 8;
        int textWidth = Math.max(80, width - 16);
        textHelper.set(left, -top, textPosition.z + 15);
        RenderText.drawText("X-RAY HITCAM", null, textHelper, TEXT_COLOR, TextAlignment.LEFT_ALIGNED, 0.62F, false, textWidth, true, gui.worldLightValue, textAlpha);
        textHelper.set(left, -(top + 13), textPosition.z + 15);
        RenderText.drawText(analysis.resultType.title, null, textHelper, analysis.resultType == DamageXRaySystem.ResultType.ARMOR_STOP ? ALERT_COLOR : TEXT_COLOR, TextAlignment.LEFT_ALIGNED, 0.72F, false, textWidth, true, gui.worldLightValue, textAlpha);
        textHelper.set(left, -(top + height - 52), textPosition.z + 15);
        RenderText.drawText(analysis.targetName, null, textHelper, TEXT_COLOR, TextAlignment.LEFT_ALIGNED, 0.56F, false, textWidth, true, gui.worldLightValue, textAlpha);
        textHelper.set(left, -(top + height - 40), textPosition.z + 15);
        RenderText.drawText(analysis.bulletName, null, textHelper, TEXT_COLOR, TextAlignment.LEFT_ALIGNED, 0.56F, false, textWidth, true, gui.worldLightValue, textAlpha);
        textHelper.set(left, -(top + height - 28), textPosition.z + 15);
        RenderText.drawText("Pen " + (int) analysis.getTotalArmor() + "/" + (int) analysis.getPenetrationPotential() + "mm  Dmg " + String.format("%.1f", analysis.getTotalDamage()), null, textHelper, TEXT_COLOR, TextAlignment.LEFT_ALIGNED, 0.56F, false, textWidth, true, gui.worldLightValue, textAlpha);

        int eventLines = 0;
        for (HitEvent event : analysis.hitEvents) {
            if (event.pathProgress > analysis.getPlaybackProgress() + 0.02F) {
                continue;
            }
            textHelper.set(left + 4, -(top + 31 + eventLines * 11), textPosition.z + 15);
            RenderText.drawText(event.getShortLabel(), null, textHelper, event.stopped ? ALERT_COLOR : TEXT_COLOR, TextAlignment.LEFT_ALIGNED, 0.48F, false, textWidth - 8, true, gui.worldLightValue, textAlpha);
            if (++eventLines == 4) {
                break;
            }
        }
    }
}
