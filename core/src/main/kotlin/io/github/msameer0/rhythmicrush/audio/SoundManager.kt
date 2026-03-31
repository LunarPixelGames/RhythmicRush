package io.github.msameer0.rhythmicrush.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import kotlin.math.max
import kotlin.math.min

/**
 * The `SoundManager` class is responsible for managing audio playback within the application.
 * It handles the loading, playback, and lifecycle of background music and provides centralized
 */
class SoundManager {
    private var musicVolume = 1f

    /**
     * Gets the current volume level for sound effects.
     *
     * @return the sound effects volume as a float between 0.0 and 1.0
     */
    var sfxVolume: Float = 1f
        /**
         * Sets the volume level for sound effects.
         *
         *
         * The provided value is clamped between 0.0 (silent) and 1.0 (maximum volume).
         *
         * @param volume the desired volume level for sound effects, clamped between 0.0f and 1.0f
         */
        set(volume) {
            field = max(0f, min(1f, volume))
        }

    private var menuMusic: Music? = null

    /**
     * Starts playing the menu background music.
     *
     *
     * If the music instance has not been created yet, it loads the audio file from the internal assets,
     * configures it to loop, and applies the current music volume. If the music is already
     * loaded but paused or stopped, it resumes playback.
     */
    fun playMenuMusic() {
        if (menuMusic == null) {
            Gdx.app.log("SoundManager", "Loading menu music...")
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("musics/954091_vulg.mp3"))
            menuMusic!!.setLooping(true)
            menuMusic!!.setVolume(musicVolume)
            Gdx.app.log("SoundManager", "Menu music loaded.")
        }

        if (!menuMusic!!.isPlaying()) {
            menuMusic!!.play()
        }
    }

    /**
     * Pauses the menu background music if it is currently playing.
     *
     *
     * This method checks if the music instance exists and is active before
     * invoking the pause command.
     */
    fun pauseMenuMusic() {
        if (menuMusic != null && menuMusic!!.isPlaying()) {
            menuMusic!!.pause()
        }
    }

    /**
     * Stops the playback of the menu background music.
     *
     *
     * If the music instance has been initialized, this method stops the playback
     * and resets the position to the beginning of the audio file.
     */
    fun stopMenuMusic() {
        if (menuMusic != null) {
            menuMusic!!.stop()
        }
    }

    /**
     * Gets the current volume level for background music.
     *
     * @return the music volume as a float between 0.0 and 1.0
     */
    fun getMusicVolume(): Float {
        return musicVolume
    }

    /**
     * Sets the volume level for background music.
     *
     *
     * the provided value is clamped between 0.0 (silent) and 1.0 (maximum volume).
     * If the menu music is currently loaded, its volume is updated immediately.
     *
     * @param volume the desired volume level, clamped between 0.0f and 1.0f
     */
    fun setMusicVolume(volume: Float) {
        musicVolume = max(0f, min(1f, volume))
        if (menuMusic != null) menuMusic!!.setVolume(musicVolume)
    }

    fun dispose() {
        if (menuMusic != null) {
            Gdx.app.log("SoundManager", "Disposing menu music...")
            menuMusic!!.stop()
            menuMusic!!.dispose()
            menuMusic = null
            Gdx.app.log("SoundManager", "Menu music disposed.")
        }
    }
}
