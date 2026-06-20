package mcinterface1211;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Optional FMOD Studio bridge for MC 1.21.1.  Uses reflection so this interface keeps working
 * when the FMOD LWJGL module or native libraries are not present.
 */
public class FMODWrapper {
    private static final String FMOD_BANK_DIRECTORY = "fmod";
    private static final int FMOD_OK = 0;
    private static final boolean DEBUG_PARAMETERS = Boolean.getBoolean("mts.fmod.debugParameters");

    private static final Map<SoundInstance, Long> playingEvents = new IdentityHashMap<>();
    private static final Map<String, Long> eventDescriptions = new HashMap<>();
    private static final Map<Integer, String> fmodResultNames = new HashMap<>();
    private static final Map<String, Float> lastLoggedParameterValues = new HashMap<>();
    private static final Set<String> missingEvents = new HashSet<>();
    private static final Set<String> badParameters = new HashSet<>();
    private static final List<Long> loadedBanks = new ArrayList<>();

    private static File gameDirectory;
    private static File fmodNativeLibrary;
    private static File fmodStudioNativeLibrary;
    private static boolean nativeLoadAttempted;
    private static boolean bindingsLoadAttempted;
    private static boolean systemInitAttempted;
    private static boolean banksLoadAttempted;
    private static boolean available;
    private static boolean paused;
    private static long studioSystem;

    private static Class<?> fmodClass;
    private static Class<?> fmodStudioClass;
    private static Class<?> memoryStackClass;
    private static Class<?> pointerBufferClass;
    private static Class<?> attributesClass;
    private static Class<?> vectorClass;

    private static Method stackPushMethod;
    private static Method stackMallocPointerMethod;
    private static Method stackMallocIntMethod;
    private static Method stackMallocFloatMethod;
    private static Method pointerGetMethod;
    private static Method vectorSetMethod;
    private static Method attributesCallocMethod;
    private static Method attributesPositionMethod;
    private static Method attributesVelocityMethod;
    private static Method attributesForwardMethod;
    private static Method attributesUpMethod;
    private static Method systemCreateMethod;
    private static Method systemInitializeMethod;
    private static Method systemUpdateMethod;
    private static Method systemReleaseMethod;
    private static Method systemUnloadAllMethod;
    private static Method systemLoadBankFileMethod;
    private static Method systemGetEventMethod;
    private static Method systemSetListenerAttributesMethod;
    private static Method eventDescriptionCreateInstanceMethod;
    private static Method eventInstanceSet3DAttributesMethod;
    private static Method eventInstanceSetVolumeMethod;
    private static Method eventInstanceSetPitchMethod;
    private static Method eventInstanceGetParameterByNameMethod;
    private static Method eventInstanceSetParameterByNameMethod;
    private static Method eventInstanceSetPausedMethod;
    private static Method eventInstanceStartMethod;
    private static Method eventInstanceStopMethod;
    private static Method eventInstanceReleaseMethod;
    private static Method eventInstanceGetPlaybackStateMethod;

    private static int fmodVersion;
    private static int studioInitNormal;
    private static int fmodInitNormal;
    private static int loadBankNormal;
    private static int playbackStopped;
    private static int stopImmediate;

    public static synchronized void initialize(String gameDirectoryPath) {
        gameDirectory = new File(gameDirectoryPath);
        loadNativeLibraries();
        if (ensureSystem()) {
            loadBanks();
        }
    }

    public static synchronized boolean isAvailable() {
        return available;
    }

    public static synchronized boolean playEvent(SoundInstance sound) {
        String eventName = sound.soundDef != null ? sound.soundDef.eventName : null;
        if (eventName == null || eventName.isEmpty() || !ensureSystem()) {
            return false;
        }
        loadBanks();
        try (AutoCloseable stack = pushStack()) {
            long eventDescription = getEventDescription(eventName, stack);
            if (eventDescription == 0) {
                return false;
            }

            Object eventBuffer = mallocPointer(stack, 1);
            if (!checkResult((int) eventDescriptionCreateInstanceMethod.invoke(null, eventDescription, eventBuffer), "create event instance")) {
                return false;
            }
            long eventInstance = getPointer(eventBuffer, 0);
            if (eventInstance == 0) {
                return false;
            }

            updateEventProperties(sound, eventInstance, stack);
            if (!checkResult((int) eventInstanceStartMethod.invoke(null, eventInstance), "start event " + eventName)) {
                releaseEvent(eventInstance);
                return false;
            }
            if (paused && eventInstanceSetPausedMethod != null) {
                eventInstanceSetPausedMethod.invoke(null, eventInstance, 1);
            }

            playingEvents.put(sound, eventInstance);
            sound.sourceIndex = -1;
            sound.entity.sounds.add(sound);
            return true;
        } catch (Throwable e) {
            logError("FMOD event playback failed for " + eventName + ": " + e.getMessage());
            return false;
        }
    }

