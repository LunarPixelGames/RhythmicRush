package io.github.msameer0.rhythmicrush.lwjgl3

import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.account.AccountClient
import io.github.msameer0.rhythmicrush.account.UnavailableAccountClient
import io.github.msameer0.rhythmicrush.ads.AdController
import io.github.msameer0.rhythmicrush.lwjgl3.account.DesktopAccountClient
import io.github.msameer0.rhythmicrush.lwjgl3.account.DesktopAccountConfiguration
import io.github.msameer0.rhythmicrush.lwjgl3.input.DesktopTextInputController
import io.github.msameer0.rhythmicrush.lwjgl3.window.DesktopWindowController
import io.github.msameer0.rhythmicrush.update.UpdateManager

class Lwjgl3Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (StartupHelper.startNewJvmIfRequired()) {
                return
            }
            createApplication()
        }

        private fun createApplication() : Lwjgl3Application {
            val accountConfigurationProvider = DesktopAccountConfiguration()
            val accountConfiguration = accountConfigurationProvider.getConfiguration()
            val accountClient: AccountClient =
                if (accountConfiguration.isValid &&
                    !accountConfiguration.firebaseWebApiKey.isNullOrBlank()
                ) {
                    DesktopAccountClient(accountConfiguration)
                } else {
                    UnavailableAccountClient(
                        "Desktop account configuration is missing or invalid."
                    )
                }
            val game = RhythmicRushGame(
                DesktopAdController(),
                DesktopUpdateManager(),
                accountClient,
                accountConfigurationProvider,
                DesktopTextInputController()
            )

            game.windowController = DesktopWindowController()

            return Lwjgl3Application(game, getDefaultConfiguration())
        }

        private fun getDefaultConfiguration() : Lwjgl3ApplicationConfiguration {
            val config = Lwjgl3ApplicationConfiguration()

            config.setTitle("RhythmicRush");
            config.useVsync(false);
            config.setForegroundFPS(0);
            config.setWindowedMode(1280, 720);
            config.setResizable(false);
            config.setWindowIcon("icon_128.png", "icon_32.png", "icon_16.png");

            return config
        }
    }

    class DesktopAdController : AdController {
        override fun showInterstitialAd() {
            Gdx.app.log("AdController", "Not showing ad on PC.")
        }

        override fun showBannerAd(show: Boolean) {
            Gdx.app.log("AdController", "Not showing ad on PC.")
        }

        override fun isAdLoaded(): Boolean {
            return false
        }
    }

    class DesktopUpdateManager : UpdateManager {
        override fun checkForUpdate() {
            Gdx.app.log("UpdateManager", "Not checking for updates on PC.")
        }

        override fun forceUpdate() {
            Gdx.app.log("UpdateManager", "Force update not available on PC.")
        }

        override fun isUpdatePending(): Boolean = false

        override fun isUpdateAvailable(): Boolean = false
    }

}
