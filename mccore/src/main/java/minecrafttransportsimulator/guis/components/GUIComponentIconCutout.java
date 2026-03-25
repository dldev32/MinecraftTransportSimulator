package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;

/**
 * Extension of {@link GUIComponentCutout} that supports color tinting.
 * Used for damage panel icons that change from white to red when a part is destroyed.
 *
 * @author don_bruce
 */
public class GUIComponentIconCutout extends GUIComponentCutout {
    public ColorRGB tintColor = ColorRGB.WHITE;

    public GUIComponentIconCutout(AGUIBase gui, int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        super(gui, x, y, width, height, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        RenderableData currentRenderable = renderLitTexture ? renderableL : renderable;
        if (currentRenderable.isTranslucent == blendingEnabled) {
            currentRenderable.setTexture(renderLitTexture ? gui.getTexture().replace(NORMAL_SUFFIX, LIT_SUFFIX) : gui.getTexture());
            currentRenderable.vertexObject.setSpriteProperties(0, 0, 0, width, height, textureXOffset / (float) gui.getTextureWidth(), textureYOffset / (float) gui.getTextureHeight(), (textureXOffset + textureSectionWidth) / (float) gui.getTextureWidth(), (textureYOffset + textureSectionHeight) / (float) gui.getTextureHeight());
            currentRenderable.transform.setTranslation(position);
            currentRenderable.setColor(tintColor);
            currentRenderable.setLightValue(gui.worldLightValue);
            currentRenderable.setLightMode(renderBright || ignoreGUILightingState ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.IGNORE_ORIENTATION_LIGHTING);
            currentRenderable.render();
        }
    }
}
