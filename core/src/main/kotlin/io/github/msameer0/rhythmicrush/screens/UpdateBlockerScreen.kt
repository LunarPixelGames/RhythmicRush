package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.MathUtils
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.font.FontManager

/**
 * Full-screen blocker shown when a mandatory update is required or in progress.
 * Prevents user from accessing any game functionality until update is complete.
 *
 * This screen:
 * - Shows prominent update status message
 * - Blocks all input/navigation
 * - Auto-refreshes state to detect when update completes
 * - Forces restart/app close if update becomes mandatory
 */
class UpdateBlockerScreen(private val game: RhythmicRushGame) : Screen {

    private lateinit var font: BitmapFont
    private lateinit var smallFont: BitmapFont
    private val layout = GlyphLayout()
    private var elapsedTime = 0f
    private var checkUpdateStateIntervalSeconds = 1f
    private var lastCheckTime = 0f

    override fun show() {
        font = game.fontManager.getTitle(FontManager.SIZE_XLARGE)
        smallFont = game.fontManager.getBody(FontManager.SIZE_MEDIUM)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        elapsedTime += delta
        lastCheckTime += delta

        // Check update state periodically
        if (lastCheckTime >= checkUpdateStateIntervalSeconds) {
            lastCheckTime = 0f
            // Check if update is no longer pending - if so, go back to main menu
            if (!game.updateManager.isUpdatePending() && !game.updateManager.isUpdateAvailable()) {
                Gdx.app.log("UpdateBlocker", "Update completed, returning to main menu")
                game.setScreen(MainMenuScreen(game))
                return
            }
        }

        game.batch.begin()

        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()

        // Title
        val title = "Update Required"
        layout.setText(font, title)
        font.draw(
            game.batch,
            layout,
            screenWidth / 2 - layout.width / 2,
            screenHeight - 150f
        )

        // Pulsing message
        val messageSuffix = when ((elapsedTime * 2).toInt() % 4) {
            0 -> "."
            1 -> ".."
            2 -> "..."
            else -> ""
        }
        val message = "Updating your game$messageSuffix"
        layout.setText(smallFont, message)
        smallFont.draw(
            game.batch,
            layout,
            screenWidth / 2 - layout.width / 2,
            screenHeight / 2 + 50f
        )

        // Description
        val description = "Please do not close the app\n\nYour game will restart when ready"
        layout.setText(smallFont, description)
        smallFont.draw(
            game.batch,
            layout,
            screenWidth / 2 - layout.width / 2,
            screenHeight / 2 - 80f
        )

        // Progress bar
        drawProgressBar(
            screenWidth / 2 - 150f,
            screenHeight / 2 - 150f,
            300f,
            30f,
            0.6f + 0.4f * MathUtils.sin(elapsedTime * 3)
        )

        game.batch.end()
    }

    /**
     * Draw a simple animated progress bar.
     */
    private fun drawProgressBar(x: Float, y: Float, width: Float, height: Float, progress: Float) {
        val progressWidth = width * progress.coerceIn(0f, 1f)

        // Background
        game.batch.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
        val shapeRenderer = com.badlogic.gdx.graphics.glutils.ShapeRenderer()
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = com.badlogic.gdx.graphics.Color.DARK_GRAY
        shapeRenderer.rect(x, y, width, height)

        // Progress fill
        shapeRenderer.color = com.badlogic.gdx.graphics.Color(0.2f, 0.8f, 1f, 1f)
        shapeRenderer.rect(x, y, progressWidth, height)

        shapeRenderer.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.batch.begin()
    }

    override fun resize(width: Int, height: Int) {}

    override fun pause() {}

    override fun resume() {}

    override fun hide() {}

    override fun dispose() {}
}

