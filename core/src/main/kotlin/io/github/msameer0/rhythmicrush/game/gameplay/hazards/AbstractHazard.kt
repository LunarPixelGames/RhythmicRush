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
    /**
     * Enumeration of supported hazard types.
     */
    enum class HazardType {
        SPIKE, HALF_SPIKE, SAW_BLADE
    }

    var type: HazardType? = null
    var bounds: Rectangle = Rectangle(x, y, width, height)
    val hazardPolygon: Polygon = Polygon(floatArrayOf(0f, 0f, width, 0f, width, height, 0f, height))

    open fun updatePosition(scrollSpeed: Float, delta: Float) {
        x -= scrollSpeed * delta
        updateBounds()
    }

    protected fun updateBounds() {
        bounds.setPosition(x, y)
        hazardPolygon.setPosition(x, y)
        hazardPolygon.setOrigin(width / 2f, height / 2f)
    }

    open fun reset() {
        this.x = 0f
        this.y = 0f
        this.bounds.set(0f, 0f, 0f, 0f)
    }

    open fun tryTouch(player: AbstractPlayer) {
        // Broad phase check using polygons' bounding rectangles (accounts for rotation)
        val pPoly = player.getPlayerPolygon()
        if (!pPoly.boundingRectangle.overlaps(hazardPolygon.boundingRectangle)) return
        
        // Precise phase check (rotated)
        if (Intersector.overlapConvexPolygons(pPoly, hazardPolygon)) {
            onTouch(player)
        }
    }

    abstract fun onTouch(player: AbstractPlayer?)
}
