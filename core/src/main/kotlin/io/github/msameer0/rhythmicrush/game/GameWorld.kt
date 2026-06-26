package io.github.msameer0.rhythmicrush.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.GameConstants
import io.github.msameer0.rhythmicrush.game.engine.Tickable
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.AbstractOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.BlackOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.BlueOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.GreenOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.PinkOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.RedOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.YellowOrb
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads.AbstractPad
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads.BlackPad
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads.BluePad
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads.GreenPad
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads.PinkPad
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads.RedPad
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads.YellowPad
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.GravityPortal
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.MiniPortal
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.level.LevelData
import io.github.msameer0.rhythmicrush.game.level.PatternShape
import io.github.msameer0.rhythmicrush.game.registries.Registries
import io.github.msameer0.rhythmicrush.game.trigger.AbstractTrigger
import io.github.msameer0.rhythmicrush.game.trigger.ColorTrigger
import io.github.msameer0.rhythmicrush.game.trigger.PulseTrigger
import kotlin.math.min
import kotlin.math.ceil

/**
 * Represents the core game state, including the player, level objects, and world simulation logic.
 */
class GameWorld : Tickable {
    /** Represents one expanding ring in the player death effect. */
    class DeathBurst {
        var x: Float = 0f
        var y: Float = 0f
        var startRadius: Float = 0f
        var endRadius: Float = 0f
        var startDelay: Float = 0f
        var duration: Float = 0f
        var maxAlpha: Float = 0f
        var age: Float = 0f

        fun init(
            x: Float, y: Float, startRadius: Float, endRadius: Float,
            startDelay: Float, duration: Float, maxAlpha: Float
        ): DeathBurst {
            this.x = x; this.y = y
            this.startRadius = startRadius; this.endRadius = endRadius
            this.startDelay = startDelay; this.duration = duration
            this.maxAlpha = maxAlpha; this.age = 0f
            return this
        }

        fun reset() { age = 0f }

        private val minimumAlpha: Float
            get() = 0.07f

        val activeAge: Float
            get() = age - startDelay

        val isActive: Boolean
            get() = activeAge >= 0f

        val progress: Float
            get() = if (!isActive) 0f else min(activeAge / duration, 1f)

        val radius: Float
            get() = startRadius + (endRadius - startRadius) * progress

        val alpha: Float
            get() = if (!isActive) 0f else
                minimumAlpha + (maxAlpha - minimumAlpha) * (1f - progress * progress * progress)

        val brightness: Float
            get() = if (!isActive) 0.55f else 0.55f + 0.4f * progress
    }

    class PortalGlow {
        var x: Float = 0f
        var y: Float = 0f
        val color: Color = Color()
        var startRadius: Float = 24f
        var endRadius: Float = GameConstants.Editor.GRID_SIZE * 1.5f
        var duration: Float = 0.48f
        var maxAlpha: Float = 0.42f
        var age: Float = 0f

        fun init(x: Float, y: Float, color: Color): PortalGlow {
            this.x = x; this.y = y
            this.color.set(color)
            this.startRadius = 24f
            this.endRadius = GameConstants.Editor.GRID_SIZE * 1.5f
            this.duration = 0.48f
            this.maxAlpha = 0.42f
            this.age = 0f
            return this
        }

        fun reset() { age = 0f }

        private val minimumAlpha: Float
            get() = 0.07f

        val progress: Float
            get() = min(age / duration, 1f)
        val radius: Float
            get() = startRadius + (endRadius - startRadius) * progress
        val alpha: Float
            get() =
                minimumAlpha + (maxAlpha - minimumAlpha) * (1f - progress * progress * progress)
    }

