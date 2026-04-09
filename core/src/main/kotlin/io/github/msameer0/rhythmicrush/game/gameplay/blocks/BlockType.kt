package io.github.msameer0.rhythmicrush.game.gameplay.blocks

enum class BlockType(
    /**
     * Represents the different types of blocks available in the game.
     * Each type is associated with a unique identifier and a specific texture name
     * used for rendering the block's visual appearance.
     */
    val id: Int, val textureName: String
) {
    DEFAULT(0, "default"),
    PILLAR(1, "pillar"),
    CORNER_LEFT(2, "corner_left"),
    CORNER_RIGHT(3, "corner_right"),
    TOP_DEFAULT(4, "top_default"),
    LEFT_DEFAULT(5, "left_default"),
    RIGHT_DEFAULT(6, "right_default"),
    DEFAULT_NO_OUTLINE(7, "default_no_outline");

}
