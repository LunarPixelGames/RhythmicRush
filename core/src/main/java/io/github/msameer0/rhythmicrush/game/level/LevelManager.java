package io.github.msameer0.rhythmicrush.game.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

/**
 * Manages the loading and caching of all level data in the game.
 * This class scans the internal levels directory at startup and stores
 * {@link LevelData} objects in memory for efficient access during gameplay
 * and level selection.
 */
public class LevelManager {

    private final Array<LevelData> levels = new Array<>();

    public LevelManager() {
        Gdx.app.log("LevelManager", "Scanning and pre-loading all levels...");
        loadAll();
        Gdx.app.log("LevelManager", "Pre-loaded " + levels.size + " levels.");
    }

    /**
     * Scans the internal assets directory for level files and populates the level list.
     * This method searches for JSON files in the "levels/" directory following a numeric
     * naming convention (e.g., "0.json", "1.json").
     */
    private void loadAll() {
        levels.clear();
        int index = 0;
        while (true) {
            FileHandle fh = Gdx.files.internal("levels/" + index + ".json");
            if (!fh.exists()) break;
            try {
                LevelData data = LevelSerializer.load(fh);
                if (data != null) {
                    levels.add(data);
                }
                index++;
            } catch (Exception e) {
                Gdx.app.error("LevelManager", "Failed to load level " + index + ": " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Retrieves the list of all loaded levels.
     * @return An {@link Array} of {@link LevelData} objects.
     */
    public Array<LevelData> getLevels() {
        return levels;
    }

    /**
     * Retrieves a specific level by its index.
     * @param index The index of the level to retrieve.
     * @return The {@link LevelData} for the specified index, or null if out of bounds.
     */
    public LevelData getLevel(int index) {
        if (index >= 0 && index < levels.size) {
            return levels.get(index);
        }
        return null;
    }

    /**
     * Returns the number of loaded levels.
     * @return The size of the levels array.
     */
    public int getLevelCount() {
        return levels.size;
    }
}
