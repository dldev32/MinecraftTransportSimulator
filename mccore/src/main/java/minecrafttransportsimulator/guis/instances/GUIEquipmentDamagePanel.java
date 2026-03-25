package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponent3DModel;
import minecrafttransportsimulator.guis.components.GUIComponentDamageBar;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentDamageRect;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * War Thunder-style equipment damage panel that displays vehicle and part health
 * as a visual diagram. Shows a 3D model of the vehicle with color-coded part
 * indicators based on their damage state. Positioned in the bottom-left corner
 * of the screen as a translucent overlay.
 *
 * @author don_bruce
 */
public class GUIEquipmentDamagePanel extends AGUIBase {
    private static final int PANEL_WIDTH = 120;
    private static final int PANEL_HEIGHT = 160;
    private static final int BAR_WIDTH = 50;
    private static final int BAR_HEIGHT = 5;
    private static final int ENTRY_HEIGHT = 10;
    private static final int MODEL_SIZE = 48;
    private static final int PADDING = 4;

    private static final ColorRGB COLOR_HEALTHY = new ColorRGB(0, 200, 0);
    private static final ColorRGB COLOR_DAMAGED = new ColorRGB(255, 200, 0);
    private static final ColorRGB COLOR_CRITICAL = new ColorRGB(255, 100, 0);
    private static final ColorRGB COLOR_DESTROYED = new ColorRGB(200, 0, 0);
    private static final ColorRGB PANEL_BG_COLOR = new ColorRGB(20, 25, 30);

    private final EntityVehicleF_Physics vehicle;
    private final PartSeat seat;

    private GUIComponentDamageRect panelBackground;
    private GUIComponentLabel titleLabel;
    private GUIComponentLabel hullHealthLabel;
    private GUIComponentDamageBar hullHealthBar;
    private GUIComponent3DModel vehicleModel;

    private final List<PartDamageEntry> partEntries = new ArrayList<>();

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

        //Semi-transparent dark background panel.
        addComponent(panelBackground = new GUIComponentDamageRect(panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BG_COLOR, 0.65F));
        panelBackground.ignoreGUILightingState = true;

        //Title label: vehicle name.
        addComponent(titleLabel = new GUIComponentLabel(panelLeft + PANEL_WIDTH / 2, panelTop + 3, ColorRGB.WHITE, "", TextAlignment.CENTERED, 0.75F));
        titleLabel.ignoreGUILightingState = true;

        //3D model of the vehicle (isometric top-down view).
        addComponent(vehicleModel = new GUIComponent3DModel(panelLeft + PANEL_WIDTH / 2, panelTop + 8 + MODEL_SIZE / 2, MODEL_SIZE, true, false, false));

        //Hull health label and bar below the model.
        int healthY = panelTop + 12 + MODEL_SIZE;
        addComponent(hullHealthLabel = new GUIComponentLabel(panelLeft + PADDING, healthY, ColorRGB.WHITE, "", TextAlignment.LEFT_ALIGNED, 0.6F));
        hullHealthLabel.ignoreGUILightingState = true;
        addComponent(hullHealthBar = new GUIComponentDamageBar(panelLeft + PANEL_WIDTH - BAR_WIDTH - PADDING, healthY, BAR_WIDTH, BAR_HEIGHT));

        //Part damage entries below the hull health.
        partEntries.clear();
        int currentY = healthY + ENTRY_HEIGHT;

