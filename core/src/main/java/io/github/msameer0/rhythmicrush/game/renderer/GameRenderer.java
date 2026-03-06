package io.github.msameer0.rhythmicrush.game.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.msameer0.rhythmicrush.AtlasManager;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;

import java.util.EnumMap;
import java.util.Map;

public class GameRenderer {

    private final GameWorld          world;
    private final OrthographicCamera camera;
    private final SpriteBatch        batch;
    private final ShapeRenderer      shape;

    // regions pulled from AtlasManager — not owned here, never disposed here
    private final Map<BlockType, TextureRegion> blockRegions;
    private final TextureRegion                 spikeRegion;

    public GameRenderer(GameWorld world, OrthographicCamera camera,
                        SpriteBatch batch, AtlasManager atlasManager) {
        this.world  = world;
        this.camera = camera;
        this.batch  = batch;
        this.shape  = new ShapeRenderer();

        blockRegions = new EnumMap<>(BlockType.class);
        for (BlockType type : BlockType.values()) {
            TextureRegion r = atlasManager.getBlocksAtlas().findRegion(type.textureName);
            if (r != null) blockRegions.put(type, r);
        }
        spikeRegion = atlasManager.getSpikesAtlas().findRegion("spike");
    }

    public void render() {
        camera.update();
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        float groundY    = world.getGroundY();
        float worldWidth = camera.viewportWidth;
        float worldLeft  = camera.position.x - worldWidth / 2f;

        // Ground
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.2f, 0.2f, 0.5f, 1f);
        shape.rect(worldLeft, 0, worldWidth, groundY);
        shape.end();

        // Player
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(1f, 0.5f, 0.2f, 1f);
        shape.rect(world.getPlayer().x, world.getPlayer().y,
            world.getPlayer().width, world.getPlayer().height);
        shape.end();

        // Portals
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (AbstractPortal portal : world.getPortals()) {
            shape.setColor(portal instanceof CubePortal
                ? new com.badlogic.gdx.graphics.Color(0f, 0.8f, 0f, 1f)
                : new com.badlogic.gdx.graphics.Color(0f, 0.5f, 1f, 1f));
            shape.rect(portal.getX(), portal.getY(), portal.getWidth(), portal.getHeight());
        }
        shape.end();

        // Spikes (textured, rotation aware)
        batch.begin();
        for (AbstractHazard hazard : world.getHazards()) {
            if (hazard instanceof Spike && spikeRegion != null) {
                Spike spike = (Spike) hazard;
                batch.draw(spikeRegion,
                    hazard.getX(), hazard.getY(),
                    hazard.getWidth()  / 2f,
                    hazard.getHeight() / 2f,
                    hazard.getWidth(), hazard.getHeight(),
                    1f, 1f,
                    spike.getRotation());
            }
        }
        batch.end();

        // Blocks (textured)
        batch.begin();
        for (Block block : world.getBlocks()) {
            TextureRegion region = blockRegions.get(block.getType());
            if (region != null) {
                batch.draw(region, block.getX(), block.getY(),
                    block.getWidth(), block.getHeight());
            }
        }
        batch.end();
    }

    // Only dispose what GameRenderer created — ShapeRenderer only
    // Atlases are owned by AtlasManager, never disposed here
    public void dispose() {
        shape.dispose();
    }
}
