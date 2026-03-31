package io.github.msameer0.rhythmicrush.settings

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

/**
 * Manages the persistence, retrieval, and application of user-configurable game settings.
 *
 *
 * This class serves as a central hub for various preferences including audio levels,
 * graphical toggles (FPS caps, VSync), and UI visibility options. Settings are
 * serialized to and deserialized from a local JSON file using the LibGDX [Json]
 * utility to ensure configurations are saved across game sessions.
 *
 */
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

    /**
     * A data transfer object (DTO) used to represent a serializable snapshot of the settings.
     * This class is primarily used by [Json] for loading and saving settings to the local filesystem.
     */
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

    private val json: Json

    /**
     * Initializes a new SettingsManager, configures the JSON serializer,
     * and attempts to load existing settings from the local storage.
     */
    init {
        json = Json()
        json.setOutputType(JsonWriter.OutputType.json)
        json.setUsePrototypes(false)
        load()
    }

    /**
     * Persists the current settings to a local JSON file.
     *
     *
     * This method captures a snapshot of the current configuration states,
     * serializes them into a formatted JSON string, and writes the output
     * to the local storage path defined by [.SAVE_PATH].
     *
     */
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

    /**
     * Loads the settings from the local storage file.
     *
     *
     * This method attempts to read the JSON file at [.SAVE_PATH]. If the file exists,
     * it deserializes the content into a [Data] object and updates the current
     * instance's fields with the stored values. If the file is missing or corrupted,
     * the default settings are retained and an error is logged.
     *
     */
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

    /**
     * Applies the frames per second (FPS) limit to the game's graphics settings.
     *
     *
     * If [.capFps] is enabled, the foreground FPS is restricted to the value
     * defined in [.fpsCapValue]. Otherwise, the cap is set to 0, which
     * effectively disables the FPS limit in LibGDX.
     *
     */
    fun applyFpsCap() {
        Gdx.graphics.setForegroundFPS(if (capFps) fpsCapValue else 0)
    }

    /**
     * Applies the vertical synchronization (VSync) setting to the graphics configuration.
     *
     *
     * VSync is automatically enabled on non-desktop platforms to ensure rendering stability.
     * On desktop platforms, the state is determined by the [.enableVsync] toggle.
     *
     */
    fun applyVsync() {
        val vsync = (Gdx.app.getType() != Application.ApplicationType.Desktop)
            || enableVsync
        Gdx.graphics.setVSync(vsync)
    }

    companion object {
        private const val SAVE_PATH = "saves/settings.json"
    }
}
