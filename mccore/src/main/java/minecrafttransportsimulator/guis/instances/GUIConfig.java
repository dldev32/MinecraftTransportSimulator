package minecrafttransportsimulator.guis.instances;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONConfigEntry;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboardDynamic;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

public class GUIConfig extends AGUIBase {
    private static final float GUI_SCALE = 1.0F / 1.2F;
    private static final int CONFIG_GUI_WIDTH = scale(512);
    private static final int CONFIG_GUI_HEIGHT = scale(376);
    private static final int TAB_HEIGHT = scale(20);
    private static final int TAB_GAP = scale(2);
    private static final int TAB_WIDTH = (CONFIG_GUI_WIDTH - TAB_GAP * 3) / 4;
    private static final int CONTENT_TOP = scale(32);
    private static final int CONTENT_BOTTOM = CONFIG_GUI_HEIGHT - scale(14);
    private static final int LEFT_NAV_X = scale(16);
    private static final int LEFT_NAV_Y = scale(48);
    private static final int LEFT_NAV_WIDTH = scale(96);
    private static final int LEFT_NAV_BUTTON_HEIGHT = scale(18);
    private static final int LEFT_NAV_SPACING = scale(23);
    private static final int DIVIDER_X = scale(128);
    private static final int LIST_X_WITH_NAV = scale(146);
    private static final int LIST_X_FULL = scale(24);
    private static final int LIST_WIDTH_WITH_NAV = scale(338);
    private static final int LIST_WIDTH_FULL = scale(460);
    private static final int ROW_HEIGHT = scale(18);
    private static final int SETTING_ROWS_WITH_NAV = 17;
    private static final int SETTING_ROWS_FULL = 17;
    private static final int SCROLL_X = CONFIG_GUI_WIDTH - scale(18);
    private static final int SCROLL_TRACK_WIDTH = scale(4);
    private static final int SCROLL_THUMB_WIDTH = scale(8);
    private static final int SCROLL_THUMB_MIN_HEIGHT = scale(18);

    private static int scale(int value) {
        return Math.max(1, Math.round(value * GUI_SCALE));
    }

    private static final float PANEL_ALPHA = 0.60F;
    private static final float BUTTON_ALPHA = 0.60F;
    private static final ColorRGB COLOR_PANEL = new ColorRGB(0, 0, 0);
    private static final ColorRGB COLOR_BUTTON = new ColorRGB(0, 0, 0);
    private static final ColorRGB COLOR_BUTTON_HOVER = new ColorRGB(48, 48, 48);
    private static final ColorRGB COLOR_BUTTON_ACTIVE = new ColorRGB(22, 22, 22);
    private static final ColorRGB COLOR_TEXT = ColorRGB.WHITE;
    private static final ColorRGB COLOR_DIM_TEXT = new ColorRGB(165, 165, 165);
    private static final ColorRGB COLOR_CHANGED = ColorRGB.YELLOW;
    private static final ColorRGB COLOR_DIVIDER = ColorRGB.WHITE;
    private static final ColorRGB COLOR_OUTLINE = ColorRGB.WHITE;
    private static final ColorRGB COLOR_ROW_ALT = new ColorRGB(0, 0, 0);
    private static final ColorRGB COLOR_SCROLL_TRACK = new ColorRGB(90, 90, 90);
    private static final ColorRGB COLOR_SCROLL_THUMB = new ColorRGB(210, 210, 210);

    private final Map<MainPage, FlatButton> pageButtons = new LinkedHashMap<>();
    private final Map<CommonPage, TextButton> commonButtons = new LinkedHashMap<>();
    private final Map<ControlsPage, TextButton> controlsButtons = new LinkedHashMap<>();
    private final List<SettingRow> clientSettingRows = new ArrayList<>();
    private final List<SettingRow> serverSettingRows = new ArrayList<>();
    private final List<SettingRow> renderingSettingRows = new ArrayList<>();
    private final List<SettingRow> developmentSettingRows = new ArrayList<>();
    private final List<SolidRect> settingRowBackgrounds = new ArrayList<>();
    private final List<ControlListRow> keyboardRows = new ArrayList<>();
    private final List<GUIComponentButton> joystickSelectionButtons = new ArrayList<>();
    private final List<String> visibleJoystickNames = new ArrayList<>();
    private final List<GUIComponentButton> joystickComponentButtons = new ArrayList<>();
    private final List<TextLabel> joystickAssignmentLabels = new ArrayList<>();
    private final List<SolidRect> joystickStateBacks = new ArrayList<>();
    private final List<SolidRect> joystickStateFills = new ArrayList<>();
    private final List<Integer> visibleJoystickComponentIndexes = new ArrayList<>();
    private final List<ControlListRow> joystickDigitalAssignmentRows = new ArrayList<>();
    private final List<ControlListRow> joystickAnalogAssignmentRows = new ArrayList<>();
    private final Set<String> changedSettings = new HashSet<>();
    private final Set<String> changedKeyboardControls = new HashSet<>();
    private final Set<String> changedJoystickControls = new HashSet<>();

    private MainPage activePage = MainPage.COMMON;
    private CommonPage activeCommonPage = CommonPage.CLIENT;
    private ControlsPage activeControlsPage = ControlsPage.KEYBOARD;
    private int commonClientScroll;
    private int commonServerScroll;
    private int renderingScroll;
    private int developmentScroll;
    private int keyboardScroll;
    private int joystickSelectionScroll;
    private int joystickComponentScroll;
    private int joystickAssignmentScroll;

    private SolidRect mainPanel;
    private SolidRect sideDivider;
    private SolidRect pageScrollTrack;
    private SolidRect pageScrollThumb;
    private TextLabel noResultsLabel;
    private TextLabel joystickHeaderLabel;
    private TextButton joystickBackButton;
    private TextButton deadzoneDownButton;
    private TextButton deadzoneUpButton;
    private TextLabel deadzoneValueLabel;
    private TextLabel joystickColumnIndexLabel;
    private TextLabel joystickColumnNameLabel;
    private TextLabel joystickColumnStateLabel;
    private TextLabel joystickColumnAssignmentLabel;
    private TextLabel joystickAssignmentPromptLabel;
    private FlatButton clearAssignmentButton;
    private FlatButton cancelAssignmentButton;
    private FlatButton confirmBoundsButton;
    private FlatButton invertAxisButton;
    private GUIComponentTextBox axisMinBoundsTextBox;
    private GUIComponentTextBox axisMaxBoundsTextBox;
    private TextLabel joystickCalibrationLabel1;
    private TextLabel joystickCalibrationLabel2;
    private TextLabel controlSelectionFaultLabel;

    private int activeScrollTrackTop;
    private int activeScrollTrackHeight;
    private int activeScrollThumbHeight;
    private int activeScrollItemCount;
    private int activeScrollRowsToRender;
    private boolean scrollbarDragging;
    private int scrollbarDragOffset;

    private int selectedJoystickComponentCount;
    private String selectedJoystickName;
    private boolean assigningDigital;
    private int joystickComponentId = -1;
    private ControlsJoystick controlCalibrating;
    private boolean calibrating;

    public GUIConfig() {
        super();
        if (!InterfaceManager.inputInterface.isJoystickSupportEnabled()) {
            InterfaceManager.inputInterface.initJoysticks();
        }
    }

    @Override
    public void setupComponents() {
        components.clear();
        addComponent(mainPanel = new SolidRect(guiLeft, guiTop, CONFIG_GUI_WIDTH, CONFIG_GUI_HEIGHT, COLOR_PANEL, PANEL_ALPHA, 0.7F));
        addComponent(sideDivider = new SolidRect(guiLeft + DIVIDER_X, guiTop + CONTENT_TOP, 1, CONTENT_BOTTOM - CONTENT_TOP, COLOR_DIVIDER, 1.0F));
        addSettingRowBackgrounds();
        addPageButtons();
        addSubPageButtons();
        addScrollComponents();
        addSettingRows();
        addKeyboardRows();
        addJoystickComponents();
        addComponent(noResultsLabel = new TextLabel(guiLeft + LIST_X_FULL, guiTop + CONTENT_TOP + 60, LIST_WIDTH_FULL, 14, LanguageSystem.GUI_CONFIG_NO_RESULTS.getCurrentValue(), COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 1.0F));
    }

    private void addSettingRowBackgrounds() {
        settingRowBackgrounds.clear();
        for (int i = 0; i < SETTING_ROWS_FULL; ++i) {
            SolidRect background = new SolidRect(guiLeft + LIST_X_FULL, guiTop + CONTENT_TOP, LIST_WIDTH_FULL, ROW_HEIGHT - 2, COLOR_ROW_ALT, 0.35F);
            background.visible = false;
            settingRowBackgrounds.add(background);
            addComponent(background);
        }
    }

    @Override
    public void setStates() {
        if (!canStayOpen()) {
            close();
            return;
        }

        int wheelMovement = InterfaceManager.inputInterface.getTrackedMouseWheel();
        hideDynamicContent();
        updateNavigation();

        if (activePage == MainPage.COMMON) {
            updateCommonPage(wheelMovement);
        } else if (activePage == MainPage.RENDERING) {
            updateSettingList(renderingSettingRows, getAndSetRenderingScroll(wheelMovement, renderingSettingRows.size()), SETTING_ROWS_FULL, LIST_X_FULL, CONTENT_TOP + 10, LIST_WIDTH_FULL);
        } else if (activePage == MainPage.CONTROLS) {
            updateControlsPage(wheelMovement);
        } else if (activePage == MainPage.DEVELOPMENT) {
            updateDevelopmentPage(wheelMovement);
        }
    }

    @Override
    public boolean onClick(int mouseX, int mouseY) {
        if (pageScrollThumb.visible && pageScrollThumb.isMouseInBounds(mouseX, mouseY)) {
            scrollbarDragging = true;
            scrollbarDragOffset = mouseY - (int) -pageScrollThumb.position.y;
            return true;
        } else {
            boolean clicked = super.onClick(mouseX, mouseY);
            return clicked || editingText;
        }
    }

    @Override
    public void onRelease() {
        scrollbarDragging = false;
        super.onRelease();
    }

    @Override
    public boolean onMouseDragged(int mouseX, int mouseY) {
        if (scrollbarDragging) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return false;
    }

