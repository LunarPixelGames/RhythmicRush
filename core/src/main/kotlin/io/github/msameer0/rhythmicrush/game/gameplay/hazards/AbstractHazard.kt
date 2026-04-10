package io.github.msameer0.rhythmicrush.game.gameplay.hazards

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

abstract class AbstractHazard(
    var x: Float, var y: Float, var width: Float, var height: Float
) {
    enum class HazardType {
        SPIKE, HALF_SPIKE, SAW_BLADE
    }

    var type: HazardType? = null
    var bounds: Rectangle = Rectangle(x, y, width, height)

    open fun updatePosition(scrollSpeed: Float, delta: Float) {
        x -= scrollSpeed * delta
        updateBounds()
    }

    protected fun updateBounds() {
        bounds.setPosition(x, y)
    }

    open fun reset() {
        this.x = 0f
        this.y = 0f
        this.bounds.set(0f, 0f, 0f, 0f)
    }

    open fun tryTouch(player: AbstractPlayer) {
        if (player.getBounds().overlaps(bounds)) {
            onTouch(player)
        }
    }

    abstract fun onTouch(player: AbstractPlayer?)
}
