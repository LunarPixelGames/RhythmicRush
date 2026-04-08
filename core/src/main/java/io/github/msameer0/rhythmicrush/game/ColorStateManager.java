package io.github.msameer0.rhythmicrush.game;

import com.badlogic.gdx.graphics.Color;

/**
 * Manages all color transition state for the game world: background and ground
 * color fades and pulses driven by level triggers.
 *
 * <p>GameWorld owns one instance and delegates all color-animation work here.
 * When TarsosDSP audio analysis is added later, the pulse API here is the
 * natural hook point — call {@link #startBgPulse} / {@link #startGroundPulse}
 * in response to beat callbacks instead of (or in addition to) trigger events.</p>
 */
public class ColorStateManager {

    // ── Inner state types ─────────────────────────────────────────────────────

    private static class ColorFade {
        final Color from = new Color();
        final Color to   = new Color();
        float duration, elapsed;
        boolean active = false;

        void init(Color from, Color to, float duration) {
            this.from.set(from);
            this.to.set(to);
            this.duration = duration;
            this.elapsed  = 0f;
            this.active   = true;
        }
    }

    private static class ColorPulse {
        final Color target = new Color();
        float fadeIn, hold, fadeOut, elapsed;
        boolean active = false;

        void init(Color target, float fadeIn, float hold, float fadeOut) {
            this.target.set(target);
            this.fadeIn  = fadeIn;
            this.hold    = hold;
            this.fadeOut = fadeOut;
            this.elapsed = 0f;
            this.active  = true;
        }

        float getIntensity() {
            if (!active) return 0f;
            if (elapsed < fadeIn)                    return fadeIn  > 0 ? elapsed / fadeIn : 1f;
            if (elapsed < fadeIn + hold)             return 1f;
            if (elapsed < fadeIn + hold + fadeOut)   return fadeOut > 0 ? 1f - (elapsed - fadeIn - hold) / fadeOut : 0f;
            return 0f;
        }

        void update(float delta) {
            if (!active) return;
            elapsed += delta;
            if (elapsed >= fadeIn + hold + fadeOut) active = false;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final ColorFade  bgFade     = new ColorFade();
    private final ColorFade  groundFade = new ColorFade();
    private final ColorPulse bgPulse    = new ColorPulse();
    private final ColorPulse groundPulse = new ColorPulse();

    /** The "resting" colors that fades animate toward and pulses blend from. */
    private final Color baseBgColor     = new Color(0.1f,  0.1f,  0.18f, 1f);
    private final Color baseGroundColor = new Color(0.09f, 0.13f, 0.24f, 1f);

    /** The live colors actually used for rendering each frame. */
    private final Color backgroundColor = new Color(baseBgColor);
    private final Color groundColor     = new Color(baseGroundColor);

    // ── Public API ────────────────────────────────────────────────────────────

    public void startBgFade(Color target, float duration) {
        bgFade.init(baseBgColor, target, duration);
    }

    public void startGroundFade(Color target, float duration) {
        groundFade.init(baseGroundColor, target, duration);
    }

    public void startBgPulse(Color target, float fadeIn, float hold, float fadeOut) {
        bgPulse.init(target, fadeIn, hold, fadeOut);
    }

    public void startGroundPulse(Color target, float fadeIn, float hold, float fadeOut) {
        groundPulse.init(target, fadeIn, hold, fadeOut);
    }

    /**
     * Advances all active transitions. Call once per rendered frame (not per
     * physics tick).
     *
     * @param delta Seconds since the last frame.
     */
    public void update(float delta) {
        if (bgFade.active) {
            bgFade.elapsed += delta;
            float t = Math.min(bgFade.elapsed / bgFade.duration, 1f);
            baseBgColor.set(
                lerp(bgFade.from.r, bgFade.to.r, t),
                lerp(bgFade.from.g, bgFade.to.g, t),
                lerp(bgFade.from.b, bgFade.to.b, t), 1f);
            if (t >= 1f) bgFade.active = false;
        }

        if (groundFade.active) {
            groundFade.elapsed += delta;
            float t = Math.min(groundFade.elapsed / groundFade.duration, 1f);
            baseGroundColor.set(
                lerp(groundFade.from.r, groundFade.to.r, t),
                lerp(groundFade.from.g, groundFade.to.g, t),
                lerp(groundFade.from.b, groundFade.to.b, t), 1f);
            if (t >= 1f) groundFade.active = false;
        }

        bgPulse.update(delta);
        groundPulse.update(delta);

        backgroundColor.set(baseBgColor);
        if (bgPulse.active) backgroundColor.lerp(bgPulse.target, bgPulse.getIntensity());

        groundColor.set(baseGroundColor);
        if (groundPulse.active) groundColor.lerp(groundPulse.target, groundPulse.getIntensity());
    }

    /** Resets all transitions and restores default colors. */
    public void reset() {
        bgFade.active     = false;
        groundFade.active = false;
        bgPulse.active    = false;
        groundPulse.active = false;
        baseBgColor.set(0.1f, 0.1f, 0.18f, 1f);
        baseGroundColor.set(0.09f, 0.13f, 0.24f, 1f);
        backgroundColor.set(baseBgColor);
        groundColor.set(baseGroundColor);
    }

    /** Resets all active transitions without changing the base colors. */
    public void cancelTransitions() {
        bgFade.active      = false;
        groundFade.active  = false;
        bgPulse.active     = false;
        groundPulse.active = false;
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public Color getBaseBgColor()     { return baseBgColor; }
    public Color getBaseGroundColor() { return baseGroundColor; }
    public Color getBackgroundColor() { return backgroundColor; }
    public Color getGroundColor()     { return groundColor; }

    public void setBaseBgColor(Color c)     { baseBgColor.set(c); }
    public void setBaseGroundColor(Color c) { baseGroundColor.set(c); }
    public void setBackgroundColor(Color c) { backgroundColor.set(c); }
    public void setGroundColor(Color c)     { groundColor.set(c); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
