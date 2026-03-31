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

        TexturePacker.process(
            settings,
            "textures_to_put_in_atlases/objects/gamemodes",  // input folder (one PNG per BlockType.textureName)
            "assets/game/objects",  // output folder
            "gamemodes" // atlas name → blocks.atlas + blocks.png
        )
    }
}
