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
        var gen: FreeTypeFontGenerator? = null
        try {
            if (file == null) throw IllegalStateException("No font candidate found")
            gen = FreeTypeFontGenerator(file)
            val p = FreeTypeFontParameter()
            p.magFilter = Texture.TextureFilter.Linear
            p.minFilter = Texture.TextureFilter.MipMapLinearLinear
            p.genMipMaps = true
            for (i in SIZES.indices) {
                p.size = SIZES[i]
                tempFonts[i] = gen.generateFont(p)
            }
        } catch (e: Exception) {
            Gdx.app.error("FontManager", "Could not load font: " + e.message)
            for (i in SIZES.indices) {
                if (tempFonts[i] == null) tempFonts[i] = BitmapFont()
            }
        } finally {
            gen?.dispose()
        }
        @Suppress("UNCHECKED_CAST")
        return tempFonts as Array<BitmapFont>
    }

    fun dispose() {
        Gdx.app.log("FontManager", "Disposing fonts...")
        for (f in titleFonts) f.dispose()
        for (f in bodyFonts) f.dispose()
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
        var best = 0
        var bestDiff: Int = abs(SIZES[0] - size)
        for (i in 1..<SIZES.size) {
            val diff: Int = abs(SIZES[i] - size)
            if (diff < bestDiff) {
                bestDiff = diff
                best = i
            }
        }
        return family[best]
    }

    companion object {
        const val SIZE_SMALL: Int = 22
        const val SIZE_MEDIUM: Int = 28
        const val SIZE_LARGE: Int = 32
        const val SIZE_XLARGE: Int = 48

        private val SIZES = intArrayOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE, SIZE_XLARGE)
    }
}
