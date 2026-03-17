package io.github.msameer0.rhythmicrush.game.level;

import com.badlogic.gdx.utils.Array;

/**
 * Represents the data structure for a level in the game.
 * This class contains metadata, visual settings, and a list of all objects
 * and triggers that constitute a playable level.
 */
public class LevelData {

    public String name = "Unnamed Level";
    public String fileName = null;
    public String musicFile = "";
    public String bgColor = "1a1a2e";
    public String groundColor = "16213e";
    public String difficulty = "normal";

    public Array<ObjectEntry> objects = new Array<>();


    /**
     * Represents an individual entity or event within a level.
     * This includes physical obstacles (blocks, spikes) and functional triggers (color changes).
     */
    public static class ObjectEntry {
        public String type;
        public float x, y, size;
        public String blockType;
        public float rotation;

        public String triggerBgColor;
        public String triggerGroundColor;
        public float fadeDuration = 1f;

        public ObjectEntry() {
        }

        public ObjectEntry(String type, float x, float y, float size) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }

    /**
     * Calculates the horizontal end point of the level based on the positions and sizes
     * of its physical objects. Triggers (such as color changes) are ignored.
     *
     * @return The maximum X-coordinate reached by any physical object in the level.
     */
    public float getLevelEndX() {
        float max = 0;
        for (ObjectEntry e : objects) {
            if ("color_trigger".equals(e.type)) continue;
            float right = e.x + e.size;
            if (right > max) max = right;
        }
        return max;
    }
}
