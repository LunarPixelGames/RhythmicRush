package io.github.msameer0.rhythmicrush.font

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import kotlin.math.abs

class FontManager {
    private val fonts: Array<BitmapFont?>

    init {
        Gdx.app.log("FontManager", "Initializing fonts...")
        fonts = arrayOfNulls<BitmapFont>(SIZES.size)
        var gen: FreeTypeFontGenerator? = null
        try {
            gen = FreeTypeFontGenerator(Gdx.files.internal("fonts/zendots-regular.ttf"))
            val p =
                FreeTypeFontParameter()
            p.magFilter = Texture.TextureFilter.Linear
            p.minFilter = Texture.TextureFilter.MipMapLinearLinear
            p.genMipMaps = true
            for (i in SIZES.indices) {
                p.size = SIZES[i]
                fonts[i] = gen.generateFont(p)
            }
            Gdx.app.log("FontManager", "Fonts initialized successfully.")
        } catch (e: Exception) {
            Gdx.app.error("FontManager", "Could not load font: " + e.message)
            var i = 0
            while (i < SIZES.size) {
                if (fonts[i] == null) fonts[i] = BitmapFont()
                i++
            }
        } finally {
            gen?.dispose()
        }
    }

    fun dispose() {
        Gdx.app.log("FontManager", "Disposing fonts...")
        for (f in fonts) f?.dispose()
        Gdx.app.log("FontManager", "Fonts disposed.")
    }

    fun get(size: Int): BitmapFont? {
        var best = 0
        var bestDiff: Int = abs(SIZES[0] - size)
        for (i in 1..<SIZES.size) {
            val diff: Int = abs(SIZES[i] - size)
            if (diff < bestDiff) {
                bestDiff = diff
                best = i
            }
        }
        return fonts[best]
    }

    companion object {
        const val SIZE_SMALL: Int = 22
        const val SIZE_MEDIUM: Int = 28
        const val SIZE_LARGE: Int = 32
        const val SIZE_XLARGE: Int = 48

        private val SIZES = intArrayOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE, SIZE_XLARGE)
    }
}
