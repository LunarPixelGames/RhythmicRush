package io.github.msameer0.rhythmicrush.game.gameplay.blocks

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Registry(id = "slope")
class Slope : Block {
    companion object {
        const val CIRCLE_RATIO = 0.8f
    }

    var rotation: Float = 0f

    constructor() : super()

    constructor(x: Float, y: Float, size: Float, rotation: Float) : super(x, y, size, BlockType.DEFAULT) {
        this.rotation = rotation
    }

    fun init(x: Float, y: Float, size: Float, type: BlockType, rotation: Float): Slope {
        super.init(x, y, size, type)
        this.rotation = rotation
        return this
    }

    override fun reset() {
        super.reset()
        this.rotation = 0f
    }

    private fun normaliseRot(): Int {
        return ((rotation / 90f).roundToInt() * 90 % 360 + 360) % 360
    }

    fun getSlopeLine(): FloatArray {
        val rot = normaliseRot()
        return if (rot == 0 || rot == 180) {
            floatArrayOf(x, y, x + width, y + height)
        } else {
            floatArrayOf(x, y + height, x + width, y)
        }
    }

    public override fun tryTouch(player: AbstractPlayer) {
        val pr = player.getBounds()

        val r = (pr.width * 0.5f) * CIRCLE_RATIO
        val cx = pr.x + pr.width * 0.5f
        val cy = pr.y + pr.height * 0.5f

        val bL = bounds.x
        val bR = bounds.x + bounds.width
        val bB = bounds.y
        val bT = bounds.y + bounds.height
        val bW = bounds.width
        val bH = bounds.height

        val margin = r * 2.0f
        if (cx + r < bL - margin || cx - r > bR + margin || cy + r < bB - margin || cy - r > bT + margin) return

        val flipped = player.isGravityFlipped()
        val rot = normaliseRot()
        val scrollSpeed =
            if (player.getWorld() != null) player.getWorld()!!.scrollSpeed else 320f

        val isFloor = (rot == 0 || rot == 270)
        val isCeiling = (rot == 90 || rot == 180)

        val isClimbing = (!flipped && rot == 0) || (flipped && rot == 90)
        val isDescending = (!flipped && rot == 270) || (flipped && rot == 180)

        val isShip = player.getType() == AbstractPlayer.PlayerType.SHIP

        if (!isShip) {
            if ((!flipped && !isFloor) || (flipped && !isCeiling)) return
        }

        val lx1: Float
        val ly1: Float
        val lx2: Float
        val ly2: Float
        var nx: Float
        var ny: Float
        val edgeLen = sqrt((bW * bW + bH * bH).toDouble()).toFloat()

        when (rot) {
            0 -> {
                lx1 = bL
                ly1 = bB
                lx2 = bR
                ly2 = bT
                nx = -bH / edgeLen
                ny = bW / edgeLen
            }
            90 -> {
                lx1 = bR
                ly1 = bB
                lx2 = bL
                ly2 = bT
                nx = bH / edgeLen
                ny = bW / edgeLen
            }
            180 -> {
                lx1 = bR
                ly1 = bT
                lx2 = bL
                ly2 = bB
                nx = bH / edgeLen
                ny = -bW / edgeLen
            }
            else -> {
                lx1 = bL
                ly1 = bT
                lx2 = bR
                ly2 = bB
                nx = -bH / edgeLen
                ny = -bW / edgeLen
            }
        }

        if (isCeiling && ny > 0) {
            nx = -nx
            ny = -ny
        } else if (isFloor && ny < 0) {
            nx = -nx
            ny = -ny
        }

        val dist = (cx - lx1) * nx + (cy - ly1) * ny

        var snapTolerance = 0f

        if (isDescending) {
            snapTolerance = r * 0.5f
        } else if (isClimbing) {
            if (player.getCurrentSlopeRotation() != 0f) {
                snapTolerance = r * 0.5f
            }
        }

        if (dist >= r + snapTolerance) return
        if (dist < -r) return

        val edgeDx = lx2 - lx1
        val edgeDy = ly2 - ly1
        val t = ((cx - lx1) * edgeDx + (cy - ly1) * edgeDy) / (edgeLen * edgeLen)
        if (t < -0.01f || t > 1.01f) return

        val targetVy = -(scrollSpeed * nx) / ny

        var isJumpingOff = false

        val isFloorForPlayer = (!flipped && isFloor) || (flipped && isCeiling)

        if (isFloorForPlayer) {
            if (!flipped && player.getVelocityY() > max(0f, targetVy) + 1.5f) isJumpingOff = true
            if (flipped && player.getVelocityY() < min(0f, targetVy) - 1.5f) isJumpingOff = true
        }

        if (isJumpingOff) return

        val penetration = r - dist

        val pushOutY = penetration / ny

        val newCy = cy + pushOutY
        val offsetY = cy - player.y
        player.setY(newCy - offsetY)

        if (isFloorForPlayer) {
            player.setGrounded(true)
        }

        player.setCurrentSlopeRotation(if (isClimbing) 45f else -45f)

        if (isDescending) {
            player.setVelocityY(targetVy)
        } else if (isClimbing) {
            if (!flipped && player.getVelocityY() <= targetVy) {
                player.setVelocityY(targetVy)
            } else if (flipped && player.getVelocityY() >= targetVy) {
                player.setVelocityY(targetVy)
            }
        }
    }
}
