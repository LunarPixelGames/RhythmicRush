package io.github.msameer0.rhythmicrush;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

/**
 * Loads all atlases once at app startup and keeps them for the full lifetime.
 * Screens and renderers pull regions from here — never load or dispose atlases themselves.
 */
public class AtlasManager {

    private final TextureAtlas menuAtlas;
    private final TextureAtlas levelSelectAtlas;
    private final TextureAtlas blocksAtlas;
    private final TextureAtlas spikesAtlas;
    private final TextureAtlas gamemodesAtlas;

    public AtlasManager() {
        menuAtlas        = new TextureAtlas(Gdx.files.internal("menu.atlas"));
        levelSelectAtlas = new TextureAtlas(Gdx.files.internal("level_select_atlases/level_select.atlas"));
        blocksAtlas      = new TextureAtlas(Gdx.files.internal("game/objects/blocks.atlas"));
        spikesAtlas      = new TextureAtlas(Gdx.files.internal("game/objects/spikes.atlas"));
        gamemodesAtlas = new TextureAtlas(Gdx.files.internal("game/objects/gamemodes.atlas"));
    }

    public TextureAtlas getMenuAtlas()        { return menuAtlas; }
    public TextureAtlas getLevelSelectAtlas() { return levelSelectAtlas; }
    public TextureAtlas getBlocksAtlas()      { return blocksAtlas; }
    public TextureAtlas getSpikesAtlas()      { return spikesAtlas; }

    public TextureAtlas getGamemodesAtlas() {
        return gamemodesAtlas;
    }

    public void dispose() {
        menuAtlas.dispose();
        levelSelectAtlas.dispose();
        blocksAtlas.dispose();
        spikesAtlas.dispose();
        gamemodesAtlas.dispose();
    }
}
