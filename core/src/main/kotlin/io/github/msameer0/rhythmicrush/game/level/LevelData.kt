package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.utils.Array

class LevelData {
    var name = "Unnamed Level"
    var fileName = ""
    var musicFile = ""
    var bgImage = ""
    var bgColor = "1a1a2e"
    var groundColor = "16213e"
    var difficulty = "normal"

    var objects = Array<ObjectEntry>()

    class ObjectEntry {
        var type: String = ""
        var x: Float = 0f
        var y: Float = 0f
        var size: Float = 0f
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
