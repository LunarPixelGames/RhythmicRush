package io.github.msameer0.rhythmicrush.game.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.msameer0.rhythmicrush.atlas.AtlasManager
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.AbstractOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import java.util.EnumMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Renders the visual representation of the game world.
 *
 * Delegates hitbox debug drawing to [HitboxRenderer]. All other
 * sub-systems (color transitions, pooling) live in their own classes;
 * this class only owns rendering concerns.
 */
class GameRenderer(
    private val world: GameWorld,
    private val camera: OrthographicCamera,
    private val batch: SpriteBatch,
    atlasManager: AtlasManager
) {

    companion object {
        // ── Camera / player constants ─────────────────────────────────────────────
        private const val CAMERA_X_OFFSET = 425f
        private const val CUBE_SPIN_FACTOR = 0.5f
        private const val SHIP_TILT_FACTOR = 0.18f
        private const val SHIP_MAX_TILT = 50f
        private const val SHIP_TILT_LERP = 8f

        // ── Fallback colours (used when atlas region is missing) ──────────────────
        private val FALLBACK_CUBE_PORTAL = Color(0f, 0.8f, 0f, 1f)
        private val FALLBACK_SHIP_PORTAL = Color(0f, 0.5f, 1f, 1f)
        private val FALLBACK_YELLOW_ORB  = Color(1f, 0.9f, 0.1f, 1f)
    }

    private val shape = ShapeRenderer()
    private val hitboxRenderer = HitboxRenderer(world, shape)

    // ── Texture regions ───────────────────────────────────────────────────────
    private val blockRegionsByOrdinal: Array<TextureRegion?>
    private val slopeRegion: TextureRegion?
    private val spikeRegion: TextureRegion?
    private val halfSpikeRegion: TextureRegion?
    private val sawBladeRegion: TextureRegion?
    private val cubeRegion: TextureRegion?
    private val shipRegion: TextureRegion?
    private val cubePortalRegion: TextureRegion?
    private val shipPortalRegion: TextureRegion?
    private val gravityPortalRegion: TextureRegion?
    private val miniPortalRegion: TextureRegion?

    // Orb regions keyed by type for O(1) lookup
    private val orbRegions = EnumMap<AbstractOrb.OrbType, TextureRegion>(AbstractOrb.OrbType::class.java)

    // ── Player visual state ───────────────────────────────────────────────────
    var playerVisualRotation = 0f
        private set

    private var _lastDelta = 0f

    init {
        val types = BlockType.entries.toTypedArray()
        blockRegionsByOrdinal = arrayOfNulls(types.size)
        for (type in types) {
            blockRegionsByOrdinal[type.ordinal] = atlasManager.blocksAtlas.findRegion(type.textureName)
        }

        slopeRegion = atlasManager.blocksAtlas.findRegion("slope")
        spikeRegion = atlasManager.spikesAtlas.findRegion("spike")
        halfSpikeRegion = atlasManager.spikesAtlas.findRegion("half_spike")
        sawBladeRegion = atlasManager.spikesAtlas.findRegion("saw_blade")
        cubeRegion = atlasManager.gamemodesAtlas.findRegion("cube")
        shipRegion = atlasManager.gamemodesAtlas.findRegion("ship")
        cubePortalRegion = atlasManager.portalsAtlas.findRegion("cube_portal")
        shipPortalRegion = atlasManager.portalsAtlas.findRegion("ship_portal")
        gravityPortalRegion = atlasManager.portalsAtlas.findRegion("gravity_portal")
        miniPortalRegion = atlasManager.portalsAtlas.findRegion("mini_portal")

        // Orb region map
        orbRegions[AbstractOrb.OrbType.YELLOW] = atlasManager.orbsAtlas.findRegion("yellow_orb")
        orbRegions[AbstractOrb.OrbType.BLUE] = atlasManager.orbsAtlas.findRegion("blue_orb")
        orbRegions[AbstractOrb.OrbType.PINK] = atlasManager.orbsAtlas.findRegion("pink_orb")
        orbRegions[AbstractOrb.OrbType.BLACK] = atlasManager.orbsAtlas.findRegion("black_orb")
        orbRegions[AbstractOrb.OrbType.GREEN] = atlasManager.orbsAtlas.findRegion("green_orb")
        orbRegions[AbstractOrb.OrbType.RED] = atlasManager.orbsAtlas.findRegion("red_orb")
    }

    // ── Public render entry point ─────────────────────────────────────────────

    fun render(delta: Float, paused: Boolean, showHitboxes: Boolean) {
        _lastDelta = delta
        world.updateVisuals(delta)
        val player = world.player ?: return

        updateCamera(player)

        shape.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        drawSawBlades(paused)
        drawPortalFallbacks()
        drawMainPass(player, delta, paused)
        drawGround()

        if (showHitboxes) hitboxRenderer.draw(camera, player)
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun updateCamera(player: AbstractPlayer) {
        camera.position.x = player.x + CAMERA_X_OFFSET
        if (player.isMini()) camera.position.x -= 12.5f
        camera.update()

        val worldLeft = camera.position.x - camera.viewportWidth / 2f
        world.cullX = worldLeft
    }

    // ── Saw blades (separate pre-pass for rotation) ───────────────────────────

    private fun drawSawBlades(paused: Boolean) {
        batch.begin()
        for (hazard in world.hazards) {
            if (hazard.type != AbstractHazard.HazardType.SAW_BLADE) continue
            if (sawBladeRegion == null) continue
            val saw = hazard as SawBlade
            val d = saw.diameter
            if (!paused) saw.tickVisualRotation(_lastDelta)
            batch.draw(
                sawBladeRegion,
                saw.x, saw.y,
                d / 2f, d / 2f, d, d, 1f, 1f,
                saw.visualRotation
            )
        }
        batch.end()
    }

    // ── Portal fallbacks (shapes when no texture is loaded) ───────────────────

    private fun drawPortalFallbacks() {
        var anyFallback = false
        for (portal in world.portals) {
            val pType = portal.type
            val region = portalRegion(pType)
            if (region == null) {
                if (!anyFallback) {
                    Gdx.gl.glEnable(GL20.GL_BLEND)
                    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
                    shape.begin(ShapeRenderer.ShapeType.Filled)
                    anyFallback = true
                }
                shape.color = if (pType == AbstractPortal.PortalType.CUBE) FALLBACK_CUBE_PORTAL else FALLBACK_SHIP_PORTAL
                shape.rect(portal.x, portal.y, portal.width, portal.height)
            }
        }
        if (anyFallback) {
            shape.end()
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }
    }

    // ── Main batch pass ───────────────────────────────────────────────────────

    private fun drawMainPass(player: AbstractPlayer, delta: Float, paused: Boolean) {
        batch.begin()
        drawPortals()
        drawHazards()
        drawBlocks()
        drawOrbs()
        updatePlayerRotation(player, delta, paused)
        drawPlayer(player)
        batch.end()
    }

    private fun drawPortals() {
        for (portal in world.portals) {
            val region = portalRegion(portal.type)
            if (region != null) {
                batch.draw(region, portal.x, portal.y, portal.width, portal.height)
            }
        }
    }

    private fun drawHazards() {
        for (hazard in world.hazards) {
            when (hazard.type) {
                AbstractHazard.HazardType.SPIKE -> {
                    if (spikeRegion != null) {
                        val spike = hazard as Spike
                        batch.draw(
                            spikeRegion,
                            hazard.x, hazard.y,
                            hazard.width / 2f, hazard.height / 2f,
                            hazard.width, hazard.height,
                            1f, 1f, spike.rotation
                        )
                    }
                }
                AbstractHazard.HazardType.HALF_SPIKE -> {
                    if (halfSpikeRegion != null) {
                        val hSpike = hazard as HalfSpike
                        batch.draw(
                            halfSpikeRegion,
                            hazard.x, hazard.y,
                            hazard.width / 2f, hazard.height / 2f,
                            hazard.width, hazard.height,
                            1f, 1f, hSpike.rotation
                        )
                    }
                }
                AbstractHazard.HazardType.SAW_BLADE -> {
                    // Handled in pre-pass
                }
                else -> {}
            }
        }
    }

    private fun drawBlocks() {
        for (block in world.blocks) {
            if (block is Slope) {
                val region = slopeRegion ?: blockRegionsByOrdinal[block.type.ordinal]
                if (region != null) {
                    batch.draw(
                        region,
                        block.x, block.y,
                        block.width / 2f, block.height / 2f,
                        block.width, block.height,
                        1f, 1f, block.rotation
                    )
                }
            } else {
                val region = blockRegionsByOrdinal[block.type.ordinal]
                if (region != null) {
                    batch.draw(region, block.x, block.y, block.width, block.height)
                }
            }
        }
    }

    private fun drawOrbs() {
        val orbs = world.orbs
        val cullStart = world.orbCull
        for (i in cullStart until orbs.size) {
            val orb = orbs.get(i)
            val region = orbRegions[orb.type]
            if (region != null) {
                batch.draw(region, orb.x, orb.y, orb.width, orb.height)
            } else {
                drawOrbFallback(orb)
            }
        }
    }

    private fun drawOrbFallback(orb: AbstractOrb) {
        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = FALLBACK_YELLOW_ORB
        shape.circle(
            orb.x + orb.width / 2f,
            orb.y + orb.height / 2f,
            orb.width / 2f, 24
        )
        shape.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
        batch.begin()
    }

    // ── Player ────────────────────────────────────────────────────────────────

    private fun drawPlayer(player: AbstractPlayer) {
        val pType = player.getType()
        val region = if (pType == AbstractPlayer.PlayerType.SHIP) shipRegion else cubeRegion

        if (region == null) {
            batch.end()
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.setColor(1f, 0.5f, 0.2f, 1f)
            shape.rect(player.x, player.y, player.width, player.height)
            shape.end()
            batch.begin()
            return
        }

        // Sync Y-flip with gravity state
        if (player.isGravityFlipped()) {
            if (!region.isFlipY) region.flip(false, true)
        } else {
            if (region.isFlipY) region.flip(false, true)
        }

        val scaleX = if (pType == AbstractPlayer.PlayerType.SHIP) 1.35f else 1f
        val scaleY = scaleX

        batch.draw(
            region,
            player.x, player.y,
            player.width / 2f, player.height / 2f,
            player.width, player.height,
            scaleX, scaleY, playerVisualRotation
        )
    }

    // ── Ground ────────────────────────────────────────────────────────────────

    private fun drawGround() {
        val worldWidth = camera.viewportWidth
        val worldLeft = camera.position.x - worldWidth / 2f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = world.groundColor
        shape.rect(worldLeft, 0f, worldWidth, world.groundY)
        shape.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    // ── Player rotation ───────────────────────────────────────────────────────

    private fun updatePlayerRotation(player: AbstractPlayer, delta: Float, paused: Boolean) {
        val vy = player.velocityY
        val slopeRot = player.getCurrentSlopeRotation()
        val pType = player.getType()

        if (pType == AbstractPlayer.PlayerType.CUBE) {
            if (player.isGrounded()) {
                val nearest90 = round((playerVisualRotation - slopeRot) / 90f) * 90f
                playerVisualRotation = lerp(playerVisualRotation, nearest90 + slopeRot, delta * 15f)
            } else if (!world.isPlayerDead && !paused) {
                val t = delta * 60f
                val rotation = (abs(vy) * CUBE_SPIN_FACTOR / 60f + 5f / 60f) * t + 300f * delta
                if (player.isGravityFlipped()) playerVisualRotation += rotation
                else playerVisualRotation -= rotation
            }
        } else if (pType == AbstractPlayer.PlayerType.SHIP) {
            var targetAngle = max(-SHIP_MAX_TILT, min(SHIP_MAX_TILT, vy * SHIP_TILT_FACTOR))
            if (player.isGrounded()) targetAngle += slopeRot
            playerVisualRotation = lerp(playerVisualRotation, targetAngle, SHIP_TILT_LERP * delta)
        } else {
            playerVisualRotation = 0f
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun portalRegion(type: AbstractPortal.PortalType?): TextureRegion? {
        return when (type) {
            AbstractPortal.PortalType.CUBE -> cubePortalRegion
            AbstractPortal.PortalType.SHIP -> shipPortalRegion
            AbstractPortal.PortalType.GRAVITY -> gravityPortalRegion
            AbstractPortal.PortalType.MINI -> miniPortalRegion
            else -> null
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * min(t, 1f)
    }

    fun dispose() {
        shape.dispose()
    }
}
