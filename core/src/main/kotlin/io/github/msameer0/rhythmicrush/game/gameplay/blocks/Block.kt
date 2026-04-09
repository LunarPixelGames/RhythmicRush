package io.github.msameer0.rhythmicrush.game.gameplay.blocks

import com.badlogic.gdx.math.Rectangle
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "block")
open class Block {
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f
    var height: Float = 0f
    var bounds: Rectangle = Rectangle()
    var type: BlockType

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

    fun init(x: Float, y: Float, size: Float, type: BlockType): Block {
        this.x = x
        this.y = y
        this.width = size
        this.height = size
        this.type = type
        bounds.set(x, y, width, height)
        return this
    }

    fun updatePosition(scrollSpeed: Float, delta: Float) {
        x -= scrollSpeed * delta
        updateBounds()
    }

    fun updateBounds() {
        bounds.setPosition(x, y)
    }

    open fun reset() {
        this.x = 0f
        this.y = 0f
        this.width = 0f
        this.height = 0f
        this.type = BlockType.DEFAULT
        this.bounds.set(0f, 0f, 0f, 0f)
    }

    open fun tryTouch(player: AbstractPlayer) {
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

        val hMargin = playerRect.width * 0.25f
        val vMargin = playerRect.height * 0.25f

        if (playerRight - hMargin > blockLeft && playerLeft + hMargin < blockRight && playerTop - vMargin > blockBottom && playerBottom + vMargin < blockTop) {
            val world = player.getWorld()
            world?.playerDied()
        }
    }


}
