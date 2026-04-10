package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.ObjectMap

class ProgressManager {
    companion object {
        const val SAVE_PATH: String = "saves/progress.json"
    }

    val map = ObjectMap<String, LevelProgress>()
    var json: Json = Json()

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

            val sb = StringBuilder("{\n")
            var first = true
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
                val p = json.readValue<LevelProgress?>(LevelProgress::class.java, entry)
                if (p != null) map.put(entry.name, p)
                entry = entry.next
            }
            Gdx.app.log("ProgressManager", "Progress loaded: " + map.size + " entries.")
        } catch (e: java.lang.Exception) {
            Gdx.app.error("ProgressManager", "Failed to load progress: " + e.message)
        }
    }
}
