package io.github.msameer0.rhythmicrush.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class SoundManager {

    private float musicVolume = 1f;
    private float sfxVolume   = 1f;

    private Music menuMusic;

    public SoundManager() {}

    public void playMenuMusic() {
        if (menuMusic == null) {
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("musics/954091_vulg.mp3"));
            menuMusic.setLooping(true);
            menuMusic.setVolume(musicVolume);
        }

        if (!menuMusic.isPlaying()) {
            menuMusic.play();
        }
    }

    public void pauseMenuMusic() {
        if (menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.pause();
        }
    }

    public void stopMenuMusic() {
        if (menuMusic != null) {
            menuMusic.stop();
        }
    }

    public float getMusicVolume() { return musicVolume; }

    public void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        if (menuMusic != null) menuMusic.setVolume(musicVolume);
    }

    public float getSfxVolume() { return sfxVolume; }

    public void setSfxVolume(float volume) {
        sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    public void dispose() {
        if (menuMusic != null) {
            menuMusic.stop();
            menuMusic.dispose();
            menuMusic = null;
        }
    }
}
