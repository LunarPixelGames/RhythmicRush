package io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "blue_orb")
class BlueOrb : AbstractOrb {

    constructor(x: Float, y: Float) : super(x, y) {
        this.type = OrbType.BLUE
    }

    constructor() : super() {
        this.type = OrbType.BLUE
    }

    override fun init(x: Float, y: Float): BlueOrb {
        super.init(x, y)
        this.type = OrbType.BLUE
        return this
    }

    override fun onClick(player: AbstractPlayer) {
        if (player.isGravityFlipped()) {
            player.setVelocityY(-300f)
        } else {
            player.setVelocityY(300f)
        }
        player.setGravityFlipped(!player.isGravityFlipped())
    }
}
