package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.settings.SettingsManager;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.engine.FixedTickEngine;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;

/**
 * The primary screen responsible for the core gameplay loop of Rhythmic Rush.
 *
 * <p>This screen manages the integration between the {@link GameWorld} (logic),
 * {@link FixedTickEngine} (physics/timing), and {@link GameRenderer} (graphics).
 * It handles real-time player input, level progression tracking, music synchronization,
 * and provides a comprehensive Heads-Up Display (HUD) including progress bars,
 * attempt counters, and pause menus.</p>
 *
 * <p>Key features include:</p>
 * <ul>
 *     <li>Automated respawn logic with a brief pause upon death.</li>
 *     <li>Music volume fading on level completion.</li>
 *     <li>Interactive pause overlay with volume sliders and level statistics.</li>
 *     <li>"New Best" notification popups for high-score progression.</li>
 *     <li>Dynamic UI scaling using {@link ExtendViewport}.</li>
 * </ul>
 */
public class GameScreen extends AbstractScreen {

    private static final float DEATH_PAUSE_DURATION = 0.75f;
    private static final float MUSIC_FADE_DURATION = 3f;

    private GameWorld world;
    private FixedTickEngine engine;
    private GameRenderer renderer;
    private BitmapFont font;
    private BitmapFont pauseFont;
    private GlyphLayout glyphLayout;
    private Music levelMusic;

    private OrthographicCamera gameCamera;
    private Viewport gameViewport;
    private LevelData levelData;
    private int levelIndex = 0;

    private boolean deathPaused = false;
    private float deathTimer = 0f;
    private float lastDelta = 0f;
    private boolean musicFading = false;
    private float musicFadeTimer = 0f;
    private boolean paused = false;
    private boolean levelCompletedState = false;

    private boolean hitboxesActive = false;

    private int sessionAttempts = 0;
    private String levelKey = null;

    private final StringBuilder _hudSb = new StringBuilder(32);

    private static final float POPUP_FADE_IN = 0.25f;
    private static final float POPUP_HOLD = 1.20f;
    private static final float POPUP_FADE_OUT = 0.45f;
    private static final float POPUP_TOTAL = POPUP_FADE_IN + POPUP_HOLD + POPUP_FADE_OUT;
    private float popupTimer = -1f;
    private int popupBestPct = 0;

    private ShapeRenderer shapes;
    private Texture panelTexture;
    private int lastPanelW = -1, lastPanelH = -1;

    private static final float PANEL_W = 520f;
    private static final float PANEL_H = 360f;
    private static final float BTN_SIZE = 72f;
    private static final float PAUSE_BTN = 44f;

    private TextureRegion resumeRegion;
    private TextureRegion backRegion;

    private static final Color COL_OVERLAY = new Color(0f, 0f, 0f, 0.65f);
    private static final Color COL_PANEL = new Color(0.11f, 0.11f, 0.17f, 1f);
    private static final Color COL_HEADING = new Color(1f, 0.85f, 0.35f, 1f);
    private static final Color COL_LABEL = new Color(1f, 1f, 1f, 0.85f);
    private static final Color COL_DIM = new Color(1f, 1f, 1f, 0.50f);
    private static final Color COL_TRACK = new Color(0.28f, 0.28f, 0.35f, 1f);
    private static final Color COL_FILL = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_THUMB = new Color(1f, 1f, 1f, 1f);
    private static final Color HUD_ATTEMPT = new Color(1f, 1f, 1f, 0.85f);
    private static final Color HUD_BEST = new Color(1f, 1f, 1f, 0.55f);
    private static final Color HUD_FPS = new Color(1f, 1f, 1f, 0.45f);

    private boolean pauseSliderDragging = false;

    private boolean ignoreInputUntilRelease = false;

    private boolean levelEndingSequence = false;
    private float levelEndTimer = 0f;
    private static final float END_DELAY_TOTAL = 2.0f;
    private static final float END_MUSIC_FADE_START = 1.0f;

    private static long lastAdTimeMillis = 0;
    private static final long AD_COOLDOWN_MS = 60000;   // <- this is 6 minutes

    /**
     * Constructs a new GameScreen, initializing the core game logic, physics engine,
     * and rendering systems for a specific level.
     *
     * @param game       The main game instance for accessing global managers and batching.
     * @param levelData  The data containing level layout, music, and metadata.
     * @param levelIndex The index of the level in the level selection list.
     */
    public GameScreen(RhythmicRushGame game, LevelData levelData, int levelIndex) {
        super(game);
        this.levelData = levelData;
        this.levelIndex = levelIndex;

        gameCamera = new OrthographicCamera();
        gameViewport = new ExtendViewport(1280, 720, gameCamera);
        gameViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        world = new GameWorld();
        engine = new FixedTickEngine(world);
        renderer = new GameRenderer(world, gameCamera, game.getBatch(), game.getAtlasManager());
        font = game.getFontManager().get(FontManager.SIZE_SMALL);
        pauseFont = game.getFontManager().get(FontManager.SIZE_SMALL);
        glyphLayout = new GlyphLayout();
        shapes = new ShapeRenderer();

        resumeRegion = game.getAtlasManager().getMenuAtlas().findRegion("start_button");
        backRegion = game.getAtlasManager().getLevelSelectAtlas().findRegion("back");

        if (levelData != null) {
            world.loadLevel(levelData);
            levelKey = levelData.fileName != null ? levelData.fileName : levelData.name + ".json";
            recordAttempt();
        }

        hitboxesActive = game.getSettingsManager().showHitboxes;
    }


    /**
     * Gets the current horizontal center position (X-coordinate) of the game camera.
     *
     * @return The x-coordinate of the camera's center in world units.
     */
    private float camCX() {
        return gameCamera.position.x;
    }

    /**
     * Gets the current y-coordinate of the game camera's center position.
     *
     * @return The vertical center of the camera in world units.
     */
    private float camCY() {
        return gameCamera.position.y;
    }

    /**
     * Calculates the X-coordinate of the left edge of the current camera view in world units.
     *
     * @return The world X-coordinate of the left boundary of the viewport.
     */
    private float camLeft() {
        return gameCamera.position.x - gameViewport.getWorldWidth() / 2f;
    }

    /**
     * Calculates the Y-coordinate of the bottom edge of the current camera view in world units.
     *
     * @return The world Y-coordinate of the bottom boundary of the viewport.
     */
    private float camBot() {
        return gameCamera.position.y - gameViewport.getWorldHeight() / 2f;
    }

    /**
     * Calculates the X-coordinate of the right edge of the current camera view in world units.
     *
     * @return The world X-coordinate of the right boundary of the viewport.
     */
    private float camRight() {
        return gameCamera.position.x + gameViewport.getWorldWidth() / 2f;
    }

