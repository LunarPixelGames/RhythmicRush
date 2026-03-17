package io.github.msameer0.rhythmicrush.game.level;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Utility class for serializing and deserializing level data to and from JSON format.
 * This class uses LibGDX's {@link Json} utility to handle the conversion of
 * {@link LevelData} objects.
 */
public class LevelSerializer {

    private static final Json json = buildJson();

    /**
     * Configures and initializes a {@link Json} instance for level serialization.
     * Sets the output type to standard JSON and disables the use of prototypes
     * to ensure consistent data representation.
     *
     * @return a configured {@link Json} instance
     */
    private static Json buildJson() {
        Json j = new Json();
        j.setOutputType(JsonWriter.OutputType.json);
        j.setUsePrototypes(false);
        return j;
    }

    /**
     * Serializes the provided {@link LevelData} into a pretty-printed JSON string
     * and writes it to the specified file.
     *
     * @param data the level data object to be saved
     * @param file the file handle representing the destination file
     */
    public static void save(LevelData data, FileHandle file) {
        String out = json.prettyPrint(data);
        file.writeString(out, false);
    }

    /**
     * Loads a {@link LevelData} object from a JSON file.
     * This method also sets the {@code fileName} property of the resulting object
     * to match the name of the file on disk.
     *
     * @param file the handle to the JSON file to be loaded
     * @return the deserialized {@link LevelData} instance, or {@code null} if the file content is invalid
     */
    public static LevelData load(FileHandle file) {
        LevelData data = json.fromJson(LevelData.class, file);
        if (data != null) data.fileName = file.name(); // e.g. "0.json"
        return data;
    }

    /**
     * Deserializes a {@link LevelData} object from a JSON string.
     *
     * @param jsonText the JSON string representing the level data
     * @return a {@link LevelData} instance populated from the provided JSON string
     */
    public static LevelData fromString(String jsonText) {
        return json.fromJson(LevelData.class, jsonText);
    }
}
