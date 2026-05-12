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
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.font.FontManager
import io.github.msameer0.rhythmicrush.game.level.LevelData
import io.github.msameer0.rhythmicrush.ui.AnimatedButton

/**
 * Screen used for browsing and selecting game levels, including progress display and navigation.
 */
class LevelSelectScreen @JvmOverloads constructor(
    game: RhythmicRushGame,
    initialIndex: Int = 0
) : AbstractScreen(game) {

    companion object {
        private const val PANEL_CORNER_RADIUS = 40f
        private const val SWIPE_THRESHOLD = 80f
        private const val TRANSITION_DURATION = 0.45f
    }

    private val levels = Array<LevelData>()
    private var selectedLevel: Int = initialIndex

    private lateinit var font: BitmapFont
    private val layout = GlyphLayout()

    private lateinit var backButton: TextureRegion
    private lateinit var leftArrow: TextureRegion
    private lateinit var rightArrow: TextureRegion
    private lateinit var difficultyTextures: kotlin.Array<TextureRegion?>

    private lateinit var btnBack: AnimatedButton
    private lateinit var btnLeft: AnimatedButton
    private lateinit var btnRight: AnimatedButton
    private lateinit var btnPanel: AnimatedButton
    private lateinit var btnPractice: AnimatedButton
    private lateinit var btnYoutube: AnimatedButton

    private var panelTexture: Texture? = null
    private var lastPanelW = -1
    private var lastPanelH = -1

    private var panelX = 0f
    private var panelY = 0f
    private var panelW = 0f
    private var panelH = 0f

    private var isTransitioning = false
    private var transitionDir = 0
    private var transitionTime = 0f

    private var nextLevelIndex = 0
    private var panelXStart = 0f
    private var panelXTarget = 0f

    private var isSwiping = false
    private var touchStartX = 0f
    private var touchStartY = 0f

    override fun show() {
        super.show()

        font = game.fontManager.get(FontManager.SIZE_XLARGE)

        val atlas = game.atlasManager.levelSelectAtlas
        backButton = atlas.findRegion("back")
        leftArrow = atlas.findRegion("left_arrow")
        rightArrow = atlas.findRegion("right_arrow")

        difficultyTextures = arrayOf(
            atlas.findRegion("1_diff"),
            atlas.findRegion("2_diff"),
            atlas.findRegion("3_diff"),
            atlas.findRegion("4_diff"),
            atlas.findRegion("5_diff")
        )

        loadLevels()
        btnBack = AnimatedButton(backButton, 0f, 0f, 0f, 0f) { game.screen = MainMenuScreen(game) }
        btnLeft = AnimatedButton(leftArrow, 0f, 0f, 0f, 0f) { navigate(-1) }
        btnRight = AnimatedButton(rightArrow, 0f, 0f, 0f, 0f) { navigate(1) }
        btnPanel = AnimatedButton(null, 0f, 0f, 0f, 0f) { playSelected() }
        btnPractice = AnimatedButton(null, 0f, 0f, 0f, 0f) { playPractice() }
        btnYoutube = AnimatedButton(null, 0f, 0f, 0f, 0f) { watchYoutube() }
        updateScaledSizes()
    }

    private fun loadLevels() {
        levels.clear()
        levels.addAll(game.levelManager.getLevels())

        if (levels.size == 0) {
            val placeholder = LevelData()
            placeholder.name = "No Levels Found"
            placeholder.difficulty = "normal"
            placeholder.fileName = "-1.json"
            levels.add(placeholder)
        }
        selectedLevel = kotlin.math.max(0, kotlin.math.min(selectedLevel, levels.size - 1))
    }

    private fun difficultyIndex(difficulty: String?): Int {
        if (difficulty == null) return 1
        return when (difficulty.lowercase()) {
            "easy" -> 0
            "hard" -> 2
            "insane" -> 3
            "extreme" -> 4
            else -> 1
        }
    }

    private fun difficultyTexture(difficulty: String?): TextureRegion {
        val idx = difficultyIndex(difficulty)
        return difficultyTextures[idx] ?: difficultyTextures[1]
        ?: throw IllegalStateException("Default difficulty texture not found")
    }

    private fun updateScaledSizes() {
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight

        val backW = vw * 0.08f
        val leftW = vw * 0.08f
        val rightW = vw * 0.08f

        if (::btnBack.isInitialized) btnBack.setBounds(10f, vh - backW - 10f, backW, backW)
        if (::btnLeft.isInitialized) btnLeft.setBounds(10f, vh / 2f - leftW / 2f, leftW, leftW)
        if (::btnRight.isInitialized) btnRight.setBounds(
            vw - rightW - 10f,
            vh / 2f - rightW / 2f,
            rightW,
            rightW
        )

        panelW = vw * 0.6f
        panelH = vh * 0.28f
        panelX = vw / 2f - panelW / 2f
        panelY = vh / 2f - panelH / 2f + 40f
        if (::btnPanel.isInitialized) btnPanel.setBounds(panelX, panelY, panelW, panelH)

        val practiceW = vw * 0.28f
        val practiceH = vh * 0.07f
        if (::btnPractice.isInitialized) btnPractice.setBounds(
            vw - practiceW - 20f,
            vh - practiceH - 20f,
            practiceW,
            practiceH
        )

        if (::btnYoutube.isInitialized) btnYoutube.setBounds(
            vw - practiceW - 20f,
            20f,
            practiceW,
            practiceH
        )
    }

    override fun update(delta: Float) {
        btnBack.update(delta)
        btnLeft.update(delta)
        btnRight.update(delta)
        btnPanel.update(delta)
        btnPractice.update(delta)
        if (levels.size > 0 && levels.get(selectedLevel).youtubeLink.isNotEmpty()) {
            btnYoutube.update(delta)
        }
        handleInput()

        if (isTransitioning) {
            transitionTime += delta
            val alpha = kotlin.math.min(transitionTime / TRANSITION_DURATION, 1f)
            val interp = Interpolation.swing.apply(alpha)

            panelX = panelXStart + (panelXTarget - panelXStart) * interp

            if (transitionTime >= TRANSITION_DURATION) {
                selectedLevel = nextLevelIndex
                panelXTarget = viewport.worldWidth / 2f - panelW / 2f
                panelXStart = panelXTarget
                panelX = panelXStart
                isTransitioning = false
            }
        }
    }

    override fun draw() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        game.batch.begin()

        if (isTransitioning) {
            val oldLevel = levels.get(selectedLevel)
            val newLevel = levels.get(nextLevelIndex)

            val offset = transitionDir * viewport.worldWidth

            drawLevelPanel(oldLevel, panelX, panelY)
            drawLevelPanel(newLevel, panelX + offset, panelY)
        } else {
            drawLevelPanel(levels.get(selectedLevel), panelX, panelY)
            drawPracticeButton()
            if (levels.size > 0 && levels.get(selectedLevel).youtubeLink.isNotEmpty()) {
                drawYoutubeButton()
            }
        }

        game.batch.end()
    }

    private fun drawPracticeButton() {
        if (!::btnPractice.isInitialized) return
        val scale = btnPractice.scale
        val bw = btnPractice.w * scale
        val bh = btnPractice.h * scale
        val bx = btnPractice.x + btnPractice.w / 2f - bw / 2f
        val by = btnPractice.y + btnPractice.h / 2f - bh / 2f

        game.batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        val shapes = ShapeRenderer()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.15f, 0.45f, 0.15f, 0.8f)
        drawRoundedRect(shapes, bx, by, bw, bh, 10f)
        shapes.end()
        shapes.dispose()
        Gdx.gl.glDisable(GL20.GL_BLEND)
        game.batch.begin()

        font.data.setScale(0.35f * scale)
        val text = "Enter Practice Mode"
        layout.setText(font, text)
        drawTextWithShadow(
            font,
            text,
            bx + bw / 2f - layout.width / 2f,
            by + bh / 2f + layout.height / 2f,
            Color.WHITE
        )
    }

    private fun drawYoutubeButton() {
        if (!::btnYoutube.isInitialized) return
        val scale = btnYoutube.scale
        val bw = btnYoutube.w * scale
        val bh = btnYoutube.h * scale
        val bx = btnYoutube.x + btnYoutube.w / 2f - bw / 2f
        val by = btnYoutube.y + btnYoutube.h / 2f - bh / 2f

        game.batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        val shapes = ShapeRenderer()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.85f, 0.15f, 0.15f, 0.8f)
        drawRoundedRect(shapes, bx, by, bw, bh, 10f)
        shapes.end()
        shapes.dispose()
        Gdx.gl.glDisable(GL20.GL_BLEND)
        game.batch.begin()

        font.data.setScale(0.35f * scale)
        val text = "Watch Showcase"
        layout.setText(font, text)
        drawTextWithShadow(
            font,
            text,
            bx + bw / 2f - layout.width / 2f,
            by + bh / 2f + layout.height / 2f,
            Color.WHITE
        )
    }

    private fun drawLevelPanel(current: LevelData, pX: Float, pY: Float) {
        game.batch.projectionMatrix = camera.combined

        val texW = panelW.toInt()
        val texH = panelH.toInt()
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            panelTexture?.dispose()
            panelTexture = createRoundedRect(
                texW, texH,
                (PANEL_CORNER_RADIUS * (panelW / 1920f) * 2.4f).toInt(),
                Color(0.2f, 0.2f, 0.28f, 1f)
            )
            panelTexture?.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            lastPanelW = texW
            lastPanelH = texH
        }

        panelTexture?.let { game.batch.draw(it, pX, pY) }
        btnBack.draw(game.batch)
        btnLeft.draw(game.batch)
        btnRight.draw(game.batch)

        val diffRegion = difficultyTexture(current.difficulty)
        val iconSize = panelH * 0.55f
        val spacing = panelW * 0.05f

        font.data.setScale(1.7f)
        val name = current.name ?: "Unknown"
        layout.setText(font, name)
        val nameW = layout.width
        val nameH = layout.height

        val maxTotalWidth = panelW * 0.9f
        var scale = 1f
        val totalWidth = iconSize + spacing + nameW
        if (totalWidth > maxTotalWidth) {
            scale = (maxTotalWidth - iconSize - spacing) / nameW
        }

        font.data.setScale(0.85f * scale)
        layout.setText(font, name)

        val iconX = pX + (panelW - (iconSize + spacing + layout.width)) / 2f
        val iconY = pY + panelH / 2f - iconSize / 2f + 10f
        val textX = iconX + iconSize + spacing
        val textY = pY + panelH / 2f + layout.height / 2f + 10f

        game.batch.draw(diffRegion, iconX, iconY, iconSize, iconSize)
        drawTextWithShadow(font, name, textX, textY, Color.WHITE)

        font.data.setScale(0.8f * scale)
        val diff = current.difficulty ?: "Normal"
        val diffLabel = diff.substring(0, 1).uppercase() + diff.substring(1)
        layout.setText(font, diffLabel)
        drawTextWithShadow(font, diffLabel, textX, textY - nameH - 4f, Color(1f, 1f, 1f, 0.55f))

        val levelKey = current.fileName ?: "${current.name}.json"
        val progress = game.progressManager.getOrCreate(levelKey)

        font.data.setScale(0.8f)
        val bestText = "Best: ${progress.bestPercent}%"
        val attemptsText = "Total Attempts: ${progress.totalAttempts}"
        val statsX = pX + panelW / 2f

        layout.setText(font, bestText)
        drawTextWithShadow(
            font,
            bestText,
            statsX - layout.width / 2f,
            pY - 18f,
            Color(1f, 1f, 1f, 0.8f)
        )

        layout.setText(font, attemptsText)
        drawTextWithShadow(
            font,
            attemptsText,
            statsX - layout.width / 2f,
            pY - 44f,
            Color(1f, 1f, 1f, 0.55f)
        )

        var currentLevelNum = -1
        for (i in 0 until levels.size) {
            if (levels.get(i) === current) {
                currentLevelNum = i + 1
                break
            }
        }

        font.data.setScale(1f)
        font.color = Color.WHITE
    }

    private fun handleInput() {
        val t = viewport.unproject(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))

        if (Gdx.input.justTouched()) {
            btnBack.onTouchDown(t.x, t.y)
            btnLeft.onTouchDown(t.x, t.y)
            btnRight.onTouchDown(t.x, t.y)
            btnPanel.onTouchDown(t.x, t.y)
            btnPractice.onTouchDown(t.x, t.y)
            if (levels.size > 0 && levels.get(selectedLevel).youtubeLink.isNotEmpty()) {
                btnYoutube.onTouchDown(t.x, t.y)
            }

            isSwiping = true
            touchStartX = t.x
            touchStartY = t.y
        }

        if (Gdx.input.isTouched && isSwiping) {
            selectedLevel = com.badlogic.gdx.math.MathUtils.clamp(selectedLevel, 0, levels.size - 1)
            val dx = t.x - touchStartX
            val dy = t.y - touchStartY

            if (kotlin.math.abs(dx) > SWIPE_THRESHOLD && kotlin.math.abs(dy) < SWIPE_THRESHOLD * 1.5f) {
                if (dx > 0) {
                    navigate(-1)
                } else {
                    navigate(1)
                }
                isSwiping = false
            }
        }

        if (!Gdx.input.isTouched) {
            if (isSwiping) {
                btnBack.onTouchUp(t.x, t.y)
                btnLeft.onTouchUp(t.x, t.y)
                btnRight.onTouchUp(t.x, t.y)
                btnPanel.onTouchUp(t.x, t.y)
                btnPractice.onTouchUp(t.x, t.y)
                btnYoutube.onTouchUp(t.x, t.y)
            }
            isSwiping = false
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) navigate(-1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) navigate(1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) playSelected()
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) playSelected()
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) playPractice()
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.screen = MainMenuScreen(game)
    }

    private fun playPractice() {
        val data = levels.get(selectedLevel)
        if ("-1.json" != data.fileName) {
            game.screen = GameScreen(game, data, selectedLevel, true)
        }
    }

    private fun watchYoutube() {
        if (levels.size == 0) return
        val data = levels.get(selectedLevel)
        if (data.youtubeLink.isNotEmpty()) {
            Gdx.net.openURI(data.youtubeLink)
        }
    }

    private fun navigate(dir: Int) {
        if (isTransitioning) {
            selectedLevel = nextLevelIndex
            panelX = viewport.worldWidth / 2f - panelW / 2f
            isTransitioning = false
        }

        nextLevelIndex = (selectedLevel + dir + levels.size) % levels.size
        transitionDir = dir

        panelXStart = panelX
        panelXTarget = panelX - dir * viewport.worldWidth

        transitionTime = 0f
        isTransitioning = true
    }

    private fun playSelected() {
        val t = viewport.unproject(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))

        if (isTransitioning) {
            if (hits(t, panelX, panelY, panelW, panelH)) {
                val data = levels.get(selectedLevel)
                if ("-1.json" != data.fileName) {
                    game.screen = GameScreen(game, data, selectedLevel)
                }
                return
            }
            val nextX = panelX + transitionDir * viewport.worldWidth
            if (hits(t, nextX, panelY, panelW, panelH)) {
                val data = levels.get(nextLevelIndex)
                if ("-1.json" != data.fileName) {
                    game.screen = GameScreen(game, data, nextLevelIndex)
                }
            }
        } else {
            val data = levels.get(selectedLevel)
            if ("-1.json" != data.fileName) {
                game.screen = GameScreen(game, data, selectedLevel)
            }
        }
    }

    private fun hits(t: Vector2, x: Float, y: Float, w: Float, h: Float): Boolean {
        return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h
    }

    private fun drawRoundedRect(
        shapes: ShapeRenderer,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        r: Float
    ) {
        shapes.rect(x, y + r, w, h - 2 * r)
        shapes.rect(x + r, y, w - 2 * r, r)
        shapes.rect(x + r, y + h - r, w - 2 * r, r)
        shapes.circle(x + r, y + r, r, 20)
        shapes.circle(x + w - r, y + r, r, 20)
        shapes.circle(x + r, y + h - r, r, 20)
        shapes.circle(x + w - r, y + h - r, r, 20)
    }

    private fun createRoundedRect(w: Int, h: Int, r: Int, color: Color): Texture {
        val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        pixmap.setColor(color)
        pixmap.fillRectangle(r, 0, w - 2 * r, h)
        pixmap.fillRectangle(0, r, w, h - 2 * r)
        pixmap.fillCircle(r, r, r)
        pixmap.fillCircle(w - r, r, r)
        pixmap.fillCircle(r, h - r, r)
        pixmap.fillCircle(w - r, h - r, r)
        val t = Texture(pixmap)
        pixmap.dispose()
        return t
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        updateScaledSizes()
    }

    override fun dispose() {
        panelTexture?.dispose()
        super.dispose()
    }
}
