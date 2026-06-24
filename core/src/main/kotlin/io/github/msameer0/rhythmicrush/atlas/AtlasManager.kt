package io.github.msameer0.rhythmicrush.atlas

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import io.github.msameer0.rhythmicrush.settings.SettingsManager

/**
 * Manages the loading and disposal of texture atlases used throughout the game.
 */
class AtlasManager(quality: SettingsManager.TextureQuality) {
    val menuAtlas: TextureAtlas
    val levelSelectAtlas: TextureAtlas
    val blocksAtlas: TextureAtlas
    val spikesAtlas: TextureAtlas
//    val portalsAtlas: TextureAtlas
    val portalsBackAtlas: TextureAtlas
    val portalsFrontAtlas: TextureAtlas
    val orbsAtlas: TextureAtlas
    val padsAtlas: TextureAtlas
    val cubesAtlas: TextureAtlas
    val shipsAtlas: TextureAtlas

    init {
        Gdx.app.log("AtlasManager", "Loading texture atlases with quality: $quality")
        val suffix = quality.suffix

        menuAtlas = TextureAtlas(Gdx.files.internal("menu$suffix.atlas"))
        levelSelectAtlas =
            TextureAtlas(Gdx.files.internal("level_select_atlases/level_select$suffix.atlas"))
        blocksAtlas = TextureAtlas(Gdx.files.internal("game/objects/blocks$suffix.atlas"))
        spikesAtlas = TextureAtlas(Gdx.files.internal("game/objects/spikes$suffix.atlas"))
//        portalsAtlas = TextureAtlas(Gdx.files.internal("game/objects/portals$suffix.atlas"))
        portalsBackAtlas = TextureAtlas(Gdx.files.internal("game/objects/portals_back$suffix.atlas"))
        portalsFrontAtlas = TextureAtlas(Gdx.files.internal("game/objects/portals_front$suffix.atlas"))
        orbsAtlas = TextureAtlas(Gdx.files.internal("game/objects/orbs$suffix.atlas"))
        padsAtlas = TextureAtlas(Gdx.files.internal("game/objects/pads$suffix.atlas"))
        cubesAtlas = TextureAtlas(Gdx.files.internal("game/objects/cubes$suffix.atlas"))
        shipsAtlas = TextureAtlas(Gdx.files.internal("game/objects/ships$suffix.atlas"))
        Gdx.app.log("AtlasManager", "All texture atlases loaded.")
    }

    fun dispose() {
        Gdx.app.log("AtlasManager", "Disposing texture atlases...")
        menuAtlas.dispose()
        levelSelectAtlas.dispose()
        blocksAtlas.dispose()
        spikesAtlas.dispose()
//        portalsAtlas.dispose()
        portalsBackAtlas.dispose()
        portalsFrontAtlas.dispose()
        orbsAtlas.dispose()
        padsAtlas.dispose()
        cubesAtlas.dispose()
        shipsAtlas.dispose()
        Gdx.app.log("AtlasManager", "All texture atlases disposed.")
    }
}
