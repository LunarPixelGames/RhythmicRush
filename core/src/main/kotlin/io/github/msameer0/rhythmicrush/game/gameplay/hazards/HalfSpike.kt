package io.github.msameer0.rhythmicrush.game.gameplay.hazards

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.engine.Rotatable
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry
import kotlin.math.roundToInt

@Registry(id = "half_spike")
class HalfSpike : AbstractHazard, Rotatable {
    override var rotation: Float = 0f
    val hitbox: Rectangle

    constructor() : super(0f, 0f, TEXTURE_SIZE, TEXTURE_SIZE) {
        this.type = HazardType.HALF_SPIKE
        this.hitbox = Rectangle()
    }

    constructor(x: Float, y: Float) : this(x, y, 0f) {
        this.type = HazardType.HALF_SPIKE
    }

    constructor(x: Float, y: Float, rotation: Float) : super(x, y, TEXTURE_SIZE, TEXTURE_SIZE) {
        this.type = HazardType.HALF_SPIKE
        this.rotation = rotation
        this.hitbox = Rectangle()
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
        when (((rotation / 90f).roundToInt() * 90 % 360 + 360) % 360) {
            90 -> hitbox.set(x + TEXTURE_SIZE - HITBOX_H, y + HITBOX_CENTER_X, HITBOX_H, HITBOX_W)
            180 -> hitbox.set(x + HITBOX_CENTER_X, y + TEXTURE_SIZE - HITBOX_H, HITBOX_W, HITBOX_H)
            270 -> hitbox.set(x, y + HITBOX_CENTER_X, HITBOX_H, HITBOX_W)
            else -> hitbox.set(x + HITBOX_CENTER_X, y, HITBOX_W, HITBOX_H)
        }
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
        if (hitbox.overlaps(player?.getBounds())) {
            player?.getWorld()!!.playerDied()
        }
    }

    companion object {
        private const val PLAYER_SIZE = 50f
        private const val TEXTURE_SIZE = 50f
        private val HITBOX_W: Float = PLAYER_SIZE * 0.25f
        private val HITBOX_H: Float = PLAYER_SIZE * 0.2f
        private val HITBOX_CENTER_X: Float = (TEXTURE_SIZE - HITBOX_W) / 2f
    }
}
