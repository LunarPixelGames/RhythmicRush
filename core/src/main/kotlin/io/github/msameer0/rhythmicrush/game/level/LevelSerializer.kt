package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.UBJsonReader
import com.badlogic.gdx.utils.UBJsonWriter

/**
 * Provides static methods for serializing and deserializing level data.
 * Supports both JSON (for editing) and UBJSON (for production/storage).
 */
class LevelSerializer {
    companion object {
        val json: Json = buildJson()

        private fun buildJson(): Json {
            return Json().apply {
                setOutputType(JsonWriter.OutputType.json)
                setUsePrototypes(false)
                setIgnoreUnknownFields(true)
            }
        }

        fun save(data: LevelData, file: FileHandle) {
            json.setWriter(null)
            val out = json.prettyPrint(data)
            file.writeString(out, false)
        }

        fun saveBinary(data: LevelData, file: FileHandle) {
            val writer = UBJsonWriter(file.write(false))
            try {
                // Json only writes text, so JsonValue bridges it to UBJSON.
                val jsonText = json.toJson(data)
                val value = com.badlogic.gdx.utils.JsonReader().parse(jsonText)
                writer.value(value)
            } finally {
                writer.close()
            }
        }

        fun load(file: FileHandle): LevelData? {
            if (!file.exists()) return null

            json.setWriter(null)
            val data = try {
                if (isBinary(file)) {
                    val reader = UBJsonReader()
                    val value = reader.parse(file)
                    json.readValue(LevelData::class.java, value)
                } else {
                    json.fromJson(LevelData::class.java, file)
                }
            } catch (_: Exception) {
                try {
                    json.fromJson(LevelData::class.java, file)
                } catch (_: Exception) {
                    null
                }
            }

            if (data != null) {
                data.fileName = file.name()
                if (data.id < 0) {
                    data.id = file.nameWithoutExtension().toIntOrNull() ?: -1
                }
            }
            return data
        }

        private fun isBinary(file: FileHandle): Boolean {
            if (file.extension().lowercase() == "ubj") return true
            // Extensionless files need a lightweight text-or-binary check.
            val firstByte = file.read().use { it.read().takeIf { byte -> byte >= 0 }?.toChar() }
            return firstByte != '{' && firstByte != '[' && firstByte != ' ' && firstByte != '\n' && firstByte != '\r'
        }

        fun fromString(jsonText: String?): LevelData? {
            return json.fromJson(LevelData::class.java, jsonText)
        }
    }
}
