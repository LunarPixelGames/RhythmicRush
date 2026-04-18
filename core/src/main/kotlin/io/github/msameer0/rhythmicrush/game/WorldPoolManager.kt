package io.github.msameer0.rhythmicrush.game

import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.game.engine.ObjectPool
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block
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
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship

/**
 * Manages object pooling for frequently created and destroyed game objects to optimize performance.
 */
class WorldPoolManager {

    private val blockPool = object : ObjectPool<Block>() {
        override fun create() = Block()
        override fun reset(obj: Block) = obj.reset()
    }
    private val slopePool = object : ObjectPool<Slope>() {
        override fun create() = Slope()
        override fun reset(obj: Slope) = obj.reset()
    }

    private val spikePool = object : ObjectPool<Spike>() {
        override fun create() = Spike()
        override fun reset(obj: Spike) = obj.reset()
    }
    private val halfSpikePool = object : ObjectPool<HalfSpike>() {
        override fun create() = HalfSpike()
        override fun reset(obj: HalfSpike) = obj.reset()
    }
    private val sawBladePool = object : ObjectPool<SawBlade>() {
        override fun create() = SawBlade()
        override fun reset(obj: SawBlade) = obj.reset()
    }

    private val cubePortalPool = object : ObjectPool<CubePortal>() {
        override fun create() = CubePortal()
        override fun reset(obj: CubePortal) = obj.reset()
    }
    private val shipPortalPool = object : ObjectPool<ShipPortal>() {
        override fun create() = ShipPortal()
        override fun reset(obj: ShipPortal) = obj.reset()
    }
    private val gravityPortalPool = object : ObjectPool<GravityPortal>() {
        override fun create() = GravityPortal()
        override fun reset(obj: GravityPortal) = obj.reset()
    }
    private val miniPortalPool = object : ObjectPool<MiniPortal>() {
        override fun create() = MiniPortal()
        override fun reset(obj: MiniPortal) = obj.reset()
    }

    private val cubePool = object : ObjectPool<Cube>() {
        override fun create() = Cube()
        override fun reset(obj: Cube) {}
    }
    private val shipPool = object : ObjectPool<Ship>() {
        override fun create() = Ship()
        override fun reset(obj: Ship) {}
    }

    private val yellowOrbPool = object : ObjectPool<YellowOrb>() {
        override fun create() = YellowOrb()
        override fun reset(obj: YellowOrb) = obj.reset()
    }
    private val blueOrbPool = object : ObjectPool<BlueOrb>() {
        override fun create() = BlueOrb()
        override fun reset(obj: BlueOrb) = obj.reset()
    }
    private val pinkOrbPool = object : ObjectPool<PinkOrb>() {
        override fun create() = PinkOrb()
        override fun reset(obj: PinkOrb) = obj.reset()
    }
    private val redOrbPool = object : ObjectPool<RedOrb>() {
        override fun create() = RedOrb()
        override fun reset(obj: RedOrb) = obj.reset()
    }
    private val blackOrbPool = object : ObjectPool<BlackOrb>() {
        override fun create() = BlackOrb()
        override fun reset(obj: BlackOrb) = obj.reset()
    }
    private val greenOrbPool = object : ObjectPool<GreenOrb>() {
        override fun create() = GreenOrb()
        override fun reset(obj: GreenOrb) = obj.reset()
    }

    private val yellowPadPool = object : ObjectPool<YellowPad>() {
        override fun create() = YellowPad()
        override fun reset(obj: YellowPad) = obj.reset()
    }
    private val bluePadPool = object : ObjectPool<BluePad>() {
        override fun create() = BluePad()
        override fun reset(obj: BluePad) = obj.reset()
    }
    private val pinkPadPool = object : ObjectPool<PinkPad>() {
        override fun create() = PinkPad()
        override fun reset(obj: PinkPad) = obj.reset()
    }
    private val redPadPool = object : ObjectPool<RedPad>() {
        override fun create() = RedPad()
        override fun reset(obj: RedPad) = obj.reset()
    }
    private val blackPadPool = object : ObjectPool<BlackPad>() {
        override fun create() = BlackPad()
        override fun reset(obj: BlackPad) = obj.reset()
    }
    private val greenPadPool = object : ObjectPool<GreenPad>() {
        override fun create() = GreenPad()
        override fun reset(obj: GreenPad) = obj.reset()
    }

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

    fun obtainYellowPad(): YellowPad = yellowPadPool.obtain()
    fun obtainBluePad(): BluePad = bluePadPool.obtain()
    fun obtainPinkPad(): PinkPad = pinkPadPool.obtain()
    fun obtainRedPad(): RedPad = redPadPool.obtain()
    fun obtainBlackPad(): BlackPad = blackPadPool.obtain()
    fun obtainGreenPad(): GreenPad = greenPadPool.obtain()

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

    fun freePad(p: AbstractPad) {
        when (p) {
            is YellowPad -> yellowPadPool.free(p)
            is BluePad -> bluePadPool.free(p)
            is PinkPad -> pinkPadPool.free(p)
            is RedPad -> redPadPool.free(p)
            is BlackPad -> blackPadPool.free(p)
            is GreenPad -> greenPadPool.free(p)
        }
    }

    fun freeAll(
        blocks: Array<Block>,
        hazards: Array<AbstractHazard>,
        portals: Array<AbstractPortal>,
        orbs: Array<AbstractOrb>,
        pads: Array<AbstractPad>,
        blockCull: Int,
        hazardCull: Int,
        portalCull: Int,
        orbCull: Int,
        padCull: Int
    ) {
        for (i in blockCull until blocks.size) freeBlock(blocks.get(i))
        blocks.clear()

        for (i in hazardCull until hazards.size) freeHazard(hazards.get(i))
        hazards.clear()

        for (i in portalCull until portals.size) freePortal(portals.get(i))
        portals.clear()

        for (i in orbCull until orbs.size) freeOrb(orbs.get(i))
        orbs.clear()

        for (i in padCull until pads.size) freePad(pads.get(i))
        pads.clear()
    }
}
