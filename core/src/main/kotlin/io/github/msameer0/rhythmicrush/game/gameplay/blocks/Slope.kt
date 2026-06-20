package io.github.msameer0.rhythmicrush.game.gameplay.blocks

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * Special type of block that provides sloped surfaces for non-orthogonal player movement.
 */
@Registry(id = "slope")
class Slope : Block {
    companion object {
        const val CIRCLE_RATIO = 1f
        const val ASCENDING_EXIT_VELOCITY_MULTIPLIER = 1.3f
        private const val DESCENDING_ENTRY_EPSILON = 0.5f
        private const val COLLISION_LINE_OFFSET = 5f

        private val tmpEdge = Vector2()
        private val tmpNormal = Vector2()
        private val tmpCenter = Vector2()
        private val tmpOrigin = Vector2()
    }


    constructor() : super()

    constructor(x: Float, y: Float, size: Float, rotation: Float) : super(
        x, y, size, BlockType.DEFAULT
    ) {
        this.rotation = rotation
    }

    override fun init(x: Float, y: Float, size: Float, type: BlockType, rotation: Float): Slope {
        super.init(x, y, size, type, rotation)
        return this
    }

    override fun reset() {
        super.reset()
    }

    private fun normalizedRotation(): Int {
        return (MathUtils.round(rotation / 90f) * 90 % 360 + 360) % 360
    }

    fun getSlopeLine(): FloatArray {
        val normalizedRotation = normalizedRotation()
        val line = if (normalizedRotation == 0 || normalizedRotation == 180) {
            floatArrayOf(x, y, x + width, y + height)
        } else {
            floatArrayOf(x, y + height, x + width, y)
        }

        val edgeX = line[2] - line[0]
        val edgeY = line[3] - line[1]
        val edgeLength = kotlin.math.sqrt(edgeX * edgeX + edgeY * edgeY)
        var normalX = -edgeY / edgeLength
        var normalY = edgeX / edgeLength
        val isFloor = normalizedRotation == 0 || normalizedRotation == 270
        if ((isFloor && normalY < 0f) || (!isFloor && normalY > 0f)) {
            normalX = -normalX
            normalY = -normalY
        }

        val offsetX = normalX * COLLISION_LINE_OFFSET
        val offsetY = normalY * COLLISION_LINE_OFFSET
        line[0] += offsetX
        line[1] += offsetY
        line[2] += offsetX
        line[3] += offsetY
        return line
    }

    fun coversSupportBlock(block: Block, player: AbstractPlayer): Boolean {
        val playerBounds = player.getBounds()
        val overlapLeft = maxOf(x, block.x, playerBounds.x)
        val overlapRight = minOf(
            x + width,
            block.x + block.width,
            playerBounds.x + playerBounds.width
        )
        if (overlapRight - overlapLeft <= 0.01f) return false

        val overlapBottom = maxOf(y, block.y)
        val overlapTop = minOf(y + height, block.y + block.height)
        if (overlapTop - overlapBottom <= 0.01f) return false

        val line = getSlopeLine()
        val sampleX = (overlapLeft + overlapRight) * 0.5f
        val lineAmount = (sampleX - line[0]) / (line[2] - line[0])
        val surfaceY = line[1] + (line[3] - line[1]) * lineAmount
        val blockCenterY = (overlapBottom + overlapTop) * 0.5f
        val rotation = normalizedRotation()

        return if (rotation == 0 || rotation == 270) {
            blockCenterY <= surfaceY
        } else {
            blockCenterY >= surfaceY
        }
    }

    override fun tryTouch(player: AbstractPlayer) {
        tryTouch(player, false)
    }

