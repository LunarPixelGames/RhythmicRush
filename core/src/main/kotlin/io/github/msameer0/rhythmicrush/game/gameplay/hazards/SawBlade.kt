package io.github.msameer0.rhythmicrush.game.gameplay.hazards

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry
import com.badlogic.gdx.math.Polygon
import kotlin.math.max
import kotlin.math.min

/**
 * A rotating circular hazard that kills the player on overlap.
 */
@Registry(id = "saw_blade")
class SawBlade : AbstractHazard {
    var degreesPerSecond: Float = 120f
    var visualRotation: Float = 0f


    constructor(x: Float, y: Float, diameter: Float, degreesPerSecond: Float) : super(
        x, y, diameter, diameter
    ) {
        this.type = HazardType.SAW_BLADE
        this.degreesPerSecond = degreesPerSecond
    }

    constructor() : super(0f, 0f, DEFAULT_SIZE, DEFAULT_SIZE) {
        this.type = HazardType.SAW_BLADE
    }

    @JvmOverloads
    fun init(
        x: Float, y: Float, diameter: Float, degreesPerSec: Float = degreesPerSecond
    ): SawBlade {
        this.x = x
        this.y = y
        this.width = diameter
        this.height = diameter
        this.degreesPerSecond = degreesPerSec
        this.visualRotation = 0f
        this.type = HazardType.SAW_BLADE
        val d = min(max(1f, diameter), 1000f)
        bounds.set(x, y, d, d)
        return this
    }

    override fun updatePosition(scrollSpeed: Float, delta: Float) {
        super.updatePosition(scrollSpeed, delta)
    }

    override fun tryTouch(player: AbstractPlayer) {
        val radius = width * 0.35f
        val cx = x + width / 2f
        val cy = y + height / 2f

        val pPoly = player.getPlayerPolygon()

        // Broad phase using bounding box
        if (!pPoly.boundingRectangle.overlaps(bounds)) return

        // Precise phase: Circle vs Rotated Polygon
        if (circleOverlapsPolygon(cx, cy, radius, pPoly)) {
            onTouch(player)
        }
    }

    private fun circleOverlapsPolygon(cx: Float, cy: Float, radius: Float, poly: Polygon): Boolean {
        if (poly.contains(cx, cy)) return true

        val vertices = poly.transformedVertices
        val squareRadius = radius * radius

        for (i in 0 until vertices.size step 2) {
            val x1 = vertices[i]
            val y1 = vertices[i + 1]
            val x2 = if (i + 2 < vertices.size) vertices[i + 2] else vertices[0]
            val y2 = if (i + 3 < vertices.size) vertices[i + 3] else vertices[1]

            val dx = x2 - x1
            val dy = y2 - y1
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared == 0f) 0f
            else (((cx - x1) * dx + (cy - y1) * dy) / lengthSquared).coerceIn(0f, 1f)
            val nearestX = x1 + t * dx
            val nearestY = y1 + t * dy
            val offsetX = cx - nearestX
            val offsetY = cy - nearestY
            if (offsetX * offsetX + offsetY * offsetY <= squareRadius) return true
        }

        return false
    }

    fun tickVisualRotation(delta: Float) {
        visualRotation += degreesPerSecond * delta
        visualRotation = ((visualRotation % 360f) + 360f) % 360f
    }

    public override fun onTouch(player: AbstractPlayer?) {
        if (player?.getWorld() != null) {
            player.getWorld()?.playerDied()
        }
    }


    val diameter: Float
        get() = width

    companion object {
        const val DEFAULT_SIZE: Float = 100f
    }
}
