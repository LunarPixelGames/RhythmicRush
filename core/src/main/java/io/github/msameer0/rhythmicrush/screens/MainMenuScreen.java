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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.settings.SettingsManager;
import io.github.msameer0.rhythmicrush.ui.AnimatedButton;

public class MainMenuScreen extends AbstractScreen {

    // ── Menu regions ──────────────────────────────────────────────────────────
    private TextureRegion title, startButton, settingsButton, backArrow;
    private Color bgColor;

    private float titleX, titleY, titleW, titleH;

    // Animated buttons — handle press/release/bounce themselves
    private AnimatedButton btnPlay;
    private AnimatedButton btnSettings;

    // ── Settings overlay ──────────────────────────────────────────────────────
    private boolean       settingsOpen = false;
    private ShapeRenderer shapes;
    private BitmapFont    font;
    private GlyphLayout   layout;

    // ── Categories ────────────────────────────────────────────────────────────
    private static final int      CAT_GAMEPLAY = 0;
    private static final int      CAT_GRAPHICS = 1;
    private static final int      CAT_COUNT    = 2;
    private static final String[] CAT_NAMES    = { "Gameplay", "Graphics" };

    /**
     * Max rows shown per sub-page within a category.
     * If a category has more rows than this, it automatically gets multiple sub-pages.
     * Increase this number if you want more rows per page, or decrease to paginate more aggressively.
     */
    private static final int MAX_ROWS_PER_PAGE = 5;

    private int currentCat     = CAT_GAMEPLAY;
    private int currentSubPage = 0;  // sub-page within the current category

    // ── Panel geometry ────────────────────────────────────────────────────────
    private float panelX, panelY, panelW, panelH;
    private float backX,  backY,  backW,  backH;
    private float rowStartY;
    private float arrowLeftX, arrowRightX, arrowY, arrowSize;

    /**
     * Target fraction of viewport height for the settings panel.
     * Change this one value to resize the entire settings menu proportionally.
     */
    private static final float PANEL_HEIGHT_FRACTION = 0.88f;

    // Derived at layout time — not fixed constants so they scale with screen size
    private float rowStep;
    private float panelPadT;
    private float panelPadB;
    private float settingsFontScale;   // label scale applied to the loaded font
    private float settingsHeadingScale; // heading "Settings" scale

    // ── Interaction state ─────────────────────────────────────────────────────
    private boolean draggingSlider    = false;
    private int     draggingSliderRow = -1;

    private boolean       fpsInputActive = false;
    private StringBuilder fpsInputBuffer = new StringBuilder();

    private Texture panelTexture;
    private int     lastPanelW = -1, lastPanelH = -1;

    // ── Row model ─────────────────────────────────────────────────────────────
    private enum RowType { TOGGLE, SLIDER, INT_FIELD }

    private static class SettingRow {
        final RowType type;
        final String  label;
        final String  id;
        SettingRow(RowType t, String label, String id) { this.type = t; this.label = label; this.id = id; }
    }

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color COL_OVERLAY   = new Color(0f,    0f,    0f,    0.62f);
    private static final Color COL_PANEL     = new Color(0.13f, 0.13f, 0.19f, 1f);
    private static final Color COL_LABEL     = new Color(1f,    1f,    1f,    0.90f);
    private static final Color COL_DIM       = new Color(1f,    1f,    1f,    0.45f);
    private static final Color COL_ON        = new Color(0.35f, 0.85f, 0.55f, 1f);
    private static final Color COL_OFF       = new Color(0.50f, 0.50f, 0.55f, 1f);
    private static final Color COL_TRACK     = new Color(0.28f, 0.28f, 0.35f, 1f);
    private static final Color COL_FILL      = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_THUMB     = new Color(1f,    1f,    1f,    1f);
    private static final Color COL_HEADING   = new Color(1f,    0.85f, 0.35f, 1f);
    private static final Color COL_TAB_ACT   = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_TAB_INACT = new Color(0.35f, 0.35f, 0.45f, 1f);
    private static final Color COL_INPUT_BG  = new Color(0.18f, 0.18f, 0.26f, 1f);
    private static final Color COL_INPUT_BD  = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_DOT_ACT   = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_DOT_INACT = new Color(0.35f, 0.35f, 0.45f, 1f);

