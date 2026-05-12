package io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * A pad that flips the player's gravity on collision.
 */
@Registry(id = "blue_pad")
class BluePad : AbstractPad {

    constructor(x: Float, y: Float, rotation: Float = 0f) : super(x, y, rotation) {
        this.type = PadType.BLUE
    }

    constructor() : super() {
        this.type = PadType.BLUE
    }

    override fun init(x: Float, y: Float, rotation: Float): BluePad {
        super.init(x, y, rotation)
        this.type = PadType.BLUE
        return this
    }

    override fun onActivate(player: AbstractPlayer) {
        if (player.isGravityFlipped()) {
            player.setVelocityY(-600f)
        } else {
            player.setVelocityY(600f)
        }
        player.setGravityFlipped(!player.isGravityFlipped())
    }
}
