package io.github.msameer0.rhythmicrush.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Manages the persistence, retrieval, and application of user-configurable game settings.
 * <p>
 * This class serves as a central hub for various preferences including audio levels,
 * graphical toggles (FPS caps, VSync), and UI visibility options. Settings are
 * serialized to and deserialized from a local JSON file using the LibGDX {@link Json}
 * utility to ensure configurations are saved across game sessions.
 * </p>
 */
public class SettingsManager {

    private static final String SAVE_PATH = "saves/settings.json";

    public boolean menuMusicEnabled = true;
    public float musicVolume = 1f;
    public boolean showHitboxes = false;
    public boolean showHitboxesOnDeath = false;
    public boolean lockCursorInGame = false;
    public boolean showFps = false;
    public boolean capFps = false;
    public int fpsCapValue = 60;
    public boolean enableVsync = false;
    public boolean showPercentage = true;
    public boolean showProgressBar = true;
    public boolean showAttempts = true;
    public boolean showBest = true;
    public float uiPadding = 12f;
    public float practiceButtonOpacity = 0.5f;

    /**
     * A data transfer object (DTO) used to represent a serializable snapshot of the settings.
     * This class is primarily used by {@link Json} for loading and saving settings to the local filesystem.
     */
    public static class Data {
        public boolean menuMusicEnabled = true;
        public float musicVolume = 1f;
        public boolean showHitboxes = false;
        public boolean showHitboxesOnDeath = false;
        public boolean lockCursorInGame = false;
        public boolean showFps = false;
        public boolean capFps = false;
        public int fpsCapValue = 60;
        public boolean enableVsync = false;
        public boolean showPercentage = true;
        public boolean showProgressBar = true;
        public boolean showAttempts = true;
        public boolean showBest = true;
        public float uiPadding = 12f;
        public float practiceButtonOpacity = 0.5f;
    }

    private final Json json;

    /**
     * Initializes a new SettingsManager, configures the JSON serializer,
     * and attempts to load existing settings from the local storage.
     */
    public SettingsManager() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        load();
    }

    /**
     * Persists the current settings to a local JSON file.
     * <p>
     * This method captures a snapshot of the current configuration states,
     * serializes them into a formatted JSON string, and writes the output
     * to the local storage path defined by {@link #SAVE_PATH}.
     * </p>
     */
    public void save() {
        Gdx.app.log("SettingsManager", "Saving settings...");
        try {
            Data snapshot = new Data();
            snapshot.menuMusicEnabled = menuMusicEnabled;
            snapshot.musicVolume = musicVolume;
            snapshot.showHitboxes = showHitboxes;
            snapshot.showHitboxesOnDeath = showHitboxesOnDeath;
            snapshot.lockCursorInGame = lockCursorInGame;
            snapshot.showFps = showFps;
            snapshot.capFps = capFps;
            snapshot.fpsCapValue = fpsCapValue;
            snapshot.enableVsync = enableVsync;
            snapshot.showPercentage = showPercentage;
            snapshot.showProgressBar = showProgressBar;
            snapshot.showAttempts = showAttempts;
            snapshot.showBest = showBest;
            snapshot.uiPadding = uiPadding;
            snapshot.practiceButtonOpacity = practiceButtonOpacity;
            FileHandle file = Gdx.files.local(SAVE_PATH);
            file.parent().mkdirs();
            file.writeString(json.prettyPrint(snapshot), false);
            Gdx.app.log("SettingsManager", "Settings saved successfully.");
        } catch (Exception e) {
            Gdx.app.error("SettingsManager", "Failed to save: " + e.getMessage());
        }
    }

    /**
     * Loads the settings from the local storage file.
     * <p>
     * This method attempts to read the JSON file at {@link #SAVE_PATH}. If the file exists,
     * it deserializes the content into a {@link Data} object and updates the current
     * instance's fields with the stored values. If the file is missing or corrupted,
     * the default settings are retained and an error is logged.
     * </p>
     */
    private void load() {
        Gdx.app.log("SettingsManager", "Loading settings...");
        try {
            FileHandle file = Gdx.files.local(SAVE_PATH);
            if (!file.exists()) {
                Gdx.app.log("SettingsManager", "No settings file found. Using defaults.");
                return;
            }
            Data d = json.fromJson(Data.class, file);
            if (d == null) return;
            menuMusicEnabled = d.menuMusicEnabled;
            musicVolume = d.musicVolume;
            showHitboxes = d.showHitboxes;
            showHitboxesOnDeath = d.showHitboxesOnDeath;
            lockCursorInGame = d.lockCursorInGame;
            showFps = d.showFps;
            capFps = d.capFps;
            fpsCapValue = d.fpsCapValue;
            enableVsync = d.enableVsync;
            showPercentage = d.showPercentage;
            showProgressBar = d.showProgressBar;
            showAttempts = d.showAttempts;
            showBest = d.showBest;
            uiPadding = d.uiPadding;
            practiceButtonOpacity = d.practiceButtonOpacity;
            Gdx.app.log("SettingsManager", "Settings loaded successfully.");
        } catch (Exception e) {
            Gdx.app.error("SettingsManager", "Failed to load: " + e.getMessage());
        }
    }

    /**
     * Applies the frames per second (FPS) limit to the game's graphics settings.
     * <p>
     * If {@link #capFps} is enabled, the foreground FPS is restricted to the value
     * defined in {@link #fpsCapValue}. Otherwise, the cap is set to 0, which
     * effectively disables the FPS limit in LibGDX.
     * </p>
     */
    public void applyFpsCap() {
        Gdx.graphics.setForegroundFPS(capFps ? fpsCapValue : 0);
    }

    /**
     * Applies the vertical synchronization (VSync) setting to the graphics configuration.
     * <p>
     * VSync is automatically enabled on non-desktop platforms to ensure rendering stability.
     * On desktop platforms, the state is determined by the {@link #enableVsync} toggle.
     * </p>
     */
    public void applyVsync() {
        boolean vsync = (Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.Desktop)
            || enableVsync;
        Gdx.graphics.setVSync(vsync);
    }
}
