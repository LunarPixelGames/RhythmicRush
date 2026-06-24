package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.UBJsonReader
import com.badlogic.gdx.utils.UBJsonWriter
import io.github.msameer0.rhythmicrush.account.CloudProgress
import io.github.msameer0.rhythmicrush.account.LevelCloudProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages the persistence of level completion progress and attempt counts.
 */
class ProgressManager {
    companion object {
        const val SAVE_PATH: String = "saves/progress.ubj"
        const val COINS_KEY: String = "coins"
        const val POINTS_KEY: String = "points"
        const val SCHEMA_KEY: String = "schemaVersion"
        const val CURRENT_SCHEMA: Int = 2
        private const val BACKUP_DIRECTORY = "saves/backups"
        private const val MAX_BACKUPS = 5
    }

    val map = ObjectMap<String, LevelProgress>()
    private val json = Json()
    var coins: Int = 0
    var points: Int = 0

    init {
        json.setOutputType(JsonWriter.OutputType.json)
        json.setUsePrototypes(false)
        load()
    }

    fun getOrCreate(levelKey: String): LevelProgress {
        val existingProgress = map.get(levelKey)
        if (existingProgress != null) return existingProgress

        return LevelProgress().also { map.put(levelKey, it) }
    }

    fun save(createBackup: Boolean = false) {
        Gdx.app.log("ProgressManager", "Saving progress...")
        try {
            val file = Gdx.files.local(SAVE_PATH)
            file.parent().mkdirs()
            val temporary = Gdx.files.local("$SAVE_PATH.tmp")
            writeSnapshot(temporary)
            UBJsonReader().parse(temporary)
            if (createBackup && file.exists()) createBackup(file)
            val previous = Gdx.files.local("$SAVE_PATH.previous")
            if (previous.exists()) previous.delete()
            if (file.exists()) file.moveTo(previous)
            try {
                temporary.moveTo(file)
                if (previous.exists()) previous.delete()
            } catch (exception: Exception) {
                if (!file.exists() && previous.exists()) previous.moveTo(file)
                throw exception
            }
            Gdx.app.log("ProgressManager", "Progress saved successfully.")
        } catch (exception: Exception) {
            Gdx.app.error(
                "ProgressManager",
                "Failed to save progress: ${exception.message}"
            )
        }
    }

    fun exportForSync(): List<LevelCloudProgress> {
        val result = mutableListOf<LevelCloudProgress>()
        for (entry in map) {
            result.add(
                LevelCloudProgress(
                    levelId = entry.key,
                    bestPercent = entry.value.bestPercent,
                    totalAttempts = entry.value.localDeviceAttempts,
                    completed = entry.value.bestPercent >= 100
                )
            )
        }
        return result
    }

    fun mergeCloud(progress: CloudProgress) {
        for (cloud in progress.levels) {
            val local = getOrCreate(cloud.levelId)
            local.bestPercent = maxOf(local.bestPercent, cloud.bestPercent)
            local.totalAttempts = maxOf(local.totalAttempts, cloud.totalAttempts)
            if (cloud.completed) local.completionRewardGranted = true
        }
        coins = progress.coins
        points = progress.points
        save(createBackup = true)
    }

    private fun load() {
        Gdx.app.log("ProgressManager", "Loading progress...")
        try {
            val file = Gdx.files.local(SAVE_PATH)
            if (!file.exists()) {
                Gdx.app.log("ProgressManager", "No progress file found.")
                return
            }

            val root = UBJsonReader().parse(file)
            val savedSchema = root.getInt(SCHEMA_KEY, 1)
            var entry = root.child
            while (entry != null) {
                when (entry.name) {
                    SCHEMA_KEY -> Unit
                    COINS_KEY -> coins = entry.asInt()
                    POINTS_KEY -> points = entry.asInt()
                    else -> {
                        val levelProgress =
                            json.readValue<LevelProgress?>(
                                LevelProgress::class.java,
                                entry
                            )
                        if (levelProgress != null) {
                            if (savedSchema < 2) {
                                levelProgress.localDeviceAttempts = levelProgress.totalAttempts
                                if (levelProgress.bestPercent >= 100) {
                                    levelProgress.completionRewardGranted = true
                                }
                            }
                            map.put(entry.name, levelProgress)
                        }
                    }
                }
                entry = entry.next
            }
            Gdx.app.log("ProgressManager", "Progress loaded: " + map.size + " entries.")
        } catch (exception: Exception) {
            Gdx.app.error(
                "ProgressManager",
                "Failed to load progress: ${exception.message}"
            )
        }
    }

    private fun writeSnapshot(file: com.badlogic.gdx.files.FileHandle) {
        val writer = UBJsonWriter(file.write(false))
        try {
            writer.`object`()
            writer.set(SCHEMA_KEY, CURRENT_SCHEMA)
            writer.set(COINS_KEY, coins)
            writer.set(POINTS_KEY, points)
            for (entry in map) {
                writer.name(entry.key)
                writer.value(com.badlogic.gdx.utils.JsonReader().parse(json.toJson(entry.value)))
            }
            writer.pop()
        } finally {
            writer.close()
        }
    }

    private fun createBackup(source: com.badlogic.gdx.files.FileHandle) {
        val directory = Gdx.files.local(BACKUP_DIRECTORY)
        directory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        source.copyTo(directory.child("progress-$timestamp.ubj"))
        val backups = directory.list(".ubj").sortedByDescending { it.lastModified() }
        for (index in MAX_BACKUPS until backups.size) backups[index].delete()
    }
}
