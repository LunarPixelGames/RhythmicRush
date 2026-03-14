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
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;

public class GameScreen extends AbstractScreen {

    private static final float DEATH_PAUSE_DURATION = 0.75f;
    private static final float MUSIC_FADE_DURATION  = 3f;

    // ── Core ──────────────────────────────────────────────────────────────────
    private GameWorld    world;
    private GameRenderer renderer;
    private BitmapFont   font;
    private BitmapFont   pauseFont;
    private GlyphLayout  glyphLayout;
    private Music        levelMusic;

    private OrthographicCamera gameCamera;
    private Viewport           gameViewport;
    private LevelData          levelData;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean deathPaused    = false;
    private float   deathTimer     = 0f;
    private float   lastDelta      = 0f;
    private boolean musicFading    = false;
    private float   musicFadeTimer = 0f;
    private boolean paused         = false;

    /**
     * Tracks whether hitboxes should currently be shown.
     * Set to true permanently if showHitboxes is on, or temporarily
     * after death if showHitboxesOnDeath is on (cleared on next respawn).
     */
    private boolean hitboxesActive = false;

    // ── Progress ──────────────────────────────────────────────────────────────
    private int    sessionAttempts = 0;
    private String levelKey        = null;

    // ── Pause overlay rendering ───────────────────────────────────────────────
    private ShapeRenderer  shapes;
    private Texture        panelTexture;
    private int            lastPanelW = -1, lastPanelH = -1;

    private static final float PANEL_W   = 520f;
    private static final float PANEL_H   = 300f;
    private static final float BTN_SIZE  = 72f;
    private static final float PAUSE_BTN = 44f;

    private TextureRegion resumeRegion;
    private TextureRegion backRegion;

    private static final Color COL_OVERLAY = new Color(0f,    0f,    0f,    0.65f);
    private static final Color COL_PANEL   = new Color(0.11f, 0.11f, 0.17f, 1f);
    private static final Color COL_HEADING = new Color(1f,    0.85f, 0.35f, 1f);
    private static final Color COL_LABEL   = new Color(1f,    1f,    1f,    0.85f);
    private static final Color COL_DIM     = new Color(1f,    1f,    1f,    0.50f);

    // ── Constructor ───────────────────────────────────────────────────────────

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
        shapes      = new ShapeRenderer();

        pauseFont = loadFont(28);

        resumeRegion = game.getAtlasManager().getMenuAtlas().findRegion("start_button");
        backRegion   = game.getAtlasManager().getLevelSelectAtlas().findRegion("back");

        if (levelData != null) {
            world.loadLevel(levelData);
            levelKey = levelData.fileName != null ? levelData.fileName : levelData.name + ".json";
            recordAttempt();
        }

        // Initialise hitbox state from settings
        hitboxesActive = game.getSettingsManager().showHitboxes;
    }

    // ── World-space helpers ───────────────────────────────────────────────────

    private float camCX()    { return gameCamera.position.x; }
    private float camCY()    { return gameCamera.position.y; }
    private float camLeft()  { return gameCamera.position.x - gameViewport.getWorldWidth()  / 2f; }
    private float camBot()   { return gameCamera.position.y - gameViewport.getWorldHeight() / 2f; }
    private float camRight() { return gameCamera.position.x + gameViewport.getWorldWidth()  / 2f; }
    private float camTop()   { return gameCamera.position.y + gameViewport.getWorldHeight() / 2f; }

    private float panelX()       { return camCX() - PANEL_W / 2f; }
    private float panelY()       { return camCY() - PANEL_H / 2f; }
    private float resumeX()      { return camCX() + 16f; }
    private float resumeY()      { return panelY() + 20f; }
    private float backX()        { return camCX() - BTN_SIZE - 16f; }
    private float backY()        { return panelY() + 20f; }
    private float pauseCircleCX(){ return camRight() - PAUSE_BTN / 2f - 14f; }
    private float pauseCircleCY(){ return camTop()   - PAUSE_BTN / 2f - 14f; }

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

        // ── R = instant respawn ────────────────────────────────────────────────
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
                world.reset();
                startMusic();
                recordAttempt();
                // Clear the on-death hitbox flag now that we've respawned
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
        world.update(delta);

        if (world.isPlayerDead()) {
            recordDeath();
            stopAndDisposeMusic();
            musicFading = false;
            deathPaused = true;
            deathTimer  = 0f;
            lastDelta   = 0f;
            // Activate hitboxes on death if the setting is enabled
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

        // Pass both paused and hitbox flags to the renderer
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
        float gap   = r * 0.18f;
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
        if (levelMusic != null) { if (paused) levelMusic.pause(); else levelMusic.play(); }
        if (game.getSettingsManager().lockCursorInGame)
            Gdx.input.setCursorCatched(!paused);
    }

    private void handlePauseTouched() {
        if (!Gdx.input.justTouched()) return;
        Vector2 t = unprojectWorld();
        if (hits(t, backX(),   backY(), BTN_SIZE * 0.9f, BTN_SIZE)) { exitToLevelSelect(); return; }
        if (hits(t, resumeX(), backY(), BTN_SIZE * 0.9f, BTN_SIZE)) { setPaused(false);    return; }
    }

    private void exitToLevelSelect() {
        stopAndDisposeMusic();
        game.setScreen(new LevelSelectScreen(game));
    }

    private void triggerRespawn() {
        recordDeath();
        stopAndDisposeMusic();
        deathPaused    = true;
        deathTimer     = 0f;
        musicFading    = false;
        musicFadeTimer = 0f;
        lastDelta      = 0f;
        // If showHitboxesOnDeath is on, show them during the death pause too
        if (game.getSettingsManager().showHitboxesOnDeath)
            hitboxesActive = true;
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
        boolean jump =
            Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyPressed(Input.Keys.W)     ||
                Gdx.input.isKeyPressed(Input.Keys.UP)    ||
                Gdx.input.isTouched();
        player.setJumpHeld(jump);
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

    private Vector2 unprojectWorld() {
        com.badlogic.gdx.math.Vector3 v = new com.badlogic.gdx.math.Vector3(
            Gdx.input.getX(), Gdx.input.getY(), 0);
        gameCamera.unproject(v);
        return new Vector2(v.x, v.y);
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
