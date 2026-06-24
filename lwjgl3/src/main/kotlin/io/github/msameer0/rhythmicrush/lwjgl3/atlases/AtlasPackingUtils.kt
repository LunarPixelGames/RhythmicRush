package io.github.msameer0.rhythmicrush.lwjgl3.atlases

import com.badlogic.gdx.tools.texturepacker.TexturePacker

object AtlasPackingUtils {
    data class QualitySettings(val suffix: String, val scale: Float)

    val qualities = listOf(
        QualitySettings("", 1.0f),      // High
        QualitySettings("_medium", 0.5f), // Medium
        QualitySettings("_low", 0.25f)    // Low
    )

    fun packAllQualities(
        inputDir: String,
        outputDir: String,
        atlasName: String,
        baseSettings: TexturePacker.Settings = TexturePacker.Settings()
    ) {
        qualities.forEach { quality ->
            val settings = TexturePacker.Settings(baseSettings)
            settings.scale = floatArrayOf(quality.scale)

            // Adjust max size for lower qualities if needed,
            // but usually we want to keep it consistent or proportional.
            // TexturePacker will handle the scaling of input images.

            TexturePacker.process(
                settings,
                inputDir,
                outputDir,
                atlasName + quality.suffix
            )
        }
    }
}
