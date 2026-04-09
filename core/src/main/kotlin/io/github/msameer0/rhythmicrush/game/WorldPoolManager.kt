package io.github.msameer0.rhythmicrush.game

import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.game.engine.ObjectPool
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.*
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.*
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.*
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship

/**
 * Owns and manages all [ObjectPool] instances used by [GameWorld].
 *
 * Centralises pool declarations, obtain helpers, and free-all cleanup so
 * that `GameWorld` itself can focus on gameplay logic rather than memory
 * management boilerplate.
 */
class WorldPoolManager {

    // ── Pools ─────────────────────────────────────────────────────────────────

    private val blockPool = object : ObjectPool<Block>() {
        override fun create() = Block()
        override fun reset(obj: Block) = obj.reset()
    }
    private val slopePool = object : ObjectPool<Slope>() {
        override fun create() = Slope()
        override fun reset(obj: Slope) = obj.reset()
    }

    private val spikePool = object : ObjectPool<Spike>() { override fun create() = Spike(); override fun reset(obj: Spike) = obj.reset() }
    private val halfSpikePool = object : ObjectPool<HalfSpike>() { override fun create() = HalfSpike(); override fun reset(obj: HalfSpike) = obj.reset() }
    private val sawBladePool = object : ObjectPool<SawBlade>() { override fun create() = SawBlade(); override fun reset(obj: SawBlade) = obj.reset() }

    private val cubePortalPool = object : ObjectPool<CubePortal>() { override fun create() = CubePortal(); override fun reset(obj: CubePortal) = obj.reset() }
    private val shipPortalPool = object : ObjectPool<ShipPortal>() { override fun create() = ShipPortal(); override fun reset(obj: ShipPortal) = obj.reset() }
    private val gravityPortalPool = object : ObjectPool<GravityPortal>() { override fun create() = GravityPortal(); override fun reset(obj: GravityPortal) = obj.reset() }
    private val miniPortalPool = object : ObjectPool<MiniPortal>() { override fun create() = MiniPortal(); override fun reset(obj: MiniPortal) = obj.reset() }

    private val cubePool = object : ObjectPool<Cube>() { override fun create() = Cube(); override fun reset(obj: Cube) {} }
    private val shipPool = object : ObjectPool<Ship>() { override fun create() = Ship(); override fun reset(obj: Ship) {} }

    private val yellowOrbPool = object : ObjectPool<YellowOrb>() { override fun create() = YellowOrb(); override fun reset(obj: YellowOrb) = obj.reset() }
    private val blueOrbPool = object : ObjectPool<BlueOrb>() { override fun create() = BlueOrb(); override fun reset(obj: BlueOrb) = obj.reset() }
    private val pinkOrbPool = object : ObjectPool<PinkOrb>() { override fun create() = PinkOrb(); override fun reset(obj: PinkOrb) = obj.reset() }
    private val redOrbPool = object : ObjectPool<RedOrb>() { override fun create() = RedOrb(); override fun reset(obj: RedOrb) = obj.reset() }
    private val blackOrbPool = object : ObjectPool<BlackOrb>() { override fun create() = BlackOrb(); override fun reset(obj: BlackOrb) = obj.reset() }
    private val greenOrbPool = object : ObjectPool<GreenOrb>() { override fun create() = GreenOrb(); override fun reset(obj: GreenOrb) = obj.reset() }

    // ── Obtain helpers ────────────────────────────────────────────────────────

    fun obtainBlock(): Block = blockPool.obtain()
    fun obtainSlope(): Slope = slopePool.obtain()
    fun obtainSpike(): Spike = spikePool.obtain()
    fun obtainHalfSpike(): HalfSpike = halfSpikePool.obtain()
    fun obtainSawBlade(): SawBlade = sawBladePool.obtain()

    fun obtainCubePortal(): CubePortal = cubePortalPool.obtain()
    fun obtainShipPortal(): ShipPortal = shipPortalPool.obtain()
    fun obtainGravityPortal(): GravityPortal = gravityPortalPool.obtain()
    fun obtainMiniPortal(): MiniPortal = miniPortalPool.obtain()

    fun obtainCube(): Cube = cubePool.obtain()
    fun obtainShip(): Ship = shipPool.obtain()

