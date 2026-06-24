package io.github.msameer0.rhythmicrush.game.gameplay.hazards

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

/**
 * Base class for all deadly hazards that can kill the player on contact.
 */
abstract class AbstractHazard(
    var x: Float, var y: Float, var width: Float, var height: Float
) {
    var worldX: Float = x
    /**
     * Enumeration of supported hazard types.
     */
    enum class HazardType {
        SPIKE, HALF_SPIKE, SAW_BLADE
    }

    var type: HazardType? = null
    var bounds: Rectangle = Rectangle(x, y, width, height)
    val hazardPolygon: Polygon = Polygon(floatArrayOf(0f, 0f, width, 0f, width, height, 0f, height))

    open fun updatePosition(worldScrolled: Float) {
        x = worldX - worldScrolled
        updateBounds()
    }

    protected fun updateBounds() {
        bounds.setPosition(x, y)
        hazardPolygon.setPosition(x, y)
        hazardPolygon.setOrigin(width / 2f, height / 2f)
    }

    open fun reset() {
        this.x = 0f
        this.worldX = 0f
        this.y = 0f
        this.bounds.set(0f, 0f, 0f, 0f)
    }

    open fun tryTouch(player: AbstractPlayer) {
        if (!player.bounds.overlaps(bounds)) return
        
        if (Intersector.overlapConvexPolygons(player.getPlayerPolygon(), hazardPolygon)) {
            onTouch(player)
        }
    }

    abstract fun onTouch(player: AbstractPlayer?)
}
