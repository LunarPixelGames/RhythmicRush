package io.github.msameer0.rhythmicrush.game.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectMap;


/**
 * Manages the persistence of player progress for different levels.
 * Handles the loading and saving of {@link LevelProgress} data to a local JSON file.
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


    public LevelProgress getOrCreate(String levelKey) {
        if (!map.containsKey(levelKey)) {
            map.put(levelKey, new LevelProgress());
        }
        return map.get(levelKey);
    }

    /**
     * Persists the current player progress to a local JSON file.
     * <p>
     * This method serializes the internal map of {@link LevelProgress} objects into a JSON format
     * and writes it to the local storage path defined by {@code SAVE_PATH}. If the parent
     * directories do not exist, they are created automatically. In the event of an IO failure
     * or serialization error, the exception is caught and logged via {@link Gdx#app}.
     * </p>
     */
    public void save() {
        try {
            FileHandle file = Gdx.files.local(SAVE_PATH);
            file.parent().mkdirs();

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


    /**
     * Loads the player progress from the local storage file.
     * <p>
     * This method attempts to read the JSON file at {@code SAVE_PATH}. If the file exists,
     * it parses the content and populates the internal progress map. If the file does not
     * exist, the method returns silently, leaving the map empty. Any exceptions encountered
     * during the reading or parsing process are caught and logged.
     * </p>
     */
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