    private void addPageButtons() {
        int index = 0;
        for (MainPage page : MainPage.values()) {
            final MainPage pageForButton = page;
            FlatButton button = new FlatButton(guiLeft + (TAB_WIDTH + TAB_GAP) * index, guiTop - TAB_HEIGHT - TAB_GAP, TAB_WIDTH, TAB_HEIGHT, page.title.getCurrentValue(), true) {
                @Override
                public void onClicked(boolean leftSide) {
                    if (pageForButton != MainPage.DEVELOPMENT || !isDevelopmentLocked()) {
                        activePage = pageForButton;
                        clearTransientSelection();
                        clearTextFocus();
                    }
                }
            };
            pageButtons.put(page, button);
            addComponent(button);
            ++index;
        }
    }

    private void addSubPageButtons() {
        int index = 0;
        for (CommonPage page : CommonPage.values()) {
            final CommonPage pageForButton = page;
            TextButton button = new TextButton(guiLeft + LEFT_NAV_X, guiTop + LEFT_NAV_Y + LEFT_NAV_SPACING * index, LEFT_NAV_WIDTH, LEFT_NAV_BUTTON_HEIGHT, page.title.getCurrentValue(), true) {
                @Override
                public void onClicked(boolean leftSide) {
                    activeCommonPage = pageForButton;
                    clearTextFocus();
                }
            };
            commonButtons.put(page, button);
            addComponent(button);
            ++index;
        }

        index = 0;
        for (ControlsPage page : ControlsPage.values()) {
            final ControlsPage pageForButton = page;
            TextButton button = new TextButton(guiLeft + LEFT_NAV_X, guiTop + LEFT_NAV_Y + LEFT_NAV_SPACING * index, LEFT_NAV_WIDTH, LEFT_NAV_BUTTON_HEIGHT, page.title.getCurrentValue(), true) {
                @Override
                public void onClicked(boolean leftSide) {
                    activeControlsPage = pageForButton;
                    clearTransientSelection();
                    clearTextFocus();
                }
            };
            controlsButtons.put(page, button);
            addComponent(button);
            ++index;
        }
    }

