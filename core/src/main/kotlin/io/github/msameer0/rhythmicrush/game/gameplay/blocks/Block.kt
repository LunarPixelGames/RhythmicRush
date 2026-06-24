package io.github.msameer0.rhythmicrush.game.gameplay.blocks

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.engine.Rotatable
import io.github.msameer0.rhythmicrush.game.engine.Tickable
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "block")
/**
 * Base class for all physical blocks in the game that the player can collide with or walk on.
 */
open class Block : Tickable, Rotatable {
    override var rotation: Float = 0f
    var x: Float = 0f
    var worldX: Float = 0f
    var y: Float = 0f
    var width: Float = 0f
    var height: Float = 0f
    var bounds: Rectangle = Rectangle()
    var type: BlockType
    var untouchable: Boolean = false

    constructor() {
        this.type = BlockType.DEFAULT
    }

    constructor(x: Float, y: Float, size: Float) : this(x, y, size, BlockType.DEFAULT)

    constructor(x: Float, y: Float, size: Float, type: BlockType) {
        this.x = x
        this.y = y
        this.width = size
        this.height = size
        this.type = type
        bounds = Rectangle(x, y, width, height)
    }

    open fun init(x: Float, y: Float, size: Float, type: BlockType, rotation: Float): Block {
        this.x = x
        this.worldX = x
        this.y = y
        this.width = size
        this.height = size
        this.type = type
        this.rotation = rotation
        this.untouchable = false
        bounds.set(x, y, width, height)
        return this
    }

    open fun init(x: Float, y: Float, size: Float, type: BlockType): Block {
        return init(x, y, size, type, 0f)
    }

    fun updatePosition(worldScrolled: Float) {
        x = worldX - worldScrolled
        updateBounds()
    }

    fun updateBounds() {
        bounds.setPosition(x, y)
    }

    private val blockPolygon = Polygon()
    private var polygonWidth = -1f
    private var polygonHeight = -1f

    open fun reset() {
        this.x = 0f
        this.worldX = 0f
        this.y = 0f
        this.width = 0f
        this.height = 0f
        this.type = BlockType.DEFAULT
        this.rotation = 0f
        this.untouchable = false
        this.bounds.set(0f, 0f, 0f, 0f)
    }

    open fun tryTouch(player: AbstractPlayer) {
        if (untouchable) return

        val playerRect = player.getBounds()
        if (!playerRect.overlaps(bounds)) return

        val playerBottom = playerRect.y
        val playerTop = playerRect.y + playerRect.height
        val playerLeft = playerRect.x
        val playerRight = playerRect.x + playerRect.width

        val blockTop = bounds.y + bounds.height
        val blockBottom = bounds.y
        val blockLeft = bounds.x
        val blockRight = bounds.x + bounds.width

        val overlapTop = blockTop - playerBottom
        val overlapBottom = playerTop - blockBottom
        val overlapLeft = playerRight - blockLeft
        val overlapRight = blockRight - playerLeft

        var minOverlap = overlapTop
        if (overlapBottom < minOverlap) minOverlap = overlapBottom
        if (overlapLeft < minOverlap) minOverlap = overlapLeft
        if (overlapRight < minOverlap) minOverlap = overlapRight

        val flipped = player.isGravityFlipped()

        if (!flipped) {
            if (minOverlap == overlapTop && player.velocityY <= 0) {
                player.setY(blockTop)
                player.setVelocityY(0f)
                player.setGrounded(true)
                return
            }

            if (minOverlap == overlapBottom && player.velocityY >= 0 && player.isSafeFromBelow()) {
                player.setY(blockBottom - player.height)
                player.setVelocityY(0f)
                return
            }
        } else {
            if (minOverlap == overlapBottom && player.velocityY >= 0) {
                player.setY(blockBottom - player.height)
                player.setVelocityY(0f)
                player.setGrounded(true)
                return
            }

            if (minOverlap == overlapTop && player.velocityY <= 0 && player.isSafeFromBelow()) {
                player.setY(blockTop)
                player.setVelocityY(0f)
                return
            }
        }

        if (polygonWidth != width || polygonHeight != height) {
            var verts = blockPolygon.vertices
            if (verts == null || verts.size != 8) {
                verts = FloatArray(8)
                blockPolygon.vertices = verts
            }
            verts[0] = 0f; verts[1] = 0f
            verts[2] = width; verts[3] = 0f
            verts[4] = width; verts[5] = height
            verts[6] = 0f; verts[7] = height
            polygonWidth = width
            polygonHeight = height
        }
        blockPolygon.setPosition(x, y)
        blockPolygon.setOrigin(width / 2f, height / 2f)
        blockPolygon.rotation = rotation

        if (Intersector.overlapConvexPolygons(player.getInnerPolygon(), blockPolygon)) {
            val world = player.getWorld()
            world?.playerDied()
        }
    }

    override fun tick(delta: Float) {
    }
}
