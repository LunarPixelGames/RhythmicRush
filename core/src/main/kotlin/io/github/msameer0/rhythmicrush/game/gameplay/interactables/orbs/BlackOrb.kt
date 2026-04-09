package io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "black_orb")
class BlackOrb : AbstractOrb {

    constructor(x: Float, y: Float) : super(x, y) {
        this.type = OrbType.BLACK
    }

    constructor() : super() {
        this.type = OrbType.BLACK
    }

    override fun init(x: Float, y: Float): BlackOrb {
        super.init(x, y)
        this.type = OrbType.BLACK
        return this
    }

    override fun onClick(player: AbstractPlayer) {
        if (player.isGravityFlipped()) {
            player.setVelocityY(900f)
        } else {
            player.setVelocityY(-900f)
        }
    }
}
