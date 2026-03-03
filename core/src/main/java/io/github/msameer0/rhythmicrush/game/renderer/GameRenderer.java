package io.github.msameer0.rhythmicrush.game.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;

public class GameRenderer {
    private GameWorld world;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shape;

    public GameRenderer(GameWorld world, OrthographicCamera camera, SpriteBatch batch) {
        this.world = world;
        this.camera = camera;
        this.batch = batch;
        this.shape = new ShapeRenderer();
    }

    public void render() {
        camera.update();

        // draw ground
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.2f, 0.2f, 0.5f, 1f);
        shape.rect(0, world.getGroundY(), 2000, 50); // long ground for testing
        shape.end();

        // draw player
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(1f, 0.5f, 0.2f, 1f);
        shape.rect(world.getPlayer().x, world.getPlayer().y,
            world.getPlayer().width, world.getPlayer().height);
        shape.end();

        // draw portals
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (AbstractPortal portal : world.getPortals()) {
            if (portal instanceof io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal)
                shape.setColor(0f, 0.8f, 0f, 1f); // green
            else
                shape.setColor(0f, 0.5f, 1f, 1f); // blue
            shape.rect(portal.getX(), portal.getY(), portal.getWidth(), portal.getHeight());
        }
        shape.end();
    }
}
