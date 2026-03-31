package io.github.msameer0.rhythmicrush.lwjgl3.atlases

import com.badlogic.gdx.tools.texturepacker.TexturePacker

object PackMenuAtlas {
    @JvmStatic
    fun main(args: Array<String>) {
        val settings = TexturePacker.Settings()
        settings.maxWidth = 2048
        settings.maxHeight = 2048
        settings.edgePadding = true // keep optional padding
        settings.duplicatePadding = true

        TexturePacker.process(
            settings,
            "textures_to_put_in_atlases/menu",  // input folder
            "assets",  // output folder
            "menu" // atlas name
        )
    }
}
