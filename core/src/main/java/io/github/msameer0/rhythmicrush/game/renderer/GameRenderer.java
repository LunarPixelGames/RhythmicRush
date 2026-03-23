package io.github.msameer0.rhythmicrush.game.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.atlas.AtlasManager;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * Responsible for rendering the visual representation of the game world.
 * <p>
 * This class handles the drawing of all game elements including the player, blocks,
 * hazards, and portals. It manages camera positioning, player-specific visual rotations
 * (such as cube spinning and ship tilting), and provides optional debug rendering
 * for hitboxes.
 * </p>
 *
 * <p>
 * It utilizes {@link SpriteBatch} for texture-based
 * rendering and {@link ShapeRenderer} for
 * primitive shapes like the ground and debug overlays.
 * </p>
 */
public class GameRenderer {

    private final GameWorld world;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;

    private final TextureRegion[] blockRegionsByOrdinal;

    private final TextureRegion spikeRegion;
    private final TextureRegion cubeRegion;
    private final TextureRegion shipRegion;
    private final TextureRegion cubePortalRegion;
    private final TextureRegion shipPortalRegion;
    private final TextureRegion gravityPortalRegion;
    private final TextureRegion miniPortalRegion;

    private static final Color FALLBACK_CUBE_PORTAL = new Color(0f, 0.8f, 0f, 1f);
    private static final Color FALLBACK_SHIP_PORTAL = new Color(0f, 0.5f, 1f, 1f);

    private float playerVisualRotation = 0f;

    private static final float CAMERA_X_OFFSET = 425f;
    private static final float CUBE_SPIN_FACTOR = 0.5f;
    private static final float SHIP_TILT_FACTOR = 0.18f;
    private static final float SHIP_MAX_TILT = 50f;
    private static final float SHIP_TILT_LERP = 8f;

    private static final Color HB_PLAYER_FILL = new Color(1.0f, 0.9f, 0.0f, 0.75f);
    private static final Color HB_HAZARD_FILL = new Color(1.0f, 0.2f, 0.2f, 0.75f);
    private static final Color HB_BLOCK_FILL = new Color(0.2f, 0.5f, 1.0f, 0.75f);
    private static final Color HB_PORTAL_FILL = new Color(0.2f, 1.0f, 0.4f, 0.75f);

    private static final Color HB_PLAYER_LINE = new Color(1.0f, 0.9f, 0.0f, 1.0f);
    private static final Color HB_HAZARD_LINE = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color HB_BLOCK_LINE = new Color(0.2f, 0.5f, 1.0f, 1.0f);
    private static final Color HB_PORTAL_LINE = new Color(0.2f, 1.0f, 0.4f, 1.0f);

    /**
     * Constructs a new GameRenderer and initializes the required texture regions from the provided atlases.
     *
     * @param world The game world containing the entities to be rendered.
     * @param camera The camera used for viewing the game world.
     * @param batch The sprite batch used for drawing textures.
     * @param atlasManager The manager providing access to various texture atlases for blocks,
     *                     hazards, players, and portals.
     */
    public GameRenderer(GameWorld world, OrthographicCamera camera,
                        SpriteBatch batch, AtlasManager atlasManager) {
        this.world = world;
        this.camera = camera;
        this.batch = batch;
        this.shape = new ShapeRenderer();

        BlockType[] types = BlockType.values();
        blockRegionsByOrdinal = new TextureRegion[types.length];
        for (BlockType type : types) {
            blockRegionsByOrdinal[type.ordinal()] =
                atlasManager.getBlocksAtlas().findRegion(type.textureName);
        }

        spikeRegion = atlasManager.getSpikesAtlas().findRegion("spike");
        cubeRegion = atlasManager.getGamemodesAtlas().findRegion("cube");
        shipRegion = atlasManager.getGamemodesAtlas().findRegion("ship");
        cubePortalRegion = atlasManager.getPortalsAtlas().findRegion("cube_portal");
        shipPortalRegion = atlasManager.getPortalsAtlas().findRegion("ship_portal");
        gravityPortalRegion = atlasManager.getPortalsAtlas().findRegion("gravity_portal");
        miniPortalRegion = atlasManager.getPortalsAtlas().findRegion("mini_portal");
    }

