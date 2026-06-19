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
import io.github.msameer0.rhythmicrush.ui.NeonUI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * The main menu screen providing navigation to level selection, settings, and information.
 */
class MainMenuScreen @JvmOverloads constructor(
    game: RhythmicRushGame,
    private val startInSettings: Boolean = false
) : AbstractScreen(game) {

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
    private lateinit var btnOverlayBack: AnimatedButton
    private lateinit var btnOverlayLeft: AnimatedButton
    private lateinit var btnOverlayRight: AnimatedButton

    private var settingsOpen = false
    private var infoOpen = false
    private lateinit var shapes: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var headingFont: BitmapFont
    private val layout = GlyphLayout()

    companion object {
        private const val CAT_AUDIO = 0
        private const val CAT_GAMEPLAY = 1
        private const val CAT_INTERFACE = 2
        private const val CAT_GRAPHICS = 3
        private const val CAT_COUNT = 4
        private val CAT_NAMES = arrayOf("Audio", "Gameplay", "Controls", "Video")
        private val TAB_CATEGORIES = intArrayOf(CAT_GAMEPLAY, CAT_INTERFACE, CAT_AUDIO, CAT_GRAPHICS)
        private val TAB_NAMES = arrayOf("Gameplay", "Controls", "Audio", "Video")

        private const val INFO_TAB_HOWTOPLAY = 0
        private const val INFO_TAB_CREDITS_A = 1
        private const val INFO_TAB_CREDITS_B = 2
        private const val INFO_TAB_SOCIALS = 3
        private const val INFO_TAB_COUNT = 4
        private val INFO_TAB_NAMES = arrayOf("How to Play", "Credits I", "Credits II", "Socials")

        // Four rows leaves a dedicated footer band for navigation dots at 720p and 1080p.
        private const val MAX_ROWS_PER_PAGE = 4
        private const val PANEL_HEIGHT_FRACTION = 0.88f

        private val COL_OVERLAY = NeonUI.OVERLAY
        private val COL_PANEL = NeonUI.PANEL
        private val COL_PANEL_SHADOW = Color(0f, 0f, 0f, 0.22f)
        private val COL_LABEL = NeonUI.TEXT
        private val COL_DIM = NeonUI.TEXT_SECONDARY
        private val COL_ON = NeonUI.LIME
        private val COL_OFF = Color(0.50f, 0.50f, 0.55f, 1f)
        private val COL_TRACK = Color(0.28f, 0.28f, 0.35f, 1f)
        private val COL_FILL = NeonUI.BLUE
        private val COL_THUMB = Color(1f, 1f, 1f, 1f)
        private val COL_HEADING = NeonUI.YELLOW
        private val COL_TAB_ACT = NeonUI.LIME
        private val COL_TAB_INACT = Color(0.35f, 0.35f, 0.45f, 1f)
        private val COL_INPUT_BG = Color(0.18f, 0.18f, 0.26f, 1f)
        private val COL_INPUT_BD = NeonUI.BLUE
        private val COL_DOT_ACT = NeonUI.LIME
        private val COL_DOT_INACT = Color(0.35f, 0.35f, 0.45f, 1f)

        private fun hits(t: Vector2, x: Float, y: Float, w: Float, h: Float): Boolean {
            return t.x >= x && t.x <= x + w && t.y >= y && t.y <= y + h
        }
    }

    private var currentSettingsPage = 0
    private var currentInfoPage = 0

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
    private var panelPadX = 0f
    private var panelPadY = 0f
    private var headerY = 0f
    private var contentTopY = 0f
    private var footerY = 0f
    private var rowLabelX = 0f
    private var controlRightX = 0f
    private var sliderTrackW = 0f
    private var footerDotY = 0f
    private var settingsFontScale = 0f
    private var settingsHeadingScale = 0f

    private class InfoLine(val text: String, val url: String, var y: Float = 0f)

    private val creditLines = arrayOf(
        InfoLine("Euphoria by ForeverBound", "https://www.newgrounds.com/audio/listen/680209"),
        InfoLine("Icefield by Waterflame", "https://www.newgrounds.com/audio/listen/1379251"),
        InfoLine("Hypercharge by Cobalt039", "https://www.newgrounds.com/audio/listen/1538780"),
        InfoLine("Bounce by Waterflame", "https://www.newgrounds.com/audio/listen/654553"),
        InfoLine("Rhythm Factory by Waterflame", "https://www.newgrounds.com/audio/listen/1533782"),
        InfoLine("Vulg by OcularNebula", "https://www.newgrounds.com/audio/listen/954091"),
        InfoLine("Event Horizon by DJVI", "https://www.newgrounds.com/audio/listen/809594"),
        InfoLine("Geometry Bounce by DJ Nate", "https://www.newgrounds.com/audio/listen/770546")
    )

    private val howToEntries = arrayOf(
        Pair("Space / Click", "Jump as cube, rise as ship"),
        Pair("Esc", "Pause gameplay or close menu overlays"),
        Pair("R", "Restart the level"),
        Pair("Practice: Z / X", "Place or remove checkpoint")
    )

    private val socialLines = arrayOf(
        InfoLine("YouTube: @LunarPixelGames", "https://www.youtube.com/@LunarPixelGames"),
        InfoLine("GitHub: LunarPixelGames", "https://github.com/LunarPixelGames")
    )

    private val privacyPolicyLine =
        InfoLine("Privacy Policy", "https://lunarpixelgames.github.io/RhythmicRush/PRIVACY")

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
        font = game.fontManager.getBody(FontManager.SIZE_LARGE)
        headingFont = game.fontManager.getTitle(FontManager.SIZE_XLARGE)

        btnPlay =
            AnimatedButton(startButton, 0f, 0f, 0f, 0f) { game.screen = LevelSelectScreen(game) }
        btnSettings = AnimatedButton(settingsButton, 0f, 0f, 0f, 0f) { settingsOpen = true }
        btnInfo = AnimatedButton(infoButton, 0f, 0f, 0f, 0f) { infoOpen = true }
        btnOverlayBack = AnimatedButton(backArrow, 0f, 0f, 0f, 0f, null)
        btnOverlayLeft = AnimatedButton(null, 0f, 0f, 0f, 0f, null)
        btnOverlayRight = AnimatedButton(null, 0f, 0f, 0f, 0f, null)
        settingsOpen = startInSettings
        if (startInSettings) currentSettingsPage = 0

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

        if (cat == CAT_AUDIO) {
            rows.add(SettingRow(RowType.TOGGLE, "Menu Music", "menuMusic"))
            rows.add(SettingRow(RowType.SLIDER, "Music Volume", "volume"))
            rows.add(SettingRow(RowType.SLIDER, "SFX Volume", "sfxVolume"))
        } else if (cat == CAT_GAMEPLAY) {
            rows.add(SettingRow(RowType.TOGGLE, "Death Effect", "deathEffect"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Hitboxes", "hitboxes"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Hitboxes on Death", "hitboxesDeath"))
            rows.add(SettingRow(RowType.TOGGLE, "Pulse Orbs", "pulseOrbs"))
            if (desktop) rows.add(SettingRow(RowType.TOGGLE, "Lock Cursor in Game", "lockCursor"))
        } else if (cat == CAT_INTERFACE) {
            rows.add(SettingRow(RowType.TOGGLE, "Show Percentage", "showPercentage"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Progress Bar", "showProgressBar"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Attempts", "showAttempts"))
            rows.add(SettingRow(RowType.TOGGLE, "Show Best", "showBest"))
            rows.add(SettingRow(RowType.SLIDER, "Practice Buttons Opacity", "practiceOpacity"))
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

    private fun getPageRows(page: Int): Array<SettingRow> {
        val (cat, sub) = settingsPageToCatSub(page)
        val all = buildAllRows(cat)
        val start = sub * MAX_ROWS_PER_PAGE
        val end = min(start + MAX_ROWS_PER_PAGE, all.size)
        if (start >= all.size) return Array()
        val pageRows = Array<SettingRow>()
        for (i in start until end) pageRows.add(all.get(i))
        return pageRows
    }

    private fun totalSettingsPages(): Int {
        var total = 0
        for (i in 0 until CAT_COUNT) total += subPageCount(i)
        return total
    }

    private fun settingsPageToCatSub(page: Int): Pair<Int, Int> {
        var remaining = page
        for (i in 0 until CAT_COUNT) {
            val count = subPageCount(i)
            if (remaining < count) return Pair(i, remaining)
            remaining -= count
        }
        return Pair(CAT_COUNT - 1, 0)
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

        if (::btnInfo.isInitialized) btnInfo.setBounds(
            vw - settingsW - 20f,
            10f,
            settingsW,
            settingsH
        )

        panelW = min(vw * 0.80f, 1460f)
        val targetH = min(vh * 0.86f, 920f)
        panelPadX = panelW * 0.07f
        panelPadY = targetH * 0.065f
        // Header includes both the title and tab strip; footer is reserved for page dots/hints.
        panelPadT = targetH * 0.23f
        panelPadB = targetH * 0.13f
        rowStep = (targetH - panelPadT - panelPadB) / MAX_ROWS_PER_PAGE
        sliderTrackW = panelW * 0.28f
        val scaleRef = targetH / 760f
        settingsFontScale = 0.78f * scaleRef
        settingsHeadingScale = 1.08f * scaleRef
        recomputePanelHeight()
    }

    private fun recomputePanelHeight() {
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight
        panelH = MAX_ROWS_PER_PAGE * rowStep + panelPadT + panelPadB
        panelX = vw / 2f - panelW / 2f
        panelY = vh / 2f - panelH / 2f

        backW = min(vw * 0.065f, vh * 0.105f)
        backH = backW
        backX = vw * 0.028f
        backY = vh - backH - vh * 0.04f

        headerY = panelY + panelH - panelPadY - 18f
        contentTopY = panelY + panelH - panelPadT
        footerY = panelY + panelPadB * 0.78f
        footerDotY = panelY + panelPadB * 0.28f
        arrowSize = 52f
        rowStartY = contentTopY - rowStep * 0.55f
        rowLabelX = panelX + panelPadX + arrowSize + 28f
        controlRightX = panelX + panelW - panelPadX - arrowSize - 28f

        arrowY = panelY + panelH / 2f - arrowSize / 2f
        arrowLeftX = panelX + 18f
        arrowRightX = panelX + panelW - arrowSize - 18f
        if (::btnOverlayBack.isInitialized) btnOverlayBack.setBounds(backX, backY, backW, backH)
        if (::btnOverlayLeft.isInitialized) btnOverlayLeft.setBounds(arrowLeftX - 10f, arrowY - 10f, arrowSize + 20f, arrowSize + 20f)
        if (::btnOverlayRight.isInitialized) btnOverlayRight.setBounds(arrowRightX - 10f, arrowY - 10f, arrowSize + 20f, arrowSize + 20f)
        lastPanelW = -1
    }

    override fun update(delta: Float) {
        if (!settingsOpen && !infoOpen) {
            btnPlay.update(delta)
            btnSettings.update(delta)
            btnInfo.update(delta)
        }
        if (settingsOpen || infoOpen) {
            btnOverlayBack.update(delta)
            btnOverlayLeft.update(delta)
            btnOverlayRight.update(delta)
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
            panelTexture = createRoundedRect(texW, texH, (24f * (panelW / 960f)).toInt(), COL_PANEL)
            lastPanelW = texW
            lastPanelH = texH
        }
        game.batch.begin()
        panelTexture?.let {
            game.batch.color = COL_PANEL_SHADOW
            game.batch.draw(it, panelX + 10f, panelY - 12f)
            game.batch.color = Color.WHITE
            game.batch.draw(it, panelX, panelY)
        }
        game.batch.draw(backArrow, backX, backY, backW, backH)
        game.batch.end()
        drawSettingsTabs()
        drawSettingsHeading()
        drawSettingsRows(getPageRows(currentSettingsPage))
        drawSettingsDots()
        if (settingsPageToCatSub(currentSettingsPage).first == CAT_AUDIO) {
            game.batch.begin()
            font.data.setScale(settingsFontScale * 0.56f)
            val hint = "TIP  Adjust the audio settings to your preference."
            layout.setText(font, hint)
            drawTextWithShadow(font, hint, panelX + panelW / 2f - layout.width / 2f, footerY, NeonUI.TEXT_MUTED)
            game.batch.end()
        }
    }

    private fun drawSettingsHeading() {
        game.batch.begin()
        headingFont.data.setScale(settingsHeadingScale)
        val titleText = "Settings"
        layout.setText(headingFont, titleText)
        drawTextWithShadow(
            headingFont,
            titleText,
            (panelX + panelW / 2f) - (layout.width / 2f),
            headerY,
            COL_HEADING
        )
        drawOverlayBackButton()
        game.batch.end()
        drawArrow(arrowLeftX, arrowY, arrowSize, true)
        drawArrow(arrowRightX, arrowY, arrowSize, false)
    }

    private fun drawSettingsTabs() {
        val activeCat = settingsPageToCatSub(currentSettingsPage).first
        val tabX = panelX + panelW * 0.16f
        val tabW = panelW * 0.17f
        val tabGap = panelW * 0.012f
        val tabH = rowStep * 0.48f
        val tabY = settingsTabY()
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in TAB_CATEGORIES.indices) {
            val x = tabX + i * (tabW + tabGap)
            if (TAB_CATEGORIES[i] == activeCat) {
                shapes.color = COL_TAB_ACT
                shapes.rect(x + tabW * 0.12f, tabY, tabW * 0.76f, 3f)
            }
        }
        shapes.end()
        game.batch.begin()
        font.data.setScale(settingsFontScale * 1.00f)
        for (i in TAB_CATEGORIES.indices) {
            val x = tabX + i * (tabW + tabGap)
            layout.setText(font, TAB_NAMES[i].uppercase())
            drawTextWithShadow(
                font, TAB_NAMES[i].uppercase(),
                x + tabW / 2f - layout.width / 2f,
                tabY + tabH / 2f + layout.height / 2f,
                if (TAB_CATEGORIES[i] == activeCat) NeonUI.LIME else NeonUI.TEXT_SECONDARY
            )
        }
        game.batch.end()
    }

    private fun settingsTabY(): Float = contentTopY - rowStep * 0.10f

    private fun drawSettingsDots() {
        val total = totalSettingsPages()
        if (total <= 1) return
        val dotR = 8f
        val dotGap = 28f
        val startX = panelX + panelW / 2f - (total * dotGap - (dotGap - dotR * 2f)) / 2f + dotR
        val dotY = footerDotY
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until total) {
            shapes.color = if (i == currentSettingsPage) COL_DOT_ACT else COL_DOT_INACT
            shapes.circle(startX + i * dotGap, dotY, dotR, 16)
        }
        shapes.end()
    }

    private fun drawArrow(x: Float, y: Float, size: Float, pointLeft: Boolean) {
        val button = if (pointLeft) btnOverlayLeft else btnOverlayRight
        val scale = button.scale
        val drawSize = size * scale
        val drawX = x + size / 2f - drawSize / 2f
        val drawY = y + size / 2f - drawSize / 2f
        val cx = drawX + drawSize / 2f
        val cy = drawY + drawSize / 2f
        val hs = drawSize * 0.35f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_DIM
        if (pointLeft) shapes.triangle(cx + hs, cy + hs, cx + hs, cy - hs, cx - hs, cy)
        else shapes.triangle(cx - hs, cy + hs, cx - hs, cy - hs, cx + hs, cy)
        shapes.end()
    }

    private fun drawSettingsRows(rows: Array<SettingRow>) {
        val s = game.settingsManager
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until rows.size) {
            val ry = rowY(i)
            shapes.color = Color(COL_DIM.r, COL_DIM.g, COL_DIM.b, 0.12f)
            shapes.rect(rowLabelX, ry - rowStep * 0.40f, controlRightX - rowLabelX, 1.5f)
        }
        shapes.end()
        for (i in 0 until rows.size) {
            val row = rows.get(i)
            val ry = rowY(i)
            when (row.type) {
                RowType.TOGGLE -> drawToggleRow(ry, row.label, getToggleValue(row.id, s), 45f)
                RowType.SLIDER -> {
                    val v = when (row.id) {
                        "uiPadding" -> s.uiPadding / 50f
                        "practiceOpacity" -> s.practiceButtonOpacity
                        "sfxVolume" -> s.sfxVolume
                        else -> s.musicVolume
                    }
                    drawSliderRow(ry, row.label, v, 45f)
                }

                RowType.INT_FIELD -> drawIntFieldRow(ry, row.label, s.fpsCapValue, 45f)
            }
            game.batch.begin()
            font.data.setScale(settingsFontScale * 0.90f)
            drawTextWithShadow(font, settingDescription(row.id), rowLabelX, ry - rowStep * 0.18f, NeonUI.TEXT_SECONDARY)
            game.batch.end()
        }
    }

    private fun settingDescription(id: String): String = when (id) {
        "menuMusic" -> "Enable or disable menu background music."
        "volume" -> "Adjust the volume of background music."
        "sfxVolume" -> "Adjust the volume of sound effects."
        "deathEffect" -> "Show the impact animation after a failed attempt."
        "hitboxes" -> "Display collision shapes while playing."
        "hitboxesDeath" -> "Keep collision shapes visible after a crash."
        "lockCursor" -> "Keep the pointer captured during gameplay."
        "pulseOrbs" -> "Animate interactive orbs with the beat."
        "showPercentage" -> "Display live completion percentage."
        "showProgressBar" -> "Display the level progress track."
        "showAttempts" -> "Show the current session attempt."
        "showBest" -> "Show your saved personal best."
        "practiceOpacity" -> "Adjust practice checkpoint control visibility."
        "showFps" -> "Display the current frame rate."
        "capFps" -> "Limit the maximum frame rate."
        "fpsValue" -> "Choose the frame-rate limit."
        "vsync" -> "Synchronize frames with the display."
        "uiPadding" -> "Adjust HUD spacing from the screen edges."
        else -> ""
    }

    private fun getToggleValue(id: String, s: SettingsManager): Boolean {
        return when (id) {
            "menuMusic" -> s.menuMusicEnabled
            "deathEffect" -> s.deathEffectEnabled
            "hitboxes" -> s.showHitboxes
            "hitboxesDeath" -> s.showHitboxesOnDeath
            "lockCursor" -> s.lockCursorInGame
            "pulseOrbs" -> s.pulseOrbs
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
        val pillX = controlRightX - pillW
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
        drawTextWithShadow(font, label, rowLabelX, ry + layout.height / 2f, COL_LABEL)
        game.batch.end()
    }

    private fun drawSliderRow(ry: Float, label: String, value: Float, hp: Float) {
        val trackW = sliderTrackW
        val trackH = rowStep * 0.06f
        val trackX = controlRightX - trackW
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
        drawTextWithShadow(font, label, rowLabelX, ry + layout.height / 2f, COL_LABEL)
        val pct = "${round(value * 100f).toInt()}%"
        font.data.setScale(settingsFontScale * 0.77f)
        layout.setText(font, pct)
        drawTextWithShadow(
            font,
            pct,
            controlRightX - layout.width,
            ry + layout.height / 2f + rowStep * 0.28f,
            COL_DIM
        )
        game.batch.end()
    }

    private fun drawIntFieldRow(ry: Float, label: String, value: Int, hp: Float) {
        val boxH = rowStep * 0.40f
        val boxW = boxH * 3.0f
        val boxX = controlRightX - boxW
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_INPUT_BG
        shapes.rect(boxX, ry - boxH / 2f, boxW, boxH)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = if (fpsInputActive) COL_INPUT_BD else Color(
            COL_INPUT_BD.r,
            COL_INPUT_BD.g,
            COL_INPUT_BD.b,
            0.4f
        )
        shapes.rect(boxX, ry - boxH / 2f, boxW, boxH)
        shapes.end()
        val display =
            if (fpsInputActive) fpsInputBuffer.toString() + (if (System.currentTimeMillis() / 500 % 2 == 0L) "|" else " ") else value.toString()
        game.batch.begin()
        font.data.setScale(settingsFontScale)
        drawTextWithShadow(font, label, rowLabelX, ry + layout.height / 2f, COL_LABEL)
        font.data.setScale(settingsFontScale * 0.95f)
        layout.setText(font, display)
        drawTextWithShadow(
            font,
            display,
            boxX + boxW / 2f - layout.width / 2f,
            ry + layout.height / 2f,
            if (fpsInputActive) Color.WHITE else COL_DIM
        )
        game.batch.end()
    }

    private fun handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) game.screen = LevelSelectScreen(game)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit()
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && System.getProperty("devMode") != null) {
            game.screen = LevelEditorScreen(game)
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
        val t = unproject()
        if (Gdx.input.justTouched()) {
            btnOverlayBack.onTouchDown(t.x, t.y)
            btnOverlayLeft.onTouchDown(t.x, t.y)
            btnOverlayRight.onTouchDown(t.x, t.y)
        }
        if (!Gdx.input.isTouched) {
            val backPressed = btnOverlayBack.isPressed
            val leftPressed = btnOverlayLeft.isPressed
            val rightPressed = btnOverlayRight.isPressed
            btnOverlayBack.onTouchUp(t.x, t.y)
            btnOverlayLeft.onTouchUp(t.x, t.y)
            btnOverlayRight.onTouchUp(t.x, t.y)
            if (backPressed && btnOverlayBack.hits(t.x, t.y)) {
                closeSettings(); return
            }
            if (leftPressed && btnOverlayLeft.hits(t.x, t.y)) {
                navigateSettings(-1); return
            }
            if (rightPressed && btnOverlayRight.hits(t.x, t.y)) {
                navigateSettings(1); return
            }
        }
        if (fpsInputActive) {
            for (k in Input.Keys.NUM_0..Input.Keys.NUM_9) if (Gdx.input.isKeyJustPressed(k)) fpsInputBuffer.append(
                (k - Input.Keys.NUM_0).toString()
            )
            for (k in Input.Keys.NUMPAD_0..Input.Keys.NUMPAD_9) if (Gdx.input.isKeyJustPressed(k)) fpsInputBuffer.append(
                (k - Input.Keys.NUM_0).toString()
            )
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && fpsInputBuffer.isNotEmpty()) fpsInputBuffer.deleteCharAt(
                fpsInputBuffer.length - 1
            )
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) confirmFpsInput(s)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (fpsInputActive) confirmFpsInput(s) else closeSettings()
        }
        if (Gdx.input.isTouched && draggingSlider) {
            val sliderX = settingsSliderX()
            val norm = MathUtils.clamp(
                (unproject().x - sliderX) / sliderTrackW,
                0f,
                1f
            )
            val rows = getPageRows(currentSettingsPage)
            if (draggingSliderRow in 0 until rows.size) {
                val r = rows.get(draggingSliderRow)
                if (r.id == "volume") {
                    s.musicVolume = norm; game.soundManager.setMusicVolume(norm)
                } else if (r.id == "sfxVolume") {
                    s.sfxVolume = norm; game.soundManager.setSfxVolume(norm)
                } else if (r.id == "uiPadding") s.uiPadding = norm * 50f
                else if (r.id == "practiceOpacity") s.practiceButtonOpacity = norm
            }
        }
        if (!Gdx.input.isTouched) {
            if (draggingSlider) s.save(); draggingSlider = false; draggingSliderRow = -1
        }
        if (!Gdx.input.justTouched()) return

        val tabX = panelX + panelW * 0.16f
        val tabW = panelW * 0.17f
        val tabGap = panelW * 0.012f
        val tabH = rowStep * 0.48f
        val tabY = settingsTabY()
        for (i in TAB_CATEGORIES.indices) {
            if (hits(t, tabX + i * (tabW + tabGap), tabY, tabW, tabH)) {
                currentSettingsPage = firstPageForCategory(TAB_CATEGORIES[i])
                return
            }
        }

        val pRows = getPageRows(currentSettingsPage)
        for (i in 0 until pRows.size) {
            val ry = rowY(i)
            val r = pRows.get(i)
            if (r.type == RowType.TOGGLE && hitPill(t, ry)) handleToggle(r.id, s)
            else if (r.type == RowType.SLIDER && hitSliderThumb(t, ry, 0.5f)) {
                draggingSlider = true; draggingSliderRow = i
            } else if (r.type == RowType.INT_FIELD && hitIntBox(t, ry)) {
                fpsInputActive =
                    true; fpsInputBuffer.setLength(0); fpsInputBuffer.append(s.fpsCapValue)
            }
        }
    }

    private fun handleToggle(id: String, s: SettingsManager) {
        when (id) {
            "menuMusic" -> {
                s.menuMusicEnabled =
                    !s.menuMusicEnabled; if (s.menuMusicEnabled) game.soundManager.playMenuMusic() else game.soundManager.stopMenuMusic()
            }

            "hitboxes" -> s.showHitboxes = !s.showHitboxes
            "deathEffect" -> s.deathEffectEnabled = !s.deathEffectEnabled
            "hitboxesDeath" -> s.showHitboxesOnDeath = !s.showHitboxesOnDeath
            "pulseOrbs" -> s.pulseOrbs = !s.pulseOrbs
            "showFps" -> s.showFps = !s.showFps
            "capFps" -> {
                s.capFps = !s.capFps; s.applyFpsCap()
            }

            "vsync" -> {
                s.enableVsync = !s.enableVsync; s.applyVsync()
            }

            "showPercentage" -> s.showPercentage = !s.showPercentage
            "showProgressBar" -> s.showProgressBar = !s.showProgressBar
            "showAttempts" -> s.showAttempts = !s.showAttempts
            "showBest" -> s.showBest = !s.showBest
        }
        s.save()
    }

    private fun navigateSettings(dir: Int) {
        val total = totalSettingsPages()
        currentSettingsPage = (currentSettingsPage + dir + total) % total
    }

    private fun firstPageForCategory(category: Int): Int {
        var page = 0
        for (i in 0 until category) page += subPageCount(i)
        return page
    }

    private fun closeSettings() {
        settingsOpen = false
        btnOverlayBack.cancel()
        btnOverlayLeft.cancel()
        btnOverlayRight.cancel()
        game.settingsManager.save()
    }

    private fun confirmFpsInput(s: SettingsManager) {
        try {
            val v = fpsInputBuffer.toString().toInt(); if (v > 0) {
                s.fpsCapValue = v; s.applyFpsCap(); s.save()
            }
        } catch (e: Exception) {
        }
        fpsInputActive = false
    }

    private fun rowY(i: Int): Float = rowStartY - i * rowStep - rowStep / 2f
    private fun hitPill(t: Vector2, ry: Float): Boolean =
        t.x >= controlRightX - rowStep * 0.9f && t.x <= controlRightX + 8f && t.y in (ry - rowStep * 0.3f)..(ry + rowStep * 0.3f)

    private fun hitSliderThumb(t: Vector2, ry: Float, v: Float): Boolean =
        t.y in (ry - rowStep * 0.3f)..(ry + rowStep * 0.3f) &&
            t.x in (settingsSliderX() - 8f)..(settingsSliderX() + sliderTrackW + 8f)

    private fun settingsSliderX(): Float =
        controlRightX - sliderTrackW

    private fun hitIntBox(t: Vector2, ry: Float): Boolean =
        t.x >= controlRightX - rowStep * 1.3f && t.x <= controlRightX + 8f && t.y in (ry - rowStep * 0.3f)..(ry + rowStep * 0.3f)

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
            panelTexture = createRoundedRect(texW, texH, (24f * (panelW / 960f)).toInt(), COL_PANEL)
            lastPanelW = texW
            lastPanelH = texH
        }

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        panelTexture?.let {
            game.batch.color = COL_PANEL_SHADOW
            game.batch.draw(it, panelX + 10f, panelY - 12f)
            game.batch.color = Color.WHITE
            game.batch.draw(it, panelX, panelY)
        }

        val titleText = "Info Menu"
        headingFont.data.setScale(settingsHeadingScale)
        layout.setText(headingFont, titleText)
        drawTextWithShadow(
            headingFont,
            titleText,
            (panelX + panelW / 2f) - (layout.width / 2f),
            headerY,
            COL_HEADING
        )
        drawOverlayBackButton()
        game.batch.end()

        drawArrow(arrowLeftX, arrowY, arrowSize, true)
        drawArrow(arrowRightX, arrowY, arrowSize, false)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(COL_DIM.r, COL_DIM.g, COL_DIM.b, 0.16f)
        val dividerStartX = panelX + panelPadX
        shapes.rect(dividerStartX, contentTopY + 18f, panelX + panelW - panelPadX - dividerStartX, 2f)
        shapes.end()
        game.batch.begin()
        val contentX = rowLabelX
        val contentY = contentTopY - 18f
        val infoStep = infoRowStep()
        val lineSpacing = infoStep * 0.64f
        val pageContentY = contentY - lineSpacing * 0.78f
        font.data.setScale(settingsFontScale * 1.16f)
        font.color = COL_HEADING
        font.draw(game.batch, INFO_TAB_NAMES[currentInfoPage], contentX, contentY)

        if (currentInfoPage == INFO_TAB_HOWTOPLAY) {
            font.color = COL_LABEL
            font.draw(game.batch, "Click to jump over spikes. It's that simple.", contentX, pageContentY)
            val blockGap = infoStep * 0.86f
            for (i in howToEntries.indices) {
                val entry = howToEntries[i]
                val y = pageContentY - (i + 1) * blockGap
                font.data.setScale(settingsFontScale * 1.00f)
                font.color = NeonUI.LIME
                font.draw(game.batch, entry.first, contentX, y)
                font.data.setScale(settingsFontScale * 1.02f)
                font.color = COL_LABEL
                font.draw(game.batch, entry.second, contentX + infoStep * 1.72f, y)
            }
        } else if (currentInfoPage == INFO_TAB_CREDITS_A) {
            font.color = COL_HEADING
            font.draw(game.batch, "Music Credits", contentX, pageContentY)
            for (i in 0 until 4) {
                val line = creditLines[i]
                line.y = pageContentY - (i + 1.15f) * lineSpacing
                font.color = COL_TAB_ACT
                font.draw(game.batch, "- " + line.text, contentX, line.y)
            }
            font.color = COL_DIM
            font.draw(game.batch, "More tracks on the next page.", contentX, footerY + 18f)
        } else if (currentInfoPage == INFO_TAB_CREDITS_B) {
            font.color = COL_HEADING
            font.draw(game.batch, "Music Credits", contentX, pageContentY)
            for (i in 4 until creditLines.size) {
                val line = creditLines[i]
                val lineIndex = i - 4
                line.y = pageContentY - (lineIndex + 1.15f) * lineSpacing
                font.color = COL_TAB_ACT
                font.draw(game.batch, "- " + line.text, contentX, line.y)
            }
        } else if (currentInfoPage == INFO_TAB_SOCIALS) {
            font.color = COL_HEADING
            font.draw(game.batch, "Follow Us", contentX, pageContentY)
            for (i in socialLines.indices) {
                val line = socialLines[i]
                line.y = pageContentY - (i + 1.15f) * lineSpacing
                font.color = COL_TAB_ACT
                font.draw(game.batch, line.text, contentX, line.y)
            }
            font.color = COL_DIM
            font.draw(
                game.batch,
                "Thanks for playing Rhythmic Rush!",
                contentX,
                pageContentY - 3.9f * lineSpacing
            )

            font.color = COL_TAB_ACT
            layout.setText(font, privacyPolicyLine.text)
            privacyPolicyLine.y = footerY + 10f
            font.draw(
                game.batch,
                privacyPolicyLine.text,
                panelX + panelW / 2f - layout.width / 2f,
                privacyPolicyLine.y
            )
        }

        font.data.setScale(1f)
        game.batch.end()
        drawInfoDots()
    }

    private fun infoRowStep(): Float = rowStep * MAX_ROWS_PER_PAGE / 5f

    private fun drawInfoDots() {
        val total = INFO_TAB_COUNT
        val dotR = 8f
        val dotGap = 28f
        val startX = panelX + panelW / 2f - (total * dotGap - (dotGap - dotR * 2f)) / 2f + dotR
        val dotY = footerDotY
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until total) {
            shapes.color = if (i == currentInfoPage) COL_DOT_ACT else COL_DOT_INACT
            shapes.circle(startX + i * dotGap, dotY, dotR, 16)
        }
        shapes.end()
    }


    private fun handleInfoInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeInfo()
            return
        }

        val t = unproject()
        if (Gdx.input.justTouched()) {
            btnOverlayBack.onTouchDown(t.x, t.y)
            btnOverlayLeft.onTouchDown(t.x, t.y)
            btnOverlayRight.onTouchDown(t.x, t.y)
        }
        if (!Gdx.input.isTouched) {
            val backPressed = btnOverlayBack.isPressed
            val leftPressed = btnOverlayLeft.isPressed
            val rightPressed = btnOverlayRight.isPressed
            btnOverlayBack.onTouchUp(t.x, t.y)
            btnOverlayLeft.onTouchUp(t.x, t.y)
            btnOverlayRight.onTouchUp(t.x, t.y)
            if (backPressed && btnOverlayBack.hits(t.x, t.y)) {
                closeInfo()
                return
            }
            if (leftPressed && btnOverlayLeft.hits(t.x, t.y)) {
                navigateInfo(-1); return
            }
            if (rightPressed && btnOverlayRight.hits(t.x, t.y)) {
                navigateInfo(1); return
            }
        }

        if (!Gdx.input.justTouched()) return

        val lines: Array<InfoLine>? = when (currentInfoPage) {
            INFO_TAB_CREDITS_A -> Array.with(*creditLines.copyOfRange(0, 4))
            INFO_TAB_CREDITS_B -> Array.with(*creditLines.copyOfRange(4, creditLines.size))
            INFO_TAB_SOCIALS -> Array(socialLines)
            else -> null
        }

        if (lines != null) {
            val lineH = rowStep * 0.42f
            for (i in 0 until lines.size) {
                val line = lines.get(i)
                if (t.x >= panelX + panelPadX && t.x <= panelX + panelW - panelPadX &&
                    t.y >= line.y - lineH && t.y <= line.y
                ) {
                    Gdx.net.openURI(line.url)
                    return
                }
            }
        }

        if (currentInfoPage == INFO_TAB_SOCIALS) {
            val lineH = rowStep * 0.42f
            if (t.x >= panelX + panelW / 2f - 120f && t.x <= panelX + panelW / 2f + 120f &&
                t.y >= privacyPolicyLine.y - lineH && t.y <= privacyPolicyLine.y
            ) {
                Gdx.net.openURI(privacyPolicyLine.url)
            }
        }
    }

    private fun navigateInfo(dir: Int) {
        currentInfoPage = (currentInfoPage + dir + INFO_TAB_COUNT) % INFO_TAB_COUNT
    }

    private fun closeInfo() {
        infoOpen = false
        currentInfoPage = 0
        btnOverlayBack.cancel()
        btnOverlayLeft.cancel()
        btnOverlayRight.cancel()
    }

    private fun drawOverlayBackButton() {
        val scale = btnOverlayBack.scale
        val drawW = backW * scale
        val drawH = backH * scale
        val drawX = backX + backW / 2f - drawW / 2f
        val drawY = backY + backH / 2f - drawH / 2f
        game.batch.draw(backArrow, drawX, drawY, drawW, drawH)
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

    override fun resize(width: Int, height: Int) {
        super.resize(width, height); updateScaledSizes()
    }

    override fun dispose() {
        shapes.dispose(); panelTexture?.dispose(); super.dispose()
    }

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
        pm.fillCircle(r, r, r); pm.fillCircle(w - r, r, r); pm.fillCircle(
            r,
            h - r,
            r
        ); pm.fillCircle(w - r, h - r, r)
        val t = Texture(pm); pm.dispose(); return t
    }
}