    companion object {
        private const val COLLISION_LOOKAHEAD = 2800f
        private const val UPPER_DEATH_MARGIN_BLOCKS = 25f
        private const val END_WALL_OFFSET_BLOCKS = 11f
        private const val END_CAPTURE_HOLD_DURATION = 0.5f
        private const val END_CAPTURE_PULL_DURATION = 1.15f
        private const val END_ABSORPTION_DELAY = 1.5f
        private const val END_CAPTURE_ARC_HEIGHT = 180f
        private val GREEN_PORTAL_GLOW = Color.valueOf("54FF78")
        private val PINK_PORTAL_GLOW = Color.valueOf("FF55D7")
        private val YELLOW_ORB_GLOW = Color.valueOf("FFE34A")
        private val BLUE_ORB_GLOW = Color.valueOf("55A7FF")
        private val PINK_ORB_GLOW = Color.valueOf("FF55D7")
        private val RED_ORB_GLOW = Color.valueOf("FF5252")
        private val BLACK_ORB_GLOW = Color.valueOf("9B72CF")
        private val GREEN_ORB_GLOW = Color.valueOf("54FF78")

        private fun parseOptionalHex(hex: String?): Color? {
            return if (!hex.isNullOrEmpty()) hexToColor(hex) else null
        }

        @JvmStatic
        fun hexToColor(hexStr: String?): Color {
            return hexToColor(hexStr, Color())
        }

        @JvmStatic
        fun hexToColor(hexStr: String?, out: Color): Color {
            var hex = hexStr
            if (hex.isNullOrEmpty()) return out.set(0f, 0f, 0f, 1f)
            if (hex.startsWith("#")) hex = hex.substring(1)
            val colorValue = hex.toLong(16)
            val red = (colorValue shr 16 and 0xFF).toFloat() / 255f
            val green = (colorValue shr 8 and 0xFF).toFloat() / 255f
            val blue = (colorValue and 0xFF).toFloat() / 255f
            return out.set(red, green, blue, 1f)
        }

        private fun resolveBlockType(textureName: String?): BlockType {
            if (textureName != null) {
                for (blockType in BlockType.entries) {
                    if (blockType.textureName == textureName) return blockType
                }
            }
            return BlockType.DEFAULT
        }
    }

    private val pools = WorldPoolManager()
    private val colors = ColorStateManager()

    private val deathBurstPool = Array<DeathBurst>()
    private val portalGlowPool = Array<PortalGlow>()

    var currentLoudness: Float = 0f
    var targetLoudness: Float = 0f

    var bgImage: String = ""
    var bgShape: PatternShape? = null
        private set
    var groundShape: PatternShape? = null
        private set
    var decorationSeed: Int = 0
        private set
    var player: AbstractPlayer? = null
        private set
    val groundY = GameConstants.World.GROUND_Y
    var scrollSpeed = GameConstants.World.SCROLL_SPEED

    var isPlayerDead = false
    var isLevelComplete = false
    var worldScrolled = 0f
    private var levelEndX = 0f
    var endWallScreenX = Float.MAX_VALUE
        private set
    var endCaptureActive = false
        private set
    var endCaptureProgress = 0f
        private set
    var endCaptureShakeStrength = 0f
        private set
    var playerAbsorbed = false
        private set
    val endSequenceMusicFadeProgress: Float
        get() = (
            endCaptureAge /
                (END_CAPTURE_HOLD_DURATION + END_CAPTURE_PULL_DURATION + END_ABSORPTION_DELAY)
            ).coerceIn(0f, 1f)
    var endWallVisibleCenterY = 540f
    var endWallLockX = Float.MAX_VALUE
    var cameraWindowBottom = 0f
    var overrideCameraY: Float? = null
    var overrideCameraWindowBottom: Float? = null

    fun overrideCameraState(y: Float, windowBottom: Float) {
        overrideCameraY = y
        overrideCameraWindowBottom = windowBottom
    }
    private var endCaptureStartY = 0f
    private var endCaptureTargetY = 0f
    private var endCaptureStartX = 0f
    private var endCaptureTargetX = 0f
    private var endCaptureAge = 0f
    private var upperDeathY = GameConstants.World.GROUND_Y +
        UPPER_DEATH_MARGIN_BLOCKS * GameConstants.Editor.GRID_SIZE
    var cullX = 0f
    var boundaryBottom: Float = -Float.MAX_VALUE
    var boundaryTop: Float = Float.MAX_VALUE

    var blockCull = 0
    private var blockStart = 0
    var hazardCull = 0
    private var hazardStart = 0
    var portalCull = 0
    private var portalStart = 0
    var orbCull = 0
    private var orbStart = 0
    var padCull = 0
    private var padStart = 0
    var triggerIdx = 0

    val portals = Array<AbstractPortal>()
    val hazards = Array<AbstractHazard>()
    val blocks = Array<Block>()
    val orbs = Array<AbstractOrb>()
    val pads = Array<AbstractPad>()



    private val triggers = Array<AbstractTrigger>()
    private var currentLevelData: LevelData? = null
    val deathBursts = Array<DeathBurst>()
    val portalGlows = Array<PortalGlow>()

    init {
        player = pools.obtainCube().init(100f, groundY)
        player?.setWorld(this)
        Gdx.app.log("GameWorld", "Player initialized.")
    }

    fun obtainPlayer(typeId: String): AbstractPlayer {
        if ("cube" == typeId) return pools.obtainCube()
        if ("ship" == typeId) return pools.obtainShip()
        return Registries.PLAYERS.create(typeId) ?: pools.obtainCube()
    }

    fun setPlayer(next: AbstractPlayer) {
        freePlayer()
        player = next
        next.setWorld(this)
    }

    private fun freePlayer() {
        player?.let { pools.freePlayer(it) }
        player = null
    }