    /**
     * Calculates the Y-coordinate of the top edge of the current camera view in world units.
     *
     * @return The world Y-coordinate of the top boundary of the viewport.
     */
    private float camTop() {
        return gameCamera.position.y + gameViewport.getWorldHeight() / 2f;
    }

    /**
     * Calculates the starting X-coordinate for the volume slider track within the pause menu.
     *
     * @return The world X-coordinate where the slider track begins.
     */
    private float pauseSliderTrackX() {
        return panelX() + PANEL_W * 0.18f;
    }

    /**
     * Calculates the width of the volume slider track within the pause menu.
     *
     * @return The horizontal width of the slider track in world units.
     */
    private float pauseSliderTrackW() {
        return PANEL_W * 0.64f;
    }

    /**
     * Calculates the vertical Y-coordinate for the volume slider within the pause menu.
     *
     * @return The world Y-coordinate where the center of the slider track is positioned.
     */
    private float pauseSliderY() {
        return panelY() + BTN_SIZE + 38f;
    }

    /**
     * Checks if a given world coordinate point overlaps with the pause menu's volume slider.
     * <p>
     * This method includes a small padding (16 units) around the visual track to make
     * the touch target easier for the player to hit.
     * </p>
     *
     * @param t The world coordinates of the touch or mouse click.
     * @return true if the coordinates are within the slider's interaction bounds, false otherwise.
     */
    private boolean hitsPauseSlider(Vector2 t) {
        float tx = pauseSliderTrackX(), tw = pauseSliderTrackW();
        float ty = pauseSliderY();
        return t.x >= tx - 16f && t.x <= tx + tw + 16f
            && t.y >= ty - 16f && t.y <= ty + 16f;
    }

    /**
     * Calculates the horizontal X-coordinate for the left edge of the pause menu panel.
     * This centers the panel horizontally relative to the current camera position.
     *
     * @return The world X-coordinate of the panel's left boundary.
     */
    private float panelX() {
        return camCX() - PANEL_W / 2f;
    }

    /**
     * Calculates the vertical Y-coordinate for the bottom edge of the pause menu panel.
     * This centers the panel vertically relative to the current camera position.
     *
     * @return The world Y-coordinate of the panel's bottom boundary.
     */
    private float panelY() {
        return camCY() - PANEL_H / 2f;
    }

    /**
     * Calculates the horizontal X-coordinate for the "Resume" button within the pause menu.
     * This positions the button to the right of the menu's center line.
     *
     * @return The world X-coordinate where the resume button should be drawn.
     */
    private float resumeX() {
        return camCX() + 16f;
    }

    /**
     * Calculates the vertical Y-coordinate for the "Resume" button within the pause menu.
     * This positions the button near the bottom edge of the pause panel.
     *
     * @return The world Y-coordinate where the resume button should be drawn.
     */
    private float resumeY() {
        return panelY() + 20f;
    }

    /**
     * Calculates the horizontal X-coordinate for the "Back" button within the pause menu.
     * This positions the button to the left of the menu's center line.
     *
     * @return The world X-coordinate where the back button should be drawn.
     */
    private float backX() {
        return camCX() - BTN_SIZE - 16f;
    }

    /**
     * Calculates the vertical Y-coordinate for the "Back" button within the pause menu.
     * This positions the button near the bottom edge of the pause panel.
     *
     * @return The world Y-coordinate where the back button should be drawn.
     */
    private float backY() {
        return panelY() + 20f;
    }

    /**
     * Calculates the horizontal X-coordinate for the center of the pause button circle.
     * This positions the button at a fixed offset from the right edge of the screen.
     *
     * @return The world X-coordinate for the center of the pause button.
     */
    private float pauseCircleCX() {
        return camRight() - PAUSE_BTN / 2f - 14f;
    }

    /**
     * Calculates the vertical Y-coordinate for the center of the pause button circle.
     * This positions the button at a fixed offset from the top edge of the screen.
     *
     * @return The world Y-coordinate for the center of the pause button.
     */
    private float pauseCircleCY() {
        return camTop() - PAUSE_BTN / 2f - 14f;
    }


    /**
     * Called when this screen becomes the current screen for the game.
     * <p>
     * This implementation handles the transition from menu to gameplay by stopping
     * the menu music, initializing the level's music track, and optionally locking
     * the hardware cursor to the game window based on the user's settings.
     * </p>
     */
    @Override
    public void show() {
        game.getSoundManager().stopMenuMusic();
        startMusic();
        if (game.getSettingsManager().lockCursorInGame)
            Gdx.input.setCursorCatched(true);
    }

