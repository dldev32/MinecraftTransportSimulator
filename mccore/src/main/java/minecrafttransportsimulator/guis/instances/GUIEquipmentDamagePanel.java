package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentIconCutout;
import minecrafttransportsimulator.guis.components.GUIComponentVehicleDamageModel;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * War Thunder-style equipment damage panel that displays vehicle and part health.
 * Shows a top-down 3D model of the vehicle with all attached parts, surrounded by
 * a white 2px outline that fills with semi-transparent color based on damage state.
 * Part type icons from the panel texture are displayed at the top, changing from
 * white to red when the corresponding part is destroyed (0 HP).
 *
 * @author don_bruce
 */
public class GUIEquipmentDamagePanel extends AGUIBase {
    private static final String PANEL_TEXTURE = "mts:textures/guis/damage_panel.png";

    //Panel layout constants matching the texture.
    private static final int PANEL_WIDTH = 140;
    private static final int PANEL_HEIGHT = 160;
    private static final int ICON_CELL_W = 20;
    private static final int ICON_CELL_H = 16;
    private static final int ICON_CELL_Y = 2;
    private static final int ICON_CELL_GAP = 2;
    private static final int ICON_CELL_START_X = 4;
    private static final int MODEL_AREA_Y = 21;
    private static final int MODEL_AREA_H = 110;
    private static final int PADDING = 4;

    //Texture UV for icon cells (each 20x16 in the 256x256 texture).
    //Icons at top row: engine(0), tracks(1), turret(2), gun(3), fuel(4), propeller(5)
    private static final int ICON_COUNT = 6;
    private static final int ICON_ENGINE = 0;
    private static final int ICON_TRACKS = 1;
    private static final int ICON_TURRET = 2;
    private static final int ICON_GUN = 3;
    private static final int ICON_FUEL = 4;
    private static final int ICON_PROPELLER = 5;

    private static final ColorRGB COLOR_HEALTHY = new ColorRGB(0, 200, 0);
    private static final ColorRGB COLOR_DAMAGED = new ColorRGB(255, 200, 0);
    private static final ColorRGB COLOR_CRITICAL = new ColorRGB(255, 100, 0);
    private static final ColorRGB COLOR_DESTROYED = new ColorRGB(200, 0, 0);

    private final EntityVehicleF_Physics vehicle;
    private final PartSeat seat;

    //Background panel cutout from texture.
    private GUIComponentCutout panelBackground;

    //Icon cutouts from texture (one per icon type, with color tinting support).
    private final GUIComponentIconCutout[] iconCutouts = new GUIComponentIconCutout[ICON_COUNT];
    //Track which icons are active (have at least one part of that type).
    private final boolean[] iconActive = new boolean[ICON_COUNT];
    //Track if any part of that type is destroyed.
    private final boolean[] iconDestroyed = new boolean[ICON_COUNT];

    //Vehicle damage model component.
    private GUIComponentVehicleDamageModel vehicleDamageModel;

    //Part tracking for damage state.
    private final List<PartDamageInfo> trackedParts = new ArrayList<>();

    public GUIEquipmentDamagePanel(EntityVehicleF_Physics vehicle, PartSeat seat) {
        super();
        this.vehicle = vehicle;
        this.seat = seat;
    }

    @Override
    public void setupComponents() {
        super.setupComponents();

        int panelLeft = PADDING;
        int panelTop = screenHeight - PANEL_HEIGHT - PADDING;

        //Background panel cutout (the entire 140x160 dark area from the texture).
        addComponent(panelBackground = new GUIComponentCutout(this, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT, 0, 0, PANEL_WIDTH, PANEL_HEIGHT));
        panelBackground.ignoreGUILightingState = true;

        //Create icon cutouts from the texture (tintable for damage state).
        for (int i = 0; i < ICON_COUNT; i++) {
            int texX = ICON_CELL_START_X + i * (ICON_CELL_W + ICON_CELL_GAP);
            int texY = ICON_CELL_Y;
            int screenX = panelLeft + ICON_CELL_START_X + i * (ICON_CELL_W + ICON_CELL_GAP);
            int screenY = panelTop + ICON_CELL_Y;
            iconCutouts[i] = new GUIComponentIconCutout(this, screenX, screenY, ICON_CELL_W, ICON_CELL_H, texX, texY, ICON_CELL_W, ICON_CELL_H);
            iconCutouts[i].ignoreGUILightingState = true;
            addComponent(iconCutouts[i]);
        }

        //Vehicle damage model (top-down view, centered in the model area).
        int modelCenterX = panelLeft + PANEL_WIDTH / 2;
        int modelCenterY = panelTop + MODEL_AREA_Y + MODEL_AREA_H / 2;
        addComponent(vehicleDamageModel = new GUIComponentVehicleDamageModel(modelCenterX, modelCenterY, MODEL_AREA_H * 0.8F, vehicle));

        //Scan parts and determine which icon types are active.
        trackedParts.clear();
        for (int i = 0; i < ICON_COUNT; i++) {
            iconActive[i] = false;
            iconDestroyed[i] = false;
        }

        for (APart part : vehicle.allParts) {
            int iconIndex = getIconIndexForPart(part);
            if (iconIndex >= 0) {
                iconActive[iconIndex] = true;
                trackedParts.add(new PartDamageInfo(part, iconIndex));
            }
        }

        //Also consider the hull itself as "turret" icon.
        if (vehicle.definition.general.health > 0) {
            iconActive[ICON_TURRET] = true;
        }
    }