    fun startBgFade(target: Color, duration: Float) {
        colors.startBgFade(target, duration)
    }

    fun startGroundFade(target: Color, duration: Float) {
        colors.startGroundFade(target, duration)
    }

    fun startBgPulse(target: Color, fadeIn: Float, hold: Float, fadeOut: Float) {
        colors.startBgPulse(target, fadeIn, hold, fadeOut)
    }

    fun startGroundPulse(target: Color, fadeIn: Float, hold: Float, fadeOut: Float) {
        colors.startGroundPulse(target, fadeIn, hold, fadeOut)
    }

    fun updateVisuals(delta: Float) {
        updateDeathBursts(delta)
        updatePortalGlows(delta)
        if (isPlayerDead || isLevelComplete) return
        colors.update(delta)

        val lerpSpeed = if (targetLoudness > currentLoudness) 25f else 10f
        currentLoudness += (targetLoudness - currentLoudness) * kotlin.math.min(delta * lerpSpeed, 1f)
    }

    fun updateLoudness(intensity: Float) {
        targetLoudness = intensity
    }

    fun loadLevel(data: LevelData) {
        loadLevel(data, 0f, true)
    }

    fun fastForwardTo(scrolled: Float) {
        worldScrolled = scrolled
        currentLevelData?.let { loadLevel(it, scrolled, false) }
    }

    fun loadLevel(data: LevelData, startScrolled: Float, resetPlayer: Boolean) {
        Gdx.app.log("GameWorld", "Loading level: ${data.name} at scrolled=$startScrolled")
        currentLevelData = data
        freeAllActiveObjects()

        triggers.clear()
        colors.cancelTransitions()
        isPlayerDead = false
        isLevelComplete = false
        deathBursts.clear()
        portalGlows.clear()
        worldScrolled = startScrolled
        endCaptureActive = false
        endCaptureProgress = 0f
        endCaptureShakeStrength = 0f
        playerAbsorbed = false
        endCaptureAge = 0f
        bgImage = data.bgImage ?: ""
        bgShape = PatternShape.fromId(data.bgShape)
        groundShape = PatternShape.fromId(data.groundShape)
        decorationSeed = data.name.hashCode() xor (data.id * 0x45d9f3b)
        currentLoudness = 0f
        targetLoudness = 0f

        val bg = if (!data.bgColor.isNullOrEmpty()) data.bgColor else "1a1a2e"
        val gnd = if (!data.groundColor.isNullOrEmpty()) data.groundColor else "16213e"
        val bgColor = hexToColor(bg)
        val groundColor = hexToColor(gnd)
        colors.baseBgColor.set(bgColor)
        colors.baseGroundColor.set(groundColor)
        colors.backgroundColor.set(bgColor)
        colors.groundColor.set(groundColor)

        for (objectEntry in data.objects) {
            val renderedX = objectEntry.x - startScrolled
            spawnObject(objectEntry, renderedX, startScrolled)
        }

        levelEndX = calculateEndWallWorldX(data)
        endWallScreenX = levelEndX - startScrolled
        upperDeathY = calculateUpperDeathY(data)

        blocks.sort { a, b2 -> a.x.compareTo(b2.x) }
        hazards.sort { a, b2 -> a.x.compareTo(b2.x) }
        portals.sort { a, b2 -> a.x.compareTo(b2.x) }
        orbs.sort { a, b2 -> a.x.compareTo(b2.x) }
        pads.sort { a, b2 -> a.x.compareTo(b2.x) }
        triggers.sort { a, b2 -> a.worldX.compareTo(b2.worldX) }

        triggerIdx = 0
        val playerWorldX = 100f + startScrolled
        while (triggerIdx < triggers.size && triggers.get(triggerIdx).worldX <= playerWorldX) {
            triggerIdx++
        }

        if (resetPlayer) {
            freePlayer()
            player = obtainPlayer("cube").init(GameConstants.Player.START_X, GameConstants.World.GROUND_Y)
            player?.worldX = GameConstants.Player.START_X + worldScrolled
            player?.setWorld(this)
        }
    }

    private fun calculateUpperDeathY(data: LevelData): Float {
        var highestObjectTop = groundY

        for (objectEntry in data.objects) {
            if (Registries.TRIGGERS.has(objectEntry.type)) continue

            val objectHeight = when {
                Registries.PORTALS.has(objectEntry.type) -> {
                    val snappedRotation =
                        (Math.round(objectEntry.rotation / 90f) * 90 % 360 + 360) % 360
                    if (snappedRotation == 90 || snappedRotation == 270) 100f else 250f
                }
                Registries.ORBS.has(objectEntry.type) -> 110f
                objectEntry.type == "spike" || objectEntry.type == "half_spike" -> 100f
                else -> objectEntry.size
            }

            highestObjectTop = maxOf(highestObjectTop, objectEntry.y + objectHeight)
        }

        return highestObjectTop + UPPER_DEATH_MARGIN_BLOCKS * GameConstants.Editor.GRID_SIZE
    }

