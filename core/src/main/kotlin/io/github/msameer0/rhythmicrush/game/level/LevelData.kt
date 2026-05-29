package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue

/**
 * Container for all data defining a game level, including metadata and object layout.
 */
class LevelData : Json.Serializable {
    var name = "Unnamed Level"
    var fileName = ""
    var musicFile = ""
    var bgImage = ""
    var bgColor = "1a1a2e"
    var groundColor = "16213e"
    var difficulty = "normal"
    var youtubeLink: String = ""

    var objects = Array<ObjectEntry>()

    /**
     * Represents a single object within a level (block, hazard, portal, etc.).
     */
    class ObjectEntry : Json.Serializable {
        var type: String = ""
        var x: Float = 0f
        var y: Float = 0f
        var size: Float = 100f
        var blockType: String = ""
        var rotation: Float = 0f
        var flipped: Boolean = false

        var triggerBgColor: String = ""
        var triggerGroundColor: String = ""
        var fadeDuration: Float = 1f

        var pulseBgColor: String = ""
        var pulseGroundColor: String = ""
        var fadeInTime: Float = 0.1f
        var holdTime: Float = 0.2f
        var fadeOutTime: Float = 0.5f

        constructor()

        constructor (type: String, x: Float, y: Float, size: Float) {
            this.type = type
            this.x = x
            this.y = y
            this.size = size
        }

        override fun write(json: Json) {
            json.writeValue("type", type)
            json.writeValue("x", x)
            json.writeValue("y", y)
            if (size != 100f) json.writeValue("size", size)
            if (blockType.isNotEmpty()) json.writeValue("blockType", blockType)
            if (rotation != 0f) json.writeValue("rotation", rotation)
            if (flipped) json.writeValue("flipped", flipped)

            if (type == "color_trigger") {
                if (triggerBgColor.isNotEmpty()) json.writeValue("triggerBgColor", triggerBgColor)
                if (triggerGroundColor.isNotEmpty()) json.writeValue("triggerGroundColor", triggerGroundColor)
                if (fadeDuration != 1f) json.writeValue("fadeDuration", fadeDuration)
            } else if (type == "pulse_trigger") {
                if (pulseBgColor.isNotEmpty()) json.writeValue("pulseBgColor", pulseBgColor)
                if (pulseGroundColor.isNotEmpty()) json.writeValue("pulseGroundColor", pulseGroundColor)
                if (fadeInTime != 0.1f) json.writeValue("fadeInTime", fadeInTime)
                if (holdTime != 0.2f) json.writeValue("holdTime", holdTime)
                if (fadeOutTime != 0.5f) json.writeValue("fadeOutTime", fadeOutTime)
            }
        }

        override fun read(json: Json, jsonData: JsonValue) {
            type = jsonData.getString("type", "")
            x = jsonData.getFloat("x", 0f)
            y = jsonData.getFloat("y", 0f)
            size = jsonData.getFloat("size", 100f)
            blockType = jsonData.getString("blockType", "")
            rotation = jsonData.getFloat("rotation", 0f)
            flipped = jsonData.getBoolean("flipped", false)

            triggerBgColor = jsonData.getString("triggerBgColor", "")
            triggerGroundColor = jsonData.getString("triggerGroundColor", "")
            fadeDuration = jsonData.getFloat("fadeDuration", 1f)

            pulseBgColor = jsonData.getString("pulseBgColor", "")
            pulseGroundColor = jsonData.getString("pulseGroundColor", "")
            fadeInTime = jsonData.getFloat("fadeInTime", 0.1f)
            holdTime = jsonData.getFloat("holdTime", 0.2f)
            fadeOutTime = jsonData.getFloat("fadeOutTime", 0.5f)
        }
    }

    override fun write(json: Json) {
        if (name != "Unnamed Level") json.writeValue("name", name)
        if (musicFile.isNotEmpty()) json.writeValue("musicFile", musicFile)
        if (bgImage.isNotEmpty()) json.writeValue("bgImage", bgImage)
        if (bgColor != "1a1a2e") json.writeValue("bgColor", bgColor)
        if (groundColor != "16213e") json.writeValue("groundColor", groundColor)
        if (difficulty != "normal") json.writeValue("difficulty", difficulty)
        if (youtubeLink.isNotEmpty()) json.writeValue("youtubeLink", youtubeLink)
        json.writeValue("objects", objects)
    }

    override fun read(json: Json, jsonData: JsonValue) {
        name = jsonData.getString("name", "Unnamed Level")
        musicFile = jsonData.getString("musicFile", "")
        bgImage = jsonData.getString("bgImage", "")
        bgColor = jsonData.getString("bgColor", "1a1a2e")
        groundColor = jsonData.getString("groundColor", "16213e")
        difficulty = jsonData.getString("difficulty", "normal")
        youtubeLink = jsonData.getString("youtubeLink", "")

        objects.clear()
        val objectsVal = jsonData.get("objects")
        if (objectsVal != null && objectsVal.isArray) {
            for (entry in objectsVal) {
                val obj = json.readValue(ObjectEntry::class.java, entry)
                if (obj != null) objects.add(obj)
            }
        }
    }

    fun getLevelEndX(): Float {
        var max = 0f
        for (e in objects) {
            if ("color_trigger" == e.type) continue
            val right = e.x + e.size
            if (right > max) max = right
        }
        return max
    }
}
