package io.github.msameer0.rhythmicrush.font

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import kotlin.math.abs

/**
 * Handles initialization, caching, and retrieval of BitmapFonts of various sizes using FreeType.
 */
class FontManager {
    private val titleFonts: Array<BitmapFont>
    private val bodyFonts: Array<BitmapFont>

    init {
        Gdx.app.log("FontManager", "Initializing fonts...")
        titleFonts = generateFamily(
            arrayOf("fonts/Orbitron-Bold.ttf", "fonts/Orbitron-SemiBold.ttf", "fonts/zendots-regular.ttf")
        )
        bodyFonts = generateFamily(
            arrayOf("fonts/Rajdhani-SemiBold.ttf", "fonts/Rajdhani-Medium.ttf", "fonts/zendots-regular.ttf")
        )
        Gdx.app.log("FontManager", "Fonts initialized successfully.")
    }

    private fun generateFamily(candidates: Array<String>): Array<BitmapFont> {
        val tempFonts = arrayOfNulls<BitmapFont>(SIZES.size)
        val file = candidates.firstNotNullOfOrNull { path ->
            Gdx.files.internal(path).takeIf { it.exists() }
        }
        var generator: FreeTypeFontGenerator? = null
        try {
            if (file == null) throw IllegalStateException("No font candidate found")
            generator = FreeTypeFontGenerator(file)
            val parameters = FreeTypeFontParameter()
            parameters.magFilter = Texture.TextureFilter.Linear
            parameters.minFilter = Texture.TextureFilter.MipMapLinearLinear
            parameters.genMipMaps = true
            for (sizeIndex in SIZES.indices) {
                parameters.size = SIZES[sizeIndex]
                tempFonts[sizeIndex] = generator.generateFont(parameters)
            }
        } catch (exception: Exception) {
            Gdx.app.error("FontManager", "Could not load font: ${exception.message}")
            for (sizeIndex in SIZES.indices) {
                if (tempFonts[sizeIndex] == null) tempFonts[sizeIndex] = BitmapFont()
            }
        } finally {
            generator?.dispose()
        }
        @Suppress("UNCHECKED_CAST")
        return tempFonts as Array<BitmapFont>
    }

    fun dispose() {
        Gdx.app.log("FontManager", "Disposing fonts...")
        for (font in titleFonts) font.dispose()
        for (font in bodyFonts) font.dispose()
        Gdx.app.log("FontManager", "Fonts disposed.")
    }

    fun get(size: Int): BitmapFont {
        return closest(bodyFonts, size)
    }

    fun getTitle(size: Int): BitmapFont {
        return closest(titleFonts, size)
    }

    fun getBody(size: Int): BitmapFont {
        return closest(bodyFonts, size)
    }

    private fun closest(family: Array<BitmapFont>, size: Int): BitmapFont {
        var closestIndex = 0
        var smallestDifference = abs(SIZES[0] - size)
        for (sizeIndex in 1..<SIZES.size) {
            val difference = abs(SIZES[sizeIndex] - size)
            if (difference < smallestDifference) {
                smallestDifference = difference
                closestIndex = sizeIndex
            }
        }
        return family[closestIndex]
    }

    companion object {
        const val SIZE_SMALL: Int = 22
        const val SIZE_MEDIUM: Int = 28
        const val SIZE_LARGE: Int = 32
        const val SIZE_XLARGE: Int = 48

        private val SIZES = intArrayOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE, SIZE_XLARGE)
    }
}
