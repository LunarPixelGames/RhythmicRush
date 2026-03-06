package io.github.msameer0.rhythmicrush;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.msameer0.rhythmicrush.audio.SoundManager;
import io.github.msameer0.rhythmicrush.screens.MainMenuScreen;

public class RhythmicRushGame extends Game {

    private SpriteBatch  batch;
    private SoundManager soundManager;
    private AtlasManager atlasManager;

    @Override
    public void create() {
        batch        = new SpriteBatch();
        atlasManager = new AtlasManager();
        soundManager = new SoundManager();
        soundManager.playMenuMusic();
        setScreen(new MainMenuScreen(this));
    }

    public SpriteBatch  getBatch()        { return batch; }
    public SoundManager getSoundManager() { return soundManager; }
    public AtlasManager getAtlasManager() { return atlasManager; }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        soundManager.dispose();
        atlasManager.dispose();
    }
}
