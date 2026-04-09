package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.msameer0.rhythmicrush.RhythmicRushGame

/**
 * An abstract base class for all game screens in Rhythmic Rush.
 * This class implements the [Screen] interface and provides common functionality
 * for camera management, viewport handling, and global input shortcuts such as
 * fullscreen and window maximization toggles.
 *
 * Subclasses are required to implement [update] and [draw]
 * to ensure a clean separation between game logic and rendering.
 */
abstract class AbstractScreen(protected val game: RhythmicRushGame) : Screen {

    @JvmField
    protected val camera: OrthographicCamera = OrthographicCamera()

    @JvmField
    protected val viewport: Viewport = ExtendViewport(800f, 480f, camera)

    /**
     * Constructs a new AbstractScreen, initializing the camera and viewport.
     *
     * This constructor sets up an [OrthographicCamera] and an [ExtendViewport]
     * with a base virtual resolution of 800x480. It immediately updates and applies
     * the viewport to match the current screen dimensions.
     */
    init {
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        viewport.apply()
    }

    /**
     * Called by the game loop to render the screen.
     *
     * This method orchestrates the frame's execution by checking for global window
     * input shortcuts, updating the game logic via [update], and
     * rendering the frame via [draw].
     *
     * @param delta the time in seconds since the last render
     */
    override fun render(delta: Float) {
        handleWindowKeys()
        update(delta)
        draw()
    }

    /**
     * Handles global window-related keyboard shortcuts.
     *
     * Specifically, it checks for the following key presses:
     * - [Input.Keys.F11]: Toggles fullscreen mode.
     *
     * This method is called during every render frame to ensure responsive window control.
     */
    private fun handleWindowKeys() {
        val wc = game.windowController ?: return
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) wc.toggleFullscreen()
    }

    protected abstract fun update(delta: Float)

    protected abstract fun draw()

    /**
     * Called when this screen becomes the current screen for the game.
     *
     * This implementation automatically starts playing the menu music via the
     * game's sound manager.
     */
    override fun show() {
        if (game.settingsManager.menuMusicEnabled) {
            game.soundManager.playMenuMusic()
        }
    }

    /**
     * Resizes the screen and updates the viewport.
     *
     * If a [io.github.msameer0.rhythmicrush.window.WindowController] is available, it is used to enforce the aspect ratio
     * constraints. The viewport is then updated with the final dimensions, centering
     */
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
