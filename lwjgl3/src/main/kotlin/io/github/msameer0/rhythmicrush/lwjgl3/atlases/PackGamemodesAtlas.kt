package io.github.msameer0.rhythmicrush.lwjgl3.atlases

import com.badlogic.gdx.tools.texturepacker.TexturePacker

object PackGamemodesAtlas {
    @JvmStatic
    fun main(args: Array<String>) {
        val settings = TexturePacker.Settings()
        settings.maxWidth = 2048
        settings.maxHeight = 2048
        settings.edgePadding = true
        settings.duplicatePadding = true

        AtlasPackingUtils.packAllQualities(
            "textures_to_put_in_atlases/objects/gamemodes/cube",
            "assets/game/objects",
            "cubes",
            settings
        )

        AtlasPackingUtils.packAllQualities(
            "textures_to_put_in_atlases/objects/gamemodes/ship",
            "assets/game/objects",
            "ships",
            settings
        )
    }
}
