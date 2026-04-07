package io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

abstract class AbstractOrb {

    enum class OrbType {
        YELLOW
    }

    var type: OrbType = OrbType.YELLOW
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 50f
    var height: Float = 50f
    protected var multiActivate: Boolean = false
    var bounds: Rectangle
    private var used: Boolean = false

    constructor() {
        this.bounds = Rectangle(x, y, width, height)
    }

    constructor(x: Float, y: Float, multiActivate: Boolean = false) {
        this.x = x
        this.y = y
        this.multiActivate = multiActivate
        this.bounds = Rectangle(x, y, width, height)
    }

    /** Called by GameWorld each tick to scroll the orb left with the world. */
    fun updatePosition(scrollSpeed: Float, delta: Float) {
        x -= scrollSpeed * delta
        bounds.setPosition(x, y)
    }

    /** Initialises (or re-initialises) this orb at the given position. Used by object pools. */
    open fun init(x: Float, y: Float): AbstractOrb {
        this.x = x
        this.y = y
        this.bounds.set(x, y, width, height)
        reset()
        return this
    }

    fun isUsed(): Boolean = used

    /**
     * Called by GameWorld when the player overlaps this orb and jump is held.
     * Single-use orbs fire only once; multiActivate orbs fire every entry.
     */
    fun tryActivate(player: AbstractPlayer) {
        if (multiActivate || !used) {
            used = true
            player.isJumpConsumed = true
            onClick(player)
        }
    }

    /**
     * Called by GameWorld each tick when the player is NOT overlapping this orb.
     * Allows multiActivate orbs to fire again on the next entry.
     * Single-use orbs are unaffected.
     */
    fun resetOverlap() {
        if (multiActivate) used = false
    }

    /** Resets the used flag entirely — call this on level reset / respawn. */
    fun reset() {
        used = false
    }

    abstract fun onClick(player: AbstractPlayer)
}
