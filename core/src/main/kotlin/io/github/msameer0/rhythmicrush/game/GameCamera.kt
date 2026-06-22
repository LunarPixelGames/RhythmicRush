package io.github.msameer0.rhythmicrush.game

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import io.github.msameer0.rhythmicrush.GameConstants
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.exp

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
    private var shakeTime = 0f
    private var shakeOffsetX = 0f
    private var shakeOffsetY = 0f
    private var captureCameraLocked = false
    private var captureCameraX = 0f
    private var captureCameraY = 0f
    private var completionShakeStrength = 0f

    fun reset() {
        isFirstUpdate = true
        lastPlayer = null
        shouldSnap = true
        cameraTargetY = 540f
        windowBottom = 0f
        camera.position.y = 540f
        shakeTime = 0f
        shakeOffsetX = 0f
        shakeOffsetY = 0f
        captureCameraLocked = false
        completionShakeStrength = 0f
    }

    fun update(player: AbstractPlayer, delta: Float) {
        camera.position.x -= shakeOffsetX
        camera.position.y -= shakeOffsetY
        shakeOffsetX = 0f
        shakeOffsetY = 0f

        if (world.endCaptureActive) {
            if (!captureCameraLocked) {
                captureCameraLocked = true
                captureCameraX = camera.position.x
                captureCameraY = camera.position.y
            }
            camera.position.x = captureCameraX
            camera.position.y = captureCameraY
            world.endWallVisibleCenterY = captureCameraY
            world.endWallLockX =
                captureCameraX + camera.viewportWidth / 2f - GameConstants.Editor.GRID_SIZE

            shakeTime += delta
            val intensity = world.endCaptureShakeStrength * 24f
            shakeOffsetX = sin(shakeTime * 63f) * intensity
            shakeOffsetY = sin(shakeTime * 79f + 1.7f) * intensity
            camera.position.x += shakeOffsetX
            camera.position.y += shakeOffsetY
            camera.update()
            world.cullX = captureCameraX - camera.viewportWidth / 2f
            return
        }
        captureCameraLocked = false

        var targetX = player.x + CAMERA_X_OFFSET
        if (player.isMini()) targetX -= 25f
        camera.position.x = targetX

        if (player != lastPlayer) {
            if (lastPlayer != null) {
                player.onCameraModeEnter(camera.position.y, world.groundY)
                
                if (player.getCameraMode() == AbstractPlayer.CameraMode.FREE) {
                    val idealBottom = camera.position.y - 250f
                    windowBottom = MathUtils.clamp(idealBottom, player.y + player.height - 500f, player.y)
                    cameraTargetY = windowBottom + 250f
                }
            } else {
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

                if (player.y + player.height > windowTop) {
                    windowBottom = (player.y + player.height) - paddingHeight
                } else if (player.y < windowBottom) {
                    windowBottom = player.y
                }

                cameraTargetY = windowBottom + (paddingHeight / 2f)
                camera.position.y = MathUtils.lerp(camera.position.y, cameraTargetY, min(delta * GameConstants.Camera.SMOOTH_LERP, 1f))
            }
        } else {
            cameraTargetY = player.getRestrictedCameraY()
            if (shouldSnap) {
                camera.position.y = cameraTargetY
                shouldSnap = false
            }
            camera.position.y = MathUtils.lerp(camera.position.y, cameraTargetY, min(delta * GameConstants.Camera.SMOOTH_LERP, 1f))
        }

        world.endWallVisibleCenterY = camera.position.y
        world.endWallLockX =
            camera.position.x + camera.viewportWidth / 2f - GameConstants.Editor.GRID_SIZE

        camera.update()

        val worldLeft = camera.position.x - camera.viewportWidth / 2f
        world.cullX = worldLeft
    }

    fun getCeilingY(): Float = lastPlayer?.getCameraCeilingY() ?: Float.MAX_VALUE

    fun getFloorY(): Float = lastPlayer?.getCameraFloorY(world.groundY) ?: -Float.MAX_VALUE

    fun getWindowBottom(): Float = windowBottom
    fun getPaddingHeight(): Float = GameConstants.Camera.PADDING_HEIGHT

    fun clearShake() {
        camera.position.x -= shakeOffsetX
        camera.position.y -= shakeOffsetY
        shakeOffsetX = 0f
        shakeOffsetY = 0f
        camera.update()
    }

    fun beginCompletionShake() {
        clearShake()
        completionShakeStrength = world.endCaptureShakeStrength * 24f
    }

    fun updateCompletionShake(delta: Float) {
        clearShake()
        if (completionShakeStrength <= 0.05f) {
            completionShakeStrength = 0f
            return
        }

        shakeTime += delta
        completionShakeStrength *= exp(-2.15f * delta)
        shakeOffsetX = sin(shakeTime * 63f) * completionShakeStrength
        shakeOffsetY = sin(shakeTime * 79f + 1.7f) * completionShakeStrength
        camera.position.x += shakeOffsetX
        camera.position.y += shakeOffsetY
        camera.update()
    }
}
