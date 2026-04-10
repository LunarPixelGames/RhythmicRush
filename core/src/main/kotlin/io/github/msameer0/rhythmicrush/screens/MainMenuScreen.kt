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
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.font.FontManager
import io.github.msameer0.rhythmicrush.settings.SettingsManager
import io.github.msameer0.rhythmicrush.ui.AnimatedButton
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

/**
 * The primary entry point screen for the game, providing access to the main gameplay
 * and a comprehensive settings menu.
 *
 * The screen handles:
 * - The main menu interface (Play and Settings buttons).
 * - A multi-tabbed settings overlay (Gameplay and Graphics categories).
 * - Interactive UI elements including toggles, sliders, and numeric input fields.
 * - Dynamic layout scaling to accommodate various window sizes and platforms.
 * - Background music management and transition to the level selection screen.
 *
 * @see AbstractScreen
 * @see SettingsManager
 */
class MainMenuScreen(game: RhythmicRushGame) : AbstractScreen(game) {

    private lateinit var title: TextureRegion
    private lateinit var startButton: TextureRegion
    private lateinit var settingsButton: TextureRegion
    private lateinit var backArrow: TextureRegion
    private lateinit var infoButton: TextureRegion
    private lateinit var bgColor: Color

    private var titleX = 0f
    private var titleY = 0f
    private var titleW = 0f
    private var titleH = 0f

    private lateinit var btnPlay: AnimatedButton
    private lateinit var btnSettings: AnimatedButton
    private lateinit var btnInfo: AnimatedButton

    private var settingsOpen = false
    private var infoOpen = false
    private lateinit var shapes: ShapeRenderer
    private lateinit var font: BitmapFont
    private val layout = GlyphLayout()

    companion object {
        private const val CAT_GAMEPLAY = 0
        private const val CAT_GRAPHICS = 1
        private const val CAT_COUNT = 2
        private val CAT_NAMES = arrayOf("Gameplay", "Graphics")

        private const val INFO_TAB_HOWTOPLAY = 0
        private const val INFO_TAB_CREDITS = 1
        private const val INFO_TAB_SOCIALS = 2
        private const val INFO_TAB_COUNT = 3
        private val INFO_TAB_NAMES = arrayOf("How to Play", "Credits", "Socials")

        private const val MAX_ROWS_PER_PAGE = 4

        private const val PANEL_HEIGHT_FRACTION = 0.88f

        private val COL_OVERLAY = Color(0f, 0f, 0f, 0.62f)
        private val COL_PANEL = Color(0.13f, 0.13f, 0.19f, 1f)
        private val COL_LABEL = Color(1f, 1f, 1f, 0.90f)
        private val COL_DIM = Color(1f, 1f, 1f, 0.45f)
        private val COL_ON = Color(0.35f, 0.85f, 0.55f, 1f)
        private val COL_OFF = Color(0.50f, 0.50f, 0.55f, 1f)
        private val COL_TRACK = Color(0.28f, 0.28f, 0.35f, 1f)
        private val COL_FILL = Color(0.35f, 0.65f, 1.00f, 1f)
        private val COL_THUMB = Color(1f, 1f, 1f, 1f)
        private val COL_HEADING = Color(1f, 0.85f, 0.35f, 1f)
        private val COL_TAB_ACT = Color(0.35f, 0.65f, 1.00f, 1f)
        private val COL_TAB_INACT = Color(0.35f, 0.35f, 0.45f, 1f)
        private val COL_INPUT_BG = Color(0.18f, 0.18f, 0.26f, 1f)
        private val COL_INPUT_BD = Color(0.35f, 0.65f, 1.00f, 1f)
        private val COL_DOT_ACT = Color(0.35f, 0.65f, 1.00f, 1f)
        private val COL_DOT_INACT = Color(0.35f, 0.35f, 0.45f, 1f)

        private fun hits(t: Vector2, x: Float, y: Float, w: Float, h: Float): Boolean {
            return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h
        }
    }

    private var currentCat = CAT_GAMEPLAY
    private var currentInfoTab = INFO_TAB_HOWTOPLAY
    private var currentSubPage = 0

    private var panelX = 0f
    private var panelY = 0f
    private var panelW = 0f
    private var panelH = 0f
    private var backX = 0f
    private var backY = 0f
    private var backW = 0f
    private var backH = 0f
    private var rowStartY = 0f
    private var arrowLeftX = 0f
    private var arrowRightX = 0f
    private var arrowY = 0f
    private var arrowSize = 0f

    private var rowStep = 0f
    private var panelPadT = 0f
    private var panelPadB = 0f
    private var settingsFontScale = 0f
    private var settingsHeadingScale = 0f

    private var draggingSlider = false
    private var draggingSliderRow = -1

    private var fpsInputActive = false
    private val fpsInputBuffer = java.lang.StringBuilder()

