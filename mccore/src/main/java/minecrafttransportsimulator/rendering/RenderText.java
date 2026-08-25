package minecrafttransportsimulator.rendering;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Main render class for text.  This class contains a few methods for rendering text.  These mainly pertain to rendering
 * text strings given a specified set of formatting and position/rotation.
 *
 * @author don_bruce
 */
public class RenderText {
    public static final char FORMATTING_CHAR = 167;//'§' [\u00a7], this doesn't always translate right across platforms, so we use the keycode here rather than the actual char.
    public static final char BOLD_FORMATTING_CHAR = 'l';
    public static final char ITALIC_FORMATTING_CHAR = 'o';
    public static final char UNDERLINE_FORMATTING_CHAR = 'n';
    public static final char STRIKETHROUGH_FORMATTING_CHAR = 'm';
    public static final char RANDOM_FORMATTING_CHAR = 'k';
    public static final char RESET_FORMATTING_CHAR = 'r';
    public static final char UNDERLINE_CHAR = '_';
    public static final char STRIKETHROUGH_CHAR = '-';

    private static final TransformationMatrix transformHelper = new TransformationMatrix();
    private static final TransformationMatrix textTransformHelper = new TransformationMatrix();
    private static final TransformationMatrix lineTransformHelper = new TransformationMatrix();
    private static final float FONT_HEIGHT = 8.0F;
    private static final float LINE_HEIGHT = 9.0F;
    private static final float AUTO_SHADOW_MINIMUM_BRIGHTNESS = 0.2F;
    private static final int[] FORMATTING_COLORS = new int[]{0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final int GUI_TEXTURE_BASE_SIZE = 16;
    private static final RenderableData guiTextureRenderable;

    static {
        RenderableVertices guiTextureVertices = RenderableVertices.createSprite(1, null, null);
        guiTextureVertices.setSpriteProperties(0, -GUI_TEXTURE_BASE_SIZE / 2, GUI_TEXTURE_BASE_SIZE / 2, GUI_TEXTURE_BASE_SIZE, GUI_TEXTURE_BASE_SIZE, 0, 0, 1, 1);
        guiTextureRenderable = new RenderableData(guiTextureVertices);
        guiTextureRenderable.setLightMode(RenderableData.LightingMode.IGNORE_ALL_LIGHTING);
        guiTextureRenderable.setTransucentOverride();
    }

    /**
     * Draws the specified text.  This is designed for general draws where text is defined in-code, but still may
     * use custom fonts.  This method can render the text in multiple ways depending on the parameters passed-in.
     * If a centered string is specified, then the point passed-in should be  the center point of the string, rather
     * that the top-left of the string like normal.  The same goes for right-justify.  If wrapWidth is anything else but 0,
     * then the wordWrap method will be called to render multi-line text.
     * Note that this method expects transforms to be applied such that the coordinate space is local
     * to the object rendering the text on, and is NOT the global coordinate space.  The coordinates MAY, however, be
     * in pixel-space.  This is essentially 1/16 scale of blocks, as blocks are 16 pixels each.
     * This is used for things that are already rendered in pixel-space, such as instruments and GUIs.
     * This also inverts the y coordinate as with those systems +y is down whereas normally it is up.
     * Also note that if a scale was applied prior to rendering this text, it should be passed-in here.
     * This allows for proper normal calculations to prevent needing to re-normalize the text.
     */
    public static void drawText(String text, String fontName, Point3D position, ColorRGB color, TextAlignment alignment, float scale, boolean autoScale, int wrapWidth, boolean renderLit, int worldLightValue) {
        drawText(text, fontName, position, color, alignment, scale, autoScale, wrapWidth, renderLit, worldLightValue, 1.0F, TextShadowMode.AUTO);
    }

    /**
     * Shadow-aware variant of {@link #drawText(String, String, Point3D, ColorRGB, TextAlignment, float, boolean, int, boolean, int)}.
     */
    public static void drawText(String text, String fontName, Point3D position, ColorRGB color, TextAlignment alignment, float scale, boolean autoScale, int wrapWidth, boolean renderLit, int worldLightValue, TextShadowMode shadowMode) {
        drawText(text, fontName, position, color, alignment, scale, autoScale, wrapWidth, renderLit, worldLightValue, 1.0F, shadowMode);
    }

    /**
     * Alpha-aware variant of {@link #drawText(String, String, Point3D, ColorRGB, TextAlignment, float, boolean, int, boolean, int)}.
     */
    public static void drawText(String text, String fontName, Point3D position, ColorRGB color, TextAlignment alignment, float scale, boolean autoScale, int wrapWidth, boolean renderLit, int worldLightValue, float alpha) {
        drawText(text, fontName, position, color, alignment, scale, autoScale, wrapWidth, renderLit, worldLightValue, alpha, TextShadowMode.AUTO);
    }

    /**
     * Alpha- and shadow-aware text drawing variant.
     */
    public static void drawText(String text, String fontName, Point3D position, ColorRGB color, TextAlignment alignment, float scale, boolean autoScale, int wrapWidth, boolean renderLit, int worldLightValue, float alpha, TextShadowMode shadowMode) {
        if (!text.isEmpty()) {
            transformHelper.resetTransforms();
            transformHelper.applyTranslation(position);
            renderText(text, fontName, transformHelper, null, alignment, scale, autoScale, wrapWidth, true, color, renderLit, worldLightValue, alpha, shadowMode);
        }
    }

    /**
     * Similar to the 2D text drawing method, except this method will render the text according to the passed-in text JSON in 3D space at the point specified.
     * Essentially, this is JSON-defined rendering rather than manual entry of points.
     */
    public static void draw3DText(String text, AEntityD_Definable<?> entity, TransformationMatrix transform, JSONText definition, boolean pixelCoords, boolean renderLit) {
        draw3DText(text, entity, transform, definition, pixelCoords, renderLit, 1.0F, getTextShadowMode(definition));
    }

    /**
     * Shadow-aware variant of {@link #draw3DText(String, AEntityD_Definable, TransformationMatrix, JSONText, boolean, boolean)}.
     */
    public static void draw3DText(String text, AEntityD_Definable<?> entity, TransformationMatrix transform, JSONText definition, boolean pixelCoords, boolean renderLit, TextShadowMode shadowMode) {
        draw3DText(text, entity, transform, definition, pixelCoords, renderLit, 1.0F, shadowMode);
    }

    /**
     * Alpha-aware variant of {@link #draw3DText(String, AEntityD_Definable, TransformationMatrix, JSONText, boolean, boolean)}.
     */
    public static void draw3DText(String text, AEntityD_Definable<?> entity, TransformationMatrix transform, JSONText definition, boolean pixelCoords, boolean renderLit, float alpha) {
        draw3DText(text, entity, transform, definition, pixelCoords, renderLit, alpha, getTextShadowMode(definition));
    }

    /**
     * Alpha- and shadow-aware 3D text drawing variant.
     */
    public static void draw3DText(String text, AEntityD_Definable<?> entity, TransformationMatrix transform, JSONText definition, boolean pixelCoords, boolean renderLit, float alpha, TextShadowMode shadowMode) {
        if (!text.isEmpty()) {
            //Get the actual color we will need to render with based on JSON.
            ColorRGB color = entity.getTextColor(definition.inheritedColorIndex, definition.color);

            //Render the text.
            transformHelper.set(transform);
            transformHelper.applyTranslation(definition.pos);
            renderText(text, definition.fontName, transformHelper, definition.rot, TextAlignment.values()[definition.renderPosition], definition.scale, definition.autoScale, definition.wrapWidth, pixelCoords, color, renderLit, entity.worldLightValue, alpha, shadowMode);
        }
    }

    /**
     * Renders a JSON text definition directly on the player's screen.
     * GUI text uses screen-center-relative pixel coordinates, so Y is inverted before rendering.
     */
    public static void drawGUIText(String text, AEntityD_Definable<?> entity, JSONText definition, int screenWidth, int screenHeight, double zOffset, float alpha) {
        drawGUIText(text, entity, definition, screenWidth, screenHeight, zOffset, alpha, getTextShadowMode(definition));
    }

    /**
     * Shadow-aware GUI text drawing variant.
     */
    public static void drawGUIText(String text, AEntityD_Definable<?> entity, JSONText definition, int screenWidth, int screenHeight, double zOffset, float alpha, TextShadowMode shadowMode) {
        if (!text.isEmpty()) {
            ColorRGB color = entity.getTextColor(definition.inheritedColorIndex, definition.color);
            transformHelper.resetTransforms();
            transformHelper.applyTranslation(screenWidth / 2D, -screenHeight / 2D, zOffset);
            transformHelper.applyTranslation(definition.pos.x, -definition.pos.y, definition.pos.z);
            renderText(text, definition.fontName, transformHelper, definition.rot, TextAlignment.values()[definition.renderPosition], definition.scale, definition.autoScale, definition.wrapWidth, true, color, true, entity.worldLightValue, alpha, shadowMode);
        }
    }

    /**
     * Renders a texture from a JSON text definition directly on the player's screen.
     * The texture is centered on pos and starts at 16 by 16 screen pixels before scale is applied.
     */
    public static void drawGUITexture(String texture, JSONText definition, int screenWidth, int screenHeight, double zOffset, float alpha) {
        if (texture != null && !texture.isEmpty()) {
            transformHelper.resetTransforms();
            transformHelper.applyTranslation(screenWidth / 2D, -screenHeight / 2D, zOffset);
            transformHelper.applyTranslation(definition.pos.x, -definition.pos.y, definition.pos.z);
            if (definition.rot != null) {
                transformHelper.applyRotation(definition.rot);
            }
            transformHelper.applyScaling(definition.scale, definition.scale, definition.scale);

            guiTextureRenderable.setTexture(texture);
            guiTextureRenderable.setAlpha(alpha);
            guiTextureRenderable.transform.set(transformHelper);
            guiTextureRenderable.render();
        }
    }

    /**
     * Returns the width of the passed-in text as reported by Minecraft's selected font.
     * Units are unscaled font pixels.
     */
    public static float getStringWidth(String text, String fontName) {
        return InterfaceManager.renderingInterface.getStringWidth(text, fontName);
    }

    /**
     * Returns the height of the number of lines of text.  Units are in pixels,
     * using Minecraft's standard nine-pixel line height.
     */
    public static float getHeight(int numberLines) {
        return numberLines * LINE_HEIGHT;
    }

    /**
     * Returns the exact number of lines produced by word wrapping at the requested unscaled font width.
     */
    public static int getLineCount(String text, String fontName, float wrapWidth) {
        return wrapText(text, fontName, wrapWidth).size();
    }

    /**
     * Returns the widest line produced by word wrapping at the requested unscaled font width.
     */
    public static float getWrappedWidth(String text, String fontName, float wrapWidth) {
        return getWidestLine(wrapText(text, fontName, wrapWidth), fontName);
    }

    /**
     * Performs layout in MTS coordinates, then delegates each line to Minecraft's native font renderer.
     */
    private static void renderText(String text, String fontName, TransformationMatrix transform, RotationMatrix rotation, TextAlignment alignment, float scale, boolean autoScale, int wrapWidth, boolean pixelCoords, ColorRGB color, boolean renderLit, int worldLightValue, float alpha, TextShadowMode shadowMode) {
        if (alpha <= 0.0F || scale == 0.0F) {
            return;
        }

        List<String> lines;
        float verticalOffset = 0.0F;
        if (autoScale && wrapWidth > 0) {
            lines = wrapText(text, fontName, 0.0F);
            float widestLine = getWidestLine(lines, fontName);
            float scaledWidth = widestLine * Math.abs(scale);
            if (scaledWidth > wrapWidth && widestLine > 0.0F) {
                float scaleFactor = wrapWidth / scaledWidth;
                scale *= scaleFactor;
                verticalOffset = FONT_HEIGHT / 2.0F * (1.0F - scaleFactor);
            }
        } else {
            float unscaledWrapWidth = wrapWidth > 0 ? wrapWidth / Math.abs(scale) : 0.0F;
            lines = wrapText(text, fontName, unscaledWrapWidth);
        }

        if (!pixelCoords) {
            scale /= 16.0F;
        }

        int alphaByte = Math.min(255, Math.max(4, Math.round(alpha * 255.0F)));
        int packedColor = (alphaByte << 24) | (color.rgbInt & 0xFFFFFF);
        textTransformHelper.set(transform);
        if (rotation != null) {
            textTransformHelper.applyRotation(rotation);
        }
        //Minecraft's Font draws down in +Y, while all existing MTS text coordinates draw down in -Y.
        textTransformHelper.applyScaling(scale, -scale, scale);

        for (int lineIndex = 0; lineIndex < lines.size(); ++lineIndex) {
            String line = lines.get(lineIndex);
            if (line.isEmpty()) {
                continue;
            }

            float lineWidth = getStringWidth(line, fontName);
            float horizontalOffset;
            switch (alignment) {
                case CENTERED:
                    horizontalOffset = -lineWidth / 2.0F;
                    break;
                case RIGHT_ALIGNED:
                    horizontalOffset = -lineWidth;
                    break;
                default:
                    horizontalOffset = 0.0F;
                    break;
            }

            lineTransformHelper.set(textTransformHelper);
            lineTransformHelper.applyTranslation(horizontalOffset, verticalOffset + lineIndex * LINE_HEIGHT, 0.0F);
            InterfaceManager.renderingInterface.renderText(line, fontName, lineTransformHelper, packedColor, renderLit, worldLightValue, shouldRenderShadow(line, color, shadowMode));
        }
    }

    private static TextShadowMode getTextShadowMode(JSONText definition) {
        return definition.shadowMode != null ? definition.shadowMode : TextShadowMode.AUTO;
    }

    /**
     * Minecraft applies one shadow flag to a complete draw call.  AUTO therefore disables the shadow for a line
     * if any visible section uses a color too dark to remain distinct from Minecraft's darkened shadow color.
     */
    private static boolean shouldRenderShadow(String text, ColorRGB defaultColor, TextShadowMode shadowMode) {
        if (shadowMode == TextShadowMode.ALWAYS) {
            return true;
        } else if (shadowMode == TextShadowMode.NEVER) {
            return false;
        }

        int packedDefaultColor = defaultColor.rgbInt;
        float defaultRed = ((packedDefaultColor >> 16) & 0xFF) / 255.0F;
        float defaultGreen = ((packedDefaultColor >> 8) & 0xFF) / 255.0F;
        float defaultBlue = (packedDefaultColor & 0xFF) / 255.0F;
        float currentRed = defaultRed;
        float currentGreen = defaultGreen;
        float currentBlue = defaultBlue;
        for (int index = 0; index < text.length();) {
            if (text.charAt(index) == FORMATTING_CHAR && index + 1 < text.length()) {
                char formattingCode = Character.toLowerCase(text.charAt(index + 1));
                int colorIndex = "0123456789abcdef".indexOf(formattingCode);
                if (colorIndex >= 0) {
                    int formattedColor = FORMATTING_COLORS[colorIndex];
                    currentRed = ((formattedColor >> 16) & 0xFF) / 255.0F;
                    currentGreen = ((formattedColor >> 8) & 0xFF) / 255.0F;
                    currentBlue = (formattedColor & 0xFF) / 255.0F;
                } else if (formattingCode == RESET_FORMATTING_CHAR) {
                    currentRed = defaultRed;
                    currentGreen = defaultGreen;
                    currentBlue = defaultBlue;
                }
                index += 2;
                continue;
            }

            int codePoint = text.codePointAt(index);
            if (!Character.isWhitespace(codePoint) && getPerceivedBrightness(currentRed, currentGreen, currentBlue) < AUTO_SHADOW_MINIMUM_BRIGHTNESS) {
                return false;
            }
            index += Character.charCount(codePoint);
        }
        return true;
    }

    private static float getPerceivedBrightness(float red, float green, float blue) {
        return 0.2126F * red + 0.7152F * green + 0.0722F * blue;
    }

    private static float getWidestLine(List<String> lines, String fontName) {
        float widestLine = 0.0F;
        for (String line : lines) {
            widestLine = Math.max(widestLine, getStringWidth(line, fontName));
        }
        return widestLine;
    }

    /**
     * Splits text using native Minecraft font metrics while retaining active legacy formatting across lines.
     * A zero max width only honors explicit newline characters.
     */
    private static List<String> wrapText(String text, String fontName, float maxWidth) {
        List<String> lines = new ArrayList<>();
        FormattingState formatting = new FormattingState();
        String remainingText = text;

        while (true) {
            int newlineIndex = remainingText.indexOf('\n');
            int paragraphEnd = newlineIndex >= 0 ? newlineIndex : remainingText.length();
            String paragraph = remainingText.substring(0, paragraphEnd);
            if (maxWidth <= 0.0F || getStringWidth(paragraph, fontName) <= maxWidth) {
                lines.add(paragraph);
                formatting.apply(paragraph);
                if (newlineIndex < 0) {
                    break;
                }
                remainingText = formatting.getPrefix() + remainingText.substring(newlineIndex + 1);
            } else {
                TextBreak textBreak = findTextBreak(remainingText, paragraphEnd, fontName, maxWidth);
                lines.add(remainingText.substring(0, textBreak.lineEnd));
                formatting.apply(remainingText.substring(0, textBreak.nextStart));
                if (textBreak.nextStart == paragraphEnd) {
                    if (newlineIndex < 0) {
                        break;
                    }
                    remainingText = formatting.getPrefix() + remainingText.substring(newlineIndex + 1);
                } else {
                    remainingText = formatting.getPrefix() + remainingText.substring(textBreak.nextStart);
                }
            }
        }
        return lines;
    }

    /**
     * Finds a word boundary that fits the requested width, falling back to a character boundary for long words.
     */
    private static TextBreak findTextBreak(String text, int paragraphEnd, String fontName, float maxWidth) {
        int index = 0;
        int lastFittingIndex = 0;
        int lastSpaceIndex = -1;
        int indexAfterLastSpace = -1;
        boolean hasVisibleCharacter = false;
        boolean inWhitespaceRun = false;

        while (index < paragraphEnd) {
            int tokenStart = index;
            char currentChar = text.charAt(index);
            if (currentChar == FORMATTING_CHAR && index + 1 < paragraphEnd) {
                index += 2;
                lastFittingIndex = index;
                continue;
            }

            int codePoint = text.codePointAt(index);
            int tokenEnd = index + Character.charCount(codePoint);
            if (getStringWidth(text.substring(0, tokenEnd), fontName) > maxWidth) {
                if (Character.isWhitespace(codePoint)) {
                    int lineEnd = inWhitespaceRun && lastSpaceIndex >= 0 ? lastSpaceIndex : tokenStart;
                    return new TextBreak(lineEnd, findAfterWhitespaceRun(text, tokenEnd, paragraphEnd));
                }
                if (lastSpaceIndex >= 0) {
                    return new TextBreak(lastSpaceIndex, indexAfterLastSpace);
                }
                if (hasVisibleCharacter) {
                    return new TextBreak(lastFittingIndex, lastFittingIndex);
                }
                return new TextBreak(tokenEnd, tokenEnd);
            }

            if (Character.isWhitespace(codePoint)) {
                if (hasVisibleCharacter) {
                    if (!inWhitespaceRun) {
                        lastSpaceIndex = tokenStart;
                    }
                    indexAfterLastSpace = tokenEnd;
                    inWhitespaceRun = true;
                }
            } else {
                hasVisibleCharacter = true;
                inWhitespaceRun = false;
            }
            lastFittingIndex = tokenEnd;
            index = tokenEnd;
        }
        return new TextBreak(paragraphEnd, paragraphEnd);
    }

    /**
     * Skips the remainder of whitespace at a wrap boundary, retaining intervening formatting through the next-line prefix.
     */
    private static int findAfterWhitespaceRun(String text, int index, int paragraphEnd) {
        while (index < paragraphEnd) {
            if (text.charAt(index) == FORMATTING_CHAR && index + 1 < paragraphEnd) {
                index += 2;
                continue;
            }
            int codePoint = text.codePointAt(index);
            if (!Character.isWhitespace(codePoint)) {
                break;
            }
            index += Character.charCount(codePoint);
        }
        return index;
    }

    private static class TextBreak {
        private final int lineEnd;
        private final int nextStart;

        private TextBreak(int lineEnd, int nextStart) {
            this.lineEnd = lineEnd;
            this.nextStart = nextStart;
        }
    }

    /**
     * Tracks the native legacy formatting state so wrapping and explicit newlines don't reset styles.
     */
    private static class FormattingState {
        private char colorCode;
        private boolean obfuscated;
        private boolean bold;
        private boolean strikethrough;
        private boolean underlined;
        private boolean italic;

        private void apply(String text) {
            for (int i = 0; i + 1 < text.length(); ++i) {
                if (text.charAt(i) != FORMATTING_CHAR) {
                    continue;
                }

                char code = Character.toLowerCase(text.charAt(++i));
                if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f') {
                    colorCode = code;
                    clearStyles();
                } else {
                    switch (code) {
                        case RANDOM_FORMATTING_CHAR:
                            obfuscated = true;
                            break;
                        case BOLD_FORMATTING_CHAR:
                            bold = true;
                            break;
                        case STRIKETHROUGH_FORMATTING_CHAR:
                            strikethrough = true;
                            break;
                        case UNDERLINE_FORMATTING_CHAR:
                            underlined = true;
                            break;
                        case ITALIC_FORMATTING_CHAR:
                            italic = true;
                            break;
                        case RESET_FORMATTING_CHAR:
                            colorCode = 0;
                            clearStyles();
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private void clearStyles() {
            obfuscated = false;
            bold = false;
            strikethrough = false;
            underlined = false;
            italic = false;
        }

        private String getPrefix() {
            StringBuilder prefix = new StringBuilder();
            if (colorCode != 0) {
                prefix.append(FORMATTING_CHAR).append(colorCode);
            }
            if (obfuscated) {
                prefix.append(FORMATTING_CHAR).append(RANDOM_FORMATTING_CHAR);
            }
            if (bold) {
                prefix.append(FORMATTING_CHAR).append(BOLD_FORMATTING_CHAR);
            }
            if (strikethrough) {
                prefix.append(FORMATTING_CHAR).append(STRIKETHROUGH_FORMATTING_CHAR);
            }
            if (underlined) {
                prefix.append(FORMATTING_CHAR).append(UNDERLINE_FORMATTING_CHAR);
            }
            if (italic) {
                prefix.append(FORMATTING_CHAR).append(ITALIC_FORMATTING_CHAR);
            }
            return prefix.toString();
        }
    }

    /**
     * List of enums that define how text is rendered.
     */
    public enum TextAlignment {
        CENTERED,
        LEFT_ALIGNED,
        RIGHT_ALIGNED
    }

    /**
     * Controls use of Minecraft's native one-pixel text shadow.
     */
    public enum TextShadowMode {
        AUTO,
        ALWAYS,
        NEVER
    }
}
