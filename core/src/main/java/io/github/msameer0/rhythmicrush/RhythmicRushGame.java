package io.github.msameer0.rhythmicrush;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.msameer0.rhythmicrush.audio.SoundManager;
import io.github.msameer0.rhythmicrush.screens.MainMenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class RhythmicRushGame extends Game {
    private SpriteBatch batch;
    private SoundManager soundManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        soundManager = new SoundManager();

        soundManager.playMenuMusic();
        setScreen(new MainMenuScreen(this));
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        soundManager.dispose();
    }
}