    private var panelTexture: Texture? = null
    private var lastPanelW = -1
    private var lastPanelH = -1

    private enum class RowType {
        TOGGLE, SLIDER, INT_FIELD
    }

    private class SettingRow(val type: RowType, val label: String, val id: String)

    init {
        game.updateManager.checkForUpdate()
    }

    override fun show() {
        super.show()
        val menuAtlas = game.atlasManager.menuAtlas
        val levelSelectAtlas = game.atlasManager.levelSelectAtlas

        title = menuAtlas.findRegion("title")
        startButton = menuAtlas.findRegion("start_button")
        settingsButton = menuAtlas.findRegion("settings_button")
        backArrow = levelSelectAtlas.findRegion("back")
        infoButton = menuAtlas.findRegion("info")

        bgColor = Color(
            0.2f + 0.6f * Random.nextFloat(),
            0.2f + 0.6f * Random.nextFloat(),
            0.2f + 0.6f * Random.nextFloat(), 1f
        )

        shapes = ShapeRenderer()
        font = game.fontManager.get(FontManager.SIZE_LARGE)

        btnPlay = AnimatedButton(startButton, 0f, 0f, 0f, 0f) { game.screen = LevelSelectScreen(game) }
        btnSettings = AnimatedButton(settingsButton, 0f, 0f, 0f, 0f) { settingsOpen = true }
        btnInfo = AnimatedButton(infoButton, 0f, 0f, 0f, 0f) { infoOpen = true }

        if (game.settingsManager.menuMusicEnabled) {
            game.soundManager.playMenuMusic()
        } else {
            game.soundManager.stopMenuMusic()
        }

        updateScaledSizes()

        if (game.adController != null) {
            // val shouldShowBanner = MathUtils.randomBoolean(0.5f)
            // TODO: ADS
            // game.adController.showBannerAd(shouldShowBanner)
        }
    }

    override fun hide() {
        if (game.adController != null) {
            // TODO: ADS
            // game.adController.showBannerAd(false)
        }
        super.hide()
    }

    private fun buildAllRows(cat: Int): Array<SettingRow> {
        val s = game.settingsManager
        val desktop = Gdx.app.type == com.badlogic.gdx.Application.ApplicationType.Desktop
        val rows = Array<SettingRow>()

        if (cat == CAT_GAMEPLAY) {
            rows.add(SettingRow(RowType.TOGGLE, "Menu Music", "menuMusic"))
            rows.add(SettingRow(RowType.SLIDER, "Music Volume", "volume"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Hitboxes", "hitboxes"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Hitboxes on Death", "hitboxesDeath"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Percentage", "showPercentage"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Progress Bar", "showProgressBar"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Attempts", "showAttempts"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Best", "showBest"))
            rows.add(SettingRow(RowType.SLIDER, "Practice Buttons Opacity", "practiceOpacity"))
            if (desktop) {
                rows.add(SettingRow(RowType.TOGGLE, "Lock Cursor in Game", "lockCursor"))
            }
        } else {
            rows.add(SettingRow(RowType.TOGGLE, "Show FPS", "showFps"))
            if (desktop) {
                rows.add(SettingRow(RowType.TOGGLE, "Cap FPS", "capFps"))
                if (s.capFps) {
                    rows.add(SettingRow(RowType.INT_FIELD, "FPS Limit", "fpsValue"))
                }
            }
            if (desktop) {
                rows.add(SettingRow(RowType.TOGGLE, "VSync", "vsync"))
            }
            rows.add(SettingRow(RowType.SLIDER, "UI Padding", "uiPadding"))
        }
        return rows
    }

    private fun getPageRows(cat: Int, subPage: Int): Array<SettingRow> {
        val all = buildAllRows(cat)
        val start = subPage * MAX_ROWS_PER_PAGE
        val end = min(start + MAX_ROWS_PER_PAGE, all.size)
        if (start >= all.size) return Array()
        val pageRows = Array<SettingRow>()
        for (i in start until end) {
            pageRows.add(all.get(i))
        }
        return pageRows
    }

    private fun subPageCount(cat: Int): Int {
        val total = buildAllRows(cat).size
        return max(1, ceil(total.toDouble() / MAX_ROWS_PER_PAGE).toInt())
    }

