package io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs

import io.github.msameer0.rhythmicrush.GameConstants
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * An orb that flips gravity and provides a significant jump boost in the new direction.
 */
@Registry(id = "green_orb")
class GreenOrb : AbstractOrb {

    constructor(x: Float, y: Float) : super(x, y) {
        this.type = OrbType.GREEN
    }

    constructor() : super() {
        this.type = OrbType.GREEN
    }

    override fun init(x: Float, y: Float): GreenOrb {
        super.init(x, y)
        this.type = OrbType.GREEN
        return this
    }

    override fun onClick(player: AbstractPlayer) {
        player.setGravityFlipped(!player.isGravityFlipped())
        val velocity = GameConstants.Interactables.Orbs.GREEN_VELOCITY
        player.setVelocityY(if (player.isGravityFlipped()) -velocity else velocity)
    }
}