    private void addScrollComponents() {
        addComponent(pageScrollTrack = new SolidRect(guiLeft + SCROLL_X, guiTop + CONTENT_TOP, SCROLL_TRACK_WIDTH, SCROLL_THUMB_MIN_HEIGHT, COLOR_PANEL, 0.0F));
        addComponent(pageScrollThumb = new SolidRect(guiLeft + SCROLL_X - 2, guiTop + CONTENT_TOP, SCROLL_THUMB_WIDTH, SCROLL_THUMB_MIN_HEIGHT, COLOR_SCROLL_THUMB, 0.9F) {
            @Override
            public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
                if (!blendingEnabled && scrollbarDragging) {
                    updateScrollFromMouse(mouseY);
                }
                super.render(gui, mouseX, mouseY, renderBright, renderLitTexture, blendingEnabled, partialTicks);
            }
        });
    }

    private void addSettingRows() {
        populateSettingRows(clientSettingRows, ConfigSystem.client.controlSettings, "client.control", false);
        populateSettingRows(renderingSettingRows, ConfigSystem.client.renderingSettings, "client.rendering", true);
        populateServerRows();
        developmentSettingRows.add(new SettingRow("server.general.devMode", "devMode", ConfigSystem.settings.general.devMode, LanguageSystem.GUI_CONFIG_SETTING_DEV_MODE, null, SettingType.BOOLEAN));
    }

    @SuppressWarnings("unchecked")
    private void populateSettingRows(List<SettingRow> rows, Object configObject, String idPrefix, boolean includeRenderingMode) {
        for (Field field : configObject.getClass().getFields()) {
            if (field.getType().equals(JSONConfigEntry.class)) {
                try {
                    JSONConfigEntry<?> entry = (JSONConfigEntry<?>) field.get(configObject);
                    if ("joystickDeadZone".equals(field.getName())) {
                        continue;
                    } else if (entry.value instanceof Boolean) {
                        rows.add(new SettingRow(idPrefix + "." + field.getName(), field.getName(), (JSONConfigEntry<Boolean>) entry, getSettingLabel(field.getName()), null, SettingType.BOOLEAN));
                    } else if (includeRenderingMode && "renderingMode".equals(field.getName())) {
                        rows.add(new SettingRow(idPrefix + "." + field.getName(), field.getName(), entry, getSettingLabel(field.getName()), null, SettingType.MODE));
                    } else if (entry.value instanceof Number) {
                        rows.add(new SettingRow(idPrefix + "." + field.getName(), field.getName(), entry, getSettingLabel(field.getName()), getNumericMetadata(field.getName()), SettingType.NUMBER));
                    }
                } catch (Exception e) {
                    //Skip only this row if reflection fails.  The rest of the GUI can still work.
                }
            }
        }
    }

    private void populateServerRows() {
        addServerRow("server.general.keyRequiredToStartVehicles", "keyRequiredToStartVehicles", ConfigSystem.settings.general.keyRequiredToStartVehicles, LanguageSystem.GUI_CONFIG_SETTING_KEY_REQUIRED);
        addServerRow("server.general.noclipVehicles", "noclipVehicles", ConfigSystem.settings.general.noclipVehicles, LanguageSystem.GUI_CONFIG_SETTING_NOCLIP_VEHICLES);
        addServerRow("server.general.chunkloadVehicles", "chunkloadVehicles", ConfigSystem.settings.general.chunkloadVehicles, LanguageSystem.GUI_CONFIG_SETTING_CHUNKLOAD_VEHICLES);
        addServerRow("server.general.giveManualsOnJoin", "giveManualsOnJoin", ConfigSystem.settings.general.giveManualsOnJoin, LanguageSystem.GUI_CONFIG_SETTING_GIVE_MANUALS);
        addServerRow("server.general.performModCompatFunctions", "performModCompatFunctions", ConfigSystem.settings.general.performModCompatFunctions, LanguageSystem.GUI_CONFIG_SETTING_MOD_COMPAT);
        addServerRow("server.general.aircraftSpeedFactor", "aircraftSpeedFactor", ConfigSystem.settings.general.aircraftSpeedFactor, LanguageSystem.GUI_CONFIG_SETTING_AIRCRAFT_SPEED_FACTOR);
        addServerRow("server.general.carSpeedFactor", "carSpeedFactor", ConfigSystem.settings.general.carSpeedFactor, LanguageSystem.GUI_CONFIG_SETTING_CAR_SPEED_FACTOR);
        addServerRow("server.general.fuelUsageFactor", "fuelUsageFactor", ConfigSystem.settings.general.fuelUsageFactor, LanguageSystem.GUI_CONFIG_SETTING_FUEL_USAGE_FACTOR);
        addServerRow("server.general.engineHoursFactor", "engineHoursFactor", ConfigSystem.settings.general.engineHoursFactor, LanguageSystem.GUI_CONFIG_SETTING_ENGINE_HOURS_FACTOR);
        addServerRow("server.general.gravityFactor", "gravityFactor", ConfigSystem.settings.general.gravityFactor, LanguageSystem.GUI_CONFIG_SETTING_GRAVITY_FACTOR);
        addServerRow("server.general.maxFlightHeight", "maxFlightHeight", ConfigSystem.settings.general.maxFlightHeight, LanguageSystem.GUI_CONFIG_SETTING_MAX_FLIGHT_HEIGHT);
        addServerRow("server.general.seaLevel", "seaLevel", ConfigSystem.settings.general.seaLevel, LanguageSystem.GUI_CONFIG_SETTING_SEA_LEVEL);
        addServerRow("server.damage.bulletBlockBreaking", "bulletBlockBreaking", ConfigSystem.settings.damage.bulletBlockBreaking, LanguageSystem.GUI_CONFIG_SETTING_BULLET_BLOCK_BREAKING);
        addServerRow("server.damage.bulletExplosions", "bulletExplosions", ConfigSystem.settings.damage.bulletExplosions, LanguageSystem.GUI_CONFIG_SETTING_BULLET_EXPLOSIONS);
        addServerRow("server.damage.vehicleBlockBreaking", "vehicleBlockBreaking", ConfigSystem.settings.damage.vehicleBlockBreaking, LanguageSystem.GUI_CONFIG_SETTING_VEHICLE_BLOCK_BREAKING);
        addServerRow("server.damage.vehicleDestruction", "vehicleDestruction", ConfigSystem.settings.damage.vehicleDestruction, LanguageSystem.GUI_CONFIG_SETTING_VEHICLE_DESTRUCTION);
        addServerRow("server.damage.vehicleExplosions", "vehicleExplosions", ConfigSystem.settings.damage.vehicleExplosions, LanguageSystem.GUI_CONFIG_SETTING_VEHICLE_EXPLOSIONS);
        addServerRow("server.damage.wheelBreakage", "wheelBreakage", ConfigSystem.settings.damage.wheelBreakage, LanguageSystem.GUI_CONFIG_SETTING_WHEEL_BREAKAGE);
        addServerRow("server.damage.crashDamageFactor", "crashDamageFactor", ConfigSystem.settings.damage.crashDamageFactor, LanguageSystem.GUI_CONFIG_SETTING_CRASH_DAMAGE_FACTOR);
        addServerRow("server.damage.bulletDamageFactor", "bulletDamageFactor", ConfigSystem.settings.damage.bulletDamageFactor, LanguageSystem.GUI_CONFIG_SETTING_BULLET_DAMAGE_FACTOR);
        addServerRow("server.damage.wheelDamageFactor", "wheelDamageFactor", ConfigSystem.settings.damage.wheelDamageFactor, LanguageSystem.GUI_CONFIG_SETTING_WHEEL_DAMAGE_FACTOR);
    }

    private void addServerRow(String id, String fieldName, JSONConfigEntry<?> entry, LanguageEntry label) {
        SettingType type = entry.value instanceof Boolean ? SettingType.BOOLEAN : SettingType.NUMBER;
        serverSettingRows.add(new SettingRow(id, fieldName, entry, label, type == SettingType.NUMBER ? getNumericMetadata(fieldName) : null, type));
    }

    private void addKeyboardRows() {
        keyboardRows.clear();
        for (ControlGroup group : ControlGroup.values()) {
            keyboardRows.add(new ControlHeaderRow(group.title));
            for (ControlsKeyboard control : ControlsKeyboard.values()) {
                if (control.systemName.startsWith(group.prefix)) {
                    keyboardRows.add(new KeyboardBindingRow(control));
                }
            }
            for (ControlsKeyboardDynamic dynamicControl : ControlsKeyboardDynamic.values()) {
                if (dynamicControl.name().toLowerCase(Locale.ROOT).startsWith(group.prefix.toUpperCase(Locale.ROOT))) {
                    keyboardRows.add(new KeyboardDynamicRow(dynamicControl));
                }
            }
        }
    }

    private void addJoystickComponents() {
        addComponent(controlSelectionFaultLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 20, LIST_WIDTH_WITH_NAV, 80, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.8F));
        addComponent(joystickHeaderLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 2, LIST_WIDTH_WITH_NAV, 14, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
        addComponent(joystickBackButton = new TextButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 2, 56, 14, LanguageSystem.GUI_CONFIG_BACK.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                selectedJoystickName = null;
                selectedJoystickComponentCount = 0;
                joystickComponentId = -1;
                joystickComponentScroll = 0;
                joystickAssignmentScroll = 0;
                calibrating = false;
            }
        });
        addComponent(deadzoneDownButton = new TextButton(guiLeft + LIST_X_WITH_NAV + 70, guiTop + CONTENT_TOP + 2, 16, 14, "<") {
            @Override
            public void onClicked(boolean leftSide) {
                stepDeadzone(-1);
            }
        });
        addComponent(deadzoneValueLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + 90, guiTop + CONTENT_TOP + 2, 160, 14, "", COLOR_TEXT, TextAlignment.CENTERED, 0.85F));
        addComponent(deadzoneUpButton = new TextButton(guiLeft + LIST_X_WITH_NAV + 250, guiTop + CONTENT_TOP + 2, 16, 14, ">") {
            @Override
            public void onClicked(boolean leftSide) {
                stepDeadzone(1);
            }
        });
        addComponent(joystickColumnIndexLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 28, 24, 14, "#", COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F));
        addComponent(joystickColumnNameLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + 28, guiTop + CONTENT_TOP + 28, 118, 14, LanguageSystem.GUI_CONFIG_JOYSTICK_NAME.getCurrentValue(), COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F));
        addComponent(joystickColumnStateLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + 150, guiTop + CONTENT_TOP + 28, 60, 14, LanguageSystem.GUI_CONFIG_JOYSTICK_STATE.getCurrentValue(), COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F));
        addComponent(joystickColumnAssignmentLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + 216, guiTop + CONTENT_TOP + 28, 120, 14, LanguageSystem.GUI_CONFIG_JOYSTICK_ASSIGNMENT.getCurrentValue(), COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F));

        for (int i = 0; i < SETTING_ROWS_WITH_NAV; ++i) {
            GUIComponentButton deviceButton = new TextButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 24 + ROW_HEIGHT * i, LIST_WIDTH_WITH_NAV - 24, 14, "") {
                @Override
                public void onClicked(boolean leftSide) {
                    int index = joystickSelectionButtons.indexOf(this) + joystickSelectionScroll;
                    if (index >= 0 && index < visibleJoystickNames.size()) {
                        selectedJoystickName = visibleJoystickNames.get(index);
                        selectedJoystickComponentCount = InterfaceManager.inputInterface.getJoystickComponentCount(selectedJoystickName);
                        joystickComponentId = -1;
                        joystickComponentScroll = 0;
                        joystickAssignmentScroll = 0;
                    }
                }
            };
            joystickSelectionButtons.add(deviceButton);
            addComponent(deviceButton);
        }

        for (int i = 0; i < SETTING_ROWS_WITH_NAV - 2; ++i) {
            GUIComponentButton componentButton = new TextButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 46 + ROW_HEIGHT * i, 140, 14, "") {
                @Override
                public void onClicked(boolean leftSide) {
                    int index = joystickComponentButtons.indexOf(this) + joystickComponentScroll;
                    if (index >= 0 && index < visibleJoystickComponentIndexes.size()) {
                        joystickComponentId = visibleJoystickComponentIndexes.get(index);
                        assigningDigital = !InterfaceManager.inputInterface.isJoystickComponentAxis(selectedJoystickName, joystickComponentId);
                        joystickAssignmentScroll = 0;
                    }
                }
            };
            joystickComponentButtons.add(componentButton);
            addComponent(componentButton);
            SolidRect stateBack = new SolidRect(guiLeft + LIST_X_WITH_NAV + 150, guiTop + CONTENT_TOP + 48 + ROW_HEIGHT * i, 50, 8, COLOR_SCROLL_TRACK, 0.75F);
            joystickStateBacks.add(stateBack);
            addComponent(stateBack);
            SolidRect stateFill = new SolidRect(guiLeft + LIST_X_WITH_NAV + 175, guiTop + CONTENT_TOP + 48 + ROW_HEIGHT * i, 1, 8, COLOR_CHANGED, 0.9F);
            joystickStateFills.add(stateFill);
            addComponent(stateFill);
            TextLabel assignmentLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + 216, guiTop + CONTENT_TOP + 45 + ROW_HEIGHT * i, 116, 14, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F);
            joystickAssignmentLabels.add(assignmentLabel);
            addComponent(assignmentLabel);
        }

        addComponent(joystickAssignmentPromptLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 2, LIST_WIDTH_WITH_NAV, 14, LanguageSystem.GUI_CONFIG_JOYSTICK_CHOOSEMAP.getCurrentValue(), COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
        addComponent(clearAssignmentButton = new FlatButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONFIG_GUI_HEIGHT - 30, 120, 18, LanguageSystem.GUI_CONFIG_JOYSTICK_CLEAR.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                clearJoystickAssignment();
            }
        });
        addComponent(cancelAssignmentButton = new FlatButton(guiLeft + CONFIG_GUI_WIDTH - 142, guiTop + CONFIG_GUI_HEIGHT - 30, 120, 18, LanguageSystem.GUI_CONFIG_JOYSTICK_CANCEL.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                joystickComponentId = -1;
                calibrating = false;
                joystickAssignmentScroll = 0;
            }
        });
        addJoystickAssignmentRows();

        addComponent(confirmBoundsButton = new FlatButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONFIG_GUI_HEIGHT - 30, 120, 18, LanguageSystem.GUI_CONFIRM.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                boolean inverted = invertAxisButton.text.contains(LanguageSystem.GUI_CONFIG_JOYSTICK_INVERT.getCurrentValue());
                controlCalibrating.setAxisControl(selectedJoystickName, joystickComponentId, Double.parseDouble(axisMinBoundsTextBox.getText()), Double.parseDouble(axisMaxBoundsTextBox.getText()), inverted);
                changedJoystickControls.add(controlCalibrating.systemName);
                joystickComponentId = -1;
                calibrating = false;
                joystickAssignmentScroll = 0;
            }
        });
        addComponent(invertAxisButton = new FlatButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 104, 180, 18, LanguageSystem.GUI_CONFIG_JOYSTICK_AXISMODE.getCurrentValue() + LanguageSystem.GUI_CONFIG_JOYSTICK_NORMAL.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                text = LanguageSystem.GUI_CONFIG_JOYSTICK_AXISMODE.getCurrentValue() + (text.contains(LanguageSystem.GUI_CONFIG_JOYSTICK_INVERT.getCurrentValue()) ? LanguageSystem.GUI_CONFIG_JOYSTICK_NORMAL.getCurrentValue() : LanguageSystem.GUI_CONFIG_JOYSTICK_INVERT.getCurrentValue());
            }
        });
        addComponent(axisMaxBoundsTextBox = new GUIComponentTextBox(this, guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 52, 160, 14, "0.0", COLOR_TEXT, 16, 0, 0, 0, 0));
        axisMaxBoundsTextBox.enabled = false;
        addComponent(axisMinBoundsTextBox = new GUIComponentTextBox(this, guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 78, 160, 14, "0.0", COLOR_TEXT, 16, 0, 0, 0, 0));
        axisMinBoundsTextBox.enabled = false;
        addComponent(joystickCalibrationLabel1 = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 18, LIST_WIDTH_WITH_NAV, 14, LanguageSystem.GUI_CONFIG_JOYSTICK_CALIBRATE1.getCurrentValue(), COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
        addComponent(joystickCalibrationLabel2 = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 32, LIST_WIDTH_WITH_NAV, 14, LanguageSystem.GUI_CONFIG_JOYSTICK_CALIBRATE2.getCurrentValue(), COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
    }

    private void addJoystickAssignmentRows() {
        joystickDigitalAssignmentRows.clear();
        joystickAnalogAssignmentRows.clear();
        for (ControlGroup group : ControlGroup.values()) {
            joystickDigitalAssignmentRows.add(new ControlHeaderRow(group.title));
            joystickAnalogAssignmentRows.add(new ControlHeaderRow(group.title));
            for (ControlsJoystick control : ControlsJoystick.values()) {
                if (control.systemName.startsWith(group.prefix)) {
                    (control.isAxis ? joystickAnalogAssignmentRows : joystickDigitalAssignmentRows).add(new JoystickAssignmentRow(control));
                }
            }
        }
    }

    private void updateNavigation() {
        for (MainPage page : pageButtons.keySet()) {
            FlatButton button = pageButtons.get(page);
            boolean locked = page == MainPage.DEVELOPMENT && isDevelopmentLocked();
            button.visible = true;
            button.locked = locked;
            button.enabled = page != activePage && !locked;
            button.active = page == activePage;
            button.text = page.title.getCurrentValue();
        }
        sideDivider.visible = activePage == MainPage.COMMON || activePage == MainPage.CONTROLS;
        boolean serverSettingsLocked = isServerSettingsLocked();
        if (activeCommonPage == CommonPage.SERVER && serverSettingsLocked) {
            activeCommonPage = CommonPage.CLIENT;
        }
        for (CommonPage page : commonButtons.keySet()) {
            TextButton button = commonButtons.get(page);
            boolean locked = page == CommonPage.SERVER && serverSettingsLocked;
            button.visible = activePage == MainPage.COMMON;
            button.enabled = !locked;
            button.active = page == activeCommonPage;
            button.textColorOverride = COLOR_TEXT;
            button.text = page.title.getCurrentValue();
        }
        for (ControlsPage page : controlsButtons.keySet()) {
            TextButton button = controlsButtons.get(page);
            button.visible = activePage == MainPage.CONTROLS;
            button.enabled = true;
            button.active = page == activeControlsPage;
            button.textColorOverride = COLOR_TEXT;
            button.text = page.title.getCurrentValue();
        }
    }

    private void hideDynamicContent() {
        pageScrollTrack.visible = false;
        pageScrollThumb.visible = false;
        noResultsLabel.visible = false;
        for (SolidRect background : settingRowBackgrounds) {
            background.visible = false;
        }
        for (SettingRow row : clientSettingRows) {
            row.setVisible(false);
        }
        for (SettingRow row : serverSettingRows) {
            row.setVisible(false);
        }
        for (SettingRow row : renderingSettingRows) {
            row.setVisible(false);
        }
        for (SettingRow row : developmentSettingRows) {
            row.setVisible(false);
        }
        for (ControlListRow row : keyboardRows) {
            row.setVisible(false);
        }
        for (GUIComponentButton button : joystickSelectionButtons) {
            button.visible = false;
        }
        for (GUIComponentButton button : joystickComponentButtons) {
            button.visible = false;
        }
        for (TextLabel label : joystickAssignmentLabels) {
            label.visible = false;
        }
        for (SolidRect rect : joystickStateBacks) {
            rect.visible = false;
        }
        for (SolidRect rect : joystickStateFills) {
            rect.visible = false;
        }
        for (ControlListRow row : joystickDigitalAssignmentRows) {
            row.setVisible(false);
        }
        for (ControlListRow row : joystickAnalogAssignmentRows) {
            row.setVisible(false);
        }
        joystickHeaderLabel.visible = false;
        joystickBackButton.visible = false;
        deadzoneDownButton.visible = false;
        deadzoneUpButton.visible = false;
        deadzoneValueLabel.visible = false;
        joystickColumnIndexLabel.visible = false;
        joystickColumnNameLabel.visible = false;
        joystickColumnStateLabel.visible = false;
        joystickColumnAssignmentLabel.visible = false;
        joystickAssignmentPromptLabel.visible = false;
        clearAssignmentButton.visible = false;
        cancelAssignmentButton.visible = false;
        confirmBoundsButton.visible = false;
        invertAxisButton.visible = false;
        axisMinBoundsTextBox.visible = false;
        axisMaxBoundsTextBox.visible = false;
        joystickCalibrationLabel1.visible = false;
        joystickCalibrationLabel2.visible = false;
        controlSelectionFaultLabel.visible = false;
    }

    private void updateCommonPage(int wheelMovement) {
        if (activeCommonPage == CommonPage.CLIENT) {
            commonClientScroll = adjustScrollForWheel(commonClientScroll, clientSettingRows.size(), SETTING_ROWS_WITH_NAV, wheelMovement);
            commonClientScroll = clampScroll(commonClientScroll, clientSettingRows.size(), SETTING_ROWS_WITH_NAV);
            updateSettingList(clientSettingRows, commonClientScroll, SETTING_ROWS_WITH_NAV, LIST_X_WITH_NAV, CONTENT_TOP + 10, LIST_WIDTH_WITH_NAV);
        } else {
            commonServerScroll = adjustScrollForWheel(commonServerScroll, serverSettingRows.size(), SETTING_ROWS_WITH_NAV, wheelMovement);
            commonServerScroll = clampScroll(commonServerScroll, serverSettingRows.size(), SETTING_ROWS_WITH_NAV);
            updateSettingList(serverSettingRows, commonServerScroll, SETTING_ROWS_WITH_NAV, LIST_X_WITH_NAV, CONTENT_TOP + 10, LIST_WIDTH_WITH_NAV);
        }
    }

    private void updateDevelopmentPage(int wheelMovement) {
        if (isDevelopmentLocked()) {
            noResultsLabel.visible = true;
            noResultsLabel.text = LanguageSystem.GUI_CONFIG_DEVELOPMENT_LOCKED.getCurrentValue();
            setComponentPosition(noResultsLabel, guiLeft + LIST_X_FULL, guiTop + CONTENT_TOP + 40);
            return;
        }
        developmentScroll = adjustScrollForWheel(developmentScroll, developmentSettingRows.size(), SETTING_ROWS_FULL, wheelMovement);
        developmentScroll = clampScroll(developmentScroll, developmentSettingRows.size(), SETTING_ROWS_FULL);
        updateSettingList(developmentSettingRows, developmentScroll, SETTING_ROWS_FULL, LIST_X_FULL, CONTENT_TOP + 10, LIST_WIDTH_FULL);
    }

    private void updateSettingList(List<SettingRow> rows, int scrollSpot, int rowsToRender, int listX, int listY, int listWidth) {
        for (SolidRect background : settingRowBackgrounds) {
            background.visible = false;
        }
        for (int i = 0; i < rows.size(); ++i) {
            SettingRow row = rows.get(i);
            int rowIndex = i - scrollSpot;
            boolean visible = rowIndex >= 0 && rowIndex < rowsToRender;
            row.setVisible(visible);
            if (visible) {
                updateRowBackground(rowIndex, i, guiLeft + listX, guiTop + listY + ROW_HEIGHT * rowIndex, listWidth);
                row.setPosition(guiLeft + listX, guiTop + listY + ROW_HEIGHT * rowIndex, listWidth);
                row.updateState();
            }
        }
        updateScrollBar(scrollSpot, rows.size(), rowsToRender, listY, ROW_HEIGHT);
        noResultsLabel.visible = rows.isEmpty();
    }

    private int getAndSetRenderingScroll(int wheelMovement, int size) {
        renderingScroll = adjustScrollForWheel(renderingScroll, size, SETTING_ROWS_FULL, wheelMovement);
        renderingScroll = clampScroll(renderingScroll, size, SETTING_ROWS_FULL);
        return renderingScroll;
    }

    private void updateControlsPage(int wheelMovement) {
        if (activeControlsPage == ControlsPage.KEYBOARD) {
            if (wheelMovement != 0) {
                clearTextFocus();
            }
            updateKeyboardPage(wheelMovement);
        } else {
            updateJoystickPage(wheelMovement);
        }
    }

    private void updateKeyboardPage(int wheelMovement) {
        keyboardScroll = adjustScrollForWheel(keyboardScroll, keyboardRows.size(), SETTING_ROWS_WITH_NAV, wheelMovement);
        keyboardScroll = clampScroll(keyboardScroll, keyboardRows.size(), SETTING_ROWS_WITH_NAV);
        updateControlList(keyboardRows, keyboardScroll, SETTING_ROWS_WITH_NAV, LIST_X_WITH_NAV, CONTENT_TOP + 10, LIST_WIDTH_WITH_NAV);
    }

    private void updateJoystickPage(int wheelMovement) {
        if (!InterfaceManager.inputInterface.isJoystickSupportEnabled()) {
            controlSelectionFaultLabel.visible = true;
            controlSelectionFaultLabel.text = InterfaceManager.inputInterface.isJoystickSupportBlocked() ? LanguageSystem.GUI_CONFIG_JOYSTICK_DISABLED.getCurrentValue() : LanguageSystem.GUI_CONFIG_JOYSTICK_ERROR.getCurrentValue();
            return;
        }
        if (calibrating) {
            updateJoystickCalibrationPage();
        } else if (joystickComponentId != -1) {
            updateJoystickAssignmentPage(wheelMovement);
        } else if (selectedJoystickName != null) {
            updateJoystickComponentPage(wheelMovement);
        } else {
            updateJoystickSelectionPage(wheelMovement);
        }
    }

    private void updateJoystickSelectionPage(int wheelMovement) {
        visibleJoystickNames.clear();
        visibleJoystickNames.addAll(InterfaceManager.inputInterface.getAllJoystickNames());
        joystickSelectionScroll = adjustScrollForWheel(joystickSelectionScroll, visibleJoystickNames.size(), SETTING_ROWS_WITH_NAV, wheelMovement);
        joystickSelectionScroll = clampScroll(joystickSelectionScroll, visibleJoystickNames.size(), SETTING_ROWS_WITH_NAV);
        joystickHeaderLabel.visible = true;
        joystickHeaderLabel.text = LanguageSystem.GUI_CONFIG_JOYSTICK_SELECT.getCurrentValue();
        for (int i = 0; i < joystickSelectionButtons.size(); ++i) {
            GUIComponentButton button = joystickSelectionButtons.get(i);
            int index = i + joystickSelectionScroll;
            button.visible = index < visibleJoystickNames.size();
            if (button.visible) {
                button.text = visibleJoystickNames.get(index);
                setComponentPosition(button, guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + 24 + ROW_HEIGHT * i);
            }
        }
        updateScrollBar(joystickSelectionScroll, visibleJoystickNames.size(), SETTING_ROWS_WITH_NAV, CONTENT_TOP + 24, ROW_HEIGHT);
        noResultsLabel.visible = visibleJoystickNames.isEmpty();
    }

    private void updateJoystickComponentPage(int wheelMovement) {
        visibleJoystickComponentIndexes.clear();
        for (int i = 0; i < selectedJoystickComponentCount; ++i) {
            visibleJoystickComponentIndexes.add(i);
        }
        int rowsToRender = SETTING_ROWS_WITH_NAV - 2;
        joystickComponentScroll = adjustScrollForWheel(joystickComponentScroll, visibleJoystickComponentIndexes.size(), rowsToRender, wheelMovement);
        joystickComponentScroll = clampScroll(joystickComponentScroll, visibleJoystickComponentIndexes.size(), rowsToRender);
        joystickBackButton.visible = true;
        deadzoneDownButton.visible = true;
        deadzoneUpButton.visible = true;
        deadzoneValueLabel.visible = true;
        double deadzone = ConfigSystem.client.controlSettings.joystickDeadZone.value;
        deadzoneDownButton.enabled = canStep(deadzone, getNumericMetadata("joystickDeadZone"), -1);
        deadzoneUpButton.enabled = canStep(deadzone, getNumericMetadata("joystickDeadZone"), 1);
        deadzoneValueLabel.text = LanguageSystem.GUI_CONFIG_SETTING_JOYSTICK_DEADZONE.getCurrentValue() + ": " + formatNumber(deadzone);
        joystickColumnIndexLabel.visible = true;
        joystickColumnNameLabel.visible = true;
        joystickColumnStateLabel.visible = true;
        joystickColumnAssignmentLabel.visible = true;
        for (int i = 0; i < joystickComponentButtons.size(); ++i) {
            int index = i + joystickComponentScroll;
            boolean visible = index < visibleJoystickComponentIndexes.size();
            GUIComponentButton button = joystickComponentButtons.get(i);
            SolidRect back = joystickStateBacks.get(i);
            SolidRect fill = joystickStateFills.get(i);
            TextLabel assignment = joystickAssignmentLabels.get(i);
            button.visible = visible;
            back.visible = visible;
            fill.visible = visible;
            assignment.visible = visible;
            if (visible) {
                int componentIndex = visibleJoystickComponentIndexes.get(index);
                int y = guiTop + CONTENT_TOP + 46 + ROW_HEIGHT * i;
                button.text = String.format(Locale.ROOT, "%02d  %s", componentIndex + 1, InterfaceManager.inputInterface.getJoystickComponentName(selectedJoystickName, componentIndex));
                setComponentPosition(button, guiLeft + LIST_X_WITH_NAV, y);
                setComponentPosition(back, guiLeft + LIST_X_WITH_NAV + 150, y + 3);
                setComponentPosition(fill, guiLeft + LIST_X_WITH_NAV + 175, y + 3);
                setComponentPosition(assignment, guiLeft + LIST_X_WITH_NAV + 216, y);
                assignment.text = getJoystickComponentAssignment(componentIndex);
                updateJoystickComponentState(componentIndex, back, fill);
            }
        }
        updateScrollBar(joystickComponentScroll, visibleJoystickComponentIndexes.size(), rowsToRender, CONTENT_TOP + 46, ROW_HEIGHT);
    }

    private void updateJoystickAssignmentPage(int wheelMovement) {
        List<ControlListRow> rows = assigningDigital ? joystickDigitalAssignmentRows : joystickAnalogAssignmentRows;
        joystickAssignmentScroll = adjustScrollForWheel(joystickAssignmentScroll, rows.size(), SETTING_ROWS_WITH_NAV - 2, wheelMovement);
        joystickAssignmentScroll = clampScroll(joystickAssignmentScroll, rows.size(), SETTING_ROWS_WITH_NAV - 2);
        joystickAssignmentPromptLabel.visible = true;
        clearAssignmentButton.visible = true;
        cancelAssignmentButton.visible = true;
        updateControlList(rows, joystickAssignmentScroll, SETTING_ROWS_WITH_NAV - 2, LIST_X_WITH_NAV, CONTENT_TOP + 24, LIST_WIDTH_WITH_NAV);
    }

    private void updateJoystickCalibrationPage() {
        cancelAssignmentButton.visible = true;
        confirmBoundsButton.visible = true;
        invertAxisButton.visible = true;
        axisMinBoundsTextBox.visible = true;
        axisMaxBoundsTextBox.visible = true;
        joystickCalibrationLabel1.visible = true;
        joystickCalibrationLabel2.visible = true;
        float pollData = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, joystickComponentId);
        if (pollData < 0) {
            axisMinBoundsTextBox.setText(String.valueOf(Math.min(Double.parseDouble(axisMinBoundsTextBox.getText()), pollData)));
        } else {
            axisMaxBoundsTextBox.setText(String.valueOf(Math.max(Double.parseDouble(axisMaxBoundsTextBox.getText()), pollData)));
        }
    }

    private void updateControlList(List<ControlListRow> rows, int scrollSpot, int rowsToRender, int listX, int listY, int listWidth) {
        for (SolidRect background : settingRowBackgrounds) {
            background.visible = false;
        }
        for (int i = 0; i < rows.size(); ++i) {
            ControlListRow row = rows.get(i);
            int rowIndex = i - scrollSpot;
            boolean visible = rowIndex >= 0 && rowIndex < rowsToRender;
            row.setVisible(visible);
            if (visible) {
                if (!(row instanceof ControlHeaderRow)) {
                    updateRowBackground(rowIndex, i, guiLeft + listX, guiTop + listY + ROW_HEIGHT * rowIndex, listWidth);
                }
                row.setPosition(guiLeft + listX, guiTop + listY + ROW_HEIGHT * rowIndex, listWidth);
                row.updateState();
            }
        }
        updateScrollBar(scrollSpot, rows.size(), rowsToRender, listY, ROW_HEIGHT);
        noResultsLabel.visible = rows.isEmpty();
    }

    private void updateRowBackground(int rowIndex, int sourceIndex, int x, int y, int width) {
        if (rowIndex < settingRowBackgrounds.size() && (sourceIndex % 2) == 0) {
            SolidRect background = settingRowBackgrounds.get(rowIndex);
            background.visible = true;
            setComponentBounds(background, x - 4, y - 4, width + 8, ROW_HEIGHT);
        }
    }

    private void updateScrollBar(int scrollSpot, int itemCount, int rowsToRender, int rowTop, int rowHeight) {
        boolean canScroll = itemCount > rowsToRender;
        pageScrollTrack.visible = false;
        pageScrollThumb.visible = canScroll;
        if (canScroll) {
            int trackTop = rowTop;
            int trackHeight = rowHeight * rowsToRender;
            int thumbHeight = Math.min(trackHeight, Math.max(SCROLL_THUMB_MIN_HEIGHT, trackHeight * rowsToRender / itemCount));
            int maxScroll = Math.max(1, itemCount - rowsToRender);
            int thumbTravel = Math.max(0, trackHeight - thumbHeight);
            int thumbTop = trackTop + Math.round(thumbTravel * scrollSpot / (float) maxScroll);
            setComponentBounds(pageScrollThumb, guiLeft + SCROLL_X - 2, guiTop + thumbTop, SCROLL_THUMB_WIDTH, thumbHeight);
            activeScrollTrackTop = guiTop + trackTop;
            activeScrollTrackHeight = trackHeight;
            activeScrollThumbHeight = thumbHeight;
            activeScrollItemCount = itemCount;
            activeScrollRowsToRender = rowsToRender;
        }
    }

    private void updateScrollFromMouse(int mouseY) {
        if (activeScrollItemCount <= activeScrollRowsToRender) {
            return;
        }
        int maxScroll = activeScrollItemCount - activeScrollRowsToRender;
        int thumbTravel = Math.max(1, activeScrollTrackHeight - activeScrollThumbHeight);
        int thumbTop = Math.max(activeScrollTrackTop, Math.min(activeScrollTrackTop + thumbTravel, mouseY - scrollbarDragOffset));
        int newScroll = Math.round((thumbTop - activeScrollTrackTop) * maxScroll / (float) thumbTravel);
        setActiveScrollSpot(newScroll);
        setComponentPosition(pageScrollThumb, guiLeft + SCROLL_X - 2, thumbTop);
    }

    private void setActiveScrollSpot(int scrollSpot) {
        if (activePage == MainPage.COMMON) {
            if (activeCommonPage == CommonPage.CLIENT) {
                commonClientScroll = scrollSpot;
            } else {
                commonServerScroll = scrollSpot;
            }
        } else if (activePage == MainPage.RENDERING) {
            renderingScroll = scrollSpot;
        } else if (activePage == MainPage.DEVELOPMENT) {
            developmentScroll = scrollSpot;
        } else if (activePage == MainPage.CONTROLS) {
            if (activeControlsPage == ControlsPage.KEYBOARD) {
                keyboardScroll = scrollSpot;
            } else if (selectedJoystickName == null) {
                joystickSelectionScroll = scrollSpot;
            } else if (joystickComponentId != -1 && !calibrating) {
                joystickAssignmentScroll = scrollSpot;
            } else {
                joystickComponentScroll = scrollSpot;
            }
        }
    }

    private void clearTransientSelection() {
        joystickComponentId = -1;
        calibrating = false;
        joystickAssignmentScroll = 0;
    }

    private void clearTextFocus() {
        editingText = false;
        for (AGUIComponent component : components) {
            if (component instanceof GUIComponentTextBox) {
                ((GUIComponentTextBox) component).focused = false;
            }
        }
    }

    private boolean isDevelopmentLocked() {
        return InterfaceManager.clientInterface != null && InterfaceManager.clientInterface.getClientPlayer() != null && !InterfaceManager.clientInterface.getClientPlayer().isOP();
    }

    private boolean isServerSettingsLocked() {
        return isDevelopmentLocked();
    }

    private void updateJoystickComponentState(int componentIndex, SolidRect back, SolidRect fill) {
        float pollData = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, componentIndex);
        if (InterfaceManager.inputInterface.isJoystickComponentAxis(selectedJoystickName, componentIndex)) {
            int width = (int) (pollData * 25);
            if (width >= 0) {
                setComponentBounds(fill, (int) back.position.x + 25, (int) -back.position.y, width, 8);
            } else {
                setComponentBounds(fill, (int) back.position.x + 25 + width, (int) -back.position.y, -width, 8);
            }
            fill.color.setTo(COLOR_CHANGED);
        } else {
            setComponentBounds(fill, (int) back.position.x + 20, (int) -back.position.y, 10, 8);
            fill.color.setTo(pollData == 0 ? COLOR_DIM_TEXT : COLOR_CHANGED);
        }
    }

    private String getJoystickComponentAssignment(int componentIndex) {
        for (ControlsJoystick control : ControlsJoystick.values()) {
            if (selectedJoystickName.equals(control.config.joystickName) && control.config.buttonIndex == componentIndex) {
                return control.language.getCurrentValue();
            }
        }
        return "";
    }

    private void clearJoystickAssignment() {
        for (ControlsJoystick control : ControlsJoystick.values()) {
            if (selectedJoystickName.equals(control.config.joystickName) && (control.isAxis ^ assigningDigital) && control.config.buttonIndex == joystickComponentId) {
                control.clearControl();
                changedJoystickControls.add(control.systemName);
            }
        }
        joystickComponentId = -1;
        joystickAssignmentScroll = 0;
    }

    private void stepDeadzone(int direction) {
        JSONConfigEntry<Double> entry = ConfigSystem.client.controlSettings.joystickDeadZone;
        entry.value = stepNumber(entry.value, getNumericMetadata("joystickDeadZone"), direction);
        changedSettings.add("client.control.joystickDeadZone");
        ConfigSystem.saveToDisk();
    }

    private int adjustScrollForWheel(int scrollSpot, int itemCount, int rowsToRender, int wheelMovement) {
        if (wheelMovement < 0 && scrollSpot + rowsToRender < itemCount) {
            return scrollSpot + 1;
        } else if (wheelMovement > 0 && scrollSpot > 0) {
            return scrollSpot - 1;
        } else {
            return scrollSpot;
        }
    }

    private int clampScroll(int scrollSpot, int itemCount, int rowsToRender) {
        return Math.max(0, Math.min(scrollSpot, Math.max(0, itemCount - rowsToRender)));
    }

    private boolean canStep(double value, NumericMetadata metadata, int direction) {
        return direction < 0 ? value > metadata.minimum : value < metadata.maximum;
    }

    private double stepNumber(double currentValue, NumericMetadata metadata, int direction) {
        double newValue = currentValue < metadata.minimum || currentValue > metadata.maximum ? clamp(currentValue, metadata.minimum, metadata.maximum) : clamp(currentValue + metadata.step * direction, metadata.minimum, metadata.maximum);
        return roundForStep(newValue, metadata.step);
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private double roundForStep(double value, double step) {
        return Math.round(value / step) * step;
    }

    private String formatNumber(double value) {
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        while (formatted.contains(".") && formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted.endsWith(".") ? formatted.substring(0, formatted.length() - 1) : formatted;
    }

    private NumericMetadata getNumericMetadata(String fieldName) {
        if ("soundVolume".equals(fieldName) || "radioVolume".equals(fieldName)) {
            return new NumericMetadata(0.0D, 1.5D, 0.1D);
        } else if ("joystickDeadZone".equals(fieldName)) {
            return new NumericMetadata(0.0D, 1.0D, 0.01D);
        } else if ("steeringControlRate".equals(fieldName) || "steeringReturnRate".equals(fieldName)) {
            return new NumericMetadata(0.1D, 10.0D, 0.1D);
        } else if ("flightControlRate".equals(fieldName)) {
            return new NumericMetadata(0.1D, 5.0D, 0.1D);
        } else if ("mouseYokeRate".equals(fieldName)) {
            return new NumericMetadata(0.01D, 1.0D, 0.01D);
        } else if ("roadMaxLength".equals(fieldName)) {
            return new NumericMetadata(1.0D, 128.0D, 1.0D);
        } else if ("seaLevel".equals(fieldName)) {
            return new NumericMetadata(0.0D, 320.0D, 1.0D);
        } else if ("maxFlightHeight".equals(fieldName)) {
            return new NumericMetadata(0.0D, 1024.0D, 10.0D);
        } else if (fieldName.endsWith("Factor")) {
            return new NumericMetadata(0.0D, 10.0D, 0.1D);
        } else {
            return new NumericMetadata(0.0D, 100.0D, 1.0D);
        }
    }

    private LanguageEntry getSettingLabel(String fieldName) {
        switch (fieldName) {
            case "renderHUD_1P": return LanguageSystem.GUI_CONFIG_SETTING_RENDER_HUD_1P;
            case "renderHUD_3P": return LanguageSystem.GUI_CONFIG_SETTING_RENDER_HUD_3P;
            case "fullHUD_1P": return LanguageSystem.GUI_CONFIG_SETTING_FULL_HUD_1P;
            case "fullHUD_3P": return LanguageSystem.GUI_CONFIG_SETTING_FULL_HUD_3P;
            case "transpHUD_1P": return LanguageSystem.GUI_CONFIG_SETTING_TRANSP_HUD_1P;
            case "transpHUD_3P": return LanguageSystem.GUI_CONFIG_SETTING_TRANSP_HUD_3P;
            case "renderWindows": return LanguageSystem.GUI_CONFIG_SETTING_RENDER_WINDOWS;
            case "innerWindows": return LanguageSystem.GUI_CONFIG_SETTING_INNER_WINDOWS;
            case "freecam_3P": return LanguageSystem.GUI_CONFIG_SETTING_FREECAM_3P;
            case "renderFlares": return LanguageSystem.GUI_CONFIG_SETTING_RENDER_FLARES;
            case "renderBeams": return LanguageSystem.GUI_CONFIG_SETTING_RENDER_BEAMS;
            case "brightLights": return LanguageSystem.GUI_CONFIG_SETTING_BRIGHT_LIGHTS;
            case "blendedLights": return LanguageSystem.GUI_CONFIG_SETTING_BLENDED_LIGHTS;
            case "lightsTransp": return LanguageSystem.GUI_CONFIG_SETTING_LIGHTS_TRANSP;
            case "playerTweaks": return LanguageSystem.GUI_CONFIG_SETTING_PLAYER_TWEAKS;
            case "renderingMode": return LanguageSystem.GUI_CONFIG_SETTING_RENDERING_MODE;
            case "kbOverride": return LanguageSystem.GUI_CONFIG_SETTING_KB_OVERRIDE;
            case "north360": return LanguageSystem.GUI_CONFIG_SETTING_NORTH_360;
            case "simpleThrottle": return LanguageSystem.GUI_CONFIG_SETTING_SIMPLE_THROTTLE;
            case "halfThrottle": return LanguageSystem.GUI_CONFIG_SETTING_HALF_THROTTLE;
            case "autostartEng": return LanguageSystem.GUI_CONFIG_SETTING_AUTOSTART_ENG;
            case "autoTrnSignals": return LanguageSystem.GUI_CONFIG_SETTING_AUTO_TRN_SIGNALS;
            case "useShifter": return LanguageSystem.GUI_CONFIG_SETTING_USE_SHIFTER;
            case "heliAutoLevel": return LanguageSystem.GUI_CONFIG_SETTING_HELI_AUTO_LEVEL;
            case "mouseYoke": return LanguageSystem.GUI_CONFIG_SETTING_MOUSE_YOKE;
            case "classicJystk": return LanguageSystem.GUI_CONFIG_SETTING_CLASSIC_JYSTK;
            case "steeringControlRate": return LanguageSystem.GUI_CONFIG_SETTING_STEERING_CONTROL_RATE;
            case "steeringReturnRate": return LanguageSystem.GUI_CONFIG_SETTING_STEERING_RETURN_RATE;
            case "flightControlRate": return LanguageSystem.GUI_CONFIG_SETTING_FLIGHT_CONTROL_RATE;
            case "mouseYokeRate": return LanguageSystem.GUI_CONFIG_SETTING_MOUSE_YOKE_RATE;
            case "joystickDeadZone": return LanguageSystem.GUI_CONFIG_SETTING_JOYSTICK_DEADZONE;
            case "soundVolume": return LanguageSystem.GUI_CONFIG_SETTING_SOUND_VOLUME;
            case "radioVolume": return LanguageSystem.GUI_CONFIG_SETTING_RADIO_VOLUME;
            default: return new LanguageEntry(fieldName);
        }
    }

    private String getKeyboardControlName(ControlsKeyboard control) {
        return control.config.isMouseButton ? InterfaceManager.inputInterface.getNameForMouseButton(control.config.keyCode) : InterfaceManager.inputInterface.getNameForKeyCode(control.config.keyCode);
    }

    private String getDynamicControlText(ControlsKeyboardDynamic control) {
        String modName = control.modControl.config.isMouseButton ? InterfaceManager.inputInterface.getNameForMouseButton(control.modControl.config.keyCode) : InterfaceManager.inputInterface.getNameForKeyCode(control.modControl.config.keyCode);
        String mainName = control.mainControl.config.isMouseButton ? InterfaceManager.inputInterface.getNameForMouseButton(control.mainControl.config.keyCode) : InterfaceManager.inputInterface.getNameForKeyCode(control.mainControl.config.keyCode);
        return modName + " + " + mainName;
    }

    private void setComponentPosition(AGUIComponent component, int x, int y) {
        component.position.x = x;
        component.position.y = -y;
        if (component instanceof TextButton) {
            component.textPosition.set(x + component.width / 2F, -y, component.textPosition.z);
        } else if (component instanceof GUIComponentButton) {
            GUIComponentButton button = (GUIComponentButton) component;
            component.textPosition.set(button.centeredText ? x + component.width / 2 : x, -y - (component.height - 8) / 2, component.textPosition.z);
        } else if (component instanceof GUIComponentTextBox) {
            component.textPosition.set(x, -y, component.textPosition.z);
        } else if (component instanceof TextLabel) {
            TextLabel label = (TextLabel) component;
            component.textPosition.set(label.alignment == TextAlignment.CENTERED ? x + component.width / 2F : x, -y, component.textPosition.z);
        } else {
            component.textPosition.set(x, -y, component.textPosition.z);
        }
    }

    private void setComponentBounds(AGUIComponent component, int x, int y, int width, int height) {
        component.width = width;
        component.height = height;
        setComponentPosition(component, x, y);
    }

    @Override
    public int getWidth() {
        return CONFIG_GUI_WIDTH;
    }

    @Override
    public int getHeight() {
        return CONFIG_GUI_HEIGHT;
    }

    @Override
    protected String getTexture() {
        return STANDARD_TEXTURE_NAME;
    }

    private enum MainPage {
        COMMON(LanguageSystem.GUI_CONFIG_PAGE_COMMON),
        RENDERING(LanguageSystem.GUI_CONFIG_PAGE_RENDERING),
        CONTROLS(LanguageSystem.GUI_CONFIG_PAGE_CONTROLS),
        DEVELOPMENT(LanguageSystem.GUI_CONFIG_PAGE_DEVELOPMENT);

        private final LanguageEntry title;

        MainPage(LanguageEntry title) {
            this.title = title;
        }
    }

    private enum CommonPage {
        CLIENT(LanguageSystem.GUI_CONFIG_COMMON_CLIENT),
        SERVER(LanguageSystem.GUI_CONFIG_COMMON_SERVER);

        private final LanguageEntry title;

        CommonPage(LanguageEntry title) {
            this.title = title;
        }
    }

    private enum ControlsPage {
        KEYBOARD(LanguageSystem.GUI_CONFIG_CONTROLS_KEYBOARD),
        JOYSTICK(LanguageSystem.GUI_CONFIG_CONTROLS_JOYSTICK);

        private final LanguageEntry title;

        ControlsPage(LanguageEntry title) {
            this.title = title;
        }
    }

    private enum ControlGroup {
        MAIN("general", LanguageSystem.GUI_CONFIG_CONTROLS_MAIN),
        GROUND("car", LanguageSystem.GUI_CONFIG_CONTROLS_GROUND),
        AIRCRAFT("aircraft", LanguageSystem.GUI_CONFIG_CONTROLS_AIRCRAFT);

        private final String prefix;
        private final LanguageEntry title;

        ControlGroup(String prefix, LanguageEntry title) {
            this.prefix = prefix;
            this.title = title;
        }
    }

    private enum SettingType {
        BOOLEAN,
        NUMBER,
        MODE
    }

    private static class NumericMetadata {
        private final double minimum;
        private final double maximum;
        private final double step;

        private NumericMetadata(double minimum, double maximum, double step) {
            this.minimum = minimum;
            this.maximum = maximum;
            this.step = step;
        }
    }

    private class SettingRow {
        private final String id;
        private final String fieldName;
        private final JSONConfigEntry<?> entry;
        private final LanguageEntry labelLanguage;
        private final NumericMetadata metadata;
        private final SettingType type;
        private final HoverLabel label;
        private TextButton toggleButton;
        private TextButton minusButton;
        private TextButton plusButton;
        private TextButton modeButton;
        private TextLabel valueLabel;

        private SettingRow(String id, String fieldName, JSONConfigEntry<?> entry, LanguageEntry labelLanguage, NumericMetadata metadata, SettingType type) {
            this.id = id;
            this.fieldName = fieldName;
            this.entry = entry;
            this.labelLanguage = labelLanguage;
            this.metadata = metadata;
            this.type = type;
            addComponent(label = new HoverLabel(0, 0, 160, 14, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F, entry.comment));
            if (type == SettingType.BOOLEAN) {
                addComponent(toggleButton = new TextButton(0, 0, 70, 14, "") {
                    @Override
                    public void onClicked(boolean leftSide) {
                        toggleBoolean();
                    }
                });
            } else if (type == SettingType.NUMBER) {
                addComponent(minusButton = new TextButton(0, 0, 16, 14, "<") {
                    @Override
                    public void onClicked(boolean leftSide) {
                        stepNumeric(-1);
                    }
                });
                addComponent(valueLabel = new TextLabel(0, 0, 55, 14, "", COLOR_TEXT, TextAlignment.CENTERED, 0.85F));
                addComponent(plusButton = new TextButton(0, 0, 16, 14, ">") {
                    @Override
                    public void onClicked(boolean leftSide) {
                        stepNumeric(1);
                    }
                });
            } else if (type == SettingType.MODE) {
                addComponent(modeButton = new TextButton(0, 0, 112, 14, "") {
                    @Override
                    public void onClicked(boolean leftSide) {
                        ConfigSystem.client.renderingSettings.renderingMode.value = (ConfigSystem.client.renderingSettings.renderingMode.value + 1) % 3;
                        changedSettings.add(id);
                        ConfigSystem.saveToDisk();
                    }
                });
            }
        }

        private String getRenderingModeText(int mode) {
            switch (mode) {
                case 0:
                    return LanguageSystem.GUI_CONFIG_RENDERING_MODE0.getCurrentValue();
                case 1:
                    return LanguageSystem.GUI_CONFIG_RENDERING_MODE1.getCurrentValue();
                default:
                    return LanguageSystem.GUI_CONFIG_RENDERING_MODE2.getCurrentValue();
            }
        }

        private void setVisible(boolean visible) {
            label.visible = visible;
            if (toggleButton != null) {
                toggleButton.visible = visible;
            }
            if (minusButton != null) {
                minusButton.visible = visible;
                plusButton.visible = visible;
                valueLabel.visible = visible;
            }
            if (modeButton != null) {
                modeButton.visible = visible;
            }
        }

        private void setPosition(int x, int y, int width) {
            int controlRight = x + width - 8;
            int labelWidth = Math.max(80, width - 145);
            setComponentBounds(label, x, y, labelWidth, 14);
            if (toggleButton != null) {
                setComponentBounds(toggleButton, controlRight - 68, y, 68, 14);
            }
            if (minusButton != null) {
                int controlLeft = controlRight - 68;
                setComponentBounds(minusButton, controlLeft, y, 16, 14);
                setComponentBounds(valueLabel, controlLeft + 18, y, 32, 14);
                setComponentBounds(plusButton, controlLeft + 52, y, 16, 14);
            }
            if (modeButton != null) {
                setComponentBounds(modeButton, controlRight - 68, y, 68, 14);
            }
        }

        private void updateState() {
            label.text = labelLanguage.getCurrentValue();
            label.color = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
            if (toggleButton != null) {
                toggleButton.text = Boolean.TRUE.equals(entry.value) ? LanguageSystem.GUI_CONFIG_ON.getCurrentValue() : LanguageSystem.GUI_CONFIG_OFF.getCurrentValue();
                toggleButton.textColorOverride = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
            }
            if (minusButton != null) {
                double value = ((Number) entry.value).doubleValue();
                valueLabel.text = formatNumber(value);
                valueLabel.color = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
                minusButton.enabled = canStep(value, metadata, -1);
                plusButton.enabled = canStep(value, metadata, 1);
                minusButton.textColorOverride = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
                plusButton.textColorOverride = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
            }
            if (modeButton != null) {
                int mode = ConfigSystem.client.renderingSettings.renderingMode.value;
                modeButton.text = getRenderingModeText(mode);
                modeButton.enabled = true;
                modeButton.active = false;
                modeButton.textColorOverride = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
            }
        }

        @SuppressWarnings("unchecked")
        private void toggleBoolean() {
            if ("mouseYoke".equals(fieldName)) {
                ControlSystem.toggleMouseYoke();
            } else {
                JSONConfigEntry<Boolean> booleanEntry = (JSONConfigEntry<Boolean>) entry;
                booleanEntry.value = !Boolean.TRUE.equals(booleanEntry.value);
                ConfigSystem.saveToDisk();
            }
            changedSettings.add(id);
        }

        @SuppressWarnings("unchecked")
        private void stepNumeric(int direction) {
            double newValue = stepNumber(((Number) entry.value).doubleValue(), metadata, direction);
            if (entry.value instanceof Float) {
                ((JSONConfigEntry<Float>) entry).value = (float) newValue;
            } else if (entry.value instanceof Double) {
                ((JSONConfigEntry<Double>) entry).value = newValue;
            } else if (entry.value instanceof Integer) {
                ((JSONConfigEntry<Integer>) entry).value = (int) Math.round(newValue);
            }
            changedSettings.add(id);
            ConfigSystem.saveToDisk();
        }
    }

    private abstract class ControlListRow {
        protected abstract void setVisible(boolean visible);
        protected abstract void setPosition(int x, int y, int width);
        protected void updateState() {
        }
    }

    private class ControlHeaderRow extends ControlListRow {
        private final LanguageEntry title;
        private final TextLabel label;

        private ControlHeaderRow(LanguageEntry title) {
            this.title = title;
            addComponent(label = new TextLabel(0, 0, 160, 16, "", COLOR_DIM_TEXT, TextAlignment.CENTERED, 1.0F));
        }

        @Override
        protected void setVisible(boolean visible) {
            label.visible = visible;
        }

        @Override
        protected void setPosition(int x, int y, int width) {
            setComponentBounds(label, x, y, width, 16);
        }

        @Override
        protected void updateState() {
            label.text = title.getCurrentValue();
        }
    }

    private class KeyboardBindingRow extends ControlListRow {
        private final ControlsKeyboard control;
        private final TextLabel label;
        private final KeybindBox box;

        private KeyboardBindingRow(final ControlsKeyboard control) {
            this.control = control;
            addComponent(label = new TextLabel(0, 0, 190, 14, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
            addComponent(box = new KeybindBox(control));
        }

        @Override
        protected void setVisible(boolean visible) {
            label.visible = visible;
            box.visible = visible;
        }

        @Override
        protected void setPosition(int x, int y, int width) {
            setComponentBounds(label, x, y, width - 115, 14);
            setComponentBounds(box, x + width - 104, y, 100, 14);
        }

        @Override
        protected void updateState() {
            label.text = control.language.getCurrentValue();
            label.color = changedKeyboardControls.contains(control.systemName) ? COLOR_CHANGED : COLOR_TEXT;
            if (!box.focused) {
                box.setText(getKeyboardControlName(control));
            }
            box.fontColor = changedKeyboardControls.contains(control.systemName) ? COLOR_CHANGED : COLOR_TEXT;
        }
    }

    private class KeyboardDynamicRow extends ControlListRow {
        private final ControlsKeyboardDynamic control;
        private final TextLabel label;
        private final TextLabel value;

        private KeyboardDynamicRow(ControlsKeyboardDynamic control) {
            this.control = control;
            addComponent(label = new TextLabel(0, 0, 190, 14, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
            addComponent(value = new TextLabel(0, 0, 100, 14, "", COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
        }

        @Override
        protected void setVisible(boolean visible) {
            label.visible = visible;
            value.visible = visible;
        }

        @Override
        protected void setPosition(int x, int y, int width) {
            setComponentBounds(label, x, y, width - 115, 14);
            setComponentBounds(value, x + width - 104, y, 100, 14);
        }

        @Override
        protected void updateState() {
            label.text = control.language.getCurrentValue();
            value.text = getDynamicControlText(control);
        }
    }

    private class JoystickAssignmentRow extends ControlListRow {
        private final ControlsJoystick control;
        private final TextButton button;

        private JoystickAssignmentRow(final ControlsJoystick control) {
            this.control = control;
            addComponent(button = new TextButton(0, 0, 200, 14, control.language.getCurrentValue()) {
                @Override
                public void onClicked(boolean leftSide) {
                    if (control.isAxis) {
                        controlCalibrating = control;
                        axisMinBoundsTextBox.setText("0.0");
                        axisMaxBoundsTextBox.setText("0.0");
                        calibrating = true;
                    } else {
                        control.setControl(selectedJoystickName, joystickComponentId);
                        changedJoystickControls.add(control.systemName);
                        joystickComponentId = -1;
                        joystickAssignmentScroll = 0;
                    }
                }
            });
        }

        @Override
        protected void setVisible(boolean visible) {
            button.visible = visible;
        }

        @Override
        protected void setPosition(int x, int y, int width) {
            setComponentBounds(button, x, y, width - 24, 14);
        }

        @Override
        protected void updateState() {
            button.text = control.language.getCurrentValue();
            button.textColorOverride = changedJoystickControls.contains(control.systemName) ? COLOR_CHANGED : COLOR_TEXT;
        }
    }

    private class KeybindBox extends GUIComponentTextBox {
        private final ControlsKeyboard control;
        private final RenderableData underlineRenderable;

        private KeybindBox(final ControlsKeyboard control) {
            super(GUIConfig.this, 0, 0, 100, 14, "", COLOR_TEXT, 18, 0, 0, 0, 0);
            this.control = control;
            this.underlineRenderable = createRectRenderable(COLOR_TEXT, 1.0F);
        }

        @Override
        public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey controlKey) {
            setText(InterfaceManager.inputInterface.getNameForKeyCode(typedCode));
            control.config.keyCode = typedCode;
            control.config.isMouseButton = false;
            ConfigSystem.client.controls.keyboard.put(control.systemName, control.config);
            changedKeyboardControls.add(control.systemName);
            ConfigSystem.saveToDisk();
            focused = false;
            editingText = false;
        }

        @Override
        public boolean handleMouseClicked(int mouseButton) {
            setText(InterfaceManager.inputInterface.getNameForMouseButton(mouseButton));
            control.config.keyCode = mouseButton;
            control.config.isMouseButton = true;
            ConfigSystem.client.controls.keyboard.put(control.systemName, control.config);
            changedKeyboardControls.add(control.systemName);
            ConfigSystem.saveToDisk();
            focused = false;
            editingText = false;
            return true;
        }

        @Override
        public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
            if (!blendingEnabled && (focused || isMouseInBounds(mouseX, mouseY))) {
                String underlineText = focused ? "_" : text;
                int underlineWidth = Math.min(width - 12, Math.max(18, (int) (RenderText.getStringWidth(underlineText, null) * 0.85F) + 6));
                int underlineX = (int) position.x + (width - underlineWidth) / 2;
                drawRect(underlineRenderable, underlineX, (int) -position.y + height - 3, underlineWidth, 1, fontColor, 1.0F, getZOffset());
            }
        }

        @Override
        public void renderText(boolean renderTextLit, int worldLightValue) {
            textPosition.x = position.x + width / 2F;
            RenderText.drawText(focused ? "_" : text, null, textPosition, fontColor, TextAlignment.CENTERED, 0.85F, true, width, renderTextLit || ignoreGUILightingState, worldLightValue);
        }
    }

    private abstract class TextButton extends GUIComponentButton {
        private final RenderableData underlineRenderable;
        private final boolean outlined;
        private ColorRGB textColorOverride = COLOR_TEXT;
        private boolean active;

        private TextButton(int x, int y, int width, int height, String text) {
            this(x, y, width, height, text, false);
        }

        private TextButton(int x, int y, int width, int height, String text, boolean outlined) {
            super(GUIConfig.this, x, y, width, height, text, true, COLOR_TEXT, false);
            this.outlined = outlined;
            this.underlineRenderable = createRectRenderable(COLOR_TEXT, 1.0F);
        }

        @Override
        public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
            if (!blendingEnabled) {
                if (outlined) {
                    drawOutline(underlineRenderable, (int) position.x, (int) -position.y, width, height, enabled ? textColorOverride : COLOR_DIM_TEXT, active || enabled && isMouseInBounds(mouseX, mouseY) ? 1.0F : 0.65F, getZOffset());
                } else if (enabled && (active || isMouseInBounds(mouseX, mouseY))) {
                    drawRect(underlineRenderable, (int) position.x, (int) -position.y + height - 3, width, 1, textColorOverride, 1.0F, getZOffset());
                }
            }
        }

        @Override
        public void renderText(boolean renderTextLit, int worldLightValue) {
            RenderText.drawText(text, null, textPosition, enabled ? textColorOverride : COLOR_DIM_TEXT, TextAlignment.CENTERED, 0.85F, true, width, renderTextLit || ignoreGUILightingState, worldLightValue);
        }
    }

    private abstract class FlatButton extends GUIComponentButton {
        private final RenderableData backgroundRenderable;
        private final RenderableData lockRenderable;
        private final boolean outlined;
        private boolean active;
        private boolean locked;

        private FlatButton(int x, int y, int width, int height, String text) {
            this(x, y, width, height, text, false);
        }

        private FlatButton(int x, int y, int width, int height, String text, boolean outlined) {
            super(GUIConfig.this, x, y, width, height, text);
            this.backgroundRenderable = createRectRenderable(COLOR_BUTTON, BUTTON_ALPHA);
            this.lockRenderable = createRectRenderable(COLOR_TEXT, 1.0F);
            this.outlined = outlined;
        }

        @Override
        public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
            if (!blendingEnabled) {
                ColorRGB color = active ? COLOR_BUTTON_ACTIVE : enabled && isMouseInBounds(mouseX, mouseY) ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
                drawRect(backgroundRenderable, (int) position.x, (int) -position.y, width, height, color, BUTTON_ALPHA, getZOffset());
                if (outlined) {
                    drawOutline(backgroundRenderable, (int) position.x, (int) -position.y, width, height, COLOR_OUTLINE, 1.0F, getZOffset() + 1);
                }
                if (locked) {
                    drawLockIcon((int) position.x + width - 16, (int) -position.y + 5);
                }
            }
        }

        @Override
        public void renderText(boolean renderTextLit, int worldLightValue) {
            RenderText.drawText(text, null, textPosition, locked ? COLOR_DIM_TEXT : COLOR_TEXT, TextAlignment.CENTERED, 0.8F, true, width - (locked ? 18 : 4), renderTextLit || ignoreGUILightingState, worldLightValue);
        }

        private void drawLockIcon(int x, int y) {
            drawRect(lockRenderable, x + 2, y + 5, 10, 7, locked ? COLOR_DIM_TEXT : COLOR_TEXT, 1.0F, getZOffset() + 1);
            drawRect(lockRenderable, x + 3, y + 2, 2, 4, locked ? COLOR_DIM_TEXT : COLOR_TEXT, 1.0F, getZOffset() + 1);
            drawRect(lockRenderable, x + 9, y + 2, 2, 4, locked ? COLOR_DIM_TEXT : COLOR_TEXT, 1.0F, getZOffset() + 1);
            drawRect(lockRenderable, x + 4, y + 1, 6, 2, locked ? COLOR_DIM_TEXT : COLOR_TEXT, 1.0F, getZOffset() + 1);
        }
    }

    private class HoverLabel extends TextLabel {
        private final String tooltipText;

        private HoverLabel(int x, int y, int width, int height, String text, ColorRGB color, TextAlignment alignment, float scale, String tooltipText) {
            super(x, y, width, height, text, color, alignment, scale);
            this.tooltipText = tooltipText;
        }

        @Override
        public List<String> getTooltipText() {
            if (tooltipText == null || tooltipText.isEmpty()) {
                return null;
            }
            List<String> lines = new ArrayList<>();
            lines.add(tooltipText);
            return lines;
        }
    }

    private class TextLabel extends AGUIComponent {
        protected ColorRGB color;
        protected final TextAlignment alignment;
        protected final float scale;

        private TextLabel(int x, int y, int width, int height, String text, ColorRGB color, TextAlignment alignment, float scale) {
            super(x, y, width, height);
            this.text = text;
            this.color = color;
            this.alignment = alignment;
            this.scale = scale;
            this.textPosition.x = alignment == TextAlignment.CENTERED ? position.x + width / 2F : position.x;
        }

        @Override
        public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        }

        @Override
        public void renderText(boolean renderTextLit, int worldLightValue) {
            RenderText.drawText(text, null, textPosition, color, alignment, scale, true, width, renderTextLit || ignoreGUILightingState, worldLightValue);
        }
    }

    private class SolidRect extends AGUIComponent {
        private final RenderableData renderableData;
        private final ColorRGB color = new ColorRGB();
        private float alpha;
        private final float outlineAlpha;

        private SolidRect(int x, int y, int width, int height, ColorRGB color, float alpha) {
            this(x, y, width, height, color, alpha, 0.0F);
        }

        private SolidRect(int x, int y, int width, int height, ColorRGB color, float alpha, float outlineAlpha) {
            super(x, y, width, height);
            this.color.setTo(color);
            this.alpha = alpha;
            this.outlineAlpha = outlineAlpha;
            this.renderableData = createRectRenderable(color, alpha);
        }

        @Override
        public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
            if (!blendingEnabled) {
                drawRect(renderableData, (int) position.x, (int) -position.y, width, height, color, alpha, getZOffset());
                if (outlineAlpha > 0) {
                    drawOutline(renderableData, (int) position.x, (int) -position.y, width, height, COLOR_OUTLINE, 1.0F, getZOffset() + 1);
                }
            }
        }
    }

    private RenderableData createRectRenderable(ColorRGB color, float alpha) {
        RenderableData data = new RenderableData(RenderableVertices.createSprite(1, null, null), STANDARD_TEXTURE_NAME);
        data.setColor(color);
        data.setAlpha(alpha);
        data.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        return data;
    }

    private void drawRect(RenderableData data, int x, int y, int width, int height, ColorRGB color, float alpha, int z) {
        if (width <= 0 || height <= 0) {
            return;
        }
        data.vertexObject.setSpriteProperties(0, 0, 0, width, height, 7 / 256F, 7 / 256F, 8 / 256F, 8 / 256F);
        data.setColor(color);
        data.setAlpha(alpha);
        data.transform.resetTransforms();
        data.transform.setTranslation(x, -y, z);
        data.render();
    }

    private void drawOutline(RenderableData data, int x, int y, int width, int height, ColorRGB color, float alpha, int z) {
        drawRect(data, x, y, width, 1, color, alpha, z);
        drawRect(data, x, y + height - 1, width, 1, color, alpha, z);
        drawRect(data, x, y, 1, height, color, alpha, z);
        drawRect(data, x + width - 1, y, 1, height, color, alpha, z);
    }
}