    public static synchronized void update(IWrapperPlayer player) {
        if (!available) {
            return;
        }
        try {
            try (AutoCloseable stack = pushStack()) {
                if (player != null) {
                    updateListener(player, stack);
                }
                List<SoundInstance> finishedSounds = new ArrayList<>();
                for (Map.Entry<SoundInstance, Long> eventEntry : playingEvents.entrySet()) {
                    SoundInstance sound = eventEntry.getKey();
                    long eventInstance = eventEntry.getValue();
                    if (sound.stopSound || !isEventPlaying(eventInstance, stack)) {
                        stopAndReleaseEvent(eventInstance, sound.stopSound);
                        finishedSounds.add(sound);
                    } else {
                        updateEventProperties(sound, eventInstance, stack);
                    }
                }
                for (SoundInstance sound : finishedSounds) {
                    playingEvents.remove(sound);
                    sound.stopSound = true;
                    sound.entity.sounds.remove(sound);
                }
            }
            systemUpdateMethod.invoke(null, studioSystem);
        } catch (Throwable e) {
            logError("FMOD update failed: " + e.getMessage());
        }
    }

    public static synchronized void updateRenderListener(Point3D position, Point3D velocity, Point3D forward, Point3D up) {
        if (!available || paused) {
            return;
        }
        try {
            try (AutoCloseable stack = pushStack()) {
                updateListener(position, velocity, forward, up, stack);
                for (Map.Entry<SoundInstance, Long> eventEntry : playingEvents.entrySet()) {
                    SoundInstance sound = eventEntry.getKey();
                    if (!sound.stopSound) {
                        updateEvent3DProperties(sound, eventEntry.getValue(), stack);
                    }
                }
            }
            systemUpdateMethod.invoke(null, studioSystem);
        } catch (Throwable e) {
            logError("FMOD render listener update failed: " + e.getMessage());
        }
    }

    public static synchronized void pauseAll(boolean paused) {
        FMODWrapper.paused = paused;
        if (!available || eventInstanceSetPausedMethod == null) {
            return;
        }
        try {
            for (long eventInstance : playingEvents.values()) {
                eventInstanceSetPausedMethod.invoke(null, eventInstance, paused ? 1 : 0);
            }
            systemUpdateMethod.invoke(null, studioSystem);
        } catch (Throwable e) {
            logError("FMOD pause state update failed: " + e.getMessage());
        }
    }

    public static synchronized void stopAllSounds() {
        if (!available) {
            return;
        }
        try {
            for (Map.Entry<SoundInstance, Long> eventEntry : playingEvents.entrySet()) {
                stopAndReleaseEvent(eventEntry.getValue(), true);
                eventEntry.getKey().stopSound = true;
                eventEntry.getKey().entity.sounds.remove(eventEntry.getKey());
            }
            playingEvents.clear();
            systemUpdateMethod.invoke(null, studioSystem);
        } catch (Throwable e) {
            logError("FMOD stopAllSounds failed: " + e.getMessage());
        }
    }

    public static synchronized void shutdown() {
        if (!available) {
            return;
        }
        stopAllSounds();
        try {
            systemUnloadAllMethod.invoke(null, studioSystem);
            systemReleaseMethod.invoke(null, studioSystem);
        } catch (Throwable e) {
            logError("FMOD shutdown failed: " + e.getMessage());
        } finally {
            studioSystem = 0;
            available = false;
            systemInitAttempted = false;
            banksLoadAttempted = false;
            loadedBanks.clear();
            eventDescriptions.clear();
            lastLoggedParameterValues.clear();
        }
    }

