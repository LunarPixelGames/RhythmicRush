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
import io.github.msameer0.rhythmicrush.screens.LevelEditorScreen

/**
 * The primary entry point screen for the game, providing access to the main gameplay
 * and a comprehensive settings menu.
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

    private class InfoLine(val text: String, val url: String, var y: Float = 0f)

    private val creditLines = arrayOf(
        InfoLine("Euphoria by ForeverBound", "https://www.newgrounds.com/audio/listen/680209"),
        InfoLine("Icefield by Waterflame", "https://www.newgrounds.com/audio/listen/1379251"),
        InfoLine("Hypercharge by Cobalt039", "https://www.newgrounds.com/audio/listen/1538780"),
        InfoLine("Bounce by Waterflame", "https://www.newgrounds.com/audio/listen/654553"),
        InfoLine("Rhythm Factory by Waterflame", "https://www.newgrounds.com/audio/listen/1533782"),
        InfoLine("Vulg by OcularNebula", "https://www.newgrounds.com/audio/listen/954091")
    )

    private val socialLines = arrayOf(
        InfoLine("YouTube: @LunarPixelGames", "https://www.youtube.com/@LunarPixelGames"),
        InfoLine("GitHub: LunarPixelGames", "https://github.com/LunarPixelGames")
    )

    private val privacyPolicyLine = InfoLine("Privacy Policy", "https://lunarpixelgames.github.io/RhythmicRush/PRIVACY")

    private var draggingSlider = false
    private var draggingSliderRow = -1

    private var fpsInputActive = false
    private val fpsInputBuffer = StringBuilder()

    private var panelTexture: Texture? = null
    private var lastPanelW = -1
    private var lastPanelH = -1

    private enum class RowType { TOGGLE, SLIDER, INT_FIELD }
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
            0.2f + 0.6f * MathUtils.random(),
            0.2f + 0.6f * MathUtils.random(),
            0.2f + 0.6f * MathUtils.random(), 1f
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
            if (desktop) rows.add(SettingRow(RowType.TOGGLE, "Lock Cursor in Game", "lockCursor"))
        } else {
            rows.add(SettingRow(RowType.TOGGLE, "Show FPS", "showFps"))
            if (desktop) {
                rows.add(SettingRow(RowType.TOGGLE, "Cap FPS", "capFps"))
                if (s.capFps) rows.add(SettingRow(RowType.INT_FIELD, "FPS Limit", "fpsValue"))
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
        for (i in start until end) pageRows.add(all.get(i))
        return pageRows
    }

    private fun subPageCount(cat: Int): Int {
        val total = buildAllRows(cat).size
        return max(1, ceil(total.toFloat() / MAX_ROWS_PER_PAGE).toInt())
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
        if (::btnSettings.isInitialized) btnSettings.setBounds(20f, 10f, settingsW, settingsH)

        if (::btnInfo.isInitialized) btnInfo.setBounds(vw - settingsW - 20f, 10f, settingsW, settingsH)

        panelW = min(vw * 0.78f, 780f)
        val targetH = vh * PANEL_HEIGHT_FRACTION
        rowStep = (targetH * 0.72f) / MAX_ROWS_PER_PAGE
        panelPadT = targetH * 0.22f
        panelPadB = targetH * 0.045f
        val scaleRef = rowStep / 72f
        settingsFontScale = 0.62f * scaleRef
        settingsHeadingScale = 0.95f * scaleRef
        recomputePanelHeight()
    }

    private fun recomputePanelHeight() {
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight
        currentSubPage = MathUtils.clamp(currentSubPage, 0, subPageCount(currentCat) - 1)
        panelH = MAX_ROWS_PER_PAGE * rowStep + panelPadT + panelPadB + rowStep * 0.5f
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
        game.batch.begin()
        panelTexture?.let { game.batch.draw(it, panelX, panelY) }
        game.batch.draw(backArrow, backX, backY, backW, backH)
        game.batch.end()
        drawHeadingAndTabs()
        drawSettingsRows(getPageRows(currentCat, currentSubPage))
        drawSubPageDots()
    }

    private fun drawHeadingAndTabs() {
        game.batch.begin()
        font.data.setScale(settingsHeadingScale)
        layout.setText(font, "Settings")
        drawTextWithShadow(font, "Settings", (panelX + panelW / 2f) - (layout.width / 2f), panelY + panelH - 16f, COL_HEADING)
        font.data.setScale(settingsFontScale * 0.92f)
        val tabY = panelY + panelH - panelPadT * 0.47f
        for (i in 0 until CAT_COUNT) {
            val tabW = panelW / CAT_COUNT
            val tabCX = panelX + tabW * i + tabW / 2f
            layout.setText(font, CAT_NAMES[i])
            drawTextWithShadow(font, CAT_NAMES[i], tabCX - layout.width / 2f, tabY, if (i == currentCat) COL_TAB_ACT else COL_TAB_INACT)
        }
        game.batch.end()
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_TAB_ACT
        shapes.rect(panelX + (panelW / CAT_COUNT) * currentCat + 8f, tabY - 20f, (panelW / CAT_COUNT) - 16f, 3f)
        shapes.end()
        drawArrow(arrowLeftX, arrowY, arrowSize, true)
        drawArrow(arrowRightX, arrowY, arrowSize, false)
    }

    private fun drawArrow(x: Float, y: Float, size: Float, pointLeft: Boolean) {
        val cx = x + size / 2f
        val cy = y + size / 2f
        val hs = size * 0.28f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_DIM
        if (pointLeft) shapes.triangle(cx + hs, cy + hs, cx + hs, cy - hs, cx - hs, cy)
        else shapes.triangle(cx - hs, cy + hs, cx - hs, cy - hs, cx + hs, cy)
        shapes.end()
    }

    private fun drawSubPageDots() {
        val total = subPageCount(currentCat)
        if (total <= 1) return
        val dotR = 5f
        val dotGap = 16f
        val startX = panelX + panelW / 2f - (total * dotGap - 6f) / 2f + dotR
        val dotY = panelY + panelPadB / 2f + dotR
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until total) {
            shapes.color = if (i == currentSubPage) COL_DOT_ACT else COL_DOT_INACT
            shapes.circle(startX + i * dotGap, dotY, dotR, 16)
        }
        shapes.end()
    }

    private fun drawSettingsRows(rows: Array<SettingRow>) {
        val s = game.settingsManager
        for (i in 0 until rows.size) {
            val row = rows.get(i)
            val ry = rowY(i)
            when (row.type) {
                RowType.TOGGLE -> drawToggleRow(ry, row.label, getToggleValue(row.id, s), 45f)
                RowType.SLIDER -> {
                    val v = when (row.id) {
                        "uiPadding" -> s.uiPadding / 50f
                        "practiceOpacity" -> s.practiceButtonOpacity
                        else -> s.musicVolume
                    }
                    drawSliderRow(ry, row.label, v, 45f)
                }
                RowType.INT_FIELD -> drawIntFieldRow(ry, row.label, s.fpsCapValue, 45f)
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

    private fun drawToggleRow(ry: Float, label: String, value: Boolean, hp: Float) {
        val pillH = rowStep * 0.35f
        val pillW = pillH * 2.1f
        val pillX = panelX + panelW - hp - pillW
        val pillY = ry - pillH / 2f
        val r = pillH / 2f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = if (value) COL_ON else COL_OFF
        shapes.circle(pillX + r, pillY + r, r, 24)
        shapes.circle(pillX + pillW - r, pillY + r, r, 24)
        shapes.rect(pillX + r, pillY, pillW - pillH, pillH)
        shapes.color = COL_THUMB
        shapes.circle(if (value) (pillX + pillW - r) else (pillX + r), pillY + r, r * 0.7f, 24)
        shapes.end()
        game.batch.begin()
        font.data.setScale(settingsFontScale)
        drawTextWithShadow(font, label, panelX + hp, ry + layout.height / 2f, COL_LABEL)
        game.batch.end()
    }

    private fun drawSliderRow(ry: Float, label: String, value: Float, hp: Float) {
        val trackW = panelW * 0.34f
        val trackH = rowStep * 0.06f
        val trackX = panelX + panelW - hp - trackW
        val thumbR = rowStep * 0.15f
        val fillW = trackW * value
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_TRACK
        shapes.rect(trackX, ry - trackH / 2f, trackW, trackH)
        shapes.color = COL_FILL
        if (fillW > 0) shapes.rect(trackX, ry - trackH / 2f, fillW, trackH)
        shapes.color = COL_THUMB
        shapes.circle(trackX + fillW, ry, thumbR, 24)
        shapes.end()
        game.batch.begin()
        font.data.setScale(settingsFontScale)
        drawTextWithShadow(font, label, panelX + hp, ry + layout.height / 2f, COL_LABEL)
        val pct = "${round(value * 100f).toInt()}%"
        font.data.setScale(settingsFontScale * 0.77f)
        layout.setText(font, pct)
        drawTextWithShadow(font, pct, trackX - layout.width - 15f, ry + layout.height / 2f, COL_DIM)
        game.batch.end()
    }

    private fun drawIntFieldRow(ry: Float, label: String, value: Int, hp: Float) {
        val boxH = rowStep * 0.40f
        val boxW = boxH * 3.0f
        val boxX = panelX + panelW - hp - boxW
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_INPUT_BG
        shapes.rect(boxX, ry - boxH / 2f, boxW, boxH)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = if (fpsInputActive) COL_INPUT_BD else Color(COL_INPUT_BD.r, COL_INPUT_BD.g, COL_INPUT_BD.b, 0.4f)
        shapes.rect(boxX, ry - boxH / 2f, boxW, boxH)
        shapes.end()
        val display = if (fpsInputActive) fpsInputBuffer.toString() + (if (System.currentTimeMillis() / 500 % 2 == 0L) "|" else " ") else value.toString()
        game.batch.begin()
        font.data.setScale(settingsFontScale)
        drawTextWithShadow(font, label, panelX + hp, ry + layout.height / 2f, COL_LABEL)
        font.data.setScale(settingsFontScale * 0.95f)
        layout.setText(font, display)
        drawTextWithShadow(font, display, boxX + boxW / 2f - layout.width / 2f, ry + layout.height / 2f, if (fpsInputActive) Color.WHITE else COL_DIM)
        game.batch.end()
    }

    private fun handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) game.screen = LevelSelectScreen(game)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit()
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) game.screen = LevelEditorScreen(game)
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
            for (k in Input.Keys.NUM_0..Input.Keys.NUM_9) if (Gdx.input.isKeyJustPressed(k)) fpsInputBuffer.append((k - Input.Keys.NUM_0).toString())
            for (k in Input.Keys.NUMPAD_0..Input.Keys.NUMPAD_9) if (Gdx.input.isKeyJustPressed(k)) fpsInputBuffer.append((k - Input.Keys.NUMPAD_0).toString())
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && fpsInputBuffer.isNotEmpty()) fpsInputBuffer.deleteCharAt(fpsInputBuffer.length - 1)
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) confirmFpsInput(s)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { if (fpsInputActive) confirmFpsInput(s) else closeSettings() }
        if (Gdx.input.isTouched && draggingSlider) {
            val norm = MathUtils.clamp((unproject().x - (panelX + panelW - 45f - panelW * 0.34f)) / (panelW * 0.34f), 0f, 1f)
            val rows = getPageRows(currentCat, currentSubPage)
            if (draggingSliderRow in 0 until rows.size) {
                val r = rows.get(draggingSliderRow)
                if (r.id == "volume") { s.musicVolume = norm; game.soundManager.setMusicVolume(norm) }
                else if (r.id == "uiPadding") s.uiPadding = norm * 50f
                else if (r.id == "practiceOpacity") s.practiceButtonOpacity = norm
            }
        }
        if (!Gdx.input.isTouched) { if (draggingSlider) s.save(); draggingSlider = false; draggingSliderRow = -1 }
        if (!Gdx.input.justTouched()) return
        val t = unproject()
        if (hits(t, backX, backY, backW, backH)) { closeSettings(); return }
        if (hits(t, arrowLeftX, arrowY, arrowSize, arrowSize)) { navigate(-1, s); return }
        if (hits(t, arrowRightX, arrowY, arrowSize, arrowSize)) { navigate(1, s); return }
        for (i in 0 until CAT_COUNT) {
            val tw = panelW / CAT_COUNT
            if (t.x in (panelX + tw * i)..(panelX + tw * (i+1)) && t.y > panelY + panelH - 80f) {
                currentCat = i; currentSubPage = 0; recomputePanelHeight(); return
            }
        }
        val pRows = getPageRows(currentCat, currentSubPage)
        for (i in 0 until pRows.size) {
            val ry = rowY(i)
            val r = pRows.get(i)
            if (r.type == RowType.TOGGLE && hitPill(t, ry)) handleToggle(r.id, s)
            else if (r.type == RowType.SLIDER && hitSliderThumb(t, ry, 0.5f)) { draggingSlider = true; draggingSliderRow = i }
            else if (r.type == RowType.INT_FIELD && hitIntBox(t, ry)) { fpsInputActive = true; fpsInputBuffer.setLength(0); fpsInputBuffer.append(s.fpsCapValue) }
        }
    }

    private fun handleToggle(id: String, s: SettingsManager) {
        when (id) {
            "menuMusic" -> { s.menuMusicEnabled = !s.menuMusicEnabled; if (s.menuMusicEnabled) game.soundManager.playMenuMusic() else game.soundManager.stopMenuMusic() }
            "hitboxes" -> s.showHitboxes = !s.showHitboxes
            "hitboxesDeath" -> s.showHitboxesOnDeath = !s.showHitboxesOnDeath
            "showFps" -> s.showFps = !s.showFps
            "capFps" -> { s.capFps = !s.capFps; s.applyFpsCap() }
            "vsync" -> { s.enableVsync = !s.enableVsync; s.applyVsync() }
            "showPercentage" -> s.showPercentage = !s.showPercentage
            "showProgressBar" -> s.showProgressBar = !s.showProgressBar
            "showAttempts" -> s.showAttempts = !s.showAttempts
            "showBest" -> s.showBest = !s.showBest
        }
        s.save()
    }

    private fun navigate(dir: Int, s: SettingsManager) {
        val total = subPageCount(currentCat)
        currentSubPage = (currentSubPage + dir + total) % total
    }

    private fun closeSettings() { settingsOpen = false; game.settingsManager.save() }
    private fun confirmFpsInput(s: SettingsManager) {
        try { val v = fpsInputBuffer.toString().toInt(); if (v > 0) { s.fpsCapValue = v; s.applyFpsCap(); s.save() } } catch (e: Exception) {}
        fpsInputActive = false
    }

    private fun rowY(i: Int): Float = rowStartY - i * rowStep - rowStep / 2f
    private fun hitPill(t: Vector2, ry: Float): Boolean = t.x > panelX + panelW - 140f && t.y in (ry - 20f)..(ry + 20f)
    private fun hitSliderThumb(t: Vector2, ry: Float, v: Float): Boolean = t.y in (ry - 25f)..(ry + 25f) && t.x > panelX + panelW * 0.5f
    private fun hitIntBox(t: Vector2, ry: Float): Boolean = t.x > panelX + panelW - 140f && t.y in (ry - 20f)..(ry + 20f)

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
        drawTextWithShadow(font, "Information", (panelX + panelW / 2f) - (layout.width / 2f), panelY + panelH - 16f, COL_HEADING)

        val tabFontScale = settingsFontScale * 0.85f
        font.data.setScale(tabFontScale)
        val tabY = panelY + panelH - panelPadT * 0.47f
        for (i in 0 until INFO_TAB_COUNT) {
            val tw = panelW / INFO_TAB_COUNT
            val tabCX = panelX + tw * i + tw / 2f
            val tabColor = if (i == currentInfoTab) COL_TAB_ACT else COL_TAB_INACT
            layout.setText(font, INFO_TAB_NAMES[i])
            drawTextWithShadow(font, INFO_TAB_NAMES[i], tabCX - layout.width / 2f, tabY, tabColor)
        }

        val tw = panelW / INFO_TAB_COUNT
        val tabCX = panelX + tw * currentInfoTab
        layout.setText(font, INFO_TAB_NAMES[currentInfoTab])
        val tabTextH = layout.height
        val underlineY = tabY - tabTextH - 3f

        game.batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_TAB_ACT
        shapes.rect(tabCX + 8f, underlineY, tw - 16f, 3f)
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
            for (i in creditLines.indices) {
                val line = creditLines[i]
                line.y = contentY - (i + 1.25f) * lineSpacing
                font.color = COL_TAB_ACT
                font.draw(game.batch, "- " + line.text, contentX, line.y)
            }
        } else if (currentInfoTab == INFO_TAB_SOCIALS) {
            font.color = COL_HEADING
            font.draw(game.batch, "Follow Us (Click to open):", contentX, contentY)
            for (i in socialLines.indices) {
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
            val tw = panelW / INFO_TAB_COUNT
            val tabX = panelX + tw * i
            val tabTopY = panelY + panelH - 42f
            if (t.x >= tabX && t.x <= tabX + tw && t.y >= tabTopY - 28f && t.y <= tabTopY + 8f) {
                if (i != currentInfoTab) {
                    currentInfoTab = i
                }
                return
            }
        }

        // Handle link clicks
        val lines: Array<InfoLine>? = when (currentInfoTab) {
            INFO_TAB_CREDITS -> Array(creditLines)
            INFO_TAB_SOCIALS -> Array(socialLines)
            else -> null
        }

        if (lines != null) {
            val lineH = 32f * (panelH / 480f)
            for (i in 0 until lines.size) {
                val line = lines.get(i)
                if (t.x >= panelX + 35f && t.x <= panelX + panelW - 35f &&
                    t.y >= line.y - lineH && t.y <= line.y
                ) {
                    Gdx.net.openURI(line.url)
                    return
                }
            }
        }

        if (currentInfoTab == INFO_TAB_SOCIALS) {
            val lineH = 32f * (panelH / 480f)
            if (t.x >= panelX + panelW / 2f - 100f && t.x <= panelX + panelW / 2f + 100f &&
                t.y >= privacyPolicyLine.y - lineH && t.y <= privacyPolicyLine.y
            ) {
                Gdx.net.openURI(privacyPolicyLine.url)
            }
        }
    }

    private fun closeInfo() {
        infoOpen = false
        currentInfoTab = INFO_TAB_HOWTOPLAY
    }

    private fun drawRoundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        shapes.rect(x + r, y, w - 2 * r, h)
        shapes.rect(x, y + r, r, h - 2 * r)
        shapes.rect(x + w - r, y + r, r, h - 2 * r)
        shapes.circle(x + r, y + r, r, 20)
        shapes.circle(x + w - r, y + r, r, 20)
        shapes.circle(x + r, y + h - r, r, 20)
        shapes.circle(x + w - r, y + h - r, r, 20)
    }

    private fun unproject(): Vector2 {
        val touch = com.badlogic.gdx.math.Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(touch)
        return Vector2(touch.x, touch.y)
    }

    override fun resize(width: Int, height: Int) { super.resize(width, height); updateScaledSizes() }
    override fun dispose() { shapes.dispose(); panelTexture?.dispose(); super.dispose() }

    private fun drawShadowText(f: BitmapFont, text: String, x: Float, y: Float, color: Color) {
        val oldColor = f.color.cpy()
        f.setColor(0f, 0f, 0f, color.a * 0.5f)
        f.draw(game.batch, text, x + 2f, y - 2f)
        f.color = color
        f.draw(game.batch, text, x, y)
        f.color = oldColor
    }

    private fun drawTextWithShadow(f: BitmapFont, text: String, x: Float, y: Float, color: Color) {
        f.setColor(0f, 0f, 0f, color.a * 0.5f)
        f.draw(game.batch, text, x + 2f, y - 2f)
        f.color = color
        f.draw(game.batch, text, x, y)
    }

    private fun createRoundedRect(w: Int, h: Int, r: Int, color: Color): Texture {
        val pm = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pm.setColor(color)
        pm.fillRectangle(r, 0, w - 2 * r, h)
        pm.fillRectangle(0, r, w, h - 2 * r)
        pm.fillCircle(r, r, r); pm.fillCircle(w - r, r, r); pm.fillCircle(r, h - r, r); pm.fillCircle(w - r, h - r, r)
        val t = Texture(pm); pm.dispose(); return t
    }
}