    fun obtainYellowOrb(): YellowOrb = yellowOrbPool.obtain()
    fun obtainBlueOrb(): BlueOrb = blueOrbPool.obtain()
    fun obtainPinkOrb(): PinkOrb = pinkOrbPool.obtain()
    fun obtainRedOrb(): RedOrb = redOrbPool.obtain()
    fun obtainBlackOrb(): BlackOrb = blackOrbPool.obtain()
    fun obtainGreenOrb(): GreenOrb = greenOrbPool.obtain()

    // ── Free helpers ──────────────────────────────────────────────────────────

    fun freePlayer(player: AbstractPlayer) {
        when (player) {
            is Cube -> cubePool.free(player)
            is Ship -> shipPool.free(player)
        }
    }

    fun freeBlock(b: Block) {
        when (b) {
            is Slope -> slopePool.free(b)
            else -> blockPool.free(b)
        }
    }

    fun freeHazard(h: AbstractHazard) {
        when (h) {
            is Spike -> spikePool.free(h)
            is HalfSpike -> halfSpikePool.free(h)
            is SawBlade -> sawBladePool.free(h)
        }
    }

    fun freePortal(p: AbstractPortal) {
        when (p) {
            is CubePortal -> cubePortalPool.free(p)
            is ShipPortal -> shipPortalPool.free(p)
            is GravityPortal -> gravityPortalPool.free(p)
            is MiniPortal -> miniPortalPool.free(p)
        }
    }

    fun freeOrb(o: AbstractOrb) {
        when (o) {
            is YellowOrb -> yellowOrbPool.free(o)
            is BlueOrb -> blueOrbPool.free(o)
            is PinkOrb -> pinkOrbPool.free(o)
            is RedOrb -> redOrbPool.free(o)
            is BlackOrb -> blackOrbPool.free(o)
            is GreenOrb -> greenOrbPool.free(o)
        }
    }

    // ── Bulk free ─────────────────────────────────────────────────────────────

    /**
     * Frees all active entities back to their pools and clears the provided arrays.
     * Also resets the typed subset arrays for hazards and portals.
     *
     * @param blocks          Active block array (will be cleared).
     * @param hazards         Active hazard array (will be cleared).
     * @param portals         Active portal array (will be cleared).
     * @param orbs            Active orb array (will be cleared).
     * @param activeSpikes    Typed hazard subset (will be cleared).
     * @param activeHalfSpikes Typed hazard subset (will be cleared).
     * @param activeSawBlades Typed hazard subset (will be cleared).
     * @param activeCubePortals    Typed portal subset (will be cleared).
     * @param activeShipPortals    Typed portal subset (will be cleared).
     * @param activeGravityPortals Typed portal subset (will be cleared).
     * @param activeMiniPortals    Typed portal subset (will be cleared).
     * @param blockCull  Index from which active (non-culled) blocks start.
     * @param hazardCull Index from which active hazards start.
     * @param portalCull Index from which active portals start.
     * @param orbCull    Index from which active orbs start.
     */
    fun freeAll(
        blocks: Array<Block>, hazards: Array<AbstractHazard>,
        portals: Array<AbstractPortal>, orbs: Array<AbstractOrb>,
        activeSpikes: Array<Spike>, activeHalfSpikes: Array<HalfSpike>, activeSawBlades: Array<SawBlade>,
        activeCubePortals: Array<CubePortal>, activeShipPortals: Array<ShipPortal>,
        activeGravityPortals: Array<GravityPortal>, activeMiniPortals: Array<MiniPortal>,
        blockCull: Int, hazardCull: Int, portalCull: Int, orbCull: Int
    ) {
        for (i in blockCull until blocks.size) freeBlock(blocks.get(i))
        blocks.clear()

        for (i in hazardCull until hazards.size) freeHazard(hazards.get(i))
        hazards.clear()
        activeSpikes.clear()
        activeHalfSpikes.clear()
        activeSawBlades.clear()

        for (i in portalCull until portals.size) freePortal(portals.get(i))
        portals.clear()
        activeCubePortals.clear()
        activeShipPortals.clear()
        activeGravityPortals.clear()
        activeMiniPortals.clear()

        for (i in orbCull until orbs.size) freeOrb(orbs.get(i))
        orbs.clear()
    }
}
