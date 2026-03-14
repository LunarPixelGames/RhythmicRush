package io.github.msameer0.rhythmicrush.game.level;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Converts LevelData ↔ JSON files.
 * Uses LibGDX's built-in Json so it works on all platforms.
 */
public class LevelSerializer {

    private static final Json json = buildJson();

    private static Json buildJson() {
        Json j = new Json();
        j.setOutputType(JsonWriter.OutputType.json);  // strict, human-readable JSON
        j.setUsePrototypes(false);                    // always write all fields
        return j;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Serialises level to a pretty-printed JSON file. */
    public static void save(LevelData data, FileHandle file) {
        String out = json.prettyPrint(data);
        file.writeString(out, false);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Deserialises a JSON file back into a LevelData. */
    public static LevelData load(FileHandle file) {
        LevelData data = json.fromJson(LevelData.class, file);
        if (data != null) data.fileName = file.name(); // e.g. "0.json"
        return data;
    }

    /** Deserialises from a raw JSON string (useful for testing). */
    public static LevelData fromString(String jsonText) {
        return json.fromJson(LevelData.class, jsonText);
    }
}
