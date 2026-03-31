package io.github.msameer0.rhythmicrush.lwjgl3.atlases

import com.badlogic.gdx.tools.texturepacker.TexturePacker

object PackSpikesAtlas {
    @JvmStatic
    fun main(args: Array<String>) {
        val settings = TexturePacker.Settings()
        settings.maxWidth = 2048
        settings.maxHeight = 2048
        settings.edgePadding = true
        settings.duplicatePadding = true

        TexturePacker.process(
            settings,
            "textures_to_put_in_atlases/objects/spikes",  // input
            "assets/game/objects",  // output
            "spikes" // atlas name → spikes.atlas
        )
    }
}
