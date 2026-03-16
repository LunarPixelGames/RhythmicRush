package io.github.msameer0.rhythmicrush.game.engine;

/**
 * Drives a {@link Tickable} at a fixed rate of {@value TICK_RATE} ticks per second,
 * completely decoupled from the render frame rate.
 *
 * <h3>Click-between-steps</h3>
 * Input events (jump press / release) are queued via {@link #queueInput(boolean, float)}
 * with a frame-time offset. During {@link #update(float)}, each tick checks whether
 * any queued event falls within its time window and delivers it at the correct step
 * rather than always at the first step of the next frame.
 *
 * <p>This ensures jumps are registered at the physics step closest to when the
 * player actually tapped, regardless of frame rate — critical for tight/precise levels.</p>
 */
public class FixedTickEngine {

    public static final int   TICK_RATE  = 240;
    public static final float TICK_DELTA = 1f / TICK_RATE;

    private static final float MAX_ACCUMULATOR = 0.25f;

    // ── Input event queue ─────────────────────────────────────────────────────
    // Fixed-size ring buffer — no allocation mid-game.
    // Capacity of 16 is more than enough (you can't physically queue 16 distinct
    // press/release events in a single 16ms frame).
    private static final int QUEUE_CAPACITY = 16;
    private final boolean[] eventHeld       = new boolean[QUEUE_CAPACITY];
    private final float[]   eventOffset     = new float[QUEUE_CAPACITY]; // seconds from frame start
    private int             eventHead       = 0;
    private int             eventCount      = 0;

    private final Tickable tickable;
    private float          accumulator  = 0f;
    // Tracks the accumulator value at the start of the current frame so we
    // can compute each tick's absolute time offset within the frame.
    private float          frameStart   = 0f;

    public FixedTickEngine(Tickable tickable) {
        this.tickable = tickable;
    }

    /**
     * Queue a jump input event to be delivered at the correct physics step.
     *
     * <p>Call this when a jump press or release is <em>detected</em> (i.e. on
     * state change only, not every frame). Pass the current accumulator value
     * so the engine knows where within the frame this event occurred.</p>
     *
     * @param held          true = jump pressed, false = jump released
     * @param frameOffset   seconds elapsed since the start of this frame
     *                      (pass {@code accumulator} from the caller before
     *                      calling {@link #update(float)})
     */
    public void queueInput(boolean held, float frameOffset) {
        if (eventCount >= QUEUE_CAPACITY) return; // queue full — drop (very rare)
        int slot = (eventHead + eventCount) % QUEUE_CAPACITY;
        eventHeld[slot]   = held;
        eventOffset[slot] = frameOffset;
        eventCount++;
    }

    /**
     * Call once per rendered frame with the real elapsed time.
     * Delivers queued input events at the correct physics step.
     */
    public void update(float frameDelta) {
        accumulator = Math.min(accumulator + frameDelta, MAX_ACCUMULATOR);
        frameStart  = accumulator; // record how much time we're about to consume

        float elapsed = 0f; // time consumed so far this frame (in seconds)

        while (accumulator >= TICK_DELTA) {
            // Deliver any input events whose offset falls before this tick ends
            float tickEnd = elapsed + TICK_DELTA;
            while (eventCount > 0 && eventOffset[eventHead] <= tickEnd) {
                tickable.onInput(eventHeld[eventHead]);
                eventHead  = (eventHead + 1) % QUEUE_CAPACITY;
                eventCount--;
            }

            tickable.tick(TICK_DELTA);
            accumulator -= TICK_DELTA;
            elapsed     += TICK_DELTA;
        }

        // Deliver any remaining events that didn't fit into a tick this frame
        // (accumulator < TICK_DELTA left over) — apply them now so they're
        // ready for the first tick of the next frame.
        while (eventCount > 0) {
            tickable.onInput(eventHeld[eventHead]);
            eventHead  = (eventHead + 1) % QUEUE_CAPACITY;
            eventCount--;
        }
    }

    /** Resets the accumulator and clears queued input. Call after respawn. */
    public void reset() {
        accumulator = 0f;
        frameStart  = 0f;
        eventHead   = 0;
        eventCount  = 0;
    }

    /** Current accumulator value — use as frameOffset when queuing input. */
    public float getAccumulator() { return accumulator; }
}
