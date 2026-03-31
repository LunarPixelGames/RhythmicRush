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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.utils.Array;
import java.util.Random;

import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.settings.SettingsManager;
import io.github.msameer0.rhythmicrush.ui.AnimatedButton;

/**
 * The primary entry point screen for the game, providing access to the main gameplay
 * and a comprehensive settings menu.
 *
 * <p>The screen handles:
 * <ul>
 *   <li>The main menu interface (Play and Settings buttons).</li>
 *   <li>A multi-tabbed settings overlay (Gameplay and Graphics categories).</li>
 *   <li>Interactive UI elements including toggles, sliders, and numeric input fields.</li>
 *   <li>Dynamic layout scaling to accommodate various window sizes and platforms.</li>
 *   <li>Background music management and transition to the level selection screen.</li>
 * </ul>
 *
 * @see AbstractScreen
 * @see SettingsManager
 */
public class MainMenuScreen extends AbstractScreen {

    private TextureRegion title, startButton, settingsButton, backArrow;
    private Color bgColor;

    private float titleX, titleY, titleW, titleH;

    private AnimatedButton btnPlay;
    private AnimatedButton btnSettings;

    private boolean settingsOpen = false;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private GlyphLayout layout;

    private static final int CAT_GAMEPLAY = 0;
    private static final int CAT_GRAPHICS = 1;
    private static final int CAT_COUNT = 2;
    private static final String[] CAT_NAMES = {"Gameplay", "Graphics"};

    private static final int MAX_ROWS_PER_PAGE = 5;

    private int currentCat = CAT_GAMEPLAY;
    private int currentSubPage = 0;

    private float panelX, panelY, panelW, panelH;
    private float backX, backY, backW, backH;
    private float rowStartY;
    private float arrowLeftX, arrowRightX, arrowY, arrowSize;

    private static final float PANEL_HEIGHT_FRACTION = 0.88f;

    private float rowStep;
    private float panelPadT;
    private float panelPadB;
    private float settingsFontScale;
    private float settingsHeadingScale;

    private boolean draggingSlider = false;
    private int draggingSliderRow = -1;

    private boolean fpsInputActive = false;
    private final StringBuilder fpsInputBuffer = new StringBuilder();

    private Texture panelTexture;
    private int lastPanelW = -1, lastPanelH = -1;

    private enum RowType {TOGGLE, SLIDER, INT_FIELD}

    /**
     * Represents a single interactive row within the settings menu.
     * Each row defines a specific setting's input type, its display label,
     * and a unique identifier used to map the UI element to the {@link SettingsManager}.
     */
    private static class SettingRow {
        final RowType type;
        final String label;
        final String id;

        SettingRow(RowType t, String label, String id) {
            this.type = t;
            this.label = label;
            this.id = id;
        }
    }

    private static final Color COL_OVERLAY = new Color(0f, 0f, 0f, 0.62f);
    private static final Color COL_PANEL = new Color(0.13f, 0.13f, 0.19f, 1f);
    private static final Color COL_LABEL = new Color(1f, 1f, 1f, 0.90f);
    private static final Color COL_DIM = new Color(1f, 1f, 1f, 0.45f);
    private static final Color COL_ON = new Color(0.35f, 0.85f, 0.55f, 1f);
    private static final Color COL_OFF = new Color(0.50f, 0.50f, 0.55f, 1f);
    private static final Color COL_TRACK = new Color(0.28f, 0.28f, 0.35f, 1f);
    private static final Color COL_FILL = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_THUMB = new Color(1f, 1f, 1f, 1f);
    private static final Color COL_HEADING = new Color(1f, 0.85f, 0.35f, 1f);
    private static final Color COL_TAB_ACT = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_TAB_INACT = new Color(0.35f, 0.35f, 0.45f, 1f);
    private static final Color COL_INPUT_BG = new Color(0.18f, 0.18f, 0.26f, 1f);
    private static final Color COL_INPUT_BD = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_DOT_ACT = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_DOT_INACT = new Color(0.35f, 0.35f, 0.45f, 1f);


    /**
     * Constructs a new MainMenuScreen.
     *
     * @param game The main game instance used to manage screens and shared resources.
     */
    public MainMenuScreen(RhythmicRushGame game) {
        super(game);
        game.getUpdateManager().checkForUpdate();
    }

    /**
     * Called when this screen becomes the current screen for the game.
     * <p>
     * This method initializes the UI components and resources specific to the main menu, including:
     * <ul>
     *   <li>Loading texture regions for the title and buttons from the asset manager.</li>
     *   <li>Generating a randomized background color for the session.</li>
     *   <li>Initializing rendering tools like {@link ShapeRenderer} and {@link GlyphLayout}.</li>
     *   <li>Creating {@link AnimatedButton} instances for 'Play' and 'Settings' actions.</li>
     *   <li>Managing the starting state of the menu music based on user settings.</li>
     *   <li>Calculating the initial layout positioning via {@link #updateScaledSizes()}.</li>
     * </ul>
     */
    @Override
    public void show() {
        super.show();
        title = game.getAtlasManager().getMenuAtlas().findRegion("title");
        startButton = game.getAtlasManager().getMenuAtlas().findRegion("start_button");
        settingsButton = game.getAtlasManager().getMenuAtlas().findRegion("settings_button");
        backArrow = game.getAtlasManager().getLevelSelectAtlas().findRegion("back");

        Random rand = new Random();
        bgColor = new Color(
            0.2f + 0.6f * rand.nextFloat(),
            0.2f + 0.6f * rand.nextFloat(),
            0.2f + 0.6f * rand.nextFloat(), 1f);

        shapes = new ShapeRenderer();
        font = game.getFontManager().get(FontManager.SIZE_LARGE);
        layout = new GlyphLayout();

        btnPlay = new AnimatedButton(startButton, 0, 0, 0, 0, () -> game.setScreen(new LevelSelectScreen(game)));
        btnSettings = new AnimatedButton(settingsButton, 0, 0, 0, 0, () -> settingsOpen = true);

        if (game.getSettingsManager().getMenuMusicEnabled())
            game.getSoundManager().playMenuMusic();
        else
            game.getSoundManager().stopMenuMusic();

        updateScaledSizes();

        if (game.getAdController() != null) {
            boolean shouldShowBanner = MathUtils.randomBoolean(0.5f);
            game.getAdController().showBannerAd(shouldShowBanner);
        }
    }

