package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

/**
 * Provides static methods for serializing and deserializing level data to and from JSON format.
 */
class LevelSerializer {
    companion object {
        val json: Json = buildJson()

        private fun buildJson(): Json {
            val j = Json()
            j.setOutputType(JsonWriter.OutputType.json)
            j.setUsePrototypes(false)
            return j
        }

        fun save(data: LevelData, file: FileHandle) {
            val out = json.prettyPrint(data)
            file.writeString(out, false)
        }

        fun load(file: FileHandle): LevelData? {
            val data = json.fromJson(LevelData::class.java, file)
            if (data != null) data.fileName = file.name()

            return data
        }

        fun fromString(jsonText: String?): LevelData? {
            return json.fromJson(LevelData::class.java, jsonText)
        }
    }
}
