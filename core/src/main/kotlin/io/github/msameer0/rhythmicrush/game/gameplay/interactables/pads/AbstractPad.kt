package io.github.msameer0.rhythmicrush.game.gameplay.interactables.pads

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

/**
 * Base class for all interactable pads that provide movement boosts or gravity changes when touched.
 */
abstract class AbstractPad {

    /**
     * Enumeration of supported pad types.
     */
    enum class PadType {
        YELLOW,
        BLUE,
        PINK,
        RED,
        BLACK,
        GREEN
    }

    var type: PadType = PadType.YELLOW
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 100f
    var height: Float = 100f
    var rotation: Float = 0f

    var hitbox: Rectangle = Rectangle()
    private var used: Boolean = false

    constructor() {
        updateHitbox()
    }

    constructor(x: Float, y: Float, rotation: Float = 0f) {
        this.x = x
        this.y = y
        this.rotation = rotation
        updateHitbox()
    }

    fun updatePosition(scrollSpeed: Float, delta: Float) {
        x -= scrollSpeed * delta
        updateHitbox()
    }

    open fun init(x: Float, y: Float, rotation: Float): AbstractPad {
        this.x = x
        this.y = y
        this.rotation = rotation
        updateHitbox()
        reset()
        return this
    }

    /**
     * Updates the thin hitbox based on the current rotation.
     * Hitbox is 1/10th of the height of a block.
     */
    fun updateHitbox() {
        val hHeight = height / 10f
        val rot = ((rotation / 90f).toInt() % 4 + 4) % 4 * 90

        when (rot) {
            0 -> hitbox.set(x, y, width, hHeight)
            90 -> hitbox.set(x + width - hHeight, y, hHeight, height)
            180 -> hitbox.set(x, y + height - hHeight, width, hHeight)
            270 -> hitbox.set(x, y, hHeight, height)
            else -> hitbox.set(x, y, width, hHeight)
        }
    }

    fun isUsed(): Boolean = used

    /**
     * Checks if the pad can be activated by the player based on gravity and rotation.
     */
    fun canActivate(player: AbstractPlayer): Boolean {
        val flipped = player.isGravityFlipped()
        val rot = ((rotation / 90f).toInt() % 4 + 4) % 4 * 90

        val isRegularPad = type == PadType.YELLOW || type == PadType.BLUE ||
                          type == PadType.PINK || type == PadType.RED

        if (rot == 90 || rot == 270) return true

        return if (isRegularPad) {
            if (!flipped) rot == 0 else rot == 180
        } else {
            if (!flipped) rot == 180 else rot == 0
        }
    }

    fun tryTouch(player: AbstractPlayer) {
        if (hitbox.overlaps(player.bounds)) {
            if (!used && canActivate(player)) {
                used = true
                onActivate(player)
            }
        } else {
            used = false
        }
    }

    fun reset() {
        used = false
    }

    abstract fun onActivate(player: AbstractPlayer)
}
