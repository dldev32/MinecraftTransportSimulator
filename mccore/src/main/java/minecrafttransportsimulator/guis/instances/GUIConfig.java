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
    private static final float DEFAULT_GUI_SCALE = 1.0F / 1.2F;
    private static final int BASE_CONFIG_GUI_WIDTH = 512;
    private static final int BASE_CONFIG_GUI_HEIGHT = 376;
    private static final int BASE_TAB_HEIGHT = 20;
    private static final int BASE_TAB_GAP = 2;
    private static final int FIT_MARGIN = 8;
    private static final float MAX_SCREEN_WIDTH_FILL = 0.46F;
    private static final float MAX_SCREEN_HEIGHT_FILL = 0.65F;
    private static final double OUTLINE_THICKNESS = 0.5D;
    private static final int DECORATION_Z_OFFSET = 20;

    private float guiScale = DEFAULT_GUI_SCALE;
    private int CONFIG_GUI_WIDTH = scale(BASE_CONFIG_GUI_WIDTH);
    private int CONFIG_GUI_HEIGHT = scale(BASE_CONFIG_GUI_HEIGHT);
    private int TAB_HEIGHT = scale(BASE_TAB_HEIGHT);
    private int TAB_GAP = scale(BASE_TAB_GAP);
    private int TAB_WIDTH = (CONFIG_GUI_WIDTH - TAB_GAP * 3) / 4;
    private int CONTENT_TOP = scale(32);
    private int CONTENT_BOTTOM = CONFIG_GUI_HEIGHT - scale(14);
    private int LEFT_NAV_X = scale(16);
    private int LEFT_NAV_Y = scale(48);
    private int LEFT_NAV_WIDTH = scale(96);
    private int LEFT_NAV_BUTTON_HEIGHT = scale(18);
    private int LEFT_NAV_SPACING = scale(23);
    private int DIVIDER_X = scale(128);
    private int LIST_X_WITH_NAV = scale(146);
    private int LIST_X_FULL = scale(24);
    private int LIST_WIDTH_WITH_NAV = scale(338);
    private int LIST_WIDTH_FULL = scale(460);
    private int ROW_HEIGHT = scale(18);
    private int SETTING_ROWS_WITH_NAV = 17;
    private int SETTING_ROWS_FULL = 17;
    private int SCROLL_X = CONFIG_GUI_WIDTH - scale(18);
    private int SCROLL_TRACK_WIDTH = scale(4);
    private int SCROLL_THUMB_WIDTH = scale(8);
    private int SCROLL_THUMB_MIN_HEIGHT = scale(18);

    private int scale(int value) {
        return Math.max(1, Math.round(value * guiScale));
    }

    private int localScale(int value) {
        return Math.max(1, Math.round(value * guiScale / DEFAULT_GUI_SCALE));
    }

    private float textScale(float value) {
        return value * guiScale / DEFAULT_GUI_SCALE;
    }

    private float centeredTextY(int y, int height) {
        return -y - Math.max(0, height - localScale(8)) / 2F;
    }

    private float textButtonTextY(int y, int height) {
        return -y - Math.max(0, height - localScale(12)) / 2F;
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
    private SettingRow activeSliderRow;

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
    public void setupComponentsInit(int screenWidth, int screenHeight) {
        updateScaledLayout(screenWidth, screenHeight);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.guiLeft = (screenWidth - getWidth()) / 2;
        this.guiTop = (screenHeight - getHeight() - TAB_HEIGHT - TAB_GAP) / 2 + TAB_HEIGHT + TAB_GAP;
        setupComponents();
    }

    @Override
    public void setupComponents() {
        components.clear();
        clearLayoutCollections();
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

    private void updateScaledLayout(int screenWidth, int screenHeight) {
        float widthFillScale = screenWidth * MAX_SCREEN_WIDTH_FILL / BASE_CONFIG_GUI_WIDTH;
        float heightFillScale = screenHeight * MAX_SCREEN_HEIGHT_FILL / (BASE_CONFIG_GUI_HEIGHT + BASE_TAB_HEIGHT + BASE_TAB_GAP);
        float widthFitScale = (screenWidth - FIT_MARGIN * 2) / (float) BASE_CONFIG_GUI_WIDTH;
        float heightFitScale = (screenHeight - FIT_MARGIN * 2) / (float) (BASE_CONFIG_GUI_HEIGHT + BASE_TAB_HEIGHT + BASE_TAB_GAP);
        guiScale = Math.max(0.05F, Math.min(DEFAULT_GUI_SCALE, Math.min(Math.min(widthFillScale, heightFillScale), Math.min(widthFitScale, heightFitScale))));
        CONFIG_GUI_WIDTH = scale(BASE_CONFIG_GUI_WIDTH);
        CONFIG_GUI_HEIGHT = scale(BASE_CONFIG_GUI_HEIGHT);
        TAB_HEIGHT = scale(BASE_TAB_HEIGHT);
        TAB_GAP = scale(BASE_TAB_GAP);
        TAB_WIDTH = (CONFIG_GUI_WIDTH - TAB_GAP * 3) / 4;
        CONTENT_TOP = scale(32);
        CONTENT_BOTTOM = CONFIG_GUI_HEIGHT - scale(14);
        LEFT_NAV_X = scale(16);
        LEFT_NAV_Y = scale(48);
        LEFT_NAV_WIDTH = scale(96);
        LEFT_NAV_BUTTON_HEIGHT = scale(18);
        LEFT_NAV_SPACING = scale(23);
        DIVIDER_X = scale(128);
        LIST_X_WITH_NAV = scale(146);
        LIST_X_FULL = scale(24);
        LIST_WIDTH_WITH_NAV = scale(338);
        LIST_WIDTH_FULL = scale(460);
        ROW_HEIGHT = scale(18);
        SETTING_ROWS_WITH_NAV = 17;
        SETTING_ROWS_FULL = 17;
        SCROLL_X = CONFIG_GUI_WIDTH - scale(18);
        SCROLL_TRACK_WIDTH = scale(4);
        SCROLL_THUMB_WIDTH = scale(8);
        SCROLL_THUMB_MIN_HEIGHT = scale(18);
    }

    private void clearLayoutCollections() {
        pageButtons.clear();
        commonButtons.clear();
        controlsButtons.clear();
        clientSettingRows.clear();
        serverSettingRows.clear();
        renderingSettingRows.clear();
        developmentSettingRows.clear();
        settingRowBackgrounds.clear();
        keyboardRows.clear();
        joystickSelectionButtons.clear();
        visibleJoystickNames.clear();
        joystickComponentButtons.clear();
        joystickAssignmentLabels.clear();
        joystickStateBacks.clear();
        joystickStateFills.clear();
        visibleJoystickComponentIndexes.clear();
        joystickDigitalAssignmentRows.clear();
        joystickAnalogAssignmentRows.clear();
    }

    private void addSettingRowBackgrounds() {
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
            clearTextFocus();
            scrollbarDragging = true;
            scrollbarDragOffset = mouseY - (int) -pageScrollThumb.position.y;
            return true;
        } else if ((activeSliderRow = getSliderRowAt(mouseX, mouseY)) != null) {
            clearTextFocus();
            activeSliderRow.updateSliderFromMouse(mouseX);
            return true;
        } else {
            commitFocusedNumericTextBoxes(mouseX, mouseY);
            boolean clicked = super.onClick(mouseX, mouseY);
            return clicked || editingText;
        }
    }

    @Override
    public void onRelease() {
        scrollbarDragging = false;
        activeSliderRow = null;
        super.onRelease();
    }

    @Override
    public boolean onMouseDragged(int mouseX, int mouseY) {
        if (scrollbarDragging) {
            updateScrollFromMouse(mouseY);
            return true;
        } else if (activeSliderRow != null) {
            activeSliderRow.updateSliderFromMouse(mouseX);
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
        populateClientRows();
        populateSettingRows(renderingSettingRows, ConfigSystem.client.renderingSettings, "client.rendering", true);
        populateServerRows();
        developmentSettingRows.add(new SettingRow("server.general.devMode", "devMode", ConfigSystem.settings.general.devMode, LanguageSystem.GUI_CONFIG_SETTING_DEV_MODE, null, SettingType.BOOLEAN));
        developmentSettingRows.add(new SettingRow("server.general.vehicleStatusPanel", "vehicleStatusPanel", ConfigSystem.settings.general.vehicleStatusPanel, LanguageSystem.GUI_CONFIG_SETTING_VEHICLE_STATUS_PANEL, null, SettingType.BOOLEAN));
    }

    private void populateClientRows() {
        addSettingHeader(clientSettingRows, LanguageSystem.GUI_CONFIG_CATEGORY_GENERAL);
        addClientRow("kbOverride");
        addClientRow("north360");
        addAircraftControlModeRow();
        addClientRow("aimAssist");
        addRenderingClientRow("freecam_3P");
        addClientRow("classicJystk");
        addClientRow("DismountSafteySpeed");

        addSettingHeader(clientSettingRows, LanguageSystem.GUI_CONFIG_CONTROLS_GROUND);
        addClientRow("simpleThrottle");
        addClientRow("halfThrottle");
        addClientRow("autostartEng");
        addClientRow("autoTrnSignals");
        addClientRow("useShifter");
        addClientRow("steeringControlRate");
        addClientRow("steeringReturnRate");

        addSettingHeader(clientSettingRows, LanguageSystem.GUI_CONFIG_CONTROLS_AIRCRAFT);
        addClientRow("heliAutoLevel");
        addClientRow("flightControlRate");
        addClientRow("mouseYokeRate");

        addSettingHeader(clientSettingRows, LanguageSystem.GUI_CONFIG_CATEGORY_AUDIO);
        addClientRow("soundVolume");
        addClientRow("radioVolume");
    }

    private void addClientRow(String fieldName) {
        addConfigRow(clientSettingRows, ConfigSystem.client.controlSettings, "client.control", fieldName, false);
    }

    private void addRenderingClientRow(String fieldName) {
        addConfigRow(clientSettingRows, ConfigSystem.client.renderingSettings, "client.rendering", fieldName, false);
    }

    @SuppressWarnings("unchecked")
    private void populateSettingRows(List<SettingRow> rows, Object configObject, String idPrefix, boolean includeRenderingMode) {
        for (Field field : configObject.getClass().getFields()) {
            if (field.getType().equals(JSONConfigEntry.class)) {
                addConfigRow(rows, configObject, idPrefix, field.getName(), includeRenderingMode);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addConfigRow(List<SettingRow> rows, Object configObject, String idPrefix, String fieldName, boolean includeRenderingMode) {
        try {
            Field field = configObject.getClass().getField(fieldName);
            JSONConfigEntry<?> entry = (JSONConfigEntry<?>) field.get(configObject);
            if ("joystickDeadZone".equals(fieldName) || includeRenderingMode && "freecam_3P".equals(fieldName)) {
                return;
            } else if (entry.value instanceof Boolean) {
                rows.add(new SettingRow(idPrefix + "." + fieldName, fieldName, (JSONConfigEntry<Boolean>) entry, getSettingLabel(fieldName), null, SettingType.BOOLEAN));
            } else if (includeRenderingMode && "renderingMode".equals(fieldName)) {
                rows.add(new SettingRow(idPrefix + "." + fieldName, fieldName, entry, getSettingLabel(fieldName), null, SettingType.MODE));
            } else if (entry.value instanceof Number) {
                rows.add(new SettingRow(idPrefix + "." + fieldName, fieldName, entry, getSettingLabel(fieldName), getNumericMetadata(fieldName), SettingType.NUMBER));
            }
        } catch (Exception e) {
            //Skip only this row if reflection fails.  The rest of the GUI can still work.
        }
    }

    private void populateServerRows() {
        addSettingHeader(serverSettingRows, LanguageSystem.GUI_CONFIG_CATEGORY_GENERAL);
        addServerRow("server.general.keyRequiredToStartVehicles", "keyRequiredToStartVehicles", ConfigSystem.settings.general.keyRequiredToStartVehicles, LanguageSystem.GUI_CONFIG_SETTING_KEY_REQUIRED);
        addServerRow("server.general.noclipVehicles", "noclipVehicles", ConfigSystem.settings.general.noclipVehicles, LanguageSystem.GUI_CONFIG_SETTING_NOCLIP_VEHICLES);
        addServerRow("server.general.chunkloadVehicles", "chunkloadVehicles", ConfigSystem.settings.general.chunkloadVehicles, LanguageSystem.GUI_CONFIG_SETTING_CHUNKLOAD_VEHICLES);
        addServerRow("server.general.giveManualsOnJoin", "giveManualsOnJoin", ConfigSystem.settings.general.giveManualsOnJoin, LanguageSystem.GUI_CONFIG_SETTING_GIVE_MANUALS);
        addServerRow("server.general.performModCompatFunctions", "performModCompatFunctions", ConfigSystem.settings.general.performModCompatFunctions, LanguageSystem.GUI_CONFIG_SETTING_MOD_COMPAT);

        addSettingHeader(serverSettingRows, LanguageSystem.GUI_CONFIG_CATEGORY_VEHICLE_PHYSICS);
        addServerRow("server.general.aircraftSpeedFactor", "aircraftSpeedFactor", ConfigSystem.settings.general.aircraftSpeedFactor, LanguageSystem.GUI_CONFIG_SETTING_AIRCRAFT_SPEED_FACTOR);
        addServerRow("server.general.carSpeedFactor", "carSpeedFactor", ConfigSystem.settings.general.carSpeedFactor, LanguageSystem.GUI_CONFIG_SETTING_CAR_SPEED_FACTOR);
        addServerRow("server.general.fuelUsageFactor", "fuelUsageFactor", ConfigSystem.settings.general.fuelUsageFactor, LanguageSystem.GUI_CONFIG_SETTING_FUEL_USAGE_FACTOR);
        addServerRow("server.general.engineHoursFactor", "engineHoursFactor", ConfigSystem.settings.general.engineHoursFactor, LanguageSystem.GUI_CONFIG_SETTING_ENGINE_HOURS_FACTOR);
        addServerRow("server.general.gravityFactor", "gravityFactor", ConfigSystem.settings.general.gravityFactor, LanguageSystem.GUI_CONFIG_SETTING_GRAVITY_FACTOR);
        addServerRow("server.general.maxFlightHeight", "maxFlightHeight", ConfigSystem.settings.general.maxFlightHeight, LanguageSystem.GUI_CONFIG_SETTING_MAX_FLIGHT_HEIGHT);
        addServerRow("server.general.seaLevel", "seaLevel", ConfigSystem.settings.general.seaLevel, LanguageSystem.GUI_CONFIG_SETTING_SEA_LEVEL);

        addSettingHeader(serverSettingRows, LanguageSystem.GUI_CONFIG_CATEGORY_DAMAGE);
        addServerRow("server.damage.bulletBlockBreaking", "bulletBlockBreaking", ConfigSystem.settings.damage.bulletBlockBreaking, LanguageSystem.GUI_CONFIG_SETTING_BULLET_BLOCK_BREAKING);
        addServerRow("server.damage.bulletExplosions", "bulletExplosions", ConfigSystem.settings.damage.bulletExplosions, LanguageSystem.GUI_CONFIG_SETTING_BULLET_EXPLOSIONS);
        addServerRow("server.damage.vehicleBlockBreaking", "vehicleBlockBreaking", ConfigSystem.settings.damage.vehicleBlockBreaking, LanguageSystem.GUI_CONFIG_SETTING_VEHICLE_BLOCK_BREAKING);
        addServerRow("server.damage.vehicleDestruction", "vehicleDestruction", ConfigSystem.settings.damage.vehicleDestruction, LanguageSystem.GUI_CONFIG_SETTING_VEHICLE_DESTRUCTION);
        addServerRow("server.damage.vehicleExplosions", "vehicleExplosions", ConfigSystem.settings.damage.vehicleExplosions, LanguageSystem.GUI_CONFIG_SETTING_VEHICLE_EXPLOSIONS);
        addServerRow("server.damage.wheelBreakage", "wheelBreakage", ConfigSystem.settings.damage.wheelBreakage, LanguageSystem.GUI_CONFIG_SETTING_WHEEL_BREAKAGE);
        addServerRow("server.damage.propellerDamageFactor", "propellerDamageFactor", ConfigSystem.settings.damage.propellerDamageFactor, LanguageSystem.GUI_CONFIG_SETTING_PROPELLER_DAMAGE_FACTOR);
        addServerRow("server.damage.crashDamageFactor", "crashDamageFactor", ConfigSystem.settings.damage.crashDamageFactor, LanguageSystem.GUI_CONFIG_SETTING_CRASH_DAMAGE_FACTOR);
        addServerRow("server.damage.bulletDamageFactor", "bulletDamageFactor", ConfigSystem.settings.damage.bulletDamageFactor, LanguageSystem.GUI_CONFIG_SETTING_BULLET_DAMAGE_FACTOR);
        addServerRow("server.damage.wheelDamageFactor", "wheelDamageFactor", ConfigSystem.settings.damage.wheelDamageFactor, LanguageSystem.GUI_CONFIG_SETTING_WHEEL_DAMAGE_FACTOR);
    }

    private void addSettingHeader(List<SettingRow> rows, LanguageEntry title) {
        rows.add(new SettingRow(title));
    }

    private void addAircraftControlModeRow() {
        clientSettingRows.add(new SettingRow("client.control.aircraftControlMode", "aircraftControlMode", LanguageSystem.GUI_CONFIG_SETTING_AIRCRAFT_CONTROL_MODE, LanguageSystem.GUI_CONFIG_SETTING_AIRCRAFT_CONTROL_MODE_TOOLTIP.getCurrentValue(), SettingType.AIRCRAFT_CONTROL_MODE));
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
        int rowControlHeight = localScale(14);
        addComponent(controlSelectionFaultLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(20), LIST_WIDTH_WITH_NAV, localScale(80), "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.8F));
        addComponent(joystickHeaderLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(2), LIST_WIDTH_WITH_NAV, rowControlHeight, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
        addComponent(joystickBackButton = new TextButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(2), localScale(56), rowControlHeight, LanguageSystem.GUI_CONFIG_BACK.getCurrentValue()) {
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
        addComponent(deadzoneDownButton = new TextButton(guiLeft + LIST_X_WITH_NAV + localScale(70), guiTop + CONTENT_TOP + localScale(2), localScale(16), rowControlHeight, "<") {
            @Override
            public void onClicked(boolean leftSide) {
                stepDeadzone(-1);
            }
        });
        addComponent(deadzoneValueLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + localScale(90), guiTop + CONTENT_TOP + localScale(2), localScale(160), rowControlHeight, "", COLOR_TEXT, TextAlignment.CENTERED, 0.85F));
        addComponent(deadzoneUpButton = new TextButton(guiLeft + LIST_X_WITH_NAV + localScale(250), guiTop + CONTENT_TOP + localScale(2), localScale(16), rowControlHeight, ">") {
            @Override
            public void onClicked(boolean leftSide) {
                stepDeadzone(1);
            }
        });
        addComponent(joystickColumnIndexLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(28), localScale(24), rowControlHeight, "#", COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F));
        addComponent(joystickColumnNameLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + localScale(28), guiTop + CONTENT_TOP + localScale(28), localScale(118), rowControlHeight, LanguageSystem.GUI_CONFIG_JOYSTICK_NAME.getCurrentValue(), COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F));
        addComponent(joystickColumnStateLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + localScale(150), guiTop + CONTENT_TOP + localScale(28), localScale(60), rowControlHeight, LanguageSystem.GUI_CONFIG_JOYSTICK_STATE.getCurrentValue(), COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F));
        addComponent(joystickColumnAssignmentLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + localScale(216), guiTop + CONTENT_TOP + localScale(28), localScale(120), rowControlHeight, LanguageSystem.GUI_CONFIG_JOYSTICK_ASSIGNMENT.getCurrentValue(), COLOR_DIM_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F));

        for (int i = 0; i < SETTING_ROWS_WITH_NAV; ++i) {
            GUIComponentButton deviceButton = new TextButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(24) + ROW_HEIGHT * i, LIST_WIDTH_WITH_NAV - localScale(24), rowControlHeight, "") {
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
            GUIComponentButton componentButton = new TextButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(46) + ROW_HEIGHT * i, localScale(140), rowControlHeight, "") {
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
            SolidRect stateBack = new SolidRect(guiLeft + LIST_X_WITH_NAV + localScale(150), guiTop + CONTENT_TOP + localScale(48) + ROW_HEIGHT * i, localScale(50), localScale(8), COLOR_SCROLL_TRACK, 0.75F);
            joystickStateBacks.add(stateBack);
            addComponent(stateBack);
            SolidRect stateFill = new SolidRect(guiLeft + LIST_X_WITH_NAV + localScale(175), guiTop + CONTENT_TOP + localScale(48) + ROW_HEIGHT * i, localScale(1), localScale(8), COLOR_CHANGED, 0.9F);
            joystickStateFills.add(stateFill);
            addComponent(stateFill);
            TextLabel assignmentLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV + localScale(216), guiTop + CONTENT_TOP + localScale(45) + ROW_HEIGHT * i, localScale(116), rowControlHeight, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.75F);
            joystickAssignmentLabels.add(assignmentLabel);
            addComponent(assignmentLabel);
        }

        addComponent(joystickAssignmentPromptLabel = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(2), LIST_WIDTH_WITH_NAV, rowControlHeight, LanguageSystem.GUI_CONFIG_JOYSTICK_CHOOSEMAP.getCurrentValue(), COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
        addComponent(clearAssignmentButton = new FlatButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONFIG_GUI_HEIGHT - localScale(30), localScale(120), localScale(18), LanguageSystem.GUI_CONFIG_JOYSTICK_CLEAR.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                clearJoystickAssignment();
            }
        });
        addComponent(cancelAssignmentButton = new FlatButton(guiLeft + CONFIG_GUI_WIDTH - localScale(142), guiTop + CONFIG_GUI_HEIGHT - localScale(30), localScale(120), localScale(18), LanguageSystem.GUI_CONFIG_JOYSTICK_CANCEL.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                joystickComponentId = -1;
                calibrating = false;
                joystickAssignmentScroll = 0;
            }
        });
        addJoystickAssignmentRows();

        addComponent(confirmBoundsButton = new FlatButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONFIG_GUI_HEIGHT - localScale(30), localScale(120), localScale(18), LanguageSystem.GUI_CONFIRM.getCurrentValue()) {
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
        addComponent(invertAxisButton = new FlatButton(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(104), localScale(180), localScale(18), LanguageSystem.GUI_CONFIG_JOYSTICK_AXISMODE.getCurrentValue() + LanguageSystem.GUI_CONFIG_JOYSTICK_NORMAL.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                text = LanguageSystem.GUI_CONFIG_JOYSTICK_AXISMODE.getCurrentValue() + (text.contains(LanguageSystem.GUI_CONFIG_JOYSTICK_INVERT.getCurrentValue()) ? LanguageSystem.GUI_CONFIG_JOYSTICK_NORMAL.getCurrentValue() : LanguageSystem.GUI_CONFIG_JOYSTICK_INVERT.getCurrentValue());
            }
        });
        addComponent(axisMaxBoundsTextBox = new GUIComponentTextBox(this, guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(52), localScale(160), rowControlHeight, "0.0", COLOR_TEXT, 16, 0, 0, 0, 0));
        axisMaxBoundsTextBox.enabled = false;
        addComponent(axisMinBoundsTextBox = new GUIComponentTextBox(this, guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(78), localScale(160), rowControlHeight, "0.0", COLOR_TEXT, 16, 0, 0, 0, 0));
        axisMinBoundsTextBox.enabled = false;
        addComponent(joystickCalibrationLabel1 = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(18), LIST_WIDTH_WITH_NAV, rowControlHeight, LanguageSystem.GUI_CONFIG_JOYSTICK_CALIBRATE1.getCurrentValue(), COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
        addComponent(joystickCalibrationLabel2 = new TextLabel(guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(32), LIST_WIDTH_WITH_NAV, rowControlHeight, LanguageSystem.GUI_CONFIG_JOYSTICK_CALIBRATE2.getCurrentValue(), COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F));
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
                if (!row.header) {
                    updateRowBackground(rowIndex, i, guiLeft + listX, guiTop + listY + ROW_HEIGHT * rowIndex, listWidth);
                }
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
                setComponentPosition(button, guiLeft + LIST_X_WITH_NAV, guiTop + CONTENT_TOP + localScale(24) + ROW_HEIGHT * i);
            }
        }
        updateScrollBar(joystickSelectionScroll, visibleJoystickNames.size(), SETTING_ROWS_WITH_NAV, CONTENT_TOP + localScale(24), ROW_HEIGHT);
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
                int y = guiTop + CONTENT_TOP + localScale(46) + ROW_HEIGHT * i;
                button.text = String.format(Locale.ROOT, "%02d  %s", componentIndex + 1, InterfaceManager.inputInterface.getJoystickComponentName(selectedJoystickName, componentIndex));
                setComponentPosition(button, guiLeft + LIST_X_WITH_NAV, y);
                setComponentPosition(back, guiLeft + LIST_X_WITH_NAV + localScale(150), y + localScale(3));
                setComponentPosition(fill, guiLeft + LIST_X_WITH_NAV + localScale(175), y + localScale(3));
                setComponentPosition(assignment, guiLeft + LIST_X_WITH_NAV + localScale(216), y);
                assignment.text = getJoystickComponentAssignment(componentIndex);
                updateJoystickComponentState(componentIndex, back, fill);
            }
        }
        updateScrollBar(joystickComponentScroll, visibleJoystickComponentIndexes.size(), rowsToRender, CONTENT_TOP + localScale(46), ROW_HEIGHT);
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
            int backgroundPadding = localScale(4);
            background.visible = true;
            setComponentBounds(background, x - backgroundPadding, y - backgroundPadding, width + backgroundPadding * 2, ROW_HEIGHT);
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
                GUIComponentTextBox box = (GUIComponentTextBox) component;
                if (box instanceof NumericValueBox && box.focused) {
                    ((NumericValueBox) box).commitText();
                }
                box.focused = false;
            }
        }
    }

    private void commitFocusedNumericTextBoxes(int mouseX, int mouseY) {
        for (AGUIComponent component : components) {
            if (component instanceof NumericValueBox) {
                NumericValueBox box = (NumericValueBox) component;
                if (box.focused && !box.isMouseInBounds(mouseX, mouseY)) {
                    box.commitText();
                }
            }
        }
    }

    private SettingRow getSliderRowAt(int mouseX, int mouseY) {
        SettingRow row = getSliderRowAt(clientSettingRows, mouseX, mouseY);
        if (row == null) {
            row = getSliderRowAt(serverSettingRows, mouseX, mouseY);
        }
        if (row == null) {
            row = getSliderRowAt(renderingSettingRows, mouseX, mouseY);
        }
        return row == null ? getSliderRowAt(developmentSettingRows, mouseX, mouseY) : row;
    }

    private SettingRow getSliderRowAt(List<SettingRow> rows, int mouseX, int mouseY) {
        for (SettingRow row : rows) {
            if (row.isSliderMouseInBounds(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
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
            int centerOffset = localScale(25);
            int barHeight = localScale(8);
            int width = (int) (pollData * centerOffset);
            if (width >= 0) {
                setComponentBounds(fill, (int) back.position.x + centerOffset, (int) -back.position.y, width, barHeight);
            } else {
                setComponentBounds(fill, (int) back.position.x + centerOffset + width, (int) -back.position.y, -width, barHeight);
            }
            fill.color.setTo(COLOR_CHANGED);
        } else {
            setComponentBounds(fill, (int) back.position.x + localScale(20), (int) -back.position.y, localScale(10), localScale(8));
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
        return !metadata.bounded || (direction < 0 ? value > metadata.minimum : value < metadata.maximum);
    }

    private double stepNumber(double currentValue, NumericMetadata metadata, int direction) {
        double newValue = currentValue + metadata.step * direction;
        if (metadata.bounded) {
            newValue = currentValue < metadata.minimum || currentValue > metadata.maximum ? clamp(currentValue, metadata.minimum, metadata.maximum) : clamp(newValue, metadata.minimum, metadata.maximum);
        }
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

    private String formatNumericValue(double value, NumericMetadata metadata) {
        return metadata.percent ? Math.round(value * 100) + "%" : formatNumber(value);
    }

    private NumericMetadata getNumericMetadata(String fieldName) {
        if ("soundVolume".equals(fieldName) || "radioVolume".equals(fieldName)) {
            return new NumericMetadata(0.0D, 1.5D, 0.01D, true, true, true);
        } else if ("joystickDeadZone".equals(fieldName)) {
            return new NumericMetadata(0.0D, 1.0D, 0.01D, true, false, false);
        } else if ("steeringControlRate".equals(fieldName) || "steeringReturnRate".equals(fieldName)) {
            return new NumericMetadata(0.0D, 0.0D, 0.5D, false, false, false);
        } else if ("flightControlRate".equals(fieldName)) {
            return new NumericMetadata(0.0D, 0.0D, 0.5D, false, false, false);
        } else if ("mouseYokeRate".equals(fieldName)) {
            return new NumericMetadata(0.0D, 0.0D, 0.5D, false, false, false);
        } else if ("DismountSafteySpeed".equals(fieldName)) {
            return new NumericMetadata(0.0D, 0.0D, 0.5D, false, false, false);
        } else if ("roadMaxLength".equals(fieldName)) {
            return new NumericMetadata(0.0D, 0.0D, 8.0D, false, false, false);
        } else if ("seaLevel".equals(fieldName)) {
            return new NumericMetadata(0.0D, 0.0D, 8.0D, false, false, false);
        } else if ("maxFlightHeight".equals(fieldName)) {
            return new NumericMetadata(0.0D, 0.0D, 50.0D, false, false, false);
        } else if (fieldName.endsWith("Factor")) {
            return new NumericMetadata(0.0D, 0.0D, 0.5D, false, false, false);
        } else {
            return new NumericMetadata(0.0D, 0.0D, 1.0D, false, false, false);
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
            case "arcadeMode": return LanguageSystem.GUI_CONFIG_SETTING_ARCADE_MODE;
            case "aimAssist": return LanguageSystem.GUI_CONFIG_SETTING_AIM_ASSIST;
            case "classicJystk": return LanguageSystem.GUI_CONFIG_SETTING_CLASSIC_JYSTK;
            case "steeringControlRate": return LanguageSystem.GUI_CONFIG_SETTING_STEERING_CONTROL_RATE;
            case "steeringReturnRate": return LanguageSystem.GUI_CONFIG_SETTING_STEERING_RETURN_RATE;
            case "flightControlRate": return LanguageSystem.GUI_CONFIG_SETTING_FLIGHT_CONTROL_RATE;
            case "mouseYokeRate": return LanguageSystem.GUI_CONFIG_SETTING_MOUSE_YOKE_RATE;
            case "DismountSafteySpeed": return LanguageSystem.GUI_CONFIG_SETTING_DISMOUNT_SAFETY_SPEED;
            case "joystickDeadZone": return LanguageSystem.GUI_CONFIG_SETTING_JOYSTICK_DEADZONE;
            case "soundVolume": return LanguageSystem.GUI_CONFIG_SETTING_SOUND_VOLUME;
            case "radioVolume": return LanguageSystem.GUI_CONFIG_SETTING_RADIO_VOLUME;
            default: return new LanguageEntry(fieldName);
        }
    }

    private String getKeyboardControlName(ControlsKeyboard control) {
        return control.config.isMouseButton ? getMouseButtonName(control.config.keyCode) : InterfaceManager.inputInterface.getNameForKeyCode(control.config.keyCode);
    }

    private String getDynamicControlText(ControlsKeyboardDynamic control) {
        String modName = control.modControl.config.isMouseButton ? getMouseButtonName(control.modControl.config.keyCode) : InterfaceManager.inputInterface.getNameForKeyCode(control.modControl.config.keyCode);
        String mainName = control.mainControl.config.isMouseButton ? getMouseButtonName(control.mainControl.config.keyCode) : InterfaceManager.inputInterface.getNameForKeyCode(control.mainControl.config.keyCode);
        return modName + " + " + mainName;
    }

    private String getMouseButtonName(int mouseButton) {
        String rawName = InterfaceManager.inputInterface.getNameForMouseButton(mouseButton);
        if ("MOUSE_LEFT".equals(rawName)) {
            return LanguageSystem.GUI_CONFIG_MOUSE_LEFT.getCurrentValue();
        } else if ("MOUSE_RIGHT".equals(rawName)) {
            return LanguageSystem.GUI_CONFIG_MOUSE_RIGHT.getCurrentValue();
        } else if ("MOUSE_MIDDLE".equals(rawName)) {
            return LanguageSystem.GUI_CONFIG_MOUSE_MIDDLE.getCurrentValue();
        } else if (rawName != null && rawName.startsWith("MOUSE_")) {
            try {
                return String.format(Locale.ROOT, LanguageSystem.GUI_CONFIG_MOUSE_NUMBERED.getCurrentValue(), Integer.parseInt(rawName.substring(6)));
            } catch (NumberFormatException e) {
                return rawName;
            }
        }
        return rawName;
    }

    private void setComponentPosition(AGUIComponent component, int x, int y) {
        component.position.x = x;
        component.position.y = -y;
        if (component instanceof TextButton) {
            component.textPosition.set(x + component.width / 2F, textButtonTextY(y, component.height), component.textPosition.z);
        } else if (component instanceof GUIComponentButton) {
            GUIComponentButton button = (GUIComponentButton) component;
            component.textPosition.set(button.centeredText ? x + component.width / 2 : x, centeredTextY(y, component.height), component.textPosition.z);
        } else if (component instanceof GUIComponentTextBox) {
            component.textPosition.set(component instanceof NumericValueBox ? x + component.width / 2F : x, centeredTextY(y, component.height), component.textPosition.z);
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
        MODE,
        AIRCRAFT_CONTROL_MODE
    }

    private static class NumericMetadata {
        private final double minimum;
        private final double maximum;
        private final double step;
        private final boolean bounded;
        private final boolean slider;
        private final boolean percent;

        private NumericMetadata(double minimum, double maximum, double step, boolean bounded, boolean slider, boolean percent) {
            this.minimum = minimum;
            this.maximum = maximum;
            this.step = step;
            this.bounded = bounded;
            this.slider = slider;
            this.percent = percent;
        }
    }

    private class SettingRow {
        private final String id;
        private final String fieldName;
        private final JSONConfigEntry<?> entry;
        private final LanguageEntry labelLanguage;
        private final NumericMetadata metadata;
        private final SettingType type;
        private final boolean header;
        private final HoverLabel label;
        private TextButton toggleButton;
        private TextButton minusButton;
        private TextButton plusButton;
        private TextButton modeButton;
        private TextLabel valueLabel;
        private GUIComponentTextBox valueInputBox;
        private SliderBar sliderBar;

        private SettingRow(LanguageEntry headerLanguage) {
            this.id = "header." + headerLanguage.key;
            this.fieldName = "";
            this.entry = null;
            this.labelLanguage = headerLanguage;
            this.metadata = null;
            this.type = null;
            this.header = true;
            addComponent(label = new HoverLabel(0, 0, 160, 16, "", COLOR_DIM_TEXT, TextAlignment.CENTERED, 1.0F, null));
        }

        private SettingRow(String id, String fieldName, LanguageEntry labelLanguage, String tooltipText, SettingType type) {
            this.id = id;
            this.fieldName = fieldName;
            this.entry = null;
            this.labelLanguage = labelLanguage;
            this.metadata = null;
            this.type = type;
            this.header = false;
            addComponent(label = new HoverLabel(0, 0, 160, 14, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F, tooltipText));
            if (type == SettingType.AIRCRAFT_CONTROL_MODE) {
                addComponent(modeButton = new TextButton(0, 0, 112, 14, "") {
                    @Override
                    public void onClicked(boolean leftSide) {
                        cycleAircraftControlMode();
                    }
                });
            }
        }

        private SettingRow(String id, String fieldName, JSONConfigEntry<?> entry, LanguageEntry labelLanguage, NumericMetadata metadata, SettingType type) {
            this.id = id;
            this.fieldName = fieldName;
            this.entry = entry;
            this.labelLanguage = labelLanguage;
            this.metadata = metadata;
            this.type = type;
            this.header = false;
            addComponent(label = new HoverLabel(0, 0, 160, 14, "", COLOR_TEXT, TextAlignment.LEFT_ALIGNED, 0.85F, entry.comment));
            if (type == SettingType.BOOLEAN) {
                addComponent(toggleButton = new TextButton(0, 0, 70, 14, "") {
                    @Override
                    public void onClicked(boolean leftSide) {
                        toggleBoolean();
                    }
                });
            } else if (type == SettingType.NUMBER) {
                if (metadata.slider) {
                    addComponent(sliderBar = new SliderBar(0, 0, 72, 14, this));
                    addComponent(valueLabel = new TextLabel(0, 0, 34, 14, "", COLOR_TEXT, TextAlignment.CENTERED, 0.85F));
                } else {
                    addComponent(minusButton = new TextButton(0, 0, 16, 14, "<") {
                        @Override
                        public void onClicked(boolean leftSide) {
                            stepNumeric(-1);
                        }
                    });
                    addComponent(valueInputBox = new NumericValueBox(this));
                    addComponent(plusButton = new TextButton(0, 0, 16, 14, ">") {
                        @Override
                        public void onClicked(boolean leftSide) {
                            stepNumeric(1);
                        }
                    });
                }
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

        private int getAircraftControlMode() {
            if (ConfigSystem.client.controlSettings.arcadeMode.value) {
                return 2;
            } else if (ConfigSystem.client.controlSettings.mouseYoke.value) {
                return 1;
            } else {
                return 0;
            }
        }

        private String getAircraftControlModeText(int mode) {
            switch (mode) {
                case 1:
                    return LanguageSystem.GUI_CONFIG_AIRCRAFT_CONTROL_CLASSIC.getCurrentValue();
                case 2:
                    return LanguageSystem.GUI_CONFIG_AIRCRAFT_CONTROL_ARCADE.getCurrentValue();
                default:
                    return LanguageSystem.GUI_CONFIG_AIRCRAFT_CONTROL_OFF.getCurrentValue();
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
            if (header) {
                return;
            }
            if (toggleButton != null) {
                toggleButton.visible = visible;
            }
            if (minusButton != null) {
                minusButton.visible = visible;
                plusButton.visible = visible;
                valueInputBox.visible = visible;
            }
            if (sliderBar != null) {
                sliderBar.visible = visible;
                valueLabel.visible = visible;
            }
            if (modeButton != null) {
                modeButton.visible = visible;
            }
        }

        private void setPosition(int x, int y, int width) {
            if (header) {
                setComponentBounds(label, x, y, width, localScale(16));
                return;
            }
            int rowControlHeight = localScale(14);
            int controlRight = x + width - localScale(8);
            int labelWidth = Math.max(localScale(80), width - localScale(145));
            setComponentBounds(label, x, y, labelWidth, rowControlHeight);
            if (toggleButton != null) {
                int toggleWidth = localScale(68);
                setComponentBounds(toggleButton, controlRight - toggleWidth, y, toggleWidth, rowControlHeight);
            }
            if (minusButton != null) {
                int controlWidth = localScale(48);
                int buttonWidth = localScale(10);
                int valueWidth = localScale(28);
                int controlLeft = controlRight - localScale(34) - controlWidth / 2;
                setComponentBounds(minusButton, controlLeft, y, buttonWidth, rowControlHeight);
                setComponentBounds(valueInputBox, controlLeft + buttonWidth, y, valueWidth, rowControlHeight);
                valueInputBox.textPosition.y = textButtonTextY(y, rowControlHeight);
                setComponentBounds(plusButton, controlLeft + buttonWidth + valueWidth, y, buttonWidth, rowControlHeight);
            }
            if (sliderBar != null) {
                int controlLeft = controlRight - localScale(76);
                int sliderWidth = localScale(42);
                setComponentBounds(sliderBar, controlLeft, y - localScale(2), sliderWidth, rowControlHeight);
                setComponentBounds(valueLabel, controlLeft + localScale(46), y, localScale(30), rowControlHeight);
                valueLabel.textPosition.y = textButtonTextY(y, rowControlHeight);
            }
            if (modeButton != null) {
                int modeWidth = type == SettingType.AIRCRAFT_CONTROL_MODE ? localScale(86) : localScale(68);
                int modeCenterOffset = type == SettingType.AIRCRAFT_CONTROL_MODE ? (modeWidth - localScale(68)) / 2 : 0;
                setComponentBounds(modeButton, controlRight - modeWidth + modeCenterOffset, y, modeWidth, rowControlHeight);
            }
        }

        private void updateState() {
            label.text = labelLanguage.getCurrentValue();
            if (header) {
                label.color = COLOR_DIM_TEXT;
                return;
            }
            label.color = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
            if (toggleButton != null) {
                toggleButton.text = Boolean.TRUE.equals(entry.value) ? LanguageSystem.GUI_CONFIG_ON.getCurrentValue() : LanguageSystem.GUI_CONFIG_OFF.getCurrentValue();
                toggleButton.textColorOverride = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
            }
            if (minusButton != null) {
                double savedValue = ((Number) entry.value).doubleValue();
                if (!valueInputBox.focused) {
                    valueInputBox.setText(formatNumber(savedValue));
                }
                valueInputBox.fontColor = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
                double value = getNumericValueForStep();
                minusButton.enabled = canStep(value, metadata, -1);
                plusButton.enabled = canStep(value, metadata, 1);
                minusButton.textColorOverride = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
                plusButton.textColorOverride = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
            }
            if (sliderBar != null) {
                double value = ((Number) entry.value).doubleValue();
                valueLabel.text = formatNumericValue(value, metadata);
                valueLabel.color = changedSettings.contains(id) ? COLOR_CHANGED : COLOR_TEXT;
            }
            if (modeButton != null) {
                modeButton.text = type == SettingType.AIRCRAFT_CONTROL_MODE ? getAircraftControlModeText(getAircraftControlMode()) : getRenderingModeText(ConfigSystem.client.renderingSettings.renderingMode.value);
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

        private void cycleAircraftControlMode() {
            int newMode = (getAircraftControlMode() + 1) % 3;
            if (newMode == 0) {
                ControlSystem.setMouseYokeEnabled(false, false);
                ConfigSystem.client.controlSettings.arcadeMode.value = false;
            } else if (newMode == 1) {
                ControlSystem.setMouseYokeEnabled(true, false);
            } else {
                ControlSystem.setMouseYokeEnabled(false, false);
                ConfigSystem.client.controlSettings.arcadeMode.value = true;
            }
            changedSettings.add(id);
            ConfigSystem.saveToDisk();
        }

        private boolean isSliderMouseInBounds(int mouseX, int mouseY) {
            return sliderBar != null && sliderBar.visible && sliderBar.isMouseInBounds(mouseX, mouseY);
        }

        private void updateSliderFromMouse(int mouseX) {
            double sliderRange = metadata.maximum - metadata.minimum;
            double sliderPercent = clamp((mouseX - sliderBar.position.x) / sliderBar.width, 0.0D, 1.0D);
            setNumericValue(roundForStep(metadata.minimum + sliderRange * sliderPercent, metadata.step));
        }

        private void setNumericValueFromText(String text) {
            if (text.isEmpty() || "-".equals(text) || ".".equals(text) || "-.".equals(text)) {
                return;
            }
            try {
                setNumericValue(Double.parseDouble(text));
            } catch (NumberFormatException e) {
                //Ignore transient invalid edits.  Validation keeps these rare, but paste/input methods may vary.
            }
        }

        private double getNumericValueForStep() {
            if (valueInputBox != null && valueInputBox.focused) {
                String text = valueInputBox.getText();
                if (!text.isEmpty() && !"-".equals(text) && !".".equals(text) && !"-.".equals(text)) {
                    try {
                        return Double.parseDouble(text);
                    } catch (NumberFormatException e) {
                        //Fall back to saved config value below.
                    }
                }
            }
            return ((Number) entry.value).doubleValue();
        }

        @SuppressWarnings("unchecked")
        private void setNumericValue(double newValue) {
            if (metadata.bounded) {
                newValue = clamp(newValue, metadata.minimum, metadata.maximum);
            }
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

        private void stepNumeric(int direction) {
            double newValue = stepNumber(getNumericValueForStep(), metadata, direction);
            setNumericValue(newValue);
            if (valueInputBox != null) {
                valueInputBox.setText(formatNumber(newValue));
            }
        }
    }

    private class NumericValueBox extends GUIComponentTextBox {
        private final SettingRow row;

        private NumericValueBox(SettingRow row) {
            super(GUIConfig.this, 0, 0, 46, 14, "", COLOR_TEXT, 12, 0, 0, 0, 0);
            this.row = row;
        }

        @Override
        public boolean isTextValid(String newText) {
            return newText.isEmpty() || newText.matches("-?\\d*(\\.\\d*)?");
        }

        @Override
        public void handleTextChange() {
        }

        @Override
        public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
            if (typedCode == 257 || typedCode == 335 || typedCode == 28 || typedCode == 156) {
                commitText();
            } else if (control == null && typedChar != 0 && !Character.isDigit(typedChar) && typedChar != '-' && typedChar != '.') {
                return;
            } else {
                super.handleKeyTyped(typedChar, typedCode, control);
            }
        }

        private void commitText() {
            row.setNumericValueFromText(getText());
            setText(formatNumber(((Number) row.entry.value).doubleValue()));
            focused = false;
            editingText = false;
        }

        @Override
        public void renderText(boolean renderTextLit, int worldLightValue) {
            String displayedText = focused && AGUIBase.inClockPeriod(20, 10) ? text + "_" : text;
            textPosition.x = position.x + width / 2F;
            RenderText.drawText(displayedText, null, textPosition, fontColor, TextAlignment.CENTERED, textScale(0.85F), false, 0, renderTextLit || ignoreGUILightingState, worldLightValue);
        }
    }

    private class SliderBar extends AGUIComponent {
        private final SettingRow row;
        private final RenderableData trackRenderable;
        private final RenderableData fillRenderable;
        private final RenderableData thumbRenderable;

        private SliderBar(int x, int y, int width, int height, SettingRow row) {
            super(x, y, width, height);
            this.row = row;
            this.trackRenderable = createRectRenderable(COLOR_DIM_TEXT, 1.0F);
            this.fillRenderable = createRectRenderable(COLOR_TEXT, 1.0F);
            this.thumbRenderable = createRectRenderable(COLOR_SCROLL_THUMB, 1.0F);
        }

        @Override
        public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
            double value = ((Number) row.entry.value).doubleValue();
            double ratio = clamp((value - row.metadata.minimum) / (row.metadata.maximum - row.metadata.minimum), 0.0D, 1.0D);
            int x = (int) position.x;
            int y = (int) -position.y;
            int trackHeight = localScale(2);
            int thumbWidth = Math.max(localScale(3), localScale(4));
            int thumbHeight = Math.max(localScale(8), height - localScale(3));
            int thumbX = x + (int) Math.round(ratio * (width - thumbWidth));
            int trackY = y + height / 2 - trackHeight;
            drawRect(trackRenderable, x, trackY, width, trackHeight, COLOR_DIM_TEXT, 0.55F, getZOffset(), blendingEnabled);
            drawRect(fillRenderable, x, trackY, thumbX - x + thumbWidth / 2, trackHeight, changedSettings.contains(row.id) ? COLOR_CHANGED : COLOR_TEXT, 0.85F, getZOffset() + 1, blendingEnabled);
            drawRect(thumbRenderable, thumbX, y + (height - thumbHeight) / 2, thumbWidth, thumbHeight, COLOR_SCROLL_THUMB, isMouseInBounds(mouseX, mouseY) ? 1.0F : 0.85F, getZOffset() + 2, blendingEnabled);
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
            setComponentBounds(label, x, y, width, localScale(16));
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
            int boxWidth = localScale(100);
            setComponentBounds(label, x, y, width - localScale(115), localScale(14));
            setComponentBounds(box, x + width - boxWidth - localScale(4), y, boxWidth, localScale(14));
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
            int valueWidth = localScale(100);
            setComponentBounds(label, x, y, width - localScale(115), localScale(14));
            setComponentBounds(value, x + width - valueWidth - localScale(4), y, valueWidth, localScale(14));
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
            setComponentBounds(button, x, y, width - localScale(24), localScale(14));
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
            setText(getMouseButtonName(mouseButton));
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
                int underlineWidth = Math.min(width - localScale(12), Math.max(localScale(18), (int) (RenderText.getStringWidth(underlineText, null) * textScale(0.85F)) + localScale(6)));
                int underlineX = (int) position.x + (width - underlineWidth) / 2;
                drawRect(underlineRenderable, underlineX, (int) -position.y + height - localScale(3), underlineWidth, localScale(1), fontColor, 1.0F, getZOffset());
            }
        }

        @Override
        public void renderText(boolean renderTextLit, int worldLightValue) {
            textPosition.x = position.x + width / 2F;
            RenderText.drawText(focused ? "_" : text, null, textPosition, fontColor, TextAlignment.CENTERED, textScale(0.85F), true, width, renderTextLit || ignoreGUILightingState, worldLightValue);
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
            if (outlined) {
                drawOutline(underlineRenderable, (int) position.x, (int) -position.y, width, height, enabled ? textColorOverride : COLOR_DIM_TEXT, active || enabled && isMouseInBounds(mouseX, mouseY) ? 1.0F : 0.65F, getZOffset(), blendingEnabled);
            } else if (!blendingEnabled) {
                if (enabled && (active || isMouseInBounds(mouseX, mouseY))) {
                    drawRect(underlineRenderable, (int) position.x, (int) -position.y + height - localScale(2), width, localScale(1), textColorOverride, 1.0F, getZOffset());
                }
            }
        }

        @Override
        public void renderText(boolean renderTextLit, int worldLightValue) {
            RenderText.drawText(text, null, textPosition, enabled ? textColorOverride : COLOR_DIM_TEXT, TextAlignment.CENTERED, textScale(0.85F), true, width, renderTextLit || ignoreGUILightingState, worldLightValue);
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
            ColorRGB color = active ? COLOR_BUTTON_ACTIVE : enabled && isMouseInBounds(mouseX, mouseY) ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
            drawRect(backgroundRenderable, (int) position.x, (int) -position.y, width, height, color, BUTTON_ALPHA, getZOffset(), blendingEnabled);
            if (!blendingEnabled) {
                if (outlined) {
                    drawOutline(backgroundRenderable, (int) position.x, (int) -position.y, width, height, COLOR_OUTLINE, 1.0F, getZOffset() + 1);
                }
                if (locked) {
                    drawLockIcon((int) position.x + width - localScale(16), (int) -position.y + localScale(5));
                }
            }
        }

        @Override
        public void renderText(boolean renderTextLit, int worldLightValue) {
            int lockInset = locked ? localScale(18) : 0;
            textPosition.x = position.x + (width - lockInset) / 2F;
            RenderText.drawText(text, null, textPosition, locked ? COLOR_DIM_TEXT : COLOR_TEXT, TextAlignment.CENTERED, textScale(0.8F), true, width - (locked ? localScale(18) : localScale(4)), renderTextLit || ignoreGUILightingState, worldLightValue);
        }

        private void drawLockIcon(int x, int y) {
            drawRect(lockRenderable, x + localScale(2), y + localScale(5), localScale(10), localScale(7), locked ? COLOR_DIM_TEXT : COLOR_TEXT, 1.0F, getZOffset() + 1);
            drawRect(lockRenderable, x + localScale(3), y + localScale(2), localScale(2), localScale(4), locked ? COLOR_DIM_TEXT : COLOR_TEXT, 1.0F, getZOffset() + 1);
            drawRect(lockRenderable, x + localScale(9), y + localScale(2), localScale(2), localScale(4), locked ? COLOR_DIM_TEXT : COLOR_TEXT, 1.0F, getZOffset() + 1);
            drawRect(lockRenderable, x + localScale(4), y + localScale(1), localScale(6), localScale(2), locked ? COLOR_DIM_TEXT : COLOR_TEXT, 1.0F, getZOffset() + 1);
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
            RenderText.drawText(text, null, textPosition, color, alignment, textScale(scale), true, width, renderTextLit || ignoreGUILightingState, worldLightValue);
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
            drawRect(renderableData, (int) position.x, (int) -position.y, width, height, color, alpha, getZOffset(), blendingEnabled);
            if (outlineAlpha > 0) {
                drawOutline(renderableData, (int) position.x, (int) -position.y, width, height, COLOR_OUTLINE, outlineAlpha, getZOffset() + 1, blendingEnabled);
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
        data.transform.setTranslation(x, -y, getRectRenderZ(alpha, z));
        data.render();
    }

    private void drawRect(RenderableData data, int x, int y, int width, int height, ColorRGB color, float alpha, int z, boolean blendingEnabled) {
        if (shouldRenderInPass(alpha, blendingEnabled)) {
            drawRect(data, x, y, width, height, color, alpha, z);
        }
    }

    private void drawScaledRect(RenderableData data, double x, double y, int width, int height, double scaleX, double scaleY, ColorRGB color, float alpha, int z) {
        if (width <= 0 || height <= 0 || scaleX <= 0 || scaleY <= 0) {
            return;
        }
        data.vertexObject.setSpriteProperties(0, 0, 0, width, height, 7 / 256F, 7 / 256F, 8 / 256F, 8 / 256F);
        data.setColor(color);
        data.setAlpha(alpha);
        data.transform.resetTransforms();
        data.transform.setTranslation(x, -y, getRectRenderZ(alpha, z));
        data.transform.applyScaling(scaleX, scaleY, 1.0D);
        data.render();
    }

    private void drawScaledRect(RenderableData data, double x, double y, int width, int height, double scaleX, double scaleY, ColorRGB color, float alpha, int z, boolean blendingEnabled) {
        if (shouldRenderInPass(alpha, blendingEnabled)) {
            drawScaledRect(data, x, y, width, height, scaleX, scaleY, color, alpha, z);
        }
    }

    private void drawOutline(RenderableData data, int x, int y, int width, int height, ColorRGB color, float alpha, int z) {
        drawScaledRect(data, x, y, width, 1, 1.0D, OUTLINE_THICKNESS, color, alpha, z);
        drawScaledRect(data, x, y + height - OUTLINE_THICKNESS, width, 1, 1.0D, OUTLINE_THICKNESS, color, alpha, z);
        drawScaledRect(data, x, y, 1, height, OUTLINE_THICKNESS, 1.0D, color, alpha, z);
        drawScaledRect(data, x + width - OUTLINE_THICKNESS, y, 1, height, OUTLINE_THICKNESS, 1.0D, color, alpha, z);
    }

    private void drawOutline(RenderableData data, int x, int y, int width, int height, ColorRGB color, float alpha, int z, boolean blendingEnabled) {
        drawScaledRect(data, x, y, width, 1, 1.0D, OUTLINE_THICKNESS, color, alpha, z, blendingEnabled);
        drawScaledRect(data, x, y + height - OUTLINE_THICKNESS, width, 1, 1.0D, OUTLINE_THICKNESS, color, alpha, z, blendingEnabled);
        drawScaledRect(data, x, y, 1, height, OUTLINE_THICKNESS, 1.0D, color, alpha, z, blendingEnabled);
        drawScaledRect(data, x + width - OUTLINE_THICKNESS, y, 1, height, OUTLINE_THICKNESS, 1.0D, color, alpha, z, blendingEnabled);
    }

    private boolean shouldRenderInPass(float alpha, boolean blendingEnabled) {
        return alpha > 0.0F && (alpha < 1.0F) == blendingEnabled;
    }

    private int getRectRenderZ(float alpha, int z) {
        return alpha >= 1.0F ? z + DECORATION_Z_OFFSET : z;
    }
}