    private static boolean ensureSystem() {
        if (available) {
            return true;
        }
        if (systemInitAttempted) {
            return false;
        }
        systemInitAttempted = true;
        if (!loadBindings()) {
            return false;
        }
        try (AutoCloseable stack = pushStack()) {
            Object systemBuffer = mallocPointer(stack, 1);
            if (!checkResult((int) systemCreateMethod.invoke(null, systemBuffer, fmodVersion), "create studio system")) {
                return false;
            }
            studioSystem = getPointer(systemBuffer, 0);
            if (studioSystem == 0) {
                logError("FMOD Studio returned an empty system pointer.");
                return false;
            }
            if (!checkResult((int) systemInitializeMethod.invoke(null, studioSystem, 1024, studioInitNormal, fmodInitNormal, 0L), "initialize studio system")) {
                studioSystem = 0;
                return false;
            }
            available = true;
            logInfo("FMOD Studio engine initialized.");
            return true;
        } catch (Throwable e) {
            logInfo("FMOD Studio engine is not available: " + e.getMessage());
            studioSystem = 0;
            return false;
        }
    }

    private static boolean loadBindings() {
        if (bindingsLoadAttempted) {
            return fmodStudioClass != null;
        }
        bindingsLoadAttempted = true;
        configureLWJGLLibraryNames();
        try {
            fmodClass = Class.forName("org.lwjgl.fmod.FMOD");
            fmodStudioClass = Class.forName("org.lwjgl.fmod.FMODStudio");
            memoryStackClass = Class.forName("org.lwjgl.system.MemoryStack");
            pointerBufferClass = Class.forName("org.lwjgl.PointerBuffer");
            attributesClass = Class.forName("org.lwjgl.fmod.FMOD_3D_ATTRIBUTES");
            vectorClass = Class.forName("org.lwjgl.fmod.FMOD_VECTOR");

            stackPushMethod = memoryStackClass.getMethod("stackPush");
            stackMallocPointerMethod = memoryStackClass.getMethod("mallocPointer", int.class);
            stackMallocIntMethod = memoryStackClass.getMethod("mallocInt", int.class);
            stackMallocFloatMethod = memoryStackClass.getMethod("mallocFloat", int.class);
            pointerGetMethod = pointerBufferClass.getMethod("get", int.class);
            vectorSetMethod = vectorClass.getMethod("set", float.class, float.class, float.class);
            attributesCallocMethod = attributesClass.getMethod("calloc", memoryStackClass);
            attributesPositionMethod = attributesClass.getMethod("position$");
            attributesVelocityMethod = attributesClass.getMethod("velocity");
            attributesForwardMethod = attributesClass.getMethod("forward");
            attributesUpMethod = attributesClass.getMethod("up");

            systemCreateMethod = fmodStudioClass.getMethod("FMOD_Studio_System_Create", pointerBufferClass, int.class);
            systemInitializeMethod = fmodStudioClass.getMethod("FMOD_Studio_System_Initialize", long.class, int.class, int.class, int.class, long.class);
            systemUpdateMethod = fmodStudioClass.getMethod("FMOD_Studio_System_Update", long.class);
            systemReleaseMethod = fmodStudioClass.getMethod("FMOD_Studio_System_Release", long.class);
            systemUnloadAllMethod = fmodStudioClass.getMethod("FMOD_Studio_System_UnloadAll", long.class);
            systemLoadBankFileMethod = fmodStudioClass.getMethod("FMOD_Studio_System_LoadBankFile", long.class, CharSequence.class, int.class, pointerBufferClass);
            systemGetEventMethod = fmodStudioClass.getMethod("FMOD_Studio_System_GetEvent", long.class, CharSequence.class, pointerBufferClass);
            systemSetListenerAttributesMethod = fmodStudioClass.getMethod("FMOD_Studio_System_SetListenerAttributes", long.class, int.class, attributesClass, vectorClass);
            eventDescriptionCreateInstanceMethod = fmodStudioClass.getMethod("FMOD_Studio_EventDescription_CreateInstance", long.class, pointerBufferClass);
            eventInstanceSet3DAttributesMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_Set3DAttributes", long.class, attributesClass);
            eventInstanceSetVolumeMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_SetVolume", long.class, float.class);
            eventInstanceSetPitchMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_SetPitch", long.class, float.class);
            eventInstanceGetParameterByNameMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_GetParameterByName", long.class, CharSequence.class, FloatBuffer.class, FloatBuffer.class);
            eventInstanceSetParameterByNameMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_SetParameterByName", long.class, CharSequence.class, float.class, int.class);
            eventInstanceStartMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_Start", long.class);
            eventInstanceStopMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_Stop", long.class, int.class);
            eventInstanceReleaseMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_Release", long.class);
            eventInstanceGetPlaybackStateMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_GetPlaybackState", long.class, IntBuffer.class);
            try {
                eventInstanceSetPausedMethod = fmodStudioClass.getMethod("FMOD_Studio_EventInstance_SetPaused", long.class, int.class);
            } catch (NoSuchMethodException e) {
                eventInstanceSetPausedMethod = null;
            }

            fmodVersion = getIntConstant(fmodClass, "FMOD_VERSION", 0);
            studioInitNormal = getIntConstant(fmodStudioClass, "FMOD_STUDIO_INIT_NORMAL", 0);
            fmodInitNormal = getIntConstant(fmodClass, "FMOD_INIT_NORMAL", 0);
            loadBankNormal = getIntConstant(fmodStudioClass, "FMOD_STUDIO_LOAD_BANK_NORMAL", 0);
            playbackStopped = getIntConstant(fmodStudioClass, "FMOD_STUDIO_PLAYBACK_STOPPED", 2);
            stopImmediate = getIntConstant(fmodStudioClass, "FMOD_STUDIO_STOP_IMMEDIATE", 1);
            cacheFMODResultNames();
            return true;
        } catch (ClassNotFoundException e) {
            logInfo("FMOD LWJGL class not found on runtime classpath; MTS will continue using OpenAL. Missing class: " + e.getMessage());
            fmodStudioClass = null;
            return false;
        } catch (UnsatisfiedLinkError e) {
            logInfo("FMOD native library could not be linked; MTS will continue using OpenAL. " + describeThrowable(e));
            fmodStudioClass = null;
            return false;
        } catch (Throwable e) {
            logInfo("FMOD LWJGL bindings could not be initialized; MTS will continue using OpenAL. " + describeThrowable(e));
            fmodStudioClass = null;
            return false;
        }
    }

