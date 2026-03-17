package io.github.msameer0.rhythmicrush.game.level;

/**
 * Represents the persistent progress data for an individual level.
 * This class tracks usage statistics and completion milestones, such as total attempts
 * and the highest percentage reached. It is designed to be serialized as part of
 * the game's progress save file.
 */
public class LevelProgress {
    public int totalAttempts = 0;
    public int bestPercent = 0;

    public LevelProgress() {
    }
}
