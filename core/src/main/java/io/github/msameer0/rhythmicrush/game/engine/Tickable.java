package io.github.msameer0.rhythmicrush.game.engine;

/**
 * Implemented by anything that wants to be driven by the fixed-step engine.
 * {@link FixedTickEngine} calls {@code tick()} at a constant rate regardless
 * of the actual frame rate.
 */
public interface Tickable {
    /**
     * Called once per fixed physics step.
     *
     * @param delta always equals {@code FixedTickEngine.TICK_DELTA} (1 / 240 s)
     */
    void tick(float delta);
}
