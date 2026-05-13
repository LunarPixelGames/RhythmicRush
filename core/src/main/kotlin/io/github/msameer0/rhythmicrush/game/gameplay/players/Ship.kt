package io.github.msameer0.rhythmicrush.game.gameplay.players

import com.badlogic.gdx.math.MathUtils
import io.github.msameer0.rhythmicrush.game.registries.Registry
import io.github.msameer0.rhythmicrush.GameConstants

/**
 * Ship game mode featuring flying mechanics controlled by holding the jump button.
 */
@Registry(id = "ship")
class Ship : AbstractPlayer {

    @JvmField
    var maxUpSpeed: Float = GameConstants.Player.Ship.MAX_UP_SPEED
    @JvmField
    var maxDownSpeed: Float = GameConstants.Player.Ship.MAX_DOWN_SPEED
    @JvmField
    var accel: Float = GameConstants.Player.Ship.ACCEL
    @JvmField
    var decel: Float = GameConstants.Player.Ship.DECEL

    companion object {
        private const val SHIP_TILT_EXAGGERATION = GameConstants.Player.Ship.TILT_EXAGGERATION
        private const val SHIP_MAX_TILT = GameConstants.Player.Ship.MAX_TILT
        private const val SHIP_TILT_LERP = GameConstants.Player.Ship.TILT_LERP
    }

    private var groundY: Float = GameConstants.World.GROUND_Y
    private var restrictedCameraY: Float = 810f
    private var corridorBottom: Float = 0f
    private var corridorTop: Float = 1080f
    private var isUsingCorridor: Boolean = false

    constructor(startX: Float, startY: Float) : super() {
        this.x = startX
        this.y = startY
        gravity = GameConstants.Player.Cube.GRAVITY
        type = PlayerType.SHIP
    }

    constructor() : super() {
        gravity = GameConstants.Player.Cube.GRAVITY
        type = PlayerType.SHIP
    }

    override fun init(startX: Float, startY: Float, velocityY: Float, jumpHeld: Boolean): Ship {
        type = PlayerType.SHIP
        x = startX
        y = startY
        gravity = GameConstants.Player.Cube.GRAVITY
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

    override fun update(delta: Float, groundY: Float, ceilingY: Float) {
        val lastSlopeRotation = currentSlopeRotation
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

        if (y < groundY) {
            y = groundY
            velocityY = 0f
        } else if (y + height > ceilingY) {
            y = ceilingY - height
            velocityY = 0f
        }

        updateBounds()

        // Update Tilt Rotation with exaggeration and smoothing to prevent snapping
        val scrollSpeed = world?.scrollSpeed ?: GameConstants.World.SCROLL_SPEED
        var targetAngle = MathUtils.atan2(velocityY, scrollSpeed) * MathUtils.radiansToDegrees * GameConstants.Player.Ship.TILT_EXAGGERATION
        if (gravityFlipped) targetAngle = -targetAngle
        targetAngle = MathUtils.clamp(targetAngle, -GameConstants.Player.Ship.MAX_TILT, GameConstants.Player.Ship.MAX_TILT)
        
        setRotation(MathUtils.lerp(getRotation(), targetAngle, MathUtils.clamp(GameConstants.Player.Ship.TILT_LERP * delta, 0f, 1f)))
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
        lastPortalCenterY = other.lastPortalCenterY
        lastPortalBottomY = other.lastPortalBottomY
    }

    // Camera and Boundary management
    override fun getCameraMode(): CameraMode = CameraMode.RESTRICTED

    override fun onCameraModeEnter(cameraY: Float, worldGroundY: Float) {
        val distToGround = lastPortalBottomY - worldGroundY

        if (distToGround in 0f..400f) {
            // Case 1: Near Ground (0-4 grids) - Use real ground with padding
            restrictedCameraY = worldGroundY + 501f // 540 - 39 padding
            isUsingCorridor = false
        } else {
            // Case 2: High Air - Use dynamic corridor centered on portal
            restrictedCameraY = lastPortalBottomY + 100f // Portal center (assuming 200px height)
            corridorBottom = lastPortalBottomY - 400f // 4 grids below
            corridorTop = lastPortalBottomY + 600f    // 4 grids above (200 portal + 400)
            isUsingCorridor = true
        }
    }

    override fun getRestrictedCameraY(): Float = restrictedCameraY

    override fun getCameraFloorY(worldGroundY: Float): Float {
        return if (isUsingCorridor) corridorBottom else worldGroundY
    }

    override fun getCameraCeilingY(): Float {
        return if (isUsingCorridor) corridorTop else (restrictedCameraY - 501f + 1080f - 39f)
    }

    override fun isUsingCorridor(): Boolean = isUsingCorridor
    override fun getCorridorTop(): Float? = if (isUsingCorridor) corridorTop else null
    override fun getCorridorBottom(): Float? = if (isUsingCorridor) corridorBottom else null
}
