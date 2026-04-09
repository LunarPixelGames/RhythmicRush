package io.github.msameer0.rhythmicrush.game.gameplay.blocks

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "slope")
class Slope : Block {
    companion object {
        const val CIRCLE_RATIO = 0.8f

        // Vector2 pool to avoid allocations in the hot path
        private val tmpEdge = Vector2()
        private val tmpNormal = Vector2()
        private val tmpCenter = Vector2()
        private val tmpOrigin = Vector2()
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
        // MathUtils.round instead of kotlin roundToInt — stays in libGDX land
        return (MathUtils.round(rotation / 90f) * 90 % 360 + 360) % 360
    }

    fun getSlopeLine(): FloatArray {
        val rot = normaliseRot()
        return if (rot == 0 || rot == 180) {
            floatArrayOf(x, y, x + width, y + height)
        } else {
            floatArrayOf(x, y + height, x + width, y)
        }
    }

    override fun tryTouch(player: AbstractPlayer) {
        val pr = player.getBounds()

        val r   = pr.width * 0.5f * CIRCLE_RATIO
        val cx  = pr.x + pr.width  * 0.5f
        val cy  = pr.y + pr.height * 0.5f

        val bL  = bounds.x
        val bR  = bounds.x + bounds.width
        val bB  = bounds.y
        val bT  = bounds.y + bounds.height
        val bW  = bounds.width
        val bH  = bounds.height

        val margin = r * 2f
        if (cx + r < bL - margin || cx - r > bR + margin ||
            cy + r < bB - margin || cy - r > bT + margin) return

        val flipped     = player.isGravityFlipped()
        val rot         = normaliseRot()
        val scrollSpeed = player.getWorld()?.scrollSpeed ?: 320f

        val isFloor   = rot == 0   || rot == 270
        val isCeiling = rot == 90  || rot == 180

        val isClimbing   = (!flipped && rot == 0)   || (flipped && rot == 90)
        val isDescending = (!flipped && rot == 270) || (flipped && rot == 180)

        val isShip = player.getType() == AbstractPlayer.PlayerType.SHIP

        if (!isShip) {
            if ((!flipped && !isFloor) || (flipped && !isCeiling)) return
        }

        val lx1: Float; val ly1: Float
        val lx2: Float; val ly2: Float

        when (rot) {
            0   -> { lx1 = bL; ly1 = bB; lx2 = bR; ly2 = bT }
            90  -> { lx1 = bR; ly1 = bB; lx2 = bL; ly2 = bT }
            180 -> { lx1 = bR; ly1 = bT; lx2 = bL; ly2 = bB }
            else -> { lx1 = bL; ly1 = bT; lx2 = bR; ly2 = bB }
        }

        tmpEdge.set(lx2 - lx1, ly2 - ly1)
        val edgeLen = tmpEdge.len()

        tmpNormal.set(tmpEdge).nor()
        tmpNormal.rotate90(1)

        var nx = tmpNormal.x
        var ny = tmpNormal.y

        if (isCeiling && ny > 0) { nx = -nx; ny = -ny }
        else if (isFloor && ny < 0) { nx = -nx; ny = -ny }

        tmpCenter.set(cx, cy)
        tmpOrigin.set(lx1, ly1)
        val dist = tmpCenter.sub(tmpOrigin).dot(tmpNormal.set(nx, ny))

        val snapTolerance = when {
            isDescending -> r * 0.5f
            isClimbing && player.getCurrentSlopeRotation() != 0f -> r * 0.5f
            else -> 0f
        }

        if (dist >= r + snapTolerance) return
        if (dist < -r) return

        tmpCenter.set(cx - lx1, cy - ly1)
        val t = tmpCenter.dot(tmpEdge) / (edgeLen * edgeLen)
        if (t < -0.01f || t > 1.01f) return

        val targetVy    = -(scrollSpeed * nx) / ny
        val isFloorForPlayer = (!flipped && isFloor) || (flipped && isCeiling)

        if (isFloorForPlayer) {
            val jumpingOff = (!flipped && player.getVelocityY() > maxOf(0f, targetVy) + 1.5f) ||
                ( flipped && player.getVelocityY() < minOf(0f, targetVy) - 1.5f)
            if (jumpingOff) return
        }

        val pushOutY = (r - dist) / ny
        val offsetY  = cy - player.y
        player.setY(cy + pushOutY - offsetY)

        if (isFloorForPlayer) player.setGrounded(true)

        player.setCurrentSlopeRotation(if (isClimbing) 45f else -45f)

        when {
            isDescending -> player.setVelocityY(targetVy)
            isClimbing && !flipped && player.getVelocityY() <= targetVy -> player.setVelocityY(targetVy)
            isClimbing &&  flipped && player.getVelocityY() >= targetVy -> player.setVelocityY(targetVy)
        }
    }
}
