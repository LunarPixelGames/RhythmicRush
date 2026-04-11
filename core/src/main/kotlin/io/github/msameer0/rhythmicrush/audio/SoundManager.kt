package io.github.msameer0.rhythmicrush.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.math.MathUtils

/**
 * Manages audio playback including menu music and sound effect volumes.
 */
class SoundManager {
    private var musicVolume = 1f

    var sfxVolume: Float = 1f
        set(volume) {
            field = MathUtils.clamp(volume, 0f, 1f)
        }

    private var menuMusic: Music? = null

    fun playMenuMusic() {
        if (menuMusic == null) {
            Gdx.app.log("SoundManager", "Loading menu music...")
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("musics/954091_vulg.mp3"))?.apply {
                isLooping = true
                volume = musicVolume
            }
            Gdx.app.log("SoundManager", "Menu music loaded.")
        }

        menuMusic?.let {
            if (!it.isPlaying) it.play()
        }
    }

    fun pauseMenuMusic() {
        menuMusic?.let {
            if (it.isPlaying) it.pause()
        }
    }

    fun stopMenuMusic() {
        menuMusic?.stop()
    }

    fun getMusicVolume(): Float {
        return musicVolume
    }

    fun setMusicVolume(volume: Float) {
        musicVolume = MathUtils.clamp(volume, 0f, 1f)
        menuMusic?.volume = musicVolume
    }

    fun dispose() {
        menuMusic?.let {
            Gdx.app.log("SoundManager", "Disposing menu music...")
            it.stop()
            it.dispose()
            menuMusic = null
            Gdx.app.log("SoundManager", "Menu music disposed.")
        }
    }
}
