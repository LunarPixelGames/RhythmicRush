package io.github.msameer0.rhythmicrush.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * A UI button component that features a spring-based scale animation when interacted with.
 * The button expands when pressed and returns to its original size with a bouncing effect
 * before triggering its assigned action.
 */
public class AnimatedButton {

    private static final float PRESS_SCALE = 1.13f;
    private static final float SPRING_K = 520f;
    private static final float SPRING_DAMPING = 18f;

    public float x, y, w, h;

    private TextureRegion region;
    private float scale = 1f;
    private float velocity = 0f;
    private float target = 1f;
    private boolean pressed = false;
    private boolean pendingFire = false;

    private final Runnable action;

    /**
     * Constructs a new AnimatedButton with a specified texture, position, dimensions, and click action.
     *
     * @param region the texture region to be displayed for the button
     * @param x      the x-coordinate of the button
     * @param y      the y-coordinate of the button
     * @param w      the width of the button
     * @param h      the height of the button
     * @param action the Runnable to execute when the button is pressed and released
     */
    public AnimatedButton(TextureRegion region, float x, float y, float w, float h, Runnable action) {
        this.region = region;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.action = action;
    }


    /**
     * Updates the button's animation state using a spring-damping system and triggers the
     * assigned action if a click is pending.
     *
     * <p>This method calculates the spring physics for the scale transition and checks
     * if the button has sufficiently returned to its resting state after a touch
     * release before executing the {@link Runnable} action.</p>
     *
     * @param delta the time elapsed since the last update in seconds
     */
    public void update(float delta) {
        float remaining = Math.min(delta, 0.25f);
        float step = 0.01f;

        while (remaining > 0) {
            float dt = Math.min(remaining, step);

            float displacement = scale - target;
            float acceleration = -SPRING_K * displacement - SPRING_DAMPING * velocity;
            velocity += acceleration * dt;
            scale += velocity * dt;

            remaining -= dt;
        }

        if (pendingFire && !pressed && Math.abs(scale - 1f) < 0.02f && Math.abs(velocity) < 0.5f) {
            pendingFire = false;
            scale = 1f;
            velocity = 0f;
            if (action != null) action.run();
        }
    }


    /**
     * Handles a touch-down event. If the touch coordinates are within the button's bounds,
     * it initiates the press animation and sets the button's state to pressed.
     *
     * @param tx the x-coordinate of the touch event
     * @param ty the y-coordinate of the touch event
     */
    public void onTouchDown(float tx, float ty) {
        if (!hits(tx, ty)) return;
        pressed = true;
        target = PRESS_SCALE;
        velocity = 0f;
        pendingFire = false;
    }

    /**
     * Handles a touch-up event. Resets the button's pressed state
     */
    public void onTouchUp(float tx, float ty) {
        if (!pressed) return;
        pressed = false;
        target = 1f;
        if (hits(tx, ty)) pendingFire = true;
    }

    /**
     * Cancels the current button interaction, resetting its pressed state and
     * preventing any pending actions from being triggered. The button will
     * animate back to its original scale.
     */
    public void cancel() {
        pressed = false;
        pendingFire = false;
        target = 1f;
    }


    /**
     * Draws the button to the screen using the provided {@link SpriteBatch}.
     * The button is rendered centered relative to its original position,
     * accounting for the current animation scale.
     *
     * @param batch the SpriteBatch used for rendering
     */
    public void draw(SpriteBatch batch) {
        if (region == null) return;
        float sw = w * scale;
        float sh = h * scale;
        float sx = x + w / 2f - sw / 2f;
        float sy = y + h / 2f - sh / 2f;
        batch.draw(region, sx, sy, sw, sh);
    }


    /**
     * Checks if the specified coordinates are within the button's bounds.
     *
     * <p>If the button is currently pressed, the hit area is expanded by a 10% padding
     */
    public boolean hits(float tx, float ty) {
        float pad = pressed ? w * 0.1f : 0f;
        return tx >= x - pad && tx <= x + w + pad
            && ty >= y - pad && ty <= y + h + pad;
    }

    /**
     * Checks whether the button is currently in a pressed state.
     *
     * @return true if the button is currently being pressed, false otherwise
     */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Sets the texture region used to render the button.
     *
     * @param r the new {@link TextureRegion} to be displayed
     */
    public void setRegion(TextureRegion r) {
        region = r;
    }

    /**
     * Sets the position and dimensions of the button.
     *
     * @param x the new x-coordinate of the button
     * @param y the new y-coordinate of the button
     * @param w the new width of the button
     * @param h the new height of the button
     */
    public void setBounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
