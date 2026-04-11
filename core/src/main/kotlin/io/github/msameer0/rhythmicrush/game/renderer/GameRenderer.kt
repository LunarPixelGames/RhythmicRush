package io.github.msameer0.rhythmicrush.game.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.ObjectMap
import io.github.msameer0.rhythmicrush.atlas.AtlasManager
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.AbstractOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.settings.SettingsManager
import kotlin.math.min

/**
 * Primary renderer for the game world, handling sprites, shapes, backgrounds, and the player.
 */
class GameRenderer(
    private val world: GameWorld,
    private val camera: OrthographicCamera,
    private val batch: SpriteBatch,
    private val settings: SettingsManager,
    atlasManager: AtlasManager
) {

    companion object {
        private const val CAMERA_X_OFFSET = 425f
        private const val CUBE_SPIN_FACTOR = 0.5f
        private const val SHIP_TILT_FACTOR = 0.18f
        private const val SHIP_MAX_TILT = 50f
        private const val SHIP_TILT_LERP = 8f

        private val FALLBACK_CUBE_PORTAL = Color(0f, 0.8f, 0f, 1f)
        private val FALLBACK_SHIP_PORTAL = Color(0f, 0.5f, 1f, 1f)
        private val FALLBACK_YELLOW_ORB = Color(1f, 0.9f, 0.1f, 1f)
    }

    private val shape = ShapeRenderer()
    private val hitboxRenderer = HitboxRenderer(world, shape)

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

    private val orbRegions = ObjectMap<AbstractOrb.OrbType, TextureRegion>()

    var playerVisualRotation = 0f
        private set

    private var _lastDelta = 0f

    init {
        val types = BlockType.entries.toTypedArray()
        blockRegionsByOrdinal = arrayOfNulls(types.size)
        for (type in types) {
            blockRegionsByOrdinal[type.ordinal] =
                atlasManager.blocksAtlas.findRegion(type.textureName)
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

        orbRegions.put(AbstractOrb.OrbType.YELLOW, atlasManager.orbsAtlas.findRegion("yellow_orb"))
        orbRegions.put(AbstractOrb.OrbType.BLUE, atlasManager.orbsAtlas.findRegion("blue_orb"))
        orbRegions.put(AbstractOrb.OrbType.PINK, atlasManager.orbsAtlas.findRegion("pink_orb"))
        orbRegions.put(AbstractOrb.OrbType.BLACK, atlasManager.orbsAtlas.findRegion("black_orb"))
        orbRegions.put(AbstractOrb.OrbType.GREEN, atlasManager.orbsAtlas.findRegion("green_orb"))
        orbRegions.put(AbstractOrb.OrbType.RED, atlasManager.orbsAtlas.findRegion("red_orb"))
    }

    @JvmOverloads
    fun render(
        delta: Float,
        paused: Boolean,
        showHitboxes: Boolean,
        bgTexture: com.badlogic.gdx.graphics.Texture? = null,
        bgColor: Color = world.backgroundColor,
        beatIntensity: Float = 0f
    ) {
        _lastDelta = delta
        val player = world.player ?: return

        val rightEdge = camera.position.x + camera.viewportWidth / 2f + 100f

        shape.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined
        batch.begin()

        if (bgTexture != null) {
            drawBackground(bgTexture, bgColor)
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        batch.color = Color.WHITE

        drawSawBlades(paused, rightEdge)
        drawMainPass(player, delta, paused, rightEdge, beatIntensity)

        batch.end()

        // 2. Shape Pass: Fallbacks, Ground, and Hitboxes
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shape.begin(ShapeRenderer.ShapeType.Filled)

        drawPortalFallbacks(rightEdge)
        drawGround()
        
        shape.end()
        
        if (showHitboxes) hitboxRenderer.draw(camera, player, rightEdge)

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawBackground(texture: com.badlogic.gdx.graphics.Texture, color: Color) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.color = color

        val viewW = camera.viewportWidth
        val viewH = camera.viewportHeight
        val x = camera.position.x - viewW / 2f
        val y = camera.position.y - viewH / 2f

        batch.draw(texture, x, y, viewW, viewH)
    }

    fun updateCamera(player: AbstractPlayer) {
        camera.position.x = player.x + CAMERA_X_OFFSET
        if (player.isMini()) camera.position.x -= 12.5f
        camera.update()

        val worldLeft = camera.position.x - camera.viewportWidth / 2f
        world.cullX = worldLeft
    }

    private fun drawSawBlades(paused: Boolean, rightEdge: Float) {
        val cullStart = world.hazardCull
        for (i in cullStart until world.hazards.size) {
            val hazard = world.hazards.get(i)
            if (hazard.x > rightEdge) break
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
    }

    private fun drawPortalFallbacks(rightEdge: Float) {
        val cullStart = world.portalCull
        for (i in cullStart until world.portals.size) {
            val portal = world.portals.get(i)
            if (portal.x > rightEdge) break
            val pType = portal.type
            val region = portalRegion(pType)
            if (region == null) {
                shape.color =
                    if (pType == AbstractPortal.PortalType.CUBE) FALLBACK_CUBE_PORTAL else FALLBACK_SHIP_PORTAL
                shape.rect(portal.x, portal.y, portal.width, portal.height)
            }
        }
    }

    private fun drawMainPass(player: AbstractPlayer, delta: Float, paused: Boolean, rightEdge: Float, beatIntensity: Float = 0f) {
        drawPortals(rightEdge)
        drawHazards(rightEdge)
        drawBlocks(rightEdge)
        drawOrbs(rightEdge, beatIntensity)
        updatePlayerRotation(player, delta, paused)
        drawPlayer(player)
    }

    private fun drawPortals(rightEdge: Float) {
        val cullStart = world.portalCull
        for (i in cullStart until world.portals.size) {
            val portal = world.portals.get(i)
            if (portal.x > rightEdge) break
            val region = portalRegion(portal.type)
            if (region != null) {
                batch.draw(
                    region,
                    portal.x, portal.y,
                    portal.width / 2f, portal.height / 2f,
                    portal.width, portal.height,
                    1f, 1f, portal.rotation
                )
            }
        }
    }

    private fun drawHazards(rightEdge: Float) {
        val cullStart = world.hazardCull
        for (i in cullStart until world.hazards.size) {
            val hazard = world.hazards.get(i)
            if (hazard.x > rightEdge) break
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
                }

                else -> {}
            }
        }
    }

    private fun drawBlocks(rightEdge: Float) {
        val cullStart = world.blockCull
        for (i in cullStart until world.blocks.size) {
            val block = world.blocks.get(i)
            if (block.x > rightEdge) break
            val region = (if (block is Slope) slopeRegion else null)
                ?: blockRegionsByOrdinal[block.type.ordinal]
            if (region != null) {
                batch.draw(
                    region,
                    block.x, block.y,
                    block.width / 2f, block.height / 2f,
                    block.width, block.height,
                    1f, 1f, block.rotation
                )
            }
        }
    }

    private fun drawOrbs(rightEdge: Float, beatIntensity: Float = 0f) {
        val orbs = world.orbs
        val cullStart = world.orbCull
        
        val doPulse = settings.pulseOrbs
        val scale = if (doPulse) 0.65f + (beatIntensity * 0.70f) else 1f
        
        for (i in cullStart until orbs.size) {
            val orb = orbs.get(i)
            if (orb.x > rightEdge) break
            val region = orbRegions[orb.type]
            if (region != null) {
                val visualW = orb.width * scale
                val visualH = orb.height * scale
                val visualX = orb.x + (orb.width - visualW) / 2f
                val visualY = orb.y + (orb.height - visualH) / 2f
                
                batch.draw(region, visualX, visualY, visualW, visualH)
            } else {
                drawOrbFallback(orb)
            }
        }
    }

    private fun drawOrbFallback(orb: AbstractOrb) {
        val oldColor = shape.color.cpy()
        shape.color = FALLBACK_YELLOW_ORB
        shape.circle(
            orb.x + orb.width / 2f,
            orb.y + orb.height / 2f,
            orb.width / 2f, 24
        )
        shape.color = oldColor
    }

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

    private fun drawGround() {
        val worldWidth = camera.viewportWidth
        val worldLeft = camera.position.x - worldWidth / 2f

        shape.color = world.groundColor
        shape.rect(worldLeft, 0f, worldWidth, world.groundY)
    }

    private fun updatePlayerRotation(player: AbstractPlayer, delta: Float, paused: Boolean) {
        val vy = player.velocityY
        val slopeRot = player.getCurrentSlopeRotation()
        val pType = player.getType()

        if (pType == AbstractPlayer.PlayerType.CUBE) {
            if (player.isGrounded()) {
                val nearest90 = MathUtils.round((playerVisualRotation - slopeRot) / 90f) * 90f
                playerVisualRotation =
                    MathUtils.lerp(playerVisualRotation, nearest90 + slopeRot, min(delta * 15f, 1f))
            } else if (!world.isPlayerDead && !paused) {
                val t = delta * 60f
                val rotation =
                    (kotlin.math.abs(vy) * CUBE_SPIN_FACTOR / 60f + 5f / 60f) * t + 300f * delta
                if (player.isGravityFlipped()) playerVisualRotation += rotation
                else playerVisualRotation -= rotation
            }
        } else if (pType == AbstractPlayer.PlayerType.SHIP) {
            var targetAngle = MathUtils.clamp(vy * SHIP_TILT_FACTOR, -SHIP_MAX_TILT, SHIP_MAX_TILT)
            if (player.isGrounded()) targetAngle += slopeRot
            playerVisualRotation =
                MathUtils.lerp(playerVisualRotation, targetAngle, min(SHIP_TILT_LERP * delta, 1f))
        } else {
            playerVisualRotation = 0f
        }
    }

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
