package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.rendering.AModelParser;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Custom component that renders a vehicle and all its attached parts from a top-down view.
 * Renders a white 2px outline around the model silhouette, with a semi-transparent color fill
 * inside that changes based on damage state. When undamaged, the interior is fully transparent
 * (only the outline is visible). As damage increases, the interior fills with color.
 * <br><br>
 * The outline is achieved by rendering the model at a slightly larger scale with the outline color,
 * then rendering the model at normal scale with the panel background color to mask the interior,
 * and finally rendering a semi-transparent damage color fill if damaged.
 *
 * @author don_bruce
 */
public class GUIComponentVehicleDamageModel extends AGUIComponent {
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTEX_X = 5;
    private static final int VERTEX_Y = 6;
    private static final int VERTEX_Z = 7;

    private static final RotationMatrix TOP_DOWN_ROTATION = new RotationMatrix().setToAxisAngle(1, 0, 0, -90);
    private static final ColorRGB OUTLINE_COLOR = ColorRGB.WHITE;
    private static final ColorRGB PANEL_BG_COLOR = new ColorRGB(30, 30, 35);

    /**
     * Texture used for solid-color rendering (white pixel block from damage_panel.png).
     */
    private static final String SOLID_TEXTURE = "mts:textures/guis/damage_panel.png";

    private static final Map<String, RenderableData> outlineCache = new HashMap<>();
    private static final Map<String, RenderableData> maskCache = new HashMap<>();
    private static final Map<String, RenderableData> fillCache = new HashMap<>();
    private static final Map<String, Float> scaleCache = new HashMap<>();

    public final float scaleFactor;
    private final EntityVehicleF_Physics vehicle;
    private float damagePercent = 0;
    private ColorRGB damageColor = new ColorRGB(0, 200, 0);

    public GUIComponentVehicleDamageModel(int x, int y, float scaleFactor, EntityVehicleF_Physics vehicle) {
        super(x, y, 0, 0);
        this.scaleFactor = scaleFactor;
        this.vehicle = vehicle;
    }

    @Override
    public int getZOffset() {
        return MODEL_DEFAULT_ZOFFSET;
    }

    /**
     * Sets the overall damage percentage (0.0 = healthy, 1.0 = destroyed).
     * This controls the semi-transparent fill inside the outline.
     */
    public void setDamagePercent(float percent) {
        this.damagePercent = Math.max(0, Math.min(1, percent));
    }

    /**
     * Sets the color used for the damage fill.
     */
    public void setDamageColor(ColorRGB color) {
        this.damageColor = color;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (blendingEnabled) {
            //Render the translucent damage fill on the blending pass.
            if (damagePercent > 0.01F) {
                String cacheKey = buildCacheKey();
                RenderableData fillRenderable = fillCache.get(cacheKey);
                if (fillRenderable != null) {
                    float baseScale = scaleCache.getOrDefault(cacheKey, 1.0F) * scaleFactor;
                    fillRenderable.transform.resetTransforms();
                    fillRenderable.transform.setTranslation(position);
                    fillRenderable.transform.applyRotation(TOP_DOWN_ROTATION);
                    fillRenderable.transform.applyScaling(baseScale, baseScale, baseScale);
                    fillRenderable.setColor(damageColor);
                    fillRenderable.setAlpha(damagePercent * 0.6F);
                    fillRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
                    fillRenderable.render();
                }
            }
            return;
        }

        //Non-blending pass: build cache if needed, then render outline + mask.
        String cacheKey = buildCacheKey();
        if (!outlineCache.containsKey(cacheKey)) {
            buildCombinedModel(cacheKey);
        }

        RenderableData outlineRenderable = outlineCache.get(cacheKey);
        RenderableData maskRenderable = maskCache.get(cacheKey);
        if (outlineRenderable == null || maskRenderable == null) {
            return;
        }

        float baseScale = scaleCache.getOrDefault(cacheKey, 1.0F) * scaleFactor;
        //The outline scale adds ~2px border around the model.
        //For a model that renders at ~scaleFactor pixels, 2px on each side = 4px extra.
        float outlineExtra = 4.0F / (scaleFactor > 0 ? scaleFactor : 48.0F);
        float outlineScale = baseScale * (1.0F + outlineExtra);

        //Pass 1: Render the outline (slightly enlarged model with white color).
        outlineRenderable.transform.resetTransforms();
        outlineRenderable.transform.setTranslation(position.x, position.y, position.z - 1);
        outlineRenderable.transform.applyRotation(TOP_DOWN_ROTATION);
        outlineRenderable.transform.applyScaling(outlineScale, outlineScale, outlineScale);
        outlineRenderable.setColor(OUTLINE_COLOR);
        outlineRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        outlineRenderable.render();

        //Pass 2: Render the mask (normal-size model with dark background color to create outline effect).
        maskRenderable.transform.resetTransforms();
        maskRenderable.transform.setTranslation(position);
        maskRenderable.transform.applyRotation(TOP_DOWN_ROTATION);
        maskRenderable.transform.applyScaling(baseScale, baseScale, baseScale);
        maskRenderable.setColor(PANEL_BG_COLOR);
        maskRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        maskRenderable.render();
    }

