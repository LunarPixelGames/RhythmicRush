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
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.engine.FixedTickEngine;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;

public class GameScreen extends AbstractScreen {

    private static final float DEATH_PAUSE_DURATION = 0.75f;
    private static final float MUSIC_FADE_DURATION  = 3f;

    // ── Core ──────────────────────────────────────────────────────────────────
    private GameWorld        world;
    private FixedTickEngine  engine;
    private GameRenderer     renderer;
    private BitmapFont       font;
    private BitmapFont       pauseFont;
    private GlyphLayout      glyphLayout;
    private Music            levelMusic;

    private OrthographicCamera gameCamera;
    private Viewport           gameViewport;
    private LevelData          levelData;
    private int                levelIndex = 0;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean deathPaused    = false;
    private float   deathTimer     = 0f;
    private float   lastDelta      = 0f;
    private boolean musicFading    = false;
    private float   musicFadeTimer = 0f;
    private boolean paused         = false;

    /**
     * Tracks whether hitboxes should currently be shown.
     * Permanently true when showHitboxes is on; temporarily true after death
     * when showHitboxesOnDeath is on (cleared on next respawn).
     */
    private boolean hitboxesActive = false;

    // ── Progress ──────────────────────────────────────────────────────────────
    private int    sessionAttempts = 0;
    private String levelKey        = null;

    // Reusable StringBuilder — avoids a new String allocation every frame for HUD text
    private final StringBuilder _hudSb = new StringBuilder(32);

    // ── Pause overlay rendering ───────────────────────────────────────────────
    private ShapeRenderer shapes;
    private Texture       panelTexture;
    private int           lastPanelW = -1, lastPanelH = -1;

    private static final float PANEL_W   = 520f;
    private static final float PANEL_H   = 360f;
    private static final float BTN_SIZE  = 72f;
    private static final float PAUSE_BTN = 44f;

    private TextureRegion resumeRegion;
    private TextureRegion backRegion;

    private static final Color COL_OVERLAY    = new Color(0f,    0f,    0f,    0.65f);
    private static final Color COL_PANEL      = new Color(0.11f, 0.11f, 0.17f, 1f);
    private static final Color COL_HEADING    = new Color(1f,    0.85f, 0.35f, 1f);
    private static final Color COL_LABEL      = new Color(1f,    1f,    1f,    0.85f);
    private static final Color COL_DIM        = new Color(1f,    1f,    1f,    0.50f);
    private static final Color COL_TRACK      = new Color(0.28f, 0.28f, 0.35f, 1f);
    private static final Color COL_FILL       = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color COL_THUMB      = new Color(1f,    1f,    1f,    1f);
    // HUD text colors — static to avoid per-frame allocation
    private static final Color HUD_ATTEMPT    = new Color(1f,    1f,    1f,    0.85f);
    private static final Color HUD_BEST       = new Color(1f,    1f,    1f,    0.55f);
    private static final Color HUD_FPS        = new Color(1f,    1f,    1f,    0.45f);

    private boolean pauseSliderDragging = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameScreen(RhythmicRushGame game, LevelData levelData, int levelIndex) {
        super(game);
        this.levelData  = levelData;
        this.levelIndex = levelIndex;

        gameCamera   = new OrthographicCamera();
        gameViewport = new ExtendViewport(1280, 720, gameCamera);
        gameViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        world    = new GameWorld();
        engine   = new FixedTickEngine(world);
        renderer = new GameRenderer(world, gameCamera, game.getBatch(), game.getAtlasManager());
        font     = new BitmapFont();
        font.getData().setScale(1.5f);
        glyphLayout = new GlyphLayout();
        shapes      = new ShapeRenderer();

        pauseFont = loadFont(28);

        resumeRegion = game.getAtlasManager().getMenuAtlas().findRegion("start_button");
        backRegion   = game.getAtlasManager().getLevelSelectAtlas().findRegion("back");

        if (levelData != null) {
            world.loadLevel(levelData);
            levelKey = levelData.fileName != null ? levelData.fileName : levelData.name + ".json";
            recordAttempt();
        }

        hitboxesActive = game.getSettingsManager().showHitboxes;
    }

