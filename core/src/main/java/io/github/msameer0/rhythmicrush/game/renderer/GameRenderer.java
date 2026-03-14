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

import java.util.EnumMap;
import java.util.Map;

public class GameRenderer {

    private final GameWorld          world;
    private final OrthographicCamera camera;
    private final SpriteBatch        batch;
    private final ShapeRenderer      shape;

    // ── Texture regions ───────────────────────────────────────────────────────
    private final Map<BlockType, TextureRegion> blockRegions;
    private final TextureRegion spikeRegion;
    private final TextureRegion cubeRegion;
    private final TextureRegion shipRegion;
    private final TextureRegion cubePortalRegion;
    private final TextureRegion shipPortalRegion;

    // ── Player visual rotation ────────────────────────────────────────────────
    private float playerVisualRotation = 0f;

    private static final float CAMERA_X_OFFSET = 425f;
    private static final float CUBE_SPIN_FACTOR = 0.5f;
    private static final float SHIP_TILT_FACTOR = 0.18f;
    private static final float SHIP_MAX_TILT    = 50f;
    private static final float SHIP_TILT_LERP   = 8f;

    // ── Hitbox colors ─────────────────────────────────────────────────────────
    // Fill: 75% opacity. Border: fully opaque same hue.
    private static final Color HB_PLAYER_FILL  = new Color(1.0f, 0.9f, 0.0f, 0.75f); // yellow
    private static final Color HB_HAZARD_FILL  = new Color(1.0f, 0.2f, 0.2f, 0.75f); // red
    private static final Color HB_BLOCK_FILL   = new Color(0.2f, 0.5f, 1.0f, 0.75f); // blue
    private static final Color HB_PORTAL_FILL  = new Color(0.2f, 1.0f, 0.4f, 0.75f); // green

    private static final Color HB_PLAYER_LINE  = new Color(1.0f, 0.9f, 0.0f, 1.0f);
    private static final Color HB_HAZARD_LINE  = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color HB_BLOCK_LINE   = new Color(0.2f, 0.5f, 1.0f, 1.0f);
    private static final Color HB_PORTAL_LINE  = new Color(0.2f, 1.0f, 0.4f, 1.0f);

    // Matches Block.tryTouch() inner-hitbox margin: 25% on each side → 50% inner rect
    private static final float BLOCK_HB_MARGIN = 0.25f;

