package io.github.msameer0.rhythmicrush.game.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import io.github.msameer0.rhythmicrush.game.level.PatternShape
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic, camera-streamed decorative slabs. Only geometry and theme-relative
 * shading live here; the game's existing color state remains the source of truth.
 */
class ProceduralBackground {
    private val farLayer = BackgroundSlabLayer(
        seedSalt = 0x4f1bbcdc,
        parallaxFactor = 0.12f,
        widthRange = 520f..980f,
        heightRange = 230f..430f,
        gapRange = 90f..290f,
        alpha = 0.28f,
        darkenAmount = 0.20f,
        bandCount = 3
    )
    private val nearLayer = BackgroundSlabLayer(
        seedSalt = 0x2c9277b5,
        parallaxFactor = 0.22f,
        widthRange = 380f..800f,
        heightRange = 180f..350f,
        gapRange = 65f..240f,
        alpha = 0.38f,
        darkenAmount = 0.30f,
        bandCount = 4
    )

    fun render(
        renderer: ShapeRenderer,
        shape: PatternShape,
        baseColor: Color,
        seed: Int,
        scrollX: Float,
        left: Float,
        bottom: Float,
        width: Float,
        height: Float
    ) {
        renderer.color = baseColor
        renderer.rect(left, bottom, width, height)
        farLayer.render(renderer, shape, baseColor, seed, scrollX, left, bottom, width, height)
        nearLayer.render(renderer, shape, baseColor, seed, scrollX, left, bottom, width, height)
    }
}

class BackgroundSlabLayer(
    private val seedSalt: Int,
    private val parallaxFactor: Float,
    private val widthRange: ClosedFloatingPointRange<Float>,
    private val heightRange: ClosedFloatingPointRange<Float>,
    private val gapRange: ClosedFloatingPointRange<Float>,
    private val alpha: Float,
    private val darkenAmount: Float,
    private val bandCount: Int
) {
    private val slabColor = Color()

    fun render(
        renderer: ShapeRenderer,
        shape: PatternShape,
        baseColor: Color,
        levelSeed: Int,
        scrollX: Float,
        screenLeft: Float,
        screenBottom: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val parallaxScroll = scrollX * parallaxFactor
        val visibleStart = screenLeft + parallaxScroll - CHUNK_WIDTH
        val visibleEnd = screenLeft + screenWidth + parallaxScroll + CHUNK_WIDTH
        val firstChunk = floor(visibleStart / CHUNK_WIDTH).toInt()
        val lastChunk = ceil(visibleEnd / CHUNK_WIDTH).toInt()
        val bandHeight = screenHeight / bandCount

        setDarkenedColor(slabColor, baseColor, darkenAmount, alpha)
        renderer.color = slabColor

        for (band in 0 until bandCount) {
            for (chunk in firstChunk..lastChunk) {
                val random = DecorationRandom(mixSeed(levelSeed, seedSalt, band, chunk))
                var x = chunk * CHUNK_WIDTH - random.range(80f, 360f)
                val chunkEnd = (chunk + 1) * CHUNK_WIDTH + widthRange.endInclusive

                while (x < chunkEnd) {
                    val gap = random.range(gapRange)
                    x += gap

                    // Missing panels create breaks without destroying the loose row structure.
                    if (random.nextFloat() < 0.17f) {
                        x += random.range(widthRange) * 0.45f
                        continue
                    }

                    var slabWidth = random.range(widthRange)
                    var slabHeight = random.range(heightRange)
                    if (shape == PatternShape.SQUARE) {
                        val size = min(slabWidth, slabHeight * 1.35f)
                        slabWidth = size
                        slabHeight = size
                    }

                    val bandBase = screenBottom + band * bandHeight
                    val yJitter = random.range(-bandHeight * 0.32f, bandHeight * 0.24f)
                    val slabY = bandBase + yJitter
                    val drawX = x - parallaxScroll

                    drawDecorativeShape(renderer, shape, drawX, slabY, slabWidth, slabHeight)
                    x += slabWidth
                }
            }
        }
    }

    companion object {
        private const val CHUNK_WIDTH = 2200f
    }
}

class ProceduralGroundDecoration {
    private val panelColor = Color()