    private static void loadNativeLibraries() {
        if (nativeLoadAttempted) {
            return;
        }
        nativeLoadAttempted = true;
        for (String libraryName : getNativeLibraryNames()) {
            File libraryFile = findNativeLibrary(libraryName);
            if (libraryFile == null) {
                logInfo("FMOD native library not found in game directory: " + libraryName);
                continue;
            }
            assignNativeLibrary(libraryName, libraryFile);
            logInfo("Found FMOD native library: " + libraryFile.getAbsolutePath());
        }
    }

    private static void assignNativeLibrary(String libraryName, File libraryFile) {
        if (libraryName.toLowerCase(Locale.ROOT).contains("studio")) {
            fmodStudioNativeLibrary = libraryFile;
        } else {
            fmodNativeLibrary = libraryFile;
        }
    }

    private static void configureLWJGLLibraryNames() {
        if (fmodNativeLibrary != null) {
            String libraryPath = fmodNativeLibrary.getAbsolutePath();
            System.setProperty("org.lwjgl.fmod.libname", libraryPath);
            setLWJGLConfiguration("FMOD_LIBRARY_NAME", libraryPath);
            addLWJGLLibraryPath(fmodNativeLibrary.getParentFile());
        }
        if (fmodStudioNativeLibrary != null) {
            String libraryPath = fmodStudioNativeLibrary.getAbsolutePath();
            System.setProperty("org.lwjgl.fmod.studio.libname", libraryPath);
            setLWJGLConfiguration("FMOD_STUDIO_LIBRARY_NAME", libraryPath);
            addLWJGLLibraryPath(fmodStudioNativeLibrary.getParentFile());
        }
    }

    private static void addLWJGLLibraryPath(File directory) {
        if (directory == null) {
            return;
        }
        String directoryPath = directory.getAbsolutePath();
        String currentPath = System.getProperty("org.lwjgl.librarypath");
        if (currentPath == null || currentPath.isEmpty()) {
            System.setProperty("org.lwjgl.librarypath", directoryPath);
            setLWJGLConfiguration("LIBRARY_PATH", directoryPath);
            return;
        }
        for (String pathEntry : currentPath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (pathEntry.equals(directoryPath)) {
                return;
            }
        }
        String updatedPath = directoryPath + File.pathSeparator + currentPath;
        System.setProperty("org.lwjgl.librarypath", updatedPath);
        setLWJGLConfiguration("LIBRARY_PATH", updatedPath);
    }

