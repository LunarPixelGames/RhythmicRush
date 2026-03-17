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
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship;

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

    private static final Rectangle _tmpRect = new Rectangle();

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
    }

    /**
     * Renders the current state of the game world.
     * <p>
     * This method handles the primary rendering loop, performing the following steps:
     * <ul>
     *     <li>Updates the camera position based on the player's movement.</li>
     *     <li>Culls world objects that are outside the current viewport.</li>
     *     <li>Renders the ground using a {@link ShapeRenderer}.</li>
     *     <li>Renders game entities (portals, hazards, and blocks) using a {@link SpriteBatch}.</li>
     *     <li>Updates player visual effects like rotation and draws the player sprite.</li>
     *     <li>Draws fallback shapes for interactables if their textures are missing.</li>
     *     <li>Optionally renders debug hitboxes for all physical entities.</li>
     * </ul>
     *
     * @param delta        The time elapsed since the last frame in seconds.
     * @param paused       Whether the game is currently paused, affecting visual updates.
     * @param showHitboxes Whether to render debug outlines and fills for entity hitboxes.
     */
    public void render(float delta, boolean paused, boolean showHitboxes) {
        AbstractPlayer player = world.getPlayer();

        camera.position.x = player.x + CAMERA_X_OFFSET;
        camera.update();

        final float worldWidth = camera.viewportWidth;
        final float worldLeft = camera.position.x - worldWidth / 2f;
        world.setCullX(worldLeft);

        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(world.getGroundColor());
        shape.rect(worldLeft, 0, worldWidth, world.getGroundY());
        shape.end();

        batch.begin();

        for (AbstractPortal portal : world.getPortals()) {
            TextureRegion region = (portal instanceof CubePortal)
                ? cubePortalRegion : shipPortalRegion;
            if (region != null) {
                batch.draw(region, portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
            }
        }

        if (spikeRegion != null) {
            for (AbstractHazard hazard : world.getHazards()) {
                if (hazard instanceof Spike) {
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

        boolean needsFallback = false;
        for (AbstractPortal portal : world.getPortals()) {
            TextureRegion region = (portal instanceof CubePortal)
                ? cubePortalRegion : shipPortalRegion;
            if (region == null) {
                needsFallback = true;
                break;
            }
        }
        if (needsFallback) {
            shape.begin(ShapeRenderer.ShapeType.Filled);
            for (AbstractPortal portal : world.getPortals()) {
                TextureRegion region = (portal instanceof CubePortal)
                    ? cubePortalRegion : shipPortalRegion;
                if (region == null) {
                    shape.setColor(portal instanceof CubePortal
                        ? FALLBACK_CUBE_PORTAL : FALLBACK_SHIP_PORTAL);
                    shape.rect(portal.getX(), portal.getY(),
                        portal.getWidth(), portal.getHeight());
                }
            }
            shape.end();
        }

        if (showHitboxes) drawHitboxes(player);
    }

    /**
     * Renders the player character at its current position using the active {@link SpriteBatch}.
     * <p>
     * This method selects the appropriate texture based on the player's current gamemode
     * (e.g., {@link Ship} or {@link Cube}). It applies visual scaling and rotations calculated
     * in the update phase. If the required texture region is unavailable, it switches
     * temporarily to a {@link ShapeRenderer} to draw a fallback colored rectangle.
     * </p>
     *
     * @param player The player instance to be drawn.
     */
    private void drawPlayerInBatch(AbstractPlayer player) {
        TextureRegion region = (player instanceof Ship) ? shipRegion : cubeRegion;

        if (region == null) {
            batch.end();
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(1f, 0.5f, 0.2f, 1f);
            shape.rect(player.x, player.y, player.width, player.height);
            shape.end();
            batch.begin();
            return;
        }

        float scaleX = (player instanceof Ship) ? 1.1f : 1f;
        float scaleY = (player instanceof Ship) ? 1.1f : 1f;

        batch.draw(region,
            player.x, player.y,
            player.width / 2f,
            player.height / 2f,
            player.width, player.height,
            scaleX, scaleY,
            playerVisualRotation);
    }

    /**
     * Renders debug hitboxes for all active game entities using a {@link ShapeRenderer}.
     * <p>
     * This method draws semi-transparent filled rectangles and solid outlines for blocks,
     * portals, hazards, and the player to visualize their physical boundaries. It handles
     * specific hitbox logic for complex shapes, such as {@link Spike} objects, which
     * may have hitboxes differing from their visual bounds.
     * </p>
     * <p>
     * GL blending is enabled during this process to allow for the transparency defined in
     * the {@code HB_} color constants.
     * </p>
     *
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
            if (h instanceof Spike) {
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
            if (h instanceof Spike) {
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
     * Updates the visual rotation of the player based on their current state and gamemode.
     * <p>
     * For {@link Cube} players, the rotation simulates a spinning animation while in the air
     * and snaps to the nearest 90-degree increment when grounded to ensure the cube sits flat.
     * For {@link Ship} players, the rotation creates a tilting effect based on vertical velocity,
     * limited by a maximum tilt angle.
     * </p>
     *
     * @param player The player instance whose visual rotation is being calculated.
     * @param delta  The time elapsed since the last frame in seconds.
     * @param paused Whether the game is currently paused, preventing rotational progression.
     */
    private void updatePlayerRotation(AbstractPlayer player, float delta, boolean paused) {
        float vy = player.getVelocityY();

        if (player instanceof Cube) {
            if (player.isGrounded()) {
                float nearest90 = Math.round(playerVisualRotation / 90f) * 90f;
                playerVisualRotation = lerp(playerVisualRotation, nearest90, delta * 15f);
            } else if (!world.isPlayerDead() && !paused) {
                float t = delta * 60f;
                playerVisualRotation -=
                    (Math.abs(vy) * CUBE_SPIN_FACTOR / 60f + 5f / 60f) * t + 300f * delta;
            }
        } else if (player instanceof Ship) {
            float targetAngle = Math.max(-SHIP_MAX_TILT,
                Math.min(SHIP_MAX_TILT, vy * SHIP_TILT_FACTOR));
            playerVisualRotation = lerp(playerVisualRotation, targetAngle, SHIP_TILT_LERP * delta);
        } else {
            playerVisualRotation = 0f;
        }
    }

    /**
     * Linearly interpolates between two values.
     * <p>
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1f);
    }

    /**
     * Releases all resources used by this renderer.
     * <p>
     * Specifically, it disposes of the {@link ShapeRenderer} instance to prevent
     * memory leaks. This should be called when the renderer is no longer needed.
     * </p>
     */
    public void dispose() {
        shape.dispose();
    }
}
