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
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship;

import java.util.EnumMap;
import java.util.Map;

public class GameRenderer {

    private final GameWorld world;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final ShapeRenderer shape;

    // ── Game object regions ───────────────────────────────────────────────────
    private final Map<BlockType, TextureRegion> blockRegions;
    private final TextureRegion spikeRegion;
    private final TextureRegion cubeRegion;
    private final TextureRegion shipRegion;
    private final TextureRegion cubePortalRegion;
    private final TextureRegion shipPortalRegion;

    // ── Player visual rotation (persists across frames) ───────────────────────
    private float playerVisualRotation = 0f;

    // Cube spin: degrees per unit of |velocityY| per second while airborne
    private static final float CUBE_SPIN_FACTOR = 0.7f;
    // Ship tilt: maps velocityY → target angle, then lerps
    private static final float SHIP_TILT_FACTOR = 0.12f;
    private static final float SHIP_MAX_TILT = 45f;
    private static final float SHIP_TILT_LERP = 8f;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameRenderer(GameWorld world, OrthographicCamera camera,
                        SpriteBatch batch, AtlasManager atlasManager) {
        this.world = world;
        this.camera = camera;
        this.batch = batch;
        this.shape = new ShapeRenderer();

        blockRegions = new EnumMap<>(BlockType.class);
        for (BlockType type : BlockType.values()) {
            TextureRegion r = atlasManager.getBlocksAtlas().findRegion(type.textureName);
            if (r != null) blockRegions.put(type, r);
        }
        spikeRegion = atlasManager.getSpikesAtlas().findRegion("spike");
        cubeRegion = atlasManager.getGamemodesAtlas().findRegion("cube");
        shipRegion = atlasManager.getGamemodesAtlas().findRegion("ship");
        cubePortalRegion = atlasManager.getPortalsAtlas().findRegion("cube_portal");
        shipPortalRegion = atlasManager.getPortalsAtlas().findRegion("ship_portal");
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(float delta) {
        camera.update();
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        float worldWidth = camera.viewportWidth;
        float worldLeft = camera.position.x - worldWidth / 2f;

        AbstractPlayer player = world.getPlayer();

        // ── Ground ────────────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(world.getGroundColor());
        shape.rect(worldLeft, 0, worldWidth, world.getGroundY());
        shape.end();

        // ── Portals (textured) ────────────────────────────────────────────────
        batch.begin();
        for (AbstractPortal portal : world.getPortals()) {
            TextureRegion region = (portal instanceof CubePortal) ? cubePortalRegion : shipPortalRegion;
            if (region != null) {
                batch.draw(region, portal.getX(), portal.getY(),
                    portal.getWidth(), portal.getHeight());
            } else {
                // fallback colored rect if atlas region missing
                batch.end();
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(portal instanceof CubePortal
                    ? new com.badlogic.gdx.graphics.Color(0f, 0.8f, 0f, 1f)
                    : new com.badlogic.gdx.graphics.Color(0f, 0.5f, 1f, 1f));
                shape.rect(portal.getX(), portal.getY(), portal.getWidth(), portal.getHeight());
                shape.end();
                batch.begin();
            }
        }
        batch.end();

        // ── Spikes (textured, rotation aware) ─────────────────────────────────
        batch.begin();
        for (AbstractHazard hazard : world.getHazards()) {
            if (hazard instanceof Spike && spikeRegion != null) {
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
        batch.end();

        // ── Blocks (textured) ─────────────────────────────────────────────────
        batch.begin();
        for (Block block : world.getBlocks()) {
            TextureRegion region = blockRegions.get(block.getType());
            if (region != null) {
                batch.draw(region, block.getX(), block.getY(),
                    block.getWidth(), block.getHeight());
            }
        }
        batch.end();

        // ── Player (textured, velocity-driven rotation) ───────────────────────
        updatePlayerRotation(player, delta);
        drawPlayer(player);
    }

    // ── Player rotation logic ─────────────────────────────────────────────────

    private void updatePlayerRotation(AbstractPlayer player, float delta) {
        float vy = player.getVelocityY();

        if (player instanceof Cube) {
            if (player.isGrounded()) {
                float nearest90 = Math.round(playerVisualRotation / 90f) * 90f;
                playerVisualRotation = lerp(playerVisualRotation, nearest90, delta * 15f);
            } else if (!world.isPlayerDead()) {
                playerVisualRotation -= ((Math.abs(vy) * CUBE_SPIN_FACTOR * delta) + 5);
            }
            // if dead and airborne — do nothing, rotation stays frozen
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
            player.width / 2f,
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
