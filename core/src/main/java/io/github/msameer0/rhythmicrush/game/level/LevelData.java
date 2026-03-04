package io.github.msameer0.rhythmicrush.game.level;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain data container for a level. Serialized to/from JSON.
 * Lives in core — no platform-specific imports.
 */
public class LevelData {

    // ── Metadata ─────────────────────────────────────────────────────────────
    public String name        = "Unnamed Level";
    public String musicFile   = "";          // e.g. "musics/song.mp3"
    public String bgColor     = "1a1a2e";    // hex, no #
    public String groundColor = "16213e";    // hex, no #

    // ── Objects ───────────────────────────────────────────────────────────────
    public List<ObjectEntry> objects = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    /** Every placeable object is stored as one of these. */
    public static class ObjectEntry {
        public String type;   // "block" | "spike" | "cube_portal" | "ship_portal"
        public float  x;
        public float  y;
        public float  size;   // used by block (width == height == size)

        /** No-arg constructor required by LibGDX Json deserialiser. */
        public ObjectEntry() {}

        public ObjectEntry(String type, float x, float y, float size) {
            this.type = type;
            this.x    = x;
            this.y    = y;
            this.size = size;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the X coordinate of the rightmost object in the level,
     * which is used to calculate level length and progress.
     */
    public float getLevelEndX() {
        float max = 0;
        for (ObjectEntry e : objects) {
            float right = e.x + e.size;
            if (right > max) max = right;
        }
        return max;
    }
}
