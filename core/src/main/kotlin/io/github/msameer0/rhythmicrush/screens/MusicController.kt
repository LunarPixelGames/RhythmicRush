package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import kotlin.math.min
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.game.level.LevelData

class MusicController(
    private val game: RhythmicRushGame,
    private val levelData: LevelData?
) {

    companion object {
        private const val MUSIC_FADE_DURATION = 3f
    }

    var levelMusic: Music? = null
        private set

    var isFading: Boolean = false
        private set

    private var fadeTimer = 0f

    fun start() {
        start(0f)
    }

    fun start(startTime: Float) {
        val musicFile = levelData?.musicFile ?: return
        if (musicFile.isEmpty()) return

        try {
            var fh = Gdx.files.internal("musics/$musicFile")
            if (!fh.exists()) {
                fh = Gdx.files.local("assets/musics/$musicFile")
            }
            if (fh.exists()) {
                levelMusic = Gdx.audio.newMusic(fh)?.apply {
                    volume = game.settingsManager.musicVolume
                    isLooping = false
                    play()
                    if (startTime > 0f) position = startTime
                }
            }
        } catch (e: Exception) {
            Gdx.app.error("MusicController", "Could not load music: ${e.message}")
        }
    }

    fun pause() {
        levelMusic?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        levelMusic?.takeIf { !it.isPlaying }?.play()
    }

    fun stopAndDispose() {
        levelMusic?.let {
            if (it.isPlaying) it.stop()
            it.dispose()
        }
        levelMusic = null
        isFading = false
        fadeTimer = 0f
    }

    fun setVolume(volume: Float) {
        levelMusic?.volume = volume
    }

    fun applyFadeProgress(fadeProgress: Float) {
        levelMusic?.let {
            val baseVol = game.settingsManager.musicVolume
            it.volume = baseVol * (1f - min(fadeProgress, 1f))
        }
    }

    fun beginFade() {
        if (levelMusic != null) {
            isFading = true
            fadeTimer = 0f
        }
    }

    fun updateFade(delta: Float): Boolean {
        if (!isFading || levelMusic == null) return false
        fadeTimer += delta
        val base = game.settingsManager.musicVolume
        val vol = base * (1f - min(fadeTimer / MUSIC_FADE_DURATION, 1f))
        levelMusic?.volume = vol
        if (fadeTimer >= MUSIC_FADE_DURATION) {
            stopAndDispose()
            return true
        }
        return false
    }

    val isPlaying: Boolean
        get() = levelMusic?.isPlaying == true

    fun getMusic(): Music? = levelMusic
}
