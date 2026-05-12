package io.github.msameer0.rhythmicrush.game

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import kotlin.math.min

/**
 * Handles the game camera's position and movement logic, including player following
 * and gamemode-specific behaviors like the Cube's 500px panning window.
 */
class GameCamera(val camera: OrthographicCamera, private val world: GameWorld) {

    companion object {
        private const val CAMERA_X_OFFSET = 307f
    }

    private var cameraTargetY = 540f
    private var windowBottom = 0f
    private var isFirstUpdate = true

    private var lastPlayerType: AbstractPlayer.PlayerType? = null
    private var shipLockedY = 810f

    fun reset() {
        isFirstUpdate = true
        lastPlayerType = null
    }

    fun update(player: AbstractPlayer, delta: Float) {
        // Horizontal Movement (X-axis follow)
        var targetX = player.x + CAMERA_X_OFFSET
        if (player.isMini()) targetX -= 25f
        camera.position.x = targetX

        // Vertical Movement (Y-axis logic)
        val currentType = player.getType()

        if (currentType != lastPlayerType) {
            if (currentType == AbstractPlayer.PlayerType.SHIP) {
                // Centered on portal, but max 810 to keep ground visible at 34px
                shipLockedY = Math.max(player.y, 810f)
            } else if (currentType == AbstractPlayer.PlayerType.CUBE) {
                isFirstUpdate = true
            }
            lastPlayerType = currentType
        }

        if (currentType == AbstractPlayer.PlayerType.CUBE) {
            val paddingHeight = 500f

            if (isFirstUpdate) {
                windowBottom = player.y
                cameraTargetY = windowBottom + (paddingHeight / 2f)
                camera.position.y = cameraTargetY
                isFirstUpdate = false
            } else {
                val windowTop = windowBottom + paddingHeight

                // Adjust window if player moves outside (only by the amount they moved out)
                if (player.y + player.height > windowTop) {
                    windowBottom = (player.y + player.height) - paddingHeight
                } else if (player.y < windowBottom) {
                    windowBottom = player.y
                }

                cameraTargetY = windowBottom + (paddingHeight / 2f)
                // Pan smoothly towards the window center
                camera.position.y = MathUtils.lerp(camera.position.y, cameraTargetY, min(delta * 6f, 1f))
            }
        } else if (currentType == AbstractPlayer.PlayerType.SHIP) {
            // Pan smoothly towards the locked height
            camera.position.y = MathUtils.lerp(camera.position.y, shipLockedY, min(delta * 4f, 1f))
        }

        camera.update()

        // Update world culling based on camera position
        val worldLeft = camera.position.x - camera.viewportWidth / 2f
        world.cullX = worldLeft
    }

    fun getCeilingY(): Float {
        if (lastPlayerType == AbstractPlayer.PlayerType.SHIP) {
            return camera.position.y + 540f - 39f
        }
        return Float.MAX_VALUE
    }

    fun getFloorY(): Float {
        if (lastPlayerType == AbstractPlayer.PlayerType.SHIP) {
            return camera.position.y - 540f + 39f
        }
        return -Float.MAX_VALUE
    }
}
