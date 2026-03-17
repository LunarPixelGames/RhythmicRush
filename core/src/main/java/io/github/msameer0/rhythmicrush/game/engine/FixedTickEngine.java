package io.github.msameer0.rhythmicrush.game.engine;

/**
 * A fixed-timestep game engine designed to decouple game logic updates from the rendering frame rate.
 *
 * <p>The engine maintains a constant tick rate (default 240Hz) using an accumulator to ensure
 * deterministic physics and logic updates regardless of the hardware's frame rate. It includes
 * an internal event queue to synchronize input events with the high-frequency ticks, preventing
 * input lag or missed inputs between frames.</p>
 *
 * <p>This implementation is particularly suited for rhythm or precision-based games where
 * timing consistency is critical.</p>
 */
public class FixedTickEngine {

    public static final int TICK_RATE = 240;
    public static final float TICK_DELTA = 1f / TICK_RATE;

    private static final float MAX_ACCUMULATOR = 0.25f;

    private static final int QUEUE_CAPACITY = 16;
    private final boolean[] eventHeld = new boolean[QUEUE_CAPACITY];
    private final float[] eventOffset = new float[QUEUE_CAPACITY];
    private int eventHead = 0;
    private int eventCount = 0;

    private final Tickable tickable;
    private float accumulator = 0f;
    private float frameStart = 0f;

    /**
     * Constructs a new FixedTickEngine with the specified logic handler.
     *
     * @param tickable the instance that will receive game logic updates and synchronized input events
     */
    public FixedTickEngine(Tickable tickable) {
        this.tickable = tickable;
    }

    /**
     * Queues an input event to be processed during the next engine update.
     *
     * <p>This allows input events captured at the variable frame rate to be synchronized
     * with the fixed tick rate. The event will be dispatched to the {@link Tickable}
     * instance when the engine's internal clock reaches the specified offset.</p>
     *
     * @param held whether the input key is currently pressed
     * @param frameOffset the timing offset of the input, typically retrieved via {@link #getAccumulator()}
     */
    public void queueInput(boolean held, float frameOffset) {
        if (eventCount >= QUEUE_CAPACITY) return;
        int slot = (eventHead + eventCount) % QUEUE_CAPACITY;
        eventHeld[slot] = held;
        eventOffset[slot] = frameOffset;
        eventCount++;
    }

    /**
     * Updates the engine's internal clock and executes the necessary number of logic ticks
     * based on the elapsed time.
     *
     * <p>This method implements a fixed-timestep loop using an accumulator. It ensures that
     * the game logic is updated at a consistent rate ({@link #TICK_RATE}) regardless of the
     * rendering frame rate. During the update, any queued input events are dispatched to
     * the {@link Tickable} instance at the precise moment they occurred relative to the
     * logic ticks.</p>
     *
     */
    public void update(float frameDelta) {
        accumulator = Math.min(accumulator + frameDelta, MAX_ACCUMULATOR);
        frameStart = accumulator;

        float elapsed = 0f;

        while (accumulator >= TICK_DELTA) {
            float tickEnd = elapsed + TICK_DELTA;
            while (eventCount > 0 && eventOffset[eventHead] <= tickEnd) {
                tickable.onInput(eventHeld[eventHead]);
                eventHead = (eventHead + 1) % QUEUE_CAPACITY;
                eventCount--;
            }

            tickable.tick(TICK_DELTA);
            accumulator -= TICK_DELTA;
            elapsed += TICK_DELTA;
        }

        while (eventCount > 0) {
            tickable.onInput(eventHeld[eventHead]);
            eventHead = (eventHead + 1) % QUEUE_CAPACITY;
            eventCount--;
        }
    }

    /**
     * Resets the accumulator and clears queued input. Call after respawn.
     */
    public void reset() {
        accumulator = 0f;
        frameStart = 0f;
        eventHead = 0;
        eventCount = 0;
    }

    /**
     * Current accumulator value — use as frameOffset when queuing input.
     */
    public float getAccumulator() {
        return accumulator;
    }
}
