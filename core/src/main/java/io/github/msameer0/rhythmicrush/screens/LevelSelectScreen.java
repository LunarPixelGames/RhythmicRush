package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.utils.Array;

import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.ui.AnimatedButton;

/**
 * The Screen that allows players to browse, view stats for, and select levels to play.
 * <p>
 * This screen handles:
 * <ul>
 *     <li>Loading level data from internal JSON files.</li>
 *     <li>Displaying level metadata such as name, difficulty, and high scores.</li>
 *     <li>Navigation between levels via on-screen buttons or keyboard arrows.</li>
 *     <li>Scaling UI elements dynamically based on viewport dimensions.</li>
 * </ul>
 *
 * @author msameer0
 */
public class LevelSelectScreen extends AbstractScreen {

    private static final float PANEL_CORNER_RADIUS = 40f;

    private final Array<LevelData> levels = new Array<>();
    private int selectedLevel;

    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private TextureRegion backButton, leftArrow, rightArrow;
    private TextureRegion[] difficultyTextures;

    private AnimatedButton btnBack, btnLeft, btnRight, btnPanel, btnPractice;

    private Texture panelTexture;
    private int lastPanelW = -1, lastPanelH = -1;

    private float panelX, panelY, panelW, panelH;

    private boolean isTransitioning = false;
    private int transitionDir = 0;
    private float transitionTime = 0f;
    private final float TRANSITION_DURATION = 0.45f;

    private int nextLevelIndex = 0;
    private float panelXStart, panelXTarget;

    /**
     * Constructs a new LevelSelectScreen starting at the first available level.
     *
     * @param game The main game instance used for screen transitions and asset management.
     */
    public LevelSelectScreen(RhythmicRushGame game) {
        this(game, 0);
    }

    /**
     * Constructs a new LevelSelectScreen starting at a specified level index.
     *
     * @param game         The main game instance used for screen transitions and asset management.
     * @param initialIndex The index of the level to be initially selected.
     */
    public LevelSelectScreen(RhythmicRushGame game, int initialIndex) {
        super(game);
        this.selectedLevel = initialIndex;
    }

    /**
     * Called when this screen becomes the current screen for the game.
     * <p>
     * This method initializes the UI components by:
     * <ul>
     *     <li>Retrieving the required fonts and texture regions from the asset managers.</li>
     *     <li>Loading the list of available levels from the internal storage.</li>
     *     <li>Instantiating animated buttons for navigation and level selection.</li>
     *     <li>Setting up the initial layout and scaling of UI elements.</li>
     * </ul>
     */
    @Override
    public void show() {
        super.show();

        font = game.getFontManager().get(FontManager.SIZE_XLARGE);

        TextureAtlas atlas = game.getAtlasManager().getLevelSelectAtlas();
        backButton = atlas.findRegion("back");
        leftArrow = atlas.findRegion("left_arrow");
        rightArrow = atlas.findRegion("right_arrow");

        difficultyTextures = new TextureRegion[]{
            atlas.findRegion("1_diff"),
            atlas.findRegion("2_diff"),
            atlas.findRegion("3_diff"),
            atlas.findRegion("4_diff"),
            atlas.findRegion("5_diff"),
        };

        loadLevels();
        btnBack = new AnimatedButton(backButton, 0, 0, 0, 0, () -> game.setScreen(new MainMenuScreen(game)));
        btnLeft = new AnimatedButton(leftArrow, 0, 0, 0, 0, () -> navigate(-1));
        btnRight = new AnimatedButton(rightArrow, 0, 0, 0, 0, () -> navigate(1));
        btnPanel = new AnimatedButton(null, 0, 0, 0, 0, this::playSelected);
        btnPractice = new AnimatedButton(null, 0, 0, 0, 0, this::playPractice);
        updateScaledSizes();
    }