    private fun updateScaledSizes() {
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight

        val maxTitleWidth = vw * 0.85f
        var titleScale = (maxTitleWidth / title.regionWidth) * 0.675f
        val maxTitleHeight = vh * 0.36f
        if (title.regionHeight * titleScale > maxTitleHeight) {
            titleScale = maxTitleHeight / title.regionHeight
        }
        titleW = title.regionWidth * titleScale
        titleH = title.regionHeight * titleScale
        titleX = vw / 2f - titleW / 2f
        titleY = vh - titleH - vh * 0.03f

        val maxStartW = vw * 0.25f * 0.55f
        val startScale = maxStartW / startButton.regionWidth
        val startW = startButton.regionWidth * startScale
        val startH = startButton.regionHeight * startScale
        val startX = vw / 2f - startW / 2f
        val minY = titleY - startH - vh * 0.06f
        val midY = vh / 2f - startH / 2f
        val startY = min(midY, minY)
        if (::btnPlay.isInitialized) btnPlay.setBounds(startX, startY, startW, startH)

        val maxSettingsW = vw * 0.1f * 0.85f
        val settingsScale = maxSettingsW / settingsButton.regionWidth
        val settingsW = settingsButton.regionWidth * settingsScale
        val settingsH = settingsButton.regionHeight * settingsScale
        val settingsX = 20f
        val settingsY = 20f - 10f
        if (::btnSettings.isInitialized) btnSettings.setBounds(settingsX, settingsY, settingsW, settingsH)

        val infoW = settingsW
        val infoH = settingsH
        val infoX = vw - infoW - 20f
        val infoY = 20f - 10f
        if (::btnInfo.isInitialized) btnInfo.setBounds(infoX, infoY, infoW, infoH)

        panelW = min(vw * 0.78f, 780f)

        val targetH = vh * PANEL_HEIGHT_FRACTION
        val dotsExtra = targetH * 0.055f
        val padT = targetH * 0.22f
        val padB = targetH * 0.045f
        val rowsH = targetH - padT - padB - dotsExtra
        rowStep = rowsH / MAX_ROWS_PER_PAGE
        panelPadT = padT
        panelPadB = padB

        val scaleRef = rowStep / 72f
        settingsFontScale = 0.62f * scaleRef
        settingsHeadingScale = 0.95f * scaleRef

        recomputePanelHeight()
    }

    private fun recomputePanelHeight() {
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight

        currentSubPage = max(0, min(currentSubPage, subPageCount(currentCat) - 1))

        val dotsExtra = rowStep * 0.5f
        panelH = MAX_ROWS_PER_PAGE * rowStep + panelPadT + panelPadB + dotsExtra
        panelX = vw / 2f - panelW / 2f
        panelY = vh / 2f - panelH / 2f

        backW = panelH * 0.075f
        backH = backW
        backX = panelX + 12f
        backY = panelY + panelH - backH - 12f

        rowStartY = panelY + panelH - panelPadT

        arrowSize = backH
        arrowY = panelY + panelH - arrowSize - 12f
        arrowLeftX = panelX + backW + 20f
        arrowRightX = panelX + panelW - arrowSize - 12f

        lastPanelW = -1
    }

    override fun update(delta: Float) {
        if (!settingsOpen && !infoOpen) {
            btnPlay.update(delta)
            btnSettings.update(delta)
            btnInfo.update(delta)
        }
        if (settingsOpen) handleSettingsInput()
        else if (infoOpen) handleInfoInput()
        else handleMenuInput()
    }

    override fun draw() {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        game.batch.draw(title, titleX, titleY, titleW, titleH)
        btnPlay.draw(game.batch)
        btnSettings.draw(game.batch)
        btnInfo.draw(game.batch)
        game.batch.end()

        if (settingsOpen) drawSettingsOverlay()
        else if (infoOpen) drawInfoOverlay()
    }

