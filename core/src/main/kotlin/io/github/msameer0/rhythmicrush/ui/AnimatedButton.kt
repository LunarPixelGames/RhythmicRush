package io.github.msameer0.rhythmicrush.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.abs
import kotlin.math.min

/**
 * A UI button component that features a spring-based scale animation when interacted with.
 * The button expands when pressed and returns to its original size with a bouncing effect
 * before triggering its assigned action.
 */
class AnimatedButton
/**
 * Constructs a new AnimatedButton with a specified texture, position, dimensions, and click action.
 *
 * @param region the texture region to be displayed for the button
 * @param x      the x-coordinate of the button
 * @param y      the y-coordinate of the button
 * @param w      the width of the button
 * @param h      the height of the button
 * @param action the Runnable to execute when the button is pressed and released
 */(
    private var region: TextureRegion?,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    private val action: Runnable?
) {
    /**
     * @return the current animation scale of the button.
     */
    var scale: Float = 1f
        private set
    private var velocity = 0f
    private var target = 1f

    /**
     * Checks whether the button is currently in a pressed state.
     *
     * @return true if the button is currently being pressed, false otherwise
     */
    var isPressed: Boolean = false
        private set
    private var pendingFire = false


    /**
     * Updates the button's animation state using a spring-damping system and triggers the
     * assigned action if a click is pending.
     *
     *
     * This method calculates the spring physics for the scale transition and checks
     * if the button has sufficiently returned to its resting state after a touch
     * release before executing the [Runnable] action.
     *
     * @param delta the time elapsed since the last update in seconds
     */
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


    /**
     * Handles a touch-down event. If the touch coordinates are within the button's bounds,
     * it initiates the press animation and sets the button's state to pressed.
     *
     * @param tx the x-coordinate of the touch event
     * @param ty the y-coordinate of the touch event
     */
    fun onTouchDown(tx: Float, ty: Float) {
        if (!hits(tx, ty)) return
        this.isPressed = true
        target = PRESS_SCALE
        velocity = 0f
        pendingFire = false
    }

    /**
     * Handles a touch-up event. Resets the button's pressed state
     */
    fun onTouchUp(tx: Float, ty: Float) {
        if (!this.isPressed) return
        this.isPressed = false
        target = 1f
        if (hits(tx, ty)) pendingFire = true
    }

    /**
     * Cancels the current button interaction, resetting its pressed state and
     * preventing any pending actions from being triggered. The button will
     * animate back to its original scale.
     */
    fun cancel() {
        this.isPressed = false
        pendingFire = false
        target = 1f
    }


    /**
     * Draws the button to the screen using the provided [SpriteBatch].
     * The button is rendered centered relative to its original position,
     * accounting for the current animation scale.
     *
     * @param batch the SpriteBatch used for rendering
     */
    fun draw(batch: SpriteBatch) {
        if (region == null) return
        val sw = w * scale
        val sh = h * scale
        val sx = x + w / 2f - sw / 2f
        val sy = y + h / 2f - sh / 2f
        batch.draw(region, sx, sy, sw, sh)
    }


    /**
     * Checks if the specified coordinates are within the button's bounds.
     *
     *
     * If the button is currently pressed, the hit area is expanded by a 10% padding
     */
    fun hits(tx: Float, ty: Float): Boolean {
        val pad = if (this.isPressed) w * 0.1f else 0f
        return tx >= x - pad && tx <= x + w + pad && ty >= y - pad && ty <= y + h + pad
    }

    /**
     * Sets the texture region used to render the button.
     *
     * @param r the new [TextureRegion] to be displayed
     */
    fun setRegion(r: TextureRegion?) {
        region = r
    }

    /**
     * Sets the position and dimensions of the button.
     *
     * @param x the new x-coordinate of the button
     * @param y the new y-coordinate of the button
     * @param w the new width of the button
     * @param h the new height of the button
     */
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
