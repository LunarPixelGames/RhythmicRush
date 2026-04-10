package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import kotlin.math.min
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.game.level.LevelData

/**
 * Handles all music playback, volume control, and fade logic for a gameplay session.
 *
 * Owns the lifecycle of the [Music] instance: loading, playing,
 * pausing, fading, and disposal. GameScreen delegates all audio concerns here.
 */
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

    /** Starts music from the beginning. */
    fun start() {
        start(0f)
    }

    /**
     * Starts music at a given time offset (used for practice checkpoint respawns).
     *
     * @param startTime Seconds into the track to begin playback from.
     */
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

    /** Pauses playback without disposing the track. */
    fun pause() {
        levelMusic?.takeIf { it.isPlaying }?.pause()
    }

    /** Resumes a paused track. */
    fun resume() {
        levelMusic?.takeIf { !it.isPlaying }?.play()
    }

    /** Stops and disposes the active track immediately. */
    fun stopAndDispose() {
        levelMusic?.let {
            if (it.isPlaying) it.stop()
            it.dispose()
        }
        levelMusic = null
        isFading = false
        fadeTimer = 0f
    }

    /** Applies a new volume level directly (e.g. from the pause slider). */
    fun setVolume(volume: Float) {
        levelMusic?.volume = volume
    }

    /**
     * Applies a partial fade toward silence based on external progress (0–1).
     * Used by the level-end sequence where GameScreen controls timing.
     *
     * @param fadeProgress 0 = full volume, 1 = silent.
     */
    fun applyFadeProgress(fadeProgress: Float) {
        levelMusic?.let {
            val baseVol = game.settingsManager.musicVolume
            it.volume = baseVol * (1f - min(fadeProgress, 1f))
        }
    }

    /**
     * Begins the full music-fade-then-stop sequence (used for music-fading on
     * death / exit). Call [updateFade] each frame while active.
     */
    fun beginFade() {
        if (levelMusic != null) {
            isFading = true
            fadeTimer = 0f
        }
    }

    /**
     * Updates the fade timer. Returns `true` when the fade has finished
     * and the music has been stopped and disposed automatically.
     *
     * @param delta Seconds since last frame.
     * @return true if the fade just completed this frame.
     */
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
