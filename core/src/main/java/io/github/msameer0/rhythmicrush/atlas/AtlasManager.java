package io.github.msameer0.rhythmicrush.atlas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

/**
 * Manages the loading, access, and disposal of all {@link TextureAtlas} resources used throughout the game.
 * <p>
 */
public class AtlasManager {

    private final TextureAtlas menuAtlas;
    private final TextureAtlas levelSelectAtlas;
    private final TextureAtlas blocksAtlas;
    private final TextureAtlas spikesAtlas;
    private final TextureAtlas gamemodesAtlas;
    private final TextureAtlas portalsAtlas;

    public AtlasManager() {
        Gdx.app.log("AtlasManager", "Loading texture atlases...");
        menuAtlas = new TextureAtlas(Gdx.files.internal("menu.atlas"));
        levelSelectAtlas = new TextureAtlas(Gdx.files.internal("level_select_atlases/level_select.atlas"));
        blocksAtlas = new TextureAtlas(Gdx.files.internal("game/objects/blocks.atlas"));
        spikesAtlas = new TextureAtlas(Gdx.files.internal("game/objects/spikes.atlas"));
        gamemodesAtlas = new TextureAtlas(Gdx.files.internal("game/objects/gamemodes.atlas"));
        portalsAtlas = new TextureAtlas(Gdx.files.internal("game/objects/portals.atlas"));
        Gdx.app.log("AtlasManager", "All texture atlases loaded.");
    }

    public TextureAtlas getMenuAtlas() {
        return menuAtlas;
    }

    public TextureAtlas getLevelSelectAtlas() {
        return levelSelectAtlas;
    }

    public TextureAtlas getBlocksAtlas() {
        return blocksAtlas;
    }

    public TextureAtlas getSpikesAtlas() {
        return spikesAtlas;
    }

    public TextureAtlas getPortalsAtlas() {
        return portalsAtlas;
    }

    public TextureAtlas getGamemodesAtlas() {
        return gamemodesAtlas;
    }

    public void dispose() {
        Gdx.app.log("AtlasManager", "Disposing texture atlases...");
        menuAtlas.dispose();
        levelSelectAtlas.dispose();
        blocksAtlas.dispose();
        spikesAtlas.dispose();
        gamemodesAtlas.dispose();
        portalsAtlas.dispose();
        Gdx.app.log("AtlasManager", "All texture atlases disposed.");
    }
}
