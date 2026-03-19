package io.github.msameer0.rhythmicrush.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * The {@code SoundManager} class is responsible for managing audio playback within the application.
 * It handles the loading, playback, and lifecycle of background music and provides centralized
 */
public class SoundManager {

    private float musicVolume = 1f;
    private float sfxVolume = 1f;

    private Music menuMusic;

    public SoundManager() {
    }

    /**
     * Starts playing the menu background music.
     * <p>
     * If the music instance has not been created yet, it loads the audio file from the internal assets,
     * configures it to loop, and applies the current music volume. If the music is already
     * loaded but paused or stopped, it resumes playback.
     */
    public void playMenuMusic() {
        if (menuMusic == null) {
            Gdx.app.log("SoundManager", "Loading menu music...");
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("musics/954091_vulg.mp3"));
            menuMusic.setLooping(true);
            menuMusic.setVolume(musicVolume);
            Gdx.app.log("SoundManager", "Menu music loaded.");
        }

        if (!menuMusic.isPlaying()) {
            menuMusic.play();
        }
    }

    /**
     * Pauses the menu background music if it is currently playing.
     * <p>
     * This method checks if the music instance exists and is active before
     * invoking the pause command.
     */
    public void pauseMenuMusic() {
        if (menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.pause();
        }
    }

    /**
     * Stops the playback of the menu background music.
     * <p>
     * If the music instance has been initialized, this method stops the playback
     * and resets the position to the beginning of the audio file.
     */
    public void stopMenuMusic() {
        if (menuMusic != null) {
            menuMusic.stop();
        }
    }

    /**
     * Gets the current volume level for background music.
     *
     * @return the music volume as a float between 0.0 and 1.0
     */
    public float getMusicVolume() {
        return musicVolume;
    }

    /**
     * Sets the volume level for background music.
     * <p>
     * the provided value is clamped between 0.0 (silent) and 1.0 (maximum volume).
     * If the menu music is currently loaded, its volume is updated immediately.
     *
     * @param volume the desired volume level, clamped between 0.0f and 1.0f
     */
    public void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        if (menuMusic != null) menuMusic.setVolume(musicVolume);
    }

    /**
     * Gets the current volume level for sound effects.
     *
     * @return the sound effects volume as a float between 0.0 and 1.0
     */
    public float getSfxVolume() {
        return sfxVolume;
    }

    /**
     * Sets the volume level for sound effects.
     * <p>
     * The provided value is clamped between 0.0 (silent) and 1.0 (maximum volume).
     *
     * @param volume the desired volume level for sound effects, clamped between 0.0f and 1.0f
     */
    public void setSfxVolume(float volume) {
        sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    public void dispose() {
        if (menuMusic != null) {
            Gdx.app.log("SoundManager", "Disposing menu music...");
            menuMusic.stop();
            menuMusic.dispose();
            menuMusic = null;
            Gdx.app.log("SoundManager", "Menu music disposed.");
        }
    }
}
