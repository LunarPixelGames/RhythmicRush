package io.github.msameer0.rhythmicrush.lwjgl3;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class PackMenuAtlas {
    public static void main(String[] args) {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.edgePadding = true; // keep optional padding
        settings.duplicatePadding = true;

        TexturePacker.process(settings,
            "assets/menu", // input folder
            "assets",      // output folder
            "menu"         // atlas name
        );
    }
}
