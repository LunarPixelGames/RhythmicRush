package io.github.msameer0.rhythmicrush.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.game.engine.Tickable
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType
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
import io.github.msameer0.rhythmicrush.game.registries.Registries
import io.github.msameer0.rhythmicrush.game.trigger.AbstractTrigger
import io.github.msameer0.rhythmicrush.game.trigger.ColorTrigger
import io.github.msameer0.rhythmicrush.game.trigger.PulseTrigger
import kotlin.math.min

/**
 * Represents the core game state, including the player, level objects, and world simulation logic.
 */
class GameWorld : Tickable {

    companion object {
        private const val COLLISION_LOOKAHEAD = 2800f
        private const val POST_END_DELAY = 2f

        private fun parseOptionalHex(hex: String?): Color? {
            return if (!hex.isNullOrEmpty()) hexToColor(hex) else null
        }

        @JvmStatic
        fun hexToColor(hexStr: String?): Color {
            var hex = hexStr
            if (hex.isNullOrEmpty()) return Color(0f, 0f, 0f, 1f)
            if (hex.startsWith("#")) hex = hex.substring(1)
            val `val` = hex.toLong(16)
            val r = (`val` shr 16 and 0xFF).toFloat() / 255f
            val g = (`val` shr 8 and 0xFF).toFloat() / 255f
            val b = (`val` and 0xFF).toFloat() / 255f
            return Color(r, g, b, 1f)
        }

        private fun resolveBlockType(textureName: String?): BlockType {
            if (textureName != null) {
                for (t in BlockType.entries) {
                    if (t.textureName == textureName) return t
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
    var player: AbstractPlayer? = null
        private set
    val groundY = 304f
    val scrollSpeed = 640f

    var isPlayerDead = false
    var isLevelComplete = false
    var worldScrolled = 0f
    private var levelEndX = 0f
    private var postEndTimer = -1f
    var cullX = 0f

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
        worldScrolled = startScrolled
        postEndTimer = -1f
        bgImage = data.bgImage ?: ""
        currentLoudness = 0f
        targetLoudness = 0f

        val bg = if (!data.bgColor.isNullOrEmpty()) data.bgColor else "1a1a2e"
        val gnd = if (!data.groundColor.isNullOrEmpty()) data.groundColor else "16213e"
        colors.baseBgColor.set(hexToColor(bg))
        colors.baseGroundColor.set(hexToColor(gnd))
        colors.backgroundColor.set(hexToColor(bg))
        colors.groundColor.set(hexToColor(gnd))

        for (e in data.objects) {
            val rx = e.x - startScrolled
            spawnObject(e, rx, startScrolled)
        }

        levelEndX = data.getLevelEndX()

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
            player = obtainPlayer("cube").init(100f, groundY)
            player?.worldX = 100f + worldScrolled
            player?.setWorld(this)
        }
    }

    private fun spawnObject(e: LevelData.ObjectEntry, rx: Float, startScrolled: Float) {
        if (Registries.BLOCKS.has(e.type)) {
            if (e.x + e.size < startScrolled - 400) return
            val bt = resolveBlockType(e.blockType)
            if ("slope" == e.type) {
                blocks.add(pools.obtainSlope().init(rx, e.y, e.size, bt, e.rotation))
            } else {
                blocks.add(pools.obtainBlock().init(rx, e.y, e.size, bt, e.rotation))
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
                val s = pools.obtainSpike().init(rx, e.y, e.rotation)
                hazards.add(s)
            }

            "half_spike" -> {
                val hs = pools.obtainHalfSpike().init(rx, e.y, e.rotation)
                hazards.add(hs)
            }

            "saw_blade" -> {
                val sb = pools.obtainSawBlade().init(rx, e.y, e.size, e.rotation)
                hazards.add(sb)
            }
        }
    }

    private fun spawnPortal(e: LevelData.ObjectEntry, rx: Float) {
        var p: AbstractPortal? = null
        when (e.type) {
            "cube_portal" -> {
                p = pools.obtainCubePortal()
            }

            "ship_portal" -> {
                p = pools.obtainShipPortal()
            }

            "gravity_portal" -> {
                p = pools.obtainGravityPortal()
            }

            "mini_portal" -> {
                p = pools.obtainMiniPortal()
            }
        }
        if (p != null) {
            p.init(rx, e.y, e.rotation)
            portals.add(p)
        }
    }

    private fun spawnOrb(e: LevelData.ObjectEntry, rx: Float) {
        var orb: AbstractOrb? = null
        when (e.type) {
            "yellow_orb" -> {
                val o = pools.obtainYellowOrb(); o.init(rx, e.y); orb = o
            }

            "blue_orb" -> {
                val o = pools.obtainBlueOrb(); o.init(rx, e.y); orb = o
            }

            "pink_orb" -> {
                val o = pools.obtainPinkOrb(); o.init(rx, e.y); orb = o
            }

            "red_orb" -> {
                val o = pools.obtainRedOrb(); o.init(rx, e.y); orb = o
            }

            "black_orb" -> {
                val o = pools.obtainBlackOrb(); o.init(rx, e.y); orb = o
            }

            "green_orb" -> {
                val o = pools.obtainGreenOrb(); o.init(rx, e.y); orb = o
            }
        }
        if (orb != null) orbs.add(orb)
    }

    private fun spawnPad(e: LevelData.ObjectEntry, rx: Float) {
        var pad: AbstractPad? = null
        when (e.type) {
            "yellow_pad" -> {
                val p = pools.obtainYellowPad(); p.init(rx, e.y, e.rotation); pad = p
            }

            "blue_pad" -> {
                val p = pools.obtainBluePad(); p.init(rx, e.y, e.rotation); pad = p
            }

            "pink_pad" -> {
                val p = pools.obtainPinkPad(); p.init(rx, e.y, e.rotation); pad = p
            }

            "red_pad" -> {
                val p = pools.obtainRedPad(); p.init(rx, e.y, e.rotation); pad = p
            }

            "black_pad" -> {
                val p = pools.obtainBlackPad(); p.init(rx, e.y, e.rotation); pad = p
            }

            "green_pad" -> {
                val p = pools.obtainGreenPad(); p.init(rx, e.y, e.rotation); pad = p
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
            worldScrolled = 0f
            postEndTimer = -1f
            levelEndX = 0f
            freePlayer()
            player = obtainPlayer("cube").init(100f, groundY)
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

        val p = player ?: return
        p.update(delta, groundY)

        // Check if player went offscreen using the viewport height
        // The ExtendViewport uses 1080f as its virtual height
        val viewportHeight = 1080f  // This matches your ExtendViewport(1920f, 1080f, camera)
        val killZoneMargin = p.height + 700f  // Extra buffer to ensure player is fully offscreen

        if (p.y < -killZoneMargin || p.y > viewportHeight + killZoneMargin) {
            playerDied()
            return
        }

        for (i in portalCull until portals.size) portals.get(i).updatePosition(scrollSpeed, delta)
        for (i in hazardCull until hazards.size) hazards.get(i).updatePosition(scrollSpeed, delta)
        for (i in blockCull until blocks.size) blocks.get(i).updatePosition(scrollSpeed, delta)
        for (i in orbCull until orbs.size) orbs.get(i).updatePosition(scrollSpeed, delta)
        for (i in padCull until pads.size) pads.get(i).updatePosition(scrollSpeed, delta)

        val px = p.x
        val rangeMin = px - 600f
        val rangeMax = px + COLLISION_LOOKAHEAD

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
            val b = blocks.get(i)
            if (b.x > rangeMax) break
            b.tryTouch(p)
        }

        for (i in portalStart until portals.size) {
            val portal = portals.get(i)
            if (portal.x > rangeMax) break
            if (!portal.tryTouch(p)) continue
            handlePortalActivation(portal)
        }

        for (i in hazardStart until hazards.size) {
            val h = hazards.get(i)
            if (h.x > rangeMax) break
            h.tryTouch(p)
        }

        for (i in orbStart until orbs.size) {
            val orb = orbs.get(i)
            if (orb.x > rangeMax) break
            if (orb.bounds.overlaps(p.bounds)) {
                if (p.isJumpHeld() && !p.isJumpConsumed()) orb.tryActivate(p)
            } else {
                orb.resetOverlap()
            }
        }

        for (i in padStart until pads.size) {
            val pad = pads.get(i)
            if (pad.x > rangeMax) break
            pad.tryTouch(p)
        }

        p.tryJump()

        worldScrolled += scrollSpeed * delta
        p.worldX = 100f + worldScrolled

        while (triggerIdx < triggers.size) {
            val t = triggers.get(triggerIdx)
            if (p.worldX < t.worldX) break
            t.fired = true
            t.fire(this)
            triggerIdx++
        }

        p.postUpdate()

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
        val p = player ?: return
        if (portal is GravityPortal) {
            p.setGravityFlipped(!p.isGravityFlipped())
        } else if (portal is MiniPortal) {
            p.setMini(!p.isMini())
        } else {
            var next: AbstractPlayer? = null
            if (portal is CubePortal) {
                next = obtainPlayer("cube").init(p.x, p.y)
            } else if (portal is ShipPortal) {
                next = obtainPlayer("ship").init(p.x, p.y)
            }
            if (next != null) {
                next.setWorld(this)
                next.copyState(p)
                next.x = p.x
                next.setY(p.y)
                freePlayer()
                player = next
            }
        }
    }

    fun cull() {
        val p = player ?: return
        val threshold = p.x - 1000f

        while (blockCull < blocks.size) {
            val b = blocks.get(blockCull)
            if (b.x + b.width >= threshold - 400) break
            pools.freeBlock(b)
            blockCull++
        }

        while (hazardCull < hazards.size) {
            val h = hazards.get(hazardCull)
            if (h.x + h.width >= threshold) break
            pools.freeHazard(h)
            hazardCull++
        }

        while (portalCull < portals.size) {
            val pObj = portals.get(portalCull)
            if (pObj.x + pObj.width >= threshold) break
            pools.freePortal(pObj)
            portalCull++
        }

        while (orbCull < orbs.size) {
            val o = orbs.get(orbCull)
            if (o.x + o.width >= threshold) break
            pools.freeOrb(o)
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
            isPlayerDead = true
        }
    }

    private fun freeAllActiveObjects() {
        pools.freeAll(
            blocks, hazards, portals, orbs, pads,
            blockCull, hazardCull, portalCull, orbCull, padCull
        )
        blockCull = 0; blockStart = 0
        hazardCull = 0; hazardStart = 0
        portalCull = 0; portalStart = 0
        orbCull = 0; orbStart = 0
        padCull = 0; padStart = 0
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
