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
    public boolean menuMusicEnabled    = true;
    public float   musicVolume         = 1f;
    public boolean showHitboxes        = false;
    public boolean showHitboxesOnDeath = false;
    public boolean lockCursorInGame    = false;

    // ── Plain data class used only for deserialization — no constructor side effects ──
    public static class Data {
        public boolean menuMusicEnabled    = true;
        public float   musicVolume         = 1f;
        public boolean showHitboxes        = false;
        public boolean showHitboxesOnDeath = false;
        public boolean lockCursorInGame    = false;
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
            // Serialize a Data snapshot — not `this` — to avoid including the Json field
            Data snapshot = new Data();
            snapshot.menuMusicEnabled    = menuMusicEnabled;
            snapshot.musicVolume         = musicVolume;
            snapshot.showHitboxes        = showHitboxes;
            snapshot.showHitboxesOnDeath = showHitboxesOnDeath;
            snapshot.lockCursorInGame    = lockCursorInGame;
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
            // Deserialize into Data (plain POJO, no constructor side effects)
            Data d = json.fromJson(Data.class, file);
            if (d == null) return;
            menuMusicEnabled    = d.menuMusicEnabled;
            musicVolume         = d.musicVolume;
            showHitboxes        = d.showHitboxes;
            showHitboxesOnDeath = d.showHitboxesOnDeath;
            lockCursorInGame    = d.lockCursorInGame;
        } catch (Exception e) {
            Gdx.app.error("SettingsManager", "Failed to load: " + e.getMessage());
        }
    }
}
