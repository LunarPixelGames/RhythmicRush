package io.github.msameer0.rhythmicrush.game.registries

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
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.GravityPortal
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.MiniPortal
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship
import io.github.msameer0.rhythmicrush.game.trigger.AbstractTrigger
import io.github.msameer0.rhythmicrush.game.trigger.ColorTrigger
import io.github.msameer0.rhythmicrush.game.trigger.PulseTrigger

class Registries {
    companion object {
        @JvmField
        val BLOCKS: GameRegistry<Block?> = GameRegistry<Block?>()

        @JvmField
        val HAZARDS: GameRegistry<AbstractHazard?> = GameRegistry<AbstractHazard?>()

        @JvmField
        val PORTALS: GameRegistry<AbstractPortal?> = GameRegistry<AbstractPortal?>()

        @JvmField
        val PLAYERS: GameRegistry<AbstractPlayer?> = GameRegistry<AbstractPlayer?>()

        @JvmField
        val TRIGGERS: GameRegistry<AbstractTrigger?> = GameRegistry<AbstractTrigger?>()

        @JvmField
        val ORBS: GameRegistry<AbstractOrb?> = GameRegistry<AbstractOrb?>()

        @JvmStatic
        fun init() {
            BLOCKS.register(Block::class.java) { Block() }
            BLOCKS.register(Slope::class.java) { Slope() }

            HAZARDS.register(Spike::class.java) { Spike() }
            HAZARDS.register(HalfSpike::class.java) { HalfSpike() }
            HAZARDS.register(SawBlade::class.java) { SawBlade() }

            PORTALS.register(CubePortal::class.java) { CubePortal(0f, 0f) }
            PORTALS.register(ShipPortal::class.java) { ShipPortal(0f, 0f) }
            PORTALS.register(GravityPortal::class.java) { GravityPortal(0f, 0f) }
            PORTALS.register(MiniPortal::class.java) { MiniPortal(0f, 0f) }

            ORBS.register(YellowOrb::class.java) { YellowOrb() }
            ORBS.register(BlueOrb::class.java) { BlueOrb() }
            ORBS.register(PinkOrb::class.java) { PinkOrb() }
            ORBS.register(RedOrb::class.java) { RedOrb() }
            ORBS.register(BlackOrb::class.java) { BlackOrb() }
            ORBS.register(GreenOrb::class.java) { GreenOrb() }

            PLAYERS.register(Cube::class.java) { Cube(0f, 0f) }
            PLAYERS.register(Ship::class.java) { Ship(0f, 0f) }

            TRIGGERS.register(ColorTrigger::class.java) { ColorTrigger() }
            TRIGGERS.register(PulseTrigger::class.java) { PulseTrigger() }
        }
    }
}
