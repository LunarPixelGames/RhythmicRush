package io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "red_orb")
class RedOrb : AbstractOrb {

    constructor(x: Float, y: Float) : super(x, y) {
        this.type = OrbType.RED
    }

    constructor() : super() {
        this.type = OrbType.RED
    }

    override fun init(x: Float, y: Float): RedOrb {
        super.init(x, y)
        this.type = OrbType.RED
        return this
    }

    override fun onClick(player: AbstractPlayer) {
        if (player.isGravityFlipped) {
            player.setVelocityY(-900f)
        } else {
            player.setVelocityY(900f)
        }
    }
}