    fun tryTouch(player: AbstractPlayer, requireDescendingSurfaceCrossing: Boolean) {
        val playerBounds = player.getBounds()

        val playerRadius = playerBounds.width * 0.5f * CIRCLE_RATIO
        val playerCenterX = playerBounds.x + playerBounds.width * 0.5f
        val playerCenterY = playerBounds.y + playerBounds.height * 0.5f

        val blockLeft = bounds.x
        val blockRight = bounds.x + bounds.width
        val blockBottom = bounds.y
        val blockTop = bounds.y + bounds.height

        val collisionMargin = playerRadius * 2f
        if (
            playerCenterX + playerRadius < blockLeft - collisionMargin ||
            playerCenterX - playerRadius > blockRight + collisionMargin ||
            playerCenterY + playerRadius < blockBottom - collisionMargin ||
            playerCenterY - playerRadius > blockTop + collisionMargin
        ) {
            return
        }

        val flipped = player.isGravityFlipped()
        val normalizedRotation = normalizedRotation()
        val scrollSpeed = player.getWorld()?.scrollSpeed ?: 320f

        val isFloor = normalizedRotation == 0 || normalizedRotation == 270
        val isCeiling = normalizedRotation == 90 || normalizedRotation == 180

        val isClimbing =
            (!flipped && normalizedRotation == 0) ||
                (flipped && normalizedRotation == 90)
        val isDescending =
            (!flipped && normalizedRotation == 270) ||
                (flipped && normalizedRotation == 180)

        val isShip = player.getType() == AbstractPlayer.PlayerType.SHIP

        if (!isShip) {
            if ((!flipped && !isFloor) || (flipped && !isCeiling)) return
        }

        val lineStartX: Float
        val lineStartY: Float
        val lineEndX: Float
        val lineEndY: Float

        when (normalizedRotation) {
            0 -> {
                lineStartX = blockLeft
                lineStartY = blockBottom
                lineEndX = blockRight
                lineEndY = blockTop
            }

            90 -> {
                lineStartX = blockRight
                lineStartY = blockBottom
                lineEndX = blockLeft
                lineEndY = blockTop
            }

            180 -> {
                lineStartX = blockRight
                lineStartY = blockTop
                lineEndX = blockLeft
                lineEndY = blockBottom
            }

            else -> {
                lineStartX = blockLeft
                lineStartY = blockTop
                lineEndX = blockRight
                lineEndY = blockBottom
            }
        }

        if (isDescending && requireDescendingSurfaceCrossing) {
            if (playerCenterX < blockLeft || playerCenterX > blockRight) return

            val collisionLine = getSlopeLine()
            val lineAmount =
                (playerCenterX - collisionLine[0]) /
                    (collisionLine[2] - collisionLine[0])
            val surfaceY =
                collisionLine[1] +
                    (collisionLine[3] - collisionLine[1]) * lineAmount
            val gravityFacingEdgeY =
                if (flipped) playerBounds.y + playerBounds.height else playerBounds.y
            val crossedSurface =
                if (flipped) {
                    gravityFacingEdgeY > surfaceY + DESCENDING_ENTRY_EPSILON
                } else {
                    gravityFacingEdgeY < surfaceY - DESCENDING_ENTRY_EPSILON
                }

            if (!crossedSurface) return
        }

        tmpEdge.set(lineEndX - lineStartX, lineEndY - lineStartY)
        val edgeLength = tmpEdge.len()

        tmpNormal.set(tmpEdge).nor()
        tmpNormal.rotate90(1)

        var normalX = tmpNormal.x
        var normalY = tmpNormal.y

        if (isCeiling && normalY > 0) {
            normalX = -normalX
            normalY = -normalY
        } else if (isFloor && normalY < 0) {
            normalX = -normalX
            normalY = -normalY
        }

        tmpCenter.set(playerCenterX, playerCenterY)
        tmpOrigin.set(
            lineStartX + normalX * COLLISION_LINE_OFFSET,
            lineStartY + normalY * COLLISION_LINE_OFFSET
        )
        val distanceFromSlope =
            tmpCenter.sub(tmpOrigin).dot(tmpNormal.set(normalX, normalY))

        val snapTolerance = when {
            isDescending -> playerRadius * 0.5f
            isClimbing && player.getCurrentSlopeRotation() != 0f -> playerRadius * 0.5f
            else -> 0f
        }

        if (distanceFromSlope >= playerRadius + snapTolerance) return
        if (distanceFromSlope < -playerRadius) return

        tmpCenter.set(
            playerCenterX - tmpOrigin.x,
            playerCenterY - tmpOrigin.y
        )
        val positionAlongSlope = tmpCenter.dot(tmpEdge) / (edgeLength * edgeLength)
        if (positionAlongSlope < -0.01f || positionAlongSlope > 1.01f) return

        val targetVelocityY = -(scrollSpeed * normalX) / normalY
        val isFloorForPlayer = (!flipped && isFloor) || (flipped && isCeiling)

        if (isFloorForPlayer) {
            val jumpingOff =
                (!flipped &&
                    player.getVelocityY() > maxOf(0f, targetVelocityY) + 1.5f) ||
                    (flipped &&
                        player.getVelocityY() < minOf(0f, targetVelocityY) - 1.5f)
            if (jumpingOff) return
        }

        val pushOutY = (playerRadius - distanceFromSlope) / normalY
        val playerCenterOffsetY = playerCenterY - player.y
        player.setY(playerCenterY + pushOutY - playerCenterOffsetY)

        if (isFloorForPlayer) player.setGrounded(true)

        player.setCurrentSlopeRotation(if (isClimbing) 45f else -45f)

        when {
            isDescending -> player.setVelocityY(targetVelocityY)
            isClimbing && !flipped &&
                player.getVelocityY() <= targetVelocityY -> player.setVelocityY(
                targetVelocityY
            )

            isClimbing && flipped &&
                player.getVelocityY() >= targetVelocityY -> player.setVelocityY(
                targetVelocityY
            )
        }
    }

    override fun tick(delta: Float) {
    }
}
