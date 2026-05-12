package io.github.msameer0.rhythmicrush.game.gameplay.players

import com.badlogic.gdx.math.MathUtils
import io.github.msameer0.rhythmicrush.game.registries.Registry
import io.github.msameer0.rhythmicrush.GameConstants

/**
 * Standard Cube game mode focusing on platforming and precise jumping.
 */
@Registry(id = "cube")
class Cube : AbstractPlayer {

    @JvmField
    var jumpVelocity: Float = GameConstants.Player.Cube.JUMP_VELOCITY

    private var isGrounded: Boolean = false

    private var coyoteTimer: Float = 0f

    companion object {
        private const val COYOTE_TIME = GameConstants.Player.Cube.COYOTE_TIME
        private const val CUBE_SPIN_FACTOR = 0.5f
    }

    constructor(startX: Float, groundY: Float) : super() {
        this.x = startX
        this.y = groundY
        gravity = GameConstants.Player.Cube.GRAVITY
        type = PlayerType.CUBE
    }

    constructor() : super() {
        gravity = GameConstants.Player.Cube.GRAVITY
        type = PlayerType.CUBE
    }

    override fun init(startX: Float, startY: Float, velocityY: Float, jumpHeld: Boolean): Cube {
        type = PlayerType.CUBE
        x = startX
        y = startY
        gravity = GameConstants.Player.Cube.GRAVITY
        this.velocityY = velocityY
        isGrounded = false
        this.jumpHeld = jumpHeld
        jumpConsumed = false
        gravityFlipped = false
        setMini(false)
        coyoteTimer = 0f
        world = null
        bounds.setPosition(x, y)
        return this
    }

    override fun init(startX: Float, startY: Float): Cube = init(startX, startY, 0f, false)

    override fun update(delta: Float, groundY: Float, ceilingY: Float) {
        val wasGrounded = isGrounded
        val lastSlopeRotation = currentSlopeRotation
        isGrounded = false
        currentSlopeRotation = 0f

        val effectiveGravity = if (gravityFlipped) -gravity else gravity
        velocityY += effectiveGravity * delta
        y += velocityY * delta

        if (!gravityFlipped) {
            if (!isGrounded && y <= groundY) {
                y = groundY
                velocityY = 0f
                isGrounded = true
            }
        } else {
            @Suppress("UNUSED_VARIABLE")
            val ceilingY = 1080 - groundY - height
        }

        if (wasGrounded) {
            coyoteTimer = COYOTE_TIME

            // Snap rotation to nearest 90 degrees relative to slope
            val currentRot = getRotation()
            val nearest90 = MathUtils.round((currentRot - lastSlopeRotation) / 90f) * 90f
            setRotation(MathUtils.lerp(currentRot, nearest90 + lastSlopeRotation, MathUtils.clamp(delta * 15f, 0f, 1f)))
        } else {
            coyoteTimer = kotlin.math.max(0f, coyoteTimer - delta)

            // Rotate in air
            val spinAmount = GameConstants.Player.Cube.SPIN_SPEED * delta
            if (gravityFlipped) setRotation(getRotation() + spinAmount)
            else setRotation(getRotation() - spinAmount)
        }

        updateBounds()
    }

    private fun canJump(): Boolean = isGrounded || coyoteTimer > 0f

    override fun jump() {
        if (canJump()) {
            val v = if (mini) jumpVelocity * 0.75f else jumpVelocity
            velocityY = if (gravityFlipped) -v else v
            isGrounded = false
            coyoteTimer = 0f
            jumpConsumed = true
        }
    }

    override fun tryJump() {
        if (jumpHeld && canJump()) jump()
    }

    override fun setGrounded(g: Boolean) {
        isGrounded = g
    }

    override fun isGrounded(): Boolean = isGrounded

    override fun copyState(other: AbstractPlayer) {
        x = other.x
        y = other.y
        velocityY = other.velocityY
        isGrounded = other.isGrounded()
        jumpHeld = other.isJumpHeld()
        jumpConsumed = other.isJumpConsumed()
        gravityFlipped = other.isGravityFlipped()
        currentSlopeRotation = other.getCurrentSlopeRotation()
        setMini(other.isMini())
        coyoteTimer = 0f
    }
}
