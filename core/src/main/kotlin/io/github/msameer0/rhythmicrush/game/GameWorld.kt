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

/**
 * Represents the core game state, including the player, level objects, and world simulation logic.
 */
class GameWorld : Tickable {
    /** Represents one expanding ring in the player death effect. */
    data class DeathBurst(
        val x: Float,
        val y: Float,
        val startRadius: Float,
        val endRadius: Float,
        val startDelay: Float,
        val duration: Float,
        val maxAlpha: Float,
        var age: Float = 0f
    ) {
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

    data class PortalGlow(
        var x: Float,
        val y: Float,
        val color: Color,
        val startRadius: Float = 24f,
        val endRadius: Float = GameConstants.Editor.GRID_SIZE * 1.5f,
        val duration: Float = 0.48f,
        val maxAlpha: Float = 0.42f,
        var age: Float = 0f
    ) {
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
        private const val POST_END_DELAY = 2f
        private const val UPPER_DEATH_MARGIN_BLOCKS = 25f
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
            var hex = hexStr
            if (hex.isNullOrEmpty()) return Color(0f, 0f, 0f, 1f)
            if (hex.startsWith("#")) hex = hex.substring(1)
            val colorValue = hex.toLong(16)
            val red = (colorValue shr 16 and 0xFF).toFloat() / 255f
            val green = (colorValue shr 8 and 0xFF).toFloat() / 255f
            val blue = (colorValue and 0xFF).toFloat() / 255f
            return Color(red, green, blue, 1f)
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
    private var postEndTimer = -1f
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
        postEndTimer = -1f
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

        levelEndX = data.getLevelEndX()
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

    private fun spawnObject(e: LevelData.ObjectEntry, rx: Float, startScrolled: Float) {
        if (Registries.BLOCKS.has(e.type)) {
            if (e.x + e.size < startScrolled - 400) return
            val bt = resolveBlockType(e.blockType)
            if ("slope" == e.type) {
                blocks.add(pools.obtainSlope().init(rx, e.y, e.size, bt, e.rotation))
            } else {
                val block = pools.obtainBlock().init(rx, e.y, e.size, bt, e.rotation)
                block.untouchable = e.untouchable
                blocks.add(block)
            }
        } else if (Registries.HAZARDS.has(e.type)) {
            val hW = if ("saw_blade" == e.type) e.size else 100f
            if (e.x + hW < startScrolled - 200) return
            spawnHazard(e, rx)
        } else if (Registries.PORTALS.has(e.type)) {
            if (e.x + 100f < startScrolled - 200) return
            spawnPortal(e, rx)
        } else if (Registries.ORBS.has(e.type)) {
            if (e.x + e.size < startScrolled - 200) return
            spawnOrb(e, rx)
        } else if (Registries.PADS.has(e.type)) {
            if (e.x + e.size < startScrolled - 100) return
            spawnPad(e, rx)
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
            postEndTimer = -1f
            levelEndX = 0f
            freePlayer()
            player = obtainPlayer("cube").init(GameConstants.Player.START_X, groundY)
            player?.setWorld(this)
        }
    }

    override fun onInput(held: Boolean): Boolean {
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
        val slopeBeforeUpdate = currentPlayer.getCurrentSlopeRotation()
        currentPlayer.update(delta, maxOf(groundY, boundaryBottom), boundaryTop)

        val lowerKillZoneMargin = currentPlayer.height + 700f

        if (currentPlayer.y < -lowerKillZoneMargin || currentPlayer.y > upperDeathY) {
            playerDied()
            return
        }

        for (i in portalCull until portals.size) portals.get(i).updatePosition(scrollSpeed, delta)
        for (i in hazardCull until hazards.size) hazards.get(i).updatePosition(scrollSpeed, delta)
        for (i in blockCull until blocks.size) blocks.get(i).updatePosition(scrollSpeed, delta)
        for (i in orbCull until orbs.size) orbs.get(i).updatePosition(scrollSpeed, delta)
        for (i in padCull until pads.size) pads.get(i).updatePosition(scrollSpeed, delta)
        for (i in 0 until portalGlows.size) portalGlows[i].x -= scrollSpeed * delta

        val playerX = currentPlayer.x
        val rangeMin = playerX - 600f
        val rangeMax = playerX + COLLISION_LOOKAHEAD

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

            var coveredBySlope = false
            for (slopeIndex in blockStart until blocks.size) {
                val possibleSlope = blocks.get(slopeIndex)
                if (possibleSlope.x > rangeMax) break
                if (
                    possibleSlope is Slope &&
                    possibleSlope.coversSupportBlock(block, currentPlayer)
                ) {
                    coveredBySlope = true
                    break
                }
            }

            if (!coveredBySlope) block.tryTouch(currentPlayer)
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
            pad.tryTouch(currentPlayer)
        }

        currentPlayer.tryJump()

        worldScrolled += scrollSpeed * delta
        currentPlayer.worldX = GameConstants.Player.START_X + worldScrolled

        while (triggerIdx < triggers.size) {
            val trigger = triggers.get(triggerIdx)
            if (currentPlayer.worldX < trigger.worldX) break
            trigger.fired = true
            trigger.fire(this)
            triggerIdx++
        }

        currentPlayer.postUpdate()

        if (levelEndX > 0 && worldScrolled >= levelEndX && postEndTimer < 0) postEndTimer = 0f
        if (postEndTimer >= 0) {
            postEndTimer += delta
            if (postEndTimer >= POST_END_DELAY && !isLevelComplete) {
                Gdx.app.log("GameWorld", "Level completed!")
                isLevelComplete = true
            }
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

    private fun spawnPortalGlow(portal: AbstractPortal) {
        val color = when (portal) {
            is CubePortal, is GravityPortal -> GREEN_PORTAL_GLOW
            is ShipPortal, is MiniPortal -> PINK_PORTAL_GLOW
            else -> return
        }
        portalGlows.add(
            PortalGlow(
                x = portal.x + portal.width / 2f,
                y = portal.y + portal.height / 2f,
                color = Color(color)
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
            PortalGlow(
                x = orb.x + orb.width / 2f,
                y = orb.y + orb.height / 2f,
                color = Color(color)
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

    private fun spawnDeathBursts() {
        deathBursts.clear()
        val currentPlayer = player ?: return
        val centerX = currentPlayer.x + currentPlayer.width / 2f
        val centerY = currentPlayer.y + currentPlayer.height / 2f
        val burstCount = 3
        for (i in 0 until burstCount) {
            deathBursts.add(
                DeathBurst(
                    x = centerX,
                    y = centerY,
                    startRadius = 0f,
                    endRadius = GameConstants.Editor.GRID_SIZE * 1.5f,
                    startDelay = i * 0.06f,
                    duration = 0.22f,
                    maxAlpha = 0.42f
                )
            )
        }
    }

    private fun updateDeathBursts(delta: Float) {
        for (i in deathBursts.size - 1 downTo 0) {
            val burst = deathBursts[i]
            burst.age += delta
            if (burst.age >= burst.startDelay + burst.duration) deathBursts.removeIndex(i)
        }
    }

    private fun updatePortalGlows(delta: Float) {
        for (i in portalGlows.size - 1 downTo 0) {
            val glow = portalGlows[i]
            glow.age += delta
            if (glow.age >= glow.duration) portalGlows.removeIndex(i)
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
