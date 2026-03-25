package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Component that renders a health bar with color-coded fill based on damage state.
 * Uses two sprites: one for the background (dark) and one for the filled portion (colored).
 * Color changes from green to yellow to orange to red based on health percentage.
 * The white 8x8 pixel block in the damage_panel.png texture (at 0,240) is used as a
 * color-tintable source for the bars.
 *
 * @author don_bruce
 */
public class GUIComponentDamageBar extends AGUIComponent {
    public static final String DAMAGE_PANEL_TEXTURE = "mts:textures/guis/damage_panel.png";

    private static final ColorRGB BG_COLOR = new ColorRGB(30, 30, 30);
    private static final ColorRGB COLOR_HEALTHY = new ColorRGB(0, 200, 0);
    private static final ColorRGB COLOR_DAMAGED = new ColorRGB(255, 200, 0);
    private static final ColorRGB COLOR_CRITICAL = new ColorRGB(255, 100, 0);
    private static final ColorRGB COLOR_DESTROYED = new ColorRGB(200, 0, 0);

    private final RenderableData bgRenderable;
    private final RenderableData fillRenderable;
    private final int barWidth;
    private final int barHeight;
    private float healthPercent = 1.0F;

    //White pixel block in damage_panel.png at (0, 240), size 8x8, texture is 256x256.
    private static final float WHITE_U1 = 0 / 256.0F;
    private static final float WHITE_V1 = 240 / 256.0F;
    private static final float WHITE_U2 = 8 / 256.0F;
    private static final float WHITE_V2 = 248 / 256.0F;

    public GUIComponentDamageBar(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.barWidth = width;
        this.barHeight = height;

        RenderableVertices bgVertices = RenderableVertices.createSprite(1, null, null);
        bgRenderable = new RenderableData(bgVertices, DAMAGE_PANEL_TEXTURE);
        bgRenderable.setTransucentOverride();

        RenderableVertices fillVertices = RenderableVertices.createSprite(1, null, null);
        fillRenderable = new RenderableData(fillVertices, DAMAGE_PANEL_TEXTURE);
        fillRenderable.setTransucentOverride();
    }

    /**
     * Sets the current health percentage (0.0 to 1.0).
     */
    public void setHealthPercent(float percent) {
        this.healthPercent = Math.max(0, Math.min(1, percent));
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (bgRenderable.isTranslucent == blendingEnabled) {
            //Render background bar.
            bgRenderable.vertexObject.setSpriteProperties(0, 0, 0, barWidth, barHeight, WHITE_U1, WHITE_V1, WHITE_U2, WHITE_V2);
            bgRenderable.transform.setTranslation(position);
            bgRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
            bgRenderable.setColor(BG_COLOR);
            bgRenderable.setAlpha(0.7F);
            bgRenderable.render();

            //Render filled portion based on health.
            if (healthPercent > 0) {
                int fillWidth = (int) (barWidth * healthPercent);
                if (fillWidth < 1) fillWidth = 1;
                fillRenderable.vertexObject.setSpriteProperties(0, 0, 0, fillWidth, barHeight, WHITE_U1, WHITE_V1, WHITE_U2, WHITE_V2);
                fillRenderable.transform.setTranslation(position);
                fillRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
                fillRenderable.setColor(getDamageColor(healthPercent));
                fillRenderable.setAlpha(0.9F);
                fillRenderable.render();
            }
        }
    }

    /**
     * Returns the color for the given health percentage.
     */
    static ColorRGB getDamageColor(float healthPercent) {
        if (healthPercent > 0.75F) {
            return COLOR_HEALTHY;
        } else if (healthPercent > 0.5F) {
            return COLOR_DAMAGED;
        } else if (healthPercent > 0.25F) {
            return COLOR_CRITICAL;
        } else {
            return COLOR_DESTROYED;
        }
    }
}
