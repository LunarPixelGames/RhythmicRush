package io.github.msameer0.rhythmicrush.game.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Loads and saves per-level progress to {@code saves/progress.json}.
 *
 * Progress is keyed by the level's filename (e.g. {@code "0.json"}, {@code "1.json"}).
 * Adding a new level requires zero changes here — it gets its own entry automatically
 * the first time {@link #getOrCreate(String)} is called for it.
 *
 * Usage:
 * <pre>
 *   ProgressManager pm = game.getProgressManager();
 *   LevelProgress p = pm.getOrCreate("0.json");
 *   p.totalAttempts++;
 *   p.bestPercent = Math.max(p.bestPercent, currentPercent);
 *   pm.save();
 * </pre>
 */
public class ProgressManager {

    private static final String SAVE_PATH = "saves/progress.json";

    private final ObjectMap<String, LevelProgress> map = new ObjectMap<>();
    private final Json json;

    public ProgressManager() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        load();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the {@link LevelProgress} for the given level key,
     * creating a blank one (0 attempts, 0% best) if it doesn't exist yet.
     *
     * @param levelKey  the level filename, e.g. {@code "0.json"}
     */
    public LevelProgress getOrCreate(String levelKey) {
        if (!map.containsKey(levelKey)) {
            map.put(levelKey, new LevelProgress());
        }
        return map.get(levelKey);
    }

    /** Persists the current state of all progress entries to disk. */
    public void save() {
        try {
            FileHandle file = Gdx.files.local(SAVE_PATH);
            file.parent().mkdirs();

            // Manually build JSON so we get a readable key → object map
            StringBuilder sb = new StringBuilder("{\n");
            boolean first = true;
            for (ObjectMap.Entry<String, LevelProgress> entry : map) {
                if (!first) sb.append(",\n");
                first = false;
                sb.append("  \"").append(entry.key).append("\": ");
                sb.append(json.toJson(entry.value));
            }
            sb.append("\n}");
            file.writeString(sb.toString(), false);
        } catch (Exception e) {
            Gdx.app.error("ProgressManager", "Failed to save progress: " + e.getMessage());
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void load() {
        try {
            FileHandle file = Gdx.files.local(SAVE_PATH);
            if (!file.exists()) return;

            JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(file);
            for (JsonValue entry = root.child; entry != null; entry = entry.next) {
                LevelProgress p = json.readValue(LevelProgress.class, entry);
                if (p != null) map.put(entry.name, p);
            }
        } catch (Exception e) {
            Gdx.app.error("ProgressManager", "Failed to load progress: " + e.getMessage());
        }
    }
}
