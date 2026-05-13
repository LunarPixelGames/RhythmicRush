package io.github.msameer0.rhythmicrush.game.gameplay.hazards

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.engine.Rotatable
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry
import com.badlogic.gdx.math.MathUtils

/**
 * A hazard representing a smaller, half-sized spike.
 */
@Registry(id = "half_spike")
class HalfSpike : AbstractHazard, Rotatable {
    override var rotation: Float = 0f

    constructor() : super(0f, 0f, TEXTURE_SIZE, TEXTURE_SIZE) {
        this.type = HazardType.HALF_SPIKE
    }

    constructor(x: Float, y: Float) : this(x, y, 0f) {
        this.type = HazardType.HALF_SPIKE
    }

    constructor(x: Float, y: Float, rotation: Float) : super(x, y, TEXTURE_SIZE, TEXTURE_SIZE) {
        this.type = HazardType.HALF_SPIKE
        this.rotation = rotation
        updateHitbox()
    }

    fun init(x: Float, y: Float, rotation: Float): HalfSpike {
        this.x = x
        this.y = y
        this.width = TEXTURE_SIZE
        this.height = TEXTURE_SIZE
        this.rotation = rotation
        this.type = HazardType.HALF_SPIKE
        bounds.set(x, y, width, height)
        updateHitbox()
        return this
    }

    private fun updateHitbox() {
        // Revert to original forgiving rectangular hitbox (25x20 centered)
        val xMin = (TEXTURE_SIZE - HITBOX_W) / 2f
        val xMax = xMin + HITBOX_W
        val yMin = 0f
        val yMax = HITBOX_H
        
        hazardPolygon.vertices = floatArrayOf(
            xMin, yMin,
            xMax, yMin,
            xMax, yMax,
            xMin, yMax
        )
        hazardPolygon.rotation = rotation
        updateBounds()
    }

    override fun reset() {
        super.reset()
        this.rotation = 0f
    }

    override fun updatePosition(scrollSpeed: Float, delta: Float) {
        super.updatePosition(scrollSpeed, delta)
        updateHitbox()
    }

    override fun onTouch(player: AbstractPlayer?) {
        player?.getWorld()?.playerDied()
    }

    companion object {
        private const val PLAYER_SIZE = 100f
        private const val TEXTURE_SIZE = 100f
        private val HITBOX_W: Float = PLAYER_SIZE * 0.25f
        private val HITBOX_H: Float = PLAYER_SIZE * 0.2f
        private val HITBOX_CENTER_X: Float = (TEXTURE_SIZE - HITBOX_W) / 2f
    }
}
