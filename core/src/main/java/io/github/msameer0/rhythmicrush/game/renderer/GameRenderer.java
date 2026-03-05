package io.github.msameer0.rhythmicrush.game.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;

public class GameRenderer {

    private final GameWorld          world;
    private final OrthographicCamera camera;
    private final SpriteBatch        batch;
    private final ShapeRenderer      shape;

    // ── Block atlas ───────────────────────────────────────────────────────────
    private final TextureAtlas                   blocksAtlas;
    private final java.util.Map<BlockType, TextureRegion> blockRegions;

    private final TextureAtlas spikesAtlas;
    private final TextureRegion spikeRegion;


    // ─────────────────────────────────────────────────────────────────────────

    public GameRenderer(GameWorld world, OrthographicCamera camera, SpriteBatch batch) {
        this.world  = world;
        this.camera = camera;
        this.batch  = batch;
        this.shape  = new ShapeRenderer();

        // load block atlas and cache one region per BlockType — fully dynamic,
        // adding a new BlockType with a matching PNG automatically works here
        blocksAtlas  = new TextureAtlas(Gdx.files.internal("game/objects/blocks.atlas"));
        blockRegions = new java.util.EnumMap<>(BlockType.class);
        for (BlockType type : BlockType.values()) {
            TextureRegion region = blocksAtlas.findRegion(type.textureName);
            if (region != null) {
                blockRegions.put(type, region);
            } else {
                Gdx.app.error("GameRenderer", "Missing atlas region for block type: " + type.textureName);
            }
        }

        spikesAtlas = new TextureAtlas(Gdx.files.internal("game/objects/spikes.atlas"));
        spikeRegion = spikesAtlas.findRegion("spike");
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render() {
        camera.update();
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        float groundY    = world.getGroundY();
        float worldWidth = camera.viewportWidth;
        float worldLeft  = camera.position.x - worldWidth / 2f;

        // ── Ground ────────────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.2f, 0.2f, 0.5f, 1f);
        shape.rect(worldLeft, 0, worldWidth, groundY);
        shape.end();

        // ── Player ────────────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(1f, 0.5f, 0.2f, 1f);
        shape.rect(world.getPlayer().x, world.getPlayer().y,
            world.getPlayer().width, world.getPlayer().height);
        shape.end();

        // ── Portals ───────────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (AbstractPortal portal : world.getPortals()) {
            if (portal instanceof io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal)
                shape.setColor(0f, 0.8f, 0f, 1f);
            else
                shape.setColor(0f, 0.5f, 1f, 1f);
            shape.rect(portal.getX(), portal.getY(), portal.getWidth(), portal.getHeight());
        }
        shape.end();

//        // ── Hazards ───────────────────────────────────────────────────────────
//        shape.begin(ShapeRenderer.ShapeType.Filled);
//        shape.setColor(1f, 0f, 0f, 1f);
//        for (AbstractHazard hazard : world.getHazards()) {
//            shape.rect(hazard.getX(), hazard.getY(), hazard.getWidth(), hazard.getHeight());
//        }
//        shape.end();

        // replace the hazards shape draw with:
        // draw hazards
        batch.begin();
        for (AbstractHazard hazard : world.getHazards()) {
            if (spikeRegion != null) {
                batch.draw(spikeRegion, hazard.getX(), hazard.getY(),
                    hazard.getWidth(), hazard.getHeight());
            }
        }
        batch.end();

        // ── Blocks (textured) ─────────────────────────────────────────────────
        batch.begin();
        for (Block block : world.getBlocks()) {
            TextureRegion region = blockRegions.get(block.getType());
            if (region != null) {
                batch.draw(region, block.getX(), block.getY(), block.getWidth(), block.getHeight());
            } else {
                // fallback: region missing, draw nothing (atlas error already logged)
            }
        }
        batch.end();

        // ── Hitbox overlay (uncomment to enable, wire to a settings flag later) ──
        /*
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 1f, 0.75f); // blue at 75% opacity
        for (Block block : world.getBlocks()) {
            shape.rect(block.getX(), block.getY(), block.getWidth(), block.getHeight());
        }
        shape.end();
        */
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void dispose() {
        shape.dispose();
        blocksAtlas.dispose();
        spikesAtlas.dispose();
    }
}