    @Override
    public void setStates() {
        super.setStates();

        boolean shouldShow = CameraSystem.customCameraOverlay == null
                && (seat.placementDefinition.isController || seat.canControlGuns)
                && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON
                    ? ConfigSystem.client.renderingSettings.renderHUD_1P.value
                    : ConfigSystem.client.renderingSettings.renderHUD_3P.value);

        panelBackground.visible = shouldShow;
        vehicleDamageModel.visible = shouldShow;
        for (int i = 0; i < ICON_COUNT; i++) {
            iconCutouts[i].visible = shouldShow && iconActive[i];
        }

        if (!shouldShow) {
            return;
        }

        //Calculate overall vehicle damage for the model fill.
        int maxHealth = vehicle.definition.general.health;
        float overallDamagePercent = 0;
        if (maxHealth > 0) {
            overallDamagePercent = (float) (vehicle.damageVar.currentValue / maxHealth);
        }
        vehicleDamageModel.setDamagePercent(overallDamagePercent);
        vehicleDamageModel.setDamageColor(getDamageColor(1.0F - overallDamagePercent));

        //Reset icon destroyed state.
        for (int i = 0; i < ICON_COUNT; i++) {
            iconDestroyed[i] = false;
        }

        //Check each tracked part for destruction.
        for (PartDamageInfo info : trackedParts) {
            if (!info.part.isValid || info.part.outOfHealth) {
                iconDestroyed[info.iconIndex] = true;
            }
        }

        //Check hull destruction for turret icon.
        if (vehicle.outOfHealth) {
            iconDestroyed[ICON_TURRET] = true;
        }

        //Update icon colors: white by default, red when destroyed.
        for (int i = 0; i < ICON_COUNT; i++) {
            if (iconActive[i]) {
                iconCutouts[i].tintColor = iconDestroyed[i] ? ColorRGB.RED : ColorRGB.WHITE;
            }
        }
    }

    @Override
    protected boolean renderBackground() {
        return false;
    }

    @Override
    public boolean capturesPlayer() {
        return false;
    }

    @Override
    protected boolean canStayOpen() {
        return super.canStayOpen() && seat.rider != null;
    }

    @Override
    public int getWidth() {
        return PANEL_WIDTH;
    }

    @Override
    public int getHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    public boolean renderFlushBottom() {
        return true;
    }

    @Override
    public boolean renderTranslucent() {
        return true;
    }

    @Override
    protected String getTexture() {
        return PANEL_TEXTURE;
    }

    @Override
    public void close() {
        super.close();
        GUIComponentVehicleDamageModel.clearDamageModelCaches();
    }

    /**
     * Returns the icon index for a given part type, or -1 if not shown.
     */
    private static int getIconIndexForPart(APart part) {
        if (part instanceof PartEngine) {
            return ICON_ENGINE;
        } else if (part instanceof PartGroundDevice) {
            return ICON_TRACKS;
        } else if (part instanceof PartGun) {
            return ICON_GUN;
        } else if (part instanceof PartPropeller) {
            return ICON_PROPELLER;
        } else if (part instanceof PartInteractable) {
            PartInteractable interactable = (PartInteractable) part;
            if (interactable.tank != null) {
                return ICON_FUEL;
            }
        }
        return -1;
    }

    /**
     * Returns the color for a given health percentage (1.0 = full, 0.0 = dead).
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

    /**
     * Tracks a part and its associated icon index.
     */
    private static class PartDamageInfo {
        final APart part;
        final int iconIndex;

        PartDamageInfo(APart part, int iconIndex) {
            this.part = part;
            this.iconIndex = iconIndex;
        }
    }
}
