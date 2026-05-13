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
import io.github.msameer0.rhythmicrush.game.GameCamera
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.AbstractOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads.AbstractPad
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
    atlasManager: AtlasManager,
    private val customCamera: GameCamera? = null
) {

    companion object {
        private const val CAMERA_X_OFFSET = 307f

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
    private val cubePortalBackRegion: TextureRegion?
    private val cubePortalFrontRegion: TextureRegion?
    private val shipPortalBackRegion: TextureRegion?
    private val shipPortalFrontRegion: TextureRegion?
    private val gravityPortalRegion: TextureRegion?
    private val miniPortalRegion: TextureRegion?

    private val orbRegions = ObjectMap<AbstractOrb.OrbType, TextureRegion>()
    private val padRegions = ObjectMap<AbstractPad.PadType, TextureRegion>()

    var playerVisualRotation = 0f
        private set
    
    private var boundaryProgress = 0f

    private var _lastDelta = 0f

    fun reset() {
        boundaryProgress = 0f
    }

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
        
        cubePortalBackRegion = atlasManager.portalsBackAtlas.findRegion("cube")
        cubePortalFrontRegion = atlasManager.portalsFrontAtlas.findRegion("cube")
        shipPortalBackRegion = atlasManager.portalsBackAtlas.findRegion("ship")
        shipPortalFrontRegion = atlasManager.portalsFrontAtlas.findRegion("ship")
        
        gravityPortalRegion = atlasManager.portalsAtlas.findRegion("gravity_portal")
        miniPortalRegion = atlasManager.portalsAtlas.findRegion("mini_portal")

        orbRegions.put(AbstractOrb.OrbType.YELLOW, atlasManager.orbsAtlas.findRegion("yellow_orb"))
        orbRegions.put(AbstractOrb.OrbType.BLUE, atlasManager.orbsAtlas.findRegion("blue_orb"))
        orbRegions.put(AbstractOrb.OrbType.PINK, atlasManager.orbsAtlas.findRegion("pink_orb"))
        orbRegions.put(AbstractOrb.OrbType.BLACK, atlasManager.orbsAtlas.findRegion("black_orb"))
        orbRegions.put(AbstractOrb.OrbType.GREEN, atlasManager.orbsAtlas.findRegion("green_orb"))
        orbRegions.put(AbstractOrb.OrbType.RED, atlasManager.orbsAtlas.findRegion("red_orb"))

        padRegions.put(AbstractPad.PadType.YELLOW, atlasManager.padsAtlas.findRegion("yellow_pad"))
        padRegions.put(AbstractPad.PadType.BLUE, atlasManager.padsAtlas.findRegion("blue_pad"))
        padRegions.put(AbstractPad.PadType.PINK, atlasManager.padsAtlas.findRegion("pink_pad"))
        padRegions.put(AbstractPad.PadType.RED, atlasManager.padsAtlas.findRegion("red_pad"))
        padRegions.put(AbstractPad.PadType.BLACK, atlasManager.padsAtlas.findRegion("black_pad"))
        padRegions.put(AbstractPad.PadType.GREEN, atlasManager.padsAtlas.findRegion("green_pad"))
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
        val targetProgress = if (world.player?.getType() == AbstractPlayer.PlayerType.SHIP) 1f else 0f
        boundaryProgress = MathUtils.lerp(boundaryProgress, targetProgress, min(delta * 10f, 1f))
        
        _lastDelta = delta
        val player = world.player ?: return

        val rightEdge = camera.position.x + camera.viewportWidth / 2f + 100f

        shape.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined
        batch.begin()

        if (bgTexture != null) {
            drawBackground(bgTexture, bgColor)
        }

        if (showHitboxes) {
            batch.end()
            shape.begin(ShapeRenderer.ShapeType.Filled)
            drawCameraDebug()
            shape.end()
            batch.begin()
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        batch.color = Color.WHITE

        drawSawBlades(paused, rightEdge)
        
        // Pass 1: Background elements
        drawPortalsBack(rightEdge)
        drawHazards(rightEdge)
        drawBlocks(rightEdge)
        drawOrbs(rightEdge, beatIntensity)
        drawPads(rightEdge, beatIntensity)
        
        // Pass 2: Player
        drawPlayer(player)
        
        // Pass 3: Foreground elements
        drawPortalsFront(rightEdge)

        batch.end()

        // 2. Shape Pass: Fallbacks, Ground, and Hitboxes
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shape.begin(ShapeRenderer.ShapeType.Filled)

        drawPortalFallbacks(rightEdge)
        drawOrbFallbacks(rightEdge)
        drawPadFallbacks(rightEdge)
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

    private fun drawPortalsBack(rightEdge: Float) {
        val cullStart = world.portalCull
        for (i in cullStart until world.portals.size) {
            val portal = world.portals.get(i)
            if (portal.x > rightEdge) break
            
            if (portal.type == AbstractPortal.PortalType.CUBE || portal.type == AbstractPortal.PortalType.SHIP) {
                val region = if (portal.type == AbstractPortal.PortalType.CUBE) cubePortalBackRegion else shipPortalBackRegion
                if (region != null) {
                    val visualH = portal.height
                    val visualW = visualH * (150f / 384f)
                    val drawX = portal.x + (portal.width - visualW) / 2f
                    val drawY = portal.y
                    batch.draw(region, drawX, drawY, visualW / 2f, visualH / 2f, visualW, visualH, 1f, 1f, portal.rotation)
                }
            } else {
                val region = portalRegion(portal.type)
                if (region != null) {
                    batch.draw(region, portal.x, portal.y, portal.width / 2f, portal.height / 2f, portal.width, portal.height, 1f, 1f, portal.rotation)
                }
            }
        }
    }

    private fun drawPortalsFront(rightEdge: Float) {
        val cullStart = world.portalCull
        for (i in cullStart until world.portals.size) {
            val portal = world.portals.get(i)
            if (portal.x > rightEdge) break
            
            if (portal.type == AbstractPortal.PortalType.CUBE || portal.type == AbstractPortal.PortalType.SHIP) {
                val region = if (portal.type == AbstractPortal.PortalType.CUBE) cubePortalFrontRegion else shipPortalFrontRegion
                if (region != null) {
                    val visualH = portal.height
                    val visualW = visualH * (150f / 384f)
                    val drawX = portal.x + (portal.width - visualW) / 2f
                    val drawY = portal.y
                    batch.draw(region, drawX, drawY, visualW / 2f, visualH / 2f, visualW, visualH, 1f, 1f, portal.rotation)
                }
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
            }
        }
    }

    private fun drawOrbFallbacks(rightEdge: Float) {
        val orbs = world.orbs
        val cullStart = world.orbCull
        for (i in cullStart until orbs.size) {
            val orb = orbs.get(i)
            if (orb.x > rightEdge) break
            if (orbRegions[orb.type] == null) {
                shape.color = FALLBACK_YELLOW_ORB
                shape.circle(
                    orb.x + orb.width / 2f,
                    orb.y + orb.height / 2f,
                    orb.width / 2f, 24
                )
            }
        }
    }

    private fun drawPads(rightEdge: Float, beatIntensity: Float = 0f) {
        val pads = world.pads
        val cullStart = world.padCull

        for (i in cullStart until pads.size) {
            val pad = pads.get(i)
            if (pad.x > rightEdge) break
            val region = padRegions[pad.type]
            if (region != null) {
                batch.draw(
                    region, pad.x, pad.y,
                    pad.width / 2f, pad.height / 2f,
                    pad.width, pad.height,
                    1f, 1f, pad.rotation
                )
            }
        }
    }

    private fun drawPadFallbacks(rightEdge: Float) {
        val pads = world.pads
        val cullStart = world.padCull
        for (i in cullStart until pads.size) {
            val pad = pads.get(i)
            if (pad.x > rightEdge) break
            if (padRegions[pad.type] == null) {
                shape.color = FALLBACK_YELLOW_ORB
                shape.rect(
                    pad.x, pad.y,
                    pad.width / 2f, pad.height / 2f,
                    pad.width, pad.height,
                    1f, 1f, pad.rotation
                )
            }
        }
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
            scaleX, scaleY, player.getRotation()
        )
    }

    private fun drawGround() {
        val player = world.player ?: return
        val worldWidth = camera.viewportWidth
        val worldLeft = camera.position.x - worldWidth / 2f
        val viewportHeight = camera.viewportHeight
        val screenBottom = camera.position.y - viewportHeight / 2f
        val screenTop = camera.position.y + viewportHeight / 2f
        val bp = boundaryProgress
        val isCorridor = player.isUsingCorridor()

        // 1. Draw Real Ground (Hide only if fully in a high-air corridor)
        if (!isCorridor || bp < 0.99f) {
            shape.color = world.groundColor
            shape.rect(worldLeft, 0f, worldWidth, world.groundY)
            shape.color = Color.WHITE
            shape.rect(worldLeft, world.groundY, worldWidth, 5f)
        }

        // 2. Draw Ship Mode Boundaries (Animated Slide-in)
        if (bp > 0.001f) {
            // Case 2: High Air Corridor
            if (isCorridor) {
                val targetCeilingTop = player.getCorridorTop() ?: 1080f
                val targetFloorBottom = player.getCorridorBottom() ?: 0f

                // Top Boundary (Ceiling) - Surface is targetCeilingTop
                val ceilingBottomY = screenTop - (screenTop - targetCeilingTop) * bp
                val ceilingTopY = kotlin.math.max(screenTop, targetCeilingTop)
                shape.color = world.groundColor
                shape.rect(worldLeft, ceilingBottomY, worldWidth, ceilingTopY - ceilingBottomY)
                shape.color = Color.WHITE
                shape.rect(worldLeft, ceilingBottomY - 5f, worldWidth, 5f)

                // Bottom Boundary (Fake Floor) - Surface is targetFloorBottom
                val floorTopY = screenBottom + (targetFloorBottom - screenBottom) * bp
                val floorBottomY = kotlin.math.min(screenBottom, targetFloorBottom)
                shape.color = world.groundColor
                shape.rect(worldLeft, floorBottomY, worldWidth, floorTopY - floorBottomY)
                shape.color = Color.WHITE
                shape.rect(worldLeft, floorTopY, worldWidth, 5f)
            } else {
                // Case 1: Near Ground - Only need the CEILING boundary
                // Note: Bottom ground is already handled by the "Real Ground" draw call above
                
                val targetCeilingBottom = screenTop - 39f
                val ceilingBottomY = screenTop - (screenTop - targetCeilingBottom) * bp
                shape.color = world.groundColor
                shape.rect(worldLeft, ceilingBottomY, worldWidth, 39f)
                shape.color = Color.WHITE
                shape.rect(worldLeft, ceilingBottomY - 5f, worldWidth, 5f)
            }
        }
    }

    private fun drawCameraDebug() {
        val player = world.player ?: return
        val cam = customCamera ?: return
        if (player.getType() != AbstractPlayer.PlayerType.CUBE) return

        shape.color = Color(1f, 1f, 1f, 0.15f)
        shape.rect(player.x, cam.getWindowBottom(), player.width, cam.getPaddingHeight())
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