    // ── Constructor ───────────────────────────────────────────────────────────

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
        spikeRegion      = atlasManager.getSpikesAtlas().findRegion("spike");
        cubeRegion       = atlasManager.getGamemodesAtlas().findRegion("cube");
        shipRegion       = atlasManager.getGamemodesAtlas().findRegion("ship");
        cubePortalRegion = atlasManager.getPortalsAtlas().findRegion("cube_portal");
        shipPortalRegion = atlasManager.getPortalsAtlas().findRegion("ship_portal");
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * @param delta        time since last frame
     * @param paused       suppresses player rotation update
     * @param showHitboxes draws hitbox overlays on top of all game objects
     */
    public void render(float delta, boolean paused, boolean showHitboxes) {
        AbstractPlayer player = world.getPlayer();

        camera.position.x = player.x + CAMERA_X_OFFSET;
        camera.update();
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        float worldWidth = camera.viewportWidth;
        float worldLeft  = camera.position.x - worldWidth / 2f;
        world.setCullX(worldLeft);

        // ── Ground ────────────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(world.getGroundColor());
        shape.rect(worldLeft, 0, worldWidth, world.getGroundY());
        shape.end();

        // ── Portals ───────────────────────────────────────────────────────────
        batch.begin();
        for (AbstractPortal portal : world.getPortals()) {
            TextureRegion region = (portal instanceof CubePortal) ? cubePortalRegion : shipPortalRegion;
            if (region != null) {
                batch.draw(region, portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
            } else {
                batch.end();
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(portal instanceof CubePortal
                    ? new Color(0f, 0.8f, 0f, 1f)
                    : new Color(0f, 0.5f, 1f, 1f));
                shape.rect(portal.getX(), portal.getY(), portal.getWidth(), portal.getHeight());
                shape.end();
                batch.begin();
            }
        }
        batch.end();

        // ── Spikes ────────────────────────────────────────────────────────────
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

        // ── Blocks ────────────────────────────────────────────────────────────
        batch.begin();
        for (Block block : world.getBlocks()) {
            TextureRegion region = blockRegions.get(block.getType());
            if (region != null) {
                batch.draw(region, block.getX(), block.getY(),
                    block.getWidth(), block.getHeight());
            }
        }
        batch.end();

        // ── Player ────────────────────────────────────────────────────────────
        updatePlayerRotation(player, delta, paused);
        drawPlayer(player);

        // ── Hitboxes (always on top) ──────────────────────────────────────────
        if (showHitboxes) drawHitboxes(player);
    }

    // ── Hitbox overlay ────────────────────────────────────────────────────────

    private void drawHitboxes(AbstractPlayer player) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(camera.combined);

        // ── Filled pass ───────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Blocks: inner 50% rect (25% margin each side) — matches Block.tryTouch() death check
        shape.setColor(HB_BLOCK_FILL);
        for (Block b : world.getBlocks()) {
            Rectangle r = blockInnerHitbox(b);
            shape.rect(r.x, r.y, r.width, r.height);
        }

        // Portals: full bounds rectangle
        shape.setColor(HB_PORTAL_FILL);
        for (AbstractPortal p : world.getPortals()) {
            Rectangle r = p.getBounds();
            shape.rect(r.x, r.y, r.width, r.height);
        }

        // Spikes: their own rotated-aware spikeHitbox from getHitbox()
        shape.setColor(HB_HAZARD_FILL);
        for (AbstractHazard h : world.getHazards()) {
            if (h instanceof Spike) {
                Rectangle r = ((Spike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else {
                // Non-spike hazards: full bounds
                shape.rect(h.getX(), h.getY(), h.getWidth(), h.getHeight());
            }
        }

        // Player: full bounds rectangle
        shape.setColor(HB_PLAYER_FILL);
        Rectangle pb = player.getBounds();
        shape.rect(pb.x, pb.y, pb.width, pb.height);

        shape.end();

        // ── Border pass ───────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Line);

        shape.setColor(HB_BLOCK_LINE);
        for (Block b : world.getBlocks()) {
            Rectangle r = blockInnerHitbox(b);
            shape.rect(r.x, r.y, r.width, r.height);
        }

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

    private static final Rectangle _tmpRect = new Rectangle();
    private Rectangle blockInnerHitbox(Block b) {
        _tmpRect.set(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        return _tmpRect;
    }

    // ── Player rotation ───────────────────────────────────────────────────────

    private void updatePlayerRotation(AbstractPlayer player, float delta, boolean paused) {
        float vy = player.getVelocityY();

        if (player instanceof Cube) {
            if (player.isGrounded()) {
                float nearest90 = Math.round(playerVisualRotation / 90f) * 90f;
                playerVisualRotation = lerp(playerVisualRotation, nearest90, delta * 15f);
            } else {
                if (!world.isPlayerDead() && !paused) {
                    playerVisualRotation -= ((Math.abs(vy) * CUBE_SPIN_FACTOR * delta) + 5);
                }
            }
        } else if (player instanceof Ship) {
            float targetAngle = vy * SHIP_TILT_FACTOR;
            targetAngle = Math.max(-SHIP_MAX_TILT, Math.min(SHIP_MAX_TILT, targetAngle));
            playerVisualRotation = lerp(playerVisualRotation, targetAngle, SHIP_TILT_LERP * delta);
        } else {
            playerVisualRotation = 0f;
        }
    }

    private void drawPlayer(AbstractPlayer player) {
        TextureRegion region = (player instanceof Ship) ? shipRegion : cubeRegion;

        if (region == null) {
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(1f, 0.5f, 0.2f, 1f);
            shape.rect(player.x, player.y, player.width, player.height);
            shape.end();
            return;
        }

        float scaleX = (player instanceof Ship) ? 1.1f : 1f;
        float scaleY = (player instanceof Ship) ? 1.1f : 1f;

        batch.begin();
        batch.draw(region,
            player.x, player.y,
            player.width  / 2f,
            player.height / 2f,
            player.width, player.height,
            scaleX, scaleY,
            playerVisualRotation);
        batch.end();
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
