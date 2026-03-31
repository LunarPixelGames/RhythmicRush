package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.window.WindowController;

/**
 * An abstract base class for all game screens in Rhythmic Rush.
 * This class implements the {@link Screen} interface and provides common functionality
 * for camera management, viewport handling, and global input shortcuts such as
 * fullscreen and window maximization toggles.
 *
 * <p>Subclasses are required to implement {@link #update(float)} and {@link #draw()}
 * to ensure a clean separation between game logic and rendering.</p>
 */
public abstract class AbstractScreen implements Screen {

    protected final RhythmicRushGame game;
    protected OrthographicCamera camera;
    protected Viewport viewport;

    /**
     * Constructs a new AbstractScreen, initializing the camera and viewport.
     * <p>
     * This constructor sets up an {@link OrthographicCamera} and an {@link ExtendViewport}
     * with a base virtual resolution of 800x480. It immediately updates and applies
     * the viewport to match the current screen dimensions.
     * </p>
     *
     * @param game the main game instance used for accessing global services
     */
    public AbstractScreen(RhythmicRushGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(800, 480, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        viewport.apply();
    }

    /**
     * Called by the game loop to render the screen.
     * <p>
     * This method orchestrates the frame's execution by checking for global window
     * input shortcuts, updating the game logic via {@link #update(float)}, and
     * rendering the frame via {@link #draw()}.
     * </p>
     *
     * @param delta the time in seconds since the last render
     */
    @Override
    public void render(float delta) {
        handleWindowKeys();
        update(delta);
        draw();
    }

    /**
     * Handles global window-related keyboard shortcuts.
     * <p>
     * Specifically, it checks for the following key presses:
     * <ul>
     *   <li>{@link Input.Keys#F11}: Toggles fullscreen mode.</li>
     *   <li>{@link Input.Keys#F10}: Maximizes the game window.</li>
     * </ul>
     * This method is called during every render frame to ensure responsive window control.
     */
    private void handleWindowKeys() {
        WindowController wc = game.getWindowController();
        if (wc == null) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) wc.toggleFullscreen();
    }

    protected abstract void update(float delta);

    protected abstract void draw();

    /**
     * Called when this screen becomes the current screen for the game.
     * <p>
     * This implementation automatically starts playing the menu music via the
     * game's sound manager.
     * </p>
     */
    @Override
    public void show() {
        if(game.getSettingsManager().menuMusicEnabled)
            game.getSoundManager().playMenuMusic();
    }

    /**
     * Resizes the screen and updates the viewport.
     * <p>
     * If a {@link WindowController} is available, it is used to enforce the aspect ratio
     * constraints. The viewport is then updated with the final dimensions, centering
     */
    @Override
    public void resize(int width, int height) {
        WindowController wc = game.getWindowController();
        if (wc != null) {
            width = Gdx.graphics.getWidth();
            height = Gdx.graphics.getHeight();
        }
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
    }

    protected void drawTextWithShadow(BitmapFont font, CharSequence text, float x, float y, Color mainColor) {
        final float shadowOffset = 2f;
        font.setColor(0, 0, 0, mainColor.a * 0.4f);
        font.draw(game.getBatch(), text, x + shadowOffset, y - shadowOffset);
        font.setColor(mainColor);
        font.draw(game.getBatch(), text, x, y);
    }
}
