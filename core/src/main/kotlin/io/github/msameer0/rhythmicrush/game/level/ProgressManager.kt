package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.UBJsonReader
import com.badlogic.gdx.utils.UBJsonWriter

/**
 * Manages the persistence of level completion progress and attempt counts.
 */
class ProgressManager {
    companion object {
        const val SAVE_PATH: String = "saves/progress.ubj"
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

    fun save() {
        Gdx.app.log("ProgressManager", "Saving progress...")
        try {
            val file = Gdx.files.local(SAVE_PATH)
            file.parent().mkdirs()
            val writer = UBJsonWriter(file.write(false))
            try {
                writer.`object`()
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

            val root = UBJsonReader().parse(file)
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
