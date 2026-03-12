package io.github.msameer0.rhythmicrush.game.level;

/**
 * Persistent progress for a single level.
 * Serialised as one entry inside progress.json — keyed by level filename.
 */
public class LevelProgress {

    /** Total lifetime attempts across all sessions. */
    public int totalAttempts = 0;

    /**
     * Best completion percentage ever reached (0–100).
     * Updated on death (current %) and on level complete (100).
     */
    public int bestPercent = 0;

    /** Required by LibGDX Json deserialiser. */
    public LevelProgress() {}
}
