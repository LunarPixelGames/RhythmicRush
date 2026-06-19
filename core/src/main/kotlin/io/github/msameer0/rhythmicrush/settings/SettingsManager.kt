package io.github.msameer0.rhythmicrush.settings

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

/**
 * Manages game settings, including persistence and application of graphics/audio configurations.
 */
class SettingsManager {
    var menuMusicEnabled: Boolean = true
    var musicVolume: Float = 1f
    var sfxVolume: Float = 1f
    var deathEffectEnabled: Boolean = true
    var showHitboxes: Boolean = false
    var showHitboxesOnDeath: Boolean = false
    var lockCursorInGame: Boolean = false
    var showFps: Boolean = false
    var capFps: Boolean = false
    var fpsCapValue: Int = 60
    var enableVsync: Boolean = false
    var showPercentage: Boolean = true
    var showProgressBar: Boolean = true
    var showAttempts: Boolean = true
    var showBest: Boolean = true
    var uiPadding: Float = 12f
    var practiceButtonOpacity: Float = 0.5f
    var pulseOrbs: Boolean = true

    /** Stores the serializable representation of game settings. */
    class Data {
        var menuMusicEnabled: Boolean = true
        var musicVolume: Float = 1f
        var sfxVolume: Float = 1f
        var deathEffectEnabled: Boolean = true
        var showHitboxes: Boolean = false
        var showHitboxesOnDeath: Boolean = false
        var lockCursorInGame: Boolean = false
        var showFps: Boolean = false
        var capFps: Boolean = false
        var fpsCapValue: Int = 60
        var enableVsync: Boolean = false
        var showPercentage: Boolean = true
        var showProgressBar: Boolean = true
        var showAttempts: Boolean = true
        var showBest: Boolean = true
        var uiPadding: Float = 12f
        var practiceButtonOpacity: Float = 0.5f
        var pulseOrbs: Boolean = true
    }

    private val json: Json = Json()

    init {
        json.setOutputType(JsonWriter.OutputType.json)
        json.setUsePrototypes(false)
        load()
    }

    fun save() {
        Gdx.app.log("SettingsManager", "Saving settings...")
        try {
            val snapshot = Data()
            snapshot.menuMusicEnabled = menuMusicEnabled
            snapshot.musicVolume = musicVolume
            snapshot.sfxVolume = sfxVolume
            snapshot.deathEffectEnabled = deathEffectEnabled
            snapshot.showHitboxes = showHitboxes
            snapshot.showHitboxesOnDeath = showHitboxesOnDeath
            snapshot.lockCursorInGame = lockCursorInGame
            snapshot.showFps = showFps
            snapshot.capFps = capFps
            snapshot.fpsCapValue = fpsCapValue
            snapshot.enableVsync = enableVsync
            snapshot.showPercentage = showPercentage
            snapshot.showProgressBar = showProgressBar
            snapshot.showAttempts = showAttempts
            snapshot.showBest = showBest
            snapshot.uiPadding = uiPadding
            snapshot.practiceButtonOpacity = practiceButtonOpacity
            snapshot.pulseOrbs = pulseOrbs
            val file = Gdx.files.local(SAVE_PATH)
            file.parent().mkdirs()
            file.writeString(json.prettyPrint(snapshot), false)
            Gdx.app.log("SettingsManager", "Settings saved successfully.")
        } catch (exception: Exception) {
            Gdx.app.error("SettingsManager", "Failed to save: ${exception.message}")
        }
    }

    private fun load() {
        Gdx.app.log("SettingsManager", "Loading settings...")
        try {
            val file = Gdx.files.local(SAVE_PATH)
            if (!file.exists()) {
                Gdx.app.log("SettingsManager", "No settings file found. Using defaults.")
                return
            }
            val savedSettings = json.fromJson<Data?>(Data::class.java, file) ?: return
            menuMusicEnabled = savedSettings.menuMusicEnabled
            musicVolume = savedSettings.musicVolume
            sfxVolume = savedSettings.sfxVolume
            deathEffectEnabled = savedSettings.deathEffectEnabled
            showHitboxes = savedSettings.showHitboxes
            showHitboxesOnDeath = savedSettings.showHitboxesOnDeath
            lockCursorInGame = savedSettings.lockCursorInGame
            showFps = savedSettings.showFps
            capFps = savedSettings.capFps
            fpsCapValue = savedSettings.fpsCapValue
            enableVsync = savedSettings.enableVsync
            showPercentage = savedSettings.showPercentage
            showProgressBar = savedSettings.showProgressBar
            showAttempts = savedSettings.showAttempts
            showBest = savedSettings.showBest
            uiPadding = savedSettings.uiPadding
            practiceButtonOpacity = savedSettings.practiceButtonOpacity
            pulseOrbs = savedSettings.pulseOrbs
            Gdx.app.log("SettingsManager", "Settings loaded successfully.")
        } catch (exception: Exception) {
            Gdx.app.error("SettingsManager", "Failed to load: ${exception.message}")
        }
    }

    fun applyFpsCap() {
        Gdx.graphics.setForegroundFPS(if (capFps) fpsCapValue else 0)
    }

    fun applyVsync() {
        val vsync = (Gdx.app.type != Application.ApplicationType.Desktop)
            || enableVsync
        Gdx.graphics.setVSync(vsync)
    }

    companion object {
        private const val SAVE_PATH = "saves/settings.json"
    }
}
