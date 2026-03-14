package io.github.msameer0.rhythmicrush;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Holds all user-configurable settings and persists them to {@code saves/settings.json}.
 */
public class SettingsManager {

    private static final String SAVE_PATH = "saves/settings.json";

    // ── Settings fields ───────────────────────────────────────────────────────
    public boolean menuMusicEnabled    = true;
    public float   musicVolume         = 1f;
    public boolean showHitboxes        = false;
    public boolean showHitboxesOnDeath = false;
    public boolean lockCursorInGame    = false;
    public boolean showFps             = false;
    public boolean capFps              = false;
    public int     fpsCapValue         = 60;
    public boolean enableVsync         = false;

    // ── Plain data class used only for deserialization ────────────────────────
    public static class Data {
        public boolean menuMusicEnabled    = true;
        public float   musicVolume         = 1f;
        public boolean showHitboxes        = false;
        public boolean showHitboxesOnDeath = false;
        public boolean lockCursorInGame    = false;
        public boolean showFps             = false;
        public boolean capFps              = false;
        public int     fpsCapValue         = 60;
        public boolean enableVsync         = false;
    }

    // ── Internal ──────────────────────────────────────────────────────────────
    private final Json json;

    public SettingsManager() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        load();
    }

    public void save() {
        try {
            Data snapshot = new Data();
            snapshot.menuMusicEnabled    = menuMusicEnabled;
            snapshot.musicVolume         = musicVolume;
            snapshot.showHitboxes        = showHitboxes;
            snapshot.showHitboxesOnDeath = showHitboxesOnDeath;
            snapshot.lockCursorInGame    = lockCursorInGame;
            snapshot.showFps             = showFps;
            snapshot.capFps              = capFps;
            snapshot.fpsCapValue         = fpsCapValue;
            snapshot.enableVsync         = enableVsync;
            FileHandle file = Gdx.files.local(SAVE_PATH);
            file.parent().mkdirs();
            file.writeString(json.prettyPrint(snapshot), false);
        } catch (Exception e) {
            Gdx.app.error("SettingsManager", "Failed to save: " + e.getMessage());
        }
    }

    private void load() {
        try {
            FileHandle file = Gdx.files.local(SAVE_PATH);
            if (!file.exists()) return;
            Data d = json.fromJson(Data.class, file);
            if (d == null) return;
            menuMusicEnabled    = d.menuMusicEnabled;
            musicVolume         = d.musicVolume;
            showHitboxes        = d.showHitboxes;
            showHitboxesOnDeath = d.showHitboxesOnDeath;
            lockCursorInGame    = d.lockCursorInGame;
            showFps             = d.showFps;
            capFps              = d.capFps;
            fpsCapValue         = d.fpsCapValue;
            enableVsync         = d.enableVsync;
        } catch (Exception e) {
            Gdx.app.error("SettingsManager", "Failed to load: " + e.getMessage());
        }
    }

    /** Applies the current fps cap setting to the graphics backend. Call after load or change. */
    public void applyFpsCap() {
        Gdx.graphics.setForegroundFPS(capFps ? fpsCapValue : 0);
    }

    /** Applies the current vsync setting. On non-desktop platforms vsync is always on. */
    public void applyVsync() {
        boolean vsync = (Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.Desktop)
            || enableVsync;
        Gdx.graphics.setVSync(vsync);
    }
}