    fun render(
        renderer: ShapeRenderer,
        shape: PatternShape,
        baseColor: Color,
        scrollX: Float,
        regionX: Float,
        regionY: Float,
        regionWidth: Float,
        regionHeight: Float,
        panelsExtendDown: Boolean
    ) {
        if (regionWidth <= 0f || regionHeight <= 0f) return

        renderer.color = baseColor
        renderer.rect(regionX, regionY, regionWidth, regionHeight)

        setDarkenedColor(panelColor, baseColor, 0.34f, 0.52f)
        renderer.color = panelColor

        val panelWidth = if (shape == PatternShape.SQUARE) 420f else 520f
        val panelGap = 10f
        val panelStep = panelWidth + panelGap
        val panelHeight = if (shape == PatternShape.SQUARE) {
            panelWidth
        } else {
            max(regionHeight * 1.8f, 560f)
        }
        val visibleStart = regionX + scrollX - panelStep
        val visibleEnd = regionX + regionWidth + scrollX + panelStep
        val firstPanel = floor(visibleStart / panelStep).toInt()
        val lastPanel = ceil(visibleEnd / panelStep).toInt()

        for (panel in firstPanel..lastPanel) {
            val drawX = panel * panelStep - scrollX
            val rawY = if (panelsExtendDown) {
                regionY + regionHeight - panelHeight
            } else {
                regionY
            }

            drawClippedGroundShape(
                renderer, shape,
                drawX, rawY, panelWidth, panelHeight,
                regionX, regionY, regionWidth, regionHeight
            )
        }
    }
}

private fun drawClippedGroundShape(
    renderer: ShapeRenderer,
    shape: PatternShape,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    regionX: Float,
    regionY: Float,
    regionWidth: Float,
    regionHeight: Float
) {
    val clippedX = max(x, regionX)
    val clippedY = max(y, regionY)
    val clippedRight = min(x + width, regionX + regionWidth)
    val clippedTop = min(y + height, regionY + regionHeight)
    if (clippedRight <= clippedX || clippedTop <= clippedY) return

    if (shape == PatternShape.RECTANGLE || shape == PatternShape.SQUARE) {
        renderer.rect(clippedX, clippedY, clippedRight - clippedX, clippedTop - clippedY)
        return
    }

    // Non-rectangular selections remain supported, but stay fully inside the ground fill.
    val insetX = max(x, regionX)
    val insetY = max(y, regionY)
    val insetWidth = min(x + width, regionX + regionWidth) - insetX
    val insetHeight = min(y + height, regionY + regionHeight) - insetY
    if (insetWidth > 0f && insetHeight > 0f) {
        drawDecorativeShape(renderer, shape, insetX, insetY, insetWidth, insetHeight)
    }
}

private fun drawDecorativeShape(
    renderer: ShapeRenderer,
    shape: PatternShape,
    x: Float,
    y: Float,
    width: Float,
    height: Float
) {
    when (shape) {
        PatternShape.SQUARE, PatternShape.RECTANGLE -> renderer.rect(x, y, width, height)
        PatternShape.TRIANGLE -> renderer.triangle(
            x + width / 2f, y + height,
            x, y,
            x + width, y
        )
        PatternShape.CIRCLE -> renderer.ellipse(x, y, width, height, 28)
        PatternShape.HEXAGON -> drawHexagon(renderer, x + width / 2f, y + height / 2f, width / 2f, height / 2f)
    }
}

private fun drawHexagon(renderer: ShapeRenderer, centerX: Float, centerY: Float, radiusX: Float, radiusY: Float) {
    var previousX = centerX + MathUtils.cosDeg(30f) * radiusX
    var previousY = centerY + MathUtils.sinDeg(30f) * radiusY
    for (i in 0 until 6) {
        val angle = 60f * (i + 1) + 30f
        val nextX = centerX + MathUtils.cosDeg(angle) * radiusX
        val nextY = centerY + MathUtils.sinDeg(angle) * radiusY
        renderer.triangle(centerX, centerY, previousX, previousY, nextX, nextY)
        previousX = nextX
        previousY = nextY
    }
}

private fun setDarkenedColor(out: Color, base: Color, amount: Float, alpha: Float) {
    out.set(
        base.r * (1f - amount),
        base.g * (1f - amount),
        base.b * (1f - amount),
        base.a * alpha
    )
}

private fun mixSeed(levelSeed: Int, salt: Int, band: Int, chunk: Int): Int {
    var value = levelSeed xor salt
    value = value * -0x7a143595 + band * 0x632be59b
    value = value xor (chunk * -0x61c88647)
    value = value xor (value ushr 16)
    value *= -0x7a143595
    return value xor (value ushr 15)
}

private class DecorationRandom(seed: Int) {
    private var state = if (seed == 0) 0x6d2b79f5 else seed

    fun nextFloat(): Float {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        state = x
        return (x ushr 8) / 16777216f
    }

    fun range(range: ClosedFloatingPointRange<Float>): Float = range(range.start, range.endInclusive)

    fun range(min: Float, max: Float): Float = min + (max - min) * nextFloat()
}
