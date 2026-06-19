package io.github.msameer0.rhythmicrush.screens.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.game.level.LevelData
import io.github.msameer0.rhythmicrush.ui.UI

/** Shared pause and results presentation with responsive hit targets. */
class OverlayUI(
    private val game: RhythmicRushGame,
    private val levelData: LevelData?,
    private val titleFont: BitmapFont,
    private val bodyFont: BitmapFont,
    private val shapes: ShapeRenderer,
    private val batch: SpriteBatch,
    @Suppress("UNUSED_PARAMETER") resumeRegion: TextureRegion?,
    @Suppress("UNUSED_PARAMETER") backRegion: TextureRegion?
) {
    private companion object {
        val PANEL_SHADOW = Color(0f, 0f, 0f, 0.28f)
        val PANEL_FILL = Color(0.11f, 0.11f, 0.17f, 0.98f)
        val SLIDER_TRACK = Color(UI.TEXT_MUTED.r, UI.TEXT_MUTED.g, UI.TEXT_MUTED.b, 0.35f)
        val SECONDARY_BUTTON = Color(0.16f, 0.16f, 0.24f, 0.96f)
        val PRIMARY_TEXT = Color(0.04f, 0.05f, 0.09f, 1f)
        val TOGGLE_ACTIVE = Color(UI.LIME.r, UI.LIME.g, UI.LIME.b, 0.8f)
    }

    /** Identifies the audio setting controlled by a pause-menu slider. */
    enum class SliderKind { MUSIC, SFX }

    /** Identifies an action available from the pause menu. */
    enum class PauseAction { RESTART, RESUME, PRACTICE, LEVEL_SELECT }

    /** Identifies an action available after completing a level. */
    enum class CompleteAction { MENU, PRIMARY, REPLAY }

    private val layout = GlyphLayout()
    private var panelW = 1120f
    private var panelH = 760f
    private var panelX = 0f
    private var panelY = 0f
    private var pad = 56f
    private var sliderW = 390f
    private var sliderH = 8f
    private var sliderCenterY = 0f
    private var buttonY = 0f
    private var buttonH = 94f
    private var buttonGap = 18f
    private var buttonW = 0f
    var uiScale = 1f
        private set
    var activeSlider: SliderKind? = null
        private set

    private val pauseToggleW get() = 130f * uiScale
    private val pauseToggleH get() = 48f * uiScale

    fun updateScale(viewport: Viewport) {
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight
        uiScale = MathUtils.clamp(minOf(vw / 1920f, vh / 1080f), 0.72f, 1.25f)
        panelW = minOf(vw * 0.72f, 1280f)
        panelH = minOf(vh * 0.76f, 820f)
        pad = panelW * 0.055f
        sliderW = panelW * 0.35f
        sliderH = maxOf(8f, panelH * 0.014f)
        buttonH = panelH * 0.145f
        buttonGap = panelW * 0.014f
        buttonW = (panelW - pad * 2f - buttonGap * 3f) / 4f
    }

    private fun place(camera: OrthographicCamera) {
        panelX = camera.position.x - panelW / 2f
        panelY = camera.position.y - panelH / 2f
        buttonY = panelY + pad
        sliderCenterY = panelY + panelH * 0.40f
    }

    fun drawDimOverlay(camera: OrthographicCamera, viewport: Viewport) {
        shapes.color = UI.OVERLAY
        shapes.rect(camera.position.x - viewport.worldWidth / 2f, camera.position.y - viewport.worldHeight / 2f, viewport.worldWidth, viewport.worldHeight)
    }

    fun drawPausePanelShapes(camera: OrthographicCamera) {
        place(camera)
        drawPanel()
        val rowH = panelH * 0.15f
        drawAudioRow(sliderCenterY + rowH * 0.55f, game.settingsManager.musicVolume)
        drawAudioRow(sliderCenterY - rowH * 0.55f, game.settingsManager.sfxVolume)
        for (action in PauseAction.entries) drawAction(action.ordinal, action == PauseAction.RESUME)
    }

    fun drawCompletePanelShapes(camera: OrthographicCamera) {
        place(camera)
        drawPanel()
        val completeW = (panelW - pad * 2f - buttonGap * 2f) / 3f
        for (i in 0..2) {
            val x = panelX + pad + i * (completeW + buttonGap)
            drawFlatButton(x, buttonY, completeW, buttonH, i == 1)
        }
    }

    private fun drawPanel() {
        UI.filled(shapes, panelX + 10f, panelY - 12f, panelW, panelH, 30f * uiScale, PANEL_SHADOW)
        UI.filled(shapes, panelX, panelY, panelW, panelH, 30f * uiScale, PANEL_FILL)
    }

    private fun drawAudioRow(y: Float, value: Float) {
        val sx = sliderX()
        shapes.color = SLIDER_TRACK
        shapes.rect(sx, y - sliderH / 2f, sliderW, sliderH)
        shapes.color = UI.BLUE
        if (value > 0f) shapes.rect(sx, y - sliderH / 2f, sliderW * value, sliderH)
        shapes.color = UI.TEXT
        shapes.circle(sx + sliderW * value, y, 15f * uiScale, 24)
    }

    private fun drawAction(index: Int, primary: Boolean) {
        val x = panelX + pad + index * (buttonW + buttonGap)
        drawFlatButton(x, buttonY, buttonW, buttonH, primary)
    }

    private fun drawFlatButton(x: Float, y: Float, w: Float, h: Float, primary: Boolean) {
        UI.filled(
            shapes, x, y, w, h, 14f * uiScale,
            if (primary) UI.LIME else SECONDARY_BUTTON
        )
    }

    fun drawPauseOverlay(
        camera: OrthographicCamera,
        sessionAttempts: Int,
        levelKey: String?,
        practiceMode: Boolean
    ) {
        place(camera)
        val center = panelX + panelW / 2f
        text("Paused", center, panelY + panelH * 0.91f, UI.YELLOW, 1.15f, true, true)
        text(levelData?.name ?: "Level", center, panelY + panelH * 0.79f, UI.YELLOW, 0.82f, true, true)
        val progress = levelKey?.let { game.progressManager.getOrCreate(it) }
        text("Personal Best", center - panelW * 0.14f, panelY + panelH * 0.70f, UI.TEXT_SECONDARY, 0.90f, true)
        text("${progress?.bestPercent ?: 0}%", center - panelW * 0.14f, panelY + panelH * 0.64f, UI.TEXT, 0.80f, true, true)
        text("Session Attempts", center + panelW * 0.14f, panelY + panelH * 0.70f, UI.TEXT_SECONDARY, 0.90f, true)
        text("$sessionAttempts", center + panelW * 0.14f, panelY + panelH * 0.64f, UI.TEXT, 0.80f, true, true)
        val rowH = panelH * 0.15f
        drawAudioText("Music Volume", sliderCenterY + rowH * 0.55f, game.settingsManager.musicVolume)
        drawAudioText("SFX Volume", sliderCenterY - rowH * 0.55f, game.settingsManager.sfxVolume)
        for (i in 0..3) {
            val label = when (i) {
                0 -> "RESTART"
                1 -> "RESUME"
                2 -> if (practiceMode) "NORMAL MODE" else "PRACTICE MODE"
                else -> "LEVEL SELECT"
            }
            val cx = panelX + pad + i * (buttonW + buttonGap) + buttonW / 2f
            text(
                label,
                cx,
                buttonY + buttonH / 2f + 7f * uiScale,
                if (i == 1) PRIMARY_TEXT else UI.TEXT,
                0.72f,
                true,
                true
            )
        }
        text("Enter / Space: Resume     R: Restart     Esc: Level Select", center, panelY + pad * 0.38f, UI.TEXT_MUTED, 1.12f, true)
    }

    fun drawCompleteOverlay(
        camera: OrthographicCamera,
        sessionAttempts: Int,
        levelKey: String?,
        hasNextLevel: Boolean
    ) {
        place(camera)
        val center = panelX + panelW / 2f
        val progress = levelKey?.let { game.progressManager.getOrCreate(it) }
        text("LEVEL COMPLETE!", center, panelY + panelH * 0.88f, UI.YELLOW, 1.28f, true, true)
        text(levelData?.name ?: "Level", center, panelY + panelH * 0.75f, UI.TEXT, 0.96f, true, true)
        val lx = panelX + panelW * 0.31f
        val rx = panelX + panelW * 0.69f
        text("Attempts This Session", lx, panelY + panelH * 0.47f, UI.TEXT_SECONDARY, 1.18f, false)
        text("$sessionAttempts", rx, panelY + panelH * 0.47f, UI.BLUE, 1.08f, true, true)
        text("Total Attempts", lx, panelY + panelH * 0.38f, UI.TEXT_SECONDARY, 1.18f, false)
        text("${progress?.totalAttempts ?: sessionAttempts}", rx, panelY + panelH * 0.38f, UI.BLUE, 1.08f, true, true)
        val completeW = (panelW - pad * 2f - buttonGap * 2f) / 3f
        for (i in 0..2) {
            val label = when (i) {
                0 -> "MENU"
                1 -> if (hasNextLevel) "NEXT LEVEL" else "REPLAY"
                else -> "REPLAY"
            }
            val cx = panelX + pad + i * (completeW + buttonGap) + completeW / 2f
            text(label, cx, buttonY + buttonH / 2f + 8f * uiScale, if (i == 1) PRIMARY_TEXT else UI.TEXT, 0.80f, true, true)
        }
    }

    private fun drawAudioText(label: String, y: Float, value: Float) {
        text(label, panelX + pad, y + 8f * uiScale, UI.TEXT, 1.48f, false)
        text("${MathUtils.round(value * 100f)}%", panelX + panelW - pad - panelW * 0.045f, y + 8f * uiScale, UI.BLUE, 0.78f, true, true)
    }

    private fun text(
        value: String,
        x: Float,
        y: Float,
        color: Color,
        scale: Float,
        centered: Boolean,
        titleStyle: Boolean = false
    ) {
        val font = if (titleStyle) titleFont else bodyFont
        font.data.setScale(scale * uiScale)
        layout.setText(font, value)
        val drawX = if (centered) x - layout.width / 2f else x
        font.setColor(0f, 0f, 0f, color.a * 0.45f)
        font.draw(batch, value, drawX + 2f, y - 2f)
        font.color = color
        font.draw(batch, value, drawX, y)
        font.data.setScale(1f)
    }

    fun hitPauseAction(
        touchX: Float,
        touchY: Float,
        camera: OrthographicCamera
    ): PauseAction? {
        place(camera)
        for (action in PauseAction.entries) {
            val buttonX = panelX + pad + action.ordinal * (buttonW + buttonGap)
            if (hits(touchX, touchY, buttonX, buttonY, buttonW, buttonH)) {
                return action
            }
        }
        return null
    }

    fun hitCompleteAction(
        touchX: Float,
        touchY: Float,
        camera: OrthographicCamera
    ): CompleteAction? {
        place(camera)
        val buttonWidth = (panelW - pad * 2f - buttonGap * 2f) / 3f
        for (action in CompleteAction.entries) {
            val buttonX = panelX + pad + action.ordinal * (buttonWidth + buttonGap)
            if (
                hits(
                    touchX,
                    touchY,
                    buttonX,
                    buttonY,
                    buttonWidth,
                    buttonH
                )
            ) {
                return action
            }
        }
        return null
    }

    fun hitSlider(t: Vector2, camera: OrthographicCamera): SliderKind? {
        place(camera)
        val rowH = panelH * 0.15f
        val musicY = sliderCenterY + rowH * 0.55f
        val sfxY = sliderCenterY - rowH * 0.55f
        if (t.x in (sliderX() - 18f)..(sliderX() + sliderW + 18f)) {
            if (t.y in (musicY - rowH / 2f)..(musicY + rowH / 2f)) return SliderKind.MUSIC
            if (t.y in (sfxY - rowH / 2f)..(sfxY + rowH / 2f)) return SliderKind.SFX
        }
        return null
    }

    fun beginSliderDrag(kind: SliderKind) { activeSlider = kind }
    fun endSliderDrag() { activeSlider = null }
    fun updateSliderFromDrag(worldX: Float, camera: OrthographicCamera) {
        place(camera)
        val value = MathUtils.clamp((worldX - sliderX()) / sliderW, 0f, 1f)
        when (activeSlider) {
            SliderKind.MUSIC -> {
                game.settingsManager.musicVolume = value
                game.soundManager.setMusicVolume(value)
            }
            SliderKind.SFX -> {
                game.settingsManager.sfxVolume = value
                game.soundManager.setSfxVolume(value)
            }
            null -> Unit
        }
    }

    private fun sliderX() = panelX + panelW - pad - sliderW - panelW * 0.10f
    private fun hits(
        touchX: Float,
        touchY: Float,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ): Boolean {
        return touchX in x..(x + width) && touchY in y..(y + height)
    }

    fun drawPauseToggleButtonShapes(camera: OrthographicCamera, viewport: Viewport, visible: Boolean) {
        val x = camera.position.x - viewport.worldWidth / 2f + 20f * uiScale
        val y = camera.position.y - viewport.worldHeight / 2f + 20f * uiScale
        UI.filled(shapes, x, y, pauseToggleW, pauseToggleH, 12f * uiScale, if (visible) UI.PANEL_ELEVATED else TOGGLE_ACTIVE)
    }

    fun drawPauseToggleButtonText(camera: OrthographicCamera, viewport: Viewport, visible: Boolean) {
        val x = camera.position.x - viewport.worldWidth / 2f + 20f * uiScale + pauseToggleW / 2f
        val y = camera.position.y - viewport.worldHeight / 2f + 20f * uiScale + pauseToggleH * 0.65f
        text(if (visible) "Hide Menu" else "Show Menu", x, y, if (visible) UI.TEXT else UI.BACKGROUND, 0.48f, true)
    }

    fun hitsPauseToggleButton(
        touchX: Float,
        touchY: Float,
        camera: OrthographicCamera,
        viewport: Viewport
    ): Boolean {
        val x = camera.position.x - viewport.worldWidth / 2f + 20f * uiScale
        val y = camera.position.y - viewport.worldHeight / 2f + 20f * uiScale
        return hits(touchX, touchY, x, y, pauseToggleW, pauseToggleH)
    }

    fun drawPauseSliders(@Suppress("UNUSED_PARAMETER") camera: OrthographicCamera) = Unit
    fun dispose() = Unit
}
