package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.msameer0.rhythmicrush.GameConstants
import io.github.msameer0.rhythmicrush.RhythmicRushGame

/**
 * Base class for all game screens, providing common camera, viewport, and rendering abstractions.
 */
abstract class AbstractScreen(protected val game: RhythmicRushGame) : Screen {

    @JvmField
    protected val camera: OrthographicCamera = OrthographicCamera()

    @JvmField
    protected val viewport: Viewport = ExtendViewport(1920f, 1080f, camera)

    init {
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        viewport.apply()
    }

    override fun render(delta: Float) {
        // Cap delta to prevent huge spikes (e.g. during resize/fullscreen switch) 
        // from snapping animations instantly.
        val cappedDelta = kotlin.math.min(delta, GameConstants.World.DELTA_CAP)
        handleWindowKeys()
        update(cappedDelta)
        draw()
    }

    private fun handleWindowKeys() {
        val wc = game.windowController ?: return
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) wc.toggleFullscreen()
    }

    protected abstract fun update(delta: Float)

    protected abstract fun draw()

    override fun show() {
        if (game.settingsManager.menuMusicEnabled) {
            game.soundManager.playMenuMusic()
        }
    }

    override fun resize(width: Int, height: Int) {
        var newWidth = width
        var newHeight = height
        val wc = game.windowController
        if (wc != null) {
            newWidth = Gdx.graphics.width
            newHeight = Gdx.graphics.height
        }
        viewport.update(newWidth, newHeight, true)
    }

    override fun hide() {}

    override fun pause() {}

    override fun resume() {}

    override fun dispose() {}

    protected fun drawTextWithShadow(
        font: BitmapFont,
        text: CharSequence,
        x: Float,
        y: Float,
        mainColor: Color
    ) {
        val shadowOffset = 2f
        font.color = Color(0f, 0f, 0f, mainColor.a * 0.4f)
        font.draw(game.batch, text, x + shadowOffset, y - shadowOffset)
        font.color = mainColor
        font.draw(game.batch, text, x, y)
    }
}
