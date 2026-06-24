package io.github.msameer0.rhythmicrush.lwjgl3.atlases

import com.badlogic.gdx.tools.texturepacker.TexturePacker

object PackBlocksAtlas {
    @JvmStatic
    fun main(args: Array<String>) {
        val settings = TexturePacker.Settings()
        settings.maxWidth = 2048
        settings.maxHeight = 2048
        settings.edgePadding = true
        settings.duplicatePadding = true

        AtlasPackingUtils.packAllQualities(
            "textures_to_put_in_atlases/objects/blocks",
            "assets/game/objects",
            "blocks",
            settings
        )
    }
}
