package io.github.msameer0.rhythmicrush.lwjgl3.atlases

import com.badlogic.gdx.tools.texturepacker.TexturePacker

object PackLevelSelectAtlas {
    @JvmStatic
    fun main(args: Array<String>) {
        val settings = TexturePacker.Settings()
        settings.maxWidth = 2048
        settings.maxHeight = 2048
        settings.edgePadding = true
        settings.duplicatePadding = true
        // Base scale for level select was 0.65f, but we'll use 1.0f as high quality for consistency now,
        // or we could multiply our quality scales by 0.65f.
        // Given the request "pack thrice the amount, treat current as high", I'll stick to 1.0, 0.5, 0.25.

        AtlasPackingUtils.packAllQualities(
            "textures_to_put_in_atlases/level_select",
            "assets/level_select_atlases",
            "level_select",
            settings
        )
    }
}