    @Override
    public void hide() {
        if (game.getAdController() != null) {
            game.getAdController().showBannerAd(false);
        }
        super.hide();
    }


    /**
     * Constructs a complete list of all available setting rows for a specific category.
     * This method defines the UI structure of the settings menu, including the labels,
     * input types (toggles, sliders, or input fields), and the internal identifiers
     * used to sync with the {@link SettingsManager}.
     *
     * <p>The method also handles platform-specific logic, such as hiding desktop-only
     * options (like VSync and FPS capping) when running on mobile devices, and
     * dynamically adds conditional rows (like the FPS limit field) based on current
     * toggle states.</p>
     *
     * @param cat The category index (e.g., {@code CAT_GAMEPLAY} or {@code CAT_GRAPHICS}).
     * @return A list of {@link SettingRow} objects representing all settings in the category.
     */
    private Array<SettingRow> buildAllRows(int cat) {
        SettingsManager s = game.getSettingsManager();
        boolean desktop = Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop;
        Array<SettingRow> rows = new Array<>();

        if (cat == CAT_GAMEPLAY) {
            rows.add(new SettingRow(RowType.TOGGLE, "Menu Music", "menuMusic"));
            rows.add(new SettingRow(RowType.SLIDER, "Music Volume", "volume"));
            rows.add(new SettingRow(RowType.TOGGLE, "Show Hitboxes", "hitboxes"));
            rows.add(new SettingRow(RowType.TOGGLE, "Show Hitboxes on Death", "hitboxesDeath"));
            rows.add(new SettingRow(RowType.TOGGLE, "Show Percentage", "showPercentage"));
            rows.add(new SettingRow(RowType.TOGGLE, "Show Progress Bar", "showProgressBar"));
            rows.add(new SettingRow(RowType.TOGGLE, "Show Attempts", "showAttempts"));
            rows.add(new SettingRow(RowType.TOGGLE, "Show Best", "showBest"));
            rows.add(new SettingRow(RowType.SLIDER, "Practice Buttons Opacity", "practiceOpacity"));
            if (desktop)
                rows.add(new SettingRow(RowType.TOGGLE, "Lock Cursor in Game", "lockCursor"));
        } else {
            rows.add(new SettingRow(RowType.TOGGLE, "Show FPS", "showFps"));
            if (desktop) {
                rows.add(new SettingRow(RowType.TOGGLE, "Cap FPS", "capFps"));
                if (s.getCapFps())
                    rows.add(new SettingRow(RowType.INT_FIELD, "FPS Limit", "fpsValue"));
            }
            if (desktop)
                rows.add(new SettingRow(RowType.TOGGLE, "VSync", "vsync"));
            rows.add(new SettingRow(RowType.SLIDER, "UI Padding", "uiPadding"));
        }
        return rows;
    }

    /**
     * Retrieves the subset of setting rows to be displayed on a specific sub-page
     * within a given category.
     *
     * <p>This method implements the pagination logic for the settings menu,
     * ensuring that only a manageable number of interactive elements (defined by
     * {@code MAX_ROWS_PER_PAGE}) are processed and rendered at a time.</p>
     *
     * @param cat     The category index to fetch rows from (e.g., {@code CAT_GAMEPLAY}).
     * @param subPage The zero-based index of the sub-page within the category.
     * @return A list of {@link SettingRow} objects for the specified page,
     *         or an empty list if the page index is out of bounds.
     */
    private Array<SettingRow> getPageRows(int cat, int subPage) {
        Array<SettingRow> all = buildAllRows(cat);
        int start = subPage * MAX_ROWS_PER_PAGE;
        int end = Math.min(start + MAX_ROWS_PER_PAGE, all.size);
        if (start >= all.size) return new Array<>();
        Array<SettingRow> pageRows = new Array<>();
        for (int i = start; i < end; i++) {
            pageRows.add(all.get(i));
        }
        return pageRows;
    }

    /**
     * Calculates the total number of sub-pages required to display all settings
     * within a specific category.
     *
     * <p>The result is based on the total number of rows generated by
     * {@link #buildAllRows(int)} divided by the {@code MAX_ROWS_PER_PAGE} constant.
     * It ensures at least one page is always returned, even if a category is empty.</p>
     *
     * @param cat The category index (e.g., {@code CAT_GAMEPLAY} or {@code CAT_GRAPHICS}).
     * @return The total number of sub-pages available for the given category.
     */
    private int subPageCount(int cat) {
        int total = buildAllRows(cat).size;
        return Math.max(1, (int) Math.ceil((double) total / MAX_ROWS_PER_PAGE));
    }


