package io.github.msameer0.rhythmicrush.window;

/**
 * Interface for managing window states and behaviors within the application.
 * Provides methods to control the display mode, sizing, and constraints of the game window.
 */
public interface WindowController {
    /**
     * Toggles the window between fullscreen and windowed mode.
     */
    void toggleFullscreen();
    /**
     * Maximizes the application window to occupy the maximum available screen area.
     */
    void maximizeWindow();
    /**
     * Enforces a specific aspect ratio for the window based on the provided width and height.
     * This prevents the window from being resized to dimensions that do not match the ratio.
     *
     * @param width  the horizontal component of the aspect ratio
     * @param height the vertical component of the aspect ratio
     */
    void enforceAspectRatio(int width, int height);
}
