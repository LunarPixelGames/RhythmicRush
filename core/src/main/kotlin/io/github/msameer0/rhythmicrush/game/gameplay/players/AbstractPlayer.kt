package io.github.msameer0.rhythmicrush.game.gameplay.players

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.GameWorld

/**
 * Base class for all player modes, handling common movement, input, and state properties.
 */
abstract class AbstractPlayer() {

    /**
     * Enumeration of available player game modes.
     */
    enum class PlayerType {
        CUBE,
        SHIP
    }

    @JvmField
    var gravity: Float = 0f
    @JvmField
    protected var type: PlayerType? = null

    @JvmField
    var x: Float = 0f
    @JvmField
    var y: Float = 0f
    @JvmField
    var worldX: Float = 0f
    @JvmField
    var width: Float = 100f
    @JvmField
    var height: Float = 100f
    @JvmField
    var velocityY: Float = 0f
    @JvmField
    var bounds: Rectangle = Rectangle(x, y, width, height)
    @JvmField
    var innerBounds: Rectangle = Rectangle(x + 40f, y + 40f, 20f, 20f)

    private var rotation: Float = 0f
    private val playerPolygon: com.badlogic.gdx.math.Polygon = com.badlogic.gdx.math.Polygon()
    private val innerPolygon: com.badlogic.gdx.math.Polygon = com.badlogic.gdx.math.Polygon()

    @JvmField
    protected var gravityFlipped: Boolean = false
    @JvmField
    protected var mini: Boolean = false
    @JvmField
    protected var currentSlopeRotation: Float = 0f
    @JvmField
    protected var jumpHeld: Boolean = false
    @JvmField
    protected var jumpConsumed: Boolean = false
    @JvmField
    protected var justPressed: Boolean = false
    @JvmField
    protected var world: GameWorld? = null
    @JvmField
    var lastPortalCenterY: Float = 0f
    @JvmField
    var lastPortalBottomY: Float = 0f

    fun isMini(): Boolean = mini

    fun setMini(mini: Boolean) {
        val oldWidth = this.width
        val oldHeight = this.height

        if (mini && !this.mini) {
            this.mini = true
            this.width = 50f
            this.height = 50f
            this.x += (oldWidth - this.width) / 2f
            this.y += (oldHeight - this.height) / 2f
        } else if (!mini && this.mini) {
            this.mini = false
            this.width = 100f
            this.height = 100f
            this.x -= (this.width - oldWidth) / 2f
            if (gravityFlipped) {
                this.y -= (this.height - oldHeight)
            }
        }

        bounds.setSize(width, height)
        updateBounds()
    }

    fun getGravity(): Float = gravity

    abstract fun init(
        startX: Float,
        startY: Float,
        velocityY: Float,
        jumpHeld: Boolean
    ): AbstractPlayer

    abstract fun init(startX: Float, startY: Float): AbstractPlayer
    abstract fun update(delta: Float, groundY: Float, ceilingY: Float)
    abstract fun jump()
    abstract fun copyState(other: AbstractPlayer)

    fun setJumpHeld(held: Boolean) {
        if (held && !this.jumpHeld) {
            this.jumpConsumed = false
            this.justPressed = true
        }
        this.jumpHeld = held
    }

    fun setY(y: Float) {
        this.y = y
        updateBounds()
    }


    fun postUpdate() {
        justPressed = false
    }

    fun isJustPressed(): Boolean = justPressed
    fun isJumpHeld(): Boolean = jumpHeld
    fun isJumpConsumed(): Boolean = jumpConsumed
    fun setJumpConsumed(consumed: Boolean) {
        jumpConsumed = consumed
    }

    fun getBounds(): Rectangle = bounds
    fun getInnerBounds(): Rectangle = innerBounds
    fun getRotation(): Float = rotation
    fun setRotation(rot: Float) {
        rotation = rot
    }

    fun getPlayerPolygon(): com.badlogic.gdx.math.Polygon = playerPolygon
    fun getInnerPolygon(): com.badlogic.gdx.math.Polygon = innerPolygon

    protected fun updateBounds() {
        bounds.setPosition(x, y)
        innerBounds.set(
            x + (width - 20f) / 2f,
            y + (height - 20f) / 2f,
            20f,
            20f
        )

        // Update Polygons
        if (playerPolygon.vertices.size != 8) {
            playerPolygon.vertices = floatArrayOf(
                0f, 0f,
                width, 0f,
                width, height,
                0f, height
            )
        }
        playerPolygon.setPosition(x, y)
        playerPolygon.setOrigin(width / 2f, height / 2f)
        playerPolygon.rotation = rotation

        if (innerPolygon.vertices.size != 8) {
            innerPolygon.vertices = floatArrayOf(
                0f, 0f,
                20f, 0f,
                20f, 20f,
                0f, 20f
            )
        }
        innerPolygon.setPosition(x + (width - 20f) / 2f, y + (height - 20f) / 2f)
        innerPolygon.setOrigin(10f, 10f)
        innerPolygon.rotation = rotation
    }

    fun getX(): Float = x
    fun getWorldX(): Float = worldX
    fun setWorldX(worldX: Float) {
        this.worldX = worldX
    }

    fun getY(): Float = y

    fun setWorld(world: GameWorld) {
        this.world = world
    }

    fun getWorld(): GameWorld? = world

    fun setVelocityY(vel: Float) {
        velocityY = vel
    }

    fun getVelocityY(): Float = velocityY

    open fun setGrounded(grounded: Boolean) {}
    open fun tryJump() {}
    open fun isGrounded(): Boolean = false
    open fun isSafeFromBelow(): Boolean = false

    fun getType(): PlayerType? = type

    fun isGravityFlipped(): Boolean = gravityFlipped
    fun setGravityFlipped(gravityFlipped: Boolean) {
        this.gravityFlipped = gravityFlipped
    }

    fun getCurrentSlopeRotation(): Float = currentSlopeRotation
    fun setCurrentSlopeRotation(rot: Float) {
        currentSlopeRotation = rot
    }
}
