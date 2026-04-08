package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.engine.FixedTickEngine;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;
import io.github.msameer0.rhythmicrush.screens.ui.HudRenderer;
import io.github.msameer0.rhythmicrush.screens.ui.OverlayUI;

/**
 * Primary screen for the core gameplay loop.
 *
 * <p>Acts as a coordinator only — all audio, HUD drawing, overlay UI, practice
 * checkpoints, hitbox rendering, color transitions, and pool management have
 * been extracted into dedicated classes. This class wires them together and
 * owns the high-level state machine (alive → dead → respawn, level-end, pause).</p>
 */
public class GameScreen extends AbstractScreen {

    // ── Timing constants ──────────────────────────────────────────────────────
    private static final float DEATH_PAUSE_DURATION  = 0.75f;
    private static final float END_DELAY_TOTAL       = 2.0f;
    private static final float END_MUSIC_FADE_START  = 1.0f;

    private static final long  AD_COOLDOWN_MS        = 60_000L;
    private static long        lastAdTimeMillis      = 0L;

    // ── Core systems ──────────────────────────────────────────────────────────
    private final GameWorld         world;
    private final FixedTickEngine   engine;
    private final GameRenderer      renderer;
    private final MusicController   music;
    private final HudRenderer       hud;
    private final OverlayUI         overlay;
    private final PracticeManager   practice; // null when not in practice mode

    // ── Camera / viewport ─────────────────────────────────────────────────────
    private final OrthographicCamera gameCamera;
    private final Viewport           gameViewport;

    // ── Rendering helpers ─────────────────────────────────────────────────────
    private final ShapeRenderer shapes;

    // ── Level metadata ────────────────────────────────────────────────────────
    private final LevelData levelData;
    private final int       levelIndex;
    private final boolean   isPracticeMode;
    private       String    levelKey;

    // ── Session state ─────────────────────────────────────────────────────────
    private int     sessionAttempts = 0;
    private boolean hitboxesActive  = false;

    // ── Gameplay state machine ────────────────────────────────────────────────
    private boolean paused              = false;
    private boolean deathPaused         = false;
    private float   deathTimer          = 0f;
    private boolean levelEndingSequence = false;
    private float   levelEndTimer       = 0f;
    private boolean levelCompletedState = false;
    private float   lastDelta           = 0f;
    private boolean lastJumpHeld        = false;
    private boolean ignoreInputUntilRelease = false;

    // ── Unprojection scratch vectors ──────────────────────────────────────────
    private final Vector3 _unprojectTmp  = new Vector3();
    private final Vector2 _unprojectTmp2 = new Vector2();

    // ── Input processor ───────────────────────────────────────────────────────
    private final com.badlogic.gdx.InputProcessor gameInputProcessor = new com.badlogic.gdx.InputAdapter() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            _unprojectTmp.set(screenX, screenY, 0);
            gameCamera.unproject(_unprojectTmp);
            float tx = _unprojectTmp.x, ty = _unprojectTmp.y;

            if (paused) {
                if (overlay.hitsBackButton(tx, ty, gameCamera))   { exitToLevelSelect(); return true; }
                if (overlay.hitsResumeButton(tx, ty, gameCamera)) { setPaused(false); ignoreInputUntilRelease = true; return true; }
                if (overlay.hitsSlider(new Vector2(tx, ty), gameCamera)) { overlay.beginSliderDrag(); return true; }
                return false;
            }

            if (levelCompletedState) {
                if (overlay.hitsBackButton(tx, ty, gameCamera))   { exitToLevelSelect(); return true; }
                if (overlay.hitsResumeButton(tx, ty, gameCamera)) { triggerRestart(); ignoreInputUntilRelease = true; return true; }
                return false;
            }

            if (hud.hitsPauseButton(tx, ty, gameCamera, gameViewport)) {
                setPaused(true);
                return true;
            }

