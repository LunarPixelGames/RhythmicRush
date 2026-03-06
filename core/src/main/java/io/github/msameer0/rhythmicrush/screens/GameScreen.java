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
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;

public class GameScreen extends AbstractScreen {

    private GameWorld    world;
    private GameRenderer renderer;
    private BitmapFont   font;
    private GlyphLayout  glyphLayout;
    private Music        levelMusic;

    private OrthographicCamera gameCamera;
    private Viewport           gameViewport;

    private LevelData levelData;

    // ── Constructors ──────────────────────────────────────────────────────────

    public GameScreen(RhythmicRushGame game) {
        this(game, null);
    }

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
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void show() {
        game.getSoundManager().stopMenuMusic();
        startMusic();
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
    }

    @Override
    protected void update(float delta) {
        handleInput();
        if (levelData == null) handleDebugInput();

        world.update(delta);

        if (world.isPlayerDead()) {
            // stop and dispose current music, reset world, restart music
            stopAndDisposeMusic();
            world.reset();
            startMusic();
        }

        if (world.isLevelComplete()) {
            stopAndDisposeMusic();
            world.reset();
            game.setScreen(new MainMenuScreen(game)); // replace with results screen later
        }
    }

    @Override
    protected void draw() {
        gameViewport.apply();
        renderer.render();
        drawProgressBar();
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        renderer.dispose();
        stopAndDisposeMusic();
    }

    // ── Music ─────────────────────────────────────────────────────────────────

    /** Loads and plays the level music. Safe to call on respawn. */
    private void startMusic() {
        if (levelData == null ||
            levelData.musicFile == null ||
            levelData.musicFile.isEmpty()) return;
        try {
            FileHandle fh = Gdx.files.internal("musics/" + levelData.musicFile);
            if (!fh.exists()) fh = Gdx.files.local("assets/musics/" + levelData.musicFile);
            if (fh.exists()) {
                levelMusic = Gdx.audio.newMusic(fh);
                levelMusic.setLooping(false);
                levelMusic.play();
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Could not load music: " + e.getMessage());
        }
    }

    /** Stops, disposes, and nulls the music instance. */
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
        float x = (gameViewport.getScreenWidth() - glyphLayout.width) / 2f;
        float y = gameViewport.getScreenHeight() - 12f;
        font.draw(game.getBatch(), text, x, y);
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

    private void handleDebugInput() {
        AbstractPlayer player = world.getPlayer();
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET))
            world.addPortal(new CubePortal(player.getX() + 200, player.getY()));
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET))
            world.addPortal(new ShipPortal(player.getX() + 200, player.getY()));
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE))
            world.addHazard(new Spike(player.getX() + 200, world.getGroundY()));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
            world.addBlock(new Block(player.getX() + 200, player.getY(), player.width));
    }
}
