package io.github.msameer0.rhythmicrush.window;

/**
 * Platform-agnostic window control.
 * Implemented by the lwjgl3 module, injected into RhythmicRushGame at launch.
 * Core and Android modules never import any lwjgl3 classes.
 */
public interface WindowController {
    void toggleFullscreen();
    void maximizeWindow();
    void enforceAspectRatio(int width, int height);
}
