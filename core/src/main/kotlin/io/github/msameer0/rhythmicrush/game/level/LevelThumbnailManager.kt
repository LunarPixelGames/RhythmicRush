package io.github.msameer0.rhythmicrush.game.level

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Array
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Loads and prepares level-select thumbnails once during initial game loading.
 */
class LevelThumbnailManager(levels: Array<LevelData?>) {
    private val thumbnails = Array<Texture?>()

    companion object {
        private const val OUTPUT_WIDTH = 768
        private const val OUTPUT_ASPECT = 1.325f

        // 0 = centered, positive values move the crop left, negative values move it right.
        private const val THUMBNAIL_CROP_LEFT_PERCENT = 20.5f
    }

    init {
        for (index in 0 until levels.size) {
            thumbnails.add(loadTintedThumbnail(index, difficultyColor(levels[index]?.difficulty)))
        }
    }

    operator fun get(index: Int): Texture? {
        return if (index in 0 until thumbnails.size) thumbnails[index] else null
    }

    private fun loadTintedThumbnail(index: Int, tint: Color): Texture? {
        val file = arrayOf("png", "jpg", "jpeg")
            .asSequence()
            .map { Gdx.files.internal("level_thumbnails/$index.$it") }
            .firstOrNull { it.exists() }
            ?: return null

        return try {
            val source = Pixmap(file)
            val outputHeight = (OUTPUT_WIDTH / OUTPUT_ASPECT).toInt().coerceAtLeast(1)
            val tinted = Pixmap(OUTPUT_WIDTH, outputHeight, Pixmap.Format.RGBA8888)
            val scale = max(
                OUTPUT_WIDTH.toFloat() / source.width,
                outputHeight.toFloat() / source.height
            )
            val visibleSourceWidth = OUTPUT_WIDTH / scale
            val visibleSourceHeight = outputHeight / scale
            val availableCropX = (source.width - visibleSourceWidth).coerceAtLeast(0f)
            val centeredCropX = availableCropX * 0.5f
            val cropShift = availableCropX * (THUMBNAIL_CROP_LEFT_PERCENT / 100f)
            val cropX = (centeredCropX - cropShift).coerceIn(0f, availableCropX)
            val cropY = ((source.height - visibleSourceHeight) * 0.5f).coerceAtLeast(0f)
            val cornerRadius = outputHeight * 0.075f
            val diagonalCut = OUTPUT_WIDTH * 0.14f

            for (py in 0 until outputHeight) {
                for (px in 0 until OUTPUT_WIDTH) {
                    val sourceX = (cropX + px / scale).toInt().coerceIn(0, source.width - 1)
                    val sourceY = (cropY + py / scale).toInt().coerceIn(0, source.height - 1)
                    val rgba = source.getPixel(sourceX, sourceY)
                    val red = (rgba ushr 24) and 0xff
                    val green = (rgba ushr 16) and 0xff
                    val blue = (rgba ushr 8) and 0xff
                    var alpha = rgba and 0xff

                    if (px < cornerRadius) {
                        val cornerCenterY = when {
                            py < cornerRadius -> cornerRadius
                            py > outputHeight - cornerRadius -> outputHeight - cornerRadius
                            else -> -1f
                        }
                        if (cornerCenterY >= 0f) {
                            val dx = px - cornerRadius
                            val dy = py - cornerCenterY
                            val coverage =
                                (cornerRadius + 0.75f - sqrt(dx * dx + dy * dy))
                                    .coerceIn(0f, 1f)
                            alpha = (alpha * coverage).toInt()
                        }
                    }

                    val diagonalEdge =
                        OUTPUT_WIDTH - diagonalCut * (py / (outputHeight - 1f).coerceAtLeast(1f))
                    alpha = (alpha * (diagonalEdge + 0.75f - px).coerceIn(0f, 1f)).toInt()

                    val gray = (red * 0.299f + green * 0.587f + blue * 0.114f) / 255f
                    val outRed = (gray * (0.22f + tint.r * 0.78f) * 255f).toInt().coerceIn(0, 255)
                    val outGreen = (gray * (0.22f + tint.g * 0.78f) * 255f).toInt().coerceIn(0, 255)
                    val outBlue = (gray * (0.22f + tint.b * 0.78f) * 255f).toInt().coerceIn(0, 255)
                    tinted.drawPixel(
                        px,
                        py,
                        (outRed shl 24) or (outGreen shl 16) or (outBlue shl 8) or alpha
                    )
                }
            }

            source.dispose()
            Texture(tinted).also {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                tinted.dispose()
            }
        } catch (exception: Exception) {
            Gdx.app.error("LevelThumbnailManager", "Could not load thumbnail $index: ${exception.message}")
            null
        }
    }

    private fun difficultyColor(value: String?) = when (value?.lowercase()) {
        "easy" -> Color.valueOf("55A7FF")
        "normal" -> Color.valueOf("62D96B")
        "hard" -> Color.valueOf("FFD84A")
        "insane" -> Color.valueOf("B36CFF")
        "extreme", "demon" -> Color.valueOf("FF5252")
        else -> Color.valueOf("62D96B")
    }

    fun dispose() {
        thumbnails.forEach { it?.dispose() }
        thumbnails.clear()
    }
}
