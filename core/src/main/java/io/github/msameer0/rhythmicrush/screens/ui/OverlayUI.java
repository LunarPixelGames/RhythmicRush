package io.github.msameer0.rhythmicrush.screens.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;

/**
 * Draws the pause overlay, level-complete overlay, and volume slider UI.
 *
 * <p>Extracted from {@code GameScreen} to separate overlay presentation logic
 * from core gameplay coordination. GameScreen passes its current camera/viewport
 * and UI-scaling values into each call rather than this class tracking them
 * internally, keeping state minimal.</p>
 */
public class OverlayUI {

    // ── Layout defaults (overridden per platform via updateScale) ─────────────
    private float panelW = 520f;
    private float panelH = 360f;
    private float btnSize = 72f;
    private float uiScale = 1.0f;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color COL_OVERLAY = new Color(0f, 0f, 0f, 0.65f);
    private static final Color COL_PANEL   = new Color(0.11f, 0.11f, 0.17f, 1f);
    private static final Color COL_HEADING = new Color(1f, 0.85f, 0.35f, 1f);
    private static final Color COL_LABEL   = new Color(1f, 1f, 1f, 0.85f);
    private static final Color COL_DIM     = new Color(1f, 1f, 1f, 0.50f);
    private static final Color COL_TRACK   = new Color(0.28f, 0.28f, 0.35f, 1f);
    private static final Color COL_FILL    = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_THUMB   = new Color(1f, 1f, 1f, 1f);

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final RhythmicRushGame game;
    private final LevelData levelData;
    private final BitmapFont pauseFont;
    private final GlyphLayout layout;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final TextureRegion resumeRegion;
    private final TextureRegion backRegion;

    // ── Cached panel texture ──────────────────────────────────────────────────
    private Texture panelTexture;
    private int lastPanelW = -1, lastPanelH = -1;

    // ── Slider drag state ─────────────────────────────────────────────────────
    private boolean sliderDragging = false;

    public OverlayUI(RhythmicRushGame game, LevelData levelData,
                     BitmapFont pauseFont, ShapeRenderer shapes, SpriteBatch batch,
                     TextureRegion resumeRegion, TextureRegion backRegion) {
        this.game         = game;
        this.levelData    = levelData;
        this.pauseFont    = pauseFont;
        this.layout       = new GlyphLayout();
        this.shapes       = shapes;
        this.batch        = batch;
        this.resumeRegion = resumeRegion;
        this.backRegion   = backRegion;
    }

    // ── Scaling ───────────────────────────────────────────────────────────────

    /**
     * Call on show() and resize() to apply platform-appropriate sizes.
     */
    public void updateScale() {
        boolean mobile = Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android ||
            Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS;
        if (mobile) { panelW = 740f; panelH = 480f; btnSize = 100f; uiScale = 1.4f; }
        else         { panelW = 520f; panelH = 360f; btnSize = 72f;  uiScale = 1.0f; }
        lastPanelW = -1; // invalidate cached texture
    }

    // ── Full-screen dim (call inside a ShapeRenderer.Filled block) ────────────

