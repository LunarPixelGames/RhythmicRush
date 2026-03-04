package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;

public class GameScreen extends AbstractScreen {

    private GameWorld world;
    private GameRenderer renderer;

    public GameScreen(RhythmicRushGame game) {
        super(game);

        world = new GameWorld();
        renderer = new GameRenderer(world, camera, game.getBatch());
    }

    @Override
    protected void update(float delta) {
        handleInput();
        handleDebugInput();

        world.update(delta);

        if (world.isPlayerDead()) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    @Override
    protected void draw() {
        renderer.render();
    }

    private void handleDebugInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            world.addPortal(new CubePortal(
                world.getPlayer().getX() + 200,
                world.getPlayer().getY()));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            world.addPortal(new ShipPortal(
                world.getPlayer().getX() + 200,
                world.getPlayer().getY()));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            world.addHazard(new Spike(
                world.getPlayer().getX() + 200,
                world.getGroundY()
            ));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            float size = world.getPlayer().width; // block same size as player
            world.addBlock(new Block(world.getPlayer().getX() + 200, world.getPlayer().getY(), size));
        }
    }

    private void handleInput() {
        AbstractPlayer player = world.getPlayer();

        boolean jumpPressed =
            Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyPressed(Input.Keys.W) ||
                Gdx.input.isKeyPressed(Input.Keys.UP) ||
                Gdx.input.isTouched();

        player.setJumpHeld(jumpPressed);
    }
}
