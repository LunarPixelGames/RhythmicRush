package io.github.msameer0.rhythmicrush

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.msameer0.rhythmicrush.ads.AdController
import io.github.msameer0.rhythmicrush.atlas.AtlasManager
import io.github.msameer0.rhythmicrush.audio.SoundManager
import io.github.msameer0.rhythmicrush.font.FontManager
import io.github.msameer0.rhythmicrush.game.level.LevelManager
import io.github.msameer0.rhythmicrush.game.level.ProgressManager
import io.github.msameer0.rhythmicrush.game.registries.Registries
import io.github.msameer0.rhythmicrush.screens.LoadingScreen
import io.github.msameer0.rhythmicrush.screens.MainMenuScreen
import io.github.msameer0.rhythmicrush.settings.SettingsManager
import io.github.msameer0.rhythmicrush.update.UpdateManager
import io.github.msameer0.rhythmicrush.window.WindowController

class RhythmicRushGame(val adController: AdController, val updateManager: UpdateManager) :
    Game() {

    lateinit var batch: SpriteBatch
    lateinit var soundManager: SoundManager
    lateinit var atlasManager: AtlasManager
    lateinit var fontManager: FontManager
    var windowController: WindowController? = null
    lateinit var progressManager: ProgressManager
    lateinit var levelManager: LevelManager
    lateinit var settingsManager: SettingsManager

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
        Gdx.app.log("Game", "Game disposed.")
    }
}
