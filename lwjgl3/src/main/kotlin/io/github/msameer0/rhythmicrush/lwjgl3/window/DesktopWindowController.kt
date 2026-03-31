package io.github.msameer0.rhythmicrush.lwjgl3.window

import com.badlogic.gdx.Gdx
import io.github.msameer0.rhythmicrush.window.WindowController

class DesktopWindowController : WindowController{
    companion object {
        private const val BASE_WIDTH = 1280
        private const val BASE_HEIGHT = 720
    }

    override fun toggleFullscreen() {
        if (Gdx.graphics.isFullscreen) {
            Gdx.graphics.setWindowedMode(
                BASE_WIDTH,
                BASE_HEIGHT
            )
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        }
    }
}
