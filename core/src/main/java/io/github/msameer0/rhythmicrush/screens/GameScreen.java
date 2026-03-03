package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;

public class GameScreen implements Screen {
    private final RhythmicRushGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private GameWorld world;
    private GameRenderer renderer;

    public GameScreen(RhythmicRushGame game) {
        this.game = game;
        this.batch = game.getBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        world = new GameWorld();
        renderer = new GameRenderer(world, camera, batch);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput();
        handleDebugInput();

        world.update(delta);
        renderer.render();
    }

    private void handleDebugInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) { // [
            world.addPortal(new CubePortal(
                world.getPlayer().getX() + 200,
                world.getPlayer().getY()));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) { // ]
            world.addPortal(new ShipPortal(
                world.getPlayer().getX() + 200,
                world.getPlayer().getY()));
        }
    }

    private void handleInput() {
        AbstractPlayer player = world.getPlayer();

        // Check all jump inputs
        boolean jumpPressed = Gdx.input.isKeyPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyPressed(Input.Keys.W)
            || Gdx.input.isKeyPressed(Input.Keys.UP)
            || Gdx.input.isTouched(); // mouse left click or screen touch

        player.setJumpHeld(jumpPressed);
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