    /**
     * Renders the current state of the game world.
     *
     * @param delta        The time elapsed since the last frame in seconds.
     * @param paused       Whether the game is currently paused, affecting visual updates.
     * @param showHitboxes Whether to render debug outlines and fills for entity hitboxes.
     */
    public void render(float delta, boolean paused, boolean showHitboxes) {
        world.updateVisuals(delta);
        AbstractPlayer player = world.getPlayer();


        camera.position.x = player.x + CAMERA_X_OFFSET;

        if (player.isMini()) {
            camera.position.x -= 12.5f;
        }

        camera.update();

        final float worldWidth = camera.viewportWidth;
        final float worldLeft = camera.position.x - worldWidth / 2f;
        world.setCullX(worldLeft);

        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(world.getGroundColor());
        shape.rect(worldLeft, 0, worldWidth, world.getGroundY());

        for (AbstractPortal portal : world.getPortals()) {
            AbstractPortal.PortalType pType = portal.getType();
            TextureRegion region = (pType == AbstractPortal.PortalType.CUBE)
                ? cubePortalRegion : shipPortalRegion;
            if (region == null) {
                shape.setColor(pType == AbstractPortal.PortalType.CUBE
                    ? FALLBACK_CUBE_PORTAL : FALLBACK_SHIP_PORTAL);
                shape.rect(portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
            }
        }
        shape.end();

        batch.begin();
        for (AbstractPortal portal : world.getPortals()) {
            AbstractPortal.PortalType pType = portal.getType();
            TextureRegion region = null;

            // Determine which texture region to use based on the portal type
            switch (pType) {
                case CUBE:
                    region = cubePortalRegion;
                    break;
                case SHIP:
                    region = shipPortalRegion;
                    break;
                case GRAVITY:
                    region = gravityPortalRegion;
                    break;
                case MINI:
                    region = miniPortalRegion;
                    break;
            }

            // Draw the texture region if it exists
            if (region != null) {
                batch.draw(region, portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
            }
        }

        if (spikeRegion != null) {
            for (AbstractHazard hazard : world.getHazards()) {
                if (hazard.getType() == AbstractHazard.HazardType.SPIKE) {
                    Spike spike = (Spike) hazard;
                    batch.draw(spikeRegion,
                        hazard.getX(), hazard.getY(),
                        hazard.getWidth() / 2f,
                        hazard.getHeight() / 2f,
                        hazard.getWidth(), hazard.getHeight(),
                        1f, 1f,
                        spike.getRotation());
                }
            }
        }

        for (Block block : world.getBlocks()) {
            TextureRegion region = blockRegionsByOrdinal[block.getType().ordinal()];
            if (region != null) {
                batch.draw(region, block.getX(), block.getY(),
                    block.getWidth(), block.getHeight());
            }
        }

        updatePlayerRotation(player, delta, paused);
        drawPlayerInBatch(player);
        batch.end();

        if (showHitboxes) drawHitboxes(player);
    }

    /**
     * Renders the player character sprite.
     */
    private void drawPlayerInBatch(AbstractPlayer player) {
        AbstractPlayer.PlayerType pType = player.getType();
        TextureRegion region = (pType == AbstractPlayer.PlayerType.SHIP) ? shipRegion : cubeRegion;

        if (region == null) {
            batch.end();
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(1f, 0.5f, 0.2f, 1f);
            shape.rect(player.x, player.y, player.width, player.height);
            shape.end();
            batch.begin();
            return;
        }

        if (player.isGravityFlipped()) {
            if (!region.isFlipY())
                region.flip(false, true);
        } else {
            if (region.isFlipY())
                region.flip(false, true);
        }

        float scaleX = (pType == AbstractPlayer.PlayerType.SHIP) ? 1.35f : 1f;
        float scaleY = (pType == AbstractPlayer.PlayerType.SHIP) ? 1.35f : 1f;

        batch.draw(region,
            player.x, player.y,
            player.width / 2f,
            player.height / 2f,
            player.width, player.height,
            scaleX, scaleY,
            playerVisualRotation);
    }

    /**
     * Renders debug hitboxes for all active game entities.
     */
    private void drawHitboxes(AbstractPlayer player) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(camera.combined);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(HB_BLOCK_FILL);
        for (Block b : world.getBlocks())
            shape.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());

        shape.setColor(HB_PORTAL_FILL);
        for (AbstractPortal p : world.getPortals()) {
            Rectangle r = p.getBounds();
            shape.rect(r.x, r.y, r.width, r.height);
        }

        shape.setColor(HB_HAZARD_FILL);
        for (AbstractHazard h : world.getHazards()) {
            if (h.getType() == AbstractHazard.HazardType.SPIKE) {
                Rectangle r = ((Spike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else {
                shape.rect(h.getX(), h.getY(), h.getWidth(), h.getHeight());
            }
        }

        shape.setColor(HB_PLAYER_FILL);
        Rectangle pb = player.getBounds();
        shape.rect(pb.x, pb.y, pb.width, pb.height);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(HB_BLOCK_LINE);
        for (Block b : world.getBlocks())
            shape.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());

        shape.setColor(HB_PORTAL_LINE);
        for (AbstractPortal p : world.getPortals()) {
            Rectangle r = p.getBounds();
            shape.rect(r.x, r.y, r.width, r.height);
        }

        shape.setColor(HB_HAZARD_LINE);
        for (AbstractHazard h : world.getHazards()) {
            if (h.getType() == AbstractHazard.HazardType.SPIKE) {
                Rectangle r = ((Spike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else {
                shape.rect(h.getX(), h.getY(), h.getWidth(), h.getHeight());
            }
        }

        shape.setColor(HB_PLAYER_LINE);
        shape.rect(pb.x, pb.y, pb.width, pb.height);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Updates the visual rotation of the player.
     */
    private void updatePlayerRotation(AbstractPlayer player, float delta, boolean paused) {
        float vy = player.getVelocityY();
        AbstractPlayer.PlayerType pType = player.getType();

        if (pType == AbstractPlayer.PlayerType.CUBE) {
            if (player.isGrounded()) {
                float nearest90 = Math.round(playerVisualRotation / 90f) * 90f;
                playerVisualRotation = lerp(playerVisualRotation, nearest90, delta * 15f);
            } else if (!world.isPlayerDead() && !paused) {
                float t = delta * 60f;
                float rotation = (Math.abs(vy) * CUBE_SPIN_FACTOR / 60f + 5f / 60f) * t + 300f * delta;
                if (player.isGravityFlipped()) {
                    playerVisualRotation += rotation;
                } else {
                    playerVisualRotation -= rotation;
                }
            }
        } else if (pType == AbstractPlayer.PlayerType.SHIP) {
            float targetAngle = Math.max(-SHIP_MAX_TILT, Math.min(SHIP_MAX_TILT, vy * SHIP_TILT_FACTOR));
            playerVisualRotation = lerp(playerVisualRotation, targetAngle, SHIP_TILT_LERP * delta);
        } else {
            playerVisualRotation = 0f;
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1f);
    }

    /**
     * Releases all resources used by this renderer.
     */
    public void dispose() {
        shape.dispose();
    }
}
