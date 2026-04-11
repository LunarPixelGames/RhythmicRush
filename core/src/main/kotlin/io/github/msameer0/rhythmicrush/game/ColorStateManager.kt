package io.github.msameer0.rhythmicrush.game

import com.badlogic.gdx.graphics.Color
import kotlin.math.min

/**
 * Manages the background and ground colors, supporting fades and pulses triggered by game events.
 */
class ColorStateManager {

    /**
     * Internal state representing a color transition over time.
     */
    private class ColorFade {
        val from = Color()
        val to = Color()
        var duration = 0f
        var elapsed = 0f
        var active = false

        fun init(from: Color, to: Color, duration: Float) {
            this.from.set(from)
            this.to.set(to)
            this.duration = duration
            this.elapsed = 0f
            this.active = true
        }
    }

    /**
     * Internal state representing a temporary color pulse with fade-in, hold, and fade-out phases.
     */
    private class ColorPulse {
        val target = Color()
        var fadeIn = 0f
        var hold = 0f
        var fadeOut = 0f
        var elapsed = 0f
        var active = false

        fun init(target: Color, fadeIn: Float, hold: Float, fadeOut: Float) {
            this.target.set(target)
            this.fadeIn = fadeIn
            this.hold = hold
            this.fadeOut = fadeOut
            this.elapsed = 0f
            this.active = true
        }

        fun getIntensity(): Float {
            if (!active) return 0f
            if (elapsed < fadeIn) return if (fadeIn > 0) elapsed / fadeIn else 1f
            if (elapsed < fadeIn + hold) return 1f
            if (elapsed < fadeIn + hold + fadeOut) return if (fadeOut > 0) 1f - (elapsed - fadeIn - hold) / fadeOut else 0f
            return 0f
        }

        fun update(delta: Float) {
            if (!active) return
            elapsed += delta
            if (elapsed >= fadeIn + hold + fadeOut) active = false
        }
    }

    private val bgFade = ColorFade()
    private val groundFade = ColorFade()
    private val bgPulse = ColorPulse()
    private val groundPulse = ColorPulse()

    val baseBgColor = Color(0.1f, 0.1f, 0.18f, 1f)
    val baseGroundColor = Color(0.09f, 0.13f, 0.24f, 1f)

    val backgroundColor = Color(baseBgColor)
    val groundColor = Color(baseGroundColor)

    fun startBgFade(target: Color, duration: Float) {
        bgFade.init(baseBgColor, target, duration)
    }

    fun startGroundFade(target: Color, duration: Float) {
        groundFade.init(baseGroundColor, target, duration)
    }

    fun startBgPulse(target: Color, fadeIn: Float, hold: Float, fadeOut: Float) {
        bgPulse.init(target, fadeIn, hold, fadeOut)
    }

    fun startGroundPulse(target: Color, fadeIn: Float, hold: Float, fadeOut: Float) {
        groundPulse.init(target, fadeIn, hold, fadeOut)
    }

    fun update(delta: Float) {
        if (bgFade.active) {
            bgFade.elapsed += delta
            val t = min(bgFade.elapsed / bgFade.duration, 1f)
            baseBgColor.set(
                lerp(bgFade.from.r, bgFade.to.r, t),
                lerp(bgFade.from.g, bgFade.to.g, t),
                lerp(bgFade.from.b, bgFade.to.b, t), 1f
            )
            if (t >= 1f) bgFade.active = false
        }

        if (groundFade.active) {
            groundFade.elapsed += delta
            val t = min(groundFade.elapsed / groundFade.duration, 1f)
            baseGroundColor.set(
                lerp(groundFade.from.r, groundFade.to.r, t),
                lerp(groundFade.from.g, groundFade.to.g, t),
                lerp(groundFade.from.b, groundFade.to.b, t), 1f
            )
            if (t >= 1f) groundFade.active = false
        }

        bgPulse.update(delta)
        groundPulse.update(delta)

        backgroundColor.set(baseBgColor)
        if (bgPulse.active) backgroundColor.lerp(bgPulse.target, bgPulse.getIntensity())

        groundColor.set(baseGroundColor)
        if (groundPulse.active) groundColor.lerp(groundPulse.target, groundPulse.getIntensity())
    }

    fun reset() {
        bgFade.active = false
        groundFade.active = false
        bgPulse.active = false
        groundPulse.active = false
        baseBgColor.set(0.1f, 0.1f, 0.18f, 1f)
        baseGroundColor.set(0.09f, 0.13f, 0.24f, 1f)
        backgroundColor.set(baseBgColor)
        groundColor.set(baseGroundColor)
    }

    fun cancelTransitions() {
        bgFade.active = false
        groundFade.active = false
        bgPulse.active = false
        groundPulse.active = false
    }

    fun setBaseBgColor(c: Color) {
        baseBgColor.set(c)
    }

    fun setBaseGroundColor(c: Color) {
        baseGroundColor.set(c)
    }

    fun setBackgroundColor(c: Color) {
        backgroundColor.set(c)
    }

    fun setGroundColor(c: Color) {
        groundColor.set(c)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }
}