    /**
     * Handles the resizing of the game window.
     * <p>
     * Updates the game viewport to match the new dimensions and ensures the camera
     * is correctly re-centered. It also invalidates the cached pause menu panel texture
     * by resetting {@code lastPanelW}, forcing it to be regenerated with the appropriate
     * dimensions during the next frame.
     * </p>
     *
     * @param width  The new width of the window in pixels.
     * @param height The new height of the window in pixels.
     */
    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        lastPanelW = -1;
    }

    /**
     * Called when this screen is no longer the current screen for the game.
     * <p>
     * This implementation ensures that the hardware cursor is released (visible and
     * unconstrained) so that it can be used freely in other screens, such as menus
     * or the level selector.
     * </p>
     */
    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
    }

    /**
     * Releases all resources used by the {@code GameScreen} to prevent memory leaks.
     * <p>
     * This includes disposing of the renderer, shape drawing utilities, cached UI textures,
     * and the active music track. It also ensures the hardware cursor is released
     * before exiting the screen.
     * </p>
     */
    @Override
    public void dispose() {
        super.dispose();
        renderer.dispose();
        shapes.dispose();
        if (panelTexture != null) panelTexture.dispose();
        stopAndDisposeMusic();
        Gdx.input.setCursorCatched(false);
    }


    /**
     * Updates the game state, handles input, and manages screen transitions.
     * <p>
     * This method serves as the main logic entry point for the screen. It performs the following:
     * <ul>
     *     <li>Processes global input (Esc to pause, R to respawn).</li>
     *     <li>Updates state timers for death pauses and "New Best" popups.</li>
     *     <li>Handles automatic respawn logic once the death delay concludes.</li>
     *     <li>Manages music volume fading during level completion or exit transitions.</li>
     *     <li>Captures player movement input and advances the {@link FixedTickEngine}.</li>
     *     <li>Checks the {@link GameWorld} for death or completion conditions to trigger appropriate reactions.</li>
     * </ul>
     * </p>
     *
     * @param delta The time in seconds since the last frame.
     */
    @Override
    protected void update(float delta) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (paused) exitToLevelSelect();
            else setPaused(true);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && paused) {
            setPaused(false);
            return;
        }
        if (paused) {
            handlePauseTouched();
            return;
        }
        if (levelCompletedState) {
            handleCompleteTouched();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !deathPaused && !musicFading) {
            triggerRespawn();
            return;
        }

        if (deathPaused) {
            deathTimer += delta;
            if (popupTimer >= 0f) {
                popupTimer += delta;
                if (popupTimer >= POPUP_TOTAL) popupTimer = -1f;
            }
            if (deathTimer >= DEATH_PAUSE_DURATION) {
                deathPaused = false;
                deathTimer = 0f;
                musicFading = false;
                musicFadeTimer = 0f;
                levelCompletedState = false;
                levelEndingSequence = false;
                levelEndTimer = 0f;
                paused = false;
                lastJumpHeld = false;
                popupTimer = -1f;
                world.reset();
                engine.reset();
                startMusic();
                recordAttempt();
                hitboxesActive = game.getSettingsManager().showHitboxes;
            }
            return;
        }

        lastDelta = delta;

        if (musicFading && levelMusic != null) {
            musicFadeTimer += delta;
            float base = game.getSettingsManager().musicVolume;
            float volume = base * (1f - Math.min(musicFadeTimer / MUSIC_FADE_DURATION, 1f));
            levelMusic.setVolume(volume);
            if (musicFadeTimer >= MUSIC_FADE_DURATION) {
                stopAndDisposeMusic();
                musicFading = false;
                world.reset();
                engine.reset();
                game.setScreen(new LevelSelectScreen(game, levelIndex));
            }
            return;
        }

        if (Gdx.input.justTouched()) {
            Vector2 t = unprojectWorld();
            float cx = pauseCircleCX(), cy = pauseCircleCY(), r = PAUSE_BTN / 2f + 8f;
            float dx = t.x - cx, dy = t.y - cy;
            if (dx * dx + dy * dy <= r * r) {
                setPaused(true);
                return;
            }
        }

        handleInput();

        engine.update(delta);

        if (world.isPlayerDead()) {
            recordDeath();
            stopAndDisposeMusic();
            musicFading = false;
            deathPaused = true;
            deathTimer = 0f;
            lastDelta = 0f;
            checkAndShowAd(false);
            engine.reset();
            if (game.getSettingsManager().showHitboxesOnDeath)
                hitboxesActive = true;
        }

        // 1. Trigger the ending sequence the exact moment the level is beaten
        if (world.isLevelComplete() && !levelEndingSequence && !levelCompletedState) {
            recordComplete(); // Record the win immediately so stats are updated
            levelEndingSequence = true;
            levelEndTimer = 0f;
        }

        if (levelEndingSequence && !levelCompletedState) {
            levelEndTimer += delta;

            if (levelEndTimer >= END_MUSIC_FADE_START && levelMusic != null) {
                float fadeDuration = END_DELAY_TOTAL - END_MUSIC_FADE_START;
                float timeSpentFading = levelEndTimer - END_MUSIC_FADE_START;

                float fadeProgress = Math.min(timeSpentFading / fadeDuration, 1f);

                float baseVol = game.getSettingsManager().musicVolume;
                levelMusic.setVolume(baseVol * (1f - fadeProgress));
            }

            if (levelEndTimer >= END_DELAY_TOTAL) {
                levelCompletedState = true;
                checkAndShowAd(true);
                stopAndDisposeMusic();
                Gdx.input.setCursorCatched(false);
            }
        }
    }


    /**
     * Renders the game screen and its user interface components.
     * <p>
     * This method performs the following drawing operations:
     * <ul>
     *     <li>Applies the game viewport and clears the screen with the world's background color.</li>
     *     <li>Delegates core entity and world rendering to the {@link GameRenderer}.</li>
     *     <li>Draws the Heads-Up Display (HUD) elements, including the progress bar,
     *         attempt counters, and the pause button.</li>
     *     <li>Renders transient UI elements like "New Best" popups.</li>
     *     <li>If the game is currently {@code paused}, renders the full-screen pause menu overlay.</li>
     * </ul>
     */
    @Override
    protected void draw() {
        gameViewport.apply();

        Color bg = world.getBackgroundColor();
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.render(lastDelta, paused, hitboxesActive);

        game.getBatch().setProjectionMatrix(gameCamera.combined);
        shapes.setProjectionMatrix(gameCamera.combined);

        // 1. Draw UI Shapes
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawProgressBarShapes();
        drawPauseButtonShapes();

        if (paused || levelCompletedState) {
            shapes.setColor(COL_OVERLAY);
            shapes.rect(camLeft(), camBot(), gameViewport.getWorldWidth(), gameViewport.getWorldHeight());
        }

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 2. Draw UI Text and Sprites
        game.getBatch().begin();

        drawProgressBarText();
        drawSessionAttemptsText();
        drawNewBestPopupText();

        if (paused) {
            drawPauseOverlayUI();
        } else if (levelCompletedState) {
            drawCompleteOverlayUI();
        }

        game.getBatch().end();

        // 3. Pause Slider (needs shapes again, but we keep it separate to avoid complexity for now)
        if (paused) {
            drawPauseSliderShapes();
        }
    }

    /**
     * Renders the fill and background shapes for the level progress bar.
     */
    private void drawProgressBarShapes() {
        float progress = world.getProgress();
        if (progress <= 0f) return;
        SettingsManager s = game.getSettingsManager();
        if (!s.showProgressBar) return;

        final float BAR_W = gameViewport.getWorldWidth() * 0.625f * 0.55f;
        final float BAR_H = 10f;
        final float GAP = 14f;
        final float LINE_Y = camTop() - 18f;

        float textW = 0f;
        if (s.showPercentage) {
            _hudSb.setLength(0);
            _hudSb.append(Math.round(progress * 100f)).append('%');
            font.getData().setScale(1.2f);
            glyphLayout.setText(font, _hudSb, Color.WHITE, 0, Align.left, false);
            textW = glyphLayout.width;
        }

        float totalW = (s.showPercentage ? textW + GAP : 0f) + BAR_W;
        float startX = gameCamera.position.x - totalW / 2f;

        float r = BAR_H / 2f;
        float fillW = BAR_W * progress;

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
     * Renders the percentage text for the level progress bar.
     */
    private void drawProgressBarText() {
        float progress = world.getProgress();
        if (progress <= 0f) return;
        SettingsManager s = game.getSettingsManager();
        if (!s.showPercentage) return;

        int pct = Math.round(progress * 100f);
        final float BAR_W = gameViewport.getWorldWidth() * 0.625f * 0.55f;
        final float GAP = 14f;
        final float LINE_Y = camTop() - 18f;

        _hudSb.setLength(0);
        _hudSb.append(pct).append('%');
        font.getData().setScale(1.2f);
        glyphLayout.setText(font, _hudSb, Color.WHITE, 0, Align.left, false);
        float textW = glyphLayout.width;
        float textH = glyphLayout.height;

        float totalW = textW + (s.showProgressBar ? GAP + BAR_W : 0f);
        float startX = gameCamera.position.x - totalW / 2f;

        float textDrawX = startX;
        if (s.showProgressBar) {
            textDrawX = startX + BAR_W + GAP;
        }

        boolean isPB = false;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            isPB = pct > p.bestPercent;
        }
        Color textColor = isPB ? COL_HEADING : Color.WHITE;

        // Shadow
        font.setColor(0, 0, 0, textColor.a * 0.4f);
        font.draw(game.getBatch(), _hudSb, textDrawX + 2f, LINE_Y + textH / 2f - 2f);

        // Text
        font.setColor(textColor);
        font.draw(game.getBatch(), _hudSb, textDrawX, LINE_Y + textH / 2f);
        font.getData().setScale(1f);
    }

    /**
     * Renders the background and icon shapes for the pause button.
     */
    private void drawPauseButtonShapes() {
        float cx = pauseCircleCX(), cy = pauseCircleCY(), r = PAUSE_BTN / 2f;
        shapes.setColor(0.2f, 0.2f, 0.2f, 0.75f);
        shapes.circle(cx, cy, r, 32);
        float barW = r * 0.22f, barH = r * 0.75f;
        float gap = r * 0.18f;
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.rect(cx - gap - barW, cy - barH / 2f, barW, barH);
        shapes.rect(cx + gap, cy - barH / 2f, barW, barH);
    }

    /**
     * Renders the attempt counters and FPS text.
     */
    private void drawSessionAttemptsText() {
        float left = camLeft() + 12f;
        float top = camTop() - 12f;
        float shadowOffset = 2f;

        // Attempt
        _hudSb.setLength(0);
        _hudSb.append("Attempt  ").append(sessionAttempts);
        font.setColor(0, 0, 0, HUD_ATTEMPT.a * 0.4f);
        font.draw(game.getBatch(), _hudSb, left + shadowOffset, top - shadowOffset);
        font.setColor(HUD_ATTEMPT);
        font.draw(game.getBatch(), _hudSb, left, top);

        float nextY = top - 26f;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            // Best
            _hudSb.setLength(0);
            _hudSb.append("Best  ").append(p.bestPercent).append('%');
            font.setColor(0, 0, 0, HUD_BEST.a * 0.4f);
            font.draw(game.getBatch(), _hudSb, left + shadowOffset, nextY - shadowOffset);
            font.setColor(HUD_BEST);
            font.draw(game.getBatch(), _hudSb, left, nextY);
            nextY -= 26f;
        }

        if (game.getSettingsManager().showFps) {
            // FPS
            _hudSb.setLength(0);
            _hudSb.append("FPS  ").append(Gdx.graphics.getFramesPerSecond());
            font.setColor(0, 0, 0, HUD_FPS.a * 0.4f);
            font.draw(game.getBatch(), _hudSb, left + shadowOffset, nextY - shadowOffset);
            font.setColor(HUD_FPS);
            font.draw(game.getBatch(), _hudSb, left, nextY);
        }
    }

    /**
     * Renders the text for the high-score "New Best" notification.
     */
    private void drawNewBestPopupText() {
        if (popupTimer < 0f) return;

        float alpha;
        float scale;

        if (popupTimer < POPUP_FADE_IN) {
            float progress = popupTimer / POPUP_FADE_IN;
            alpha = progress;
            scale = 1.0f + 0.8f * progress; // Zoom in from 1.0 to 1.8
        } else if (popupTimer < POPUP_FADE_IN + POPUP_HOLD) {
            float holdProgress = (popupTimer - POPUP_FADE_IN) / POPUP_HOLD;
            alpha = 1f;
            scale = 1.8f - 0.4f * holdProgress; // Scale down from 1.8 to 1.4 during hold
        } else {
            float progress = (popupTimer - POPUP_FADE_IN - POPUP_HOLD) / POPUP_FADE_OUT;
            alpha = 1f - progress;
            scale = 1.4f * (1f - progress); // Fade out and shrink to 0
        }
        alpha = Math.max(0f, Math.min(1f, alpha));
        scale = Math.max(0f, scale);

        float cx = gameCamera.position.x;
        float cy = gameCamera.position.y;

        // Draw "NEW BEST"
        font.getData().setScale(scale);
        _hudSb.setLength(0);
        _hudSb.append("NEW BEST");
        glyphLayout.setText(font, _hudSb);
        float newBestTextHeight = glyphLayout.height;
        float newBestTextY = cy + newBestTextHeight / 2f;

        // Shadow
        font.setColor(0, 0, 0, alpha * 0.4f);
        font.draw(game.getBatch(), _hudSb, cx - glyphLayout.width / 2f + 2f, newBestTextY - 2f);

        // Text
        font.setColor(COL_HEADING.r, COL_HEADING.g, COL_HEADING.b, alpha);
        font.draw(game.getBatch(), _hudSb, cx - glyphLayout.width / 2f, newBestTextY);

        // Draw percentage
        font.getData().setScale(scale * 0.6f);
        _hudSb.setLength(0);
        _hudSb.append(popupBestPct).append('%');
        glyphLayout.setText(font, _hudSb);
        float percentageTextY = newBestTextY - newBestTextHeight - 5f;

        // Shadow
        font.setColor(0, 0, 0, alpha * 0.85f * 0.4f);
        font.draw(game.getBatch(), _hudSb, cx - glyphLayout.width / 2f + 2f, percentageTextY - 2f);

        // Text
        font.setColor(1f, 1f, 1f, alpha * 0.85f);
        font.draw(game.getBatch(), _hudSb, cx - glyphLayout.width / 2f, percentageTextY);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    /**
     * Renders the main panel and content of the pause menu overlay.
     */
    private void drawPauseOverlayUI() {
        float px = panelX(), py = panelY();
        float shadowOffset = 2f;

        int texW = (int) PANEL_W, texH = (int) PANEL_H;
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();
            panelTexture = createRoundedRect(texW, texH, 24, COL_PANEL);
            lastPanelW = texW;
            lastPanelH = texH;
        }

        game.getBatch().draw(panelTexture, px, py);

        String levelName = (levelData != null && levelData.name != null) ? levelData.name : "Level";
        pauseFont.getData().setScale(1f);
        glyphLayout.setText(pauseFont, levelName);
        float x = px + PANEL_W / 2f - glyphLayout.width / 2f;
        float y = py + PANEL_H - 18f;
        pauseFont.setColor(0, 0, 0, COL_HEADING.a * 0.4f);
        pauseFont.draw(game.getBatch(), levelName, x + shadowOffset, y - shadowOffset);
        pauseFont.setColor(COL_HEADING);
        pauseFont.draw(game.getBatch(), levelName, x, y);

        float sy = py + PANEL_H - 18f - glyphLayout.height - 22f;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            pauseFont.getData().setScale(0.62f);
            String best = "Personal Best: " + p.bestPercent + "%";
            glyphLayout.setText(pauseFont, best);
            x = px + PANEL_W / 2f - glyphLayout.width / 2f;
            pauseFont.setColor(0, 0, 0, COL_LABEL.a * 0.4f);
            pauseFont.draw(game.getBatch(), best, x + shadowOffset, sy - shadowOffset);
            pauseFont.setColor(COL_LABEL);
            pauseFont.draw(game.getBatch(), best, x, sy);

            sy -= glyphLayout.height + 12f;
            pauseFont.setColor(COL_DIM);
            String att = "Total: " + p.totalAttempts + "   Session: " + sessionAttempts;
            glyphLayout.setText(pauseFont, att);
            x = px + PANEL_W / 2f - glyphLayout.width / 2f;
            pauseFont.setColor(0, 0, 0, COL_DIM.a * 0.4f);
            pauseFont.draw(game.getBatch(), att, x + shadowOffset, sy - shadowOffset);
            pauseFont.setColor(COL_DIM);
            pauseFont.draw(game.getBatch(), att, x, sy);
        }

        if (backRegion != null)
            game.getBatch().draw(backRegion, backX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE * 0.9f);
        if (resumeRegion != null)
            game.getBatch().draw(resumeRegion, resumeX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE * 0.9f);

        float sliderY = pauseSliderY();
        pauseFont.getData().setScale(0.52f);
        _hudSb.setLength(0);
        _hudSb.append("Volume");
        glyphLayout.setText(pauseFont, _hudSb);
        x = panelX() + PANEL_W / 2f - glyphLayout.width / 2f;
        y = sliderY + glyphLayout.height + 10f;
        pauseFont.setColor(0, 0, 0, COL_LABEL.a * 0.4f);
        pauseFont.draw(game.getBatch(), _hudSb, x + shadowOffset, y - shadowOffset);
        pauseFont.setColor(COL_LABEL);
        pauseFont.draw(game.getBatch(), _hudSb, x, y);

        float vol = game.getSettingsManager().musicVolume;
        pauseFont.getData().setScale(0.44f);
        _hudSb.setLength(0);
        _hudSb.append(Math.round(vol * 100f)).append('%');
        glyphLayout.setText(pauseFont, _hudSb);
        x = pauseSliderTrackX() - glyphLayout.width - 10f;
        y = sliderY + glyphLayout.height / 2f;
        pauseFont.setColor(0, 0, 0, COL_DIM.a * 0.4f);
        pauseFont.draw(game.getBatch(), _hudSb, x + shadowOffset, y - shadowOffset);
        pauseFont.setColor(COL_DIM);
        pauseFont.draw(game.getBatch(), _hudSb, x, y);

        pauseFont.getData().setScale(0.45f);
        glyphLayout.setText(pauseFont, "Back");
        x = backX() + BTN_SIZE * 0.9f / 2f - glyphLayout.width / 2f;
        y = backY() - 4f;
        pauseFont.setColor(0, 0, 0, COL_DIM.a * 0.4f);
        pauseFont.draw(game.getBatch(), "Back", x + shadowOffset, y - shadowOffset);
        pauseFont.setColor(COL_DIM);
        pauseFont.draw(game.getBatch(), "Back", x, y);

        glyphLayout.setText(pauseFont, "Resume");
        x = resumeX() + BTN_SIZE * 0.9f / 2f - glyphLayout.width / 2f;
        pauseFont.setColor(0, 0, 0, COL_DIM.a * 0.4f);
        pauseFont.draw(game.getBatch(), "Resume", x + shadowOffset, y - shadowOffset);
        pauseFont.setColor(COL_DIM);
        pauseFont.draw(game.getBatch(), "Resume", x, y);

        pauseFont.getData().setScale(1f);
    }

    /**
     * Renders the interactive shapes for the pause menu volume slider.
     */
    private void drawPauseSliderShapes() {
        float sliderY = pauseSliderY();
        float vol = game.getSettingsManager().musicVolume;
        float tx = pauseSliderTrackX(), tw = pauseSliderTrackW();
        float trackH = 5f, thumbR = 10f;
        float fillW = tw * vol;

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


    /**
     * Renders the level completion overlay, showing the success message and stats.
     */
    private void drawCompleteOverlayUI() {
        float px = panelX(), py = panelY();
        float shadowOffset = 2f;

        int texW = (int) PANEL_W, texH = (int) PANEL_H;
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();
            panelTexture = createRoundedRect(texW, texH, 24, COL_PANEL);
            lastPanelW = texW;
            lastPanelH = texH;
        }

        game.getBatch().draw(panelTexture, px, py);

        pauseFont.getData().setScale(1.1f);
        glyphLayout.setText(pauseFont, "LEVEL COMPLETE");
        float x = px + PANEL_W / 2f - glyphLayout.width / 2f;
        float y = py + PANEL_H - 22f;
        pauseFont.setColor(0, 0, 0, COL_HEADING.a * 0.4f);
        pauseFont.draw(game.getBatch(), "LEVEL COMPLETE", x + shadowOffset, y - shadowOffset);
        pauseFont.setColor(COL_HEADING);
        pauseFont.draw(game.getBatch(), "LEVEL COMPLETE", x, y);

        float sy = py + PANEL_H - 22f - glyphLayout.height - 35f;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            pauseFont.getData().setScale(0.7f);
            String stats = "Total Attempts: " + p.totalAttempts;
            glyphLayout.setText(pauseFont, stats);
            x = px + PANEL_W / 2f - glyphLayout.width / 2f;
            pauseFont.setColor(0, 0, 0, COL_LABEL.a * 0.4f);
            pauseFont.draw(game.getBatch(), stats, x + shadowOffset, sy - shadowOffset);
            pauseFont.setColor(COL_LABEL);
            pauseFont.draw(game.getBatch(), stats, x, sy);

            sy -= glyphLayout.height + 18f;
            String session = "Session Attempts: " + sessionAttempts;
            glyphLayout.setText(pauseFont, session);
            x = px + PANEL_W / 2f - glyphLayout.width / 2f;
            pauseFont.setColor(0, 0, 0, COL_DIM.a * 0.4f);
            pauseFont.draw(game.getBatch(), session, x + shadowOffset, sy - shadowOffset);
            pauseFont.setColor(COL_DIM);
            pauseFont.draw(game.getBatch(), session, x, sy);
        }

        if (backRegion != null)
            game.getBatch().draw(backRegion, backX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE * 0.9f);
        if (resumeRegion != null)
            game.getBatch().draw(resumeRegion, resumeX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE * 0.9f);

        pauseFont.getData().setScale(0.45f);
        glyphLayout.setText(pauseFont, "Menu");
        x = backX() + BTN_SIZE * 0.9f / 2f - glyphLayout.width / 2f;
        y = backY() - 4f;
        pauseFont.setColor(0, 0, 0, COL_DIM.a * 0.4f);
        pauseFont.draw(game.getBatch(), "Menu", x + shadowOffset, y - shadowOffset);
        pauseFont.setColor(COL_DIM);
        pauseFont.draw(game.getBatch(), "Menu", x, y);

        glyphLayout.setText(pauseFont, "Replay");
        x = resumeX() + BTN_SIZE * 0.9f / 2f - glyphLayout.width / 2f;
        pauseFont.setColor(0, 0, 0, COL_DIM.a * 0.4f);
        pauseFont.draw(game.getBatch(), "Replay", x + shadowOffset, y - shadowOffset);
        pauseFont.setColor(COL_DIM);
        pauseFont.draw(game.getBatch(), "Replay", x, y);

        pauseFont.getData().setScale(1f);
    }

    /**
     * Processes input for the level completion screen.
     */
    private void handleCompleteTouched() {
        Vector2 t = unprojectWorld();
        if (!Gdx.input.justTouched()) return;

        if (hits(t, backX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE)) {
            exitToLevelSelect();
        } else if (hits(t, resumeX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE)) {
            triggerRestart();
            ignoreInputUntilRelease = true;
        }
    }

    /**
     * Restarts the level from the completion screen.
     */
    private void triggerRestart() {
        levelCompletedState = false;
        levelEndingSequence = false;
        levelEndTimer = 0f;
        paused = false;
        lastJumpHeld = false;
        popupTimer = -1f;
        stopAndDisposeMusic();
        world.reset();
        engine.reset();
        startMusic();
        recordAttempt();
        if (game.getSettingsManager().lockCursorInGame)
            Gdx.input.setCursorCatched(true);
    }

    /**
     * Updates the pause state of the game and synchronizes associated systems.
     * <p>
     * This method handles the transition between active gameplay and the paused state by:
     * <ul>
     *     <li>Updating the internal {@code paused} flag.</li>
     *     <li>Resetting UI interaction states, such as volume slider dragging.</li>
     *     <li>Pausing or resuming the level music to maintain synchronization.</li>
     *     <li>Managing the hardware cursor lock (catching) based on the user's settings,
     *         ensuring the cursor is released for menu navigation while paused.</li>
     * </ul>
     * </p>
     *
     * @param p true to pause the game, false to resume.
     */
    private void setPaused(boolean p) {
        paused = p;
        if (!p) pauseSliderDragging = false;
        if (levelMusic != null) {
            if (paused) levelMusic.pause();
            else levelMusic.play();
        }
        if (game.getSettingsManager().lockCursorInGame)
            Gdx.input.setCursorCatched(!paused);
    }

    /**
     * Processes touch and mouse input specifically for the pause menu interface.
     * <p>
     * This method handles three primary interaction states:
     * <ul>
     */
    private void handlePauseTouched() {
        Vector2 t = unprojectWorld();

        if (Gdx.input.isTouched() && pauseSliderDragging) {
            float tx = pauseSliderTrackX(), tw = pauseSliderTrackW();
            float vol = Math.max(0f, Math.min(1f, (t.x - tx) / tw));
            game.getSettingsManager().musicVolume = vol;
            game.getSoundManager().setMusicVolume(vol);
            if (levelMusic != null) levelMusic.setVolume(vol);
            game.getSettingsManager().save();
        }
        if (!Gdx.input.isTouched()) pauseSliderDragging = false;

        if (!Gdx.input.justTouched()) return;
        if (hitsPauseSlider(t)) {
            pauseSliderDragging = true;
            return;
        }
        if (hits(t, backX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE)) {
            exitToLevelSelect();
            return;
        }
        if (hits(t, resumeX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE)) {
            setPaused(false);
            ignoreInputUntilRelease = true;
            return;
        }
    }

    /**
     * Terminates the current gameplay session and returns the player to the level selection menu.
     * <p>
     * This method performs necessary cleanup by stopping and disposing of the active music
     * track before transitioning the game to the {@link LevelSelectScreen}, passing along
     * the current level index to maintain the player's position in the selection list.
     * </p>
     */
    private void exitToLevelSelect() {
        stopAndDisposeMusic();
        game.setScreen(new LevelSelectScreen(game, levelIndex));
    }

    /**
     * Resets the game state and restarts the level immediately.
     * <p>
     * This method performs a full cleanup and re-initialization sequence:
     * <ul>
     *     <li>Records the final progress of the current run and updates high scores.</li>
     *     <li>Stops and disposes of any currently playing music.</li>
     *     <li>Resets internal timers (death, music fading) and input flags.</li>
     *     <li>Resets the {@link GameWorld} and {@link FixedTickEngine} to their initial states.</li>
     */
    private void triggerRespawn() {
        recordDeath();
        stopAndDisposeMusic();
        deathPaused = false;
        deathTimer = 0f;
        musicFading = false;
        musicFadeTimer = 0f;
        levelCompletedState = false;
        levelEndingSequence = false;
        levelEndTimer = 0f;
        paused = false;
        lastDelta = 0f;
        lastJumpHeld = false;
        popupTimer = -1f;
        world.reset();
        engine.reset();
        startMusic();
        recordAttempt();
        hitboxesActive = game.getSettingsManager().showHitboxes;
    }


    /**
     * Increments and persists the number of attempts for the current level.
     * <p>
     * This method updates both the volatile {@code sessionAttempts} counter (reset upon
     * leaving the screen) and the persistent {@code totalAttempts} stored in the
     * {@link LevelProgress}. The updated progress is immediately saved to ensure data
     * consistency even if the game is closed unexpectedly.
     * </p>
     */
    private void recordAttempt() {
        sessionAttempts++;
        if (levelKey == null) return;
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        p.totalAttempts++;
        game.getProgressManager().save();
    }

    /**
     * Updates the persistent progress for the current level upon player death.
     * <p>
     * This method calculates the completion percentage of the current run. If the
     * achieved percentage is higher than the previously recorded best, it updates
     * the {@link LevelProgress} data, saves the changes to the progress manager,
     * and initializes the "New Best" popup notification.
     * </p>
     */
    private void recordDeath() {
        if (levelKey == null) return;
        int pct = Math.round(world.getProgress() * 100f);
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        if (pct > p.bestPercent) {
            p.bestPercent = pct;
            game.getProgressManager().save();
            // Trigger the new best popup
            popupTimer = 0f;
            popupBestPct = pct;
        }
    }

    /**
     * Updates the persistent progress for the current level upon successful completion.
     * <p>
     * This method sets the level's best completion percentage to 100% in the
     * {@link LevelProgress} data and persists the change via the progress manager.
     * It is triggered when the player reaches the end of the level without dying.
     * </p>
     */
    private void recordComplete() {
        if (levelKey == null) return;
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        p.bestPercent = 100;
        game.getProgressManager().save();
    }


    /**
     * Renders the level progress indicator at the top of the screen.
     * <p>
     * This method handles the logic for displaying the progress percentage text and/or
     * a visual progress bar based on the user's {@link SettingsManager} preferences.
     * </p>
     * <p>
     * Key behaviors include:
     * <ul>
     *     <li>Calculating the current completion percentage from the {@link GameWorld}.</li>
     *     <li>Centering the combined UI elements (text + bar) horizontally.</li>
     *     <li>Highlighting the percentage text with {@code COL_HEADING} if the player
     *         surpasses their previous personal best during the current run.</li>
     *     <li>Drawing a rounded background track and a filled progress foreground using
     *         {@link ShapeRenderer} with alpha blending.</li>
     * </ul>
     * </p>
     */
    private void drawProgressBar() {
        float progress = world.getProgress();
        if (progress <= 0f) return;

        SettingsManager s = game.getSettingsManager();
        if (!s.showPercentage && !s.showProgressBar) return;

        int pct = Math.round(progress * 100f);
        float screenTop = camTop();
        float screenCX = gameCamera.position.x;

        final float BAR_W = gameViewport.getWorldWidth() * 0.625f * 0.55f;
        final float BAR_H = 10f;
        final float GAP = 14f;
        final float LINE_Y = screenTop - 18f;

        float textW = 0f, textH = 0f;
        if (s.showPercentage) {
            _hudSb.setLength(0);
            _hudSb.append(pct).append('%');
            font.getData().setScale(1.2f);
            glyphLayout.setText(font, _hudSb, Color.WHITE, 0, Align.left, false);
            textW = glyphLayout.width;
            textH = glyphLayout.height;
        }

        float totalW = 0f;
        if (s.showPercentage) totalW += textW;
        if (s.showPercentage && s.showProgressBar) totalW += GAP;
        if (s.showProgressBar) totalW += BAR_W;

        float startX = screenCX - totalW / 2f;

        if (s.showPercentage) {
            boolean isPB = false;
            if (levelKey != null) {
                LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
                isPB = pct > p.bestPercent;
            }
            game.getBatch().setProjectionMatrix(gameCamera.combined);
            game.getBatch().begin();
            font.setColor(isPB ? COL_HEADING : Color.WHITE);
            font.draw(game.getBatch(), _hudSb, startX, LINE_Y + textH / 2f);
            game.getBatch().end();
            font.getData().setScale(1f);
            startX += textW + (s.showProgressBar ? GAP : 0f);
        }

        if (s.showProgressBar) {
            float barX = startX;
            float barY = LINE_Y - BAR_H / 2f;
            float r = BAR_H / 2f;
            float fillW = BAR_W * progress;

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.setProjectionMatrix(gameCamera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);

            shapes.setColor(0.2f, 0.2f, 0.2f, 0.55f);
            drawRoundedRect(barX, barY, BAR_W, BAR_H, r);

            if (fillW >= BAR_H) {
                shapes.setColor(COL_FILL);
                drawRoundedRect(barX, barY, fillW, BAR_H, r);
            } else if (fillW > 0) {
                shapes.setColor(COL_FILL);
                shapes.rect(barX, barY, fillW, BAR_H);
            }

            shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    /**
     * Draws a rectangle with rounded corners using the {@link ShapeRenderer}.
     * <p>
     * This helper method constructs a rounded rectangle by drawing three overlapping
     * rectangles (forming a cross shape) and four circles at the corners. It is
     * typically used within a {@code shapes.begin(ShapeRenderer.ShapeType.Filled)} block.
     * </p>
     *
     * @param x The x-coordinate of the bottom-left corner of the rectangle.
     * @param y The y-coordinate of the bottom-left corner of the rectangle.
     * @param w The total width of the rectangle.
     * @param h The total height of the rectangle.
     * @param r The radius of the corners.
     */
    private void drawRoundedRect(float x, float y, float w, float h, float r) {
        shapes.rect(x + r, y, w - 2 * r, h);
        shapes.rect(x, y + r, r, h - 2 * r);
        shapes.rect(x + w - r, y + r, r, h - 2 * r);
        shapes.circle(x + r, y + r, r, 16);
        shapes.circle(x + w - r, y + r, r, 16);
        shapes.circle(x + r, y + h - r, r, 16);
        shapes.circle(x + w - r, y + h - r, r, 16);
    }

    /**
     * Renders the session-specific HUD elements in the top-left corner of the screen.
     * <p>
     * This method displays real-time statistics and debug information, including:
     * <ul>
     *     <li>The current session attempt counter (the number of tries since entering this screen).</li>
     *     <li>The player's personal best completion percentage for the current level.</li>
     *     <li>The current Frames Per Second (FPS), if enabled in the {@link SettingsManager}.</li>
     * </ul>
     * The HUD is rendered using a {@link StringBuilder} to avoid garbage collection overhead
     * and is positioned relative to the camera's top-left bounds to ensure it remains
     * visible regardless of camera movement.
     * </p>
     */
    private void drawSessionAttempts() {
        float left = gameCamera.position.x - gameViewport.getWorldWidth() / 2f + 12f;
        float top = gameCamera.position.y + gameViewport.getWorldHeight() / 2f - 12f;
        game.getBatch().setProjectionMatrix(gameCamera.combined);
        game.getBatch().begin();

        font.setColor(HUD_ATTEMPT);
        _hudSb.setLength(0);
        _hudSb.append("Attempt  ").append(sessionAttempts);
        font.draw(game.getBatch(), _hudSb, left, top);

        float nextY = top - 26f;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            font.setColor(HUD_BEST);
            _hudSb.setLength(0);
            _hudSb.append("Best  ").append(p.bestPercent).append('%');
            font.draw(game.getBatch(), _hudSb, left, nextY);
            nextY -= 26f;
        }

        if (game.getSettingsManager().showFps) {
            font.setColor(HUD_FPS);
            _hudSb.setLength(0);
            _hudSb.append("FPS  ").append(Gdx.graphics.getFramesPerSecond());
            font.draw(game.getBatch(), _hudSb, left, nextY);
        }

        game.getBatch().end();
    }


    private boolean lastJumpHeld = false;

    /**
     * Polls for player input and updates the game engine with jump state changes.
     * <p>
     * This method checks for multiple jump triggers, including the Spacebar, 'W' key,
     * Up Arrow, and touch/mouse clicks. If the jump state has changed since the last
     * frame (e.g., the player just pressed or just released a key), it queues the
     * input event into the {@link FixedTickEngine} using the current accumulator
     * time to ensure physics-accurate timing.
     * </p>
     */
    private void handleInput() {
        if (ignoreInputUntilRelease) {
            if (!Gdx.input.isTouched()) ignoreInputUntilRelease = false;
            return;
        }
        boolean jump =
            Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyPressed(Input.Keys.W) ||
                Gdx.input.isKeyPressed(Input.Keys.UP) ||
                Gdx.input.isTouched();

        if (jump != lastJumpHeld) {
            engine.queueInput(jump, engine.getAccumulator());
            lastJumpHeld = jump;
        }
    }


    /**
     * Initializes and begins playback of the background music associated with the current level.
     * <p>
     * This method attempts to locate the audio file specified in {@link LevelData} by checking
     * both internal and local storage paths. If the file is found, it creates a new {@link Music}
     * instance, applies the user's volume preferences from the {@link SettingsManager},
     * and starts non-looping playback.
     * </p>
     * <p>
     */
    private void startMusic() {
        if (levelData == null || levelData.musicFile == null || levelData.musicFile.isEmpty())
            return;
        try {
            FileHandle fh = Gdx.files.internal("musics/" + levelData.musicFile);
            if (!fh.exists()) fh = Gdx.files.local("assets/musics/" + levelData.musicFile);
            if (fh.exists()) {
                levelMusic = Gdx.audio.newMusic(fh);
                levelMusic.setVolume(game.getSettingsManager().musicVolume);
                levelMusic.setLooping(false);
                levelMusic.play();
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Could not load music: " + e.getMessage());
        }
    }

    /**
     * Safely stops and releases the resources of the currently active level music.
     * <p>
     * This method checks if {@code levelMusic} is initialized, stops playback if it is
     * currently playing, disposes of the native resources to prevent memory leaks,
     * and finally nullifies the reference.
     * </p>
     */
    private void stopAndDisposeMusic() {
        if (levelMusic != null) {
            if (levelMusic.isPlaying()) levelMusic.stop();
            levelMusic.dispose();
            levelMusic = null;
        }
    }


    private final Vector2 _unprojectTmp2 = new Vector2();
    private final com.badlogic.gdx.math.Vector3 _unprojectTmp = new com.badlogic.gdx.math.Vector3();

    /**
     * Converts the current screen coordinates of the mouse or touch input into world coordinates.
     * <p>
     * This method uses the {@code gameCamera} to translate the raw window-space pixels (where 0,0 is the top-left)
     * into the game's coordinate system. It utilizes a cached {@code Vector3} to avoid frequent
     * object allocation during input polling.
     * </p>
     *
     * @return A {@link Vector2} representing the touch position in the game world.
     */
    private Vector2 unprojectWorld() {
        _unprojectTmp.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        gameCamera.unproject(_unprojectTmp);
        _unprojectTmp2.set(_unprojectTmp.x, _unprojectTmp.y);
        return _unprojectTmp2;
    }

    /**
     * Checks if a 2D vector point lies within the boundaries of a specified axis-aligned rectangle.
     *
     * @param t The coordinates of the point to check (typically a touch or mouse position).
     * @param x The x-coordinate of the rectangle's left edge.
     * @param y The y-coordinate of the rectangle's bottom edge.
     * @param w The width of the rectangle.
     * @param h The height of the rectangle.
     * @return true if the point is inside or on the boundary of the rectangle, false otherwise.
     */
    private static boolean hits(Vector2 t, float x, float y, float w, float h) {
        return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h;
    }

    /**
     * Checks if the specified coordinate point (tx, ty) lies within the boundaries
     * of an axis-aligned rectangle defined by its bottom-left corner (x, y),
     * width (w), and height (h).
     *
     * @param tx The x-coordinate of the point to check.
     * @param ty The y-coordinate of the point to check.
     * @param x  The x-coordinate of the rectangle's left edge.
     * @param y  The y-coordinate of the rectangle's bottom edge.
     * @param w  The width of the rectangle.
     * @param h  The height of the rectangle.
     * @return true if the point is within the rectangle's bounds, false otherwise.
     */
    private static boolean hits(float tx, float ty, float x, float y, float w, float h) {
        return tx >= x && tx <= x + w && ty >= y && ty <= y + h;
    }

    /**
     * Creates a new {@link Texture} containing a filled rectangle with rounded corners.
     * <p>
     * This method generates the texture procedurally by creating a {@link Pixmap},
     * drawing a cross of two rectangles to fill the center, and drawing four circles
     * to create the rounded corners. The resulting texture is used for UI elements
     * like the pause menu panel.
     * </p>
     *
     * @param w     The width of the rectangle in pixels.
     * @param h     The height of the rectangle in pixels.
     * @param r     The radius of the rounded corners in pixels.
     * @param color The {@link Color} used to fill the rectangle.
     * @return A new Texture containing the rounded rectangle; the caller is responsible
     *         for disposing of this texture when it is no longer needed.
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
     * Checks the 1-minute cooldown and RNG chances before showing an ad.
     * @param isLevelComplete If true, forces a 100% chance to show the ad (still respects cooldown).
     */
    private void checkAndShowAd(boolean isLevelComplete) {
        // 1. Check if 1 minute has passed since the last ad
        if (TimeUtils.timeSinceMillis(lastAdTimeMillis) < AD_COOLDOWN_MS) {
            return; // Cooldown is still active, skip the ad!
        }

        // 2. Determine chance: 100% on win, 25% chance on normal death
        boolean shouldShowAd = isLevelComplete || MathUtils.randomBoolean(0.25f);

        // 3. Show the ad and reset the timer!
        if (shouldShowAd && game.getAdController() != null) {
            game.getAdController().showInterstitialAd();
            lastAdTimeMillis = TimeUtils.millis();
        }
    }
}
