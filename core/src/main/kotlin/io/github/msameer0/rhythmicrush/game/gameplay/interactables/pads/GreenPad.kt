package io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * A pad that flips player gravity and provides a boost on collision.
 */
@Registry(id = "green_pad")
class GreenPad : AbstractPad {

    constructor(x: Float, y: Float, rotation: Float = 180f) : super(x, y, rotation) {
        this.type = PadType.GREEN
    }

    constructor() : super() {
        this.type = PadType.GREEN
    }

    override fun init(x: Float, y: Float, rotation: Float): GreenPad {
        super.init(x, y, rotation)
        this.type = PadType.GREEN
        return this
    }

    override fun onActivate(player: AbstractPlayer) {
        player.setGravityFlipped(!player.isGravityFlipped())
        val v = io.github.msameer0.rhythmicrush.GameConstants.Interactables.Pads.GREEN_VELOCITY
        if (player.isGravityFlipped()) {
            player.setVelocityY(-v)
        } else {
            player.setVelocityY(v)
        }
    }
}
