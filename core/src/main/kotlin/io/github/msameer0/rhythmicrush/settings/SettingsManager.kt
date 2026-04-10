package io.github.msameer0.rhythmicrush.settings

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

class SettingsManager {
    var menuMusicEnabled: Boolean = true
    var musicVolume: Float = 1f
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

    class Data {
        var menuMusicEnabled: Boolean = true
        var musicVolume: Float = 1f
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
            val file = Gdx.files.local(SAVE_PATH)
            file.parent().mkdirs()
            file.writeString(json.prettyPrint(snapshot), false)
            Gdx.app.log("SettingsManager", "Settings saved successfully.")
        } catch (e: Exception) {
            Gdx.app.error("SettingsManager", "Failed to save: " + e.message)
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
            val d = json.fromJson<Data?>(Data::class.java, file)
            if (d == null) return
            menuMusicEnabled = d.menuMusicEnabled
            musicVolume = d.musicVolume
            showHitboxes = d.showHitboxes
            showHitboxesOnDeath = d.showHitboxesOnDeath
            lockCursorInGame = d.lockCursorInGame
            showFps = d.showFps
            capFps = d.capFps
            fpsCapValue = d.fpsCapValue
            enableVsync = d.enableVsync
            showPercentage = d.showPercentage
            showProgressBar = d.showProgressBar
            showAttempts = d.showAttempts
            showBest = d.showBest
            uiPadding = d.uiPadding
            practiceButtonOpacity = d.practiceButtonOpacity
            Gdx.app.log("SettingsManager", "Settings loaded successfully.")
        } catch (e: Exception) {
            Gdx.app.error("SettingsManager", "Failed to load: " + e.message)
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
