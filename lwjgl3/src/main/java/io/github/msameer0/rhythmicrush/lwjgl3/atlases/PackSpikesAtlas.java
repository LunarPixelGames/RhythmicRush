package io.github.msameer0.rhythmicrush.lwjgl3.atlases;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class PackSpikesAtlas {
    public static void main(String[] args) {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.edgePadding = true;
        settings.duplicatePadding = true;

        TexturePacker.process(settings,
            "assets/game/objects/spikes",  // input
            "assets/game/objects",         // output
            "spikes"                       // atlas name → spikes.atlas
        );
    }
}
