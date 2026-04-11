package io.github.msameer0.rhythmicrush.game.gameplay.players

import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * Ship game mode featuring flying mechanics controlled by holding the jump button.
 */
@Registry(id = "ship")
class Ship : AbstractPlayer {

    @JvmField
    var maxUpSpeed: Float = 400f
    @JvmField
    var maxDownSpeed: Float = -500f
    @JvmField
    var accel: Float = 1000f
    @JvmField
    var decel: Float = 800f

    private var groundY: Float = 50f

    constructor(startX: Float, startY: Float) : super() {
        this.x = startX
        this.y = startY
        gravity = -1800f
        type = PlayerType.SHIP
    }

    constructor() : super() {
        gravity = -1800f
        type = PlayerType.SHIP
    }

    override fun init(startX: Float, startY: Float, velocityY: Float, jumpHeld: Boolean): Ship {
        type = PlayerType.SHIP
        x = startX
        y = startY
        gravity = -1800f
        this.velocityY = velocityY
        this.jumpHeld = jumpHeld
        jumpConsumed = false
        gravityFlipped = false
        setMini(false)
        world = null
        bounds.setPosition(x, y)
        return this
    }

    override fun init(startX: Float, startY: Float): Ship = init(startX, startY, 0f, false)

    override fun update(delta: Float, groundY: Float) {
        this.groundY = groundY
        currentSlopeRotation = 0f

        val effectiveAccel = if (mini) accel * 1.3f else accel
        val effectiveDecel = if (mini) decel * 1.3f else decel

        if (!gravityFlipped) {
            if (jumpHeld) {
                velocityY += effectiveAccel * delta
                if (velocityY > maxUpSpeed) velocityY = maxUpSpeed
                if (!justPressed) jumpConsumed = true
            } else {
                velocityY -= effectiveDecel * delta
                if (velocityY < maxDownSpeed) velocityY = maxDownSpeed
            }
        } else {
            if (jumpHeld) {
                velocityY -= effectiveAccel * delta
                if (velocityY < -maxUpSpeed) velocityY = -maxUpSpeed
                if (!justPressed) jumpConsumed = true
            } else {
                velocityY += effectiveDecel * delta
                if (velocityY > -maxDownSpeed) velocityY = -maxDownSpeed
            }
        }

        y += velocityY * delta

        if (!gravityFlipped && y < groundY) {
            y = groundY
            velocityY = 0f
        }

        updateBounds()
    }

    override fun jump() {

    }

    override fun isSafeFromBelow(): Boolean = true

    fun getGroundY(): Float = groundY
    fun setGroundY(groundY: Float) {
        this.groundY = groundY
    }

    override fun copyState(other: AbstractPlayer) {
        x = other.x
        y = other.y
        velocityY = other.velocityY
        gravityFlipped = other.isGravityFlipped()
        jumpHeld = other.isJumpHeld()
        jumpConsumed = other.isJumpConsumed()
        currentSlopeRotation = other.getCurrentSlopeRotation()
        setMini(other.isMini())
    }
}
