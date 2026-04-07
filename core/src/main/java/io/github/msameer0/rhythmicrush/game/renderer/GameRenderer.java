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
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.AbstractOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * Responsible for rendering the visual representation of the game world.
 */
public class GameRenderer {

    private final GameWorld world;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;

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
    private final TextureRegion yellowOrbRegion;

    private static final Color FALLBACK_CUBE_PORTAL  = new Color(0f,    0.8f, 0f,   1f);
    private static final Color FALLBACK_SHIP_PORTAL  = new Color(0f,    0.5f, 1f,   1f);
    private static final Color FALLBACK_YELLOW_ORB   = new Color(1f,    0.9f, 0.1f, 1f);

    private float playerVisualRotation = 0f;

    private static final float CAMERA_X_OFFSET  = 425f;
    private static final float CUBE_SPIN_FACTOR  = 0.5f;
    private static final float SHIP_TILT_FACTOR  = 0.18f;
    private static final float SHIP_MAX_TILT     = 50f;
    private static final float SHIP_TILT_LERP    = 8f;

    // Hitbox colours
    private static final Color HB_PLAYER_FILL = new Color(1.0f, 0.9f, 0.0f, 0.75f);
    private static final Color HB_HAZARD_FILL = new Color(1.0f, 0.2f, 0.2f, 0.75f);
    private static final Color HB_BLOCK_FILL  = new Color(0.2f, 0.5f, 1.0f, 0.75f);
    private static final Color HB_PORTAL_FILL = new Color(0.2f, 1.0f, 0.4f, 0.75f);
    private static final Color HB_ORB_FILL    = new Color(1.0f, 0.9f, 0.1f, 0.55f);

    private static final Color HB_PLAYER_LINE = new Color(1.0f, 0.9f, 0.0f, 1.0f);
    private static final Color HB_HAZARD_LINE = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color HB_BLOCK_LINE  = new Color(0.2f, 0.5f, 1.0f, 1.0f);
    private static final Color HB_PORTAL_LINE = new Color(0.2f, 1.0f, 0.4f, 1.0f);
    private static final Color HB_ORB_LINE    = new Color(1.0f, 0.9f, 0.1f, 1.0f);

    /**
     * Constructs a new GameRenderer and initialises texture regions from the provided atlases.
     */
    public GameRenderer(GameWorld world, OrthographicCamera camera,
                        SpriteBatch batch, AtlasManager atlasManager) {
        this.world  = world;
        this.camera = camera;
        this.batch  = batch;
        this.shape  = new ShapeRenderer();

        BlockType[] types = BlockType.values();
        blockRegionsByOrdinal = new TextureRegion[types.length];
        for (BlockType type : types)
            blockRegionsByOrdinal[type.ordinal()] =
                atlasManager.getBlocksAtlas().findRegion(type.textureName);

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

        // Orb texture — adjust atlas / region name to match your project
        yellowOrbRegion = atlasManager.getOrbsAtlas().findRegion("yellow_orb");
    }

    /**
     * Renders the current state of the game world.
     *
     * @param delta        Time elapsed since the last frame in seconds.
     * @param paused       Whether the game is paused.
     * @param showHitboxes Whether to draw debug hitbox overlays.
     */
    public void render(float delta, boolean paused, boolean showHitboxes) {
        world.updateVisuals(delta);
        AbstractPlayer player = world.getPlayer();

        camera.position.x = player.x + CAMERA_X_OFFSET;
        if (player.isMini()) camera.position.x -= 12.5f;
        camera.update();

        final float worldWidth = camera.viewportWidth;
        final float worldLeft  = camera.position.x - worldWidth / 2f;
        world.setCullX(worldLeft);

        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        // ── Saw blades (separate pass — need rotation each tick) ─────────────
        batch.begin();
        for (AbstractHazard hazard : world.getHazards()) {
            if (hazard.getType() == AbstractHazard.HazardType.SAW_BLADE) {
                SawBlade saw = (SawBlade) hazard;
                if (sawBladeRegion != null) {
                    float d = saw.getDiameter();
                    if (!paused) saw.tickVisualRotation(delta);
                    batch.draw(sawBladeRegion,
                        saw.getX(), saw.getY(),
                        d / 2f, d / 2f,
                        d, d, 1f, 1f,
                        saw.getVisualRotation());
                }
            }
        }
        batch.end();

        // ── Portal fallback shapes (only when no texture) ────────────────────
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

        // ── Main batch pass ──────────────────────────────────────────────────
        batch.begin();

        // Portals
        for (AbstractPortal portal : world.getPortals()) {
            AbstractPortal.PortalType pType = portal.getType();
            TextureRegion region = null;
            switch (pType) {
                case CUBE:    region = cubePortalRegion;    break;
                case SHIP:    region = shipPortalRegion;    break;
                case GRAVITY: region = gravityPortalRegion; break;
                case MINI:    region = miniPortalRegion;    break;
            }
            if (region != null)
                batch.draw(region, portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
        }

        // Hazards (spikes / half-spikes — saw blades already drawn above)
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
                    break; // handled in separate pass above
            }
        }