    private static void setLWJGLConfiguration(String fieldName, String value) {
        try {
            Class<?> configurationClass = Class.forName("org.lwjgl.system.Configuration");
            Object configuration = configurationClass.getField(fieldName).get(null);
            configurationClass.getMethod("set", Object.class).invoke(configuration, value);
        } catch (Throwable e) {
            logInfo("Could not set LWJGL configuration " + fieldName + ": " + describeThrowable(e));
        }
    }

    private static void loadBanks() {
        if (banksLoadAttempted || !available) {
            return;
        }
        banksLoadAttempted = true;
        File bankDirectory = new File(gameDirectory, FMOD_BANK_DIRECTORY);
        if (!bankDirectory.isDirectory()) {
            logInfo("FMOD bank directory not found: " + bankDirectory.getAbsolutePath());
            return;
        }

        File[] bankFiles = bankDirectory.listFiles(file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".bank"));
        if (bankFiles == null || bankFiles.length == 0) {
            logInfo("FMOD bank directory contains no .bank files: " + bankDirectory.getAbsolutePath());
            return;
        }

        List<File> sortedBanks = new ArrayList<>();
        for (File bankFile : bankFiles) {
            sortedBanks.add(bankFile);
        }
        sortedBanks.sort(Comparator.comparingInt(FMODWrapper::getBankLoadPriority).thenComparing(File::getName));