    public void drawDimOverlay(OrthographicCamera camera, Viewport viewport) {
        shapes.setColor(COL_OVERLAY);
        shapes.rect(camLeft(camera, viewport), camBot(camera, viewport),
            viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    // ── Pause overlay ─────────────────────────────────────────────────────────

    /**
     * Draws the pause panel with level info and slider label.
     * Must be called inside an open {@code batch.begin()} block.
     */
    public void drawPauseOverlay(OrthographicCamera camera, int sessionAttempts, String levelKey) {
        ensurePanel();
        float px = panelX(camera), py = panelY(camera);
        float shadow = 2f * uiScale;

        batch.draw(panelTexture, px, py);

        // Level name
        String name = (levelData != null && levelData.getName() != null) ? levelData.getName() : "Level";
        pauseFont.getData().setScale(1.1f * uiScale);
        layout.setText(pauseFont, name);
        float x = px + panelW / 2f - layout.width / 2f;
        float y = py + panelH - 18f * uiScale;
        drawShadowText(name, x, y, COL_HEADING, shadow);

        float sy = y - layout.height - 22f * uiScale;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);

            pauseFont.getData().setScale(0.68f * uiScale);
            String best = "Personal Best: " + p.getBestPercent() + "%";
            layout.setText(pauseFont, best);
            x = px + panelW / 2f - layout.width / 2f;
            drawShadowText(best, x, sy, COL_LABEL, shadow);

            sy -= layout.height + 14f * uiScale;
            String att = "Total: " + p.getTotalAttempts() + "   Session: " + sessionAttempts;
            layout.setText(pauseFont, att);
            x = px + panelW / 2f - layout.width / 2f;
            drawShadowText(att, x, sy, COL_DIM, shadow);
        }

        // Buttons
        if (backRegion   != null) batch.draw(backRegion,   backX(camera),   backY(camera),   btnSize * 0.9f, btnSize * 0.9f);
        if (resumeRegion != null) batch.draw(resumeRegion, resumeX(camera), backY(camera), btnSize * 0.9f, btnSize * 0.9f);

        // Volume label
        float sliderY = sliderY(camera);
        pauseFont.getData().setScale(0.58f * uiScale);
        layout.setText(pauseFont, "Volume");
        x = px + panelW / 2f - layout.width / 2f;
        y = sliderY + layout.height + 12f * uiScale;
        drawShadowText("Volume", x, y, COL_LABEL, shadow);

        // Volume percentage
        float vol = game.getSettingsManager().getMusicVolume();
        pauseFont.getData().setScale(0.48f * uiScale);
        String volPct = Math.round(vol * 100f) + "%";
        layout.setText(pauseFont, volPct);
        x = sliderTrackX(camera) - layout.width - 12f * uiScale;
        y = sliderY + layout.height / 2f;
        drawShadowText(volPct, x, y, COL_DIM, shadow);

        // Button labels
        pauseFont.getData().setScale(0.5f * uiScale);
        float labelY = backY(camera) - 6f * uiScale;
        layout.setText(pauseFont, "Back");
        x = backX(camera) + btnSize * 0.9f / 2f - layout.width / 2f;
        drawShadowText("Back", x, labelY, COL_DIM, shadow);

        layout.setText(pauseFont, "Resume");
        x = resumeX(camera) + btnSize * 0.9f / 2f - layout.width / 2f;
        drawShadowText("Resume", x, labelY, COL_DIM, shadow);

        pauseFont.getData().setScale(1f);
    }

    /**
     * Draws the volume slider track, fill, and thumb.
     * Manages its own GL blend and ShapeRenderer calls — call outside batch.
     */
    public void drawPauseSlider(OrthographicCamera camera) {
        float sliderY = sliderY(camera);
        float vol     = game.getSettingsManager().getMusicVolume();
        float tx = sliderTrackX(camera), tw = sliderTrackW();
        float trackH = 5f * uiScale, thumbR = 10f * uiScale;
        float fillW  = tw * vol;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_TRACK);
        shapes.rect(tx, sliderY - trackH / 2f, tw, trackH);
        shapes.setColor(COL_FILL);
        if (fillW > 0) shapes.rect(tx, sliderY - trackH / 2f, fillW, trackH);
        shapes.setColor(COL_THUMB);
        shapes.circle(tx + fillW, sliderY, thumbR, 24);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Complete overlay ──────────────────────────────────────────────────────

    /**
     * Draws the level-complete panel.
     * Must be called inside an open {@code batch.begin()} block.
     */
    public void drawCompleteOverlay(OrthographicCamera camera, int sessionAttempts, String levelKey) {
        ensurePanel();
        float px = panelX(camera), py = panelY(camera);
        float shadow = 2f * uiScale;

        batch.draw(panelTexture, px, py);

        pauseFont.getData().setScale(1.25f * uiScale);
        layout.setText(pauseFont, "LEVEL COMPLETE");
        float x = px + panelW / 2f - layout.width / 2f;
        float y = py + panelH - 22f * uiScale;
        drawShadowText("LEVEL COMPLETE", x, y, COL_HEADING, shadow);

        float sy = y - layout.height - 35f * uiScale;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);

            pauseFont.getData().setScale(0.85f * uiScale);
            String total = "Total Attempts: " + p.getTotalAttempts();
            layout.setText(pauseFont, total);
            x = px + panelW / 2f - layout.width / 2f;
            drawShadowText(total, x, sy, COL_LABEL, shadow);