    private fun calculateEndWallWorldX(data: LevelData): Float {
        var lastOccupiedRight = 0f
        for (objectEntry in data.objects) {
            if (Registries.TRIGGERS.has(objectEntry.type)) continue
            lastOccupiedRight = maxOf(lastOccupiedRight, objectEntry.x + objectEntry.size)
        }
        val lastOccupiedGridX = if (lastOccupiedRight > 0f) {
            (ceil(lastOccupiedRight / GameConstants.Editor.GRID_SIZE) - 1f) *
                GameConstants.Editor.GRID_SIZE
        } else {
            0f
        }
        return lastOccupiedGridX +
            END_WALL_OFFSET_BLOCKS * GameConstants.Editor.GRID_SIZE
    }

    private fun spawnObject(e: LevelData.ObjectEntry, rx: Float, startScrolled: Float) {
        if (Registries.BLOCKS.has(e.type)) {
            if (e.x + e.size < startScrolled - 400) return
            val bt = resolveBlockType(e.blockType)
            if ("slope" == e.type) {
                blocks.add(pools.obtainSlope().init(e.x, e.y, e.size, bt, e.rotation))
            } else {
                val block = pools.obtainBlock().init(e.x, e.y, e.size, bt, e.rotation)
                block.untouchable = e.untouchable
                blocks.add(block)
            }
        } else if (Registries.HAZARDS.has(e.type)) {
            val hW = if ("saw_blade" == e.type) e.size else 100f
            if (e.x + hW < startScrolled - 200) return
            spawnHazard(e, e.x)
        } else if (Registries.PORTALS.has(e.type)) {
            if (e.x + 100f < startScrolled - 200) return
            spawnPortal(e, e.x)
        } else if (Registries.ORBS.has(e.type)) {
            if (e.x + e.size < startScrolled - 200) return
            spawnOrb(e, e.x)
        } else if (Registries.PADS.has(e.type)) {
            if (e.x + e.size < startScrolled - 100) return
            spawnPad(e, e.x)
        } else if (Registries.TRIGGERS.has(e.type)) {
            spawnTrigger(e)
        }
    }

    private fun spawnHazard(e: LevelData.ObjectEntry, rx: Float) {
        when (e.type) {
            "spike" -> {
                val spike = pools.obtainSpike().init(rx, e.y, e.rotation)
                hazards.add(spike)
            }

            "half_spike" -> {
                val halfSpike = pools.obtainHalfSpike().init(rx, e.y, e.rotation)
                hazards.add(halfSpike)
            }

            "saw_blade" -> {
                val sawBlade = pools.obtainSawBlade().init(rx, e.y, e.size, e.rotation)
                hazards.add(sawBlade)
            }
        }
    }

    private fun spawnPortal(e: LevelData.ObjectEntry, rx: Float) {
        val portal = when (e.type) {
            "cube_portal" -> pools.obtainCubePortal()
            "ship_portal" -> pools.obtainShipPortal()
            "gravity_portal" -> pools.obtainGravityPortal()
            "mini_portal" -> pools.obtainMiniPortal()
            else -> null
        } ?: return
        portal.init(rx, e.y, e.rotation)
        portals.add(portal)
    }

    private fun spawnOrb(e: LevelData.ObjectEntry, rx: Float) {
        var orb: AbstractOrb? = null
        when (e.type) {
            "yellow_orb" -> {
                orb = pools.obtainYellowOrb().init(rx, e.y)
            }

            "blue_orb" -> {
                orb = pools.obtainBlueOrb().init(rx, e.y)
            }

            "pink_orb" -> {
                orb = pools.obtainPinkOrb().init(rx, e.y)
            }

            "red_orb" -> {
                orb = pools.obtainRedOrb().init(rx, e.y)
            }

            "black_orb" -> {
                orb = pools.obtainBlackOrb().init(rx, e.y)
            }

            "green_orb" -> {
                orb = pools.obtainGreenOrb().init(rx, e.y)
            }
        }
        if (orb != null) orbs.add(orb)
    }