        for (File bankFile : sortedBanks) {
            try (AutoCloseable stack = pushStack()) {
                Object bankBuffer = mallocPointer(stack, 1);
                if (checkResult((int) systemLoadBankFileMethod.invoke(null, studioSystem, bankFile.getAbsolutePath(), loadBankNormal, bankBuffer), "load bank " + bankFile.getName())) {
                    loadedBanks.add(getPointer(bankBuffer, 0));
                    logInfo("Loaded FMOD bank: " + bankFile.getName());
                }
            } catch (Throwable e) {
                logError("FMOD bank load failed for " + bankFile.getAbsolutePath() + ": " + e.getMessage());
            }
        }
        try {
            systemUpdateMethod.invoke(null, studioSystem);
        } catch (Throwable e) {
            logError("FMOD update after bank load failed: " + e.getMessage());
        }
    }

    private static long getEventDescription(String eventName, AutoCloseable stack) throws Exception {
        Long cachedEventDescription = eventDescriptions.get(eventName);
        if (cachedEventDescription != null) {
            return cachedEventDescription;
        }

        Object eventBuffer = mallocPointer(stack, 1);
        int result = (int) systemGetEventMethod.invoke(null, studioSystem, eventName, eventBuffer);
        if (result != FMOD_OK) {
            if (missingEvents.add(eventName)) {
                logInfo("FMOD event not found, falling back to OpenAL if possible: " + eventName + ". Result: " + describeFMODResult(result));
            }
            return 0;
        }
        long eventDescription = getPointer(eventBuffer, 0);
        eventDescriptions.put(eventName, eventDescription);
        return eventDescription;
    }

    private static void updateListener(IWrapperPlayer player, AutoCloseable stack) throws Exception {
        Point3D position = player.getEyePosition();
        Point3D velocity = player.getVelocity();
        Point3D forward = player.getLineOfSight(1.0).normalize();
        Point3D up = new Point3D(0, 1, 0).rotate(player.getOrientation());
        updateListener(position, velocity, forward, up, stack);
    }

    private static void updateListener(Point3D position, Point3D velocity, Point3D forward, Point3D up, AutoCloseable stack) throws Exception {
        Object attributes = createAttributes(stack, position, velocity, forward, up);
        systemSetListenerAttributesMethod.invoke(null, studioSystem, 0, attributes, null);
    }

    private static void updateEventProperties(SoundInstance sound, long eventInstance, AutoCloseable stack) throws Exception {
        updateEvent3DProperties(sound, eventInstance, stack);
        updateEventParameters(sound, eventInstance, stack);
    }

    private static void updateEvent3DProperties(SoundInstance sound, long eventInstance, AutoCloseable stack) throws Exception {
        sound.updatePosition();
        Point3D forward = new Point3D(0, 0, 1).rotate(sound.entity.orientation);
        Point3D up = new Point3D(0, 1, 0).rotate(sound.entity.orientation);
        Object attributes = createAttributes(stack, sound.position, sound.entity.motion, forward, up);
        eventInstanceSet3DAttributesMethod.invoke(null, eventInstance, attributes);
        eventInstanceSetVolumeMethod.invoke(null, eventInstance, sound.volume * ConfigSystem.client.controlSettings.soundVolume.value);
        eventInstanceSetPitchMethod.invoke(null, eventInstance, sound.pitch);
    }

    private static void updateEventParameters(SoundInstance sound, long eventInstance, AutoCloseable stack) throws Exception {
        if (sound.soundDef == null || sound.soundDef.eventParameters == null || !(sound.entity instanceof AEntityD_Definable)) {
            return;
        }

        AEntityD_Definable<?> entity = (AEntityD_Definable<?>) sound.entity;
        for (JSONSound.FMODEventParameter parameter : sound.soundDef.eventParameters) {
            if (parameter.variable == null || parameter.variable.isEmpty()) {
                continue;
            }
            String parameterName = parameter.parameterName != null && !parameter.parameterName.isEmpty() ? parameter.parameterName : parameter.variable;
            float parameterValue = (float) (entity.getOrCreateVariable(parameter.variable).computeValue(0) * parameter.factor + parameter.offset);
            int result = (int) eventInstanceSetParameterByNameMethod.invoke(null, eventInstance, parameterName, parameterValue, parameter.ignoreSeekSpeed ? 1 : 0);
            if (result == FMOD_OK) {
                badParameters.remove(getParameterLogKey(sound.soundDef, parameterName));
                logParameterValue(sound, eventInstance, stack, parameter, parameterName, parameterValue);
            } else {
                String parameterLogKey = getParameterLogKey(sound.soundDef, parameterName);
                if (badParameters.add(parameterLogKey)) {
                    logError("FMOD failed to set event parameter " + parameterName + " on " + sound.soundDef.eventName + ". Result: " + describeFMODResult(result));
                }
            }
        }
    }

    private static void logParameterValue(SoundInstance sound, long eventInstance, AutoCloseable stack, JSONSound.FMODEventParameter parameter, String parameterName, float sentValue) throws Exception {
        if (!DEBUG_PARAMETERS) {
            return;
        }

        String parameterLogKey = getParameterLogKey(sound.soundDef, parameterName) + "#" + System.identityHashCode(sound);
        Float lastValue = lastLoggedParameterValues.get(parameterLogKey);
        if (lastValue != null && Math.abs(lastValue - sentValue) < 0.0001F) {
            return;
        }
        lastLoggedParameterValues.put(parameterLogKey, sentValue);

        FloatBuffer currentValue = (FloatBuffer) stackMallocFloatMethod.invoke(stack, 1);
        FloatBuffer finalValue = (FloatBuffer) stackMallocFloatMethod.invoke(stack, 1);
        int result = (int) eventInstanceGetParameterByNameMethod.invoke(null, eventInstance, parameterName, currentValue, finalValue);
        if (result == FMOD_OK) {
            logInfo("FMOD parameter " + parameterName + " on " + sound.soundDef.eventName + " <= " + parameter.variable + "=" + sentValue + ", current=" + currentValue.get(0) + ", final=" + finalValue.get(0) + ", ignoreSeekSpeed=" + parameter.ignoreSeekSpeed);
        } else {
            logInfo("FMOD parameter " + parameterName + " on " + sound.soundDef.eventName + " <= " + parameter.variable + "=" + sentValue + ", read-back failed: " + describeFMODResult(result));
        }
    }

    private static String getParameterLogKey(JSONSound soundDef, String parameterName) {
        return soundDef.eventName + "#" + parameterName;
    }

    private static boolean isEventPlaying(long eventInstance, AutoCloseable stack) throws Exception {
        IntBuffer stateBuffer = (IntBuffer) stackMallocIntMethod.invoke(stack, 1);
        int result = (int) eventInstanceGetPlaybackStateMethod.invoke(null, eventInstance, stateBuffer);
        return result == FMOD_OK && stateBuffer.get(0) != playbackStopped;
    }

    private static Object createAttributes(AutoCloseable stack, Point3D position, Point3D velocity, Point3D forward, Point3D up) throws Exception {
        Object attributes = attributesCallocMethod.invoke(null, stack);
        setVector(attributesPositionMethod.invoke(attributes), position);
        setVector(attributesVelocityMethod.invoke(attributes), velocity);
        setVector(attributesForwardMethod.invoke(attributes), forward);
        setVector(attributesUpMethod.invoke(attributes), up);
        return attributes;
    }

    private static void setVector(Object vector, Point3D point) throws Exception {
        vectorSetMethod.invoke(vector, (float) -point.x, (float) point.y, (float) point.z);
    }

    private static void stopAndReleaseEvent(long eventInstance, boolean stop) throws Exception {
        if (stop) {
            eventInstanceStopMethod.invoke(null, eventInstance, stopImmediate);
        }
        releaseEvent(eventInstance);
    }

    private static void releaseEvent(long eventInstance) throws Exception {
        eventInstanceReleaseMethod.invoke(null, eventInstance);
    }

    private static AutoCloseable pushStack() throws Exception {
        return (AutoCloseable) stackPushMethod.invoke(null);
    }

    private static Object mallocPointer(AutoCloseable stack, int size) throws Exception {
        return stackMallocPointerMethod.invoke(stack, size);
    }

    private static long getPointer(Object pointerBuffer, int index) throws Exception {
        return (long) pointerGetMethod.invoke(pointerBuffer, index);
    }

    private static boolean checkResult(int result, String action) {
        if (result == FMOD_OK) {
            return true;
        }
        logError("FMOD failed to " + action + ". Result: " + describeFMODResult(result));
        return false;
    }

    private static void cacheFMODResultNames() {
        fmodResultNames.clear();
        for (Field field : fmodClass.getFields()) {
            String fieldName = field.getName();
            if (field.getType() == int.class && (fieldName.equals("FMOD_OK") || fieldName.startsWith("FMOD_ERR_"))) {
                try {
                    fmodResultNames.put(field.getInt(null), fieldName);
                } catch (IllegalAccessException e) {
                    // Public LWJGL constants should be readable.  Ignore any odd one out.
                }
            }
        }
    }

    private static String describeFMODResult(int result) {
        String resultName = fmodResultNames.get(result);
        return result + (resultName != null ? " (" + resultName + ")" : " (unknown FMOD result)");
    }

    private static String describeThrowable(Throwable throwable) {
        StringBuilder message = new StringBuilder(throwable.getClass().getSimpleName());
        if (throwable.getMessage() != null) {
            message.append(": ").append(throwable.getMessage());
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            message.append("; cause=").append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null) {
                message.append(": ").append(cause.getMessage());
            }
        }
        return message.toString();
    }

    private static int getIntConstant(Class<?> ownerClass, String fieldName, int fallbackValue) {
        try {
            return ownerClass.getField(fieldName).getInt(null);
        } catch (Exception e) {
            return fallbackValue;
        }
    }

    private static List<String> getNativeLibraryNames() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> libraryNames = new ArrayList<>();
        if (osName.contains("win")) {
            libraryNames.add("fmod.dll");
            libraryNames.add("fmodstudio.dll");
        } else if (osName.contains("mac")) {
            libraryNames.add("libfmod.dylib");
            libraryNames.add("libfmodstudio.dylib");
        } else {
            libraryNames.add("libfmod.so");
            libraryNames.add("libfmodstudio.so");
        }
        return libraryNames;
    }

    private static File findNativeLibrary(String libraryName) {
        File[] searchDirectories = new File[] {
            new File(gameDirectory, "natives"),
            gameDirectory,
            new File(gameDirectory, FMOD_BANK_DIRECTORY)
        };
        for (File searchDirectory : searchDirectories) {
            File libraryFile = new File(searchDirectory, libraryName);
            if (libraryFile.isFile()) {
                return libraryFile;
            }
        }
        return null;
    }

    private static int getBankLoadPriority(File bankFile) {
        String fileName = bankFile.getName().toLowerCase(Locale.ROOT);
        if (fileName.equals("master.strings.bank")) {
            return 0;
        } else if (fileName.equals("master.bank")) {
            return 1;
        } else {
            return 2;
        }
    }

    private static void logInfo(String message) {
        InterfaceLoader.LOGGER.info(message);
    }

    private static void logError(String message) {
        if (InterfaceManager.coreInterface != null) {
            InterfaceManager.coreInterface.logError(message);
        } else {
            InterfaceLoader.LOGGER.error(message);
        }
    }
}
