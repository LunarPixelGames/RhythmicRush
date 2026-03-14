package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.SettingsManager;

public class MainMenuScreen extends AbstractScreen {

    // ── Menu regions ──────────────────────────────────────────────────────────
    private TextureRegion title, startButton, settingsButton, backArrow;
    private Color bgColor;

    private float titleX,    titleY,    titleW,    titleH;
    private float startX,    startY,    startW,    startH;
    private float settingsX, settingsY, settingsW, settingsH;

    // ── Settings overlay ──────────────────────────────────────────────────────
    private boolean       settingsOpen  = false;
    private ShapeRenderer shapes;
    private BitmapFont    font;
    private GlyphLayout   layout;

    private float panelX, panelY, panelW, panelH;
    private float backX,  backY,  backW,  backH;
    private float rowStartY;

    // Row indices
    private static final int ROW_MENU_MUSIC    = 0;
    private static final int ROW_VOLUME        = 1;
    private static final int ROW_HITBOXES      = 2;
    private static final int ROW_HITBOXES_DEATH= 3;
    private static final int ROW_LOCK_CURSOR   = 4;
    private static final int ROW_SHOW_FPS      = 5;
    private static final int ROW_CAP_FPS       = 6;
    private static final int ROW_FPS_VALUE     = 7;
    private static final int ROW_VSYNC         = 8;

    /** Total rows drawn (lock cursor only on desktop, fps value only when cap is on). */
    private int visibleRowCount() {
        int n = 7; // 0-6 always (lock cursor counts on desktop, skipped on mobile but panel still sized for desktop)
        if (game.getSettingsManager().capFps) n = 8;
        return n;
    }

    private static final float ROW_STEP = 64f;

    // Slider drag
    private boolean draggingSlider = false;

    // FPS cap int input
    private boolean       fpsInputActive = false;
    private StringBuilder fpsInputBuffer = new StringBuilder();

    private Texture panelTexture;
    private int     lastPanelW = -1, lastPanelH = -1;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color COL_OVERLAY  = new Color(0f,    0f,    0f,    0.62f);
    private static final Color COL_PANEL    = new Color(0.13f, 0.13f, 0.19f, 1f);
    private static final Color COL_LABEL    = new Color(1f,    1f,    1f,    0.90f);
    private static final Color COL_DIM      = new Color(1f,    1f,    1f,    0.45f);
    private static final Color COL_ON       = new Color(0.35f, 0.85f, 0.55f, 1f);
    private static final Color COL_OFF      = new Color(0.50f, 0.50f, 0.55f, 1f);
    private static final Color COL_TRACK    = new Color(0.28f, 0.28f, 0.35f, 1f);
    private static final Color COL_FILL     = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_THUMB    = new Color(1f,    1f,    1f,    1f);
    private static final Color COL_HEADING  = new Color(1f,    0.85f, 0.35f, 1f);
    private static final Color COL_INPUT_BG = new Color(0.18f, 0.18f, 0.26f, 1f);
    private static final Color COL_INPUT_BD = new Color(0.35f, 0.65f, 1.00f, 1f);

    // ─────────────────────────────────────────────────────────────────────────

    public MainMenuScreen(RhythmicRushGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();

        title          = game.getAtlasManager().getMenuAtlas().findRegion("title");
        startButton    = game.getAtlasManager().getMenuAtlas().findRegion("start_button");
        settingsButton = game.getAtlasManager().getMenuAtlas().findRegion("settings_button");
        backArrow      = game.getAtlasManager().getLevelSelectAtlas().findRegion("back");

        Random rand = new Random();
        bgColor = new Color(
            0.2f + 0.6f * rand.nextFloat(),
            0.2f + 0.6f * rand.nextFloat(),
            0.2f + 0.6f * rand.nextFloat(), 1f);

        shapes = new ShapeRenderer();
        font   = loadFont(32);
        layout = new GlyphLayout();

        if (game.getSettingsManager().menuMusicEnabled)
            game.getSoundManager().playMenuMusic();
        else
            game.getSoundManager().stopMenuMusic();

        updateScaledSizes();
    }

    // ── Geometry ──────────────────────────────────────────────────────────────

    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        float maxTitleWidth = vw * 0.9f;
        float titleScale    = (maxTitleWidth / title.getRegionWidth()) * 0.675f;
        titleW = title.getRegionWidth()  * titleScale;
        titleH = title.getRegionHeight() * titleScale;
        titleX = vw / 2f - titleW / 2f;
        titleY = vh - titleH - 20 + 30;

