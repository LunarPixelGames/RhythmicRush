package io.github.msameer0.rhythmicrush.screens.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.settings.SettingsManager;

/**
 * Draws all in-game HUD elements: progress bar, attempt/best/FPS counters,
 * the pause button, and the "New Best" popup.
 *
 * <p>Owned by {@code GameScreen} and called from its {@code draw()} method.
 * Requires an active SpriteBatch / ShapeRenderer context to be managed by the
 * caller — see individual method docs for which renderer must be open.</p>
 */
public class HudRenderer {

    // ── Popup timing ──────────────────────────────────────────────────────────
    private static final float POPUP_FADE_IN  = 0.25f;
    private static final float POPUP_HOLD     = 1.20f;
    private static final float POPUP_FADE_OUT = 0.45f;
    private static final float POPUP_TOTAL    = POPUP_FADE_IN + POPUP_HOLD + POPUP_FADE_OUT;

    private static final float PAUSE_BTN = 44f;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color COL_FILL    = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_HEADING = new Color(1f, 0.85f, 0.35f, 1f);
    private static final Color HUD_ATTEMPT = new Color(1f, 1f, 1f, 0.85f);
    private static final Color HUD_BEST    = new Color(1f, 1f, 1f, 0.55f);
    private static final Color HUD_FPS     = new Color(1f, 1f, 1f, 0.45f);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final RhythmicRushGame game;
    private final GameWorld world;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;

    // ── State (owned by GameScreen but read here) ─────────────────────────────
    private final StringBuilder sb = new StringBuilder(32);

    private float popupTimer  = -1f;
    private int   popupBestPct = 0;

    public HudRenderer(RhythmicRushGame game, GameWorld world,
                       BitmapFont font, ShapeRenderer shapes, SpriteBatch batch) {
        this.game       = game;
        this.world      = world;
        this.font       = font;
        this.glyphLayout = new GlyphLayout();
        this.shapes     = shapes;
        this.batch      = batch;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Advances the "New Best" popup timer. Call once per frame even when paused.
     *
     * @param delta Seconds since last frame.
     */
    public void update(float delta) {
        if (popupTimer >= 0f) {
            popupTimer += delta;
            if (popupTimer >= POPUP_TOTAL) popupTimer = -1f;
        }
    }

    /** Triggers the "New Best" popup animation. */
    public void showNewBestPopup(int bestPct) {
        popupTimer    = 0f;
        popupBestPct  = bestPct;
    }

    // ── Shape draws (call inside a ShapeRenderer.Filled block) ───────────────

    /**
     * Draws the progress bar background and fill shapes.
     * Must be called inside an open {@code shapes.begin(Filled)} block.
     */
    public void drawProgressBarShapes(OrthographicCamera camera, Viewport viewport) {
        float progress = world.getProgress();
        if (progress <= 0f) return;
        SettingsManager s = game.getSettingsManager();
        if (!s.getShowProgressBar()) return;

        final float BAR_W = viewport.getWorldWidth() * 0.625f * 0.55f;
        final float BAR_H = 10f;
        final float GAP   = 14f;
        final float LINE_Y = camTop(camera, viewport) - (s.getUiPadding() + 6f);

        float textW = 0f;
        if (s.getShowPercentage()) {
            sb.setLength(0);
            sb.append(Math.round(progress * 100f)).append('%');
            font.getData().setScale(1.2f);
            glyphLayout.setText(font, sb, Color.WHITE, 0, Align.left, false);
            textW = glyphLayout.width;
        }

        float totalW  = (s.getShowPercentage() ? textW + GAP : 0f) + BAR_W;
        float startX  = camera.position.x - totalW / 2f;
        float r       = BAR_H / 2f;
        float fillW   = BAR_W * progress;

        shapes.setColor(0.2f, 0.2f, 0.2f, 0.55f);
        drawRoundedRect(startX, LINE_Y - BAR_H / 2f, BAR_W, BAR_H, r);

        if (fillW >= BAR_H) {
            shapes.setColor(COL_FILL);
            drawRoundedRect(startX, LINE_Y - BAR_H / 2f, fillW, BAR_H, r);
        } else if (fillW > 0) {
            shapes.setColor(COL_FILL);
            shapes.rect(startX, LINE_Y - BAR_H / 2f, fillW, BAR_H);
        }
    }

    /**
     * Draws the pause button circle and bar icons.
     * Must be called inside an open {@code shapes.begin(Filled)} block.
     */
    public void drawPauseButtonShapes(OrthographicCamera camera, Viewport viewport) {
        float cx = pauseCircleCX(camera, viewport);
        float cy = pauseCircleCY(camera, viewport);
        float r  = PAUSE_BTN / 2f;

        shapes.setColor(0.2f, 0.2f, 0.2f, 0.75f);
        shapes.circle(cx, cy, r, 32);

        float barW = r * 0.22f, barH = r * 0.75f, gap = r * 0.18f;
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.rect(cx - gap - barW, cy - barH / 2f, barW, barH);
        shapes.rect(cx + gap,        cy - barH / 2f, barW, barH);
    }

    // ── Text draws (call inside a SpriteBatch.begin block) ────────────────────

    /**
     * Draws the percentage label beside the progress bar.
     * Must be called inside an open {@code batch.begin()} block.
     */
    public void drawProgressBarText(OrthographicCamera camera, Viewport viewport, String levelKey) {
        float progress = world.getProgress();
        if (progress <= 0f) return;
        SettingsManager s = game.getSettingsManager();
        if (!s.getShowPercentage()) return;

        int pct = Math.round(progress * 100f);
        final float BAR_W  = viewport.getWorldWidth() * 0.625f * 0.55f;
        final float GAP    = 14f;
        final float LINE_Y = camTop(camera, viewport) - (s.getUiPadding() + 6f);

        sb.setLength(0);
        sb.append(pct).append('%');
        font.getData().setScale(1.2f);
        glyphLayout.setText(font, sb, Color.WHITE, 0, Align.left, false);
        float textW = glyphLayout.width;
        float textH = glyphLayout.height;

        float totalW    = textW + (s.getShowProgressBar() ? GAP + BAR_W : 0f);
        float startX    = camera.position.x - totalW / 2f;
        float textDrawX = s.getShowProgressBar() ? startX + BAR_W + GAP : startX;

        boolean isPB = false;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            isPB = pct > p.getBestPercent();
        }
        Color textColor = isPB ? COL_HEADING : Color.WHITE;

        font.setColor(0, 0, 0, textColor.a * 0.4f);
        font.draw(batch, sb, textDrawX + 2f, LINE_Y + textH / 2f - 2f);
        font.setColor(textColor);
        font.draw(batch, sb, textDrawX, LINE_Y + textH / 2f);
        font.getData().setScale(1f);
    }

