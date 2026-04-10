package io.github.msameer0.rhythmicrush.game.engine

import com.badlogic.gdx.math.MathUtils
import kotlin.math.min

class FixedTickEngine(private val tickable: Tickable) {
    companion object {
        const val TICK_RATE = 240
        const val TICK_DELTA = 1f / TICK_RATE
        const val BUFFER_STEPS = 6
        const val MAX_ACCUMULATOR = 0.25f
        const val QUEUE_CAPACITY = 16
    }

    private val eventHeld = BooleanArray(QUEUE_CAPACITY)
    private val eventOffset = FloatArray(QUEUE_CAPACITY)
    private var eventHead = 0
    private var eventCount = 0

    private var bufferedPress = false
    private var bufferStepsLeft = 0

    var accumulator = 0f
    private var frameStart = 0f

    fun queueInput(held: Boolean, frameOffset: Float) {
        if (!held) {
            bufferedPress = false
            bufferStepsLeft = 0
        }
        if (eventCount >= QUEUE_CAPACITY) return
        val slot = (eventHead + eventCount) % QUEUE_CAPACITY
        eventHeld[slot] = held
        eventOffset[slot] = frameOffset
        eventCount++
    }

    fun update(frameDelta: Float) {
        accumulator = min(accumulator + frameDelta, MAX_ACCUMULATOR)

        while (accumulator >= TICK_DELTA) {
            var tickRemaining = TICK_DELTA
            var timeConsumedInTick = 0f

            while (eventCount > 0 && eventOffset[eventHead] <= TICK_DELTA) {
                val offset = eventOffset[eventHead]
                val subDelta = offset - timeConsumedInTick

                if (subDelta > 0f) {
                    tickable.tick(subDelta)
                    tickRemaining -= subDelta
                    timeConsumedInTick = offset
                }

                val held = eventHeld[eventHead]
                eventHead = (eventHead + 1) % QUEUE_CAPACITY
                eventCount--

                if (!held) {
                    bufferedPress = false
                    bufferStepsLeft = 0
                    tickable.onInput(false)
                } else {
                    val consumed = tickable.onInput(true)
                    if (!consumed) {
                        bufferedPress = true
                        bufferStepsLeft = BUFFER_STEPS
                    }
                }
            }

            if (bufferedPress && bufferStepsLeft > 0) {
                val consumed = tickable.onInput(true)
                if (consumed) {
                    bufferedPress = false
                    bufferStepsLeft = 0
                } else {
                    bufferStepsLeft--
                    if (bufferStepsLeft == 0) bufferedPress = false
                }
            }

            if (tickRemaining > 0f) {
                tickable.tick(tickRemaining)
            }

            accumulator -= TICK_DELTA

            var ptr = eventHead
            for (i in 0 until eventCount) {
                eventOffset[ptr] -= TICK_DELTA
                ptr = (ptr + 1) % QUEUE_CAPACITY
            }
        }
    }

    fun reset() {
        accumulator = 0f
        frameStart = 0f
        eventHead = 0
        eventCount = 0
        bufferedPress = false
        bufferStepsLeft = 0
    }
}
