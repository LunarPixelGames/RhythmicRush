package io.github.msameer0.rhythmicrush;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.msameer0.rhythmicrush.audio.SoundManager;
import io.github.msameer0.rhythmicrush.game.level.ProgressManager;
import io.github.msameer0.rhythmicrush.screens.MainMenuScreen;

public class RhythmicRushGame extends Game {

    private SpriteBatch      batch;
    private SoundManager     soundManager;
    private AtlasManager     atlasManager;
    private FontManager      fontManager;
    private WindowController windowController;
    private ProgressManager  progressManager;
    private SettingsManager  settingsManager;

    @Override
    public void create() {
        batch           = new SpriteBatch();
        atlasManager    = new AtlasManager();
        settingsManager = new SettingsManager();
        // Generate all fonts once here — never blocks a screen transition
        fontManager     = new FontManager();
        soundManager    = new SoundManager();
        progressManager = new ProgressManager();

        soundManager.setMusicVolume(settingsManager.musicVolume);
        settingsManager.applyFpsCap();
        settingsManager.applyVsync();

        if (settingsManager.menuMusicEnabled) soundManager.playMenuMusic();
        setScreen(new MainMenuScreen(this));
    }

    public SpriteBatch      getBatch()            { return batch; }
    public SoundManager     getSoundManager()     { return soundManager; }
    public AtlasManager     getAtlasManager()     { return atlasManager; }
    public FontManager      getFontManager()      { return fontManager; }
    public WindowController getWindowController() { return windowController; }
    public ProgressManager  getProgressManager()  { return progressManager; }
    public SettingsManager  getSettingsManager()  { return settingsManager; }

    public void setWindowController(WindowController wc) { this.windowController = wc; }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        soundManager.dispose();
        atlasManager.dispose();
        fontManager.dispose();
    }
}