    private fun spawnPad(e: LevelData.ObjectEntry, rx: Float) {
        var pad: AbstractPad? = null
        when (e.type) {
            "yellow_pad" -> {
                pad = pools.obtainYellowPad().init(rx, e.y, e.rotation)
            }

            "blue_pad" -> {
                pad = pools.obtainBluePad().init(rx, e.y, e.rotation)
            }

            "pink_pad" -> {
                pad = pools.obtainPinkPad().init(rx, e.y, e.rotation)
            }

            "red_pad" -> {
                pad = pools.obtainRedPad().init(rx, e.y, e.rotation)
            }

            "black_pad" -> {
                pad = pools.obtainBlackPad().init(rx, e.y, e.rotation)
            }

            "green_pad" -> {
                pad = pools.obtainGreenPad().init(rx, e.y, e.rotation)
            }
        }
        if (pad != null) pads.add(pad)
    }

    private fun spawnTrigger(e: LevelData.ObjectEntry) {
        val trigger = Registries.TRIGGERS.create(e.type)
        if (trigger is ColorTrigger) {
            val targetBg = parseOptionalHex(e.triggerBgColor)
            val targetGround = parseOptionalHex(e.triggerGroundColor)
            trigger.init(e.x, targetBg, targetGround, e.fadeDuration)
        } else if (trigger is PulseTrigger) {
            val pulseBg = parseOptionalHex(e.pulseBgColor)
            val pulseGround = parseOptionalHex(e.pulseGroundColor)
            trigger.init(e.x, pulseBg, pulseGround, e.fadeInTime, e.holdTime, e.fadeOutTime)
        }
        triggers.add(trigger)
    }

    fun reset() {
        val data = currentLevelData
        if (data != null) {
            loadLevel(data)
        } else {
            freeAllActiveObjects()
            triggers.clear()
            colors.reset()
            isPlayerDead = false
            isLevelComplete = false
            deathBursts.clear()
            portalGlows.clear()
            worldScrolled = 0f
            endCaptureActive = false
            endCaptureProgress = 0f
            endCaptureShakeStrength = 0f
            playerAbsorbed = false
            endCaptureAge = 0f
            endWallScreenX = Float.MAX_VALUE
            levelEndX = 0f
            freePlayer()
            player = obtainPlayer("cube").init(GameConstants.Player.START_X, groundY)
            player?.setWorld(this)
        }
    }

    override fun onInput(held: Boolean): Boolean {
        if (endCaptureActive) return true
        return player?.let {
            it.setJumpHeld(held)
            it.isGrounded() || !held
        } ?: true
    }

    override fun tick(delta: Float) {
        update(delta)
    }

