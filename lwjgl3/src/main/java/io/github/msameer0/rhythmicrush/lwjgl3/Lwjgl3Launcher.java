package io.github.msameer0.rhythmicrush.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.ads.AdController;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        RhythmicRushGame game = new RhythmicRushGame(new AdController() {
            @Override
            public void showInterstitialAd() {
                Gdx.app.log("AdController", "Not showing ad on PC.");
            }

            @Override
            public void showBannerAd(boolean show) {
                Gdx.app.log("AdController", "Not showing ad on PC.");
            }

            @Override
            public boolean isAdLoaded() {
                return false;
            }
        },
            () -> {

            });

        // IMPORTANT: set the controller BEFORE new Lwjgl3Application(...)
        // because Lwjgl3Application calls game.create() in its constructor
        // synchronously — so it must be set first or getWindowController()
        // returns null when the first screen tries to use it.
        game.setWindowController(new DesktopWindowController());

        return new Lwjgl3Application(game, getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("RhythmicRush");
        configuration.useVsync(false);
        configuration.setForegroundFPS(0);
        configuration.setWindowedMode(1280, 720);
        configuration.setResizable(false);
        configuration.setWindowIcon("icon_128.png", "icon_32.png", "icon_16.png");
        return configuration;
    }
}
