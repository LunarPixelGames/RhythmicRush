package io.github.msameer0.rhythmicrush.lwjgl3.atlases

import com.badlogic.gdx.tools.texturepacker.TexturePacker

object PackLevelSelectAtlas {
    @JvmStatic
    fun main(args: Array<String>) {
        val settings = TexturePacker.Settings()
        settings.maxWidth = 2048 // max page width
        settings.maxHeight = 2048 // max page height
        settings.edgePadding = true // padding around images
        settings.duplicatePadding = true
        settings.scale = floatArrayOf(0.65f) // scale all images to 65% of original size

        TexturePacker.process(
            settings,
            "textures_to_put_in_atlases/level_select",  // input folder
            "assets/level_select_atlases",  // output folder
            "level_select" // atlas name
        )

        println("Level select atlas packed!")
    }
}
