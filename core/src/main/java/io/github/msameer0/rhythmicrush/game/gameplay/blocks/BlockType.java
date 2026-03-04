package io.github.msameer0.rhythmicrush.game.gameplay.blocks;

public enum BlockType {
    DEFAULT(0, "default");

    public final int id;
    public final String textureName;

    BlockType(int id, String textureName) {
        this.id = id;
        this.textureName = textureName;
    }
}