    /**
     * Draws attempts, best %, and optional FPS in the top-left corner.
     * Must be called inside an open {@code batch.begin()} block.
     */
    public void drawSessionAttemptsText(OrthographicCamera camera, Viewport viewport,
                                        int sessionAttempts, String levelKey) {
        SettingsManager s = game.getSettingsManager();
        float p    = s.getUiPadding();
        float left = camLeft(camera, viewport) + p;
        float top  = camTop(camera, viewport) - p;
        float shadowOffset = 2f;
        float nextY = top;

        if (s.getShowAttempts()) {
            sb.setLength(0);
            sb.append("Attempt  ").append(sessionAttempts);
            font.setColor(0, 0, 0, HUD_ATTEMPT.a * 0.4f);
            font.draw(batch, sb, left + shadowOffset, nextY - shadowOffset);
            font.setColor(HUD_ATTEMPT);
            font.draw(batch, sb, left, nextY);
            nextY -= 26f;
        }

        if (s.getShowBest() && levelKey != null) {
            LevelProgress lp = game.getProgressManager().getOrCreate(levelKey);
            sb.setLength(0);
            sb.append("Best  ").append(lp.getBestPercent()).append('%');
            font.setColor(0, 0, 0, HUD_BEST.a * 0.4f);
            font.draw(batch, sb, left + shadowOffset, nextY - shadowOffset);
            font.setColor(HUD_BEST);
            font.draw(batch, sb, left, nextY);
            nextY -= 26f;
        }

        if (s.getShowFps()) {
            sb.setLength(0);
            sb.append("FPS  ").append(Gdx.graphics.getFramesPerSecond());
            font.setColor(0, 0, 0, HUD_FPS.a * 0.4f);
            font.draw(batch, sb, left + shadowOffset, nextY - shadowOffset);
            font.setColor(HUD_FPS);
            font.draw(batch, sb, left, nextY);
        }
    }