        float maxStartW  = vw * 0.25f * 0.75f;
        float startScale = maxStartW / startButton.getRegionWidth();
        startW = startButton.getRegionWidth()  * startScale;
        startH = startButton.getRegionHeight() * startScale;
        startX = vw / 2f - startW / 2f;
        startY = vh / 2f - startH / 2f;

        float maxSettingsW  = vw * 0.1f * 0.85f;
        float settingsScale = maxSettingsW / settingsButton.getRegionWidth();
        settingsW = settingsButton.getRegionWidth()  * settingsScale;
        settingsH = settingsButton.getRegionHeight() * settingsScale;
        settingsX = 20;
        settingsY = 20 - 10;

        // Panel height accounts for max possible rows (9) so it never jumps in size
        panelW = Math.min(vw * 0.72f, 740f);
        panelH = 9 * ROW_STEP + 110f;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f;

        backW = vw * 0.065f;
        backH = backW;
        backX = panelX + 12f;
        backY = panelY + panelH - backH - 12f;

        rowStartY = panelY + panelH - 80f;

        lastPanelW = -1;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    protected void update(float delta) {
        if (settingsOpen) handleSettingsInput();
        else              handleMenuInput();
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    @Override
    protected void draw() {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        game.getBatch().draw(title,          titleX,    titleY,    titleW,    titleH);
        game.getBatch().draw(startButton,    startX,    startY,    startW,    startH);
        game.getBatch().draw(settingsButton, settingsX, settingsY, settingsW, settingsH);
        game.getBatch().end();

        if (!settingsOpen) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_OVERLAY);
        shapes.rect(0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        int texW = (int) panelW, texH = (int) panelH;
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();
            panelTexture = createRoundedRect(texW, texH,
                (int)(26f * (panelW / 740f)), COL_PANEL);
            lastPanelW = texW; lastPanelH = texH;
        }
        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        game.getBatch().draw(panelTexture, panelX, panelY);
        game.getBatch().draw(backArrow, backX, backY, backW, backH);
        game.getBatch().end();

        drawSettingsRows();
    }

    // ── Settings rows ─────────────────────────────────────────────────────────

