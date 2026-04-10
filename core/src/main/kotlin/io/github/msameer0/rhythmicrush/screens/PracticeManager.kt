package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

class PracticeManager(private val world: GameWorld) {

    private class CheckpointState {
        var worldScrolled = 0f
        var playerX = 0f
        var playerWorldX = 0f
        var playerType: AbstractPlayer.PlayerType? = null
        var playerY = 0f
        var playerVY = 0f
        var gravityFlipped = false
        var mini = false
        var slopeRotation = 0f
        var triggerIdx = 0
        val baseBgColor = Color()
        val baseGroundColor = Color()
        val backgroundColor = Color()
        val groundColor = Color()
    }

    private val checkpoints = Array<CheckpointState>()

    fun placeCheckpoint() {
        val s = CheckpointState()
        s.worldScrolled = world.worldScrolled

        val p = world.player
        if (p != null) {
            s.playerX = p.x
            s.playerWorldX = p.worldX
            s.playerType = p.getType()
            s.playerY = p.y
            s.playerVY = p.velocityY
            s.gravityFlipped = p.isGravityFlipped()
            s.mini = p.isMini()
            s.slopeRotation = p.getCurrentSlopeRotation()
        }
        s.triggerIdx = world.triggerIdx

        s.baseBgColor.set(world.baseBgColor)
        s.baseGroundColor.set(world.baseGroundColor)
        s.backgroundColor.set(world.backgroundColor)
        s.groundColor.set(world.groundColor)

        checkpoints.add(s)
    }

    fun removeLastCheckpoint(): Boolean {
        if (checkpoints.size > 0) checkpoints.removeIndex(checkpoints.size - 1)
        return checkpoints.size > 0
    }

    fun hasCheckpoints(): Boolean {
        return checkpoints.size > 0
    }

    fun applyLatestCheckpoint(): Float {
        if (checkpoints.size == 0) return 0f
        val s = checkpoints.peek()

        world.fastForwardTo(s.worldScrolled)
        world.worldScrolled = s.worldScrolled
        world.baseBgColor = s.baseBgColor
        world.baseGroundColor = s.baseGroundColor
        world.backgroundColor = s.backgroundColor
        world.groundColor = s.groundColor
        world.triggerIdx = s.triggerIdx

        val p = world.obtainPlayer(
            if (s.playerType == AbstractPlayer.PlayerType.CUBE) "cube" else "ship"
        )
        p.init(s.playerX, s.playerY, s.playerVY, false)
        p.worldX = s.playerWorldX
        p.setGravityFlipped(s.gravityFlipped)
        p.setMini(s.mini)
        p.setCurrentSlopeRotation(s.slopeRotation)
        world.setPlayer(p)

        return s.worldScrolled / world.scrollSpeed
    }

    fun drawCheckpoints(shapes: ShapeRenderer, camera: OrthographicCamera) {
        if (checkpoints.size == 0) return

        val scroll = world.worldScrolled
        val hw = 12f
        val hh = 18f

        for (s in checkpoints) {
            val dx = s.playerX - (scroll - s.worldScrolled)
            val dy = s.playerY + 25f

            shapes.color = Color(0.1f, 0.4f, 0.1f, 0.8f)
            drawDiamond(shapes, dx, dy, hw + 2f, hh + 2f)

            shapes.color = Color(0.6f, 1.0f, 0.2f, 0.9f)
            drawDiamond(shapes, dx, dy, hw, hh)
        }
    }

    var btnSize = 0f
        private set

    var plusX = 0f
        private set
    var plusY = 0f
        private set
    var minusX = 0f
        private set
    var minusY = 0f
        private set

    fun updateButtonCoords(
        camCX: Float, camBot: Float, uiPad: Float,
        uiScale: Float, btnSize: Float
    ) {
        this.btnSize = btnSize
        val spacing = 35f * uiScale
        val totalW = btnSize * 2 + spacing
        plusX = camCX - totalW / 2f
        plusY = camBot + uiPad
        minusX = plusX + btnSize + spacing
        minusY = plusY
    }

    fun drawButtonShapes(shapes: ShapeRenderer, opacity: Float) {
        shapes.setColor(0.15f, 0.15f, 0.15f, 0.6f * opacity)
        shapes.rect(plusX, plusY, btnSize, btnSize)
        shapes.rect(minusX, minusY, btnSize, btnSize)
    }

    fun hitsPlus(tx: Float, ty: Float): Boolean {
        return hits(tx, ty, plusX, plusY, btnSize, btnSize)
    }

    fun hitsMinus(tx: Float, ty: Float): Boolean {
        return hits(tx, ty, minusX, minusY, btnSize, btnSize)
    }

    companion object {
        private fun drawDiamond(shapes: ShapeRenderer, x: Float, y: Float, hw: Float, hh: Float) {
            shapes.triangle(x, y + hh, x - hw, y, x + hw, y)
            shapes.triangle(x, y - hh, x - hw, y, x + hw, y)
        }

        private fun hits(tx: Float, ty: Float, x: Float, y: Float, w: Float, h: Float): Boolean {
            return tx in x..(x + w) && ty in y..(y + h)
        }
    }
}
