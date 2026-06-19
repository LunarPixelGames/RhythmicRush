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
            val j = Json()
            j.setOutputType(JsonWriter.OutputType.json)
            j.setUsePrototypes(false)
            j.setIgnoreUnknownFields(true)
            return j
        }

        /** Saves as standard readable JSON */
        fun save(data: LevelData, file: FileHandle) {
            json.setWriter(null) // Ensure we're not using a binary writer
            val out = json.prettyPrint(data)
            file.writeString(out, false)
        }

        /** Saves as compact binary UBJSON */
        fun saveBinary(data: LevelData, file: FileHandle) {
            val writer = UBJsonWriter(file.write(false))
            try {
                // Use intermediate JsonValue to bridge text-only Json class to UBJsonWriter
                val jsonText = json.toJson(data)
                val value = com.badlogic.gdx.utils.JsonReader().parse(jsonText)
                writer.value(value)
            } finally {
                writer.close()
            }
        }

        fun load(file: FileHandle): LevelData? {
            if (!file.exists()) return null

            json.setWriter(null) // Reset writer state for safety
            val data = try {
                // Try UBJSON first (check for non-text start or use extension)
                if (isBinary(file)) {
                    val reader = UBJsonReader()
                    val value = reader.parse(file)
                    json.readValue(LevelData::class.java, value)
                } else {
                    json.fromJson(LevelData::class.java, file)
                }
            } catch (e: Exception) {
                // Fallback to text JSON if UBJSON fails
                try {
                    json.fromJson(LevelData::class.java, file)
                } catch (e2: Exception) {
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
            // Peak first byte - UBJSON usually starts with object/array markers '{' '[' or type markers
            // and won't have the typical whitespace of a pretty JSON.
            // Simplified check: if it can't be read as a simple string starting with '{', it's likely binary.
            val firstByte = file.read().use { it.read().takeIf { byte -> byte >= 0 }?.toChar() }
            return firstByte != '{' && firstByte != '[' && firstByte != ' ' && firstByte != '\n' && firstByte != '\r'
        }

        fun fromString(jsonText: String?): LevelData? {
            return json.fromJson(LevelData::class.java, jsonText)
        }
    }
}
