package io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

abstract class AbstractOrb {

    enum class OrbType {
        YELLOW,
        BLUE,
        PINK,
        RED,
        BLACK,
        GREEN
    }

    var type: OrbType = OrbType.YELLOW
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 55f
    var height: Float = 55f
    protected var multiActivate: Boolean = false
    var bounds: Rectangle
    private var used: Boolean = false

    constructor() {
        this.bounds = Rectangle(x, y, width, height)
    }

    constructor(x: Float, y: Float, multiActivate: Boolean = false) {
        this.x = x
        this.y = y
        this.multiActivate = multiActivate
        this.bounds = Rectangle(x, y, width, height)
    }

    fun updatePosition(scrollSpeed: Float, delta: Float) {
        x -= scrollSpeed * delta
        bounds.setPosition(x, y)
    }

    open fun init(x: Float, y: Float): AbstractOrb {
        this.x = x
        this.y = y
        this.bounds.set(x, y, width, height)
        reset()
        return this
    }

    fun isUsed(): Boolean = used

    fun tryActivate(player: AbstractPlayer) {
        if (multiActivate || !used) {
            used = true
            player.setJumpConsumed(true)
            onClick(player)
        }
    }

    fun resetOverlap() {
        if (multiActivate) used = false
    }

    fun reset() {
        used = false
    }

    abstract fun onClick(player: AbstractPlayer)
}
