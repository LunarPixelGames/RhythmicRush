package io.github.msameer0.rhythmicrush.game.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.AtlasManager;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public class GameRenderer {

    private final GameWorld          world;
    private final OrthographicCamera camera;
    private final SpriteBatch        batch;
    private final ShapeRenderer      shape;

    // ── Texture regions ───────────────────────────────────────────────────────
    // Stored as a flat array indexed by BlockType.ordinal() for O(1) lookup
    // instead of EnumMap.get() which has a tiny but non-zero overhead per call.
    private final TextureRegion[] blockRegionsByOrdinal;

    private final TextureRegion spikeRegion;
    private final TextureRegion cubeRegion;
    private final TextureRegion shipRegion;
    private final TextureRegion cubePortalRegion;
    private final TextureRegion shipPortalRegion;

    // Fallback colors for portals when atlas region is missing (rare)
    private static final Color FALLBACK_CUBE_PORTAL  = new Color(0f, 0.8f, 0f, 1f);
    private static final Color FALLBACK_SHIP_PORTAL  = new Color(0f, 0.5f, 1f, 1f);

    // ── Player visual rotation ────────────────────────────────────────────────
    private float playerVisualRotation = 0f;

    private static final float CAMERA_X_OFFSET = 425f;
    private static final float CUBE_SPIN_FACTOR = 0.5f;
    private static final float SHIP_TILT_FACTOR = 0.18f;
    private static final float SHIP_MAX_TILT    = 50f;
    private static final float SHIP_TILT_LERP   = 8f;

    // ── Hitbox colors ─────────────────────────────────────────────────────────
    private static final Color HB_PLAYER_FILL = new Color(1.0f, 0.9f, 0.0f, 0.75f);
    private static final Color HB_HAZARD_FILL = new Color(1.0f, 0.2f, 0.2f, 0.75f);
    private static final Color HB_BLOCK_FILL  = new Color(0.2f, 0.5f, 1.0f, 0.75f);
    private static final Color HB_PORTAL_FILL = new Color(0.2f, 1.0f, 0.4f, 0.75f);

    private static final Color HB_PLAYER_LINE = new Color(1.0f, 0.9f, 0.0f, 1.0f);
    private static final Color HB_HAZARD_LINE = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color HB_BLOCK_LINE  = new Color(0.2f, 0.5f, 1.0f, 1.0f);
    private static final Color HB_PORTAL_LINE = new Color(0.2f, 1.0f, 0.4f, 1.0f);

    // Reusable tmp rect for hitbox calculations — avoids allocation
    private static final Rectangle _tmpRect = new Rectangle();

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameRenderer(GameWorld world, OrthographicCamera camera,
                        SpriteBatch batch, AtlasManager atlasManager) {
        this.world  = world;
        this.camera = camera;
        this.batch  = batch;
        this.shape  = new ShapeRenderer();

        // Pre-cache block regions into a flat array indexed by ordinal
        BlockType[] types = BlockType.values();
        blockRegionsByOrdinal = new TextureRegion[types.length];
        for (BlockType type : types) {
            blockRegionsByOrdinal[type.ordinal()] =
                atlasManager.getBlocksAtlas().findRegion(type.textureName);
        }

        spikeRegion      = atlasManager.getSpikesAtlas().findRegion("spike");
        cubeRegion       = atlasManager.getGamemodesAtlas().findRegion("cube");
        shipRegion       = atlasManager.getGamemodesAtlas().findRegion("ship");
        cubePortalRegion = atlasManager.getPortalsAtlas().findRegion("cube_portal");
        shipPortalRegion = atlasManager.getPortalsAtlas().findRegion("ship_portal");
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * @param delta        time since last frame (used for visual rotation only)
     * @param paused       suppresses cube spin update
     * @param showHitboxes draws hitbox overlays on top of all game objects
     */
    public void render(float delta, boolean paused, boolean showHitboxes) {
        AbstractPlayer player = world.getPlayer();

        // Update camera once
        camera.position.x = player.x + CAMERA_X_OFFSET;
        camera.update();

        final float worldWidth = camera.viewportWidth;
        final float worldLeft  = camera.position.x - worldWidth / 2f;
        world.setCullX(worldLeft);

        // Set projection once — reused for both shape and batch
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        // ── Ground (one shape draw) ───────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(world.getGroundColor());
        shape.rect(worldLeft, 0, worldWidth, world.getGroundY());
        shape.end();

        // ── All textured objects in ONE batch pass ────────────────────────────
        // Portals + spikes + blocks + player are all from the same atlases,
        // so they can be submitted in a single begin()/end() — one GPU flush.
        batch.begin();

        // Portals
        for (AbstractPortal portal : world.getPortals()) {
            TextureRegion region = (portal instanceof CubePortal)
                ? cubePortalRegion : shipPortalRegion;
            if (region != null) {
                batch.draw(region, portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
            }
            // Fallback portals (no texture) skipped here, drawn after batch ends
        }

        // Spikes
        if (spikeRegion != null) {
            for (AbstractHazard hazard : world.getHazards()) {
                if (hazard instanceof Spike) {
                    Spike spike = (Spike) hazard;
                    batch.draw(spikeRegion,
                        hazard.getX(),      hazard.getY(),
                        hazard.getWidth()  / 2f,
                        hazard.getHeight() / 2f,
                        hazard.getWidth(), hazard.getHeight(),
                        1f, 1f,
                        spike.getRotation());
                }
            }
        }

        // Blocks — ordinal lookup is a direct array index, no map overhead
        for (Block block : world.getBlocks()) {
            TextureRegion region = blockRegionsByOrdinal[block.getType().ordinal()];
            if (region != null) {
                batch.draw(region, block.getX(), block.getY(),
                    block.getWidth(), block.getHeight());
            }
        }

        // Player
        updatePlayerRotation(player, delta, paused);
        drawPlayerInBatch(player);

        batch.end();

        // ── Fallback colored rects for portals missing a texture ──────────────
        // This is rare (only if atlas is missing the region) and renders after
        // the main batch so it doesn't interrupt it.
        boolean needsFallback = false;
        for (AbstractPortal portal : world.getPortals()) {
            TextureRegion region = (portal instanceof CubePortal)
                ? cubePortalRegion : shipPortalRegion;
            if (region == null) { needsFallback = true; break; }
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

        // ── Hitboxes (always on top) ──────────────────────────────────────────
        if (showHitboxes) drawHitboxes(player);
    }

    // ── Player drawing (called inside the open batch) ─────────────────────────

    private void drawPlayerInBatch(AbstractPlayer player) {
        TextureRegion region = (player instanceof Ship) ? shipRegion : cubeRegion;

        if (region == null) {
            // No texture — end batch, draw shape, reopen batch
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
            player.x,        player.y,
            player.width  / 2f,
            player.height / 2f,
            player.width,  player.height,
            scaleX, scaleY,
            playerVisualRotation);
    }

    // ── Hitbox overlay ────────────────────────────────────────────────────────

    private void drawHitboxes(AbstractPlayer player) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(camera.combined);

        // Filled pass
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

        // Border pass
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

    // ── Player rotation ───────────────────────────────────────────────────────

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

    // ── Util ──────────────────────────────────────────────────────────────────

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1f);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void dispose() {
        shape.dispose();
    }
}