        // Blocks
        for (Block block : world.getBlocks()) {
            if (block instanceof Slope) {
                Slope slope = (Slope) block;
                TextureRegion region = (slopeRegion != null)
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

        // Orbs
        drawOrbs();

        // Player
        updatePlayerRotation(player, delta, paused);
        drawPlayerInBatch(player);

        batch.end();

        // ── Ground ───────────────────────────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(world.getGroundColor());
        shape.rect(worldLeft, 0, worldWidth, world.getGroundY());
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (showHitboxes) drawHitboxes(player);
    }

    // ── Orb rendering ────────────────────────────────────────────────────────

    /**
     * Draws all active orbs. Called inside an open batch block.
     * Orbs that are already used are rendered at reduced opacity so the
     * player can see they have been consumed.
     */
    private void drawOrbs() {
        int cullStart = world.getOrbCull();
        for (int i = cullStart; i < world.getOrbs().size; i++) {
            AbstractOrb orb = world.getOrbs().get(i);

            float alpha = 1f;

            if (yellowOrbRegion != null && orb.getType() == AbstractOrb.OrbType.YELLOW) {
                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(yellowOrbRegion,
                    orb.getX(), orb.getY(),
                    orb.getWidth(), orb.getHeight());
                batch.setColor(Color.WHITE);
            } else {
                // Fallback: coloured rectangle when no texture is available
                batch.end();
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(FALLBACK_YELLOW_ORB.r, FALLBACK_YELLOW_ORB.g,
                    FALLBACK_YELLOW_ORB.b, alpha);
                shape.circle(
                    orb.getX() + orb.getWidth()  / 2f,
                    orb.getY() + orb.getHeight() / 2f,
                    orb.getWidth() / 2f, 24);
                shape.end();
                Gdx.gl.glDisable(GL20.GL_BLEND);
                batch.begin();
            }
        }
    }

    // ── Player rendering ─────────────────────────────────────────────────────

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
            if (!region.isFlipY()) region.flip(false, true);
        } else {
            if (region.isFlipY()) region.flip(false, true);
        }

        float scaleX = (pType == AbstractPlayer.PlayerType.SHIP) ? 1.35f : 1f;
        float scaleY = (pType == AbstractPlayer.PlayerType.SHIP) ? 1.35f : 1f;

