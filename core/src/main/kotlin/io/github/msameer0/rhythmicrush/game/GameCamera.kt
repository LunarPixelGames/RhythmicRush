package io.github.msameer0.rhythmicrush.game

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import io.github.msameer0.rhythmicrush.GameConstants
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import kotlin.math.min

/**
 * Handles the game camera's position and movement logic, including player following
 * and gamemode-specific behaviors like the Cube's 500px panning window.
 */
class GameCamera(val camera: OrthographicCamera, private val world: GameWorld) {

    companion object {
        private const val CAMERA_X_OFFSET = GameConstants.Camera.X_OFFSET
    }

    private var cameraTargetY = 540f
    private var windowBottom = 0f
    private var isFirstUpdate = true
    private var shouldSnap = false

    private var lastPlayer: AbstractPlayer? = null

    fun reset() {
        isFirstUpdate = true
        lastPlayer = null
        shouldSnap = true
        cameraTargetY = 540f
        windowBottom = 0f
        camera.position.y = 540f
    }

    fun update(player: AbstractPlayer, delta: Float) {
        // Horizontal Movement (X-axis follow)
        var targetX = player.x + CAMERA_X_OFFSET
        if (player.isMini()) targetX -= 25f
        camera.position.x = targetX

        // Vertical Movement (Y-axis logic)
        if (player != lastPlayer) {
            // Only handle as a "transition" if we were already playing a different mode
            if (lastPlayer != null) {
                player.onCameraModeEnter(camera.position.y, world.groundY)
                
                if (player.getCameraMode() == AbstractPlayer.CameraMode.FREE) {
                    // When transitioning back to a FREE mode, try to maintain current camera Y if possible
                    val idealBottom = camera.position.y - 250f
                    windowBottom = MathUtils.clamp(idealBottom, player.y + player.height - 500f, player.y)
                    cameraTargetY = windowBottom + 250f
                }
            } else {
                // First spawn: Initialize camera mode
                player.onCameraModeEnter(camera.position.y, world.groundY)
            }
            lastPlayer = player
        }

        if (player.getCameraMode() == AbstractPlayer.CameraMode.FREE) {
            val paddingHeight = GameConstants.Camera.PADDING_HEIGHT

            if (isFirstUpdate) {
                windowBottom = player.y
                cameraTargetY = windowBottom + (paddingHeight / 2f)
                if (shouldSnap) {
                    camera.position.y = cameraTargetY
                    shouldSnap = false
                }
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
                camera.position.y = MathUtils.lerp(camera.position.y, cameraTargetY, min(delta * GameConstants.Camera.SMOOTH_LERP, 1f))
            }
        } else {
            cameraTargetY = player.getRestrictedCameraY()
            if (shouldSnap) {
                camera.position.y = cameraTargetY
                shouldSnap = false
            }
            // Pan smoothly towards the locked height
            camera.position.y = MathUtils.lerp(camera.position.y, cameraTargetY, min(delta * GameConstants.Camera.SMOOTH_LERP, 1f))
        }

        camera.update()

        // Update world culling based on camera position
        val worldLeft = camera.position.x - camera.viewportWidth / 2f
        world.cullX = worldLeft
    }

    fun getCeilingY(): Float = lastPlayer?.getCameraCeilingY() ?: Float.MAX_VALUE

    fun getFloorY(): Float = lastPlayer?.getCameraFloorY(world.groundY) ?: -Float.MAX_VALUE

    fun getWindowBottom(): Float = windowBottom
    fun getPaddingHeight(): Float = GameConstants.Camera.PADDING_HEIGHT
}
