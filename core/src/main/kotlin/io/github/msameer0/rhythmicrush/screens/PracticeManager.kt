package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

/**
 * Handles practice mode functionality, including checkpoint management and world state restoration.
 */
class PracticeManager(private val world: GameWorld) {
    private val checkpointOutlineColor = Color(0.1f, 0.4f, 0.1f, 0.8f)
    private val checkpointColor = Color(0.6f, 1.0f, 0.2f, 0.9f)

    /** Captures the world and player values restored by a practice checkpoint. */
    private class CheckpointState {
        var worldScrolled = 0f
        var playerX = 0f
        var playerWorldX = 0f
        var playerType: AbstractPlayer.PlayerType? = null
        var playerY = 0f
        var playerVelocityY = 0f
        var gravityFlipped = false
        var mini = false
        var slopeRotation = 0f
        var triggerIndex = 0
        val baseBgColor = Color()
        val baseGroundColor = Color()
        val backgroundColor = Color()
        val groundColor = Color()
    }

    private val checkpoints = Array<CheckpointState>()

    fun placeCheckpoint() {
        val checkpoint = CheckpointState()
        checkpoint.worldScrolled = world.worldScrolled

        val player = world.player
        if (player != null) {
            checkpoint.playerX = player.x
            checkpoint.playerWorldX = player.worldX
            checkpoint.playerType = player.getType()
            checkpoint.playerY = player.y
            checkpoint.playerVelocityY = player.velocityY
            checkpoint.gravityFlipped = player.isGravityFlipped()
            checkpoint.mini = player.isMini()
            checkpoint.slopeRotation = player.getCurrentSlopeRotation()
        }
        checkpoint.triggerIndex = world.triggerIdx

        checkpoint.baseBgColor.set(world.baseBgColor)
        checkpoint.baseGroundColor.set(world.baseGroundColor)
        checkpoint.backgroundColor.set(world.backgroundColor)
        checkpoint.groundColor.set(world.groundColor)

        checkpoints.add(checkpoint)
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
        val checkpoint = checkpoints.peek()

        world.fastForwardTo(checkpoint.worldScrolled)
        world.worldScrolled = checkpoint.worldScrolled
        world.baseBgColor = checkpoint.baseBgColor
        world.baseGroundColor = checkpoint.baseGroundColor
        world.backgroundColor = checkpoint.backgroundColor
        world.groundColor = checkpoint.groundColor
        world.triggerIdx = checkpoint.triggerIndex

        val player = world.obtainPlayer(
            if (checkpoint.playerType == AbstractPlayer.PlayerType.CUBE) "cube" else "ship"
        )
        player.init(
            checkpoint.playerX,
            checkpoint.playerY,
            checkpoint.playerVelocityY,
            false
        )
        player.worldX = checkpoint.playerWorldX
        player.setGravityFlipped(checkpoint.gravityFlipped)
        player.setMini(checkpoint.mini)
        player.setCurrentSlopeRotation(checkpoint.slopeRotation)
        world.setPlayer(player)

        return checkpoint.worldScrolled / world.scrollSpeed
    }

    fun drawCheckpoints(shapes: ShapeRenderer, camera: OrthographicCamera) {
        if (checkpoints.size == 0) return

        val currentScroll = world.worldScrolled
        val halfWidth = 12f
        val halfHeight = 18f

        for (checkpoint in checkpoints) {
            val drawX =
                checkpoint.playerX - (currentScroll - checkpoint.worldScrolled)
            val drawY = checkpoint.playerY + 25f

            shapes.color = checkpointOutlineColor
            drawDiamond(shapes, drawX, drawY, halfWidth + 2f, halfHeight + 2f)

            shapes.color = checkpointColor
            drawDiamond(shapes, drawX, drawY, halfWidth, halfHeight)
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

    fun hitsPlus(touchX: Float, touchY: Float): Boolean {
        return hits(touchX, touchY, plusX, plusY, btnSize, btnSize)
    }

    fun hitsMinus(touchX: Float, touchY: Float): Boolean {
        return hits(touchX, touchY, minusX, minusY, btnSize, btnSize)
    }

    companion object {
        private fun drawDiamond(shapes: ShapeRenderer, x: Float, y: Float, hw: Float, hh: Float) {
            shapes.triangle(x, y + hh, x - hw, y, x + hw, y)
            shapes.triangle(x, y - hh, x - hw, y, x + hw, y)
        }

        private fun hits(
            touchX: Float,
            touchY: Float,
            x: Float,
            y: Float,
            width: Float,
            height: Float
        ): Boolean {
            return touchX in x..(x + width) && touchY in y..(y + height)
        }
    }
}
