package io.github.msameer0.rhythmicrush.android

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.account.AccountConfiguration
import io.github.msameer0.rhythmicrush.account.AccountConfigurationProvider
import io.github.msameer0.rhythmicrush.android.account.AndroidAccountClient
import io.github.msameer0.rhythmicrush.android.ads.AndroidAdController
import io.github.msameer0.rhythmicrush.android.update.AndroidUpdateManager
import io.github.msameer0.rhythmicrush.window.WindowController

class AndroidLauncher : AndroidApplication() {
    lateinit var adController : AndroidAdController
    lateinit var updateManager : AndroidUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = AndroidUpdateManager(this)
        adController = AndroidAdController(this)

        val config = AndroidApplicationConfiguration()
        config.useImmersiveMode = true

        val accountConfiguration = ProductionAccountConfiguration.getConfiguration()
        val webClientIdResource = resources.getIdentifier(
            "default_web_client_id",
            "string",
            packageName
        )
        require(webClientIdResource != 0) {
            "google-services.json did not generate default_web_client_id."
        }
        val game = RhythmicRushGame(
            adController,
            updateManager,
            AndroidAccountClient(
                this,
                accountConfiguration.backendBaseUrl,
                getString(webClientIdResource)
            ),
            AccountConfigurationProvider { accountConfiguration }
        )
        game.windowController = AndroidWindowController()

        val layout = RelativeLayout(this)
        val gameView = initializeForView(game, config)
        layout.addView(gameView)

        adController.initialize(layout)
        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AndroidUpdateManager.UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Gdx.app.log("UpdateTest", "User cancelled or update failed")
            }
        }
    }

    override fun onPause() {
        adController.onPause()
        super.onPause()
    }

    override fun onStop() {
        (updateManager as AndroidUpdateManager).onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        adController.onResume()
        (updateManager as AndroidUpdateManager).onResume()
    }

    override fun onDestroy() {
        adController.onDestroy()
        (updateManager as AndroidUpdateManager).onDestroy()
        super.onDestroy()
    }

    class AndroidWindowController : WindowController {
        override fun toggleFullscreen() {
            Gdx.app.log("AndroidLauncher", "No Window Controls on Android")
        }
    }

    private object ProductionAccountConfiguration : AccountConfigurationProvider {
        override fun getConfiguration() = AccountConfiguration(
            environment = "production",
            firebaseProjectId = "rhythmic-rush",
            firebaseWebApiKey = null,
            backendBaseUrl =
                "https://rhythmic-rush-api.sameerthecoolguy2006.workers.dev",
            apiVersion = 1,
            contentVersion = 1
        )
    }
}
