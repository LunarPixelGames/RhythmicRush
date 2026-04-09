package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

abstract class AbstractPortal {
    enum class PortalType {
        CUBE,
        SHIP,
        GRAVITY,
        MINI
    }

    var type: PortalType? = null
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 50f
    var height: Float = 100f
    var bounds: Rectangle
    var isUsed: Boolean = false

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
        bounds = Rectangle(x, y, width, height)
    }

    constructor() {
        bounds = Rectangle()
    }

    open fun init(x: Float, y: Float): AbstractPortal? {
        this.x = x
        this.y = y
        this.isUsed = false
        bounds.set(x, y, width, height)
        return this
    }

    fun updatePosition(scrollSpeed: Float, delta: Float) {
        x -= scrollSpeed * delta
        updateBounds()
    }

    fun tryTouch(player: AbstractPlayer): Boolean {
        if (!this.isUsed && player.getBounds().overlaps(bounds)) {
            this.isUsed = true
            return true
        }
        return false
    }

    fun updateBounds() {
        bounds.setPosition(x, y)
    }
}
