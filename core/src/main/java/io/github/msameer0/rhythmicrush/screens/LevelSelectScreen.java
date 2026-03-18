package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.files.FileHandle;

import com.badlogic.gdx.utils.Array;

import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.game.level.LevelSerializer;
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

    /**
     * A helper class that represents a single level entry in the selection list.
     * It pairs the {@link LevelData} with its corresponding file index.
     */
    private static class LevelEntry {
        final LevelData data;
        final int index;

        LevelEntry(LevelData data, int index) {
            this.data = data;
            this.index = index;
        }
    }

    private Array<LevelEntry> levels = new Array<>();
    private int selectedLevel = 0;

    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private TextureRegion backButton, leftArrow, rightArrow;
    private TextureRegion[] difficultyTextures;

    private AnimatedButton btnBack, btnLeft, btnRight, btnPanel;

    private Texture panelTexture;
    private int lastPanelW = -1, lastPanelH = -1;

    private float panelX, panelY, panelW, panelH;

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
        updateScaledSizes();
    }

    /**
     * Scans the internal assets directory for level files and populates the level list.
     * <p>
     * This method searches for JSON files in the "levels/" directory following a numeric
     * naming convention (e.g., "0.json", "1.json"). It continues loading until a file index
     * is missing or a parsing error occurs.
     * <p>
     * If no valid levels are found, a placeholder level entry is created to prevent
     * the interface from being empty. Finally, it ensures the {@code selectedLevel}
     * index remains within the bounds of the newly populated list.
     */
    private void loadLevels() {
        levels.clear();
        int index = 0;
        while (true) {
            FileHandle fh = Gdx.files.internal("levels/" + index + ".json");
            if (!fh.exists()) break;
            try {
                LevelData data = LevelSerializer.load(fh);
                levels.add(new LevelEntry(data, index));
                index++;
            } catch (Exception e) {
                Gdx.app.error("LevelSelect", "Failed to load level " + index + ": " + e.getMessage());
                break;
            }
        }
        if (levels.size == 0) {
            LevelData placeholder = new LevelData();
            placeholder.name = "No Levels Found";
            placeholder.difficulty = "normal";
            levels.add(new LevelEntry(placeholder, -1));
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
            case "normal":
                return 1;
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

        float backW = vw * 0.08f, backH = backW;
        float leftW = vw * 0.08f, leftH = leftW;
        float rightW = vw * 0.08f, rightH = rightW;

        if (btnBack != null) btnBack.setBounds(10, vh - backH - 10, backW, backH);
        if (btnLeft != null) btnLeft.setBounds(10, vh / 2f - leftH / 2f, leftW, leftH);
        if (btnRight != null)
            btnRight.setBounds(vw - rightW - 10, vh / 2f - rightH / 2f, rightW, rightH);

        panelW = vw * 0.6f;
        panelH = vh * 0.28f;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f + 40;
        if (btnPanel != null) btnPanel.setBounds(panelX, panelY, panelW, panelH);
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
        handleInput();
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
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        LevelData current = levels.get(selectedLevel).data;

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();

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

        TextureRegion diffRegion = difficultyTexture(current.difficulty);
        float iconSize = panelH * 0.55f;
        float spacing = panelW * 0.05f;

        font.getData().setScale(0.85f);
        layout.setText(font, current.name);
        float nameW = layout.width, nameH = layout.height;
        float totalWidth = iconSize + spacing + nameW;
        float startX = panelX + (panelW - totalWidth) / 2f;
        float iconX = startX;
        float iconY = panelY + panelH / 2f - iconSize / 2f + 10f;
        float textX = iconX + iconSize + spacing;
        float textY = panelY + panelH / 2f + nameH / 2f + 10f;

        game.getBatch().draw(diffRegion, iconX, iconY, iconSize, iconSize);
        drawTextWithShadow(font, current.name, textX, textY, Color.WHITE);

        font.getData().setScale(0.38f);
        String diffLabel = current.difficulty != null
            ? current.difficulty.substring(0, 1).toUpperCase() + current.difficulty.substring(1)
            : "Normal";
        layout.setText(font, diffLabel);
        drawTextWithShadow(font, diffLabel, textX, textY - nameH - 4f, new Color(1f, 1f, 1f, 0.55f));

        String levelKey = levels.get(selectedLevel).index + ".json";
        LevelProgress progress = game.getProgressManager().getOrCreate(levelKey);

        font.getData().setScale(0.42f);
        String bestText = "Best: " + progress.bestPercent + "%";
        String attemptsText = "Total Attempts: " + progress.totalAttempts;
        float statsX = panelX + panelW / 2f;

        layout.setText(font, bestText);
        drawTextWithShadow(font, bestText, statsX - layout.width / 2f, panelY - 18f, new Color(1f, 1f, 1f, 0.8f));

        layout.setText(font, attemptsText);
        drawTextWithShadow(font, attemptsText, statsX - layout.width / 2f, panelY - 44f, new Color(1f, 1f, 1f, 0.55f));

        font.getData().setScale(0.35f);
        String counter = (selectedLevel + 1) + " / " + levels.size;
        layout.setText(font, counter);
        drawTextWithShadow(font, counter,
            viewport.getWorldWidth() / 2f - layout.width / 2f, 22f,
            new Color(1f, 1f, 1f, 0.4f));

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        game.getBatch().end();
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
        }
        if (!Gdx.input.isTouched()) {
            btnBack.onTouchUp(t.x, t.y);
            btnLeft.onTouchUp(t.x, t.y);
            btnRight.onTouchUp(t.x, t.y);
            btnPanel.onTouchUp(t.x, t.y);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) navigate(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) navigate(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) playSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) playSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MainMenuScreen(game));
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
        selectedLevel = (selectedLevel + dir + levels.size) % levels.size;
    }

    /**
     * Transitions the game to the {@link GameScreen} using the currently selected level.
     * <p>
     * This method retrieves the {@link LevelEntry} corresponding to the current
     * {@code selectedLevel} index. If the entry is valid (i.e., not a placeholder
     * with an index less than 0), it initializes a new game session with the
     * associated {@link LevelData}.
     */
    private void playSelected() {
        LevelEntry entry = levels.get(selectedLevel);
        if (entry.index < 0) return;
        game.setScreen(new GameScreen(game, entry.data, selectedLevel));
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
