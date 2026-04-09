package io.github.msameer0.rhythmicrush.game.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

import io.github.msameer0.rhythmicrush.atlas.AtlasManager;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.AbstractOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

import java.util.EnumMap;

/**
 * Renders the visual representation of the game world.
 *
 * <p>Delegates hitbox debug drawing to {@link HitboxRenderer}. All other
 * sub-systems (color transitions, pooling) live in their own classes;
 * this class only owns rendering concerns.</p>
 */
public class GameRenderer {

    // ── Camera / player constants ─────────────────────────────────────────────
    private static final float CAMERA_X_OFFSET = 425f;
    private static final float CUBE_SPIN_FACTOR = 0.5f;
    private static final float SHIP_TILT_FACTOR = 0.18f;
    private static final float SHIP_MAX_TILT    = 50f;
    private static final float SHIP_TILT_LERP   = 8f;

    // ── Fallback colours (used when atlas region is missing) ──────────────────
    private static final Color FALLBACK_CUBE_PORTAL = new Color(0f,   0.8f, 0f,   1f);
    private static final Color FALLBACK_SHIP_PORTAL = new Color(0f,   0.5f, 1f,   1f);
    private static final Color FALLBACK_YELLOW_ORB  = new Color(1f,   0.9f, 0.1f, 1f);

    // ── Core rendering objects ─────────────────────────────────────────────────
    private final GameWorld world;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final HitboxRenderer hitboxRenderer;

    // ── Texture regions ───────────────────────────────────────────────────────
    private final TextureRegion[] blockRegionsByOrdinal;
    private final TextureRegion slopeRegion;
    private final TextureRegion spikeRegion;
    private final TextureRegion halfSpikeRegion;
    private final TextureRegion sawBladeRegion;
    private final TextureRegion cubeRegion;
    private final TextureRegion shipRegion;
    private final TextureRegion cubePortalRegion;
    private final TextureRegion shipPortalRegion;
    private final TextureRegion gravityPortalRegion;
    private final TextureRegion miniPortalRegion;

    // Orb regions keyed by type for O(1) lookup — replaces the old if/else chain
    private final EnumMap<AbstractOrb.OrbType, TextureRegion> orbRegions =
        new EnumMap<>(AbstractOrb.OrbType.class);

    // ── Player visual state ───────────────────────────────────────────────────
    private float playerVisualRotation = 0f;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameRenderer(GameWorld world, OrthographicCamera camera,
                        SpriteBatch batch, AtlasManager atlasManager) {
        this.world  = world;
        this.camera = camera;
        this.batch  = batch;
        this.shape  = new ShapeRenderer();
        this.hitboxRenderer = new HitboxRenderer(world, shape);

        // Block regions
        BlockType[] types = BlockType.values();
        blockRegionsByOrdinal = new TextureRegion[types.length];
        for (BlockType type : types)
            blockRegionsByOrdinal[type.ordinal()] =
                atlasManager.getBlocksAtlas().findRegion(type.getTextureName());

        slopeRegion         = atlasManager.getBlocksAtlas().findRegion("slope");
        spikeRegion         = atlasManager.getSpikesAtlas().findRegion("spike");
        halfSpikeRegion     = atlasManager.getSpikesAtlas().findRegion("half_spike");
        sawBladeRegion      = atlasManager.getSpikesAtlas().findRegion("saw_blade");
        cubeRegion          = atlasManager.getGamemodesAtlas().findRegion("cube");
        shipRegion          = atlasManager.getGamemodesAtlas().findRegion("ship");
        cubePortalRegion    = atlasManager.getPortalsAtlas().findRegion("cube_portal");
        shipPortalRegion    = atlasManager.getPortalsAtlas().findRegion("ship_portal");
        gravityPortalRegion = atlasManager.getPortalsAtlas().findRegion("gravity_portal");
        miniPortalRegion    = atlasManager.getPortalsAtlas().findRegion("mini_portal");

        // Orb region map
        orbRegions.put(AbstractOrb.OrbType.YELLOW, atlasManager.getOrbsAtlas().findRegion("yellow_orb"));
        orbRegions.put(AbstractOrb.OrbType.BLUE,   atlasManager.getOrbsAtlas().findRegion("blue_orb"));
        orbRegions.put(AbstractOrb.OrbType.PINK,   atlasManager.getOrbsAtlas().findRegion("pink_orb"));
        orbRegions.put(AbstractOrb.OrbType.BLACK,  atlasManager.getOrbsAtlas().findRegion("black_orb"));
        orbRegions.put(AbstractOrb.OrbType.GREEN,  atlasManager.getOrbsAtlas().findRegion("green_orb"));
        orbRegions.put(AbstractOrb.OrbType.RED,    atlasManager.getOrbsAtlas().findRegion("red_orb"));
    }

