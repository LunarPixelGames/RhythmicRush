package io.github.msameer0.rhythmicrush.game.engine;

/**
 * Drives a {@link Tickable} at a fixed rate of {@value TICK_RATE} ticks per second,
 * completely decoupled from the render frame rate.
 *
 * <p>Usage — in your render/update loop:</p>
 * <pre>
 *     engine.update(Gdx.graphics.getDeltaTime());
 * </pre>
 *
 * <p>The accumulator is capped at {@value MAX_ACCUMULATOR} seconds so that a long
 * window freeze or debugger pause cannot cause a runaway burst of catch-up ticks.</p>
 */
public class FixedTickEngine {

    /** Physics ticks per second. */
    public static final int   TICK_RATE  = 240;

    /** Seconds per tick — passed to {@link Tickable#tick(float)} on every step. */
    public static final float TICK_DELTA = 1f / TICK_RATE;

    /**
     * Maximum time (seconds) the accumulator is allowed to hold.
     * Excess real time is discarded, preventing a death-spiral of catch-up ticks
     * after the window is frozen or the debugger paused.
     */
    private static final float MAX_ACCUMULATOR = 0.25f;

    private final Tickable tickable;
    private float          accumulator = 0f;

    public FixedTickEngine(Tickable tickable) {
        this.tickable = tickable;
    }

    /**
     * Call once per rendered frame with the real elapsed time.
     * Internally converts it into as many fixed {@value TICK_DELTA}-second
     * ticks as have accumulated.
     *
     * @param frameDelta seconds since the last rendered frame (raw delta time)
     */
    public void update(float frameDelta) {
        accumulator = Math.min(accumulator + frameDelta, MAX_ACCUMULATOR);
        while (accumulator >= TICK_DELTA) {
            tickable.tick(TICK_DELTA);
            accumulator -= TICK_DELTA;
        }
    }

    /** Resets the accumulator — call after a respawn or scene transition
     *  so stale time doesn't bleed into the new attempt. */
    public void reset() {
        accumulator = 0f;
    }
}
