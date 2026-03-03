package io.github.msameer0.rhythmicrush.lwjgl3.atlases;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class PackLevelSelectAtlas {
    public static void main(String[] args) {

        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;       // max page width
        settings.maxHeight = 2048;      // max page height
        settings.edgePadding = true;    // padding around images
        settings.duplicatePadding = true;
        settings.scale = new float[]{0.65f};         // scale all images to 65% of original size

        TexturePacker.process(settings,
            "assets/level_select", // input folder
            "assets/level_select_atlases",              // output folder
            "level_select"         // atlas name
        );

        System.out.println("Level select atlas packed!");
    }
}