    /**
     * Builds a cache key that uniquely identifies this vehicle + its current parts configuration.
     */
    private String buildCacheKey() {
        StringBuilder key = new StringBuilder();
        key.append(vehicle.definition.getModelLocation(vehicle.subDefinition));
        for (APart part : vehicle.allParts) {
            key.append("|").append(part.definition.getModelLocation(part.subDefinition));
            key.append("@").append(String.format("%.2f,%.2f,%.2f", part.localOffset.x, part.localOffset.y, part.localOffset.z));
        }
        return key.toString();
    }

    /**
     * Parses the vehicle model and all part models, combines them into single vertex buffers
     * with parts positioned at their correct offsets, and caches the result.
     */
    private void buildCombinedModel(String cacheKey) {
        //Parse vehicle model.
        String vehicleModelLoc = vehicle.definition.getModelLocation(vehicle.subDefinition);
        List<RenderableVertices> vehicleObjects = AModelParser.parseModel(vehicleModelLoc, true);

        //Calculate total vertices needed (vehicle + all parts).
        int totalFloats = 0;
        for (RenderableVertices obj : vehicleObjects) {
            if (!shouldSkipObject(obj)) {
                totalFloats += obj.vertices.capacity();
            }
        }

        //Parse each part's model and accumulate vertex count.
        int[][] partVertexRanges = new int[vehicle.allParts.size()][2];
        @SuppressWarnings("unchecked")
        List<RenderableVertices>[] partObjectsArray = new List[vehicle.allParts.size()];
        for (int p = 0; p < vehicle.allParts.size(); p++) {
            APart part = vehicle.allParts.get(p);
            String partModelLoc = part.definition.getModelLocation(part.subDefinition);
            List<RenderableVertices> partObjects = AModelParser.parseModel(partModelLoc, true);
            partObjectsArray[p] = partObjects;
            partVertexRanges[p][0] = totalFloats;
            for (RenderableVertices obj : partObjects) {
                if (!shouldSkipObject(obj)) {
                    totalFloats += obj.vertices.capacity();
                }
            }
            partVertexRanges[p][1] = totalFloats;
        }

        //Combine all vertices into one buffer.
        FloatBuffer combinedBuffer = FloatBuffer.allocate(totalFloats);

        //Add vehicle vertices (no offset).
        float minX = 999, maxX = -999, minY = 999, maxY = -999, minZ = 999, maxZ = -999;
        for (RenderableVertices obj : vehicleObjects) {
            if (!shouldSkipObject(obj)) {
                for (int i = 0; i < obj.vertices.capacity(); i += FLOATS_PER_VERTEX) {
                    float vx = obj.vertices.get(i + VERTEX_X);
                    float vy = obj.vertices.get(i + VERTEX_Y);
                    float vz = obj.vertices.get(i + VERTEX_Z);
                    minX = Math.min(minX, vx); maxX = Math.max(maxX, vx);
                    minY = Math.min(minY, vy); maxY = Math.max(maxY, vy);
                    minZ = Math.min(minZ, vz); maxZ = Math.max(maxZ, vz);
                }
                obj.vertices.rewind();
                combinedBuffer.put(obj.vertices);
                obj.vertices.rewind();
            }
        }

        //Add part vertices with position offsets.
        for (int p = 0; p < vehicle.allParts.size(); p++) {
            APart part = vehicle.allParts.get(p);
            Point3D offset = part.localOffset;
            List<RenderableVertices> partObjects = partObjectsArray[p];

            for (RenderableVertices obj : partObjects) {
                if (!shouldSkipObject(obj)) {
                    for (int i = 0; i < obj.vertices.capacity(); i += FLOATS_PER_VERTEX) {
                        //Copy vertex data, offsetting position by part's local offset.
                        for (int j = 0; j < FLOATS_PER_VERTEX; j++) {
                            float val = obj.vertices.get(i + j);
                            if (j == VERTEX_X) val += (float) offset.x;
                            else if (j == VERTEX_Y) val += (float) offset.y;
                            else if (j == VERTEX_Z) val += (float) offset.z;
                            combinedBuffer.put(val);
                        }
                        //Track bounds including parts.
                        float vx = obj.vertices.get(i + VERTEX_X) + (float) offset.x;
                        float vy = obj.vertices.get(i + VERTEX_Y) + (float) offset.y;
                        float vz = obj.vertices.get(i + VERTEX_Z) + (float) offset.z;
                        minX = Math.min(minX, vx); maxX = Math.max(maxX, vx);
                        minY = Math.min(minY, vy); maxY = Math.max(maxY, vy);
                        minZ = Math.min(minZ, vz); maxZ = Math.max(maxZ, vz);
                    }
                    obj.vertices.rewind();
                }
            }
        }
        combinedBuffer.flip();

        //Calculate scale factor to fit the model in the GUI.
        float globalMax = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
        float modelScale = globalMax > 1.5F ? 1.5F / globalMax : 1.0F;
        scaleCache.put(cacheKey, modelScale);

        //Create three renderables from the same vertex data (outline, mask, fill).
        //All use the SOLID_TEXTURE for solid-color rendering via color tinting.
        FloatBuffer outlineBuf = FloatBuffer.allocate(combinedBuffer.limit());
        FloatBuffer maskBuf = FloatBuffer.allocate(combinedBuffer.limit());
        FloatBuffer fillBuf = FloatBuffer.allocate(combinedBuffer.limit());

        //Copy and remap UVs to the white pixel block in damage_panel.png (240/256, 240/256 to 256/256, 256/256).
        float solidU = 248 / 256.0F;
        float solidV = 248 / 256.0F;
        combinedBuffer.rewind();
        while (combinedBuffer.hasRemaining()) {
            for (int j = 0; j < FLOATS_PER_VERTEX; j++) {
                float val = combinedBuffer.get();
                float outVal = val, maskVal = val, fillVal = val;
                //Remap UV coordinates to the white pixel block for solid color rendering.
                if (j == 3 || j == 4) { // U or V
                    outVal = solidU;
                    maskVal = solidU;
                    fillVal = solidU;
                }
                outlineBuf.put(outVal);
                maskBuf.put(maskVal);
                fillBuf.put(fillVal);
            }
        }
        outlineBuf.flip();
        maskBuf.flip();
        fillBuf.flip();

        RenderableData outlineRenderable = new RenderableData(
                new RenderableVertices("DMG_OUTLINE", outlineBuf, true), SOLID_TEXTURE);
        RenderableData maskRenderable = new RenderableData(
                new RenderableVertices("DMG_MASK", maskBuf, true), SOLID_TEXTURE);
        RenderableData fillRenderable = new RenderableData(
                new RenderableVertices("DMG_FILL_translucent", fillBuf, true), SOLID_TEXTURE);
        fillRenderable.setTransucentOverride();

        outlineCache.put(cacheKey, outlineRenderable);
        maskCache.put(cacheKey, maskRenderable);
        fillCache.put(cacheKey, fillRenderable);
    }

    private static boolean shouldSkipObject(RenderableVertices obj) {
        String name = obj.name.toLowerCase(Locale.ROOT);
        return name.contains("window") || obj.name.startsWith("#");
    }

    /**
     * Clears the model caches.  Called when the GUI is closed.
     */
    public static void clearDamageModelCaches() {
        outlineCache.values().forEach(RenderableData::destroy);
        maskCache.values().forEach(RenderableData::destroy);
        fillCache.values().forEach(RenderableData::destroy);
        outlineCache.clear();
        maskCache.clear();
        fillCache.clear();
        scaleCache.clear();
    }
}
