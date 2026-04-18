package io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * A pad that provides a small jump boost on collision.
 */
@Registry(id = "pink_pad")
class PinkPad : AbstractPad {

    constructor(x: Float, y: Float, rotation: Float = 0f) : super(x, y, rotation) {
        this.type = PadType.PINK
    }

    constructor() : super() {
        this.type = PadType.PINK
    }

    override fun init(x: Float, y: Float, rotation: Float): PinkPad {
        super.init(x, y, rotation)
        this.type = PadType.PINK
        return this
    }

    override fun onActivate(player: AbstractPlayer) {
        if (player.isGravityFlipped()) {
            player.setVelocityY(-600f)
        } else {
            player.setVelocityY(600f)
        }
    }
}
