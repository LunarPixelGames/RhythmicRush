package io.github.msameer0.rhythmicrush.game.engine;

/**
 * Implemented by anything driven by {@link FixedTickEngine}.
 */
public interface Tickable {

    /**
     * Called once per fixed physics step.
     *
     * @param delta always {@code FixedTickEngine.TICK_DELTA} (1/240 s)
     */
    void tick(float delta);

    /**
     * Called by {@link FixedTickEngine} at the physics step that corresponds to
     * when the input event was actually detected — implementing click-between-steps.
     *
     * <p>Override this to receive jump press/release at the correct step.
     * Default is a no-op so existing Tickable implementations don't break.</p>
     *
     * @param held true = jump/fly pressed, false = released
     */
    default void onInput(boolean held) {
    }
}
