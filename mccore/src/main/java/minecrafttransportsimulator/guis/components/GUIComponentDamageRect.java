package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Simple component that renders a colored rectangle with adjustable alpha.
 * Uses the white pixel block in the damage_panel.png texture for color tinting.
 *
 * @author don_bruce
 */
public class GUIComponentDamageRect extends AGUIComponent {
    private final RenderableData renderable;
    private final int rectWidth;
    private final int rectHeight;
    private final ColorRGB color;
    private final float alpha;

    //White pixel block in damage_panel.png at (0, 240), size 8x8, texture is 256x256.
    private static final float WHITE_U1 = 0 / 256.0F;
    private static final float WHITE_V1 = 240 / 256.0F;
    private static final float WHITE_U2 = 8 / 256.0F;
    private static final float WHITE_V2 = 248 / 256.0F;

    public GUIComponentDamageRect(int x, int y, int width, int height, ColorRGB color, float alpha) {
        super(x, y, width, height);
        this.rectWidth = width;
        this.rectHeight = height;
        this.color = color;
        this.alpha = alpha;

        RenderableVertices vertices = RenderableVertices.createSprite(1, null, null);
        renderable = new RenderableData(vertices, GUIComponentDamageBar.DAMAGE_PANEL_TEXTURE);
        renderable.setTransucentOverride();
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (renderable.isTranslucent == blendingEnabled) {
            renderable.vertexObject.setSpriteProperties(0, 0, 0, rectWidth, rectHeight, WHITE_U1, WHITE_V1, WHITE_U2, WHITE_V2);
            renderable.transform.setTranslation(position);
            renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
            renderable.setColor(color);
            renderable.setAlpha(alpha);
            renderable.render();
        }
    }
}
