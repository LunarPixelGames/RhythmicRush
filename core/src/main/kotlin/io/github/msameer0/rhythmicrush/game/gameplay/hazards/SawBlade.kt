package io.github.msameer0.rhythmicrush.game.gameplay.hazards

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry
import kotlin.math.max
import kotlin.math.min

@Registry(id = "saw_blade")
class SawBlade : AbstractHazard {
    var degreesPerSecond: Float = 120f
    var visualRotation: Float = 0f


    constructor(x: Float, y: Float, diameter: Float, degreesPerSecond: Float) : super(
        x,
        y,
        diameter,
        diameter
    ) {
        this.type = HazardType.SAW_BLADE
        this.degreesPerSecond = degreesPerSecond
    }

    constructor() : super(0f, 0f, DEFAULT_SIZE, DEFAULT_SIZE) {
        this.type = HazardType.SAW_BLADE
    }

    @JvmOverloads
    fun init(
        x: Float,
        y: Float,
        diameter: Float,
        degreesPerSec: Float = degreesPerSecond
    ): SawBlade {
        this.x = x
        this.y = y
        this.width = diameter
        this.height = diameter
        this.degreesPerSecond = degreesPerSec
        this.visualRotation = 0f
        this.type = HazardType.SAW_BLADE
        bounds.set(x, y, diameter, diameter)
        return this
    }

    override fun updatePosition(scrollSpeed: Float, delta: Float) {
        super.updatePosition(scrollSpeed, delta)
    }

    override fun tryTouch(player: AbstractPlayer) {
        val radius = width * 0.35f
        val cx = x + radius
        val cy = y + radius

        val pr = player.getBounds()

        val closestX = max(pr.x, min(cx, pr.x + pr.width))
        val closestY = max(pr.y, min(cy, pr.y + pr.height))

        val dx = cx - closestX
        val dy = cy - closestY

        if (dx * dx + dy * dy <= radius * radius) {
            onTouch(player)
        }
    }

    fun tickVisualRotation(delta: Float) {
        visualRotation += degreesPerSecond * delta
        visualRotation = ((visualRotation % 360f) + 360f) % 360f
    }

    public override fun onTouch(player: AbstractPlayer?) {
        if (player?.getWorld() != null) {
            player.getWorld()!!.playerDied()
        }
    }


    val diameter: Float
        get() = width

    companion object {
        const val DEFAULT_SIZE: Float = 100f
    }
}
