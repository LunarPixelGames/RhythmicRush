package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.font.FontManager
import io.github.msameer0.rhythmicrush.game.level.LevelData
import io.github.msameer0.rhythmicrush.ui.AnimatedButton
import io.github.msameer0.rhythmicrush.ui.UI
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign

/** Focused, responsive neon level browser. */
class LevelSelectScreen @JvmOverloads constructor(
    game: RhythmicRushGame,
    initialIndex: Int = 0
) : AbstractScreen(game) {
    private val backgroundColor = Color.valueOf("211126")
    private val panelColor = Color.valueOf("25263A")
    private val secondaryColor = Color.valueOf("2D2E46")
    private val subtleBorder = Color.valueOf("4B4163")
    private val primaryColor = Color.valueOf("B8F28A")
    private val darkOutline = Color(0.035f, 0.035f, 0.055f, 0.95f)
    private val statBlue = Color.valueOf("6FA8E8")
    private val playTextColor = Color(0.04f, 0.05f, 0.09f, 1f)
    private val progressTrackColor = Color(0.10f, 0.10f, 0.15f, 0.72f)
    private val thumbnailShade = Color(0.055f, 0.055f, 0.085f, 1f)
    private val touch = Vector2()

    private val levels = Array<LevelData>()
    private val thumbnails = Array<Texture?>()
    private var selectedLevel = initialIndex
    private lateinit var titleFont: BitmapFont
    private lateinit var bodyFont: BitmapFont
    private val layout = GlyphLayout()
    private lateinit var shapes: ShapeRenderer
    private lateinit var backRegion: TextureRegion
    private lateinit var leftRegion: TextureRegion
    private lateinit var rightRegion: TextureRegion
    private lateinit var back: AnimatedButton
    private lateinit var left: AnimatedButton
    private lateinit var right: AnimatedButton
    private lateinit var practice: AnimatedButton
    private lateinit var play: AnimatedButton

    private var cardX = 0f
    private var cardY = 0f
    private var cardW = 0f
    private var cardH = 0f
    private var actionY = 0f
    private var actionH = 0f
    private var cardStride = 0f

    private var draggingCard = false
    private var dragStartX = 0f
    private var dragStartPosition = 0f
    private var carouselPosition = 0f
    private var carouselTarget = 0f
    private var carouselVelocity = 0f

    companion object {
        private const val DRAG_COMMIT_FRACTION = 0.18f
        private const val CAROUSEL_SPRING = 105f
        private const val CAROUSEL_DAMPING = 17f
        private const val CAROUSEL_MAX_SPEED = 14f
        // Moves the thumbnail crop toward the left side of the source image.
        // 0 = centered, 15 = 15% left, -15 = 15% right.
        private const val THUMBNAIL_CROP_LEFT_PERCENT = 20.5f
    }

    override fun show() {
        super.show()
        titleFont = game.fontManager.getTitle(FontManager.SIZE_XLARGE)
        bodyFont = game.fontManager.getBody(FontManager.SIZE_LARGE)
        shapes = ShapeRenderer()
        val atlas = game.atlasManager.levelSelectAtlas
        backRegion = atlas.findRegion("back")
        leftRegion = atlas.findRegion("left_arrow")
        rightRegion = atlas.findRegion("right_arrow")
        levels.addAll(game.levelManager.getLevels())
        if (levels.size == 0) {
            val empty = LevelData()
            empty.name = "No Levels Found"
            empty.fileName = "-1.json"
            levels.add(empty)
        }
        selectedLevel = selectedLevel.coerceIn(0, levels.size - 1)
        carouselPosition = selectedLevel.toFloat()
        carouselTarget = carouselPosition
        back = AnimatedButton(backRegion, 0f, 0f, 0f, 0f) { game.screen = MainMenuScreen(game) }
        left = AnimatedButton(leftRegion, 0f, 0f, 0f, 0f, null)
        right = AnimatedButton(rightRegion, 0f, 0f, 0f, 0f, null)
        practice = AnimatedButton(null, 0f, 0f, 0f, 0f) { launch(true) }
        play = AnimatedButton(null, 0f, 0f, 0f, 0f) { launch(false) }
        updateLayout()
        loadThumbnails()
    }

    private fun updateLayout() {
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight
        val backSize = minOf(vw * 0.065f, vh * 0.105f)
        back.setBounds(vw * 0.028f, vh - backSize - vh * 0.04f, backSize, backSize)
        cardW = minOf(vw * 0.68f, 1320f)
        cardH = minOf(vh * 0.48f, 530f)
        cardX = (vw - cardW) / 2f
        cardY = vh * 0.25f
        // Keep adjacent cards completely outside the viewport until the user drags.
        val sideMargin = maxOf(cardX, vw - cardX - cardW)
        cardStride = cardW + sideMargin + 4f
        val arrowSize = minOf(vw * 0.075f, vh * 0.13f)
        left.setBounds(cardX - arrowSize - vw * 0.035f, cardY + cardH / 2f - arrowSize / 2f, arrowSize, arrowSize)
        right.setBounds(cardX + cardW + vw * 0.035f, cardY + cardH / 2f - arrowSize / 2f, arrowSize, arrowSize)
        actionH = minOf(vh * 0.105f, 112f)
        actionY = cardY - actionH - vh * 0.065f
        val gap = vw * 0.035f
        val buttonW = (cardW - gap) / 2f
        practice.setBounds(cardX, actionY, buttonW, actionH)
        play.setBounds(cardX + buttonW + gap, actionY, buttonW, actionH)
    }

    override fun update(delta: Float) {
        back.update(delta)
        left.update(delta)
        right.update(delta)
        practice.update(delta)
        play.update(delta)
        updateCarouselMotion(delta)

        touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        val touchPosition = viewport.unproject(touch)
        if (Gdx.input.justTouched()) {
            back.onTouchDown(touchPosition.x, touchPosition.y)
            left.onTouchDown(touchPosition.x, touchPosition.y)
            right.onTouchDown(touchPosition.x, touchPosition.y)
            practice.onTouchDown(touchPosition.x, touchPosition.y)
            play.onTouchDown(touchPosition.x, touchPosition.y)
            if (levels.size > 1 && hitsCard(touchPosition.x, touchPosition.y)) {
                draggingCard = true
                dragStartX = touchPosition.x
                dragStartPosition = carouselPosition
                carouselTarget = carouselPosition
                carouselVelocity = 0f
            }
        }
        if (Gdx.input.isTouched && draggingCard) {
            carouselPosition =
                dragStartPosition - (touchPosition.x - dragStartX) / cardStride
            carouselTarget = carouselPosition
        }
        if (!Gdx.input.isTouched) {
            val leftPressed = left.isPressed
            val rightPressed = right.isPressed
            back.onTouchUp(touchPosition.x, touchPosition.y)
            left.onTouchUp(touchPosition.x, touchPosition.y)
            right.onTouchUp(touchPosition.x, touchPosition.y)
            practice.onTouchUp(touchPosition.x, touchPosition.y)
            play.onTouchUp(touchPosition.x, touchPosition.y)
            if (leftPressed && left.hits(touchPosition.x, touchPosition.y)) navigate(-1)
            if (rightPressed && right.hits(touchPosition.x, touchPosition.y)) navigate(1)
            if (draggingCard) finishCardDrag()
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) navigate(-1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) navigate(1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) launch(true)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) launch(false)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.screen = MainMenuScreen(game)
    }

    override fun draw() {
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        shapes.projectionMatrix = camera.combined
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawCarouselCardShapes()
        drawButtonShape(practice, false)
        drawButtonShape(play, true)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        back.draw(game.batch)
        left.draw(game.batch)
        right.draw(game.batch)
        drawTitle("Level Select", viewport.worldWidth / 2f, viewport.worldHeight * 0.91f, 1.05f)
        drawCarouselCardContents()
        drawButtonText(practice, "PRACTICE MODE", UI.TEXT)
        drawButtonText(play, "PLAY", playTextColor)
        game.batch.end()
    }

    private fun drawCard(level: LevelData, x: Float) {
        val name = level.name
        val progress = if (level.id >= 0) game.progressManager.getOrCreate(level.getProgressKey()) else null
        val levelIndex = levels.indexOf(level, true)
        val thumbnail = if (levelIndex >= 0 && levelIndex < thumbnails.size) thumbnails[levelIndex] else null
        val thumbX = x + 3f
        val thumbY = cardY + 3f
        val thumbW = cardW * 0.52f
        val thumbH = cardH - 6f
        if (thumbnail != null) drawDiagonalThumbnail(thumbnail, thumbX, thumbY, thumbW, thumbH)

        val textX = x + cardW * 0.56f
        val difficulty = level.difficulty.replaceFirstChar { it.uppercase() }
        val difficultyColor = difficultyColor(level.difficulty)
        titleFont.data.setScale(0.88f)
        drawShadow(titleFont, name, textX, cardY + cardH * 0.78f, UI.TEXT)
        bodyFont.data.setScale(0.74f)
        drawShadow(bodyFont, difficulty, textX, cardY + cardH * 0.64f, difficultyColor)

        val lineY = cardY + cardH * 0.48f
        bodyFont.data.setScale(0.58f)
        drawShadow(bodyFont, "BEST", textX, lineY, UI.TEXT_SECONDARY)
        drawShadow(bodyFont, "ATTEMPTS", textX + cardW * 0.22f, lineY, UI.TEXT_SECONDARY)
        titleFont.data.setScale(0.64f)
        drawShadow(titleFont, "${progress?.bestPercent ?: 0}%", textX, lineY - cardH * 0.12f, statBlue)
        drawShadow(titleFont, "${progress?.totalAttempts ?: 0}", textX + cardW * 0.22f, lineY - cardH * 0.12f, statBlue)
        titleFont.data.setScale(1f)
        bodyFont.data.setScale(1f)
    }

    private fun drawBestProgressBar(level: LevelData, x: Float) {
        val bestPercent = if (level.id >= 0) {
            game.progressManager.getOrCreate(level.getProgressKey()).bestPercent
        } else {
            0
        }
        val progress = (bestPercent.coerceIn(0, 100) / 100f)
        val barX = x + cardW * 0.56f
        val barY = cardY + cardH * 0.14f
        val barW = cardW * 0.35f
        val barH = maxOf(10f, cardH * 0.025f)

        shapes.color = progressTrackColor
        UI.rounded(shapes, barX, barY, barW, barH, barH / 2f)
        if (progress > 0f) {
            shapes.color = statBlue
            UI.rounded(shapes, barX, barY, barW * progress, barH, barH / 2f)
        }
    }

    private fun drawTitle(text: String, cx: Float, y: Float, scale: Float) {
        titleFont.data.setScale(scale)
        layout.setText(titleFont, text)
        drawShadow(titleFont, text, cx - layout.width / 2f, y, UI.YELLOW)
        titleFont.data.setScale(1f)
    }

    private fun drawButtonShape(button: AnimatedButton, primary: Boolean) {
        val scale = button.scale
        val scaledWidth = button.width * scale
        val scaledHeight = button.height * scale
        val scaledX = button.x + (button.width - scaledWidth) / 2f
        val scaledY = button.y + (button.height - scaledHeight) / 2f
        drawSimplePanel(
            scaledX,
            scaledY,
            scaledWidth,
            scaledHeight,
            scaledHeight * 0.18f,
            if (primary) primaryColor else secondaryColor,
            if (primary) darkOutline else subtleBorder,
            if (primary) 4f else 3f
        )
    }

    private fun drawButtonText(button: AnimatedButton, text: String, color: Color) {
        titleFont.data.setScale(0.52f * button.scale)
        layout.setText(titleFont, text)
        drawShadow(
            titleFont,
            text,
            button.x + button.width / 2f - layout.width / 2f,
            button.y + button.height / 2f + layout.height / 2f,
            color
        )
        titleFont.data.setScale(1f)
    }

    private fun drawShadow(font: BitmapFont, text: String, x: Float, y: Float, color: Color) {
        font.setColor(0f, 0f, 0f, color.a * 0.42f)
        font.draw(game.batch, text, x + 2f, y - 2f)
        font.color = color
        font.draw(game.batch, text, x, y)
    }

    private fun difficultyColor(value: String?) = when (value?.lowercase()) {
        "easy" -> Color.valueOf("55A7FF")
        "normal" -> Color.valueOf("62D96B")
        "hard" -> Color.valueOf("FFD84A")
        "insane" -> Color.valueOf("B36CFF")
        "extreme", "demon" -> Color.valueOf("FF5252")
        else -> Color.valueOf("62D96B")
    }

    private fun navigate(direction: Int) {
        if (levels.size <= 1 || draggingCard) return
        carouselTarget += direction
        selectedLevel = wrappedIndex(round(carouselTarget).toInt())
    }

    private fun launch(practiceMode: Boolean) {
        if (isCarouselMoving() || draggingCard) return
        val level = levels[selectedLevel]
        if (level.fileName != "-1.json") game.screen = GameScreen(game, level, selectedLevel, practiceMode)
    }

    private fun drawCarouselCardShapes() {
        forEachVisibleCarouselCard { level, x -> drawCardShape(level, x) }
    }

    private fun drawCardShape(level: LevelData, x: Float) {
        drawSimplePanel(
            x,
            cardY,
            cardW,
            cardH,
            cardH * 0.075f,
            panelColor,
            subtleBorder,
            3f
        )
        drawThumbnailPlaceholder(x)
        drawBestProgressBar(level, x)
    }

    private fun drawThumbnailPlaceholder(x: Float) {
        val thumbX = x + 3f
        val thumbY = cardY + 3f
        val thumbW = cardW * 0.52f
        val thumbH = cardH - 6f
        val cut = thumbW * 0.14f
        shapes.color = thumbnailShade
        UI.rounded(shapes, thumbX, thumbY, thumbW, thumbH, cardH * 0.075f - 3f)
        shapes.color = panelColor
        shapes.triangle(
            thumbX + thumbW, thumbY + thumbH,
            thumbX + thumbW - cut, thumbY,
            thumbX + thumbW, thumbY
        )
    }

    private fun drawDiagonalThumbnail(texture: Texture, x: Float, y: Float, w: Float, h: Float) {
        game.batch.draw(texture, x, y, w, h)
    }

    private fun loadThumbnails() {
        thumbnails.forEach { it?.dispose() }
        thumbnails.clear()
        for (index in 0 until levels.size) {
            thumbnails.add(loadTintedThumbnail(index, difficultyColor(levels[index].difficulty)))
        }
    }

    private fun loadTintedThumbnail(index: Int, tint: Color): Texture? {
        val extensions = arrayOf("png", "jpg", "jpeg")
        val file = extensions
            .asSequence()
            .map { Gdx.files.internal("level_thumbnails/$index.$it") }
            .firstOrNull { it.exists() }
            ?: return null
        return try {
            val source = Pixmap(file)
            val outputWidth = 768
            val thumbnailAspect = (cardW * 0.52f) / (cardH - 6f)
            val outputHeight = (outputWidth / thumbnailAspect).toInt().coerceAtLeast(1)
            val tinted = Pixmap(outputWidth, outputHeight, Pixmap.Format.RGBA8888)
            val scale = maxOf(
                outputWidth.toFloat() / source.width,
                outputHeight.toFloat() / source.height
            )
            val visibleSourceWidth = outputWidth / scale
            val visibleSourceHeight = outputHeight / scale
            val centeredCropX = (source.width - visibleSourceWidth) * 0.5f
            val cropShift =
                (source.width - visibleSourceWidth) * (THUMBNAIL_CROP_LEFT_PERCENT / 100f)
            val cropX = (centeredCropX - cropShift)
                .coerceIn(0f, (source.width - visibleSourceWidth).coerceAtLeast(0f))
            val cropY = ((source.height - visibleSourceHeight) * 0.5f).coerceAtLeast(0f)
            val cornerRadius = outputHeight * 0.075f
            val diagonalCut = outputWidth * 0.14f
            for (py in 0 until outputHeight) {
                for (px in 0 until outputWidth) {
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
                            val edgeCoverage =
                                (cornerRadius + 0.75f - kotlin.math.sqrt(dx * dx + dy * dy))
                                    .coerceIn(0f, 1f)
                            alpha = (alpha * edgeCoverage).toInt()
                        }
                    }
                    val diagonalEdge =
                        outputWidth - diagonalCut * (py / (outputHeight - 1f).coerceAtLeast(1f))
                    val diagonalCoverage = (diagonalEdge + 0.75f - px).coerceIn(0f, 1f)
                    alpha = (alpha * diagonalCoverage).toInt()
                    val gray = (red * 0.299f + green * 0.587f + blue * 0.114f) / 255f
                    val outRed = (gray * (0.22f + tint.r * 0.78f) * 255f).toInt().coerceIn(0, 255)
                    val outGreen = (gray * (0.22f + tint.g * 0.78f) * 255f).toInt().coerceIn(0, 255)
                    val outBlue = (gray * (0.22f + tint.b * 0.78f) * 255f).toInt().coerceIn(0, 255)
                    val tintedRgba =
                        (outRed shl 24) or (outGreen shl 16) or (outBlue shl 8) or alpha
                    tinted.drawPixel(px, py, tintedRgba)
                }
            }
            source.dispose()
            Texture(tinted).also {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                tinted.dispose()
            }
        } catch (exception: Exception) {
            Gdx.app.error("LevelSelect", "Could not load thumbnail $index: ${exception.message}")
            null
        }
    }

    private fun drawSimplePanel(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        fill: Color,
        border: Color,
        thickness: Float
    ) {
        shapes.color = border
        UI.rounded(shapes, x, y, w, h, radius)
        shapes.color = fill
        UI.rounded(
            shapes,
            x + thickness,
            y + thickness,
            w - thickness * 2f,
            h - thickness * 2f,
            (radius - thickness).coerceAtLeast(1f)
        )
    }

    private fun drawCarouselCardContents() {
        forEachVisibleCarouselCard { level, x -> drawCard(level, x) }
    }

    private fun hitsCard(x: Float, y: Float): Boolean {
        return x in cardX..(cardX + cardW) && y in cardY..(cardY + cardH)
    }

    private fun finishCardDrag() {
        draggingCard = false
        val moved = carouselPosition - dragStartPosition
        val base = round(dragStartPosition)
        carouselTarget = if (abs(moved) >= DRAG_COMMIT_FRACTION) {
            base + sign(moved)
        } else {
            base
        }
        selectedLevel = wrappedIndex(round(carouselTarget).toInt())
    }

    private fun updateCarouselMotion(delta: Float) {
        if (draggingCard) return
        var remaining = delta.coerceAtMost(0.1f)
        while (remaining > 0f) {
            val dt = minOf(remaining, 1f / 120f)
            val acceleration =
                (carouselTarget - carouselPosition) * CAROUSEL_SPRING -
                    carouselVelocity * CAROUSEL_DAMPING
            carouselVelocity =
                (carouselVelocity + acceleration * dt).coerceIn(
                    -CAROUSEL_MAX_SPEED,
                    CAROUSEL_MAX_SPEED
                )
            carouselPosition += carouselVelocity * dt
            remaining -= dt
        }
        if (abs(carouselTarget - carouselPosition) < 0.0005f &&
            abs(carouselVelocity) < 0.005f
        ) {
            carouselPosition = carouselTarget
            carouselVelocity = 0f
        }
    }

    private inline fun forEachVisibleCarouselCard(draw: (LevelData, Float) -> Unit) {
        val centerVirtualIndex = kotlin.math.floor(carouselPosition).toInt()
        for (virtualIndex in (centerVirtualIndex - 2)..(centerVirtualIndex + 3)) {
            val x = cardX + (virtualIndex - carouselPosition) * cardStride
            if (x < viewport.worldWidth + 4f && x + cardW > -4f) {
                draw(levels[wrappedIndex(virtualIndex)], x)
            }
        }
    }

    private fun isCarouselMoving(): Boolean {
        return abs(carouselTarget - carouselPosition) > 0.002f ||
            abs(carouselVelocity) > 0.02f
    }

    private fun wrappedIndex(index: Int): Int {
        return (index % levels.size + levels.size) % levels.size
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        draggingCard = false
        carouselPosition = round(carouselTarget)
        carouselTarget = carouselPosition
        carouselVelocity = 0f
        selectedLevel = wrappedIndex(carouselPosition.toInt())
        updateLayout()
    }

    override fun dispose() {
        thumbnails.forEach { it?.dispose() }
        thumbnails.clear()
        shapes.dispose()
        super.dispose()
    }
}
