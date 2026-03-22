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
     * Called by {@link FixedTickEngine} when an input event should be delivered.
     *
     * <p>Used by both <b>click-between-steps</b> (delivered at the step closest
     * to when the input was detected) and <b>click-on-steps</b> (buffered and
     * retried until the player can act on it).</p>
     *
     * <p>Return {@code true} if the input was consumed — the player was in a state
     * where the input did something meaningful (e.g. cube is grounded, so jump fired).
     * Return {@code false} if the input could not be acted on yet — the engine will
     * retry it on the next step for up to {@link FixedTickEngine#BUFFER_STEPS} steps.</p>
     *
     * <p>Releases ({@code held=false}) should always return {@code true} — they are
     * never buffered.</p>
     *
     * @param held true = jump/fly pressed, false = released
     * @return true if consumed, false if should be retried next step
     */
    default boolean onInput(boolean held) { return true; }
}