    /**
     * Draws the animated "New Best" popup.
     * Must be called inside an open {@code batch.begin()} block.
     */
    public void drawNewBestPopup(OrthographicCamera camera) {
        if (popupTimer < 0f) return;

        float alpha, scale;
        if (popupTimer < POPUP_FADE_IN) {
            float t = popupTimer / POPUP_FADE_IN;
            alpha = t;
            scale = 1.0f + 0.8f * t;
        } else if (popupTimer < POPUP_FADE_IN + POPUP_HOLD) {
            float t = (popupTimer - POPUP_FADE_IN) / POPUP_HOLD;
            alpha = 1f;
            scale = 1.8f - 0.4f * t;
        } else {
            float t = (popupTimer - POPUP_FADE_IN - POPUP_HOLD) / POPUP_FADE_OUT;
            alpha = 1f - t;
            scale = 1.4f * (1f - t);
        }
        alpha = Math.max(0f, Math.min(1f, alpha));
        scale = Math.max(0f, scale);

        float cx = camera.position.x;
        float cy = camera.position.y;

        font.getData().setScale(scale);
        sb.setLength(0);
        sb.append("NEW BEST");
        glyphLayout.setText(font, sb);
        float textH = glyphLayout.height;
        float textY = cy + textH / 2f;

        font.setColor(0, 0, 0, alpha * 0.4f);
        font.draw(batch, sb, cx - glyphLayout.width / 2f + 2f, textY - 2f);
        font.setColor(COL_HEADING.r, COL_HEADING.g, COL_HEADING.b, alpha);
        font.draw(batch, sb, cx - glyphLayout.width / 2f, textY);

        font.getData().setScale(scale * 0.6f);
        sb.setLength(0);
        sb.append(popupBestPct).append('%');
        glyphLayout.setText(font, sb);
        float pctY = textY - textH - 5f;

        font.setColor(0, 0, 0, alpha * 0.85f * 0.4f);
        font.draw(batch, sb, cx - glyphLayout.width / 2f + 2f, pctY - 2f);
        font.setColor(1f, 1f, 1f, alpha * 0.85f);
        font.draw(batch, sb, cx - glyphLayout.width / 2f, pctY);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    // ── Hit testing ───────────────────────────────────────────────────────────

    /**
     * Returns whether a world-space coordinate lands on the pause button.
     *
     * @param tx World X of the touch.
     * @param ty World Y of the touch.
     */
    public boolean hitsPauseButton(float tx, float ty, OrthographicCamera camera, Viewport viewport) {
        float cx = pauseCircleCX(camera, viewport);
        float cy = pauseCircleCY(camera, viewport);
        float r  = PAUSE_BTN / 2f + 8f;
        float dx = tx - cx, dy = ty - cy;
        return dx * dx + dy * dy <= r * r;
    }

    // ── Camera convenience ────────────────────────────────────────────────────

    private static float camTop(OrthographicCamera camera, Viewport viewport) {
        return camera.position.y + viewport.getWorldHeight() / 2f;
    }

    private static float camLeft(OrthographicCamera camera, Viewport viewport) {
        return camera.position.x - viewport.getWorldWidth() / 2f;
    }

    private float pauseCircleCX(OrthographicCamera camera, Viewport viewport) {
        return camera.position.x + viewport.getWorldWidth() / 2f
            - PAUSE_BTN / 2f - (game.getSettingsManager().getUiPadding() + 2f);
    }

    private float pauseCircleCY(OrthographicCamera camera, Viewport viewport) {
        return camera.position.y + viewport.getWorldHeight() / 2f
            - PAUSE_BTN / 2f - (game.getSettingsManager().getUiPadding() + 2f);
    }

    // ── Shape helper ──────────────────────────────────────────────────────────

    private void drawRoundedRect(float x, float y, float w, float h, float r) {
        shapes.rect(x + r, y,     w - 2 * r, h);
        shapes.rect(x,     y + r, r,          h - 2 * r);
        shapes.rect(x + w - r, y + r, r,      h - 2 * r);
        shapes.circle(x + r,     y + r,     r, 16);
        shapes.circle(x + w - r, y + r,     r, 16);
        shapes.circle(x + r,     y + h - r, r, 16);
        shapes.circle(x + w - r, y + h - r, r, 16);
    }
}
