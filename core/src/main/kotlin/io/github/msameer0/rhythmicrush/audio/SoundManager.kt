package io.github.msameer0.rhythmicrush.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils

/**
 * Manages audio playback including menu music and sound effect volumes.
 */
class SoundManager {
    private var musicVolume = 1f
    private var sfxVolume = 1f

    private var menuMusic: Music? = null
    private var deathSound: Sound? = null

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

    fun getSfxVolume(): Float {
        return sfxVolume
    }

    fun setSfxVolume(volume: Float) {
        sfxVolume = MathUtils.clamp(volume, 0f, 1f)
    }

    fun playDeathSound() {
        val sound = deathSound ?: loadDeathSound() ?: return
        sound.play(sfxVolume)
    }

    private fun loadDeathSound(): Sound? {
        if (deathSound == null) {
            val file = Gdx.files.internal("sfx/death.wav")
            if (file.exists()) {
                deathSound = Gdx.audio.newSound(file)
            }
        }
        return deathSound
    }

    fun dispose() {
        menuMusic?.let {
            Gdx.app.log("SoundManager", "Disposing menu music...")
            it.stop()
            it.dispose()
            menuMusic = null
            Gdx.app.log("SoundManager", "Menu music disposed.")
        }
        deathSound?.dispose()
        deathSound = null
    }
}
