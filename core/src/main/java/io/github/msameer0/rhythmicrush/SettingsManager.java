package io.github.msameer0.rhythmicrush;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Holds all user-configurable settings and persists them to {@code saves/settings.json}.
 *
 * Add new fields here freely — they'll be saved/loaded automatically via LibGDX Json.
 * Defaults are applied if the file doesn't exist yet.
 */
public class SettingsManager {

    private static final String SAVE_PATH = "saves/settings.json";

    // ── Settings fields ───────────────────────────────────────────────────────
    public boolean menuMusicEnabled   = true;
    public float   musicVolume        = 1f;   // 0.0 – 1.0
    public boolean showHitboxes       = false;
    public boolean showHitboxesOnDeath = false;

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
            FileHandle file = Gdx.files.local(SAVE_PATH);
            file.parent().mkdirs();
            file.writeString(json.prettyPrint(this), false);
        } catch (Exception e) {
            Gdx.app.error("SettingsManager", "Failed to save: " + e.getMessage());
        }
    }

    private void load() {
        try {
            FileHandle file = Gdx.files.local(SAVE_PATH);
            if (!file.exists()) return;
            SettingsManager loaded = json.fromJson(SettingsManager.class, file);
            if (loaded == null) return;
            menuMusicEnabled    = loaded.menuMusicEnabled;
            musicVolume         = loaded.musicVolume;
            showHitboxes        = loaded.showHitboxes;
            showHitboxesOnDeath = loaded.showHitboxesOnDeath;
        } catch (Exception e) {
            Gdx.app.error("SettingsManager", "Failed to load: " + e.getMessage());
        }
    }
}