        for (APart part : vehicle.allParts) {
            String partName = getPartDisplayName(part);
            if (partName != null && part.definition.general.health > 0) {
                GUIComponentLabel nameLabel = new GUIComponentLabel(panelLeft + PADDING, currentY, ColorRGB.WHITE, "", TextAlignment.LEFT_ALIGNED, 0.5F);
                nameLabel.ignoreGUILightingState = true;
                addComponent(nameLabel);

                GUIComponentDamageBar bar = new GUIComponentDamageBar(panelLeft + PANEL_WIDTH - BAR_WIDTH - PADDING, currentY, BAR_WIDTH, BAR_HEIGHT);
                addComponent(bar);

                partEntries.add(new PartDamageEntry(part, nameLabel, bar, partName));
                currentY += ENTRY_HEIGHT;
            }
        }
    }

    @Override
    public void setStates() {
        super.setStates();

        //Only show when riding in a vehicle and HUD is visible.
        boolean shouldShow = CameraSystem.customCameraOverlay == null
                && (seat.placementDefinition.isController || seat.canControlGuns)
                && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON
                    ? ConfigSystem.client.renderingSettings.renderHUD_1P.value
                    : ConfigSystem.client.renderingSettings.renderHUD_3P.value);

        panelBackground.visible = shouldShow;
        titleLabel.visible = shouldShow;
        vehicleModel.visible = shouldShow;
        hullHealthLabel.visible = shouldShow;
        hullHealthBar.visible = shouldShow;

        if (shouldShow) {
            //Update title.
            titleLabel.text = vehicle.definition.general.name != null ? vehicle.definition.general.name : "Vehicle";

            //Update 3D model.
            vehicleModel.modelLocation = vehicle.definition.getModelLocation(vehicle.subDefinition);
            vehicleModel.textureLocation = vehicle.getTexture();

            //Update hull health.
            int maxHealth = vehicle.definition.general.health;
            int currentHealth = maxHealth > 0 ? (int) Math.ceil(maxHealth - vehicle.damageVar.currentValue) : maxHealth;
            if (currentHealth < 0) currentHealth = 0;
            float healthPercent = maxHealth > 0 ? (float) currentHealth / maxHealth : 1.0F;
            hullHealthLabel.text = "Hull";
            hullHealthLabel.color = getDamageColor(healthPercent);
            hullHealthBar.setHealthPercent(healthPercent);

            //Update part entries.
            for (PartDamageEntry entry : partEntries) {
                entry.label.visible = shouldShow;
                entry.bar.visible = shouldShow;

                if (entry.part.isValid) {
                    int partMaxHealth = entry.part.definition.general.health;
                    int partCurrentHealth = partMaxHealth > 0 ? (int) Math.ceil(partMaxHealth - entry.part.damageVar.currentValue) : partMaxHealth;
                    if (partCurrentHealth < 0) partCurrentHealth = 0;
                    float partHealthPercent = partMaxHealth > 0 ? (float) partCurrentHealth / partMaxHealth : 1.0F;

                    entry.label.text = entry.displayName;
                    entry.label.color = getDamageColor(partHealthPercent);
                    entry.bar.setHealthPercent(partHealthPercent);
                } else {
                    entry.label.text = entry.displayName;
                    entry.label.color = COLOR_DESTROYED;
                    entry.bar.setHealthPercent(0);
                }
            }
        } else {
            for (PartDamageEntry entry : partEntries) {
                entry.label.visible = false;
                entry.bar.visible = false;
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
        return screenWidth;
    }

    @Override
    public int getHeight() {
        return screenHeight;
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
        return GUIComponentDamageBar.DAMAGE_PANEL_TEXTURE;
    }

    /**
     * Returns the display name for a part, or null if the part should not be shown.
     */
    private static String getPartDisplayName(APart part) {
        if (part instanceof PartEngine) {
            return "Engine";
        } else if (part instanceof PartGroundDevice) {
            PartGroundDevice ground = (PartGroundDevice) part;
            if (ground.definition.ground.isTread) {
                return part.localOffset.x > 0 ? "R.Track" : "L.Track";
            } else {
                return part.localOffset.x > 0 ? "R.Wheel" : "L.Wheel";
            }
        } else if (part instanceof PartGun) {
            return "Gun";
        } else if (part instanceof PartPropeller) {
            return "Propeller";
        }
        return null;
    }

    /**
     * Returns the appropriate color for a given health percentage.
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
     * Helper class for tracking part damage entries in the panel.
     */
    private static class PartDamageEntry {
        final APart part;
        final GUIComponentLabel label;
        final GUIComponentDamageBar bar;
        final String displayName;

        PartDamageEntry(APart part, GUIComponentLabel label, GUIComponentDamageBar bar, String displayName) {
            this.part = part;
            this.label = label;
            this.bar = bar;
            this.displayName = displayName;
        }
    }
}
