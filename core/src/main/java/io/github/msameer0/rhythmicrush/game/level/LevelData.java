package io.github.msameer0.rhythmicrush.game.level;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain data container for a level. Serialized to/from JSON.
 * Lives in core — no platform-specific imports.
 */
public class LevelData {

    // ── Metadata ──────────────────────────────────────────────────────────────
    public String name        = "Unnamed Level";
    public String musicFile   = "";
    public String bgColor     = "1a1a2e";   // hex, no #
    public String groundColor = "16213e";   // hex, no #
    public String difficulty  = "normal";   // easy normal hard insane extreme

    // ── Objects ───────────────────────────────────────────────────────────────
    public List<ObjectEntry> objects = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    /** Every placeable object — blocks, hazards, portals, triggers — is one of these. */
    public static class ObjectEntry {
        public String type;       // "block"|"spike"|"cube_portal"|"ship_portal"|"color_trigger"
        public float  x, y, size;
        public String blockType;  // textureName for blocks, null otherwise
        public float  rotation;   // degrees — used by spikes

        // ── Color trigger fields — null means "leave this color unchanged" ────
        public String triggerBgColor;      // hex, nullable
        public String triggerGroundColor;  // hex, nullable
        public float  fadeDuration = 1f;   // seconds to lerp to new colors

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
     * Returns the X of the rightmost non-trigger object — used for progress and level end.
     * Color triggers are excluded so they don't artificially extend the level length.
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
