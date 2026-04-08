package io.github.msameer0.rhythmicrush.game.trigger

import io.github.msameer0.rhythmicrush.game.GameWorld

abstract class AbstractTrigger {
    var worldX: Float = 0f
    var fired: Boolean = false

    constructor() {
        this.worldX = 0f
    }

    constructor(worldX: Float) {
        this.worldX = worldX
    }

    abstract fun fire(world: GameWorld)

    fun reset() {
        fired = false
    }
}
