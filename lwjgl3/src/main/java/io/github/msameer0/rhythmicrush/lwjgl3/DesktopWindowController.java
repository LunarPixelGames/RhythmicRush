package io.github.msameer0.rhythmicrush.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import io.github.msameer0.rhythmicrush.window.WindowController;

public class DesktopWindowController implements WindowController {

    private static final int BASE_WIDTH  = 1280;
    private static final int BASE_HEIGHT = 720;

    @Override
    public void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(BASE_WIDTH, BASE_HEIGHT);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }
}