    /**
     * Calculates and updates the positions, dimensions, and scales of all UI elements
     * based on the current viewport dimensions.
     *
     * <p>This method performs the following layout logic:
     * <ul>
     *   <li>Scales and centers the game title at the top of the screen.</li>
     *   <li>Positions the 'Play' (Start) button in the center-middle area, ensuring it doesn't
     *       overlap with the title.</li>
     *   <li>Places the 'Settings' button at a fixed offset from the bottom-left corner.</li>
     *   <li>Determines the dimensions for the settings panel, including internal padding,
     *       row height (step), and font scaling based on the available screen height.</li>
     * </ul>
     *
     * <p>Finally, it triggers {@link #recomputePanelHeight()} to align the settings
     * sub-components with the newly calculated global scales.</p>
     */
    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        float maxTitleWidth = vw * 0.85f;
        float titleScale = (maxTitleWidth / title.getRegionWidth()) * 0.675f;
        float maxTitleHeight = vh * 0.36f;
        if (title.getRegionHeight() * titleScale > maxTitleHeight)
            titleScale = maxTitleHeight / title.getRegionHeight();
        titleW = title.getRegionWidth() * titleScale;
        titleH = title.getRegionHeight() * titleScale;
        titleX = vw / 2f - titleW / 2f;
        titleY = vh - titleH - vh * 0.03f;

        float maxStartW = vw * 0.25f * 0.55f;
        float startScale = maxStartW / startButton.getRegionWidth();
        float startW = startButton.getRegionWidth() * startScale;
        float startH = startButton.getRegionHeight() * startScale;
        float startX = vw / 2f - startW / 2f;
        float minY = titleY - startH - vh * 0.06f;
        float midY = vh / 2f - startH / 2f;
        float startY = Math.min(midY, minY);
        if (btnPlay != null) btnPlay.setBounds(startX, startY, startW, startH);

        float maxSettingsW = vw * 0.1f * 0.85f;
        float settingsScale = maxSettingsW / settingsButton.getRegionWidth();
        float settingsW = settingsButton.getRegionWidth() * settingsScale;
        float settingsH = settingsButton.getRegionHeight() * settingsScale;
        float settingsX = 20;
        float settingsY = 20 - 10f;
        if (btnSettings != null) btnSettings.setBounds(settingsX, settingsY, settingsW, settingsH);

        panelW = Math.min(vw * 0.72f, 740f);

        float targetH = vh * PANEL_HEIGHT_FRACTION;
        float dotsExtra = targetH * 0.045f;
        float padT = targetH * 0.20f;
        float padB = targetH * 0.035f;
        float rowsH = targetH - padT - padB - dotsExtra;
        rowStep = rowsH / MAX_ROWS_PER_PAGE;
        panelPadT = padT;
        panelPadB = padB;

        float scaleRef = rowStep / 68f;
        settingsFontScale = 0.65f * scaleRef;
        settingsHeadingScale = scaleRef;