    /**
     * Scans the internal assets directory for level files and populates the level list.
     * <p>
     * This method uses the {@link io.github.msameer0.rhythmicrush.game.level.LevelManager}
     * which pre-loads all levels at startup for better performance.
     * <p>
     * If no valid levels are found, a placeholder level entry is created to prevent
     * the interface from being empty. Finally, it ensures the {@code selectedLevel}
     * index remains within the bounds of the newly populated list.
     */
    private void loadLevels() {
        levels.clear();
        levels.addAll(game.getLevelManager().getLevels());

        if (levels.size == 0) {
            LevelData placeholder = new LevelData();
            placeholder.name = "No Levels Found";
            placeholder.difficulty = "normal";
            placeholder.fileName = "-1.json";
            levels.add(placeholder);
        }
        selectedLevel = Math.max(0, Math.min(selectedLevel, levels.size - 1));
    }

    /**
     * Converts a difficulty string into its corresponding numerical index.
     * <p>
     * This index is primarily used to map level difficulty strings to the
     * appropriate texture regions in the {@code difficultyTextures} array.
     * The supported difficulty levels are "easy", "normal", "hard", "insane",
     * and "extreme".
     *
     * @param difficulty The string representation of the difficulty level.
     * @return An integer index (0-4) corresponding to the difficulty,
     *         defaulting to 1 ("normal") if the input is null or unrecognized.
     */
    private int difficultyIndex(String difficulty) {
        if (difficulty == null) return 1;
        switch (difficulty.toLowerCase()) {
            case "easy":
                return 0;
            case "hard":
                return 2;
            case "insane":
                return 3;
            case "extreme":
                return 4;
            default:
                return 1;
        }
    }

    /**
     * Retrieves the visual texture associated with a specific difficulty level.
     * <p>
     * This method resolves the difficulty string to an index and returns the corresponding
     * {@link TextureRegion}. If the specific texture for the given difficulty is missing,
     * it falls back to the "normal" difficulty texture.
     *
     * @param difficulty The string representation of the difficulty level (e.g., "easy", "hard").
     * @return The {@link TextureRegion} representing the difficulty icon, or the default
     *         texture if the specified one is unavailable.
     */
    private TextureRegion difficultyTexture(String difficulty) {
        int idx = difficultyIndex(difficulty);
        TextureRegion r = difficultyTextures[idx];
        return r != null ? r : difficultyTextures[1];
    }

    /**
     * Calculates and updates the positions and dimensions of UI elements based on the
     * current viewport size.
     * <p>
     * This method ensures that navigation buttons (back, left, right) and the level
     * information panel scale proportionally with the screen resolution. It
     * re-establishes the hitboxes (bounds) for all {@link AnimatedButton} components
     * to maintain consistent relative placement and interactivity across different
     * display aspect ratios.
     */
    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        float backW = vw * 0.08f;
        float leftW = vw * 0.08f;
        float rightW = vw * 0.08f;

        if (btnBack != null) btnBack.setBounds(10, vh - backW - 10, backW, backW);
        if (btnLeft != null) btnLeft.setBounds(10, vh / 2f - leftW / 2f, leftW, leftW);
        if (btnRight != null)
            btnRight.setBounds(vw - rightW - 10, vh / 2f - rightW / 2f, rightW, rightW);

        panelW = vw * 0.6f;
        panelH = vh * 0.28f;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f + 40;
        if (btnPanel != null) btnPanel.setBounds(panelX, panelY, panelW, panelH);

