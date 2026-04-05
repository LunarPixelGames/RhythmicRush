package io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "yellow_orb")
class YellowOrb : AbstractOrb {

    constructor(x: Float, y: Float) : super(x, y) {
        this.type = OrbType.YELLOW
    }

    constructor() : super() {
        this.type = OrbType.YELLOW
    }

    override fun init(x: Float, y: Float): YellowOrb {
        super.init(x, y)
        this.type = OrbType.YELLOW
        return this
    }

    override fun onClick(player: AbstractPlayer) {
        player.setVelocityY(600f)
    }
}
