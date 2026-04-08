package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.level.LevelData;

/**
 * Handles all music playback, volume control, and fade logic for a gameplay session.
 *
 * <p>Owns the lifecycle of the {@link Music} instance: loading, playing,
 * pausing, fading, and disposal. GameScreen delegates all audio concerns here.</p>
 */
public class MusicController {

    private static final float MUSIC_FADE_DURATION = 3f;

    private final RhythmicRushGame game;
    private final LevelData levelData;

    private Music levelMusic;

    private boolean fading = false;
    private float fadeTimer = 0f;

    public MusicController(RhythmicRushGame game, LevelData levelData) {
        this.game = game;
        this.levelData = levelData;
    }

    // ── Playback ─────────────────────────────────────────────────────────────

    /** Starts music from the beginning. */
    public void start() {
        start(0f);
    }

    /**
     * Starts music at a given time offset (used for practice checkpoint respawns).
     *
     * @param startTime Seconds into the track to begin playback from.
     */
    public void start(float startTime) {
        if (levelData == null || levelData.getMusicFile() == null || levelData.getMusicFile().isEmpty())
            return;
        try {
            FileHandle fh = Gdx.files.internal("musics/" + levelData.getMusicFile());
            if (!fh.exists()) fh = Gdx.files.local("assets/musics/" + levelData.getMusicFile());
            if (fh.exists()) {
                levelMusic = Gdx.audio.newMusic(fh);
                levelMusic.setVolume(game.getSettingsManager().getMusicVolume());
                levelMusic.setLooping(false);
                levelMusic.play();
                if (startTime > 0f) levelMusic.setPosition(startTime);
            }
        } catch (Exception e) {
            Gdx.app.error("MusicController", "Could not load music: " + e.getMessage());
        }
    }

    /** Pauses playback without disposing the track. */
    public void pause() {
        if (levelMusic != null && levelMusic.isPlaying()) levelMusic.pause();
    }

    /** Resumes a paused track. */
    public void resume() {
        if (levelMusic != null && !levelMusic.isPlaying()) levelMusic.play();
    }

    /** Stops and disposes the active track immediately. */
    public void stopAndDispose() {
        if (levelMusic != null) {
            if (levelMusic.isPlaying()) levelMusic.stop();
            levelMusic.dispose();
            levelMusic = null;
        }
        fading = false;
        fadeTimer = 0f;
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    /** Applies a new volume level directly (e.g. from the pause slider). */
    public void setVolume(float volume) {
        if (levelMusic != null) levelMusic.setVolume(volume);
    }

    /**
     * Applies a partial fade toward silence based on external progress (0–1).
     * Used by the level-end sequence where GameScreen controls timing.
     *
     * @param fadeProgress 0 = full volume, 1 = silent.
     */
    public void applyFadeProgress(float fadeProgress) {
        if (levelMusic == null) return;
        float baseVol = game.getSettingsManager().getMusicVolume();
        levelMusic.setVolume(baseVol * (1f - Math.min(fadeProgress, 1f)));
    }

    /**
     * Begins the full music-fade-then-stop sequence (used for music-fading on
     * death / exit). Call {@link #updateFade(float)} each frame while active.
     */
    public void beginFade() {
        if (levelMusic != null) {
            fading = true;
            fadeTimer = 0f;
        }
    }

    /**
     * Updates the fade timer. Returns {@code true} when the fade has finished
     * and the music has been stopped and disposed automatically.
     *
     * @param delta Seconds since last frame.
     * @return true if the fade just completed this frame.
     */
    public boolean updateFade(float delta) {
        if (!fading || levelMusic == null) return false;
        fadeTimer += delta;
        float base = game.getSettingsManager().getMusicVolume();
        float vol = base * (1f - Math.min(fadeTimer / MUSIC_FADE_DURATION, 1f));
        levelMusic.setVolume(vol);
        if (fadeTimer >= MUSIC_FADE_DURATION) {
            stopAndDispose();
            return true;
        }
        return false;
    }

    // ── State queries ─────────────────────────────────────────────────────────

    public boolean isFading() { return fading; }
    public boolean isPlaying() { return levelMusic != null && levelMusic.isPlaying(); }
    public Music getMusic() { return levelMusic; }
}
