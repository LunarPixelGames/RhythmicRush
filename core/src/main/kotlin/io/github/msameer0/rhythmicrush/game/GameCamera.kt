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
    private var shouldSnap = false

    private var lastPlayerType: AbstractPlayer.PlayerType? = null
    private var shipLockedY = 810f
    
    private var boundaryCorridorBottom = 0f
    private var boundaryCorridorTop = 1080f
    private var isUsingCorridor = false

    fun reset() {
        isFirstUpdate = true
        lastPlayerType = null
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
        val currentType = player.getType()

        if (currentType != lastPlayerType) {
            // Only handle as a "transition" if we were already playing a different mode
            if (lastPlayerType != null) {
                if (currentType == AbstractPlayer.PlayerType.SHIP) {
                    val pBottom = player.lastPortalBottomY
                    val gY = world.groundY
                    val distToGround = pBottom - gY
                    
                    if (distToGround in 0f..400f) {
                        // Case 1: Near Ground (0-4 grids) - Use real ground with padding
                        shipLockedY = gY + 501f // 540 - 39 padding
                        isUsingCorridor = false
                    } else {
                        // Case 2: High Air - Use dynamic corridor centered on portal
                        shipLockedY = pBottom + 100f // Portal center (assuming 200px height)
                        boundaryCorridorBottom = pBottom - 400f // 4 grids below
                        boundaryCorridorTop = pBottom + 600f    // 4 grids above (200 portal + 400)
                        isUsingCorridor = true
                    }
                } else if (currentType == AbstractPlayer.PlayerType.CUBE) {
                    // When transitioning back to Cube, try to maintain current camera Y if possible
                    val idealBottom = camera.position.y - 250f
                    windowBottom = MathUtils.clamp(idealBottom, player.y + player.height - 500f, player.y)
                    cameraTargetY = windowBottom + 250f
                }
            } else {
                // First spawn: Initialize locked height just in case we start as Ship (checkpoints)
                if (currentType == AbstractPlayer.PlayerType.SHIP) {
                    shipLockedY = player.lastPortalBottomY + 100f // Default or from checkpoint
                }
            }
            lastPlayerType = currentType
        }

        if (currentType == AbstractPlayer.PlayerType.CUBE) {
            val paddingHeight = 500f

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
                camera.position.y = MathUtils.lerp(camera.position.y, cameraTargetY, min(delta * 6f, 1f))
            }
        } else if (currentType == AbstractPlayer.PlayerType.SHIP) {
            if (shouldSnap) {
                camera.position.y = shipLockedY
                shouldSnap = false
            }
            // Pan smoothly towards the locked height
            camera.position.y = MathUtils.lerp(camera.position.y, shipLockedY, min(delta * 6f, 1f))
        }

        camera.update()

        // Update world culling based on camera position
        val worldLeft = camera.position.x - camera.viewportWidth / 2f
        world.cullX = worldLeft
    }

    fun getCeilingY(): Float {
        if (lastPlayerType != AbstractPlayer.PlayerType.SHIP) return Float.MAX_VALUE
        if (isUsingCorridor) return boundaryCorridorTop
        return camera.position.y + 540f - 39f
    }

    fun getFloorY(): Float {
        if (lastPlayerType != AbstractPlayer.PlayerType.SHIP) return -Float.MAX_VALUE
        if (isUsingCorridor) return boundaryCorridorBottom
        return world.groundY
    }

    fun isUsingCorridor(): Boolean = isUsingCorridor
    fun getCorridorBottom(): Float = boundaryCorridorBottom
    fun getCorridorTop(): Float = boundaryCorridorTop

    fun getWindowBottom(): Float = windowBottom
    fun getPaddingHeight(): Float = 500f
}
