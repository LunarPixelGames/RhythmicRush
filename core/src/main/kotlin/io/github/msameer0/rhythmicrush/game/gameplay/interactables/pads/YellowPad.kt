package io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * A pad that provides a standard jump boost on collision.
 */
@Registry(id = "yellow_pad")
class YellowPad : AbstractPad {

    constructor(x: Float, y: Float, rotation: Float = 0f) : super(x, y, rotation) {
        this.type = PadType.YELLOW
    }

    constructor() : super() {
        this.type = PadType.YELLOW
    }

    override fun init(x: Float, y: Float, rotation: Float): YellowPad {
        super.init(x, y, rotation)
        this.type = PadType.YELLOW
        return this
    }

    override fun onActivate(player: AbstractPlayer) {
        if (player.isGravityFlipped()) {
            player.setVelocityY(-1700f)
        } else {
            player.setVelocityY(1700f)
        }
    }
}