    // ─────────────────────────────────────────────────────────────────────────

    public MainMenuScreen(RhythmicRushGame game) { super(game); }

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
        font   = game.getFontManager().get(FontManager.SIZE_LARGE);
        layout = new GlyphLayout();

        // Buttons created here with placeholder bounds — updateScaledSizes() sets real bounds
        btnPlay     = new AnimatedButton(startButton,    0, 0, 0, 0, () -> game.setScreen(new LevelSelectScreen(game)));
        btnSettings = new AnimatedButton(settingsButton, 0, 0, 0, 0, () -> settingsOpen = true);

        if (game.getSettingsManager().menuMusicEnabled)
            game.getSoundManager().playMenuMusic();
        else
            game.getSoundManager().stopMenuMusic();

        updateScaledSizes();
    }

    // ── Row list builders ─────────────────────────────────────────────────────

    /** Returns ALL rows for a category (may be more than MAX_ROWS_PER_PAGE). */
    private List<SettingRow> buildAllRows(int cat) {
        SettingsManager s = game.getSettingsManager();
        boolean desktop = Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop;
        List<SettingRow> rows = new ArrayList<>();

        if (cat == CAT_GAMEPLAY) {
            rows.add(new SettingRow(RowType.TOGGLE,    "Menu Music",             "menuMusic"));
            rows.add(new SettingRow(RowType.SLIDER,    "Music Volume",           "volume"));
            rows.add(new SettingRow(RowType.TOGGLE,    "Show Hitboxes",          "hitboxes"));
            rows.add(new SettingRow(RowType.TOGGLE,    "Show Hitboxes on Death", "hitboxesDeath"));
            rows.add(new SettingRow(RowType.TOGGLE,    "Show Percentage",        "showPercentage"));
            rows.add(new SettingRow(RowType.TOGGLE,    "Show Progress Bar",      "showProgressBar"));
            if (desktop)
                rows.add(new SettingRow(RowType.TOGGLE, "Lock Cursor in Game",   "lockCursor"));
        } else { // CAT_GRAPHICS
            rows.add(new SettingRow(RowType.TOGGLE,    "Show FPS",               "showFps"));
            if (desktop) {
                rows.add(new SettingRow(RowType.TOGGLE, "Cap FPS",               "capFps"));
                if (s.capFps)
                    rows.add(new SettingRow(RowType.INT_FIELD, "FPS Limit",      "fpsValue"));
            }
            if (desktop)
                rows.add(new SettingRow(RowType.TOGGLE, "VSync",                 "vsync"));
        }
        return rows;
    }

    /** Returns the rows visible on the current sub-page of the given category. */
    private List<SettingRow> getPageRows(int cat, int subPage) {
        List<SettingRow> all   = buildAllRows(cat);
        int start = subPage * MAX_ROWS_PER_PAGE;
        int end   = Math.min(start + MAX_ROWS_PER_PAGE, all.size());
        if (start >= all.size()) return new ArrayList<>();
        return all.subList(start, end);
    }

    /** How many sub-pages does a category have given its current dynamic row count? */
    private int subPageCount(int cat) {
        int total = buildAllRows(cat).size();
        return Math.max(1, (int) Math.ceil((double) total / MAX_ROWS_PER_PAGE));
    }

    // ── Geometry ──────────────────────────────────────────────────────────────

    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        // Title — bumped from 0.75 → 0.85 width, height cap from 30% → 36%
        float maxTitleWidth = vw * 0.85f;
        float titleScale    = (maxTitleWidth / title.getRegionWidth()) * 0.675f;
        float maxTitleHeight = vh * 0.36f;
        if (title.getRegionHeight() * titleScale > maxTitleHeight)
            titleScale = maxTitleHeight / title.getRegionHeight();
        titleW = title.getRegionWidth()  * titleScale;
        titleH = title.getRegionHeight() * titleScale;
        titleX = vw / 2f - titleW / 2f;
        titleY = vh - titleH - vh * 0.03f;

        // Play button — reduced from 0.25*0.75 → 0.25*0.55
        float maxStartW  = vw * 0.25f * 0.55f;
        float startScale = maxStartW / startButton.getRegionWidth();
        float startW = startButton.getRegionWidth()  * startScale;
        float startH = startButton.getRegionHeight() * startScale;
        float startX = vw / 2f - startW / 2f;
        float minY   = titleY - startH - vh * 0.06f;
        float midY   = vh / 2f - startH / 2f;
        float startY = Math.min(midY, minY);
        if (btnPlay != null) btnPlay.setBounds(startX, startY, startW, startH);

        float maxSettingsW  = vw * 0.1f * 0.85f;
        float settingsScale = maxSettingsW / settingsButton.getRegionWidth();
        float settingsW = settingsButton.getRegionWidth()  * settingsScale;
        float settingsH = settingsButton.getRegionHeight() * settingsScale;
        float settingsX = 20;
        float settingsY = 20 - 10f;
        if (btnSettings != null) btnSettings.setBounds(settingsX, settingsY, settingsW, settingsH);

        panelW = Math.min(vw * 0.72f, 740f);

        // Derive all spacing and font scales from the target panel height
        // so the entire settings menu scales as one unit
        float targetH = vh * PANEL_HEIGHT_FRACTION;
        float dotsExtra = targetH * 0.045f;  // ~4.5% for dot indicators
        float padT   = targetH * 0.20f;      // ~20% for heading + tabs
        float padB   = targetH * 0.035f;     // ~3.5% bottom padding
        float rowsH  = targetH - padT - padB - dotsExtra;
        rowStep      = rowsH / MAX_ROWS_PER_PAGE;
        panelPadT    = padT;
        panelPadB    = padB;

        // Font scales relative to a reference row height of 68px
        float scaleRef       = rowStep / 68f;
        settingsFontScale    = 0.65f * scaleRef;
        settingsHeadingScale = 1.00f * scaleRef;

        recomputePanelHeight();
    }

    /** Recomputes panel height using MAX_ROWS_PER_PAGE so size is always static, and re-centers panel. */
    private void recomputePanelHeight() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        currentSubPage = Math.max(0, Math.min(currentSubPage, subPageCount(currentCat) - 1));

        float dotsExtra = rowStep * 0.5f;
        panelH = MAX_ROWS_PER_PAGE * rowStep + panelPadT + panelPadB + dotsExtra;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f;

        backW = panelH * 0.075f;
        backH = backW;
        backX = panelX + 12f;
        backY = panelY + panelH - backH - 12f;

        rowStartY = panelY + panelH - panelPadT;

        arrowSize   = backH;
        arrowY      = panelY + panelH - arrowSize - 12f;
        arrowLeftX  = panelX + backW + 20f;
        arrowRightX = panelX + panelW - arrowSize - 12f;

        lastPanelW = -1;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    protected void update(float delta) {
        if (!settingsOpen) {
            btnPlay.update(delta);
            btnSettings.update(delta);
        }
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
        game.getBatch().draw(title, titleX, titleY, titleW, titleH);
        btnPlay.draw(game.getBatch());
        btnSettings.draw(game.getBatch());
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
            panelTexture = createRoundedRect(texW, texH, (int)(26f * (panelW / 740f)), COL_PANEL);
            lastPanelW = texW; lastPanelH = texH;
        }

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        game.getBatch().draw(panelTexture, panelX, panelY);
        game.getBatch().draw(backArrow, backX, backY, backW, backH);
        game.getBatch().end();

        drawHeadingAndTabs();
        drawSettingsRows(getPageRows(currentCat, currentSubPage));
        drawSubPageDots();
    }

    // ── Heading + category tabs ───────────────────────────────────────────────

    private void drawHeadingAndTabs() {
        // "Settings" heading
        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        font.getData().setScale(settingsHeadingScale);
        font.setColor(COL_HEADING);
        layout.setText(font, "Settings");
        font.draw(game.getBatch(), "Settings",
            panelX + panelW / 2f - layout.width / 2f,
            panelY + panelH - 16f);

        // Category tab labels
        float tabFontScale = settingsFontScale * 0.92f;
        font.getData().setScale(tabFontScale);
        float tabY = panelY + panelH - panelPadT * 0.47f;
        // Measure text height once for underline placement
        layout.setText(font, CAT_NAMES[0]);
        float tabTextH = layout.height;
        for (int i = 0; i < CAT_COUNT; i++) {
            float tabW  = panelW / CAT_COUNT;
            float tabCX = panelX + tabW * i + tabW / 2f;
            font.setColor(i == currentCat ? COL_TAB_ACT : COL_TAB_INACT);
            layout.setText(font, CAT_NAMES[i]);
            font.draw(game.getBatch(), CAT_NAMES[i], tabCX - layout.width / 2f, tabY);
        }
        font.getData().setScale(1f);
        game.getBatch().end();

        // Active tab underline — sits 3px below the text baseline
        float tabW     = panelW / CAT_COUNT;
        float tabCX    = panelX + tabW * currentCat;
        float underlineY = tabY - tabTextH - 3f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_TAB_ACT);
        shapes.rect(tabCX + 8f, underlineY, tabW - 16f, 3f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        drawArrow(arrowLeftX,  arrowY, arrowSize, true);
        drawArrow(arrowRightX, arrowY, arrowSize, false);
    }

    private void drawArrow(float x, float y, float size, boolean pointLeft) {
        float cx = x + size / 2f, cy = y + size / 2f, hs = size * 0.28f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_DIM);
        if (pointLeft) shapes.triangle(cx + hs, cy + hs, cx + hs, cy - hs, cx - hs, cy);
        else           shapes.triangle(cx - hs, cy + hs, cx - hs, cy - hs, cx + hs, cy);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Small dot indicators at the bottom of the panel when there are multiple sub-pages. */
    private void drawSubPageDots() {
        int total = subPageCount(currentCat);
        if (total <= 1) return;
        float dotR   = 5f;
        float dotGap = dotR * 2f + 6f;
        float totalW = total * dotGap - 6f;
        float startX = panelX + panelW / 2f - totalW / 2f + dotR;
        float dotY   = panelY + panelPadB / 2f + dotR;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < total; i++) {
            shapes.setColor(i == currentSubPage ? COL_DOT_ACT : COL_DOT_INACT);
            shapes.circle(startX + i * dotGap, dotY, dotR, 16);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Settings rows ─────────────────────────────────────────────────────────

    private void drawSettingsRows(List<SettingRow> rows) {
        SettingsManager s = game.getSettingsManager();
        for (int i = 0; i < rows.size(); i++) {
            SettingRow row = rows.get(i);
            float ry = rowY(i);
            switch (row.type) {
                case TOGGLE:    drawToggleRow(ry, row.label, getToggleValue(row.id, s)); break;
                case SLIDER:    drawSliderRow(ry, row.label, s.musicVolume);             break;
                case INT_FIELD: drawIntFieldRow(ry, row.label, s.fpsCapValue);           break;
            }
        }
    }

    private boolean getToggleValue(String id, SettingsManager s) {
        switch (id) {
            case "menuMusic":     return s.menuMusicEnabled;
            case "hitboxes":      return s.showHitboxes;
            case "hitboxesDeath": return s.showHitboxesOnDeath;
            case "lockCursor":    return s.lockCursorInGame;
            case "showFps":       return s.showFps;
            case "capFps":        return s.capFps;
            case "vsync":         return s.enableVsync;
            case "showPercentage":  return s.showPercentage;
            case "showProgressBar": return s.showProgressBar;
            default:              return false;
        }
    }

    // ── Row renderers ─────────────────────────────────────────────────────────

    private void drawToggleRow(float ry, String label, boolean value) {
        float rightEdge = panelX + panelW - 28f;
        float pillH = rowStep * 0.38f;  // scales with row height
        float pillW = pillH * 1.92f;
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
        shapes.circle(thumbCX, pillY + r, r - r * 0.3f, 24);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        font.getData().setScale(settingsFontScale);
        font.setColor(COL_LABEL);
        layout.setText(font, label);
        font.draw(game.getBatch(), label, panelX + 28f, ry + layout.height / 2f);
        font.getData().setScale(settingsFontScale * 0.62f);
        font.setColor(value ? COL_ON : COL_DIM);
        String hint = value ? "ON" : "OFF";
        layout.setText(font, hint);
        font.draw(game.getBatch(), hint,
            pillX + pillW / 2f - layout.width / 2f, ry + layout.height / 2f);
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    private void drawSliderRow(float ry, String label, float value) {
        float rightEdge = panelX + panelW - 28f;
        float trackW    = panelW * 0.36f;
        float trackH    = rowStep * 0.07f;
        float trackX    = rightEdge - trackW;
        float trackY    = ry - trackH / 2f;
        float thumbR    = rowStep * 0.16f;
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
        font.getData().setScale(settingsFontScale);
        font.setColor(COL_LABEL);
        layout.setText(font, label);
        font.draw(game.getBatch(), label, panelX + 28f, ry + layout.height / 2f);
        font.getData().setScale(settingsFontScale * 0.77f);
        font.setColor(COL_DIM);
        String pct = Math.round(value * 100f) + "%";
        layout.setText(font, pct);
        font.draw(game.getBatch(), pct, trackX - layout.width - 12f, ry + layout.height / 2f);
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    private void drawIntFieldRow(float ry, String label, int value) {
        float rightEdge = panelX + panelW - 28f;
        float boxH = rowStep * 0.44f;
        float boxW = boxH * 2.8f;
        float boxX = rightEdge - boxW;
        float boxY = ry - boxH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_INPUT_BG);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(fpsInputActive
            ? COL_INPUT_BD
            : new Color(COL_INPUT_BD.r, COL_INPUT_BD.g, COL_INPUT_BD.b, 0.4f));
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        String display = fpsInputActive
            ? (fpsInputBuffer + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : " "))
            : String.valueOf(value);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        font.getData().setScale(settingsFontScale);
        font.setColor(COL_LABEL);
        layout.setText(font, label);
        font.draw(game.getBatch(), label, panelX + 28f, ry + layout.height / 2f);
        font.getData().setScale(settingsFontScale * 0.95f);
        font.setColor(fpsInputActive ? Color.WHITE : COL_DIM);
        layout.setText(font, display);
        font.draw(game.getBatch(), display,
            boxX + boxW / 2f - layout.width / 2f, ry + layout.height / 2f);
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))  game.setScreen(new LevelSelectScreen(game));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();

        Vector2 t = unproject();

        if (Gdx.input.justTouched()) {
            btnPlay.onTouchDown(t.x, t.y);
            btnSettings.onTouchDown(t.x, t.y);
        }
        if (!Gdx.input.isTouched()) {
            btnPlay.onTouchUp(t.x, t.y);
            btnSettings.onTouchUp(t.x, t.y);
        }
    }

    private void handleSettingsInput() {
        SettingsManager s = game.getSettingsManager();

        // FPS input keyboard
        if (fpsInputActive) {
            for (int k = Input.Keys.NUM_0;    k <= Input.Keys.NUM_9;    k++)
                if (Gdx.input.isKeyJustPressed(k)) fpsInputBuffer.append((char)('0' + (k - Input.Keys.NUM_0)));
            for (int k = Input.Keys.NUMPAD_0; k <= Input.Keys.NUMPAD_9; k++)
                if (Gdx.input.isKeyJustPressed(k)) fpsInputBuffer.append((char)('0' + (k - Input.Keys.NUMPAD_0)));
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && fpsInputBuffer.length() > 0)
                fpsInputBuffer.deleteCharAt(fpsInputBuffer.length() - 1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) confirmFpsInput(s);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (fpsInputActive) confirmFpsInput(s);
            else                closeSettings();
            return;
        }

        // Arrow keys navigate sub-pages / categories
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  navigate(-1, s);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) navigate( 1, s);

        // Slider drag
        if (Gdx.input.isTouched() && draggingSlider) {
            float tx     = unproject().x;
            float trackX = panelX + panelW - 28f - panelW * 0.36f;
            float trackW = panelW * 0.36f;
            s.musicVolume = Math.max(0f, Math.min(1f, (tx - trackX) / trackW));
            game.getSoundManager().setMusicVolume(s.musicVolume);
            s.save();
        }
        if (!Gdx.input.isTouched()) { draggingSlider = false; draggingSliderRow = -1; }

        if (!Gdx.input.justTouched()) return;
        Vector2 t = unproject();

        List<SettingRow> pageRows = getPageRows(currentCat, currentSubPage);

        // Confirm fps input if click is outside its box
        if (fpsInputActive) {
            int fpsIdx = -1;
            for (int i = 0; i < pageRows.size(); i++)
                if ("fpsValue".equals(pageRows.get(i).id)) { fpsIdx = i; break; }
            if (fpsIdx < 0 || !hitIntBox(t, rowY(fpsIdx))) confirmFpsInput(s);
        }

        if (hits(t, backX, backY, backW, backH)) { closeSettings(); return; }

        // Arrow button clicks
        if (hits(t, arrowLeftX,  arrowY, arrowSize, arrowSize)) { navigate(-1, s); return; }
        if (hits(t, arrowRightX, arrowY, arrowSize, arrowSize)) { navigate( 1, s); return; }

        // Category tab clicks
        for (int i = 0; i < CAT_COUNT; i++) {
            float tabW  = panelW / CAT_COUNT;
            float tabX  = panelX + tabW * i;
            float tabTopY = panelY + panelH - 42f;
            if (t.x >= tabX && t.x <= tabX + tabW && t.y >= tabTopY - 28f && t.y <= tabTopY + 8f) {
                if (i != currentCat) {
                    confirmFpsInput(s);
                    draggingSlider = false;
                    currentCat     = i;
                    currentSubPage = 0;
                    recomputePanelHeight();
                }
                return;
            }
        }

        // Per-control row clicks
        for (int i = 0; i < pageRows.size(); i++) {
            SettingRow row = pageRows.get(i);
            float ry = rowY(i);
            switch (row.type) {
                case TOGGLE:
                    if (hitPill(t, ry)) handleToggle(row.id, s);
                    break;
                case SLIDER:
                    if (hitSliderThumb(t, ry, s.musicVolume)) {
                        draggingSlider = true; draggingSliderRow = i;
                    }
                    break;
                case INT_FIELD:
                    if (hitIntBox(t, ry) && !fpsInputActive) {
                        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) {
                            // Desktop: use key-polling approach
                            fpsInputActive = true;
                            fpsInputBuffer.setLength(0);
                            fpsInputBuffer.append(s.fpsCapValue);
                        } else {
                            // Mobile: open the native on-screen keyboard
                            Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
                                @Override
                                public void input(String text) {
                                    try {
                                        int val = Integer.parseInt(text.trim());
                                        if (val > 0) {
                                            s.fpsCapValue = val;
                                            s.applyFpsCap();
                                            s.save();
                                        }
                                    } catch (NumberFormatException ignored) {}
                                }
                                @Override public void canceled() {}
                            }, "FPS Limit", String.valueOf(s.fpsCapValue), "Enter FPS cap");
                        }
                    }
                    break;
            }
        }
    }

    private void handleToggle(String id, SettingsManager s) {
        switch (id) {
            case "menuMusic":
                s.menuMusicEnabled = !s.menuMusicEnabled;
                if (s.menuMusicEnabled) game.getSoundManager().playMenuMusic();
                else                    game.getSoundManager().stopMenuMusic();
                s.save(); break;
            case "hitboxes":      s.showHitboxes        = !s.showHitboxes;        s.save(); break;
            case "hitboxesDeath": s.showHitboxesOnDeath = !s.showHitboxesOnDeath; s.save(); break;
            case "lockCursor":    s.lockCursorInGame    = !s.lockCursorInGame;    s.save(); break;
            case "showFps":       s.showFps   = !s.showFps;   s.save(); break;
            case "showPercentage":  s.showPercentage  = !s.showPercentage;  s.save(); break;
            case "showProgressBar": s.showProgressBar = !s.showProgressBar; s.save(); break;
            case "capFps":
                s.capFps = !s.capFps;
                s.applyFpsCap(); s.save();
                if (!s.capFps) fpsInputActive = false;
                recomputePanelHeight(); break;
            case "vsync":
                s.enableVsync = !s.enableVsync;
                s.applyVsync(); s.save(); break;
        }
    }

    /**
     * Navigate forward/backward through sub-pages and categories.
     * Within a category: cycle sub-pages first.
     * At the boundary: wrap to the next/previous category at sub-page 0.
     */
    private void navigate(int dir, SettingsManager s) {
        confirmFpsInput(s);
        draggingSlider = false; draggingSliderRow = -1;

        int newSub = currentSubPage + dir;
        int pages  = subPageCount(currentCat);

        if (newSub >= 0 && newSub < pages) {
            // Stay in same category, different sub-page
            currentSubPage = newSub;
        } else {
            // Move to next/previous category
            currentCat     = (currentCat + dir + CAT_COUNT) % CAT_COUNT;
            currentSubPage = dir > 0 ? 0 : subPageCount(currentCat) - 1;
        }
        recomputePanelHeight();
    }

    private void confirmFpsInput(SettingsManager s) {
        if (!fpsInputActive) return;
        fpsInputActive = false;
        if (fpsInputBuffer.length() > 0) {
            try {
                int val = Integer.parseInt(fpsInputBuffer.toString());
                if (val > 0) { s.fpsCapValue = val; s.applyFpsCap(); s.save(); }
            } catch (NumberFormatException ignored) {}
        }
        fpsInputBuffer.setLength(0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void closeSettings() {
        settingsOpen   = false;
        draggingSlider = false; draggingSliderRow = -1;
        fpsInputActive = false; fpsInputBuffer.setLength(0);
        currentCat     = CAT_GAMEPLAY;
        currentSubPage = 0;
    }

    private float rowY(int i) { return rowStartY - i * rowStep; }

    private boolean hitPill(Vector2 t, float ry) {
        float pillH = rowStep * 0.38f;
        float pillW = pillH * 1.92f;
        float pillX = panelX + panelW - 28f - pillW;
        float pillY = ry - pillH / 2f;
        return t.x >= pillX - 4f && t.x <= pillX + pillW + 4f
            && t.y >= pillY - 4f && t.y <= pillY + pillH + 4f;
    }

    private boolean hitSliderThumb(Vector2 t, float ry, float value) {
        float trackW  = panelW * 0.36f;
        float trackX  = panelX + panelW - 28f - trackW;
        float thumbR  = rowStep * 0.16f;
        float thumbCX = trackX + trackW * value;
        return t.x >= trackX - thumbR && t.x <= trackX + trackW + thumbR
            && t.y >= ry - thumbR    && t.y <= ry + thumbR;
    }

    private boolean hitIntBox(Vector2 t, float ry) {
        float boxH = rowStep * 0.44f;
        float boxW = boxH * 2.8f;
        float boxX = panelX + panelW - 28f - boxW;
        float boxY = ry - boxH / 2f;
        return t.x >= boxX && t.x <= boxX + boxW && t.y >= boxY && t.y <= boxY + boxH;
    }

    private static boolean hits(Vector2 t, float x, float y, float w, float h) {
        return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h;
    }

    private Vector2 unproject() {
        Vector2 v = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(v);
        return v;
    }

    private Texture createRoundedRect(int w, int h, int r, Color color) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0); pm.fill();
        pm.setColor(color);
        pm.fillRectangle(r, 0, w - 2*r, h); pm.fillRectangle(0, r, w, h - 2*r);
        pm.fillCircle(r,   r,   r); pm.fillCircle(w-r, r,   r);
        pm.fillCircle(r,   h-r, r); pm.fillCircle(w-r, h-r, r);
        Texture t = new Texture(pm); pm.dispose(); return t;
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
        // font NOT disposed — owned by FontManager
        if (panelTexture != null) panelTexture.dispose();
    }
}
