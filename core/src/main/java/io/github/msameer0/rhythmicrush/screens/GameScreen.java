package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;

public class GameScreen extends AbstractScreen {

    private static final float DEATH_PAUSE_DURATION = 0.75f;

    private GameWorld    world;
    private GameRenderer renderer;
    private BitmapFont   font;
    private GlyphLayout  glyphLayout;
    private Music        levelMusic;

    private OrthographicCamera gameCamera;
    private Viewport           gameViewport;

    private LevelData levelData;

    private boolean deathPaused = false;
    private float   deathTimer  = 0f;
    private float   lastDelta   = 0f;

    private static final float MUSIC_FADE_DURATION = 3f;
    private boolean musicFading   = false;
    private float   musicFadeTimer = 0f;

    // ── Progress tracking ─────────────────────────────────────────────────────
    /** Attempts made during this play session (resets when screen is constructed). */
    private int sessionAttempts = 0;
    /** Level key used to look up / save progress — derived from levelData filename. */
    private String levelKey = null;

    // ── Constructors ──────────────────────────────────────────────────────────

    public GameScreen(RhythmicRushGame game, LevelData levelData) {
        super(game);
        this.levelData = levelData;

        gameCamera   = new OrthographicCamera();
        gameViewport = new ExtendViewport(1280, 720, gameCamera);
        gameViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        world    = new GameWorld();
        renderer = new GameRenderer(world, gameCamera, game.getBatch(), game.getAtlasManager());
        font     = new BitmapFont();
        font.getData().setScale(1.5f);
        glyphLayout = new GlyphLayout();

        if (levelData != null) {
            world.loadLevel(levelData);
            // derive a stable key from the level filename (e.g. "0.json")
            levelKey = levelData.fileName != null ? levelData.fileName : levelData.name + ".json";
            recordAttempt(); // count entering the level as attempt #1
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void show() {
        game.getSoundManager().stopMenuMusic();
        startMusic();
        if (game.getSettingsManager().lockCursorInGame)
            Gdx.input.setCursorCatched(true);
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
    }

    @Override
    protected void update(float delta) {
        if (deathPaused) {
            deathTimer += delta;
            if (deathTimer >= DEATH_PAUSE_DURATION) {
                deathPaused    = false;
                deathTimer     = 0f;
                musicFading    = false;
                musicFadeTimer = 0f;
                world.reset();
                startMusic();
                recordAttempt(); // each respawn is a new attempt
            }
            return;
        }

        lastDelta = delta;

        // advance music fade if active
        if (musicFading && levelMusic != null) {
            musicFadeTimer += delta;
            float baseVolume = game.getSettingsManager().musicVolume;
            float volume = baseVolume * (1f - Math.min(musicFadeTimer / MUSIC_FADE_DURATION, 1f));
            levelMusic.setVolume(volume);
            if (musicFadeTimer >= MUSIC_FADE_DURATION) {
                stopAndDisposeMusic();
                musicFading = false;
                world.reset();
                game.setScreen(new MainMenuScreen(game));
            }
            return; // don't process anything else while fading out
        }

        handleInput();
        world.update(delta);

        if (world.isPlayerDead()) {
            recordDeath();
            stopAndDisposeMusic();
            musicFading = false;
            deathPaused = true;
            deathTimer  = 0f;
            lastDelta   = 0f;
        }

        if (world.isLevelComplete()) {
            recordComplete();
            // start fade — transition to menu happens after fade finishes
            musicFading    = true;
            musicFadeTimer = 0f;
        }
    }

    @Override
    protected void draw() {
        gameViewport.apply();
        // clear with world's current bg color — respects color triggers
        com.badlogic.gdx.graphics.Color bg = world.getBackgroundColor();
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
        renderer.render(lastDelta);
        drawProgressBar();
        drawSessionAttempts();
    }

    // ── Progress tracking helpers ─────────────────────────────────────────────

    /** Increments session and total attempt counters and saves. */
    private void recordAttempt() {
        sessionAttempts++;
        if (levelKey == null) return;
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        p.totalAttempts++;
        game.getProgressManager().save();
    }

    /** Updates best percent on death, then saves. */
    private void recordDeath() {
        if (levelKey == null) return;
        int currentPercent = Math.round(world.getProgress() * 100f);
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        if (currentPercent > p.bestPercent) {
            p.bestPercent = currentPercent;
            game.getProgressManager().save();
        }
    }

    /** Marks 100% best on level complete, then saves. */
    private void recordComplete() {
        if (levelKey == null) return;
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        p.bestPercent = 100;
        game.getProgressManager().save();
    }

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        renderer.dispose();
        stopAndDisposeMusic();
        Gdx.input.setCursorCatched(false); // always release cursor on exit
    }

    // ── Music ─────────────────────────────────────────────────────────────────

    private void startMusic() {
        if (levelData == null ||
            levelData.musicFile == null ||
            levelData.musicFile.isEmpty()) return;
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

    private void stopAndDisposeMusic() {
        if (levelMusic != null) {
            if (levelMusic.isPlaying()) levelMusic.stop();
            levelMusic.dispose();
            levelMusic = null;
        }
    }

    // ── Progress HUD ──────────────────────────────────────────────────────────

    private void drawProgressBar() {
        float progress = world.getProgress();
        if (progress <= 0f) return;

        int percent = Math.round(progress * 100f);
        String text = percent + "%";

        game.getBatch().setProjectionMatrix(gameCamera.combined);
        game.getBatch().begin();
        font.setColor(Color.WHITE);
        glyphLayout.setText(font, text, Color.WHITE, 0, Align.center, false);
        float x = gameCamera.position.x - glyphLayout.width / 2f;
        float y = gameCamera.position.y + gameViewport.getWorldHeight() / 2f - 12f;
        font.draw(game.getBatch(), text, x, y);
        game.getBatch().end();
    }

    /** Draws session attempts (top-left) and best % (below it). */
    private void drawSessionAttempts() {
        float left = gameCamera.position.x - gameViewport.getWorldWidth() / 2f + 12f;
        float top  = gameCamera.position.y + gameViewport.getWorldHeight() / 2f - 12f;

        game.getBatch().setProjectionMatrix(gameCamera.combined);
        game.getBatch().begin();
        font.setColor(new Color(1f, 1f, 1f, 0.85f));
        font.draw(game.getBatch(), "Attempt  " + sessionAttempts, left, top);

        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            font.setColor(new Color(1f, 1f, 1f, 0.55f));
            font.draw(game.getBatch(), "Best  " + p.bestPercent + "%", left, top - 26f);
        }
        game.getBatch().end();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleInput() {
        AbstractPlayer player = world.getPlayer();
        boolean jumpPressed =
            Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyPressed(Input.Keys.W)     ||
                Gdx.input.isKeyPressed(Input.Keys.UP)    ||
                Gdx.input.isTouched();
        player.setJumpHeld(jumpPressed);
    }
}
