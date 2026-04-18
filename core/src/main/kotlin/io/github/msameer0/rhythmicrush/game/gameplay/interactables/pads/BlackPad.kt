package io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * A pad that forcefully pushes the player in the direction of current gravity.
 */
@Registry(id = "black_pad")
class BlackPad : AbstractPad {

    constructor(x: Float, y: Float, rotation: Float = 180f) : super(x, y, rotation) {
        this.type = PadType.BLACK
    }

    constructor() : super() {
        this.type = PadType.BLACK
    }

    override fun init(x: Float, y: Float, rotation: Float): BlackPad {
        super.init(x, y, rotation)
        this.type = PadType.BLACK
        return this
    }

    override fun onActivate(player: AbstractPlayer) {
        if (player.isGravityFlipped()) {
            player.setVelocityY(1100f)
        } else {
            player.setVelocityY(-1100f)
        }
    }
}