            sy -= layout.height + 20f * uiScale;
            String session = "Session Attempts: " + sessionAttempts;
            layout.setText(pauseFont, session);
            x = px + panelW / 2f - layout.width / 2f;
            drawShadowText(session, x, sy, COL_DIM, shadow);
        }

        if (backRegion   != null) batch.draw(backRegion,   backX(camera),   backY(camera),   btnSize * 0.9f, btnSize * 0.9f);
        if (resumeRegion != null) batch.draw(resumeRegion, resumeX(camera), backY(camera), btnSize * 0.9f, btnSize * 0.9f);

        pauseFont.getData().setScale(0.52f * uiScale);
        float labelY = backY(camera) - 6f * uiScale;
        layout.setText(pauseFont, "Menu");
        x = backX(camera) + btnSize * 0.9f / 2f - layout.width / 2f;
        drawShadowText("Menu", x, labelY, COL_DIM, shadow);

        layout.setText(pauseFont, "Replay");
        x = resumeX(camera) + btnSize * 0.9f / 2f - layout.width / 2f;
        drawShadowText("Replay", x, labelY, COL_DIM, shadow);

        pauseFont.getData().setScale(1f);
    }

    // ── Hit testing ───────────────────────────────────────────────────────────

    public boolean hitsBackButton(float tx, float ty, OrthographicCamera camera) {
        return hits(tx, ty, backX(camera), backY(camera), btnSize * 0.9f, btnSize);
    }

    public boolean hitsResumeButton(float tx, float ty, OrthographicCamera camera) {
        return hits(tx, ty, resumeX(camera), backY(camera), btnSize * 0.9f, btnSize);
    }

    public boolean hitsSlider(Vector2 t, OrthographicCamera camera) {
        float tx = sliderTrackX(camera), tw = sliderTrackW(), ty = sliderY(camera);
        return t.x >= tx - 16f && t.x <= tx + tw + 16f && t.y >= ty - 16f && t.y <= ty + 16f;
    }

    // ── Slider interaction ────────────────────────────────────────────────────

    public void beginSliderDrag()  { sliderDragging = true; }
    public void endSliderDrag()    { sliderDragging = false; }
    public boolean isSliderDragging() { return sliderDragging; }

    /**
     * Updates music volume from a dragged world X coordinate.
     *
     * @param worldX The unprojected world X coordinate of the touch.
     */
    public void updateSliderFromDrag(float worldX, OrthographicCamera camera) {
        float tsx = sliderTrackX(camera), tsw = sliderTrackW();
        float vol = Math.max(0f, Math.min(1f, (worldX - tsx) / tsw));
        game.getSettingsManager().setMusicVolume(vol);
        game.getSoundManager().setMusicVolume(vol);
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private float panelX(OrthographicCamera c)  { return c.position.x - panelW / 2f; }
    private float panelY(OrthographicCamera c)  { return c.position.y - panelH / 2f; }
    private float resumeX(OrthographicCamera c) { return c.position.x + 16f; }
    private float backX(OrthographicCamera c)   { return c.position.x - btnSize - 16f; }
    private float backY(OrthographicCamera c)   { return panelY(c) + 20f; }
    private float sliderTrackX(OrthographicCamera c) { return panelX(c) + panelW * 0.18f; }
    private float sliderTrackW()  { return panelW * 0.64f; }
    private float sliderY(OrthographicCamera c) { return panelY(c) + btnSize + 38f; }

    public float getSliderTrackX(OrthographicCamera c) { return sliderTrackX(c); }
    public float getSliderTrackW()                     { return sliderTrackW(); }

    private static float camLeft(OrthographicCamera c, Viewport v) { return c.position.x - v.getWorldWidth()  / 2f; }
    private static float camBot (OrthographicCamera c, Viewport v) { return c.position.y - v.getWorldHeight() / 2f; }

    public float getUiScale() { return uiScale; }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void ensurePanel() {
        int tw = (int) panelW, th = (int) panelH;
        if (panelTexture == null || tw != lastPanelW || th != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();
            panelTexture = createRoundedRect(tw, th, (int)(24 * uiScale), COL_PANEL);
            lastPanelW = tw; lastPanelH = th;
        }
    }

    private void drawShadowText(String text, float x, float y, Color color, float shadow) {
        pauseFont.setColor(0, 0, 0, color.a * 0.4f);
        pauseFont.draw(batch, text, x + shadow, y - shadow);
        pauseFont.setColor(color);
        pauseFont.draw(batch, text, x, y);
    }

    private static boolean hits(float tx, float ty, float x, float y, float w, float h) {
        return tx >= x && tx <= x + w && ty >= y && ty <= y + h;
    }

    private static Texture createRoundedRect(int w, int h, int r, Color color) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        pm.setColor(color);
        pm.fillRectangle(r, 0, w - 2 * r, h);
        pm.fillRectangle(0, r, w, h - 2 * r);
        pm.fillCircle(r, r, r);
        pm.fillCircle(w - r, r, r);
        pm.fillCircle(r, h - r, r);
        pm.fillCircle(w - r, h - r, r);
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    public void dispose() {
        if (panelTexture != null) { panelTexture.dispose(); panelTexture = null; }
    }
}