    // ── World-space helpers ───────────────────────────────────────────────────

    private float camCX()    { return gameCamera.position.x; }
    private float camCY()    { return gameCamera.position.y; }
    private float camLeft()  { return gameCamera.position.x - gameViewport.getWorldWidth()  / 2f; }
    private float camBot()   { return gameCamera.position.y - gameViewport.getWorldHeight() / 2f; }
    private float camRight() { return gameCamera.position.x + gameViewport.getWorldWidth()  / 2f; }
    private float camTop()   { return gameCamera.position.y + gameViewport.getWorldHeight() / 2f; }

    private float pauseSliderTrackX() { return panelX() + PANEL_W * 0.18f; }
    private float pauseSliderTrackW() { return PANEL_W * 0.64f; }
    private float pauseSliderY()      { return panelY() + BTN_SIZE + 38f; }

    private boolean hitsPauseSlider(Vector2 t) {
        float tx = pauseSliderTrackX(), tw = pauseSliderTrackW();
        float ty = pauseSliderY();
        return t.x >= tx - 16f && t.x <= tx + tw + 16f
            && t.y >= ty - 16f && t.y <= ty + 16f;
    }
    private float panelX()        { return camCX() - PANEL_W / 2f; }
    private float panelY()        { return camCY() - PANEL_H / 2f; }
    private float resumeX()       { return camCX() + 16f; }
    private float resumeY()       { return panelY() + 20f; }
    private float backX()         { return camCX() - BTN_SIZE - 16f; }
    private float backY()         { return panelY() + 20f; }
    private float pauseCircleCX() { return camRight() - PAUSE_BTN / 2f - 14f; }
    private float pauseCircleCY() { return camTop()   - PAUSE_BTN / 2f - 14f; }

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
        lastPanelW = -1;
    }

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        pauseFont.dispose();
        renderer.dispose();
        shapes.dispose();
        if (panelTexture != null) panelTexture.dispose();
        stopAndDisposeMusic();
        Gdx.input.setCursorCatched(false);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    protected void update(float delta) {

        // ── Pause input ────────────────────────────────────────────────────────
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (paused) exitToLevelSelect();
            else        setPaused(true);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && paused) {
            setPaused(false); return;
        }
        if (paused) {
            handlePauseTouched(); return;
        }

        // ── R = instant respawn (no death pause) ──────────────────────────────
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !deathPaused && !musicFading) {
            triggerRespawn();
            return;
        }

        // ── Death pause countdown ──────────────────────────────────────────────
        if (deathPaused) {
            deathTimer += delta;
            if (deathTimer >= DEATH_PAUSE_DURATION) {
                deathPaused    = false;
                deathTimer     = 0f;
                musicFading    = false;
                musicFadeTimer = 0f;
                lastJumpHeld   = false;
                world.reset();
                engine.reset();
                startMusic();
                recordAttempt();
                hitboxesActive = game.getSettingsManager().showHitboxes;
            }
            return;
        }

        lastDelta = delta;

        // ── Music fade (level complete) ────────────────────────────────────────
        if (musicFading && levelMusic != null) {
            musicFadeTimer += delta;
            float base   = game.getSettingsManager().musicVolume;
            float volume = base * (1f - Math.min(musicFadeTimer / MUSIC_FADE_DURATION, 1f));
            levelMusic.setVolume(volume);
            if (musicFadeTimer >= MUSIC_FADE_DURATION) {
                stopAndDisposeMusic();
                musicFading = false;
                world.reset();
                engine.reset();
                game.setScreen(new MainMenuScreen(game));
            }
            return;
        }

        // ── Check pause button touch ───────────────────────────────────────────
        if (Gdx.input.justTouched()) {
            Vector2 t = unprojectWorld();
            float cx = pauseCircleCX(), cy = pauseCircleCY(), r = PAUSE_BTN / 2f + 8f;
            float dx = t.x - cx, dy = t.y - cy;
            if (dx*dx + dy*dy <= r*r) { setPaused(true); return; }
        }

        handleInput();

        // ── Fixed-rate physics tick (240 TPS, decoupled from frame rate) ───────
        engine.update(delta);

        if (world.isPlayerDead()) {
            recordDeath();
            stopAndDisposeMusic();
            musicFading = false;
            deathPaused = true;
            deathTimer  = 0f;
            lastDelta   = 0f;
            engine.reset();
            if (game.getSettingsManager().showHitboxesOnDeath)
                hitboxesActive = true;
        }

        if (world.isLevelComplete()) {
            recordComplete();
            musicFading    = true;
            musicFadeTimer = 0f;
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    @Override
    protected void draw() {
        gameViewport.apply();

        Color bg = world.getBackgroundColor();
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.render(lastDelta, paused, hitboxesActive);

        drawProgressBar();
        drawSessionAttempts();
        drawPauseButton();

        if (paused) drawPauseOverlay();
    }

    // ── Pause button ──────────────────────────────────────────────────────────

    private void drawPauseButton() {
        float cx = pauseCircleCX(), cy = pauseCircleCY(), r = PAUSE_BTN / 2f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(gameCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.2f, 0.2f, 0.2f, 0.75f);
        shapes.circle(cx, cy, r, 32);
        float barW = r * 0.22f, barH = r * 0.75f;
        float gap  = r * 0.18f;
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.rect(cx - gap - barW, cy - barH / 2f, barW, barH);
        shapes.rect(cx + gap,        cy - barH / 2f, barW, barH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Pause overlay ─────────────────────────────────────────────────────────

    private void drawPauseOverlay() {
        float px = panelX(), py = panelY();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(gameCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_OVERLAY);
        shapes.rect(camLeft(), camBot(), gameViewport.getWorldWidth(), gameViewport.getWorldHeight());
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        int texW = (int) PANEL_W, texH = (int) PANEL_H;
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();
            panelTexture = createRoundedRect(texW, texH, 24, COL_PANEL);
            lastPanelW = texW; lastPanelH = texH;
        }

        game.getBatch().setProjectionMatrix(gameCamera.combined);
        game.getBatch().begin();
        game.getBatch().draw(panelTexture, px, py);

        String levelName = (levelData != null && levelData.name != null) ? levelData.name : "Level";
        pauseFont.getData().setScale(1f);
        pauseFont.setColor(COL_HEADING);
        glyphLayout.setText(pauseFont, levelName);
        pauseFont.draw(game.getBatch(), levelName,
            px + PANEL_W / 2f - glyphLayout.width / 2f,
            py + PANEL_H - 18f);

        float sy = py + PANEL_H - 18f - glyphLayout.height - 22f;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            pauseFont.getData().setScale(0.62f);
            pauseFont.setColor(COL_LABEL);
            String best = "Personal Best: " + p.bestPercent + "%";
            glyphLayout.setText(pauseFont, best);
            pauseFont.draw(game.getBatch(), best, px + PANEL_W / 2f - glyphLayout.width / 2f, sy);
            sy -= glyphLayout.height + 12f;
            pauseFont.setColor(COL_DIM);
            String att = "Total: " + p.totalAttempts + "   Session: " + sessionAttempts;
            glyphLayout.setText(pauseFont, att);
            pauseFont.draw(game.getBatch(), att, px + PANEL_W / 2f - glyphLayout.width / 2f, sy);
        }

        if (backRegion != null)
            game.getBatch().draw(backRegion, backX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE * 0.9f);
        if (resumeRegion != null)
            game.getBatch().draw(resumeRegion, resumeX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE * 0.9f);

        // Volume label
        float sliderY = pauseSliderY();
        pauseFont.getData().setScale(0.52f);
        pauseFont.setColor(COL_LABEL);
        _hudSb.setLength(0); _hudSb.append("Volume");
        glyphLayout.setText(pauseFont, _hudSb);
        pauseFont.draw(game.getBatch(), _hudSb,
            panelX() + PANEL_W / 2f - glyphLayout.width / 2f,
            sliderY + glyphLayout.height + 10f);

        // Percent
        float vol = game.getSettingsManager().musicVolume;
        pauseFont.getData().setScale(0.44f);
        pauseFont.setColor(COL_DIM);
        _hudSb.setLength(0); _hudSb.append(Math.round(vol * 100f)).append('%');
        glyphLayout.setText(pauseFont, _hudSb);
        pauseFont.draw(game.getBatch(), _hudSb,
            pauseSliderTrackX() - glyphLayout.width - 10f,
            sliderY + glyphLayout.height / 2f);

        pauseFont.getData().setScale(1f);
        game.getBatch().end();

        // Slider shape
        float tx = pauseSliderTrackX(), tw = pauseSliderTrackW();
        float trackH = 5f, thumbR = 10f;
        float fillW = tw * vol;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(gameCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_TRACK);
        shapes.rect(tx, sliderY - trackH / 2f, tw, trackH);
        shapes.setColor(COL_FILL);
        if (fillW > 0) shapes.rect(tx, sliderY - trackH / 2f, fillW, trackH);
        shapes.setColor(COL_THUMB);
        shapes.circle(tx + fillW, sliderY, thumbR, 24);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.getBatch().setProjectionMatrix(gameCamera.combined);
        game.getBatch().begin();

        pauseFont.getData().setScale(0.45f);
        pauseFont.setColor(COL_DIM);
        glyphLayout.setText(pauseFont, "Back");
        pauseFont.draw(game.getBatch(), "Back",
            backX()   + BTN_SIZE * 0.9f / 2f - glyphLayout.width / 2f, backY() - 4f);
        glyphLayout.setText(pauseFont, "Resume");
        pauseFont.draw(game.getBatch(), "Resume",
            resumeX() + BTN_SIZE * 0.9f / 2f - glyphLayout.width / 2f, backY() - 4f);

        pauseFont.getData().setScale(1f);
        game.getBatch().end();
    }

    // ── Pause helpers ─────────────────────────────────────────────────────────

    private void setPaused(boolean p) {
        paused = p;
        if (!p) pauseSliderDragging = false;
        if (levelMusic != null) { if (paused) levelMusic.pause(); else levelMusic.play(); }
        if (game.getSettingsManager().lockCursorInGame)
            Gdx.input.setCursorCatched(!paused);
    }

    private void handlePauseTouched() {
        Vector2 t = unprojectWorld();

        // Slider drag
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
        if (hitsPauseSlider(t)) { pauseSliderDragging = true; return; }
        if (hits(t, backX(),   backY(), BTN_SIZE * 0.9f, BTN_SIZE)) { exitToLevelSelect(); return; }
        if (hits(t, resumeX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE)) { setPaused(false);    return; }
    }

    private void exitToLevelSelect() {
        stopAndDisposeMusic();
        game.setScreen(new LevelSelectScreen(game, levelIndex));
    }

    private void triggerRespawn() {
        recordDeath();
        stopAndDisposeMusic();
        deathPaused    = false;
        deathTimer     = 0f;
        musicFading    = false;
        musicFadeTimer = 0f;
        lastDelta      = 0f;
        lastJumpHeld   = false;
        world.reset();
        engine.reset();
        startMusic();
        recordAttempt();
        hitboxesActive = game.getSettingsManager().showHitboxes;
    }

    // ── Progress tracking ─────────────────────────────────────────────────────

    private void recordAttempt() {
        sessionAttempts++;
        if (levelKey == null) return;
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        p.totalAttempts++;
        game.getProgressManager().save();
    }

    private void recordDeath() {
        if (levelKey == null) return;
        int pct = Math.round(world.getProgress() * 100f);
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        if (pct > p.bestPercent) { p.bestPercent = pct; game.getProgressManager().save(); }
    }

    private void recordComplete() {
        if (levelKey == null) return;
        LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
        p.bestPercent = 100;
        game.getProgressManager().save();
    }

    // ── HUD ───────────────────────────────────────────────────────────────────

    private void drawProgressBar() {
        float progress = world.getProgress();
        if (progress <= 0f) return;
        String text = Math.round(progress * 100f) + "%";
        game.getBatch().setProjectionMatrix(gameCamera.combined);
        game.getBatch().begin();
        font.setColor(Color.WHITE);
        glyphLayout.setText(font, text, Color.WHITE, 0, Align.center, false);
        float x = gameCamera.position.x - glyphLayout.width / 2f;
        float y = gameCamera.position.y + gameViewport.getWorldHeight() / 2f - 12f;
        font.draw(game.getBatch(), text, x, y);
        game.getBatch().end();
    }

    private void drawSessionAttempts() {
        float left = gameCamera.position.x - gameViewport.getWorldWidth()  / 2f + 12f;
        float top  = gameCamera.position.y + gameViewport.getWorldHeight() / 2f - 12f;
        game.getBatch().setProjectionMatrix(gameCamera.combined);
        game.getBatch().begin();

        font.setColor(HUD_ATTEMPT);
        _hudSb.setLength(0); _hudSb.append("Attempt  ").append(sessionAttempts);
        font.draw(game.getBatch(), _hudSb, left, top);

        float nextY = top - 26f;
        if (levelKey != null) {
            LevelProgress p = game.getProgressManager().getOrCreate(levelKey);
            font.setColor(HUD_BEST);
            _hudSb.setLength(0); _hudSb.append("Best  ").append(p.bestPercent).append('%');
            font.draw(game.getBatch(), _hudSb, left, nextY);
            nextY -= 26f;
        }

        if (game.getSettingsManager().showFps) {
            font.setColor(HUD_FPS);
            _hudSb.setLength(0); _hudSb.append("FPS  ").append(Gdx.graphics.getFramesPerSecond());
            font.draw(game.getBatch(), _hudSb, left, nextY);
        }

        game.getBatch().end();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    // Tracks previous jump state so we only queue events on state *change*
    private boolean lastJumpHeld = false;

    private void handleInput() {
        boolean jump =
            Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyPressed(Input.Keys.W)     ||
                Gdx.input.isKeyPressed(Input.Keys.UP)    ||
                Gdx.input.isTouched();

        if (jump != lastJumpHeld) {
            // Queue the event with the current accumulator offset so it gets
            // delivered at the physics step closest to when it was detected.
            engine.queueInput(jump, engine.getAccumulator());
            lastJumpHeld = jump;
        }
    }

    // ── Music ─────────────────────────────────────────────────────────────────

    private void startMusic() {
        if (levelData == null || levelData.musicFile == null || levelData.musicFile.isEmpty()) return;
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Reusable vectors — avoids allocation every frame
    private final com.badlogic.gdx.math.Vector3 _unprojectTmp = new com.badlogic.gdx.math.Vector3();

    private Vector2 unprojectWorld() {
        _unprojectTmp.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        gameCamera.unproject(_unprojectTmp);
        return new Vector2(_unprojectTmp.x, _unprojectTmp.y);
    }

    private static boolean hits(Vector2 t, float x, float y, float w, float h) {
        return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h;
    }

    private static boolean hits(float tx, float ty, float x, float y, float w, float h) {
        return tx >= x && tx <= x + w && ty >= y && ty <= y + h;
    }

    private BitmapFont loadFont(int size) {
        try {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/zendots-regular.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter p =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size; p.magFilter = Texture.TextureFilter.Linear;
            p.minFilter = Texture.TextureFilter.Linear;
            BitmapFont f = gen.generateFont(p); gen.dispose(); return f;
        } catch (Exception e) { return new BitmapFont(); }
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
}
