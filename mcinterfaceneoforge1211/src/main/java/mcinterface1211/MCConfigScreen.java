package mcinterface1211;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import minecrafttransportsimulator.jsondefs.JSONConfigEntry;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.FlightControlMode;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboardDynamic;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Vanilla Minecraft-backed config screen for the 1.21.1 interface.
 * Values still load from and save to the existing MTS JSON config files.
 */
public class MCConfigScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int TAB_WIDTH = 98;
    private static final int WIDE_BUTTON_WIDTH = 150;
    private static final int VALUE_BUTTON_WIDTH = 105;
    private static final int VOLUME_SLIDER_WIDTH = 200;
    private static final int RESET_BUTTON_WIDTH = 60;
    private static final int ROW_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 56;
    private static final int FOOTER_HEIGHT = 33;
    private static final int LABEL_COLOR = 0xE0E0E0;
    private static final int MUTED_COLOR = 0xA0A0A0;
    private static final int ERROR_COLOR = 0xFF5555;
    private static final Component TITLE = Component.translatable("gui.config.title");

    private final Screen parentScreen;
    private ConfigList list;
    private Tab tab = Tab.RENDERING;
    private ControlMode controlMode = ControlMode.ROOT;
    private List<ControlsKeyboard> capturingKeyboardControls = List.of();
    private String selectedJoystickName;
    private int selectedJoystickComponent = -1;
    private ControlsJoystick calibratingControl;
    private double calibrationMin;
    private double calibrationMax;
    private boolean calibrationInverted;

    public MCConfigScreen(Screen parentScreen) {
        super(TITLE);
        this.parentScreen = parentScreen;
        if (!InterfaceManager.inputInterface.isJoystickSupportEnabled()) {
            InterfaceManager.inputInterface.initJoysticks();
        }
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        addHeaderButtons();
        list = addRenderableWidget(new ConfigList(width, height));
        populateList();
        addFooterButtons();
    }

    private void addHeaderButtons() {
        int tabWidth = Math.min(118, Math.max(44, (width - 40) / 4));
        int totalWidth = tabWidth * 4 + 12;
        int x = width / 2 - totalWidth / 2;
        addTabButton(Tab.RENDERING, x, tabWidth, translate(LanguageSystem.GUI_CONFIG_HEADER_RENDERING));
        addTabButton(Tab.CLIENT, x + tabWidth + 4, tabWidth, translate(LanguageSystem.GUI_CONFIG_HEADER_CONFIG));
        addTabButton(Tab.SERVER, x + (tabWidth + 4) * 2, tabWidth, translate(LanguageSystem.GUI_CONFIG_HEADER_SERVER));
        addTabButton(Tab.CONTROLS, x + (tabWidth + 4) * 3, tabWidth, translate(LanguageSystem.GUI_CONFIG_HEADER_CONTROLS));
    }

    private void addTabButton(Tab newTab, int x, int tabWidth, Component label) {
        Button button = Button.builder(label, pressed -> {
            tab = newTab;
            controlMode = ControlMode.ROOT;
            capturingKeyboardControls = List.of();
            selectedJoystickName = null;
            selectedJoystickComponent = -1;
            calibratingControl = null;
            rebuildWidgets();
        }).bounds(x, 30, tabWidth, BUTTON_HEIGHT).build();
        button.active = tab != newTab || (tab == Tab.CONTROLS && controlMode != ControlMode.ROOT);
        if (font.width(label) > tabWidth - 8) {
            button.setTooltip(Tooltip.create(label));
        }
        addRenderableWidget(button);
    }

    private void populateList() {
        if (tab == Tab.RENDERING) {
            addConfigRows(ConfigSystem.client.renderingSettings);
        } else if (tab == Tab.CLIENT) {
            addClientSettings();
        } else if (tab == Tab.SERVER) {
            addServerSettings();
        } else if (controlMode == ControlMode.ROOT) {
            addControlRoot();
        } else if (controlMode == ControlMode.KEYBOARD) {
            addKeyboardRows();
        } else if (controlMode == ControlMode.JOYSTICK_DEVICE) {
            addJoystickDevices();
        } else if (controlMode == ControlMode.JOYSTICK_COMPONENT) {
            addJoystickComponents();
        } else if (controlMode == ControlMode.JOYSTICK_ASSIGNMENT) {
            addJoystickAssignments();
        } else if (controlMode == ControlMode.JOYSTICK_CALIBRATION) {
            addJoystickCalibration();
        }
    }

    private void addFooterButtons() {
        if (tab == Tab.CONTROLS && controlMode == ControlMode.KEYBOARD) {
            addRenderableWidget(Button.builder(Component.translatable("controls.resetAll"), button -> {
                for (ControlsKeyboard control : ControlsKeyboard.values()) {
                    control.config.keyCode = InterfaceManager.inputInterface.getKeyCodeForName(control.defaultKeyName);
                    control.config.isMouseButton = false;
                    ConfigSystem.client.controls.keyboard.put(control.systemName, control.config);
                }
                ConfigSystem.saveToDisk();
                capturingKeyboardControls = List.of();
            }).bounds(width / 2 - 154, height - 26, 150, BUTTON_HEIGHT).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 + 4, height - 26, 150, BUTTON_HEIGHT)
                .build());
        } else {
            if (tab == Tab.CONTROLS && controlMode != ControlMode.ROOT) {
                addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> goBack())
                    .bounds(28, height - 26, TAB_WIDTH, BUTTON_HEIGHT)
                    .build());
            }
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, BUTTON_HEIGHT)
                .build());
        }
    }

    private void addConfigRows(Object configObject) {
        for (ConfigRow row : getConfigRows(configObject)) {
            list.addConfigEntry(new ConfigEntry(row));
        }
    }

    private void addConfigRows(Object configObject, boolean editable, String... fieldNames) {
        for (String fieldName : fieldNames) {
            ConfigRow row = getConfigRow(configObject, fieldName);
            if (row != null) {
                list.addConfigEntry(new ConfigEntry(row, editable));
            }
        }
    }

    private void addClientSettings() {
        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.client.general")));
        addConfigRows(ConfigSystem.client.controlSettings, true,
            "north360", "showAimHelper", "showTutorial", "showHoverTxt", "cullingWarn");
        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.client.audio")));
        addConfigRows(ConfigSystem.client.controlSettings, true, "soundVolume", "radioVolume");
    }

    private void addServerSettings() {
        boolean editable = !isRemoteServer();
        if (!editable) {
            list.addConfigEntry(new TextEntry(Component.translatable("gui.config.server.remote_read_only").getString()));
        }
        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.server.general")));
        for (ConfigRow row : getConfigRows(ConfigSystem.settings.general)) {
            list.addConfigEntry(new ConfigEntry(row, editable));
        }
        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.server.damage")));
        for (ConfigRow row : getConfigRows(ConfigSystem.settings.damage)) {
            list.addConfigEntry(new ConfigEntry(row, editable));
        }
        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.server.advanced")));
        list.addConfigEntry(new TextEntry(Component.translatable("gui.config.server.advanced_json").getString()));
    }

    private boolean isRemoteServer() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null && !minecraft.hasSingleplayerServer();
    }

    private void addControlRoot() {
        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.controls.behavior")));
        list.addConfigEntry(new ControlSchemeEntry());
        addConfigRows(ConfigSystem.client.controlSettings, true, "autostartEng", "DismountSafteySpeed");

        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.controls.bindings")));
        list.addConfigEntry(new DualButtonEntry(
            Component.translatable("gui.config.controls.mode.keyboard"),
            Component.translatable("gui.config.controls.mode.joystick"),
            button -> {
                controlMode = ControlMode.KEYBOARD;
                rebuildWidgets();
            },
            button -> {
                controlMode = ControlMode.JOYSTICK_DEVICE;
                selectedJoystickName = null;
                selectedJoystickComponent = -1;
                rebuildWidgets();
            }
        ));

        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.controls.ground")));
        addConfigRows(ConfigSystem.client.controlSettings, true,
            "simpleThrottle", "halfThrottle", "autoTrnSignals", "steeringControlRate", "steeringReturnRate");

        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.controls.aircraft")));
        addConfigRows(ConfigSystem.client.controlSettings, true, "heliAutoLevel", "flightControlRate", "mouseYokeRate");

        list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.category.controls.input")));
        addConfigRows(ConfigSystem.client.controlSettings, true, "kbOverride", "useShifter", "classicJystk", "joystickDeadZone");
    }

    private void addKeyboardRows() {
        addKeyboardRows(categoryLabel(ControlType.GENERAL), control -> ControlType.fromSystemName(control.systemName) == ControlType.GENERAL);
        addSharedVehicleKeyboardRows();
        addKeyboardRows(categoryLabel(ControlType.CAR), control -> ControlType.fromSystemName(control.systemName) == ControlType.CAR && !isSharedVehicleKeyboardControl(control));
        addKeyboardRows(categoryLabel(ControlType.AIRCRAFT), control -> ControlType.fromSystemName(control.systemName) == ControlType.AIRCRAFT && !isSharedVehicleKeyboardControl(control));
        list.addConfigEntry(new CategoryEntry(Component.literal("")));
        for (ControlsKeyboardDynamic dynamicControl : ControlsKeyboardDynamic.values()) {
            list.addConfigEntry(new TextEntry(translate(dynamicControl.language).getString() + ": " + bindingName(dynamicControl.modControl) + " + " + bindingName(dynamicControl.mainControl)));
        }
    }

    private void addKeyboardRows(Component categoryName, Predicate<ControlsKeyboard> filter) {
        boolean addedCategory = false;
        for (ControlsKeyboard control : ControlsKeyboard.values()) {
            if (filter.test(control)) {
                if (!addedCategory) {
                    list.addConfigEntry(new CategoryEntry(categoryName));
                    addedCategory = true;
                }
                list.addConfigEntry(new KeyboardEntry(control));
            }
        }
    }

    private void addSharedVehicleKeyboardRows() {
        boolean addedCategory = false;
        List<LanguageEntry> addedLanguages = new ArrayList<>();
        for (ControlsKeyboard control : ControlsKeyboard.values()) {
            if (isSharedVehicleKeyboardControl(control) && !addedLanguages.contains(control.language)) {
                if (!addedCategory) {
                    list.addConfigEntry(new CategoryEntry(Component.translatable("gui.config.controls.category.shared_vehicle")));
                    addedCategory = true;
                }
                addedLanguages.add(control.language);
                List<ControlsKeyboard> sharedControls = new ArrayList<>();
                for (ControlsKeyboard pairedControl : ControlsKeyboard.values()) {
                    if (isSharedVehicleKeyboardControl(pairedControl) && pairedControl.language == control.language) {
                        sharedControls.add(pairedControl);
                    }
                }
                list.addConfigEntry(new KeyboardEntry(sharedControls));
            }
        }
    }

    private void addJoystickDevices() {
        if (!InterfaceManager.inputInterface.isJoystickSupportEnabled()) {
            list.addConfigEntry(new TextEntry(InterfaceManager.inputInterface.isJoystickSupportBlocked() ? translate(LanguageSystem.GUI_CONFIG_JOYSTICK_DISABLED).getString() : translate(LanguageSystem.GUI_CONFIG_JOYSTICK_ERROR).getString()));
            return;
        }

        List<String> joysticks = InterfaceManager.inputInterface.getAllJoystickNames();
        if (joysticks.isEmpty()) {
            list.addConfigEntry(new TextEntry(Component.translatable("gui.config.joystick.none").getString()));
        }
        for (String joystickName : joysticks) {
            list.addConfigEntry(new ActionEntry(Component.literal(joystickName), Component.translatable("gui.config.joystick.select_button"), button -> {
                selectedJoystickName = joystickName;
                selectedJoystickComponent = -1;
                controlMode = ControlMode.JOYSTICK_COMPONENT;
                rebuildWidgets();
            }));
        }
    }

    private void addJoystickComponents() {
        if (selectedJoystickName == null || !InterfaceManager.inputInterface.isJoystickPresent(selectedJoystickName)) {
            controlMode = ControlMode.JOYSTICK_DEVICE;
            rebuildWidgets();
            return;
        }

        int componentCount = InterfaceManager.inputInterface.getJoystickComponentCount(selectedJoystickName);
        for (int i = 0; i < componentCount; ++i) {
            int componentIndex = i;
            list.addConfigEntry(new ActionEntry(() -> Component.literal(String.format(Locale.ROOT, "%02d  %s  %s", componentIndex + 1, InterfaceManager.inputInterface.getJoystickComponentName(selectedJoystickName, componentIndex), currentJoystickValue(componentIndex))), Component.translatable("gui.config.joystick.configure"), button -> {
                selectedJoystickComponent = componentIndex;
                controlMode = ControlMode.JOYSTICK_ASSIGNMENT;
                rebuildWidgets();
            }, getComponentAssignments(i)));
        }
    }

    private void addJoystickAssignments() {
        boolean axis = InterfaceManager.inputInterface.isJoystickComponentAxis(selectedJoystickName, selectedJoystickComponent);
        ControlType lastType = null;
        for (ControlsJoystick control : ControlsJoystick.values()) {
            if (control.isAxis == axis) {
                ControlType type = ControlType.fromSystemName(control.systemName);
                if (type != lastType) {
                    list.addConfigEntry(new CategoryEntry(categoryLabel(type)));
                    lastType = type;
                }
                Component buttonText = controlUsesSelectedComponent(control) ? translate(LanguageSystem.GUI_CONFIG_JOYSTICK_CLEAR) : Component.translatable("gui.config.joystick.map");
                list.addConfigEntry(new ActionEntry(translate(control.language), buttonText, button -> {
                    if (controlUsesSelectedComponent(control)) {
                        control.clearControl();
                        rebuildWidgets();
                    } else if (axis) {
                        beginCalibration(control);
                    } else {
                        control.setControl(selectedJoystickName, selectedJoystickComponent);
                        rebuildWidgets();
                    }
                }, joystickBindingTooltip(control)));
            }
        }
    }

    private void addJoystickCalibration() {
        double currentValue = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, selectedJoystickComponent);
        calibrationMin = Math.min(calibrationMin, currentValue);
        calibrationMax = Math.max(calibrationMax, currentValue);

        list.addConfigEntry(new TextEntry(() -> Component.translatable(
            "gui.config.joystick.calibration_values",
            translate(calibratingControl.language),
            String.format(Locale.ROOT, "%.3f", InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, selectedJoystickComponent)),
            String.format(Locale.ROOT, "%.3f", calibrationMin),
            String.format(Locale.ROOT, "%.3f", calibrationMax)
        ).getString()));
        list.addConfigEntry(new TripleButtonEntry(
            Component.translatable("gui.config.joystick.set_min"),
            Component.translatable("gui.config.joystick.set_max"),
            calibrationInverted ? translate(LanguageSystem.GUI_CONFIG_JOYSTICK_INVERT) : translate(LanguageSystem.GUI_CONFIG_JOYSTICK_NORMAL),
            button -> {
                calibrationMin = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, selectedJoystickComponent);
                rebuildWidgets();
            },
            button -> {
                calibrationMax = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, selectedJoystickComponent);
                rebuildWidgets();
            },
            button -> {
                calibrationInverted = !calibrationInverted;
                rebuildWidgets();
            }
        ));
        list.addConfigEntry(new ActionEntry(Component.literal(""), translate(LanguageSystem.GUI_CONFIRM), button -> {
            if (calibrationMax == calibrationMin) {
                calibrationMin = -1;
                calibrationMax = 1;
            }
            calibratingControl.setAxisControl(selectedJoystickName, selectedJoystickComponent, calibrationMin, calibrationMax, calibrationInverted);
            controlMode = ControlMode.JOYSTICK_ASSIGNMENT;
            calibratingControl = null;
            rebuildWidgets();
        }));
    }

    private void beginCalibration(ControlsJoystick control) {
        calibratingControl = control;
        double currentValue = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, selectedJoystickComponent);
        calibrationMin = Math.min(0, currentValue);
        calibrationMax = Math.max(0, currentValue);
        calibrationInverted = control.config.invertedAxis;
        controlMode = ControlMode.JOYSTICK_CALIBRATION;
        rebuildWidgets();
    }

    private void goBack() {
        if (controlMode == ControlMode.KEYBOARD || controlMode == ControlMode.JOYSTICK_DEVICE) {
            controlMode = ControlMode.ROOT;
        } else if (controlMode == ControlMode.JOYSTICK_COMPONENT) {
            controlMode = ControlMode.JOYSTICK_DEVICE;
        } else if (controlMode == ControlMode.JOYSTICK_ASSIGNMENT || controlMode == ControlMode.JOYSTICK_CALIBRATION) {
            controlMode = ControlMode.JOYSTICK_COMPONENT;
        }
        capturingKeyboardControls = List.of();
        rebuildWidgets();
    }

    private void updateConfigWidget(ConfigRow row, AbstractWidget widget) {
        Object value = row.entry.value;
        if (widget instanceof Button button) {
            if (value instanceof Boolean) {
                button.setMessage(booleanText((Boolean) value));
            } else if ("renderingMode".equals(row.name) && value instanceof Integer) {
                button.setMessage(renderingModeText((Integer) value));
            }
        }
    }

    private AbstractWidget createConfigWidget(ConfigRow row) {
        Object value = row.entry.value;
        if (value instanceof Boolean) {
            Button button = Button.builder(booleanText((Boolean) value), pressed -> {
                setEntryValue(row, !(Boolean) row.entry.value);
                ConfigSystem.saveToDisk();
                updateConfigWidget(row, pressed);
            }).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            button.setTooltip(Tooltip.create(configOptionTooltip(row)));
            return button;
        } else if ("renderingMode".equals(row.name) && value instanceof Integer) {
            Button button = Button.builder(renderingModeText((Integer) value), pressed -> {
                int newMode = ((Integer) row.entry.value + 1) % 3;
                setEntryValue(row, newMode);
                ConfigSystem.saveToDisk();
                updateConfigWidget(row, pressed);
            }).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            button.setTooltip(Tooltip.create(configOptionTooltip(row)));
            return button;
        } else if (("soundVolume".equals(row.name) || "radioVolume".equals(row.name)) && value instanceof Float) {
            return new VolumeSlider(row);
        } else if (value instanceof Number) {
            EditBox box = new EditBox(font, 0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT, configOptionLabel(row));
            box.setValue(String.valueOf(value));
            box.setMaxLength(32);
            box.setTooltip(Tooltip.create(configOptionTooltip(row)));
            box.setResponder(text -> updateNumberEntry(row, box, text));
            return box;
        } else {
            Button button = Button.builder(Component.translatable("gui.config.value.json_only"), pressed -> {}).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            button.active = false;
            button.setTooltip(Tooltip.create(configOptionTooltip(row)));
            return button;
        }
    }

    private void updateNumberEntry(ConfigRow row, EditBox box, String text) {
        try {
            Object currentValue = row.entry.value;
            if (currentValue instanceof Integer) {
                setEntryValue(row, Integer.parseInt(text));
            } else if (currentValue instanceof Float) {
                setEntryValue(row, Float.parseFloat(text));
            } else if (currentValue instanceof Double) {
                setEntryValue(row, Double.parseDouble(text));
            } else if (currentValue instanceof Long) {
                setEntryValue(row, Long.parseLong(text));
            }
            box.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
            ConfigSystem.saveToDisk();
        } catch (NumberFormatException e) {
            box.setTextColor(ERROR_COLOR);
        }
    }

    private void setEntryValue(ConfigRow row, Object value) {
        setEntryValue(row.entry, value);
    }

    @SuppressWarnings("unchecked")
    private <T> void setEntryValue(JSONConfigEntry<T> entry, Object value) {
        entry.value = (T) value;
    }

    private List<ConfigRow> getConfigRows(Object configObject) {
        List<ConfigRow> rows = new ArrayList<>();
        for (Field field : configObject.getClass().getFields()) {
            if (field.getType().equals(JSONConfigEntry.class)) {
                try {
                    rows.add(new ConfigRow(field.getName(), (JSONConfigEntry<?>) field.get(configObject)));
                } catch (IllegalAccessException e) {
                    //Public fields should always be readable, skip any surprise.
                }
            }
        }
        return rows;
    }

    private ConfigRow getConfigRow(Object configObject, String fieldName) {
        try {
            Field field = configObject.getClass().getField(fieldName);
            if (field.getType().equals(JSONConfigEntry.class)) {
                return new ConfigRow(fieldName, (JSONConfigEntry<?>) field.get(configObject));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //Public config fields should always be readable; skip fields removed by older/newer schemas.
        }
        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!capturingKeyboardControls.isEmpty()) {
            for (ControlsKeyboard control : capturingKeyboardControls) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    control.config.keyCode = 0;
                    control.config.isMouseButton = false;
                } else {
                    control.config.keyCode = keyCode;
                    control.config.isMouseButton = false;
                }
                ConfigSystem.client.controls.keyboard.put(control.systemName, control.config);
            }
            ConfigSystem.saveToDisk();
            capturingKeyboardControls = List.of();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!capturingKeyboardControls.isEmpty()) {
            for (ControlsKeyboard control : capturingKeyboardControls) {
                control.config.keyCode = button;
                control.config.isMouseButton = true;
                ConfigSystem.client.controls.keyboard.put(control.systemName, control.config);
            }
            ConfigSystem.saveToDisk();
            capturingKeyboardControls = List.of();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 12, LABEL_COLOR);
    }

    @Override
    public void tick() {
        super.tick();
        if (controlMode == ControlMode.JOYSTICK_CALIBRATION && selectedJoystickName != null) {
            double currentValue = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, selectedJoystickComponent);
            calibrationMin = Math.min(calibrationMin, currentValue);
            calibrationMax = Math.max(calibrationMax, currentValue);
        }
    }

    @Override
    public void onClose() {
        ConfigSystem.saveToDisk();
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return parentScreen != null && parentScreen.isPauseScreen();
    }

    private String bindingName(ControlsKeyboard control) {
        if (control.config.keyCode < 0 || (control.config.keyCode == 0 && !control.config.isMouseButton)) {
            return Component.translatable("gui.config.value.unbound").getString();
        }
        return control.config.isMouseButton ? InterfaceManager.inputInterface.getNameForMouseButton(control.config.keyCode) : InterfaceManager.inputInterface.getNameForKeyCode(control.config.keyCode);
    }

    private String bindingName(List<ControlsKeyboard> controls) {
        List<String> names = new ArrayList<>();
        for (ControlsKeyboard control : controls) {
            String name = bindingName(control);
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return String.join(" / ", names);
    }

    private boolean isDefault(ControlsKeyboard control) {
        return control.config.keyCode == InterfaceManager.inputInterface.getKeyCodeForName(control.defaultKeyName) && !control.config.isMouseButton;
    }

    private boolean isDefault(List<ControlsKeyboard> controls) {
        for (ControlsKeyboard control : controls) {
            if (!isDefault(control)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCollision(List<ControlsKeyboard> controls) {
        for (ControlsKeyboard control : controls) {
            if (control.config.keyCode < 0 || (control.config.keyCode == 0 && !control.config.isMouseButton)) {
                continue;
            }
            for (ControlsKeyboard otherControl : ControlsKeyboard.values()) {
                if (!controls.contains(otherControl) && otherControl.config.keyCode == control.config.keyCode && otherControl.config.isMouseButton == control.config.isMouseButton) {
                    return true;
                }
            }
        }
        return false;
    }

    private Component collisionTooltip(List<ControlsKeyboard> controls) {
        StringBuilder collisions = new StringBuilder();
        for (ControlsKeyboard control : controls) {
            for (ControlsKeyboard otherControl : ControlsKeyboard.values()) {
                if (!controls.contains(otherControl) && otherControl.config.keyCode == control.config.keyCode && otherControl.config.isMouseButton == control.config.isMouseButton) {
                    String label = translate(otherControl.language).getString();
                    if (!collisions.toString().contains(label)) {
                        if (collisions.length() > 0) {
                            collisions.append(", ");
                        }
                        collisions.append(label);
                    }
                }
            }
        }
        return Component.translatable("controls.keybinds.duplicateKeybinds", Component.literal(collisions.toString()));
    }

    private boolean isSharedVehicleKeyboardControl(ControlsKeyboard control) {
        for (ControlsKeyboard otherControl : ControlsKeyboard.values()) {
            if (isEquivalentVehicleKeyboardControl(control, otherControl)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEquivalentVehicleKeyboardControl(ControlsKeyboard firstControl, ControlsKeyboard secondControl) {
        ControlType firstType = ControlType.fromSystemName(firstControl.systemName);
        ControlType secondType = ControlType.fromSystemName(secondControl.systemName);
        return firstControl.language == secondControl.language
            && ((firstType == ControlType.CAR && secondType == ControlType.AIRCRAFT) || (firstType == ControlType.AIRCRAFT && secondType == ControlType.CAR));
    }

    private boolean controlUsesSelectedComponent(ControlsJoystick control) {
        return selectedJoystickName != null && selectedJoystickName.equals(control.config.joystickName) && control.config.buttonIndex == selectedJoystickComponent;
    }

    private String currentJoystickValue(int componentIndex) {
        if (InterfaceManager.inputInterface.isJoystickComponentAxis(selectedJoystickName, componentIndex)) {
            return String.format(Locale.ROOT, "%.2f", InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, componentIndex));
        }
        return Component.translatable(InterfaceManager.inputInterface.getJoystickButtonValue(selectedJoystickName, componentIndex) ? "options.on" : "options.off").getString();
    }

    private String getComponentAssignments(int componentIndex) {
        StringBuilder assignments = new StringBuilder();
        for (ControlsJoystick control : ControlsJoystick.values()) {
            if (selectedJoystickName.equals(control.config.joystickName) && control.config.buttonIndex == componentIndex) {
                if (assignments.length() > 0) {
                    assignments.append(", ");
                }
                assignments.append(translate(control.language).getString());
            }
        }
        return assignments.length() == 0 ? null : assignments.toString();
    }

    private String joystickBindingTooltip(ControlsJoystick control) {
        if (control.config.joystickName == null) {
            return null;
        }
        return control.config.joystickName + " / " + control.config.buttonIndex;
    }

    private Component booleanText(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private Component renderingModeText(int value) {
        switch (value) {
            case 1:
                return translate(LanguageSystem.GUI_CONFIG_RENDERING_MODE1);
            case 2:
                return translate(LanguageSystem.GUI_CONFIG_RENDERING_MODE2);
            default:
                return translate(LanguageSystem.GUI_CONFIG_RENDERING_MODE0);
        }
    }

    private Component controlSchemeText(FlightControlMode mode) {
        switch (mode) {
            case MOUSE:
                return Component.translatable("gui.config.control_scheme.mouse");
            case ARCADE:
                return Component.translatable("gui.config.control_scheme.arcade");
            default:
                return Component.translatable("gui.config.control_scheme.manual");
        }
    }

    private Component translate(LanguageEntry entry) {
        if (entry.key == null) {
            return Component.literal(entry.getCurrentValue());
        }
        return Component.translatableWithFallback(entry.key, entry.getCurrentValue());
    }

    private Component configOptionLabel(ConfigRow row) {
        return Component.translatableWithFallback("gui.config.option." + row.name + ".label", row.name);
    }

    private Component configOptionTooltip(ConfigRow row) {
        return Component.translatableWithFallback("gui.config.option." + row.name + ".tooltip", row.entry.comment);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private Component categoryLabel(ControlType type) {
        return Component.translatable(type.categoryKey);
    }

    private enum Tab {
        RENDERING,
        CLIENT,
        SERVER,
        CONTROLS
    }

    private enum ControlMode {
        ROOT,
        KEYBOARD,
        JOYSTICK_DEVICE,
        JOYSTICK_COMPONENT,
        JOYSTICK_ASSIGNMENT,
        JOYSTICK_CALIBRATION
    }

    private enum ControlType {
        GENERAL("general", "gui.config.controls.category.general"),
        AIRCRAFT("aircraft", "gui.config.controls.category.aircraft"),
        CAR("car", "gui.config.controls.category.car");

        private final String prefix;
        private final String categoryKey;

        ControlType(String prefix, String categoryKey) {
            this.prefix = prefix;
            this.categoryKey = categoryKey;
        }

        private static ControlType fromSystemName(String systemName) {
            for (ControlType type : values()) {
                if (systemName.startsWith(type.prefix)) {
                    return type;
                }
            }
            return GENERAL;
        }
    }

    private class ConfigList extends ContainerObjectSelectionList<ListEntry> {
        private ConfigList(int screenWidth, int screenHeight) {
            super(Minecraft.getInstance(), screenWidth, screenHeight - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, ROW_HEIGHT);
            centerListVertically = false;
        }

        private void addConfigEntry(ListEntry entry) {
            addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return Math.min(430, width - 72);
        }

        @Override
        protected int getScrollbarPosition() {
            return Math.min(width - 28, getRowRight() + 6);
        }

        private int scrollbarX() {
            return getScrollbarPosition();
        }
    }

    private abstract class ListEntry extends ContainerObjectSelectionList.Entry<ListEntry> {
        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private class CategoryEntry extends ListEntry {
        private final Component name;

        private CategoryEntry(Component name) {
            this.name = name;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!name.getString().isEmpty()) {
                guiGraphics.drawCenteredString(font, name, MCConfigScreen.this.width / 2, top + height - 10, LABEL_COLOR);
            }
        }
    }

    private class TextEntry extends ListEntry {
        private final Supplier<String> textSupplier;

        private TextEntry(String text) {
            this(() -> text);
        }

        private TextEntry(Supplier<String> textSupplier) {
            this.textSupplier = textSupplier;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            String text = textSupplier.get();
            String visibleText = trimToWidth(text, width);
            guiGraphics.drawCenteredString(font, visibleText, MCConfigScreen.this.width / 2, top + height / 2 - 4, MUTED_COLOR);
            if (!visibleText.equals(text) && mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height) {
                guiGraphics.renderTooltip(font, Component.literal(text), mouseX, mouseY);
            }
        }
    }

    private class DualButtonEntry extends ListEntry {
        private final Button leftButton;
        private final Button rightButton;

        private DualButtonEntry(Component leftText, Component rightText, Button.OnPress leftPress, Button.OnPress rightPress) {
            leftButton = Button.builder(leftText, leftPress).bounds(0, 0, WIDE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            rightButton = Button.builder(rightText, rightPress).bounds(0, 0, WIDE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            leftButton.setPosition(MCConfigScreen.this.width / 2 - WIDE_BUTTON_WIDTH - 4, top - 2);
            rightButton.setPosition(MCConfigScreen.this.width / 2 + 4, top - 2);
            leftButton.render(guiGraphics, mouseX, mouseY, partialTick);
            rightButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(leftButton, rightButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(leftButton, rightButton);
        }
    }

    private class TripleButtonEntry extends ListEntry {
        private final Button firstButton;
        private final Button secondButton;
        private final Button thirdButton;

        private TripleButtonEntry(Component firstText, Component secondText, Component thirdText, Button.OnPress firstPress, Button.OnPress secondPress, Button.OnPress thirdPress) {
            firstButton = Button.builder(firstText, firstPress).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            secondButton = Button.builder(secondText, secondPress).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            thirdButton = Button.builder(thirdText, thirdPress).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int x = MCConfigScreen.this.width / 2 - VALUE_BUTTON_WIDTH - VALUE_BUTTON_WIDTH / 2 - 8;
            firstButton.setPosition(x, top - 2);
            secondButton.setPosition(x + VALUE_BUTTON_WIDTH + 8, top - 2);
            thirdButton.setPosition(x + (VALUE_BUTTON_WIDTH + 8) * 2, top - 2);
            firstButton.render(guiGraphics, mouseX, mouseY, partialTick);
            secondButton.render(guiGraphics, mouseX, mouseY, partialTick);
            thirdButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(firstButton, secondButton, thirdButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(firstButton, secondButton, thirdButton);
        }
    }

    private class ControlSchemeEntry extends ListEntry {
        private final Button button;

        private ControlSchemeEntry() {
            button = Button.builder(controlSchemeText(ControlSystem.getFlightControlMode()), pressed -> {
                ControlSystem.cycleFlightControlMode(false);
                pressed.setMessage(controlSchemeText(ControlSystem.getFlightControlMode()));
            }).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            button.setTooltip(Tooltip.create(Component.translatable("gui.config.control_scheme.tooltip")));
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int buttonX = MCConfigScreen.this.list.scrollbarX() - button.getWidth() - 10;
            int labelMaxWidth = Math.max(40, buttonX - left - 8);
            Component label = Component.translatable("gui.config.control_scheme.label");
            String visibleLabel = trimToWidth(label.getString(), labelMaxWidth);
            guiGraphics.drawString(font, visibleLabel, left, top + height / 2 - 4, LABEL_COLOR, false);
            if (mouseX >= left && mouseX <= left + font.width(visibleLabel) && mouseY >= top && mouseY <= top + height) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.config.control_scheme.tooltip"), mouseX, mouseY);
            }
            button.setPosition(buttonX, top - 2);
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(button);
        }
    }

    private class VolumeSlider extends AbstractSliderButton {
        private final ConfigRow row;

        private VolumeSlider(ConfigRow row) {
            super(0, 0, VOLUME_SLIDER_WIDTH, BUTTON_HEIGHT, Component.empty(), Math.max(0.0D, Math.min(1.0D, ((Number) row.entry.value).doubleValue() / 1.5D)));
            this.row = row;
            setTooltip(Tooltip.create(configOptionTooltip(row)));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (row != null) {
                int percent = Math.round(((Number) row.entry.value).floatValue() * 100.0F);
                setMessage(Component.translatable("options.percent_value", configOptionLabel(row), percent));
            }
        }

        @Override
        protected void applyValue() {
            float newVolume = Math.round(value * 15.0D) / 10.0F;
            float currentVolume = ((Number) row.entry.value).floatValue();
            if (Float.compare(newVolume, currentVolume) != 0) {
                setEntryValue(row, newVolume);
                ConfigSystem.saveToDisk();
            }
            updateMessage();
        }
    }

    private class ConfigEntry extends ListEntry {
        private final ConfigRow row;
        private final AbstractWidget widget;

        private ConfigEntry(ConfigRow row) {
            this(row, true);
        }

        private ConfigEntry(ConfigRow row, boolean editable) {
            this.row = row;
            this.widget = createConfigWidget(row);
            this.widget.active &= editable;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (widget instanceof VolumeSlider) {
                widget.setPosition(MCConfigScreen.this.width / 2 - widget.getWidth() / 2, top - 2);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
                return;
            }
            int widgetX = MCConfigScreen.this.list.scrollbarX() - widget.getWidth() - 10;
            int labelMaxWidth = Math.max(40, widgetX - left - 8);
            String label = trimToWidth(configOptionLabel(row).getString(), labelMaxWidth);
            guiGraphics.drawString(font, label, left, top + height / 2 - 4, LABEL_COLOR, false);
            if (row.entry.comment != null && mouseX >= left && mouseX <= left + font.width(label) && mouseY >= top && mouseY <= top + height) {
                guiGraphics.renderTooltip(font, configOptionTooltip(row), mouseX, mouseY);
            }
            widget.setPosition(widgetX, top - 2);
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(widget);
        }
    }

    private class KeyboardEntry extends ListEntry {
        private final List<ControlsKeyboard> controls;
        private final Button changeButton;
        private final Button resetButton;

        private KeyboardEntry(ControlsKeyboard control) {
            this(List.of(control));
        }

        private KeyboardEntry(List<ControlsKeyboard> controls) {
            this.controls = List.copyOf(controls);
            changeButton = Button.builder(Component.literal(bindingName(this.controls)), button -> {
                capturingKeyboardControls = this.controls;
            }).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            resetButton = Button.builder(Component.translatable("controls.reset"), button -> {
                for (ControlsKeyboard control : this.controls) {
                    control.config.keyCode = InterfaceManager.inputInterface.getKeyCodeForName(control.defaultKeyName);
                    control.config.isMouseButton = false;
                    ConfigSystem.client.controls.keyboard.put(control.systemName, control.config);
                }
                ConfigSystem.saveToDisk();
            }).bounds(0, 0, RESET_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            refreshButtons();
        }

        private void refreshButtons() {
            Component binding = Component.literal(bindingName(controls));
            if (hasCollision(controls)) {
                MutableComponent collisionText = Component.literal("[ ")
                    .append(binding.copy().withStyle(ChatFormatting.WHITE))
                    .append(" ]")
                    .withStyle(ChatFormatting.RED);
                changeButton.setMessage(collisionText);
                changeButton.setTooltip(Tooltip.create(collisionTooltip(controls)));
            } else {
                changeButton.setMessage(binding);
                changeButton.setTooltip(null);
            }
            if (capturingKeyboardControls.equals(controls)) {
                changeButton.setMessage(Component.literal("> ")
                    .append(Component.literal(bindingName(controls)).withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                    .append(" <")
                    .withStyle(ChatFormatting.YELLOW));
            }
            resetButton.active = !isDefault(controls);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            ControlsKeyboard control = controls.get(0);
            refreshButtons();
            int resetX = MCConfigScreen.this.list.scrollbarX() - RESET_BUTTON_WIDTH - 10;
            int changeX = resetX - VALUE_BUTTON_WIDTH - 5;
            resetButton.setPosition(resetX, top - 2);
            changeButton.setPosition(changeX, top - 2);
            int labelMaxWidth = Math.max(40, changeX - left - 8);
            guiGraphics.drawString(font, trimToWidth(translate(control.language).getString(), labelMaxWidth), left, top + height / 2 - 4, LABEL_COLOR, false);
            if (hasCollision(controls)) {
                guiGraphics.fill(changeX - 6, top - 1, changeX - 3, top + height, 0xFFFF0000);
            }
            changeButton.render(guiGraphics, mouseX, mouseY, partialTick);
            resetButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(changeButton, resetButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(changeButton, resetButton);
        }
    }

    private class ActionEntry extends ListEntry {
        private final Supplier<Component> labelSupplier;
        private final Button button;
        private final String tooltip;

        private ActionEntry(Component label, Component buttonText, Button.OnPress press) {
            this(label, buttonText, press, null);
        }

        private ActionEntry(Component label, Component buttonText, Button.OnPress press, String tooltip) {
            this(() -> label, buttonText, press, tooltip);
        }

        private ActionEntry(Supplier<Component> labelSupplier, Component buttonText, Button.OnPress press) {
            this(labelSupplier, buttonText, press, null);
        }

        private ActionEntry(Supplier<Component> labelSupplier, Component buttonText, Button.OnPress press, String tooltip) {
            this.labelSupplier = labelSupplier;
            this.tooltip = tooltip;
            button = Button.builder(buttonText, press).bounds(0, 0, VALUE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
            if (tooltip != null) {
                button.setTooltip(Tooltip.create(Component.literal(tooltip)));
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int buttonX = MCConfigScreen.this.list.scrollbarX() - button.getWidth() - 10;
            Component label = labelSupplier.get();
            String text = trimToWidth(label.getString(), Math.max(40, buttonX - left - 8));
            guiGraphics.drawString(font, text, left, top + height / 2 - 4, LABEL_COLOR, false);
            if (tooltip != null && mouseX >= left && mouseX <= left + font.width(text) && mouseY >= top && mouseY <= top + height) {
                guiGraphics.renderTooltip(font, Component.literal(tooltip), mouseX, mouseY);
            }
            button.setPosition(buttonX, top - 2);
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(button);
        }
    }

    private static class ConfigRow {
        private final String name;
        private final JSONConfigEntry<?> entry;

        private ConfigRow(String name, JSONConfigEntry<?> entry) {
            this.name = name;
            this.entry = entry;
        }
    }
}
