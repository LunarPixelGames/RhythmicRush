package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
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

    private val levels = Array<LevelData>()
    private var selectedLevel = initialIndex
    private lateinit var titleFont: BitmapFont
    private lateinit var bodyFont: BitmapFont
    private val layout = GlyphLayout()
    private lateinit var shapes: ShapeRenderer
    private lateinit var backRegion: TextureRegion
    private lateinit var leftRegion: TextureRegion
    private lateinit var rightRegion: TextureRegion
    private lateinit var difficultyRegions: kotlin.Array<TextureRegion?>
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
        difficultyRegions = arrayOf(
            atlas.findRegion("1_diff"), atlas.findRegion("2_diff"), atlas.findRegion("3_diff"),
            atlas.findRegion("4_diff"), atlas.findRegion("5_diff")
        )
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
        back.update(delta); left.update(delta); right.update(delta); practice.update(delta); play.update(delta)
        updateCarouselMotion(delta)

        val t = viewport.unproject(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
        if (Gdx.input.justTouched()) {
            back.onTouchDown(t.x, t.y); left.onTouchDown(t.x, t.y); right.onTouchDown(t.x, t.y)
            practice.onTouchDown(t.x, t.y); play.onTouchDown(t.x, t.y)
            if (levels.size > 1 && hitsCard(t.x, t.y)) {
                draggingCard = true
                dragStartX = t.x
                dragStartPosition = carouselPosition
                carouselTarget = carouselPosition
                carouselVelocity = 0f
            }
        }
        if (Gdx.input.isTouched && draggingCard) {
            carouselPosition = dragStartPosition - (t.x - dragStartX) / cardStride
            carouselTarget = carouselPosition
        }
        if (!Gdx.input.isTouched) {
            val leftPressed = left.isPressed
            val rightPressed = right.isPressed
            back.onTouchUp(t.x, t.y); left.onTouchUp(t.x, t.y); right.onTouchUp(t.x, t.y)
            practice.onTouchUp(t.x, t.y); play.onTouchUp(t.x, t.y)
            if (leftPressed && left.hits(t.x, t.y)) navigate(-1)
            if (rightPressed && right.hits(t.x, t.y)) navigate(1)
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
        back.draw(game.batch); left.draw(game.batch); right.draw(game.batch)
        drawTitle("Level Select", viewport.worldWidth / 2f, viewport.worldHeight * 0.91f, 1.05f)
        drawCarouselCardContents()
        drawButtonText(practice, "PRACTICE MODE", UI.TEXT)
        drawButtonText(play, "PLAY", Color(0.04f, 0.05f, 0.09f, 1f))
        game.batch.end()
    }

    private fun drawCard(level: LevelData, x: Float) {
        val name = level.name
        val progress = if (level.id >= 0) game.progressManager.getOrCreate(level.getProgressKey()) else null
        val iconSize = cardH * 0.48f
        val iconX = x + cardW * 0.09f
        val iconY = cardY + (cardH - iconSize) / 2f
        val region = difficultyRegions[difficultyIndex(level.difficulty)] ?: difficultyRegions[1]
        if (region != null) game.batch.draw(region, iconX, iconY, iconSize, iconSize)
        val textX = x + cardW * 0.43f
        titleFont.data.setScale(1.05f)
        drawShadow(titleFont, name, textX, cardY + cardH * 0.76f, UI.TEXT)
        bodyFont.data.setScale(0.86f)
        val difficulty = level.difficulty.replaceFirstChar { it.uppercase() }
        drawShadow(bodyFont, difficulty, textX, cardY + cardH * 0.58f, UI.LIME)
        val lineY = cardY + cardH * 0.46f
        bodyFont.data.setScale(0.72f)
        drawShadow(bodyFont, "BEST", textX, lineY, UI.TEXT_SECONDARY)
        drawShadow(bodyFont, "ATTEMPTS", textX + cardW * 0.25f, lineY, UI.TEXT_SECONDARY)
        titleFont.data.setScale(0.76f)
        drawShadow(titleFont, "${progress?.bestPercent ?: 0}%", textX, lineY - cardH * 0.13f, statBlue)
        drawShadow(titleFont, "${progress?.totalAttempts ?: 0}", textX + cardW * 0.25f, lineY - cardH * 0.13f, statBlue)
        titleFont.data.setScale(1f); bodyFont.data.setScale(1f)
    }

    private fun drawBestProgressBar(level: LevelData, x: Float) {
        val bestPercent = if (level.id >= 0) {
            game.progressManager.getOrCreate(level.getProgressKey()).bestPercent
        } else {
            0
        }
        val progress = (bestPercent.coerceIn(0, 100) / 100f)
        val barX = x + cardW * 0.43f
        val barY = cardY + cardH * 0.14f
        val barW = cardW * 0.46f
        val barH = maxOf(10f, cardH * 0.025f)

        shapes.color = Color(0.10f, 0.10f, 0.15f, 0.72f)
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
        val s = button.scale
        val w = button.w * s
        val h = button.h * s
        val x = button.x + (button.w - w) / 2f
        val y = button.y + (button.h - h) / 2f
        drawSimplePanel(
            x,
            y,
            w,
            h,
            h * 0.18f,
            if (primary) primaryColor else secondaryColor,
            if (primary) darkOutline else subtleBorder,
            if (primary) 4f else 3f
        )
    }

    private fun drawButtonText(button: AnimatedButton, text: String, color: Color) {
        titleFont.data.setScale(0.52f * button.scale)
        layout.setText(titleFont, text)
        drawShadow(titleFont, text, button.x + button.w / 2f - layout.width / 2f, button.y + button.h / 2f + layout.height / 2f, color)
        titleFont.data.setScale(1f)
    }

    private fun drawShadow(font: BitmapFont, text: String, x: Float, y: Float, color: Color) {
        font.setColor(0f, 0f, 0f, color.a * 0.42f); font.draw(game.batch, text, x + 2f, y - 2f)
        font.color = color; font.draw(game.batch, text, x, y)
    }

    private fun difficultyIndex(value: String?) = when (value?.lowercase()) {
        "easy" -> 0; "hard" -> 2; "insane" -> 3; "extreme", "demon" -> 4; else -> 1
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
        drawBestProgressBar(level, x)
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
        shapes.dispose()
        super.dispose()
    }
}
