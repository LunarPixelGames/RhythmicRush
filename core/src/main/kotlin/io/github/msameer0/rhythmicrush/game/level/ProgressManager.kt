package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.ObjectMap

/**
 * Manages the persistence of level completion progress and attempt counts.
 */
class ProgressManager {
    companion object {
        const val SAVE_PATH: String = "saves/progress.json"
        const val COINS_KEY: String = "coins"
        const val POINTS_KEY: String = "points"
    }

    val map = ObjectMap<String, LevelProgress>()
    var json: Json = Json()
    var coins: Int = 0
    var points: Int = 0

    constructor() {
        json.setOutputType(JsonWriter.OutputType.json)
        json.setUsePrototypes(false)
        load()
    }

    fun getOrCreate(levelKey: String): LevelProgress {
        if (!map.containsKey(levelKey)) {
            map.put(levelKey, LevelProgress())
        }
        return map.get(levelKey)
    }

    fun migrateLegacyLevelKeys(levels: Iterable<LevelData?>) {
        var changed = false
        for (level in levels) {
            if (level == null || level.id < 0) continue

            val newKey = level.getProgressKey()
            val legacyKeys = arrayOf(level.fileName, level.fileName.substringBeforeLast('.', ""), level.id.toString() + ".ubj", level.id.toString() + ".json")

            var target = map.get(newKey)
            for (legacyKey in legacyKeys) {
                if (legacyKey.isEmpty() || legacyKey == newKey) continue
                val legacy = map.get(legacyKey) ?: continue
                if (target == null) {
                    map.put(newKey, legacy)
                    target = legacy
                } else {
                    target.bestPercent = maxOf(target.bestPercent, legacy.bestPercent)
                    target.totalAttempts += legacy.totalAttempts
                }
                map.remove(legacyKey)
                changed = true
            }
        }

        if (changed) {
            save()
        }
    }

    fun save() {
        Gdx.app.log("ProgressManager", "Saving progress...")
        try {
            val file = Gdx.files.local(SAVE_PATH)
            file.parent().mkdirs()

            val sb = StringBuilder("{\n")
            sb.append("  \"").append(COINS_KEY).append("\": ").append(coins)
            sb.append(",\n")
            sb.append("  \"").append(POINTS_KEY).append("\": ").append(points)
            var first = false
            for (entry in map) {
                if (!first) sb.append(",\n")
                sb.append("  \"").append(entry.key).append("\": ")
                sb.append(json.toJson(entry.value))
                first = false
            }
            sb.append("\n}")
            file.writeString(sb.toString(), false)
            Gdx.app.log("ProgressManager", "Progress saved successfully.")
        } catch (e: Exception) {
            Gdx.app.error("ProgressManager", "Failed to save progress: " + e.message)
        }
    }

    private fun load() {
        Gdx.app.log("ProgressManager", "Loading progress...")
        try {
            val file = Gdx.files.local(SAVE_PATH)
            if (!file.exists()) {
                Gdx.app.log("ProgressManager", "No progress file found.")
                return
            }

            val root = JsonReader().parse(file)
            var entry = root.child
            while (entry != null) {
                when (entry.name) {
                    COINS_KEY -> coins = entry.asInt()
                    POINTS_KEY -> points = entry.asInt()
                    else -> {
                        val p = json.readValue<LevelProgress?>(LevelProgress::class.java, entry)
                        if (p != null) map.put(entry.name, p)
                    }
                }
                entry = entry.next
            }
            Gdx.app.log("ProgressManager", "Progress loaded: " + map.size + " entries.")
        } catch (e: java.lang.Exception) {
            Gdx.app.error("ProgressManager", "Failed to load progress: " + e.message)
        }
    }
}