    fun update(delta: Float) {
        if (isPlayerDead || isLevelComplete) return

        val currentPlayer = player ?: return
        endWallScreenX = levelEndX - worldScrolled
        if (!endCaptureActive && endWallScreenX <= endWallLockX) {
            endWallScreenX = endWallLockX
            endCaptureActive = true
            endCaptureAge = 0f
            endCaptureProgress = 0f
            endCaptureShakeStrength = 0.08f
            playerAbsorbed = false
            endCaptureStartX = currentPlayer.x
            endCaptureStartY = currentPlayer.y
            endCaptureTargetX =
                endWallLockX + GameConstants.Editor.GRID_SIZE / 2f - currentPlayer.width / 2f
            endCaptureTargetY = endWallVisibleCenterY - currentPlayer.height / 2f
            currentPlayer.setVelocityY(0f)
            currentPlayer.setGrounded(false)
        }

        if (endCaptureActive) {
            updateEndCapture(currentPlayer, delta)
            return
        }

        val slopeBeforeUpdate = currentPlayer.getCurrentSlopeRotation()
        currentPlayer.update(delta, maxOf(groundY, boundaryBottom), boundaryTop)

        val lowerKillZoneMargin = currentPlayer.height + 700f

        if (currentPlayer.y < -lowerKillZoneMargin || currentPlayer.y > upperDeathY) {
            playerDied()
            return
        }

        val remainingScrollToLock = (endWallScreenX - endWallLockX).coerceAtLeast(0f)
        val scrollAmount = min(scrollSpeed * delta, remainingScrollToLock)
        val scrollDelta = if (scrollSpeed > 0f) scrollAmount / scrollSpeed else 0f

        val playerX = currentPlayer.x
        val rangeMin = playerX - 600f
        val rangeMax = playerX + COLLISION_LOOKAHEAD
        val updateLimit = rangeMax + 1000f

        for (i in portalCull until portals.size) {
            val p = portals.get(i)
            if (p.worldX - worldScrolled > updateLimit) break
            p.updatePosition(worldScrolled)
        }
        for (i in hazardCull until hazards.size) {
            val h = hazards.get(i)
            if (h.worldX - worldScrolled > updateLimit) break
            h.updatePosition(worldScrolled)
        }
        for (i in blockCull until blocks.size) {
            val b = blocks.get(i)
            if (b.worldX - worldScrolled > updateLimit) break
            b.updatePosition(worldScrolled)
        }
        for (i in orbCull until orbs.size) {
            val o = orbs.get(i)
            if (o.worldX - worldScrolled > updateLimit) break
            o.updatePosition(worldScrolled, delta)
        }
        for (i in padCull until pads.size) {
            val p = pads.get(i)
            if (p.worldX - worldScrolled > updateLimit) break
            p.updatePosition(worldScrolled)
        }
        for (i in 0 until portalGlows.size) portalGlows[i].x -= scrollAmount

        if (blockStart < blockCull) blockStart = blockCull
        if (hazardStart < hazardCull) hazardStart = hazardCull
        if (portalStart < portalCull) portalStart = portalCull
        if (orbStart < orbCull) orbStart = orbCull

        while (blockStart < blocks.size && blocks.get(blockStart).x + blocks.get(blockStart).width < rangeMin) blockStart++
        while (hazardStart < hazards.size && hazards.get(hazardStart).x + hazards.get(hazardStart).width < rangeMin) hazardStart++
        while (portalStart < portals.size && portals.get(portalStart).x + portals.get(portalStart).width < rangeMin) portalStart++
        while (orbStart < orbs.size && orbs.get(orbStart).x + orbs.get(orbStart).width < rangeMin) orbStart++
        while (padStart < pads.size && pads.get(padStart).x + pads.get(padStart).width < rangeMin) padStart++

        for (i in blockStart until blocks.size) {
            val block = blocks.get(i)
            if (block.x > rangeMax) break
            if (block is Slope) {
                val requireDescendingSurfaceCrossing =
                    currentPlayer.getType() == AbstractPlayer.PlayerType.CUBE &&
                        slopeBeforeUpdate == 0f
                block.tryTouch(currentPlayer, requireDescendingSurfaceCrossing)
            }
        }

        for (i in blockStart until blocks.size) {
            val block = blocks.get(i)
            if (block.x > rangeMax) break
            if (block is Slope) continue
            if (!currentPlayer.bounds.overlaps(block.bounds)) continue

            block.tryTouch(currentPlayer)
        }

        val leftAscendingSlope =
            currentPlayer.getType() == AbstractPlayer.PlayerType.CUBE &&
                slopeBeforeUpdate > 0f &&
                currentPlayer.getCurrentSlopeRotation() == 0f
        if (leftAscendingSlope) {
            val launchVelocity =
                scrollSpeed * Slope.ASCENDING_EXIT_VELOCITY_MULTIPLIER
            currentPlayer.setVelocityY(
                if (currentPlayer.isGravityFlipped()) -launchVelocity else launchVelocity
            )
            currentPlayer.setGrounded(false)
        }

        for (i in portalStart until portals.size) {
            val portal = portals.get(i)
            if (portal.x > rangeMax) break
            if (!portal.tryTouch(currentPlayer)) continue
            handlePortalActivation(portal)
        }

        for (i in hazardStart until hazards.size) {
            val hazard = hazards.get(i)
            if (hazard.x > rangeMax) break
            hazard.tryTouch(currentPlayer)
        }

        for (i in orbStart until orbs.size) {
            val orb = orbs.get(i)
            if (orb.x > rangeMax) break
            if (orb.bounds.overlaps(currentPlayer.bounds)) {
                if (currentPlayer.isJumpHeld() && !currentPlayer.isJumpConsumed()) {
                    if (orb.tryActivate(currentPlayer)) spawnOrbGlow(orb)
                }
            } else {
                orb.resetOverlap()
            }
        }

        for (i in padStart until pads.size) {
            val pad = pads.get(i)
            if (pad.x > rangeMax) break
            if (pad.tryTouch(currentPlayer)) spawnPadGlow(pad)
        }

        currentPlayer.tryJump()

        worldScrolled += scrollAmount
        endWallScreenX = levelEndX - worldScrolled
        currentPlayer.worldX = GameConstants.Player.START_X + worldScrolled

        while (triggerIdx < triggers.size) {
            val trigger = triggers.get(triggerIdx)
            if (currentPlayer.worldX < trigger.worldX) break
            trigger.fired = true
            trigger.fire(this)
            triggerIdx++
        }

        currentPlayer.postUpdate()
    }