    private void drawSettingsRows() {
        SettingsManager s = game.getSettingsManager();

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        font.getData().setScale(1f);
        font.setColor(COL_HEADING);
        layout.setText(font, "Settings");
        font.draw(game.getBatch(), "Settings",
            panelX + panelW / 2f - layout.width / 2f,
            panelY + panelH - 16f);
        game.getBatch().end();

        drawToggleRow(ROW_MENU_MUSIC,     "Menu Music",            s.menuMusicEnabled);
        drawSliderRow(ROW_VOLUME,         "Music Volume",          s.musicVolume);
        drawToggleRow(ROW_HITBOXES,       "Show Hitboxes",         s.showHitboxes);
        drawToggleRow(ROW_HITBOXES_DEATH, "Show Hitboxes on Death",s.showHitboxesOnDeath);
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop)
            drawToggleRow(ROW_LOCK_CURSOR, "Lock Cursor in Game",  s.lockCursorInGame);
        drawToggleRow(ROW_SHOW_FPS,       "Show FPS",              s.showFps);
        drawToggleRow(ROW_CAP_FPS,        "Cap FPS",               s.capFps);
        if (s.capFps)
            drawIntFieldRow(ROW_FPS_VALUE, "FPS Limit", s.fpsCapValue);
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop)
            drawToggleRow(ROW_VSYNC, "VSync", s.enableVsync);    }

    // ── Row renderers ─────────────────────────────────────────────────────────

    private void drawToggleRow(int row, String label, boolean value) {
        float ry        = rowY(row);
        float rightEdge = panelX + panelW - 28f;
        float pillW = 54f, pillH = 28f;
        float pillX = rightEdge - pillW;
        float pillY = ry - pillH / 2f;
        float r     = pillH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(value ? COL_ON : COL_OFF);
        shapes.circle(pillX + r,         pillY + r, r, 24);
        shapes.circle(pillX + pillW - r, pillY + r, r, 24);
        shapes.rect(pillX + r, pillY, pillW - pillH, pillH);
        shapes.setColor(COL_THUMB);
        float thumbCX = value ? (pillX + pillW - r) : (pillX + r);
        shapes.circle(thumbCX, pillY + r, r - 4f, 24);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        font.getData().setScale(0.70f);
        font.setColor(COL_LABEL);
        layout.setText(font, label);
        font.draw(game.getBatch(), label, panelX + 28f, ry + layout.height / 2f);
        font.getData().setScale(0.44f);
        font.setColor(value ? COL_ON : COL_DIM);
        String hint = value ? "ON" : "OFF";
        layout.setText(font, hint);
        font.draw(game.getBatch(), hint,
            pillX + pillW / 2f - layout.width / 2f,
            ry + layout.height / 2f);
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    private void drawSliderRow(int row, String label, float value) {
        float ry        = rowY(row);
        float rightEdge = panelX + panelW - 28f;
        float trackW    = panelW * 0.36f;
        float trackH    = 6f;
        float trackX    = rightEdge - trackW;
        float trackY    = ry - trackH / 2f;
        float thumbR    = 12f;
        float fillW     = trackW * value;
        float thumbCX   = trackX + fillW;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_TRACK);
        shapes.rect(trackX, trackY, trackW, trackH);
        shapes.setColor(COL_FILL);
        if (fillW > 0) shapes.rect(trackX, trackY, fillW, trackH);
        shapes.setColor(COL_THUMB);
        shapes.circle(thumbCX, ry, thumbR, 24);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        font.getData().setScale(0.70f);
        font.setColor(COL_LABEL);
        layout.setText(font, label);
        font.draw(game.getBatch(), label, panelX + 28f, ry + layout.height / 2f);
        font.getData().setScale(0.52f);
        font.setColor(COL_DIM);
        String pct = Math.round(value * 100f) + "%";
        layout.setText(font, pct);
        font.draw(game.getBatch(), pct,
            trackX - layout.width - 12f,
            ry + layout.height / 2f);
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    /**
     * Draws a labelled integer input box.
     * Clicking the box activates keyboard input; digits only, ENTER/click-away confirms.
     */
    private void drawIntFieldRow(int row, String label, int value) {
        float ry        = rowY(row);
        float rightEdge = panelX + panelW - 28f;
        float boxW = 90f, boxH = 34f;
        float boxX = rightEdge - boxW;
        float boxY = ry - boxH / 2f;

        // box background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_INPUT_BG);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        // border (brighter when active)
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(fpsInputActive
            ? COL_INPUT_BD
            : new Color(COL_INPUT_BD.r, COL_INPUT_BD.g, COL_INPUT_BD.b, 0.4f));
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // value text (show buffer if active, stored value otherwise)
        String display = fpsInputActive ? (fpsInputBuffer + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : " ")) : String.valueOf(value);
        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        font.getData().setScale(0.70f);
        font.setColor(COL_LABEL);
        layout.setText(font, label);
        font.draw(game.getBatch(), label, panelX + 28f, ry + layout.height / 2f);
        font.getData().setScale(0.65f);
        font.setColor(fpsInputActive ? Color.WHITE : COL_DIM);
        layout.setText(font, display);
        font.draw(game.getBatch(), display,
            boxX + boxW / 2f - layout.width / 2f,
            ry + layout.height / 2f);
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))  game.setScreen(new LevelSelectScreen(game));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();

        if (!Gdx.input.justTouched()) return;
        Vector2 t = unproject();
        if (hits(t, startX,    startY,    startW,    startH))    game.setScreen(new LevelSelectScreen(game));
        if (hits(t, settingsX, settingsY, settingsW, settingsH)) settingsOpen = true;
    }

    private void handleSettingsInput() {
        SettingsManager s = game.getSettingsManager();

        // ── Keyboard input for fps cap value field ────────────────────────────
        if (fpsInputActive) {
            // Read typed characters this frame
            // LibGDX doesn't give us keyTyped in a Screen easily without an InputProcessor,
            // so we poll the keys that matter: digits 0-9, backspace, enter
            for (int k = Input.Keys.NUM_0; k <= Input.Keys.NUM_9; k++) {
                if (Gdx.input.isKeyJustPressed(k)) {
                    fpsInputBuffer.append((char)('0' + (k - Input.Keys.NUM_0)));
                }
            }
            for (int k = Input.Keys.NUMPAD_0; k <= Input.Keys.NUMPAD_9; k++) {
                if (Gdx.input.isKeyJustPressed(k)) {
                    fpsInputBuffer.append((char)('0' + (k - Input.Keys.NUMPAD_0)));
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && fpsInputBuffer.length() > 0)
                fpsInputBuffer.deleteCharAt(fpsInputBuffer.length() - 1);

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) confirmFpsInput(s);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (fpsInputActive) { confirmFpsInput(s); }
            else                { closeSettings(); }
            return;
        }

        if (Gdx.input.justTouched()) {
            Vector2 t = unproject();

            // If input field is active and click is outside it, confirm
            if (fpsInputActive && s.capFps && !hitRow(t, ROW_FPS_VALUE)) {
                confirmFpsInput(s);
            }

            if (hits(t, backX, backY, backW, backH)) { closeSettings(); return; }

            if (hitRow(t, ROW_MENU_MUSIC)) {
                s.menuMusicEnabled = !s.menuMusicEnabled;
                if (s.menuMusicEnabled) game.getSoundManager().playMenuMusic();
                else                    game.getSoundManager().stopMenuMusic();
                s.save();
            }
            if (hitRow(t, ROW_HITBOXES))       { s.showHitboxes        = !s.showHitboxes;        s.save(); }
            if (hitRow(t, ROW_HITBOXES_DEATH)) { s.showHitboxesOnDeath = !s.showHitboxesOnDeath; s.save(); }
            if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop)
                if (hitRow(t, ROW_LOCK_CURSOR)) { s.lockCursorInGame = !s.lockCursorInGame; s.save(); }
            if (hitRow(t, ROW_SHOW_FPS))        { s.showFps   = !s.showFps;   s.save(); }
            if (hitRow(t, ROW_CAP_FPS)) {
                s.capFps = !s.capFps;
                s.applyFpsCap();
                s.save();
                if (!s.capFps) { fpsInputActive = false; lastPanelW = -1; }
            }
            // Click on the int field box to activate it
            if (s.capFps && hitRow(t, ROW_FPS_VALUE) && !fpsInputActive) {
                fpsInputActive = true;
                fpsInputBuffer.setLength(0);
                fpsInputBuffer.append(s.fpsCapValue);
            }
            if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) {
                if (hitRow(t, ROW_VSYNC)) {
                    s.enableVsync = !s.enableVsync;
                    s.applyVsync();
                    s.save();
                }
            }
            if (sliderHit(t)) draggingSlider = true;
        }

        if (Gdx.input.isTouched() && draggingSlider) {
            float tx     = unproject().x;
            float trackX = sliderTrackX();
            float trackW = panelW * 0.36f;
            s.musicVolume = Math.max(0f, Math.min(1f, (tx - trackX) / trackW));
            game.getSoundManager().setMusicVolume(s.musicVolume);
            s.save();
        }

        if (!Gdx.input.isTouched()) draggingSlider = false;
    }

    private void confirmFpsInput(SettingsManager s) {
        fpsInputActive = false;
        if (fpsInputBuffer.length() > 0) {
            try {
                int val = Integer.parseInt(fpsInputBuffer.toString());
                if (val > 0) {
                    s.fpsCapValue = val;
                    s.applyFpsCap();
                    s.save();
                }
            } catch (NumberFormatException ignored) {}
        }
        fpsInputBuffer.setLength(0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void closeSettings() {
        settingsOpen   = false;
        draggingSlider = false;
        fpsInputActive = false;
        fpsInputBuffer.setLength(0);
    }

    private float rowY(int row)  { return rowStartY - row * ROW_STEP; }

    private boolean hitRow(Vector2 t, int row) {
        float ry = rowY(row);
        return t.x >= panelX + 10f && t.x <= panelX + panelW - 10f
            && t.y >= ry - 26f    && t.y <= ry + 26f;
    }

    private float sliderTrackX() { return panelX + panelW - 28f - panelW * 0.36f; }

    private boolean sliderHit(Vector2 t) {
        float ry = rowY(ROW_VOLUME);
        float tx = sliderTrackX();
        float tw = panelW * 0.36f;
        return t.x >= tx - 16f && t.x <= tx + tw + 16f && t.y >= ry - 20f && t.y <= ry + 20f;
    }

    private static boolean hits(Vector2 t, float x, float y, float w, float h) {
        return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h;
    }

    private Vector2 unproject() {
        Vector2 v = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(v);
        return v;
    }

    private BitmapFont loadFont(int size) {
        try {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/zendots-regular.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter p =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.magFilter = Texture.TextureFilter.Linear;
            p.minFilter = Texture.TextureFilter.Linear;
            BitmapFont f = gen.generateFont(p);
            gen.dispose();
            return f;
        } catch (Exception e) {
            return new BitmapFont();
        }
    }

    private Texture createRoundedRect(int w, int h, int r, Color color) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0); pm.fill();
        pm.setColor(color);
        pm.fillRectangle(r, 0, w - 2*r, h);
        pm.fillRectangle(0, r, w, h - 2*r);
        pm.fillCircle(r,   r,   r); pm.fillCircle(w-r, r,   r);
        pm.fillCircle(r,   h-r, r); pm.fillCircle(w-r, h-r, r);
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateScaledSizes();
    }

    @Override
    public void dispose() {
        if (shapes       != null) shapes.dispose();
        if (font         != null) font.dispose();
        if (panelTexture != null) panelTexture.dispose();
    }
}
