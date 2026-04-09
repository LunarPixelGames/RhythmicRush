package io.github.msameer0.rhythmicrush.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.abs
import kotlin.math.min

class AnimatedButton
(
    private var region: TextureRegion?,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    private val action: Runnable?
) {

    var scale: Float = 1f
        private set
    private var velocity = 0f
    private var target = 1f

    var isPressed: Boolean = false
        private set
    private var pendingFire = false

    fun update(delta: Float) {
        var remaining = min(delta, 0.25f)
        val step = 0.01f

        while (remaining > 0) {
            val dt = min(remaining, step)

            val displacement = scale - target
            val acceleration: Float = -SPRING_K * displacement - SPRING_DAMPING * velocity
            velocity += acceleration * dt
            scale += velocity * dt

            remaining -= dt
        }

        if (pendingFire && !this.isPressed && abs(scale - 1f) < 0.02f && abs(velocity) < 0.5f) {
            pendingFire = false
            scale = 1f
            velocity = 0f
            if (action != null) action.run()
        }
    }

    fun onTouchDown(tx: Float, ty: Float) {
        if (!hits(tx, ty)) return
        this.isPressed = true
        target = PRESS_SCALE
        velocity = 0f
        pendingFire = false
    }

    fun onTouchUp(tx: Float, ty: Float) {
        if (!this.isPressed) return
        this.isPressed = false
        target = 1f
        if (hits(tx, ty)) pendingFire = true
    }

    fun cancel() {
        this.isPressed = false
        pendingFire = false
        target = 1f
    }

    fun draw(batch: SpriteBatch) {
        if (region == null) return
        val sw = w * scale
        val sh = h * scale
        val sx = x + w / 2f - sw / 2f
        val sy = y + h / 2f - sh / 2f
        batch.draw(region, sx, sy, sw, sh)
    }

    fun hits(tx: Float, ty: Float): Boolean {
        val pad = if (this.isPressed) w * 0.1f else 0f
        return tx >= x - pad && tx <= x + w + pad && ty >= y - pad && ty <= y + h + pad
    }

    fun setRegion(r: TextureRegion?) {
        region = r
    }

    fun setBounds(x: Float, y: Float, w: Float, h: Float) {
        this.x = x
        this.y = y
        this.w = w
        this.h = h
    }

    companion object {
        private const val PRESS_SCALE = 1.13f
        private const val SPRING_K = 520f
        private const val SPRING_DAMPING = 18f
    }
}
