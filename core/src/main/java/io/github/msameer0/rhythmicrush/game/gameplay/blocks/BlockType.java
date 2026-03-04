package io.github.msameer0.rhythmicrush.game.gameplay.blocks;

public enum BlockType {
    DEFAULT(0, "default"),
    PILLAR(1, "pillar"),
    CORNER_LEFT(2, "corner_left"),
    CORNER_RIGHT(3, "corner_right"),
    TOP_DEFAULT(4, "top_default"),
    LEFT_DEFAULT(5, "left_default"),
    RIGHT_DEFAULT(6, "right_default"),
    DEFAULT_NO_OUTLINE(7, "default_no_outline")
    ;

    public final int id;
    public final String textureName;

    BlockType(int id, String textureName) {
        this.id = id;
        this.textureName = textureName;
    }
}
