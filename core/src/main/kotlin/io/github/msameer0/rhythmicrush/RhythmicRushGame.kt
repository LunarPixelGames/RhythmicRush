package io.github.msameer0.rhythmicrush

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.msameer0.rhythmicrush.account.AccountClient
import io.github.msameer0.rhythmicrush.account.AccountConfigurationProvider
import io.github.msameer0.rhythmicrush.account.AccountManager
import io.github.msameer0.rhythmicrush.account.AccountSyncCoordinator
import io.github.msameer0.rhythmicrush.ads.AdController
import io.github.msameer0.rhythmicrush.atlas.AtlasManager
import io.github.msameer0.rhythmicrush.audio.SoundManager
import io.github.msameer0.rhythmicrush.font.FontManager
import io.github.msameer0.rhythmicrush.game.level.LevelManager
import io.github.msameer0.rhythmicrush.game.level.LevelThumbnailManager
import io.github.msameer0.rhythmicrush.game.level.ProgressManager
import io.github.msameer0.rhythmicrush.input.GdxTextInputController
import io.github.msameer0.rhythmicrush.input.TextInputController
import io.github.msameer0.rhythmicrush.screens.LoadingScreen
import io.github.msameer0.rhythmicrush.settings.SettingsManager
import io.github.msameer0.rhythmicrush.update.UpdateManager
import io.github.msameer0.rhythmicrush.window.WindowController

/**
 * The main game entry point for Rhythmic Rush, managing central state, managers, and the current screen.
 */
class RhythmicRushGame(
    val adController: AdController,
    val updateManager: UpdateManager,
    private val accountClient: AccountClient,
    private val accountConfigurationProvider: AccountConfigurationProvider,
    val textInputController: TextInputController = GdxTextInputController
) : Game() {

    lateinit var batch: SpriteBatch
    lateinit var soundManager: SoundManager
    lateinit var atlasManager: AtlasManager
    lateinit var fontManager: FontManager
    var windowController: WindowController? = null
    lateinit var progressManager: ProgressManager
    lateinit var levelManager: LevelManager
    lateinit var levelThumbnailManager: LevelThumbnailManager
    lateinit var settingsManager: SettingsManager
    lateinit var accountManager: AccountManager
    lateinit var accountSyncCoordinator: AccountSyncCoordinator

    override fun create() {
        Gdx.app.log("Game", "Starting RhythmicRush...")

        batch = SpriteBatch()

        Gdx.app.log("Game", "Entering Loading Screen.")
        setScreen(LoadingScreen(this))
    }

    override fun dispose() {
        Gdx.app.log("Game", "Disposing game resources...")
        super.dispose()
        if (::batch.isInitialized) batch.dispose()
        if (::soundManager.isInitialized) soundManager.dispose()
        if (::atlasManager.isInitialized) atlasManager.dispose()
        if (::fontManager.isInitialized) fontManager.dispose()
        if (::levelThumbnailManager.isInitialized) levelThumbnailManager.dispose()
        if (::accountSyncCoordinator.isInitialized) accountSyncCoordinator.dispose()
        if (::accountManager.isInitialized) accountManager.dispose()
        Gdx.app.log("Game", "Game disposed.")
    }

    fun initializeAccounts() {
        if (::accountManager.isInitialized) return
        accountManager = AccountManager(
            accountClient,
            accountConfigurationProvider.getConfiguration()
        )
        accountSyncCoordinator = AccountSyncCoordinator(
            accountManager,
            progressManager,
            accountConfigurationProvider.getConfiguration().contentVersion
        )
        accountSyncCoordinator.start()
        accountManager.initialize()
    }

    fun queueCloudProgressUpload() {
        if (::accountSyncCoordinator.isInitialized) {
            accountSyncCoordinator.queueCurrentSnapshot()
        }
    }
}
