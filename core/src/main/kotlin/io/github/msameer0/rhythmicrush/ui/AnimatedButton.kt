package io.github.msameer0.rhythmicrush.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.min

/**
 * A UI button that features a spring-based animation when pressed and released.
 */
class AnimatedButton(
    private var region: TextureRegion?,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
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
            val acceleration = -SPRING_K * displacement - SPRING_DAMPING * velocity
            velocity += acceleration * dt
            scale += velocity * dt

            remaining -= dt
        }

        if (pendingFire && !this.isPressed && kotlin.math.abs(scale - 1f) < 0.02f && kotlin.math.abs(
                velocity
            ) < 0.5f
        ) {
            pendingFire = false
            scale = 1f
            velocity = 0f
            action?.run()
        }
    }

    fun onTouchDown(touchX: Float, touchY: Float) {
        if (!hits(touchX, touchY)) return
        this.isPressed = true
        target = PRESS_SCALE
        velocity = 0f
        pendingFire = false
    }

    fun onTouchUp(touchX: Float, touchY: Float) {
        if (!this.isPressed) return
        this.isPressed = false
        target = 1f
        if (hits(touchX, touchY)) pendingFire = true
    }

    fun cancel() {
        this.isPressed = false
        pendingFire = false
        target = 1f
    }

    fun draw(batch: SpriteBatch) {
        val textureRegion = region ?: return
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        val scaledX = x + width / 2f - scaledWidth / 2f
        val scaledY = y + height / 2f - scaledHeight / 2f
        batch.draw(textureRegion, scaledX, scaledY, scaledWidth, scaledHeight)
    }

    fun hits(touchX: Float, touchY: Float): Boolean {
        val padding = if (isPressed) width * 0.1f else 0f
        return touchX >= x - padding &&
            touchX <= x + width + padding &&
            touchY >= y - padding &&
            touchY <= y + height + padding
    }

    fun setRegion(r: TextureRegion?) {
        region = r
    }

    fun setBounds(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    companion object {
        private const val PRESS_SCALE = 1.13f
        private const val SPRING_K = 520f
        private const val SPRING_DAMPING = 18f
    }
}