    private fun drawSettingsOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_OVERLAY
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        val texW = panelW.toInt()
        val texH = panelH.toInt()
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            panelTexture?.dispose()
            panelTexture = createRoundedRect(texW, texH, (26f * (panelW / 740f)).toInt(), COL_PANEL)
            lastPanelW = texW
            lastPanelH = texH
        }

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        panelTexture?.let { game.batch.draw(it, panelX, panelY) }
        game.batch.draw(backArrow, backX, backY, backW, backH)
        game.batch.end()

        drawHeadingAndTabs()
        drawSettingsRows(getPageRows(currentCat, currentSubPage))
        drawSubPageDots()
    }

    private fun drawHeadingAndTabs() {
        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        font.data.setScale(settingsHeadingScale)
        layout.setText(font, "Settings")
        drawTextWithShadow(
            font, "Settings",
            (panelX + panelW / 2f) - (layout.width / 2f),
            panelY + panelH - 16f,
            COL_HEADING
        )

        val tabFontScale = settingsFontScale * 0.92f
        font.data.setScale(tabFontScale)
        val tabY = panelY + panelH - panelPadT * 0.47f
        layout.setText(font, CAT_NAMES[0])
        val tabTextH = layout.height
        for (i in 0 until CAT_COUNT) {
            val tabW = panelW / CAT_COUNT
            val tabCX = panelX + tabW * i + tabW / 2f
            val tabColor = if (i == currentCat) COL_TAB_ACT else COL_TAB_INACT
            layout.setText(font, CAT_NAMES[i])

            drawTextWithShadow(font, CAT_NAMES[i], tabCX - layout.width / 2f, tabY, tabColor)
        }
        font.data.setScale(1f)
        game.batch.end()

        val tabW = panelW / CAT_COUNT
        val tabCX = panelX + tabW * currentCat
        val underlineY = tabY - tabTextH - 3f
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_TAB_ACT
        shapes.rect(tabCX + 8f, underlineY, tabW - 16f, 3f)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        drawArrow(arrowLeftX, arrowY, arrowSize, true)
        drawArrow(arrowRightX, arrowY, arrowSize, false)
    }

    private fun drawArrow(x: Float, y: Float, size: Float, pointLeft: Boolean) {
        val cx = x + size / 2f
        val cy = y + size / 2f
        val hs = size * 0.28f
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_DIM
        if (pointLeft) shapes.triangle(cx + hs, cy + hs, cx + hs, cy - hs, cx - hs, cy)
        else shapes.triangle(cx - hs, cy + hs, cx - hs, cy - hs, cx + hs, cy)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawSubPageDots() {
        val total = subPageCount(currentCat)
        if (total <= 1) return
        val dotR = 5f
        val dotGap = dotR * 2f + 6f
        val totalW = total * dotGap - 6f
        val startX = panelX + panelW / 2f - totalW / 2f + dotR
        val dotY = panelY + panelPadB / 2f + dotR

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until total) {
            shapes.color = if (i == currentSubPage) COL_DOT_ACT else COL_DOT_INACT
            shapes.circle(startX + i * dotGap, dotY, dotR, 16)
        }
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawSettingsRows(rows: Array<SettingRow>) {
        val s = game.settingsManager
        val horizontalPadding = 45f
        for (i in 0 until rows.size) {
            val row = rows.get(i)
            val ry = rowY(i)
            when (row.type) {
                RowType.TOGGLE -> drawToggleRow(ry, row.label, getToggleValue(row.id, s), horizontalPadding)
                RowType.SLIDER -> {
                    val `val` = when (row.id) {
                        "uiPadding" -> s.uiPadding / 50f
                        "practiceOpacity" -> s.practiceButtonOpacity
                        else -> s.musicVolume
                    }
                    drawSliderRow(ry, row.label, `val`, horizontalPadding)
                }
                RowType.INT_FIELD -> drawIntFieldRow(ry, row.label, s.fpsCapValue, horizontalPadding)
            }
        }
    }

    private fun getToggleValue(id: String, s: SettingsManager): Boolean {
        return when (id) {
            "menuMusic" -> s.menuMusicEnabled
            "hitboxes" -> s.showHitboxes
            "hitboxesDeath" -> s.showHitboxesOnDeath
            "lockCursor" -> s.lockCursorInGame
            "showFps" -> s.showFps
            "capFps" -> s.capFps
            "vsync" -> s.enableVsync
            "showPercentage" -> s.showPercentage
            "showProgressBar" -> s.showProgressBar
            "showAttempts" -> s.showAttempts
            "showBest" -> s.showBest
            else -> false
        }
    }

    private fun drawToggleRow(ry: Float, label: String, value: Boolean, horizontalPadding: Float) {
        val rightEdge = panelX + panelW - horizontalPadding
        val pillH = rowStep * 0.35f
        val pillW = pillH * 2.1f
        val pillX = rightEdge - pillW
        val pillY = ry - pillH / 2f
        val r = pillH / 2f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = if (value) COL_ON else COL_OFF
        shapes.circle(pillX + r, pillY + r, r, 24)
        shapes.circle(pillX + pillW - r, pillY + r, r, 24)
        shapes.rect(pillX + r, pillY, pillW - pillH, pillH)
        shapes.color = COL_THUMB
        val thumbCX = if (value) (pillX + pillW - r) else (pillX + r)
        shapes.circle(thumbCX, pillY + r, r - r * 0.3f, 24)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        font.data.setScale(settingsFontScale)
        layout.setText(font, label)
        drawTextWithShadow(font, label, panelX + horizontalPadding, ry + layout.height / 2f, COL_LABEL)
        font.data.setScale(1f)
        game.batch.end()
    }

    private fun drawSliderRow(ry: Float, label: String, value: Float, horizontalPadding: Float) {
        val rightEdge = panelX + panelW - horizontalPadding
        val trackW = panelW * 0.34f
        val trackH = rowStep * 0.06f
        val trackX = rightEdge - trackW
        val trackY = ry - trackH / 2f
        val thumbR = rowStep * 0.15f
        val fillW = trackW * value
        val thumbCX = trackX + fillW

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_TRACK
        shapes.rect(trackX, trackY, trackW, trackH)
        shapes.color = COL_FILL
        if (fillW > 0) shapes.rect(trackX, trackY, fillW, trackH)
        shapes.color = COL_THUMB
        shapes.circle(thumbCX, ry, thumbR, 24)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        font.data.setScale(settingsFontScale)
        font.color = COL_LABEL
        layout.setText(font, label)
        drawTextWithShadow(font, label, panelX + horizontalPadding, ry + layout.height / 2f, COL_LABEL)
        font.data.setScale(settingsFontScale * 0.77f)
        font.color = COL_DIM
        val pct = "${round(value * 100f).toInt()}%"
        layout.setText(font, pct)
        drawTextWithShadow(font, pct, trackX - layout.width - 15f, ry + layout.height / 2f, COL_DIM)
        font.data.setScale(1f)
        game.batch.end()
    }

    private fun drawIntFieldRow(ry: Float, label: String, value: Int, horizontalPadding: Float) {
        val rightEdge = panelX + panelW - horizontalPadding
        val boxH = rowStep * 0.40f
        val boxW = boxH * 3.0f
        val boxX = rightEdge - boxW
        val boxY = ry - boxH / 2f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_INPUT_BG
        shapes.rect(boxX, boxY, boxW, boxH)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = if (fpsInputActive) COL_INPUT_BD else Color(COL_INPUT_BD.r, COL_INPUT_BD.g, COL_INPUT_BD.b, 0.4f)
        shapes.rect(boxX, boxY, boxW, boxH)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        val display = if (fpsInputActive) {
            fpsInputBuffer.toString() + if (System.currentTimeMillis() / 500 % 2 == 0L) "|" else " "
        } else {
            value.toString()
        }

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        font.data.setScale(settingsFontScale)
        font.color = COL_LABEL
        layout.setText(font, label)
        drawTextWithShadow(font, label, panelX + horizontalPadding, ry + layout.height / 2f, COL_LABEL)
        font.data.setScale(settingsFontScale * 0.95f)
        val valueColor = if (fpsInputActive) Color.WHITE else COL_DIM
        font.color = valueColor
        layout.setText(font, display)

        drawTextWithShadow(
            font, display,
            boxX + boxW / 2f - layout.width / 2f,
            ry + layout.height / 2f,
            valueColor
        )
        font.data.setScale(1f)
        game.batch.end()
    }

    private fun handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.screen = LevelSelectScreen(game)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && System.getProperty("devMode") != null) {
            game.screen = LevelEditorScreen(game)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }

        val t = unproject()

        if (Gdx.input.justTouched()) {
            btnPlay.onTouchDown(t.x, t.y)
            btnSettings.onTouchDown(t.x, t.y)
            btnInfo.onTouchDown(t.x, t.y)
        }
        if (!Gdx.input.isTouched) {
            btnPlay.onTouchUp(t.x, t.y)
            btnSettings.onTouchUp(t.x, t.y)
            btnInfo.onTouchUp(t.x, t.y)
        }
    }

    private fun handleSettingsInput() {
        val s = game.settingsManager

        if (fpsInputActive) {
            for (k in Input.Keys.NUM_0..Input.Keys.NUM_9) {
                if (Gdx.input.isKeyJustPressed(k)) fpsInputBuffer.append((k - Input.Keys.NUM_0).toString())
            }
            for (k in Input.Keys.NUMPAD_0..Input.Keys.NUMPAD_9) {
                if (Gdx.input.isKeyJustPressed(k)) fpsInputBuffer.append((k - Input.Keys.NUMPAD_0).toString())
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && fpsInputBuffer.isNotEmpty()) {
                fpsInputBuffer.deleteCharAt(fpsInputBuffer.length - 1)
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) confirmFpsInput(s)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (fpsInputActive) confirmFpsInput(s)
            else closeSettings()
            return
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) navigate(-1, s)
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) navigate(1, s)

        if (Gdx.input.isTouched && draggingSlider) {
            val tx = unproject().x
            val trackX = panelX + panelW - 28f - panelW * 0.36f
            val trackW = panelW * 0.36f
            val norm = max(0f, min(1f, (tx - trackX) / trackW))

            val pageRows = getPageRows(currentCat, currentSubPage)
            if (draggingSliderRow >= 0 && draggingSliderRow < pageRows.size) {
                val row = pageRows.get(draggingSliderRow)
                if ("volume" == row.id) {
                    s.musicVolume = norm
                    game.soundManager.setMusicVolume(s.musicVolume)
                } else if ("uiPadding" == row.id) {
                    s.uiPadding = norm * 50f
                } else if ("practiceOpacity" == row.id) {
                    s.practiceButtonOpacity = norm
                }
            }
        }
        if (!Gdx.input.isTouched) {
            if (draggingSlider) s.save()
            draggingSlider = false
            draggingSliderRow = -1
        }

        if (!Gdx.input.justTouched()) return
        val t = unproject()

        val pageRows = getPageRows(currentCat, currentSubPage)

        if (fpsInputActive) {
            var fpsIdx = -1
            for (i in 0 until pageRows.size) {
                if ("fpsValue" == pageRows.get(i).id) {
                    fpsIdx = i
                    break
                }
            }
            if (fpsIdx < 0 || !hitIntBox(t, rowY(fpsIdx))) confirmFpsInput(s)
        }

        if (hits(t, backX, backY, backW, backH)) {
            closeSettings()
            return
        }

        if (hits(t, arrowLeftX, arrowY, arrowSize, arrowSize)) {
            navigate(-1, s)
            return
        }
        if (hits(t, arrowRightX, arrowY, arrowSize, arrowSize)) {
            navigate(1, s)
            return
        }

        for (i in 0 until CAT_COUNT) {
            val tabW = panelW / CAT_COUNT
            val tabX = panelX + tabW * i
            val tabTopY = panelY + panelH - 42f
            if (t.x >= tabX && t.x <= tabX + tabW && t.y >= tabTopY - 28f && t.y <= tabTopY + 8f) {
                if (i != currentCat) {
                    confirmFpsInput(s)
                    draggingSlider = false
                    currentCat = i
                    currentSubPage = 0
                    recomputePanelHeight()
                }
                return
            }
        }

        for (i in 0 until pageRows.size) {
            val row = pageRows.get(i)
            val ry = rowY(i)
            when (row.type) {
                RowType.TOGGLE -> if (hitPill(t, ry)) handleToggle(row.id, s)
                RowType.SLIDER -> {
                    val `val` = when (row.id) {
                        "uiPadding" -> s.uiPadding / 50f
                        "practiceOpacity" -> s.practiceButtonOpacity
                        else -> s.musicVolume
                    }
                    if (hitSliderThumb(t, ry, `val`)) {
                        draggingSlider = true
                        draggingSliderRow = i
                    }
                }
                RowType.INT_FIELD -> if (hitIntBox(t, ry) && !fpsInputActive) {
                    if (Gdx.app.type == com.badlogic.gdx.Application.ApplicationType.Desktop) {
                        fpsInputActive = true
                        fpsInputBuffer.setLength(0)
                        fpsInputBuffer.append(s.fpsCapValue)
                    } else {
                        Gdx.input.getTextInput(object : Input.TextInputListener {
                            override fun input(text: String) {
                                try {
                                    val `val` = text.trim().toInt()
                                    if (`val` > 0) {
                                        s.fpsCapValue = `val`
                                        s.applyFpsCap()
                                        s.save()
                                    }
                                } catch (ignored: NumberFormatException) {
                                }
                            }
                            override fun canceled() {}
                        }, "FPS Limit", s.fpsCapValue.toString(), "Enter FPS cap")
                    }
                }
            }
        }
    }

    private fun handleToggle(id: String, s: SettingsManager) {
        when (id) {
            "menuMusic" -> {
                s.menuMusicEnabled = !s.menuMusicEnabled
                if (s.menuMusicEnabled) game.soundManager.playMenuMusic()
                else game.soundManager.stopMenuMusic()
                s.save()
            }
            "hitboxes" -> {
                s.showHitboxes = !s.showHitboxes
                s.save()
            }
            "hitboxesDeath" -> {
                s.showHitboxesOnDeath = !s.showHitboxesOnDeath
                s.save()
            }
            "lockCursor" -> {
                s.lockCursorInGame = !s.lockCursorInGame
                s.save()
            }
            "showFps" -> {
                s.showFps = !s.showFps
                s.save()
            }
            "showPercentage" -> {
                s.showPercentage = !s.showPercentage
                s.save()
            }
            "showProgressBar" -> {
                s.showProgressBar = !s.showProgressBar
                s.save()
            }
            "showAttempts" -> {
                s.showAttempts = !s.showAttempts
                s.save()
            }
            "showBest" -> {
                s.showBest = !s.showBest
                s.save()
            }
            "capFps" -> {
                s.capFps = !s.capFps
                s.applyFpsCap()
                s.save()
                if (!s.capFps) fpsInputActive = false
                recomputePanelHeight()
            }
            "vsync" -> {
                s.enableVsync = !s.enableVsync
                s.applyVsync()
                s.save()
            }
        }
    }

    private fun navigate(dir: Int, s: SettingsManager) {
        confirmFpsInput(s)
        draggingSlider = false
        draggingSliderRow = -1

        val newSub = currentSubPage + dir
        val pages = subPageCount(currentCat)

        if (newSub in 0 until pages) {
            currentSubPage = newSub
        } else {
            currentCat = (currentCat + dir + CAT_COUNT) % CAT_COUNT
            currentSubPage = if (dir > 0) 0 else subPageCount(currentCat) - 1
        }
        recomputePanelHeight()
    }

    private fun confirmFpsInput(s: SettingsManager) {
        if (!fpsInputActive) return
        fpsInputActive = false
        if (fpsInputBuffer.isNotEmpty()) {
            try {
                val `val` = fpsInputBuffer.toString().toInt()
                if (`val` > 0) {
                    s.fpsCapValue = `val`
                    s.applyFpsCap()
                    s.save()
                }
            } catch (ignored: NumberFormatException) {
            }
        }
        fpsInputBuffer.setLength(0)
    }

    private fun closeSettings() {
        settingsOpen = false
        draggingSlider = false
        draggingSliderRow = -1
        fpsInputActive = false
        fpsInputBuffer.setLength(0)
        currentCat = CAT_GAMEPLAY
        currentSubPage = 0
    }

    private class InfoLine(val text: String, val url: String) {
        var y: Float = 0f
    }

    private val creditLines = Array<InfoLine>().apply {
        add(InfoLine("Euphoria by ForeverBound", "https://www.newgrounds.com/audio/listen/680209"))
        add(InfoLine("Icefield by Waterflame", "https://www.newgrounds.com/audio/listen/1379251"))
        add(InfoLine("Hypercharge by Cobalt039", "https://www.newgrounds.com/audio/listen/1538780"))
        add(InfoLine("Bounce by Waterflame", "https://www.newgrounds.com/audio/listen/654553"))
        add(InfoLine("Rhythm Factory by Waterflame", "https://www.newgrounds.com/audio/listen/1533782"))
        add(InfoLine("Vulg by OcularNebula", "https://www.newgrounds.com/audio/listen/954091"))
    }

    private val socialLines = Array<InfoLine>().apply {
        add(InfoLine("YouTube: @LunarPixelGames", "https://www.youtube.com/@LunarPixelGames"))
        add(InfoLine("GitHub: LunarPixelGames", "https://github.com/LunarPixelGames"))
    }

    private val privacyPolicyLine = InfoLine("Privacy Policy", "https://lunarpixelgames.github.io/RhythmicRush/PRIVACY")

    private fun handleInfoInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeInfo()
            return
        }

        if (!Gdx.input.justTouched()) return
        val t = unproject()

        if (hits(t, backX, backY, backW, backH)) {
            closeInfo()
            return
        }

        for (i in 0 until INFO_TAB_COUNT) {
            val tabW = panelW / INFO_TAB_COUNT
            val tabX = panelX + tabW * i
            val tabTopY = panelY + panelH - 42f
            if (t.x >= tabX && t.x <= tabX + tabW && t.y >= tabTopY - 28f && t.y <= tabTopY + 8f) {
                if (i != currentInfoTab) {
                    currentInfoTab = i
                }
                return
            }
        }

        var lines: Array<InfoLine>? = null
        if (currentInfoTab == INFO_TAB_CREDITS) lines = creditLines
        else if (currentInfoTab == INFO_TAB_SOCIALS) lines = socialLines

        if (lines != null) {
            val lineH = 32f * (panelH / 480f)
            for (line in lines) {
                if (t.x >= panelX + 35f && t.x <= panelX + panelW - 35f &&
                    t.y >= line.y - lineH && t.y <= line.y) {
                    Gdx.net.openURI(line.url)
                    return
                }
            }
        }

        if (currentInfoTab == INFO_TAB_SOCIALS) {
            val lineH = 32f * (panelH / 480f)
            if (t.x >= panelX + panelW / 2f - 100f && t.x <= panelX + panelW / 2f + 100f &&
                t.y >= privacyPolicyLine.y - lineH && t.y <= privacyPolicyLine.y) {
                Gdx.net.openURI(privacyPolicyLine.url)
            }
        }
    }

    private fun closeInfo() {
        infoOpen = false
        currentInfoTab = INFO_TAB_HOWTOPLAY
    }

    private fun drawInfoOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_OVERLAY
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        val texW = panelW.toInt()
        val texH = panelH.toInt()
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            panelTexture?.dispose()
            panelTexture = createRoundedRect(texW, texH, (26f * (panelW / 740f)).toInt(), COL_PANEL)
            lastPanelW = texW
            lastPanelH = texH
        }

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        panelTexture?.let { game.batch.draw(it, panelX, panelY) }
        game.batch.draw(backArrow, backX, backY, backW, backH)

        font.data.setScale(settingsHeadingScale)
        layout.setText(font, "Information")
        drawTextWithShadow(
            font, "Information",
            (panelX + panelW / 2f) - (layout.width / 2f),
            panelY + panelH - 16f,
            COL_HEADING
        )

        val tabFontScale = settingsFontScale * 0.85f
        font.data.setScale(tabFontScale)
        val tabY = panelY + panelH - panelPadT * 0.47f
        for (i in 0 until INFO_TAB_COUNT) {
            val tabW = panelW / INFO_TAB_COUNT
            val tabCX = panelX + tabW * i + tabW / 2f
            val tabColor = if (i == currentInfoTab) COL_TAB_ACT else COL_TAB_INACT
            layout.setText(font, INFO_TAB_NAMES[i])

            drawTextWithShadow(font, INFO_TAB_NAMES[i], tabCX - layout.width / 2f, tabY, tabColor)
        }

        val tabW = panelW / INFO_TAB_COUNT
        val tabCX = panelX + tabW * currentInfoTab
        layout.setText(font, INFO_TAB_NAMES[currentInfoTab])
        val tabTextH = layout.height
        val underlineY = tabY - tabTextH - 3f

        game.batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_TAB_ACT
        shapes.rect(tabCX + 8f, underlineY, tabW - 16f, 3f)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.begin()
        val contentX = panelX + 45f
        val contentY = panelY + panelH - panelPadT - 15f
        val lineSpacing = 42f * (panelH / 480f)
        font.data.setScale(settingsFontScale * 0.82f)

        if (currentInfoTab == INFO_TAB_HOWTOPLAY) {
            val lines = arrayOf(
                "Welcome to Rhythmic Rush!",
                "",
                "Jump and fly through obstacles in this",
                "rhythm-based platformer. Timing is key!",
                "",
                "Controls:",
                "- Space / Click: Jump or fly up",
                "- ESC: Pause or Go Back"
            )
            for (i in lines.indices) {
                font.color = COL_LABEL
                font.draw(game.batch, lines[i], contentX, contentY - i * lineSpacing)
            }
        } else if (currentInfoTab == INFO_TAB_CREDITS) {
            font.color = COL_HEADING
            font.draw(game.batch, "Music Credits (Click to open):", contentX, contentY)
            for (i in 0 until creditLines.size) {
                val line = creditLines[i]
                line.y = contentY - (i + 1.25f) * lineSpacing
                font.color = COL_TAB_ACT
                font.draw(game.batch, "- " + line.text, contentX, line.y)
            }
        } else if (currentInfoTab == INFO_TAB_SOCIALS) {
            font.color = COL_HEADING
            font.draw(game.batch, "Follow Us (Click to open):", contentX, contentY)
            for (i in 0 until socialLines.size) {
                val line = socialLines[i]
                line.y = contentY - (i + 1.25f) * lineSpacing
                font.color = COL_TAB_ACT
                font.draw(game.batch, line.text, contentX, line.y)
            }
            font.color = COL_DIM
            font.draw(game.batch, "Thanks for playing Rhythmic Rush!", contentX, contentY - 4.5f * lineSpacing)

            font.color = COL_TAB_ACT
            layout.setText(font, privacyPolicyLine.text)
            privacyPolicyLine.y = panelY + panelPadB + 15f
            font.draw(game.batch, privacyPolicyLine.text, panelX + panelW / 2f - layout.width / 2f, privacyPolicyLine.y)
        }

        font.data.setScale(1f)
        game.batch.end()
    }

    private fun rowY(i: Int): Float {
        return rowStartY - i * rowStep
    }

    private fun hitPill(t: Vector2, ry: Float): Boolean {
        val pillH = rowStep * 0.38f
        val pillW = pillH * 1.92f
        val pillX = panelX + panelW - 28f - pillW
        val pillY = ry - pillH / 2f
        return t.x >= pillX - 4f && t.x <= pillX + pillW + 4f &&
                t.y >= pillY - 4f && t.y <= pillY + pillH + 4f
    }

    private fun hitSliderThumb(t: Vector2, ry: Float, value: Float): Boolean {
        val trackW = panelW * 0.36f
        val trackX = panelX + panelW - 28f - trackW
        val thumbR = rowStep * 0.16f
        val thumbCX = trackX + trackW * value
        return t.x >= trackX - thumbR && t.x <= trackX + trackW + thumbR &&
                t.y >= ry - thumbR && t.y <= ry + thumbR
    }

    private fun hitIntBox(t: Vector2, ry: Float): Boolean {
        val boxH = rowStep * 0.44f
        val boxW = boxH * 2.8f
        val boxX = panelX + panelW - 28f - boxW
        val boxY = ry - boxH / 2f
        return t.x >= boxX && t.x <= boxX + boxW && t.y >= boxY && t.y <= boxY + boxH
    }

    private fun unproject(): Vector2 {
        val v = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(v)
        return v
    }

    private fun createRoundedRect(w: Int, h: Int, r: Int, color: Color): Texture {
        val pm = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pm.setColor(0f, 0f, 0f, 0f)
        pm.fill()
        pm.setColor(color)
        pm.fillRectangle(r, 0, w - 2 * r, h)
        pm.fillRectangle(0, r, w, h - 2 * r)
        pm.fillCircle(r, r, r)
        pm.fillCircle(w - r, r, r)
        pm.fillCircle(r, h - r, r)
        pm.fillCircle(w - r, h - r, r)
        val t = Texture(pm)
        pm.dispose()
        return t
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        updateScaledSizes()
    }

    override fun dispose() {
        if (::shapes.isInitialized) shapes.dispose()
        panelTexture?.dispose()
        super.dispose()
    }
}