    // ── Public render entry point ─────────────────────────────────────────────

    /**
     * Renders the current state of the game world.
     *
     * @param delta        Seconds since the last frame.
     * @param paused       Whether the game is currently paused.
     * @param showHitboxes Whether to draw debug hitbox overlays.
     */
    public void render(float delta, boolean paused, boolean showHitboxes) {
        _lastDelta = delta;
        world.updateVisuals(delta);
        AbstractPlayer player = world.getPlayer();

        updateCamera(player);

        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawSawBlades(paused);
        drawPortalFallbacks();
        drawMainPass(player, delta, paused);
        drawGround();

        if (showHitboxes) hitboxRenderer.draw(camera, player);
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private void updateCamera(AbstractPlayer player) {
        camera.position.x = player.x + CAMERA_X_OFFSET;
        if (player.isMini()) camera.position.x -= 12.5f;
        camera.update();

        final float worldLeft = camera.position.x - camera.viewportWidth / 2f;
        world.setCullX(worldLeft);
    }

    // ── Saw blades (separate pre-pass for rotation) ───────────────────────────

    private void drawSawBlades(boolean paused) {
        batch.begin();
        for (AbstractHazard hazard : world.getHazards()) {
            if (hazard.getType() != AbstractHazard.HazardType.SAW_BLADE) continue;
            if (sawBladeRegion == null) continue;
            SawBlade saw = (SawBlade) hazard;
            float d = saw.getDiameter();
            if (!paused) saw.tickVisualRotation(delta());
            batch.draw(sawBladeRegion,
                saw.getX(), saw.getY(),
                d / 2f, d / 2f, d, d, 1f, 1f,
                saw.getVisualRotation());
        }
        batch.end();
    }

    // ── Portal fallbacks (shapes when no texture is loaded) ───────────────────

    private void drawPortalFallbacks() {
        boolean anyFallback = false;
        for (AbstractPortal portal : world.getPortals()) {
            AbstractPortal.PortalType pType = portal.getType();
            TextureRegion region = portalRegion(pType);
            if (region == null) {
                if (!anyFallback) {
                    Gdx.gl.glEnable(GL20.GL_BLEND);
                    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    shape.begin(ShapeRenderer.ShapeType.Filled);
                    anyFallback = true;
                }
                shape.setColor(pType == AbstractPortal.PortalType.CUBE
                    ? FALLBACK_CUBE_PORTAL : FALLBACK_SHIP_PORTAL);
                shape.rect(portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
            }
        }
        if (anyFallback) {
            shape.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    // ── Main batch pass ───────────────────────────────────────────────────────

    private void drawMainPass(AbstractPlayer player, float delta, boolean paused) {
        batch.begin();
        drawPortals();
        drawHazards();
        drawBlocks();
        drawOrbs();
        updatePlayerRotation(player, delta, paused);
        drawPlayer(player);
        batch.end();
    }

    private void drawPortals() {
        for (AbstractPortal portal : world.getPortals()) {
            TextureRegion region = portalRegion(portal.getType());
            if (region != null)
                batch.draw(region, portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
        }
    }

    private void drawHazards() {
        for (AbstractHazard hazard : world.getHazards()) {
            switch (hazard.getType()) {
                case SPIKE:
                    if (spikeRegion != null) {
                        Spike spike = (Spike) hazard;
                        batch.draw(spikeRegion,
                            hazard.getX(), hazard.getY(),
                            hazard.getWidth() / 2f, hazard.getHeight() / 2f,
                            hazard.getWidth(), hazard.getHeight(),
                            1f, 1f, spike.getRotation());
                    }
                    break;
                case HALF_SPIKE:
                    if (halfSpikeRegion != null) {
                        HalfSpike hSpike = (HalfSpike) hazard;
                        batch.draw(halfSpikeRegion,
                            hazard.getX(), hazard.getY(),
                            hazard.getWidth() / 2f, hazard.getHeight() / 2f,
                            hazard.getWidth(), hazard.getHeight(),
                            1f, 1f, hSpike.getRotation());
                    }
                    break;
                case SAW_BLADE:
                    break; // handled in pre-pass
            }
        }
    }

    private void drawBlocks() {
        for (Block block : world.getBlocks()) {
            if (block instanceof Slope) {
                Slope slope = (Slope) block;
                TextureRegion region = slopeRegion != null
                    ? slopeRegion : blockRegionsByOrdinal[block.getType().ordinal()];
                if (region != null)
                    batch.draw(region,
                        slope.getX(), slope.getY(),
                        slope.getWidth() / 2f, slope.getHeight() / 2f,
                        slope.getWidth(), slope.getHeight(),
                        1f, 1f, slope.getRotation());
            } else {
                TextureRegion region = blockRegionsByOrdinal[block.getType().ordinal()];
                if (region != null)
                    batch.draw(region, block.getX(), block.getY(),
                        block.getWidth(), block.getHeight());
            }
        }
    }

    private void drawOrbs() {
        Array<AbstractOrb> orbs = world.getOrbs();
        int cullStart = world.getOrbCull();
        for (int i = cullStart; i < orbs.size; i++) {
            AbstractOrb orb = orbs.get(i);
            TextureRegion region = orbRegions.get(orb.getType());
            if (region != null) {
                batch.draw(region, orb.getX(), orb.getY(),
                    orb.getWidth(), orb.getHeight());
            } else {
                drawOrbFallback(orb);
            }
        }
    }

    /** Fallback circle for orbs whose texture atlas region is missing. */
    private void drawOrbFallback(AbstractOrb orb) {
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(FALLBACK_YELLOW_ORB);
        shape.circle(
            orb.getX() + orb.getWidth()  / 2f,
            orb.getY() + orb.getHeight() / 2f,
            orb.getWidth() / 2f, 24);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
    }

    // ── Player ────────────────────────────────────────────────────────────────

    private void drawPlayer(AbstractPlayer player) {
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

        // Sync Y-flip with gravity state
        if (player.isGravityFlipped()) {
            if (!region.isFlipY()) region.flip(false, true);
        } else {
            if (region.isFlipY())  region.flip(false, true);
        }

        float scaleX = (pType == AbstractPlayer.PlayerType.SHIP) ? 1.35f : 1f;
        float scaleY = scaleX;

        batch.draw(region,
            player.x, player.y,
            player.width / 2f, player.height / 2f,
            player.width, player.height,
            scaleX, scaleY, playerVisualRotation);
    }

    // ── Ground ────────────────────────────────────────────────────────────────

    private void drawGround() {
        final float worldWidth = camera.viewportWidth;
        final float worldLeft  = camera.position.x - worldWidth / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(world.getGroundColor());
        shape.rect(worldLeft, 0, worldWidth, world.getGroundY());
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Player rotation ───────────────────────────────────────────────────────

    private void updatePlayerRotation(AbstractPlayer player, float delta, boolean paused) {
        float vy       = player.getVelocityY();
        float slopeRot = player.getCurrentSlopeRotation();
        AbstractPlayer.PlayerType pType = player.getType();

        if (pType == AbstractPlayer.PlayerType.CUBE) {
            if (player.isGrounded()) {
                float nearest90  = Math.round((playerVisualRotation - slopeRot) / 90f) * 90f;
                playerVisualRotation = lerp(playerVisualRotation, nearest90 + slopeRot, delta * 15f);
            } else if (!world.isPlayerDead() && !paused) {
                float t        = delta * 60f;
                float rotation = (Math.abs(vy) * CUBE_SPIN_FACTOR / 60f + 5f / 60f) * t + 300f * delta;
                if (player.isGravityFlipped()) playerVisualRotation += rotation;
                else                           playerVisualRotation -= rotation;
            }
        } else if (pType == AbstractPlayer.PlayerType.SHIP) {
            float targetAngle = Math.max(-SHIP_MAX_TILT,
                Math.min(SHIP_MAX_TILT, vy * SHIP_TILT_FACTOR));
            if (player.isGrounded()) targetAngle += slopeRot;
            playerVisualRotation = lerp(playerVisualRotation, targetAngle, SHIP_TILT_LERP * delta);
        } else {
            playerVisualRotation = 0f;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TextureRegion portalRegion(AbstractPortal.PortalType type) {
        switch (type) {
            case CUBE:    return cubePortalRegion;
            case SHIP:    return shipPortalRegion;
            case GRAVITY: return gravityPortalRegion;
            case MINI:    return miniPortalRegion;
            default:      return null;
        }
    }

    /** Tiny shim so the saw-blade pre-pass can read delta without threading it through. */
    private float _lastDelta = 0f;
    private float delta() { return _lastDelta; }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1f);
    }

    public void dispose() {
        shape.dispose();
    }
}