            if (isPracticeMode) {
                practice.updateButtonCoords(camCX(), camBot(),
                    game.getSettingsManager().getUiPadding(),
                    overlay.getUiScale(), practice.getBtnSize());
                if (practice.hitsPlus(tx, ty))  { placeCheckpoint(); return true; }
                if (practice.hitsMinus(tx, ty)) { removeLastCheckpoint(); return true; }
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (paused && overlay.isSliderDragging()) {
                _unprojectTmp.set(screenX, screenY, 0);
                gameCamera.unproject(_unprojectTmp);
                overlay.updateSliderFromDrag(_unprojectTmp.x, gameCamera);
                if (music.getMusic() != null)
                    music.setVolume(game.getSettingsManager().getMusicVolume());
                return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (overlay.isSliderDragging()) {
                overlay.endSliderDrag();
                game.getSettingsManager().save();
                return true;
            }
            return false;
        }
    };

    // ── Constructors ──────────────────────────────────────────────────────────

    public GameScreen(RhythmicRushGame game, LevelData levelData, int levelIndex) {
        this(game, levelData, levelIndex, false);
    }

    public GameScreen(RhythmicRushGame game, LevelData levelData, int levelIndex, boolean isPracticeMode) {
        super(game);
        this.levelData      = levelData;
        this.levelIndex     = levelIndex;
        this.isPracticeMode = isPracticeMode;

        gameCamera   = new OrthographicCamera();
        gameViewport = new ExtendViewport(1280, 720, gameCamera);
        gameViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        world    = new GameWorld();
        engine   = new FixedTickEngine(world);
        renderer = new GameRenderer(world, gameCamera, game.getBatch(), game.getAtlasManager());
        music    = new MusicController(game, levelData);
        shapes   = new ShapeRenderer();

        BitmapFont font      = game.getFontManager().get(FontManager.SIZE_SMALL);
        BitmapFont pauseFont = game.getFontManager().get(FontManager.SIZE_SMALL);

        hud = new HudRenderer(game, world, font, shapes, game.getBatch());

        TextureRegion resumeRegion = game.getAtlasManager().getMenuAtlas().findRegion("start_button");
        TextureRegion backRegion   = game.getAtlasManager().getLevelSelectAtlas().findRegion("back");
        overlay = new OverlayUI(game, levelData, pauseFont, shapes, game.getBatch(),
            resumeRegion, backRegion);

        practice = isPracticeMode ? new PracticeManager(world) : null;

        if (levelData != null) {
            world.loadLevel(levelData);
            levelKey = levelData.getFileName() != null
                ? levelData.getFileName() : levelData.getName() + ".json";
            recordAttempt();
        }

        hitboxesActive = game.getSettingsManager().getShowHitboxes();
    }

    // ── AbstractScreen lifecycle ──────────────────────────────────────────────

    @Override
    public void show() {
        overlay.updateScale();
        game.getSoundManager().stopMenuMusic();
        music.start();
        if (game.getSettingsManager().getLockCursorInGame())
            Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(gameInputProcessor);
    }

    @Override
    public void resize(int width, int height) {
        overlay.updateScale();
        gameViewport.update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        renderer.dispose();
        shapes.dispose();
        overlay.dispose();
        music.stopAndDispose();
        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(null);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    protected void update(float delta) {
        // Global key shortcuts
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if      (levelCompletedState) exitToLevelSelect();
            else if (paused)              setPaused(false);
            else                          setPaused(true);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && paused) { setPaused(false); return; }
        if (paused || levelCompletedState) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !deathPaused) {
            triggerRespawn(); return;
        }

        // Death pause: tick popup, then auto-respawn
        if (deathPaused) {
            deathTimer += delta;
            hud.update(delta);
            if (deathTimer >= DEATH_PAUSE_DURATION) triggerRespawn();
            return;
        }

        lastDelta = delta;

        // Music fade-out (exit after death)
        if (music.isFading()) {
            if (music.updateFade(delta)) {
                world.reset();
                engine.reset();
                game.setScreen(new LevelSelectScreen(game, levelIndex));
            }
            return;
        }

