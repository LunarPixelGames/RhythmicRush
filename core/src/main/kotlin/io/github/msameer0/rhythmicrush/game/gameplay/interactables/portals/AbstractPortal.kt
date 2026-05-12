package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.engine.Rotatable
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

/**
 * Base class for all portals that change the player's game mode or state on contact.
 */
abstract class AbstractPortal : Rotatable {
    override var rotation: Float = 0f

    /**
     * Enumeration of supported portal types and their effects.
     */
    enum class PortalType {
        CUBE,
        SHIP,
        GRAVITY,
        MINI
    }

    var type: PortalType? = null
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 100f
    var height: Float = 200f
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

    open fun reset() {
        this.x = 0f
        this.y = 0f
        this.rotation = 0f
        this.isUsed = false
        this.bounds.set(0f, 0f, 0f, 0f)
    }

    open fun init(x: Float, y: Float, rotation: Float): AbstractPortal? {
        this.x = x
        this.y = y
        this.rotation = rotation
        this.isUsed = false
        updateBounds()
        return this
    }

    open fun init(x: Float, y: Float): AbstractPortal? {
        return init(x, y, 0f)
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
        val rotSnapped = (Math.round(rotation / 90f) * 90 % 360 + 360) % 360
        if (rotSnapped == 90 || rotSnapped == 270) {
            val cx = x + width / 2f
            val cy = y + height / 2f
            // Swap width and height and offset by center
            bounds.set(cx - height / 2f, cy - width / 2f, height, width)
        } else {
            bounds.set(x, y, width, height)
        }
    }
}
