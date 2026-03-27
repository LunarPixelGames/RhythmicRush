package io.github.msameer0.rhythmicrush;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.msameer0.rhythmicrush.ads.AdController;
import io.github.msameer0.rhythmicrush.atlas.AtlasManager;
import io.github.msameer0.rhythmicrush.audio.SoundManager;
import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.game.level.LevelManager;
import io.github.msameer0.rhythmicrush.game.level.ProgressManager;
import io.github.msameer0.rhythmicrush.game.registries.Registries;
import io.github.msameer0.rhythmicrush.screens.MainMenuScreen;
import io.github.msameer0.rhythmicrush.settings.SettingsManager;
import io.github.msameer0.rhythmicrush.window.WindowController;

/**
 * The main entry point and central controller for the RhythmicRush game.
 * <p>
 * This class extends the {@link Game} framework and is responsible for:
 * <ul>
 *     <li>Initializing and managing core systems such as audio, assets, fonts, and settings.</li>
 *     <li>Maintaining a global {@link SpriteBatch} for efficient rendering across screens.</li>
 *     <li>Coordinating screen transitions, starting with the {@link MainMenuScreen}.</li>
 *     <li>Handling application lifecycle events and ensuring proper resource disposal.</li>
 *     <li>Providing access to global managers (Progress, Settings, Sound, etc.) via getter methods.</li>
 * </ul>
 */
public class RhythmicRushGame extends Game {

    private SpriteBatch batch;
    private SoundManager soundManager;
    private AtlasManager atlasManager;
    private FontManager fontManager;
    private WindowController windowController;
    private ProgressManager progressManager;
    private LevelManager levelManager;
    private SettingsManager settingsManager;
    private final AdController adController;

    /**
     * Constructs the game with platform-specific implementations.
     * @param adController The platform-specific ad controller (e.g., AdMob for Android, Dummy for PC)
     */
    public RhythmicRushGame(AdController adController) {
        this.adController = adController;
    }

    /**
     * Initializes the game components, managers, and settings.
     * This method is called once when the application is created. It sets up the
     * sprite batch, managers for sound, atlas, fonts, settings, and progress,
     * applies configuration settings, and transitions to the main menu screen.
     */
    @Override
    public void create() {
        com.badlogic.gdx.Gdx.app.log("Game", "Initializing RhythmicRush...");
        Registries.init();
        batch = new SpriteBatch();
        atlasManager = new AtlasManager();
        settingsManager = new SettingsManager();
        fontManager = new FontManager();
        soundManager = new SoundManager();
        progressManager = new ProgressManager();
        levelManager = new LevelManager();

        soundManager.setMusicVolume(settingsManager.musicVolume);
        settingsManager.applyFpsCap();
        settingsManager.applyVsync();

        if (settingsManager.menuMusicEnabled) soundManager.playMenuMusic();

        com.badlogic.gdx.Gdx.app.log("Game", "Initialization complete. Entering Main Menu.");
        setScreen(new MainMenuScreen(this));
    }

    /**
     * Gets the {@link SpriteBatch} used for rendering the game's graphics.
     *
     * @return the {@link SpriteBatch} instance used for drawing
     */
    public SpriteBatch getBatch() {
        return batch;
    }

    /**
     * Retrieves the level manager responsible for loading and caching level data.
     *
     * @return the {@link LevelManager} instance for the game
     */
    public LevelManager getLevelManager() {
        return levelManager;
    }

    /**
     * Retrieves the sound manager responsible for handling game audio, including music and sound effects.
     *
     * @return the {@link SoundManager} instance for the game
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }

    /**
     * Retrieves the ad controller used to display interstitial ads.
     *
     * @return the {@link AdController} instance for the game
     */
    public AdController getAdController() {
        return adController;
    }

    /**
     * Retrieves the atlas manager used for loading and managing texture atlases.
     *
     * @return the {@link AtlasManager} instance for the game
     */
    public AtlasManager getAtlasManager() {
        return atlasManager;
    }

    /**
     * Retrieves the font manager used to handle game fonts and text rendering.
     *
     * @return the {@link FontManager} instance for the game
     */
    public FontManager getFontManager() {
        return fontManager;
    }

    /**
     * Retrieves the window controller used to manage window-related operations.
     *
     * @return the {@link WindowController} instance for the game
     */
    public WindowController getWindowController() {
        return windowController;
    }

    /**
     * Retrieves the progress manager used to track and manage level progress and player achievements.
     *
     * @return the {@link ProgressManager} instance for the game
     */
    public ProgressManager getProgressManager() {
        return progressManager;
    }

    /**
     * Retrieves the settings manager used to handle game configurations and preferences.
     *
     * @return the {@link SettingsManager} instance for the game
     */
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    /**
     * Sets the window controller for the game.
     *
     * @param wc the {@link WindowController} instance used to manage window-related operations
     */
    public void setWindowController(WindowController wc) {
        this.windowController = wc;
    }

    /**
     * Releases all resources used by the game, including the sprite batch,
     * sound manager, atlas manager, and font manager.
     * This method is called when the application is destroyed to prevent memory leaks.
     */
    @Override
    public void dispose() {
        com.badlogic.gdx.Gdx.app.log("Game", "Disposing game resources...");
        super.dispose();
        batch.dispose();
        soundManager.dispose();
        atlasManager.dispose();
        fontManager.dispose();
        Gdx.app.log("Game", "Game disposed.");
    }
}
