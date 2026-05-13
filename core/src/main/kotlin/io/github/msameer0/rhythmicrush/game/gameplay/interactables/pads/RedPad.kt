package io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * A pad that provides a massive jump boost on collision.
 */
@Registry(id = "red_pad")
class RedPad : AbstractPad {

    constructor(x: Float, y: Float, rotation: Float = 0f) : super(x, y, rotation) {
        this.type = PadType.RED
    }

    constructor() : super() {
        this.type = PadType.RED
    }

    override fun init(x: Float, y: Float, rotation: Float): RedPad {
        super.init(x, y, rotation)
        this.type = PadType.RED
        return this
    }

    override fun onActivate(player: AbstractPlayer) {
        val v = io.github.msameer0.rhythmicrush.GameConstants.Interactables.Pads.RED_VELOCITY
        if (player.isGravityFlipped()) {
            player.setVelocityY(-v)
        } else {
            player.setVelocityY(v)
        }
    }
}