    private fun updateEndCapture(currentPlayer: AbstractPlayer, delta: Float) {
        endWallScreenX = endWallLockX
        endCaptureAge += delta
        currentPlayer.setVelocityY(0f)
        currentPlayer.worldX = GameConstants.Player.START_X + worldScrolled

        if (endCaptureAge <= END_CAPTURE_HOLD_DURATION) {
            endCaptureProgress = 0f
            endCaptureShakeStrength = 0.08f
            currentPlayer.postUpdate()
            return
        }

        val pullAge = endCaptureAge - END_CAPTURE_HOLD_DURATION
        val pullProgress = (pullAge / END_CAPTURE_PULL_DURATION).coerceIn(0f, 1f)
        endCaptureProgress = pullProgress

        if (pullProgress < 1f) {
            val eased = pullProgress * pullProgress * pullProgress
            val startCenterX = endCaptureStartX + currentPlayer.width / 2f
            val startCenterY = endCaptureStartY + currentPlayer.height / 2f
            val targetCenterX = endCaptureTargetX + currentPlayer.width / 2f
            val targetCenterY = endCaptureTargetY + currentPlayer.height / 2f
            val dx = targetCenterX - startCenterX
            val dy = targetCenterY - startCenterY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val controlX = (startCenterX + targetCenterX) / 2f -
                dy / distance * END_CAPTURE_ARC_HEIGHT
            val controlY = (startCenterY + targetCenterY) / 2f +
                dx / distance * END_CAPTURE_ARC_HEIGHT
            val inverse = 1f - eased
            val centerX =
                inverse * inverse * startCenterX +
                    2f * inverse * eased * controlX +
                    eased * eased * targetCenterX
            val centerY =
                inverse * inverse * startCenterY +
                    2f * inverse * eased * controlY +
                    eased * eased * targetCenterY

            currentPlayer.x = centerX - currentPlayer.width / 2f
            currentPlayer.setY(centerY - currentPlayer.height / 2f)
            currentPlayer.setRotation(
                MathUtils.lerp(currentPlayer.getRotation(), 0f, eased)
            )
            endCaptureShakeStrength = 0.08f + eased * eased * 0.92f
            currentPlayer.postUpdate()
            return
        }

        currentPlayer.x = endCaptureTargetX
        currentPlayer.setY(endCaptureTargetY)
        currentPlayer.setRotation(0f)
        playerAbsorbed = true
        endCaptureShakeStrength = 1.75f
        currentPlayer.postUpdate()

        if (pullAge >= END_CAPTURE_PULL_DURATION + END_ABSORPTION_DELAY) {
            endCaptureProgress = 1f
            isLevelComplete = true
            Gdx.app.log("GameWorld", "Level completed at end wall.")
        }
    }

    private fun handlePortalActivation(portal: AbstractPortal) {
        val currentPlayer = player ?: return
        spawnPortalGlow(portal)
        if (portal is GravityPortal) {
            currentPlayer.setGravityFlipped(!currentPlayer.isGravityFlipped())
        } else if (portal is MiniPortal) {
            currentPlayer.setMini(!currentPlayer.isMini())
        } else {
            var next: AbstractPlayer? = null
            if (portal is CubePortal) {
                next = obtainPlayer("cube").init(currentPlayer.x, currentPlayer.y)
            } else if (portal is ShipPortal) {
                next = obtainPlayer("ship").init(currentPlayer.x, currentPlayer.y)
            }
            if (next != null) {
                next.setWorld(this)
                next.copyState(currentPlayer)
                next.x = currentPlayer.x
                next.setY(currentPlayer.y)
                next.lastPortalCenterY = portal.y + portal.height / 2f
                next.lastPortalBottomY = portal.y
                freePlayer()
                player = next
            }
        }
    }

    private fun obtainPortalGlow(): PortalGlow {
        return if (portalGlowPool.size > 0) portalGlowPool.pop() else PortalGlow()
    }

    private fun spawnPortalGlow(portal: AbstractPortal) {
        val color = when (portal) {
            is CubePortal, is GravityPortal -> GREEN_PORTAL_GLOW
            is ShipPortal, is MiniPortal -> PINK_PORTAL_GLOW
            else -> return
        }
        portalGlows.add(
            obtainPortalGlow().init(
                portal.x + portal.width / 2f,
                portal.y + portal.height / 2f,
                color
            )
        )
    }

    private fun spawnOrbGlow(orb: AbstractOrb) {
        val color = when (orb.type) {
            AbstractOrb.OrbType.YELLOW -> YELLOW_ORB_GLOW
            AbstractOrb.OrbType.BLUE -> BLUE_ORB_GLOW
            AbstractOrb.OrbType.PINK -> PINK_ORB_GLOW
            AbstractOrb.OrbType.RED -> RED_ORB_GLOW
            AbstractOrb.OrbType.BLACK -> BLACK_ORB_GLOW
            AbstractOrb.OrbType.GREEN -> GREEN_ORB_GLOW
        }
        portalGlows.add(
            obtainPortalGlow().init(
                orb.x + orb.width / 2f,
                orb.y + orb.height / 2f,
                color
            )
        )
    }

