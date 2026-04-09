package io.github.msameer0.rhythmicrush.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import kotlin.math.max
import kotlin.math.min

class SoundManager {
    private var musicVolume = 1f

    var sfxVolume: Float = 1f
        set(volume) {
            field = max(0f, min(1f, volume))
        }

    private var menuMusic: Music? = null

    fun playMenuMusic() {
        if (menuMusic == null) {
            Gdx.app.log("SoundManager", "Loading menu music...")
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("musics/954091_vulg.mp3"))
            menuMusic!!.isLooping = true
            menuMusic!!.volume = musicVolume
            Gdx.app.log("SoundManager", "Menu music loaded.")
        }

        if (!menuMusic!!.isPlaying) {
            menuMusic!!.play()
        }
    }

    fun pauseMenuMusic() {
        if (menuMusic != null && menuMusic!!.isPlaying) {
            menuMusic!!.pause()
        }
    }

    fun stopMenuMusic() {
        if (menuMusic != null) {
            menuMusic!!.stop()
        }
    }

    fun getMusicVolume(): Float {
        return musicVolume
    }

    fun setMusicVolume(volume: Float) {
        musicVolume = max(0f, min(1f, volume))
        if (menuMusic != null) menuMusic!!.volume = musicVolume
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
