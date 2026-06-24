package io.github.msameer0.rhythmicrush.game.level

/**
 * Shapes supported by the procedural background and ground pattern renderer.
 */
enum class PatternShape(val id: String) {
    SQUARE("square"),
    RECTANGLE("rectangle"),
    TRIANGLE("triangle"),
    CIRCLE("circle"),
    HEXAGON("hexagon");

    companion object {
        fun fromId(id: String?): PatternShape? {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
    }
}
