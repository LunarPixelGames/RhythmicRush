package io.github.msameer0.rhythmicrush.screens.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
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

/**
 * Manages the rendering and interaction logic for gameplay overlays such as pause and completion screens.
 */
class OverlayUI(
    private val game: RhythmicRushGame,
    private val levelData: LevelData?,
    private val pauseFont: BitmapFont,
    private val shapes: ShapeRenderer,
    private val batch: SpriteBatch,
    private val resumeRegion: TextureRegion?,
    private val backRegion: TextureRegion?
) {
    enum class SliderKind { MUSIC, SFX }

    private var panelW = 520f
    private var panelH = 360f
    private var panelPadX = 40f
    private var panelPadY = 32f
    private var cornerRadius = 24f
    private var btnSize = 72f
    private var actionGap = 24f
    private var titleScale = 1f
    private var bodyScale = 1f
    private var metaScale = 1f
    private var smallScale = 1f
    private var sliderGap = 64f
    private var sliderTrackWidth = 320f
    private var sliderTrackHeight = 6f
    private var sliderThumbRadius = 10f
    var uiScale = 1.0f
        private set

    companion object {
        private val COL_OVERLAY = Color(0f, 0f, 0f, 0.65f)
        private val COL_PANEL = Color(0.11f, 0.11f, 0.17f, 1f)
        private val COL_HEADING = Color(1f, 0.85f, 0.35f, 1f)
        private val COL_LABEL = Color(1f, 1f, 1f, 0.85f)
        private val COL_DIM = Color(1f, 1f, 1f, 0.50f)
        private val COL_TRACK = Color(0.28f, 0.28f, 0.35f, 1f)
        private val COL_FILL = Color(0.35f, 0.65f, 1.00f, 1f)
        private val COL_THUMB = Color(1f, 1f, 1f, 1f)

        private fun hits(tx: Float, ty: Float, x: Float, y: Float, w: Float, h: Float): Boolean {
            return tx in x..(x + w) && ty in y..(y + h)
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

        private fun camLeft(c: OrthographicCamera, v: Viewport): Float {
            return c.position.x - v.worldWidth / 2f
        }

        private fun camBot(c: OrthographicCamera, v: Viewport): Float {
            return c.position.y - v.worldHeight / 2f
        }
    }

    private val layout = GlyphLayout()

    private var panelTexture: Texture? = null
    private var lastPanelW = -1
    private var lastPanelH = -1

    var activeSlider: SliderKind? = null
        private set

    fun updateScale(viewport: Viewport) {
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight
        val mobile = Gdx.app.type == com.badlogic.gdx.Application.ApplicationType.Android ||
            Gdx.app.type == com.badlogic.gdx.Application.ApplicationType.iOS
        val baseScale = MathUtils.clamp(minOf(vw / 1920f, vh / 1080f), 0.95f, 1.25f)
        if (mobile) {
            panelW = minOf(vw * 0.88f, 1520f)
            panelH = minOf(vh * 0.74f, 920f)
            uiScale = MathUtils.clamp(baseScale * 1.3f, 1.2f, 1.8f)
        } else {
            panelW = minOf(vw * 0.54f, 940f)
            panelH = minOf(vh * 0.58f, 560f)
            uiScale = baseScale
        }
        panelPadX = panelW * 0.085f
        panelPadY = panelH * 0.085f
        cornerRadius = panelW * 0.035f
        btnSize = panelH * 0.16f
        actionGap = btnSize * 0.32f
        titleScale = 0.9f * uiScale
        bodyScale = 0.58f * uiScale
        metaScale = 0.46f * uiScale
        smallScale = 0.4f * uiScale
        sliderGap = panelH * 0.14f
        sliderTrackWidth = panelW * 0.56f
        sliderTrackHeight = maxOf(4f, panelH * 0.012f)
        sliderThumbRadius = panelH * 0.022f
        lastPanelW = -1
    }

    fun drawDimOverlay(camera: OrthographicCamera, viewport: Viewport) {
        shapes.color = COL_OVERLAY
        shapes.rect(
            camLeft(camera, viewport), camBot(camera, viewport),
            viewport.worldWidth, viewport.worldHeight
        )
    }

    fun drawPauseOverlay(camera: OrthographicCamera, sessionAttempts: Int, levelKey: String?) {
        ensurePanel()
        val px = panelX(camera)
        val py = panelY(camera)
        val shadow = 2f * uiScale
        val centerX = px + panelW / 2f
        val titleY = py + panelH - panelPadY
        val statsY = titleY - 84f * uiScale
        val actionsY = py + panelPadY
        val contentCenterY = py + panelH * 0.40f

        panelTexture?.let { batch.draw(it, px, py) }

        val name = levelData?.name ?: "Level"
        pauseFont.data.setScale(titleScale)
        layout.setText(pauseFont, name)
        var x = centerX - layout.width / 2f
        drawShadowText(name, x, titleY, COL_HEADING, shadow)

        var sy = statsY
        if (levelKey != null) {
            val p = game.progressManager.getOrCreate(levelKey)

            pauseFont.data.setScale(bodyScale)
            val best = "Personal Best: " + p?.bestPercent + "%"
            layout.setText(pauseFont, best)
            x = centerX - layout.width / 2f
            drawShadowText(best, x, sy, COL_LABEL, shadow)

            sy -= layout.height + 14f * uiScale
            val att = "Total: " + p?.totalAttempts + "   Session: " + sessionAttempts
            layout.setText(pauseFont, att)
            x = centerX - layout.width / 2f
            drawShadowText(att, x, sy, COL_DIM, shadow)
        }

        if (backRegion != null) batch.draw(
            backRegion,
            backX(camera),
            backY(camera),
            btnSize,
            btnSize
        )
        if (resumeRegion != null) batch.draw(
            resumeRegion,
            resumeX(camera),
            backY(camera),
            btnSize,
            btnSize
        )

        val musicSliderY = contentCenterY + sliderGap * 0.5f
        pauseFont.data.setScale(bodyScale)
        layout.setText(pauseFont, "Music Volume")
        x = sliderTrackX(camera)
        var y = musicSliderY + 34f * uiScale
        drawShadowText("Music Volume", x, y, COL_LABEL, shadow)

        val vol = game.settingsManager.musicVolume
        pauseFont.data.setScale(metaScale)
        val volPct = MathUtils.round(vol * 100f).toString() + "%"
        layout.setText(pauseFont, volPct)
        x = sliderTrackX(camera) + sliderTrackWidth - layout.width
        y = musicSliderY + 34f * uiScale
        drawShadowText(volPct, x, y, COL_DIM, shadow)

        val sfxSliderY = contentCenterY - sliderGap * 0.5f
        pauseFont.data.setScale(bodyScale)
        layout.setText(pauseFont, "SFX Volume")
        x = sliderTrackX(camera)
        y = sfxSliderY + 34f * uiScale
        drawShadowText("SFX Volume", x, y, COL_LABEL, shadow)

        val sfx = game.settingsManager.sfxVolume
        pauseFont.data.setScale(metaScale)
        val sfxPct = MathUtils.round(sfx * 100f).toString() + "%"
        layout.setText(pauseFont, sfxPct)
        x = sliderTrackX(camera) + sliderTrackWidth - layout.width
        y = sfxSliderY + 34f * uiScale
        drawShadowText(sfxPct, x, y, COL_DIM, shadow)

        pauseFont.data.setScale(smallScale)
        val labelY = actionsY - 12f * uiScale
        layout.setText(pauseFont, "Back")
        x = backX(camera) + btnSize / 2f - layout.width / 2f
        drawShadowText("Back", x, labelY, COL_DIM, shadow)

        layout.setText(pauseFont, "Resume")
        x = resumeX(camera) + btnSize / 2f - layout.width / 2f
        drawShadowText("Resume", x, labelY, COL_DIM, shadow)

        pauseFont.data.setScale(1f)
    }

    fun drawPauseSliders(camera: OrthographicCamera) {
        val tx = sliderTrackX(camera)
        val tw = sliderTrackW()
        val trackH = sliderTrackHeight
        val thumbR = sliderThumbRadius

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawSliderTrack(tx, sliderY(camera, SliderKind.MUSIC), tw, trackH, thumbR, game.settingsManager.musicVolume)
        drawSliderTrack(tx, sliderY(camera, SliderKind.SFX), tw, trackH, thumbR, game.settingsManager.sfxVolume)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    fun drawCompleteOverlay(camera: OrthographicCamera, sessionAttempts: Int, levelKey: String?) {
        ensurePanel()
        val px = panelX(camera)
        val py = panelY(camera)
        val shadow = 2f * uiScale
        val centerX = px + panelW / 2f
        val titleY = py + panelH - panelPadY
        val statsY = py + panelH * 0.54f
        val actionsY = py + panelPadY

        panelTexture?.let { batch.draw(it, px, py) }

        pauseFont.data.setScale(titleScale)
        layout.setText(pauseFont, "LEVEL COMPLETE")
        var x = centerX - layout.width / 2f
        drawShadowText("LEVEL COMPLETE", x, titleY, COL_HEADING, shadow)

        var sy = statsY
        if (levelKey != null) {
            val p = game.progressManager.getOrCreate(levelKey)

            pauseFont.data.setScale(bodyScale)
            val total = "Total Attempts: " + p?.totalAttempts
            layout.setText(pauseFont, total)
            x = centerX - layout.width / 2f
            drawShadowText(total, x, sy, COL_LABEL, shadow)

            sy -= layout.height + 18f * uiScale
            val session = "Session Attempts: " + sessionAttempts
            layout.setText(pauseFont, session)
            x = centerX - layout.width / 2f
            drawShadowText(session, x, sy, COL_DIM, shadow)
        }

        if (backRegion != null) batch.draw(
            backRegion,
            backX(camera),
            backY(camera),
            btnSize,
            btnSize
        )
        if (resumeRegion != null) batch.draw(
            resumeRegion,
            resumeX(camera),
            backY(camera),
            btnSize,
            btnSize
        )

        pauseFont.data.setScale(smallScale)
        val labelY = actionsY - 12f * uiScale
        layout.setText(pauseFont, "Menu")
        x = backX(camera) + btnSize / 2f - layout.width / 2f
        drawShadowText("Menu", x, labelY, COL_DIM, shadow)

        layout.setText(pauseFont, "Replay")
        x = resumeX(camera) + btnSize / 2f - layout.width / 2f
        drawShadowText("Replay", x, labelY, COL_DIM, shadow)

        pauseFont.data.setScale(1f)
    }

    fun hitsBackButton(tx: Float, ty: Float, camera: OrthographicCamera): Boolean {
        return hits(tx, ty, backX(camera), backY(camera), btnSize, btnSize)
    }

    fun hitsResumeButton(tx: Float, ty: Float, camera: OrthographicCamera): Boolean {
        return hits(tx, ty, resumeX(camera), backY(camera), btnSize, btnSize)
    }

    fun hitSlider(t: Vector2, camera: OrthographicCamera): SliderKind? {
        val tx = sliderTrackX(camera)
        val tw = sliderTrackW()
        for (kind in SliderKind.entries) {
            val ty = sliderY(camera, kind)
            if (t.x in tx..(tx + tw) && t.y in (ty - 24f * uiScale)..(ty + 48f * uiScale)) return kind
        }
        return null
    }

    fun beginSliderDrag(kind: SliderKind) {
        activeSlider = kind
    }

    fun endSliderDrag() {
        activeSlider = null
    }

    fun updateSliderFromDrag(worldX: Float, camera: OrthographicCamera) {
        val tsx = sliderTrackX(camera)
        val tsw = sliderTrackW()
        val vol = MathUtils.clamp((worldX - tsx) / tsw, 0f, 1f)
        when (activeSlider) {
            SliderKind.MUSIC -> {
                game.settingsManager.musicVolume = vol
                game.soundManager.setMusicVolume(vol)
            }
            SliderKind.SFX -> {
                game.settingsManager.sfxVolume = vol
                game.soundManager.setSfxVolume(vol)
            }
            null -> {}
        }
    }

    private fun panelX(c: OrthographicCamera): Float {
        return c.position.x - panelW / 2f
    }

    private fun panelY(c: OrthographicCamera): Float {
        return c.position.y - panelH / 2f
    }

    private fun resumeX(c: OrthographicCamera): Float {
        return c.position.x + actionGap / 2f
    }

    private fun backX(c: OrthographicCamera): Float {
        return c.position.x - actionGap / 2f - btnSize
    }

    private fun backY(c: OrthographicCamera): Float {
        return panelY(c) + panelPadY
    }

    fun getSliderTrackX(c: OrthographicCamera): Float {
        return panelX(c) + panelPadX
    }

    private fun sliderTrackX(c: OrthographicCamera): Float {
        return getSliderTrackX(c)
    }

    fun getSliderTrackW(): Float {
        return sliderTrackWidth
    }

    private fun sliderTrackW(): Float {
        return getSliderTrackW()
    }

    private fun sliderY(c: OrthographicCamera, kind: SliderKind): Float {
        val centerY = panelY(c) + panelH * 0.40f
        return if (kind == SliderKind.MUSIC) centerY + sliderGap * 0.5f else centerY - sliderGap * 0.5f
    }

    private fun drawSliderTrack(
        tx: Float,
        sliderY: Float,
        trackW: Float,
        trackH: Float,
        thumbR: Float,
        value: Float
    ) {
        val fillW = trackW * value
        shapes.color = COL_TRACK
        shapes.rect(tx, sliderY - trackH / 2f, trackW, trackH)
        shapes.color = COL_FILL
        if (fillW > 0) shapes.rect(tx, sliderY - trackH / 2f, fillW, trackH)
        shapes.color = COL_THUMB
        shapes.circle(tx + fillW, sliderY, thumbR, 24)
    }

    private fun ensurePanel() {
        val tw = panelW.toInt()
        val th = panelH.toInt()
        if (panelTexture == null || tw != lastPanelW || th != lastPanelH) {
            panelTexture?.dispose()
            panelTexture = createRoundedRect(tw, th, cornerRadius.toInt(), COL_PANEL)
            lastPanelW = tw
            lastPanelH = th
        }
    }


    private fun drawShadowText(text: String, x: Float, y: Float, color: Color, shadow: Float) {
        pauseFont.setColor(0f, 0f, 0f, color.a * 0.4f)
        pauseFont.draw(batch, text, x + shadow, y - shadow)
        pauseFont.color = color
        pauseFont.draw(batch, text, x, y)
    }

    fun dispose() {
        panelTexture?.dispose()
        panelTexture = null
    }
}