        batch.draw(region,
            player.x, player.y,
            player.width / 2f, player.height / 2f,
            player.width, player.height,
            scaleX, scaleY,
            playerVisualRotation);
    }

    // ── Hitbox debug rendering ────────────────────────────────────────────────

    private void drawHitboxes(AbstractPlayer player) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(camera.combined);

        // ── Filled pass ──────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);

        shape.setColor(HB_BLOCK_FILL);
        for (Block b : world.getBlocks()) {
            if (b instanceof Slope) {
                Slope s = (Slope) b;
                float rot = ((int) s.getRotation() % 360 + 360) % 360;
                float x = s.getX(), y = s.getY(), w = s.getWidth(), h = s.getHeight();
                if      (rot == 0)   shape.triangle(x,     y,     x + w, y,     x + w, y + h);
                else if (rot == 90)  shape.triangle(x,     y + h, x + w, y + h, x + w, y    );
                else if (rot == 180) shape.triangle(x,     y,     x,     y + h, x + w, y + h);
                else if (rot == 270) shape.triangle(x,     y,     x,     y + h, x + w, y    );
                else                 shape.rect(x, y, w, h);
            } else {
                shape.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }
        }

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
            } else if (h.getType() == AbstractHazard.HazardType.HALF_SPIKE) {
                Rectangle r = ((HalfSpike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else {
                shape.rect(h.getX(), h.getY(), h.getWidth(), h.getHeight());
            }
        }

        // Orb hitboxes
        shape.setColor(HB_ORB_FILL);
        int cullStart = world.getOrbCull();
        for (int i = cullStart; i < world.getOrbs().size; i++) {
            AbstractOrb orb = world.getOrbs().get(i);
            Rectangle r = orb.getBounds();
            shape.circle(r.x + r.width / 2f, r.y + r.height / 2f, r.width / 2f, 24);
        }

        shape.setColor(HB_PLAYER_FILL);
        Rectangle pb = player.getBounds();
        shape.rect(pb.x, pb.y, pb.width, pb.height);

        shape.setColor(1.0f, 1.0f, 1.0f, 0.5f);
        float radius = player.width * 0.5f * Slope.CIRCLE_RATIO;
        shape.circle(pb.x + pb.width * 0.5f, pb.y + pb.height * 0.5f, radius);

        shape.end();

        // ── Line pass ────────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Line);

        shape.setColor(HB_BLOCK_LINE);
        for (Block b : world.getBlocks()) {
            if (b instanceof Slope) {
                Slope s = (Slope) b;
                float rot = ((int) s.getRotation() % 360 + 360) % 360;
                float x = s.getX(), y = s.getY(), w = s.getWidth(), h = s.getHeight();
                float[] line = s.getSlopeLine();
                float solidCX, solidCY;
                if      (rot == 0)   { solidCX = x + w; solidCY = y;     }
                else if (rot == 90)  { solidCX = x + w; solidCY = y + h; }
                else if (rot == 180) { solidCX = x;     solidCY = y + h; }
                else                 { solidCX = x;     solidCY = y;     }
                shape.triangle(line[0], line[1], line[2], line[3], solidCX, solidCY);
            } else {
                shape.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }
        }

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
            } else if (h.getType() == AbstractHazard.HazardType.HALF_SPIKE) {
                Rectangle r = ((HalfSpike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else if (h.getType() == AbstractHazard.HazardType.SAW_BLADE) {
                SawBlade saw = (SawBlade) h;
                shape.circle(saw.getX() + saw.getDiameter() / 2f,
                    saw.getY() + saw.getDiameter() / 2f,
                    saw.getDiameter() / 2f, 32);
            }
        }

        // Orb hitbox outlines
        shape.setColor(HB_ORB_LINE);
        for (int i = cullStart; i < world.getOrbs().size; i++) {
            AbstractOrb orb = world.getOrbs().get(i);
            Rectangle r = orb.getBounds();
            shape.circle(r.x + r.width / 2f, r.y + r.height / 2f, r.width / 2f, 24);
        }

        shape.setColor(HB_PLAYER_LINE);
        shape.rect(pb.x, pb.y, pb.width, pb.height);

        shape.setColor(1.0f, 1.0f, 1.0f, 0.8f);
        shape.circle(pb.x + pb.width * 0.5f, pb.y + pb.height * 0.5f, radius);

        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Player rotation ───────────────────────────────────────────────────────

    private void updatePlayerRotation(AbstractPlayer player, float delta, boolean paused) {
        float vy    = player.getVelocityY();
        AbstractPlayer.PlayerType pType = player.getType();
        float slopeRot = player.getCurrentSlopeRotation();

        if (pType == AbstractPlayer.PlayerType.CUBE) {
            if (player.isGrounded()) {
                float nearest90  = Math.round((playerVisualRotation - slopeRot) / 90f) * 90f;
                float targetRotation = nearest90 + slopeRot;
                playerVisualRotation = lerp(playerVisualRotation, targetRotation, delta * 15f);
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

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1f);
    }

    /** Releases all resources used by this renderer. */
    public void dispose() {
        shape.dispose();
    }
}