    private fun spawnPadGlow(pad: AbstractPad) {
        val color = when (pad.type) {
            AbstractPad.PadType.YELLOW -> YELLOW_ORB_GLOW
            AbstractPad.PadType.BLUE -> BLUE_ORB_GLOW
            AbstractPad.PadType.PINK -> PINK_ORB_GLOW
            AbstractPad.PadType.RED -> RED_ORB_GLOW
            AbstractPad.PadType.BLACK -> BLACK_ORB_GLOW
            AbstractPad.PadType.GREEN -> GREEN_ORB_GLOW
        }
        portalGlows.add(
            obtainPortalGlow().init(
                pad.hitbox.x + pad.hitbox.width / 2f,
                pad.hitbox.y + pad.hitbox.height / 2f,
                color
            )
        )
    }

    fun cull() {
        val currentPlayer = player ?: return
        val threshold = currentPlayer.x - 1000f

        while (blockCull < blocks.size) {
            val block = blocks.get(blockCull)
            if (block.x + block.width >= threshold - 400) break
            pools.freeBlock(block)
            blockCull++
        }

        while (hazardCull < hazards.size) {
            val hazard = hazards.get(hazardCull)
            if (hazard.x + hazard.width >= threshold) break
            pools.freeHazard(hazard)
            hazardCull++
        }

        while (portalCull < portals.size) {
            val pObj = portals.get(portalCull)
            if (pObj.x + pObj.width >= threshold) break
            pools.freePortal(pObj)
            portalCull++
        }

        while (orbCull < orbs.size) {
            val orb = orbs.get(orbCull)
            if (orb.x + orb.width >= threshold) break
            pools.freeOrb(orb)
            orbCull++
        }

        while (padCull < pads.size) {
            val pad = pads.get(padCull)
            if (pad.x + pad.width >= threshold) break
            pools.freePad(pad)
            padCull++
        }
    }

    fun playerDied() {
        if (!isPlayerDead) {
            Gdx.app.log("GameWorld", "Player died.")
            spawnDeathBursts()
            isPlayerDead = true
        }
    }

    private fun obtainDeathBurst(): DeathBurst {
        return if (deathBurstPool.size > 0) deathBurstPool.pop() else DeathBurst()
    }

    private fun spawnDeathBursts() {
        // Return existing bursts to pool before clearing
        for (i in 0 until deathBursts.size) deathBurstPool.add(deathBursts[i])
        deathBursts.clear()
        val currentPlayer = player ?: return
        val centerX = currentPlayer.x + currentPlayer.width / 2f
        val centerY = currentPlayer.y + currentPlayer.height / 2f
        val burstCount = 3
        for (i in 0 until burstCount) {
            deathBursts.add(
                obtainDeathBurst().init(
                    centerX, centerY,
                    0f, GameConstants.Editor.GRID_SIZE * 1.5f,
                    i * 0.06f, 0.22f, 0.42f
                )
            )
        }
    }

    private fun updateDeathBursts(delta: Float) {
        for (i in deathBursts.size - 1 downTo 0) {
            val burst = deathBursts[i]
            burst.age += delta
            if (burst.age >= burst.startDelay + burst.duration) {
                deathBurstPool.add(burst)
                deathBursts.removeIndex(i)
            }
        }
    }

    private fun updatePortalGlows(delta: Float) {
        for (i in portalGlows.size - 1 downTo 0) {
            val glow = portalGlows[i]
            glow.age += delta
            if (glow.age >= glow.duration) {
                portalGlowPool.add(glow)
                portalGlows.removeIndex(i)
            }
        }
    }

    private fun freeAllActiveObjects() {
        pools.freeAll(
            blocks, hazards, portals, orbs, pads,
            blockCull, hazardCull, portalCull, orbCull, padCull
        )
        blockCull = 0
        blockStart = 0
        hazardCull = 0
        hazardStart = 0
        portalCull = 0
        portalStart = 0
        orbCull = 0
        orbStart = 0
        padCull = 0
        padStart = 0
    }

    val progress: Float
        get() {
            if (levelEndX <= 0) return 0f
            return min(worldScrolled / levelEndX, 1f)
        }

    var backgroundColor: Color
        get() = colors.backgroundColor
        set(value) {
            colors.setBackgroundColor(value)
        }

    var groundColor: Color
        get() = colors.groundColor
        set(value) {
            colors.setGroundColor(value)
        }

    var baseBgColor: Color
        get() = colors.baseBgColor
        set(value) {
            colors.setBaseBgColor(value)
        }

    var baseGroundColor: Color
        get() = colors.baseGroundColor
        set(value) {
            colors.setBaseGroundColor(value)
        }

    fun addPortal(p: AbstractPortal) {
        portals.add(p)
    }

    fun addHazard(h: AbstractHazard) {
        hazards.add(h)
    }

    fun addBlock(b: Block) {
        blocks.add(b)
    }
}