        if (isPracticeMode) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) placeCheckpoint();
            if (Gdx.input.isKeyJustPressed(Input.Keys.X)) removeLastCheckpoint();
        }

        handleInput();
        engine.update(delta);

        // Check death
        if (world.isPlayerDead()) {
            recordDeath();
            music.stopAndDispose();
            deathPaused = true;
            deathTimer  = 0f;
            lastDelta   = 0f;
            engine.reset();
            if (game.getSettingsManager().getShowHitboxesOnDeath()) hitboxesActive = true;
        }

        // Level-end sequence
        if (world.isLevelComplete() && !levelEndingSequence && !levelCompletedState) {
            recordComplete();
            levelEndingSequence = true;
            levelEndTimer = 0f;
        }

        if (levelEndingSequence && !levelCompletedState) {
            levelEndTimer += delta;
            if (levelEndTimer >= END_MUSIC_FADE_START) {
                float fadeDuration = END_DELAY_TOTAL - END_MUSIC_FADE_START;
                float fadeProgress = Math.min((levelEndTimer - END_MUSIC_FADE_START) / fadeDuration, 1f);
                music.applyFadeProgress(fadeProgress);
            }
            if (levelEndTimer >= END_DELAY_TOTAL) {
                levelCompletedState = true;
                checkAndShowAd(1.0f);
                music.stopAndDispose();
                Gdx.input.setCursorCatched(false);
            }
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    @Override
    protected void draw() {
        gameViewport.apply();

        if (isPracticeMode)
            practice.updateButtonCoords(camCX(), camBot(),
                game.getSettingsManager().getUiPadding(),
                overlay.getUiScale(), practice.getBtnSize());

        // Clear with world background colour
        Color bg = world.getBackgroundColor();
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.render(lastDelta, paused, hitboxesActive);

        game.getBatch().setProjectionMatrix(gameCamera.combined);
        shapes.setProjectionMatrix(gameCamera.combined);

        // ── Shape pass ────────────────────────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        hud.drawProgressBarShapes(gameCamera, gameViewport);
        hud.drawPauseButtonShapes(gameCamera, gameViewport);

        if (isPracticeMode) {
            float opacity = game.getSettingsManager().getPracticeButtonOpacity();
            practice.drawButtonShapes(shapes, opacity);
            practice.drawCheckpoints(shapes, gameCamera);
        }

        if (paused || levelCompletedState)
            overlay.drawDimOverlay(gameCamera, gameViewport);

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Text / sprite pass ────────────────────────────────────────────────
        game.getBatch().begin();

        hud.drawProgressBarText(gameCamera, gameViewport, levelKey);
        hud.drawSessionAttemptsText(gameCamera, gameViewport, sessionAttempts, levelKey);
        hud.drawNewBestPopup(gameCamera);

        if (isPracticeMode) drawPracticeButtonText();

        if (paused)
            overlay.drawPauseOverlay(gameCamera, sessionAttempts, levelKey);
        else if (levelCompletedState)
            overlay.drawCompleteOverlay(gameCamera, sessionAttempts, levelKey);

        game.getBatch().end();

        // ── Slider (standalone shape pass, only when paused) ──────────────────
        if (paused) overlay.drawPauseSlider(gameCamera);
    }

    // ── Practice button text (small helper kept here for font/scale access) ───

    private void drawPracticeButtonText() {
        // Delegate font drawing here since BitmapFont is owned by game's FontManager
        // and PracticeManager doesn't have a direct font reference
        float opacity  = game.getSettingsManager().getPracticeButtonOpacity();
        float uiScale  = overlay.getUiScale();
        float btnSize  = practice.getBtnSize();
        BitmapFont font = game.getFontManager().get(FontManager.SIZE_SMALL);
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

        font.getData().setScale(1.5f * uiScale);
        layout.setText(font, "+");
        float plusX = practice.getPlusX() + (btnSize - layout.width)  / 2f;
        float plusY = practice.getPlusY() + (btnSize + layout.height) / 2f;
        layout.setText(font, "-");
        float minusX = practice.getMinusX() + (btnSize - layout.width)  / 2f;
        float minusY = practice.getMinusY() + (btnSize + layout.height) / 2f;

        font.setColor(0, 0, 0, 0.4f * opacity);
        font.draw(game.getBatch(), "+", plusX  + 2f * uiScale, plusY  - 2f * uiScale);
        font.draw(game.getBatch(), "-", minusX + 2f * uiScale, minusY - 2f * uiScale);
        font.setColor(1, 1, 1, opacity);
        font.draw(game.getBatch(), "+", plusX,  plusY);
        font.draw(game.getBatch(), "-", minusX, minusY);
        font.getData().setScale(1f);
    }

    // ── State transitions ─────────────────────────────────────────────────────

    private void setPaused(boolean p) {
        paused = p;
        if (!p) overlay.endSliderDrag();
        if (p)  music.pause(); else music.resume();
        if (game.getSettingsManager().getLockCursorInGame())
            Gdx.input.setCursorCatched(!paused);
    }

    private void triggerRestart() {
        levelCompletedState = false;
        levelEndingSequence = false;
        levelEndTimer       = 0f;
        paused              = false;
        lastJumpHeld        = false;
        hud.showNewBestPopup(-1); // reset popup
        music.stopAndDispose();
        world.reset();
        engine.reset();
        music.start();
        recordAttempt();
        if (game.getSettingsManager().getLockCursorInGame())
            Gdx.input.setCursorCatched(true);
    }

    private void triggerRespawn() {
        if (isPracticeMode && practice.hasCheckpoints()) {
            respawnAtCheckpoint();
            return;
        }
        recordDeath();
        music.stopAndDispose();
        deathPaused         = false;
        deathTimer          = 0f;
        levelCompletedState = false;
        levelEndingSequence = false;
        levelEndTimer       = 0f;
        paused              = false;
        lastDelta           = 0f;
        lastJumpHeld        = false;
        world.reset();
        engine.reset();
        music.start();
        recordAttempt();
        hitboxesActive = game.getSettingsManager().getShowHitboxes();
    }

    private void respawnAtCheckpoint() {
        deathPaused         = false;
        deathTimer          = 0f;
        levelCompletedState = false;
        levelEndingSequence = false;
        levelEndTimer       = 0f;
        paused              = false;
        lastDelta           = 0f;
        lastJumpHeld        = false;

        float musicOffset = practice.applyLatestCheckpoint();
        engine.reset();
        music.stopAndDispose();
        music.start(musicOffset);
        recordAttempt();
        hitboxesActive = game.getSettingsManager().getShowHitboxes();
    }

    private void placeCheckpoint() {
        if (isPracticeMode) practice.placeCheckpoint();
    }

    private void removeLastCheckpoint() {
        if (!isPracticeMode) return;
        boolean stillHasCheckpoints = practice.removeLastCheckpoint();
        if (deathPaused && stillHasCheckpoints) triggerRespawn();
    }

    private void exitToLevelSelect() {
        music.stopAndDispose();
        game.setScreen(new LevelSelectScreen(game, levelIndex));
    }

    // ── Progress recording ────────────────────────────────────────────────────

    private void recordAttempt() {
        sessionAttempts++;
        if (levelKey == null) return;
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        p.setTotalAttempts(p.getTotalAttempts() + 1);
        game.getProgressManager().save();
    }

    private void recordDeath() {
        if (levelKey == null || isPracticeMode) return;
        int pct = Math.round(world.getProgress() * 100f);
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        if (pct > p.getBestPercent()) {
            p.setBestPercent(pct);
            game.getProgressManager().save();
            hud.showNewBestPopup(pct);
            checkAndShowAd(pct / 100f);
        }
    }

    private void recordComplete() {
        if (levelKey == null || isPracticeMode) return;
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        p.setBestPercent(100);
        game.getProgressManager().save();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleInput() {
        if (ignoreInputUntilRelease) {
            if (!Gdx.input.isTouched()) ignoreInputUntilRelease = false;
            return;
        }
        boolean jump =
            Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyPressed(Input.Keys.W)     ||
                Gdx.input.isKeyPressed(Input.Keys.UP)    ||
                Gdx.input.isTouched();

        if (jump != lastJumpHeld) {
            engine.queueInput(jump, engine.getAccumulator());
            lastJumpHeld = jump;
        }
    }

    // ── Ad helper ─────────────────────────────────────────────────────────────

    private void checkAndShowAd(float adChance) {
        if (TimeUtils.timeSinceMillis(lastAdTimeMillis) < AD_COOLDOWN_MS) return;
        if (MathUtils.randomBoolean(adChance) && game.getAdController() != null) {
            // game.getAdController().showInterstitialAd(); // TODO: ADS
            lastAdTimeMillis = TimeUtils.millis();
        }
    }

    // ── Camera helpers ────────────────────────────────────────────────────────

    private float camCX()  { return gameCamera.position.x; }
    private float camBot() { return gameCamera.position.y - gameViewport.getWorldHeight() / 2f; }
}
