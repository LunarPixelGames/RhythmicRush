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

    @Override
    public void maximizeWindow() {
        if (Gdx.graphics instanceof Lwjgl3Graphics) {
            ((Lwjgl3Graphics) Gdx.graphics).getWindow().maximizeWindow();
        }
    }

    @Override
    public void enforceAspectRatio(int width, int height) {
        if (Gdx.graphics.isFullscreen()) return; // don't enforce in fullscreen
        int targetHeight = width * 9 / 16;
        if (Math.abs(height - targetHeight) > 2) { // small tolerance to avoid loop
            Gdx.graphics.setWindowedMode(width, targetHeight);
        }
    }
}
