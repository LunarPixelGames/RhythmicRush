package io.github.msameer0.rhythmicrush.game.engine;

/**
 * Drives a {@link Tickable} at a fixed rate of {@value TICK_RATE} ticks per second,
 * completely decoupled from the render frame rate.
 *
 * <h3>Click-between-steps</h3>
 * Input events are queued via {@link #queueInput(boolean, float)} with a frame-time
 * offset. During {@link #update(float)}, each tick checks whether any queued event
 * falls within its time window and delivers it at the correct step rather than always
 * at the first step of the next frame.
 *
 * <h3>Click-on-steps (input buffering)</h3>
 * If a press input is delivered at the correct step but {@link Tickable#onInput}
 * returns {@code false} (player can't act on it yet — e.g. cube is mid-air), the
 * engine holds the input for up to {@value BUFFER_STEPS} additional steps and retries
 * it each step until it is consumed or the buffer expires. This ensures that taps
 * slightly before a landing are still registered, which is critical for players
 * running below 240 Hz.
 *
 * <p>Releases ({@code held=false}) are never buffered — they always clear the buffer.</p>
 */
public class FixedTickEngine {

    public static final int   TICK_RATE    = 240;
    public static final float TICK_DELTA   = 1f / TICK_RATE;

    /**
     * How many steps a press input is held and retried if not immediately consumed.
     * 6 steps ≈ 25 ms at 240 TPS — enough to cover ~1.5 frames at 60 fps.
     */
    public static final int BUFFER_STEPS = 6;

    private static final float MAX_ACCUMULATOR = 0.25f;

    private static final int QUEUE_CAPACITY = 16;
    private final boolean[] eventHeld   = new boolean[QUEUE_CAPACITY];
    private final float[]   eventOffset = new float[QUEUE_CAPACITY];
    private int             eventHead   = 0;
    private int             eventCount  = 0;

    private boolean bufferedPress    = false;
    private int     bufferStepsLeft  = 0;

    private final Tickable tickable;
    private float          accumulator = 0f;
    private float          frameStart  = 0f;

    public FixedTickEngine(Tickable tickable) {
        this.tickable = tickable;
    }

    /**
     * Queue a jump input event to be delivered at the correct physics step.
     *
     * @param held        true = jump pressed, false = released
     * @param frameOffset seconds elapsed since the start of this frame
     *                    (pass {@code getAccumulator()} before calling {@link #update(float)})
     */
    public void queueInput(boolean held, float frameOffset) {
        if (!held) {
            bufferedPress   = false;
            bufferStepsLeft = 0;
        }
        if (eventCount >= QUEUE_CAPACITY) return;
        int slot = (eventHead + eventCount) % QUEUE_CAPACITY;
        eventHeld[slot]   = held;
        eventOffset[slot] = frameOffset;
        eventCount++;
    }

    /**
     * Call once per rendered frame. Delivers queued inputs at the correct step
     * and retries buffered presses each step until consumed or expired.
     */
    public void update(float frameDelta) {
        frameStart  = accumulator;
        accumulator = Math.min(accumulator + frameDelta, MAX_ACCUMULATOR);

        float elapsed = 0f;

        while (accumulator >= TICK_DELTA) {
            float tickEnd = frameStart + elapsed + TICK_DELTA;

            while (eventCount > 0 && eventOffset[eventHead] <= tickEnd) {
                boolean held = eventHeld[eventHead];
                eventHead  = (eventHead + 1) % QUEUE_CAPACITY;
                eventCount--;

                if (!held) {
                    bufferedPress   = false;
                    bufferStepsLeft = 0;
                    tickable.onInput(false);
                } else {
                    boolean consumed = tickable.onInput(true);
                    if (!consumed) {
                        bufferedPress   = true;
                        bufferStepsLeft = BUFFER_STEPS;
                    }
                }
            }

            if (bufferedPress && bufferStepsLeft > 0) {
                boolean consumed = tickable.onInput(true);
                if (consumed) {
                    bufferedPress   = false;
                    bufferStepsLeft = 0;
                } else {
                    bufferStepsLeft--;
                    if (bufferStepsLeft == 0) bufferedPress = false;
                }
            }

            tickable.tick(TICK_DELTA);
            accumulator -= TICK_DELTA;
            elapsed     += TICK_DELTA;
        }
    }

    /** Resets accumulator, queued inputs, and the buffer. Call after respawn. */
    public void reset() {
        accumulator     = 0f;
        frameStart      = 0f;
        eventHead       = 0;
        eventCount      = 0;
        bufferedPress   = false;
        bufferStepsLeft = 0;
    }

    /** Current accumulator value — pass as frameOffset when queuing input. */
    public float getAccumulator() { return accumulator; }
}
