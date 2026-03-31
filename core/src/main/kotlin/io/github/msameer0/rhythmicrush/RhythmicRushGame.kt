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
    lateinit var windowController: WindowController
    lateinit var progressManager: ProgressManager
    lateinit var levelManager: LevelManager
    lateinit var settingsManager: SettingsManager

    override fun create() {
        Gdx.app.log("Game", "Initializing RhythmicRush...")

        Registries.init()

        batch = SpriteBatch()
        atlasManager = AtlasManager()
        settingsManager = SettingsManager()
        fontManager = FontManager()
        soundManager = SoundManager()
        progressManager = ProgressManager()
        levelManager = LevelManager()

        soundManager.setMusicVolume(settingsManager.musicVolume)
        settingsManager.applyFpsCap()
        settingsManager.applyVsync()

        if (settingsManager.menuMusicEnabled) {
            soundManager.playMenuMusic()
        }

        Gdx.app.log("Game", "Initialization complete. Entering Main Menu.")

        setScreen(MainMenuScreen(this))
    }

    override fun dispose() {
        Gdx.app.log("Game", "Disposing game resources...")
        super.dispose()
        batch.dispose()
        soundManager.dispose()
        atlasManager.dispose()
        fontManager.dispose()
        Gdx.app.log("Game", "Game disposed.")
    }
}