        recomputePanelHeight();
    }

    /**
     * Calculates the layout and positioning for the settings panel and its internal sub-elements.
     *
     * <p>This method determines the exact pixel coordinates and dimensions for the settings UI,
     * ensuring it remains centered and properly scaled. It handles:
     * <ul>
     *   <li>Ensuring the current sub-page index remains within valid bounds for the category.</li>
     *   <li>Calculating the final panel height based on the number of rows and padding.</li>
     *   <li>Positioning the back button and navigation arrows relative to the panel edges.</li>
     *   <li>Calculating the starting vertical position for settings rows.</li>
     *   <li>Invalidating the background panel texture cache (lastPanelW) to force a redraw.</li>
     * </ul>
     */
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

        arrowSize = backH;
        arrowY = panelY + panelH - arrowSize - 12f;
        arrowLeftX = panelX + backW + 20f;
        arrowRightX = panelX + panelW - arrowSize - 12f;

        lastPanelW = -1;
    }


    /**
     * Updates the logic for the main menu screen once per frame.
     *
     * <p>This method performs the following actions:
     * <ul>
     *   <li>Updates the animation states of the 'Play' and 'Settings' buttons if the
     *       settings menu is not currently displayed.</li>
     *   <li>Delegates input handling to {@link #handleSettingsInput()} if the settings
     *       overlay is open.</li>
     *   <li>Delegates input handling to {@link #handleMenuInput()} if the main menu
     *       is active.</li>
     * </ul>
     *
     * @param delta The time in seconds since the last render frame.
     */
    @Override
    protected void update(float delta) {
        if (!settingsOpen) {
            btnPlay.update(delta);
            btnSettings.update(delta);
        }
        if (settingsOpen) handleSettingsInput();
        else handleMenuInput();
    }


    /**
     * Renders the visual components of the screen, including the background
     */
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
            panelTexture = createRoundedRect(texW, texH, (int) (26f * (panelW / 740f)), COL_PANEL);
            lastPanelW = texW;
            lastPanelH = texH;
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


    /**
     * Renders the settings menu header, category tabs, and navigation arrows.
     *
     * <p>This method handles the visual layout of the top portion of the settings panel,
     * including:
     * <ul>
     *   <li>The "Settings" title text centered at the top.</li>
     *   <li>The interactive category tabs (e.g., Gameplay, Graphics) with dynamic
     *       coloring and a sliding underline to indicate the currently active category.</li>
     *   <li>Left and right navigation arrows used for switching between sub-pages
     *       or categories.</li>
     * </ul>
     *
     * <p>The method manages its own {@code SpriteBatch} and {@code ShapeRenderer} sessions,
     * ensuring proper blending for UI transparency.</p>
     */
    private void drawHeadingAndTabs() {
        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        font.getData().setScale(settingsHeadingScale);
        layout.setText(font, "Settings");
        drawTextWithShadow(
            font,
            "Settings",
            (panelX + panelW / 2f) - (layout.width / 2f),
            panelY + panelH - 16f,
            COL_HEADING
        );

        float tabFontScale = settingsFontScale * 0.92f;
        font.getData().setScale(tabFontScale);
        float tabY = panelY + panelH - panelPadT * 0.47f;
        layout.setText(font, CAT_NAMES[0]);
        float tabTextH = layout.height;
        for (int i = 0; i < CAT_COUNT; i++) {
            float tabW = panelW / CAT_COUNT;
            float tabCX = panelX + tabW * i + tabW / 2f;
            Color tabColor = (i == currentCat ? COL_TAB_ACT : COL_TAB_INACT);
            layout.setText(font, CAT_NAMES[i]);

            drawTextWithShadow(
                font,
                CAT_NAMES[i],
                tabCX - layout.width / 2f,
                tabY,
                tabColor
            );
        }
        font.getData().setScale(1f);
        game.getBatch().end();

        float tabW = panelW / CAT_COUNT;
        float tabCX = panelX + tabW * currentCat;
        float underlineY = tabY - tabTextH - 3f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_TAB_ACT);
        shapes.rect(tabCX + 8f, underlineY, tabW - 16f, 3f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        drawArrow(arrowLeftX, arrowY, arrowSize, true);
        drawArrow(arrowRightX, arrowY, arrowSize, false);
    }

    /**
     * Renders a directional triangle (arrow) used for menu navigation.
     * <p>
     * The arrow is drawn using a {@link ShapeRenderer} in filled mode with alpha blending
     * enabled to support transparency. The orientation of the triangle is determined by
     * the {@code pointLeft} parameter.
     * </p>
     *
     * @param x         The x-coordinate of the bottom-left corner of the arrow's bounding square.
     * @param y         The y-coordinate of the bottom-left corner of the arrow's bounding square.
     * @param size      The width and height of the bounding square used to calculate the arrow's scale.
     * @param pointLeft {@code true} to point the arrow to the left, {@code false} to point it to the right.
     */
    private void drawArrow(float x, float y, float size, boolean pointLeft) {
        float cx = x + size / 2f, cy = y + size / 2f, hs = size * 0.28f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_DIM);
        if (pointLeft) shapes.triangle(cx + hs, cy + hs, cx + hs, cy - hs, cx - hs, cy);
        else shapes.triangle(cx - hs, cy + hs, cx - hs, cy - hs, cx + hs, cy);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Renders the pagination indicators (dots) at the bottom of the settings panel.
     *
     * <p>This method calculates the centered horizontal position for a series of dots
     * representing the total number of sub-pages in the current category. It highlights
     * the dot corresponding to the {@code currentSubPage} index using {@code COL_DOT_ACT},
     * while others are drawn with {@code COL_DOT_INACT}.</p>
     *
     * <p>The dots are only rendered if there is more than one sub-page available.
     * Alpha blending is used to ensure the UI elements respect transparency settings.</p>
     */
    private void drawSubPageDots() {
        int total = subPageCount(currentCat);
        if (total <= 1) return;
        float dotR = 5f;
        float dotGap = dotR * 2f + 6f;
        float totalW = total * dotGap - 6f;
        float startX = panelX + panelW / 2f - totalW / 2f + dotR;
        float dotY = panelY + panelPadB / 2f + dotR;

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


    /**
     * Iterates through a list of setting rows and delegates the rendering of each row
     * to the appropriate drawing method based on its {@link RowType}.
     *
     * <p>This method acts as the primary dispatcher for the settings UI, handling:
     * <ul>
     *   <li>{@link RowType#TOGGLE}: Renders an on/off switch using current values from {@link SettingsManager}.</li>
     *   <li>{@link RowType#SLIDER}: Renders a volume slider specifically for music volume.</li>
     *   <li>{@link RowType#INT_FIELD}: Renders a numeric input field specifically for the FPS cap.</li>
     * </ul>
     *
     * <p>Vertical positioning for each row is calculated dynamically via {@link #rowY(int)}
     * to ensure consistent spacing regardless of the number of items on the page.</p>
     *
     * @param rows The list of {@link SettingRow} objects to be rendered on the current screen.
     */
    private void drawSettingsRows(Array<SettingRow> rows) {
        SettingsManager s = game.getSettingsManager();
        for (int i = 0; i < rows.size; i++) {
            SettingRow row = rows.get(i);
            float ry = rowY(i);
            switch (row.type) {
                case TOGGLE:
                    drawToggleRow(ry, row.label, getToggleValue(row.id, s));
                    break;
                case SLIDER:
                    float val;
                    if ("uiPadding".equals(row.id)) val = s.getUiPadding() / 50f;
                    else if ("practiceOpacity".equals(row.id)) val = s.getPracticeButtonOpacity();
                    else val = s.getMusicVolume();
                    drawSliderRow(ry, row.label, val);
                    break;
                case INT_FIELD:
                    drawIntFieldRow(ry, row.label, s.getFpsCapValue());
                    break;
            }
        }
    }

    /**
     * Retrieves the current boolean state of a specific toggle setting based on its identifier.
     * <p>
     * This helper method maps the unique string {@code id} of a {@link SettingRow} to the
     * corresponding field within the {@link SettingsManager}. It is primarily used during
     * the rendering phase to determine the visual state (ON/OFF) of toggle switches.
     * </p>
     *
     * @param id The unique string identifier for the setting (e.g., "menuMusic", "vsync").
     * @param s  The {@link SettingsManager} instance containing the current configuration.
     * @return The current boolean value of the identified setting, or {@code false} if
     *         the identifier is not recognized.
     */
    private boolean getToggleValue(String id, SettingsManager s) {
        switch (id) {
            case "menuMusic":
                return s.getMenuMusicEnabled();
            case "hitboxes":
                return s.getShowHitboxes();
            case "hitboxesDeath":
                return s.getShowHitboxesOnDeath();
            case "lockCursor":
                return s.getLockCursorInGame();
            case "showFps":
                return s.getShowFps();
            case "capFps":
                return s.getCapFps();
            case "vsync":
                return s.getEnableVsync();
            case "showPercentage":
                return s.getShowPercentage();
            case "showProgressBar":
                return s.getShowProgressBar();
            case "showAttempts":
                return s.getShowAttempts();
            case "showBest":
                return s.getShowBest();
            default:
                return false;
        }
    }


    /**
     * Renders a single row containing a toggle switch (pill-shaped) for boolean settings.
     *
     * <p>The row consists of a descriptive label on the left and an interactive toggle
     * switch on the right. The switch changes color and moves its thumb position based
     * on the {@code value} (e.g., green for ON, gray for OFF). It also displays a small
     * text hint (ON/OFF) inside the toggle pill.</p>
     *
     * @param ry    The vertical center-line coordinate where the row should be drawn.
     * @param label The text description of the setting to display.
     * @param value The current state of the setting; {@code true} for ON, {@code false} for OFF.
     */
    private void drawToggleRow(float ry, String label, boolean value) {
        float rightEdge = panelX + panelW - 28f;
        float pillH = rowStep * 0.38f;
        float pillW = pillH * 1.92f;
        float pillX = rightEdge - pillW;
        float pillY = ry - pillH / 2f;
        float r = pillH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(value ? COL_ON : COL_OFF);
        shapes.circle(pillX + r, pillY + r, r, 24);
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
        layout.setText(font, label);
        drawTextWithShadow(
            font,
            label,
            panelX + 28f,
            ry + layout.height / 2f,
            COL_LABEL
        );
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    /**
     * Renders a single row containing a slider control, typically used for volume settings.
     *
     * <p>The slider consists of:
     * <ul>
     *   <li>A descriptive label on the left side of the panel.</li>
     *   <li>A horizontal track on the right side, with a filled portion representing
     *       the current value.</li>
     *   <li>A circular thumb (handle) that indicates the current position.</li>
     *   <li>A percentage text label positioned to the left of the slider track.</li>
     * </ul>
     *
     * <p>The method uses {@link ShapeRenderer} for the geometry and {@code SpriteBatch}
     * for the text, incorporating alpha blending for smooth UI transparency.</p>
     *
     * @param ry    The vertical center-line coordinate where the row should be drawn.
     * @param label The text description of the setting to display.
     * @param value A normalized float between 0.0f and 1.0f representing the slider's position.
     */
    private void drawSliderRow(float ry, String label, float value) {
        float rightEdge = panelX + panelW - 28f;
        float trackW = panelW * 0.36f;
        float trackH = rowStep * 0.07f;
        float trackX = rightEdge - trackW;
        float trackY = ry - trackH / 2f;
        float thumbR = rowStep * 0.16f;
        float fillW = trackW * value;
        float thumbCX = trackX + fillW;

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
        drawTextWithShadow(font, label, panelX + 28f, ry + layout.height / 2f, COL_LABEL);
        font.getData().setScale(settingsFontScale * 0.77f);
        font.setColor(COL_DIM);
        String pct = Math.round(value * 100f) + "%";
        layout.setText(font, pct);
        drawTextWithShadow(
            font,
            pct,
            trackX - layout.width - 12f,
            ry + layout.height / 2f,
            COL_DIM
        );
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    /**
     * Renders a single row containing a numeric input field, specifically used for integer settings
     * like the FPS limit.
     *
     * <p>This method draws a rectangular input box on the right side of the panel. When the field
     * is active (focused), it displays a blinking cursor and the current contents of the
     * {@code fpsInputBuffer}. When inactive, it displays the current persisted value.</p>
     *
     * <p>The visual state changes based on {@code fpsInputActive} to provide feedback to the user,
     * dimming the border and text when the field is not being edited.</p>
     *
     * @param ry    The vertical center-line coordinate where the row should be drawn.
     * @param label The text description of the setting to display.
     * @param value The current integer value to display when the field is not being edited.
     */
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
        drawTextWithShadow(font, label, panelX + 28f, ry + layout.height / 2f, COL_LABEL);
        font.getData().setScale(settingsFontScale * 0.95f);
        font.setColor(fpsInputActive ? Color.WHITE : COL_DIM);
        Color valueColor = fpsInputActive ? Color.WHITE : COL_DIM;
        layout.setText(font, display);

        drawTextWithShadow(
            font,
            display,
            boxX + boxW / 2f - layout.width / 2f,
            ry + layout.height / 2f,
            valueColor
        );
        font.getData().setScale(1f);
        game.getBatch().end();
    }


    /**
     * Processes input specifically for the main menu interface when the settings overlay is closed.
     *
     * <p>This method handles:
     * <ul>
     *   <li>Keyboard shortcuts: SPACE to transition to the level selection screen and
     *       ESCAPE to exit the application.</li>
     *   <li>Touch/Mouse coordinates: Translates screen coordinates to world coordinates
     *       using {@link #unproject()}.</li>
     *   <li>Button interactions: Dispatches touch down and touch up events to the
     *       'Play' and 'Settings' {@link AnimatedButton} instances.</li>
     * </ul>
     */
    private void handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            game.setScreen(new LevelSelectScreen(game));
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && System.getProperty("devMode") != null)
            game.setScreen(new LevelEditorScreen(game));
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

    /**
     * Handles all user input for the settings overlay, including keyboard shortcuts,
     * mouse interactions, and platform-specific text input.
     *
     * <p>This method manages the following interaction logic:
     * <ul>
     *   <li><b>Keyboard Navigation:</b> Processes ESCAPE to close or confirm input, ENTER to
     *       save numeric values, and Arrow keys for category/page navigation.</li>
     *   <li><b>Numeric Input:</b> Handles direct keyboard buffering for the FPS limit field
     *       on desktop, or triggers the OS-native text input dialog on mobile.</li>
     *   <li><b>Slider Interaction:</b> Manages the dragging state of the music volume slider,
     *       calculating normalized values based on the mouse/touch X-coordinate.</li>
     *   <li><b>Tab & Page Navigation:</b> Detects hits on category tabs at the top or
     *       navigation arrows to switch sub-pages.</li>
     *   <li><b>Control Toggles:</b> Detects hits on pill-shaped toggle switches to flip
     *       boolean settings via {@link #handleToggle(String, SettingsManager)}.</li>
     * </ul>
     *
     * <p>All changes are persisted immediately to the {@link SettingsManager} and
     * applied to the game state where necessary (e.g., volume updates or FPS capping).</p>
     */
    private void handleSettingsInput() {
        SettingsManager s = game.getSettingsManager();

        if (fpsInputActive) {
            for (int k = Input.Keys.NUM_0; k <= Input.Keys.NUM_9; k++)
                if (Gdx.input.isKeyJustPressed(k))
                    fpsInputBuffer.append((char) ('0' + (k - Input.Keys.NUM_0)));
            for (int k = Input.Keys.NUMPAD_0; k <= Input.Keys.NUMPAD_9; k++)
                if (Gdx.input.isKeyJustPressed(k))
                    fpsInputBuffer.append((char) ('0' + (k - Input.Keys.NUMPAD_0)));
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && fpsInputBuffer.length() > 0)
                fpsInputBuffer.deleteCharAt(fpsInputBuffer.length() - 1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) confirmFpsInput(s);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (fpsInputActive) confirmFpsInput(s);
            else closeSettings();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) navigate(-1, s);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) navigate(1, s);

        if (Gdx.input.isTouched() && draggingSlider) {
            float tx = unproject().x;
            float trackX = panelX + panelW - 28f - panelW * 0.36f;
            float trackW = panelW * 0.36f;
            float norm = Math.max(0f, Math.min(1f, (tx - trackX) / trackW));

            Array<SettingRow> pageRows = getPageRows(currentCat, currentSubPage);
            if (draggingSliderRow >= 0 && draggingSliderRow < pageRows.size) {
                SettingRow row = pageRows.get(draggingSliderRow);
                if ("volume".equals(row.id)) {
                    s.setMusicVolume(norm);
                    game.getSoundManager().setMusicVolume(s.getMusicVolume());
                } else if ("uiPadding".equals(row.id)) {
                    s.setUiPadding(norm * 50f);
                } else if ("practiceOpacity".equals(row.id)) {
                    s.setPracticeButtonOpacity(norm);
                }
            }
        }
        if (!Gdx.input.isTouched()) {
            if (draggingSlider) s.save();
            draggingSlider = false;
            draggingSliderRow = -1;
        }

        if (!Gdx.input.justTouched()) return;
        Vector2 t = unproject();

        Array<SettingRow> pageRows = getPageRows(currentCat, currentSubPage);

        if (fpsInputActive) {
            int fpsIdx = -1;
            for (int i = 0; i < pageRows.size; i++)
                if ("fpsValue".equals(pageRows.get(i).id)) {
                    fpsIdx = i;
                    break;
                }
            if (fpsIdx < 0 || !hitIntBox(t, rowY(fpsIdx))) confirmFpsInput(s);
        }

        if (hits(t, backX, backY, backW, backH)) {
            closeSettings();
            return;
        }

        if (hits(t, arrowLeftX, arrowY, arrowSize, arrowSize)) {
            navigate(-1, s);
            return;
        }
        if (hits(t, arrowRightX, arrowY, arrowSize, arrowSize)) {
            navigate(1, s);
            return;
        }

        for (int i = 0; i < CAT_COUNT; i++) {
            float tabW = panelW / CAT_COUNT;
            float tabX = panelX + tabW * i;
            float tabTopY = panelY + panelH - 42f;
            if (t.x >= tabX && t.x <= tabX + tabW && t.y >= tabTopY - 28f && t.y <= tabTopY + 8f) {
                if (i != currentCat) {
                    confirmFpsInput(s);
                    draggingSlider = false;
                    currentCat = i;
                    currentSubPage = 0;
                    recomputePanelHeight();
                }
                return;
            }
        }

        for (int i = 0; i < pageRows.size; i++) {
            SettingRow row = pageRows.get(i);
            float ry = rowY(i);
            switch (row.type) {
                case TOGGLE:
                    if (hitPill(t, ry)) handleToggle(row.id, s);
                    break;
                case SLIDER:
                    float val;
                    if ("uiPadding".equals(row.id)) val = s.getUiPadding() / 50f;
                    else if ("practiceOpacity".equals(row.id)) val = s.getPracticeButtonOpacity();
                    else val = s.getMusicVolume();

                    if (hitSliderThumb(t, ry, val)) {
                        draggingSlider = true;
                        draggingSliderRow = i;
                    }
                    break;
                case INT_FIELD:
                    if (hitIntBox(t, ry) && !fpsInputActive) {
                        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) {
                            fpsInputActive = true;
                            fpsInputBuffer.setLength(0);
                            fpsInputBuffer.append(s.getFpsCapValue());
                        } else {
                            Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
                                @Override
                                public void input(String text) {
                                    try {
                                        int val = Integer.parseInt(text.trim());
                                        if (val > 0) {
                                            s.setFpsCapValue(val);
                                            s.applyFpsCap();
                                            s.save();
                                        }
                                    } catch (NumberFormatException ignored) {
                                    }
                                }

                                @Override
                                public void canceled() {
                                }
                            }, "FPS Limit", String.valueOf(s.getFpsCapValue()), "Enter FPS cap");
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Handles the logic for toggling boolean-based settings and performing any
     * immediate side effects resulting from the change.
     *
     * <p>Depending on the provided {@code id}, this method will flip the state
     * of the corresponding setting in the {@link SettingsManager}, trigger
     * specific game engine updates (such as starting/stopping music, applying
     * FPS caps, or updating VSync), and persist the changes to storage.</p>
     *
     * @param id The unique string identifier for the setting to be toggled
     *           (e.g., "menuMusic", "vsync", "capFps").
     * @param s  The {@link SettingsManager} instance where the setting state is stored.
     */
    private void handleToggle(String id, SettingsManager s) {
        switch (id) {
            case "menuMusic":
                s.setMenuMusicEnabled(!s.getMenuMusicEnabled());
                if (s.getMenuMusicEnabled()) game.getSoundManager().playMenuMusic();
                else game.getSoundManager().stopMenuMusic();
                s.save();
                break;
            case "hitboxes":
                s.setShowHitboxes(!s.getShowHitboxes());
                s.save();
                break;
            case "hitboxesDeath":
                s.setShowHitboxesOnDeath(!s.getShowHitboxesOnDeath());
                s.save();
                break;
            case "lockCursor":
                s.setLockCursorInGame(!s.getLockCursorInGame());
                s.save();
                break;
            case "showFps":
                s.setShowFps(!s.getShowFps());
                s.save();
                break;
            case "showPercentage":
                s.setShowPercentage(!s.getShowPercentage());
                s.save();
                break;
            case "showProgressBar":
                s.setShowProgressBar(!s.getShowProgressBar());
                s.save();
                break;
            case "showAttempts":
                s.setShowAttempts(!s.getShowAttempts());
                s.save();
                break;
            case "showBest":
                s.setShowBest(!s.getShowBest());
                s.save();
                break;
            case "capFps":
                s.setCapFps(!s.getCapFps());
                s.applyFpsCap();
                s.save();
                if (!s.getCapFps()) fpsInputActive = false;
                recomputePanelHeight();
                break;
            case "vsync":
                s.setEnableVsync(!s.getEnableVsync());
                s.applyVsync();
                s.save();
                break;
        }
    }

    /**
     * Navigates between settings categories and sub-pages.
     *
     * <p>This method handles the logical transition when a user moves left or right
     * through the settings menu. It performs the following:
     * <ul>
     *   <li>Finalizes any pending numeric input (like FPS limits).</li>
     *   <li>Resets slider interaction states to prevent "sticky" dragging during transitions.</li>
     *   <li>Increments or decrements the sub-page index.</li>
     *   <li>If a page boundary is reached, it wraps around to the next or previous
     *       major category (e.g., jumping from Gameplay to Graphics).</li>
     *   <li>Triggers {@link #recomputePanelHeight()} to refresh the UI layout for the new page.</li>
     * </ul>
     *
     * @param dir The direction to navigate: positive for forward/right, negative for backward/left.
     * @param s   The {@link SettingsManager} instance used to confirm and save pending inputs.
     */
    private void navigate(int dir, SettingsManager s) {
        confirmFpsInput(s);
        draggingSlider = false;
        draggingSliderRow = -1;

        int newSub = currentSubPage + dir;
        int pages = subPageCount(currentCat);

        if (newSub >= 0 && newSub < pages) {
            currentSubPage = newSub;
        } else {
            currentCat = (currentCat + dir + CAT_COUNT) % CAT_COUNT;
            currentSubPage = dir > 0 ? 0 : subPageCount(currentCat) - 1;
        }
        recomputePanelHeight();
    }

    /**
     * Validates and finalizes the manual FPS numeric input.
     *
     * <p>This method is called when the user presses Enter, clicks outside the input field,
     * or navigates away from the current settings page. It attempts to parse the
     * {@code fpsInputBuffer} into a valid positive integer. If successful, it updates the
     * {@link SettingsManager#fpsCapValue}, applies the new limit to the game engine,
     * and persists the change to the settings file.</p>
     *
     * <p>Regardless of whether the input was valid, the method resets the input buffer
     * and deactivates the numeric input mode.</p>
     *
     * @param s The {@link SettingsManager} instance used to apply and save the new FPS value.
     */
    private void confirmFpsInput(SettingsManager s) {
        if (!fpsInputActive) return;
        fpsInputActive = false;
        if (fpsInputBuffer.length() > 0) {
            try {
                int val = Integer.parseInt(fpsInputBuffer.toString());
                if (val > 0) {
                    s.setFpsCapValue(val);
                    s.applyFpsCap();
                    s.save();
                }
            } catch (NumberFormatException ignored) {
            }
        }
        fpsInputBuffer.setLength(0);
    }


    /**
     * Closes the settings overlay and resets the menu state to its default values.
     *
     * <p>This method performs the following cleanup actions:
     * <ul>
     *   <li>Deactivates the settings visibility flag.</li>
     *   <li>Resets interaction states for sliders and numeric input fields to prevent
     *       input bleed when the menu is reopened.</li>
     *   <li>Clears the temporary FPS input buffer.</li>
     *   <li>Returns the navigation state to the first page of the Gameplay category.</li>
     * </ul>
     */
    private void closeSettings() {
        settingsOpen = false;
        draggingSlider = false;
        draggingSliderRow = -1;
        fpsInputActive = false;
        fpsInputBuffer.setLength(0);
        currentCat = CAT_GAMEPLAY;
        currentSubPage = 0;
    }

    /**
     * Calculates the vertical center-line coordinate for a settings row based on its index.
     *
     * @param i The zero-based index of the row on the current sub-page.
     * @return The y-coordinate used for positioning the row's label and interactive elements.
     */
    private float rowY(int i) {
        return rowStartY - i * rowStep;
    }

    /**
     * Determines if a given touch or mouse coordinate falls within the interactive bounds
     * of a toggle switch (pill).
     *
     * <p>This method calculates the hit-box based on the pill's dimensions and position
     * relative to the settings panel, including a small 4-pixel padding to improve
     * touch responsiveness.</p>
     *
     * @param t  The unprojected world coordinates of the touch or mouse cursor.
     * @param ry The vertical center-line coordinate of the settings row being checked.
     * @return {@code true} if the coordinates intersect the toggle switch area;
     *         {@code false} otherwise.
     */
    private boolean hitPill(Vector2 t, float ry) {
        float pillH = rowStep * 0.38f;
        float pillW = pillH * 1.92f;
        float pillX = panelX + panelW - 28f - pillW;
        float pillY = ry - pillH / 2f;
        return t.x >= pillX - 4f && t.x <= pillX + pillW + 4f
            && t.y >= pillY - 4f && t.y <= pillY + pillH + 4f;
    }

    /**
     * Determines if a given touch or mouse coordinate falls within the interactive bounds
     * of a slider control, including its track and thumb.
     *
     * <p>This method calculates the hit-box based on the slider's horizontal track width
     * and the radius of the circular thumb. It includes the full width of the track plus
     * the thumb radius as a buffer to ensure the slider remains responsive even when
     * the user's finger or cursor is slightly outside the visual bounds.</p>
     *
     * @param t     The unprojected world coordinates of the touch or mouse cursor.
     * @param ry    The vertical center-line coordinate of the settings row containing the slider.
     * @param value The current normalized value (0.0f to 1.0f) of the slider.
     * @return {@code true} if the coordinates intersect the slider's interactive area;
     *         {@code false} otherwise.
     */
    private boolean hitSliderThumb(Vector2 t, float ry, float value) {
        float trackW = panelW * 0.36f;
        float trackX = panelX + panelW - 28f - trackW;
        float thumbR = rowStep * 0.16f;
        float thumbCX = trackX + trackW * value;
        return t.x >= trackX - thumbR && t.x <= trackX + trackW + thumbR
            && t.y >= ry - thumbR && t.y <= ry + thumbR;
    }

    /**
     * Determines if a given touch or mouse coordinate falls within the interactive bounds
     * of a numeric input field (specifically used for the FPS limit box).
     *
     * <p>The hit-box calculation is based on the dimensions of the input rectangle
     * relative to the settings panel and its current vertical row position.</p>
     *
     * @param t  The unprojected world coordinates of the touch or mouse cursor.
     * @param ry The vertical center-line coordinate of the settings row containing the input field.
     * @return {@code true} if the coordinates intersect the input box; {@code false} otherwise.
     */
    private boolean hitIntBox(Vector2 t, float ry) {
        float boxH = rowStep * 0.44f;
        float boxW = boxH * 2.8f;
        float boxX = panelX + panelW - 28f - boxW;
        float boxY = ry - boxH / 2f;
        return t.x >= boxX && t.x <= boxX + boxW && t.y >= boxY && t.y <= boxY + boxH;
    }

    /**
     * Performs a basic 2D AABB (Axis-Aligned Bounding Box) collision check between a point
     * and a rectangular area.
     *
     * @param t The unprojected world coordinates of the point (e.g., touch or mouse cursor).
     * @param x The x-coordinate of the bottom-left corner of the rectangular area.
     * @param y The y-coordinate of the bottom-left corner of the rectangular area.
     * @param w The width of the rectangular area.
     * @param h The height of the rectangular area.
     * @return {@code true} if the point {@code t} is contained within the bounds of the
     *         defined rectangle; {@code false} otherwise.
     */
    private static boolean hits(Vector2 t, float x, float y, float w, float h) {
        return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h;
    }

    /**
     * Converts the current screen-space mouse or touch coordinates into world-space coordinates.
     *
     * <p>This method retrieves the raw X and Y positions from {@link Input}
     * and uses the screen's {@code viewport} to perform the transformation, ensuring
     * that input detection remains accurate regardless of window resizing or aspect ratio
     * scaling.</p>
     *
     * @return A {@link Vector2} containing the unprojected world coordinates of the user's input.
     */
    private Vector2 unproject() {
        Vector2 v = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(v);
        return v;
    }

    /**
     * Creates a new {@link Texture} containing a filled rounded rectangle.
     * <p>
     * This method generates a {@link Pixmap} of the specified dimensions, draws a
     * rounded rectangle using the provided color and radius by combining several
     * rectangles and circles, and then uploads the pixel data to a GPU texture.
     * </p>
     *
     * @param w     The width of the rectangle in pixels.
     * @param h     The height of the rectangle in pixels.
     * @param r     The radius of the corners in pixels.
     * @param color The {@link Color} to fill the rectangle with.
     * @return A new {@link Texture} instance containing the rounded rectangle.
     */
    private Texture createRoundedRect(int w, int h, int r, Color color) {
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


    /**
     * Updates the screen layout and viewport when the game window is resized.
     *
     * <p>This method ensures the {@code viewport} is updated to the new dimensions and
     * recalculates the positions, sizes, and scales of all UI elements (such as the
     * title, buttons, and settings panel) via {@link #updateScaledSizes()} to maintain
     * a consistent appearance across different resolutions.</p>
     *
     * @param width  The new width of the window in pixels.
     * @param height The new height of the window in pixels.
     */
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateScaledSizes();
    }

    /**
     * Releases all hardware resources associated with this screen to prevent memory leaks.
     * <p>
     * This method disposes of the {@link ShapeRenderer} used for UI drawing and the
     * dynamically generated {@link Texture} used for the settings panel background.
     * </p>
     */
    @Override
    public void dispose() {
        if (shapes != null) shapes.dispose();
        if (panelTexture != null) panelTexture.dispose();
    }
}
