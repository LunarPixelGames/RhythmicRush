package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;

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

    /** Optional — if non-null, the level was loaded from the editor. */
    private LevelData levelData;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Default constructor — no level data (debug / freeplay mode). */
    public GameScreen(RhythmicRushGame game) {
        this(game, null);
    }

    /** Constructor used by the level editor to playtest a level. */
    public GameScreen(RhythmicRushGame game, LevelData levelData) {
        super(game);
        this.levelData = levelData;

        world    = new GameWorld();
        renderer = new GameRenderer(world, camera, game.getBatch());
        font     = new BitmapFont();
        font.getData().setScale(1.5f);
        glyphLayout = new GlyphLayout();

        if (levelData != null) {
            world.loadLevel(levelData);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void update(float delta) {
        handleInput();
        if (levelData == null) handleDebugInput(); // debug objects only in freeplay

        world.update(delta);

        if (world.isPlayerDead()) {
            game.setScreen(new MainMenuScreen(game));
        }

        if (world.isLevelComplete()) {
            game.setScreen(new MainMenuScreen(game)); // replace with a results screen later
        }
    }

    @Override
    protected void draw() {
        renderer.render();
        drawProgressBar();
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }

    // ── Progress HUD ─────────────────────────────────────────────────────────

    private void drawProgressBar() {
        float progress = world.getProgress();
        if (progress <= 0f) return; // no level loaded, don't show

        int percent = Math.round(progress * 100f);
        String text = percent + "%";

        game.getBatch().begin();

        font.setColor(Color.WHITE);
        glyphLayout.setText(font, text, Color.WHITE, 0, Align.center, false);

        float screenW = Gdx.graphics.getWidth();
        float x = (screenW - glyphLayout.width) / 2f;
        float y = Gdx.graphics.getHeight() - 12f;

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

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            world.addPortal(new CubePortal(player.getX() + 200, player.getY()));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            world.addPortal(new ShipPortal(player.getX() + 200, player.getY()));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            world.addHazard(new Spike(player.getX() + 200, world.getGroundY()));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            world.addBlock(new Block(player.getX() + 200, player.getY(), player.width));
        }
    }

    @Override
    public void show() {
        game.getSoundManager().stopMenuMusic();
    }
}
