package io.github.msameer0.rhythmicrush.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/**
 * A textured button with spring-bounce scale animation.
 *
 * <ul>
 *   <li>Press down → scales up to {@link #PRESS_SCALE} with a spring overshoot</li>
 *   <li>Release → scales back to 1.0 with bounce, then fires the action</li>
 *   <li>Touch cancel (drag off) → scales back without firing</li>
 * </ul>
 *
 * Usage each frame:
 * <pre>
 *   button.update(delta);
 *   button.draw(batch);
 *   // input:
 *   if (Gdx.input.justTouched())  button.onTouchDown(tx, ty);
 *   if (!Gdx.input.isTouched())   button.onTouchUp(tx, ty);  // pass last touch pos
 * </pre>
 */
public class AnimatedButton {

    // ── Spring constants ──────────────────────────────────────────────────────
    /** Scale when finger is held down. */
    private static final float PRESS_SCALE    = 1.13f;
    /** Spring stiffness — higher = snappier. */
    private static final float SPRING_K       = 520f;
    /** Spring damping — lower = more bounce. */
    private static final float SPRING_DAMPING = 18f;

    // ── Geometry ──────────────────────────────────────────────────────────────
    public float x, y, w, h;

    // ── State ─────────────────────────────────────────────────────────────────
    private TextureRegion region;
    private float         scale     = 1f;
    private float         velocity  = 0f;  // spring velocity
    private float         target    = 1f;  // scale target (1.0 or PRESS_SCALE)
    private boolean       pressed   = false;
    private boolean       pendingFire = false;

    private Runnable action;

    public AnimatedButton(TextureRegion region, float x, float y, float w, float h, Runnable action) {
        this.region = region;
        this.x      = x;
        this.y      = y;
        this.w      = w;
        this.h      = h;
        this.action = action;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(float delta) {
        // Spring physics: acceleration toward target
        float displacement = scale - target;
        float acceleration = -SPRING_K * displacement - SPRING_DAMPING * velocity;
        velocity += acceleration * delta;
        scale    += velocity * delta;

        // Fire action once spring has mostly settled after release
        if (pendingFire && !pressed && Math.abs(scale - 1f) < 0.02f && Math.abs(velocity) < 0.5f) {
            pendingFire = false;
            scale       = 1f;
            velocity    = 0f;
            if (action != null) action.run();
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    /** Call when a touch-down is detected. Returns true if this button was hit. */
    public boolean onTouchDown(float tx, float ty) {
        if (!hits(tx, ty)) return false;
        pressed  = true;
        target   = PRESS_SCALE;
        velocity = 0f;
        pendingFire = false;
        return true;
    }

    /**
     * Call when the touch is released.
     * @param tx last known touch X
     * @param ty last known touch Y
     */
    public void onTouchUp(float tx, float ty) {
        if (!pressed) return;
        pressed = false;
        target  = 1f;
        // Only fire if release is still over the button (not dragged off)
        if (hits(tx, ty)) pendingFire = true;
    }

    /** Call if you want to cancel without firing (e.g. drag off button). */
    public void cancel() {
        pressed     = false;
        pendingFire = false;
        target      = 1f;
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    public void draw(SpriteBatch batch) {
        if (region == null) return;
        float sw = w * scale;
        float sh = h * scale;
        float sx = x + w / 2f - sw / 2f;  // keep centered
        float sy = y + h / 2f - sh / 2f;
        batch.draw(region, sx, sy, sw, sh);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean hits(float tx, float ty) {
        // Use a slightly larger hit area when pressed for better feel
        float pad = pressed ? w * 0.1f : 0f;
        return tx >= x - pad && tx <= x + w + pad
            && ty >= y - pad && ty <= y + h + pad;
    }

    public boolean isPressed() { return pressed; }

    public void setRegion(TextureRegion r) { region = r; }
    public void setBounds(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }
}
