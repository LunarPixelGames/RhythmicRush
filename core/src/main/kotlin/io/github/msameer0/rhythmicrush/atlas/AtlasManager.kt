package io.github.msameer0.rhythmicrush.atlas

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureAtlas

/**
 * Manages the loading, access, and disposal of all [TextureAtlas] resources used throughout the game.
 *
 *
 */
class AtlasManager {
    val menuAtlas: TextureAtlas
    val levelSelectAtlas: TextureAtlas
    val blocksAtlas: TextureAtlas
    val spikesAtlas: TextureAtlas
    val gamemodesAtlas: TextureAtlas
    val portalsAtlas: TextureAtlas

    init {
        Gdx.app.log("AtlasManager", "Loading texture atlases...")
        menuAtlas = TextureAtlas(Gdx.files.internal("menu.atlas"))
        levelSelectAtlas =
            TextureAtlas(Gdx.files.internal("level_select_atlases/level_select.atlas"))
        blocksAtlas = TextureAtlas(Gdx.files.internal("game/objects/blocks.atlas"))
        spikesAtlas = TextureAtlas(Gdx.files.internal("game/objects/spikes.atlas"))
        gamemodesAtlas = TextureAtlas(Gdx.files.internal("game/objects/gamemodes.atlas"))
        portalsAtlas = TextureAtlas(Gdx.files.internal("game/objects/portals.atlas"))
        Gdx.app.log("AtlasManager", "All texture atlases loaded.")
    }

    fun dispose() {
        Gdx.app.log("AtlasManager", "Disposing texture atlases...")
        menuAtlas.dispose()
        levelSelectAtlas.dispose()
        blocksAtlas.dispose()
        spikesAtlas.dispose()
        gamemodesAtlas.dispose()
        portalsAtlas.dispose()
        Gdx.app.log("AtlasManager", "All texture atlases disposed.")
    }
}