        float practiceSize = vw * 0.06f;
        if (btnPractice != null)
            btnPractice.setBounds(panelX + panelW + 15, panelY, practiceSize, practiceSize);
    }

    /**
     * Updates the state of the screen and its UI components.
     * <p>
     * This method is called once per frame to:
     * <ul>
     *     <li>Update the animation and hover states for the back, navigation, and panel buttons.</li>
     *     <li>Process user input including mouse clicks, touch events, and keyboard shortcuts.</li>
     * </ul>
     *
     * @param delta The time in seconds since the last render frame.
     */
    @Override
    protected void update(float delta) {
        btnBack.update(delta);
        btnLeft.update(delta);
        btnRight.update(delta);
        btnPanel.update(delta);
        btnPractice.update(delta);
        handleInput();

        if (isTransitioning) {
            transitionTime += delta;
            float alpha = Math.min(transitionTime / TRANSITION_DURATION, 1f);
            float interp = Interpolation.swing.apply(alpha);

            // slide panel
            panelX = panelXStart + (panelXTarget - panelXStart) * interp;

            if (transitionTime >= TRANSITION_DURATION) {
                // finish transition
                selectedLevel = nextLevelIndex;
                panelX = panelXStart = panelXTarget = viewport.getWorldWidth()/2f - panelW/2f; // reset to center
                isTransitioning = false;
            }
        }
    }

    /**
     * Renders the Level Selection screen and all its graphical components.
     * <p>
     * This method is responsible for the frame-by-frame drawing of:
     * <ul>
     *     <li>The background color and clearing the screen buffer.</li>
     *     <li>The dynamic level information panel, which is lazily generated as a
     *         rounded rectangle texture if dimensions change.</li>
     *     <li>Navigation buttons (Back, Left, Right).</li>
     *     <li>Level metadata for the currently selected level, including the
     *         difficulty icon and the level name.</li>
     *     <li>Player statistics retrieved from {@link LevelProgress}, such as
     *         best completion percentage and total attempt count.</li>
     *     <li>The level counter (e.g., "1 / 5") at the bottom of the screen.</li>
     * </ul>
     * <p>
     * It manages font scaling and color shifts dynamically to create visual
     * hierarchy and resets the font state at the end of the batch to ensure
     * consistency for future draw calls.
     */
    @Override
    protected void draw() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.getBatch().begin();

        if (isTransitioning) {
            LevelData oldLevel = levels.get(selectedLevel);
            LevelData newLevel = levels.get(nextLevelIndex);

            float offset = transitionDir * viewport.getWorldWidth();

            drawLevelPanel(oldLevel, panelX, panelY);
            drawLevelPanel(newLevel, panelX + offset, panelY);
        } else {
            drawLevelPanel(levels.get(selectedLevel), panelX, panelY);
        }

        game.getBatch().end();
    }

    private void drawLevelPanel(LevelData current,  float panelX,  float panelY) {
        game.getBatch().setProjectionMatrix(camera.combined);

        int texW = (int) panelW, texH = (int) panelH;
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();
            panelTexture = createRoundedRect(texW, texH,
                (int) (PANEL_CORNER_RADIUS * (panelW / 800f)),
                new Color(0.2f, 0.2f, 0.28f, 1f));
            panelTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            lastPanelW = texW;
            lastPanelH = texH;
        }

        game.getBatch().draw(panelTexture, panelX, panelY);
        btnBack.draw(game.getBatch());
        btnLeft.draw(game.getBatch());
        btnRight.draw(game.getBatch());

        if (!isTransitioning) {
            float px = btnPractice.x;
            float py = btnPractice.y;
            float ps = btnPractice.w;

            game.getBatch().end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            ShapeRenderer shapes = new ShapeRenderer();
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.15f, 0.45f, 0.15f, 0.8f);
            drawRoundedRect(shapes, px, py, ps, ps, 10f);
            shapes.end();
            shapes.dispose();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            game.getBatch().begin();

            font.getData().setScale(0.65f);
            layout.setText(font, "P");
            drawTextWithShadow(font, "P", px + ps / 2f - layout.width / 2f, py + ps / 2f + layout.height / 2f, Color.WHITE);
        }

        TextureRegion diffRegion = difficultyTexture(current.difficulty);
        float iconSize = panelH * 0.55f;
        float spacing = panelW * 0.05f;

        font.getData().setScale(0.85f);
        layout.setText(font, current.name);
        float nameW = layout.width;
        float nameH = layout.height;

        float maxTotalWidth = panelW * 0.9f;
        float scale = 1f;
        float totalWidth = iconSize + spacing + nameW;
        if (totalWidth > maxTotalWidth) {
            scale = (maxTotalWidth - iconSize - spacing) / nameW;
        }

        font.getData().setScale(0.85f * scale);
        layout.setText(font, current.name);

        float iconX = panelX + (panelW - (iconSize + spacing + layout.width)) / 2f;
        float iconY = panelY + panelH / 2f - iconSize / 2f + 10f;
        float textX = iconX + iconSize + spacing;
        float textY = panelY + panelH / 2f + layout.height / 2f + 10f;

        game.getBatch().draw(diffRegion, iconX, iconY, iconSize, iconSize);
        drawTextWithShadow(font, current.name, textX, textY, Color.WHITE);

        font.getData().setScale(0.38f * scale); // scale label similarly
        String diffLabel = current.difficulty != null
            ? current.difficulty.substring(0, 1).toUpperCase() + current.difficulty.substring(1)
            : "Normal";
        layout.setText(font, diffLabel);
        drawTextWithShadow(font, diffLabel, textX, textY - nameH - 4f, new Color(1f, 1f, 1f, 0.55f));

        String levelKey = current.fileName != null ? current.fileName : current.name + ".json";
        LevelProgress progress = game.getProgressManager().getOrCreate(levelKey);

        font.getData().setScale(0.42f);
        String bestText = "Best: " + progress.bestPercent + "%";
        String attemptsText = "Total Attempts: " + progress.totalAttempts;
        float statsX = panelX + panelW / 2f;

        layout.setText(font, bestText);
        drawTextWithShadow(font, bestText, statsX - layout.width / 2f, panelY - 18f, new Color(1f, 1f, 1f, 0.8f));

        layout.setText(font, attemptsText);
        drawTextWithShadow(font, attemptsText, statsX - layout.width / 2f, panelY - 44f, new Color(1f, 1f, 1f, 0.55f));

        int currentLevelNum = -1;
        for (int i = 0; i < levels.size; i++) {
            if (levels.get(i) == current) {
                currentLevelNum = i + 1;
                break;
            }
        }

        font.getData().setScale(0.35f);
        String counter = (currentLevelNum > 0 ? currentLevelNum : "?") + " / " + levels.size;
        layout.setText(font, counter);
        drawTextWithShadow(font, counter,
            viewport.getWorldWidth() / 2f - layout.width / 2f, 22f,
            new Color(1f, 1f, 1f, 0.4f));

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    /**
     * Processes user input from both touch/mouse and keyboard devices.
     * <p>
     * This method performs the following actions:
     * <ul>
     *     <li>Unprojects the screen coordinates of the pointer to the game world coordinates.</li>
     *     <li>Dispatches touch events to the UI buttons (Back, Left, Right, and the Level Panel)
     *         to handle clicks and releases.</li>
     *     <li>Listens for specific keyboard presses:
     *         <ul>
     *             <li><b>Left/Right Arrows:</b> Navigate through the level list.</li>
     *             <li><b>Enter/Space:</b> Launch the currently selected level.</li>
     *             <li><b>Escape:</b> Return to the main menu.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    private void handleInput() {
        Vector2 t = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

        if (Gdx.input.justTouched()) {
            btnBack.onTouchDown(t.x, t.y);
            btnLeft.onTouchDown(t.x, t.y);
            btnRight.onTouchDown(t.x, t.y);
            btnPanel.onTouchDown(t.x, t.y);
            btnPractice.onTouchDown(t.x, t.y);
        }
        if (!Gdx.input.isTouched()) {
            btnBack.onTouchUp(t.x, t.y);
            btnLeft.onTouchUp(t.x, t.y);
            btnRight.onTouchUp(t.x, t.y);
            btnPanel.onTouchUp(t.x, t.y);
            btnPractice.onTouchUp(t.x, t.y);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) navigate(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) navigate(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) playSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) playSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) playPractice();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MainMenuScreen(game));
    }

    private void playPractice() {
        LevelData data = levels.get(selectedLevel);
        if (!"-1.json".equals(data.fileName))
            game.setScreen(new GameScreen(game, data, selectedLevel, true));
    }

    /**
     * Navigates through the list of available levels by a specified offset.
     * <p>
     * This method updates the {@code selectedLevel} index, wrapping around to the
     * beginning or end of the list if the navigation exceeds the list bounds.
     *
     * @param dir The direction and magnitude of navigation (e.g., -1 for the previous
     *            level, 1 for the next level).
     */
    private void navigate(int dir) {
        if (isTransitioning) {
            selectedLevel = nextLevelIndex;
            panelX = viewport.getWorldWidth()/2f - panelW/2f;
            isTransitioning = false;
        }

        nextLevelIndex = (selectedLevel + dir + levels.size) % levels.size;
        transitionDir = dir;

        panelXStart = panelX;
        panelXTarget = panelX - dir * viewport.getWorldWidth();

        transitionTime = 0f;
        isTransitioning = true;
    }

    /**
     * Transitions the game to the {@link GameScreen} using the level that was actually clicked.
     * <p>
     * If a transition is in progress, this method detects whether the user clicked
     * the outgoing panel or the incoming panel based on their current screen positions.
     */
    private void playSelected() {
        Vector2 t = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

        if (isTransitioning) {
            if (hits(t, panelX, panelY, panelW, panelH)) {
                LevelData data = levels.get(selectedLevel);
                if (!"-1.json".equals(data.fileName))
                    game.setScreen(new GameScreen(game, data, selectedLevel));
                return;
            }
            float nextX = panelX + transitionDir * viewport.getWorldWidth();
            if (hits(t, nextX, panelY, panelW, panelH)) {
                LevelData data = levels.get(nextLevelIndex);
                if (!"-1.json".equals(data.fileName))
                    game.setScreen(new GameScreen(game, data, nextLevelIndex));
            }
        } else {
            LevelData data = levels.get(selectedLevel);
            if (!"-1.json".equals(data.fileName))
                game.setScreen(new GameScreen(game, data, selectedLevel));
        }
    }

    private static boolean hits(Vector2 t, float x, float y, float w, float h) {
        return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h;
    }

    private void drawRoundedRect(ShapeRenderer shapes, float x, float y, float w, float h, float r) {
        shapes.rect(x + r, y, w - 2 * r, h);
        shapes.rect(x, y + r, r, h - 2 * r);
        shapes.rect(x + w - r, y + r, r, h - 2 * r);
        shapes.circle(x + r, y + r, r, 16);
        shapes.circle(x + w - r, y + r, r, 16);
        shapes.circle(x + r, y + h - r, r, 16);
        shapes.circle(x + w - r, y + h - r, r, 16);
    }

    /**
     * Creates a new {@link Texture} containing a filled rounded rectangle.
     * <p>
     * This method generates a procedural UI element by using a {@link Pixmap} to draw
     * two overlapping rectangles (forming a cross) and four circles at the corners.
     * The resulting texture is uploaded to the GPU, and the temporary pixmap is
     * disposed of to prevent memory leaks.
     *
     * @param w     The width of the rectangle in pixels.
     * @param h     The height of the rectangle in pixels.
     * @param r     The radius of the rounded corners in pixels.
     * @param color The {@link Color} used to fill the shape.
     * @return A {@link Texture} object representing the rounded rectangle.
     */
    private Texture createRoundedRect(int w, int h, int r, Color color) {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setColor(color);
        pixmap.fillRectangle(r, 0, w - 2 * r, h);
        pixmap.fillRectangle(0, r, w, h - 2 * r);
        pixmap.fillCircle(r, r, r);
        pixmap.fillCircle(w - r, r, r);
        pixmap.fillCircle(r, h - r, r);
        pixmap.fillCircle(w - r, h - r, r);
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }

    /**
     * Called when the application window is resized.
     * <p>
     * This method updates the screen's {@link com.badlogic.gdx.utils.viewport.Viewport} to
     * match the new dimensions, ensuring the camera is centered. It also triggers
     * {@link #updateScaledSizes()} to recalculate the positions and scales of UI
     * elements, such as navigation buttons and the level selection panel, to maintain
     * a consistent layout across different resolutions and aspect ratios.
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
     * Cleans up resources used by this screen when it is no longer needed.
     * <p>
     * This method is responsible for disposing of the {@code panelTexture} to prevent
     * memory leaks. Other assets such as fonts and atlases are managed by the
     * global asset managers and do not need to be disposed of here.
     */
    @Override
    public void dispose() {
        if (panelTexture != null) panelTexture.dispose();
    }
}
