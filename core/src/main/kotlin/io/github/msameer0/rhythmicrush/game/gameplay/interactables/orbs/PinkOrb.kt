package io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * An orb that provides a small jump boost on activation.
 */
@Registry(id = "pink_orb")
class PinkOrb : AbstractOrb {

    constructor(x: Float, y: Float) : super(x, y) {
        this.type = OrbType.PINK
    }

    constructor() : super() {
        this.type = OrbType.PINK
    }

    override fun init(x: Float, y: Float): PinkOrb {
        super.init(x, y)
        this.type = OrbType.PINK
        return this
    }

    override fun onClick(player: AbstractPlayer) {
        if (player.isGravityFlipped()) {
            player.setVelocityY(-425f)
        } else {
            player.setVelocityY(425f)
        }
    }
}
