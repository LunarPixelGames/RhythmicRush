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
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.game.level.LevelData
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Draws the pause overlay, level-complete overlay, and volume slider UI.
 *
 * Extracted from `GameScreen` to separate overlay presentation logic
 * from core gameplay coordination. GameScreen passes its current camera/viewport
 * and UI-scaling values into each call rather than this class tracking them
 * internally, keeping state minimal.
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

    // ── Layout defaults (overridden per platform via updateScale) ─────────────
    private var panelW = 520f
    private var panelH = 360f
    private var btnSize = 72f
    var uiScale = 1.0f
        private set

    companion object {
        // ── Colours ───────────────────────────────────────────────────────────────
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

    // ── Cached panel texture ──────────────────────────────────────────────────
    private var panelTexture: Texture? = null
    private var lastPanelW = -1
    private var lastPanelH = -1

    // ── Slider drag state ─────────────────────────────────────────────────────
    var isSliderDragging = false
        private set

    // ── Scaling ───────────────────────────────────────────────────────────────

    /**
     * Call on show() and resize() to apply platform-appropriate sizes.
     */
    fun updateScale() {
        val mobile = Gdx.app.type == com.badlogic.gdx.Application.ApplicationType.Android ||
                Gdx.app.type == com.badlogic.gdx.Application.ApplicationType.iOS
        if (mobile) {
            panelW = 740f
            panelH = 480f
            btnSize = 100f
            uiScale = 1.4f
        } else {
            panelW = 520f
            panelH = 360f
            btnSize = 72f
            uiScale = 1.0f
        }
        lastPanelW = -1 // invalidate cached texture
    }

    // ── Full-screen dim (call inside a ShapeRenderer.Filled block) ────────────

    fun drawDimOverlay(camera: OrthographicCamera, viewport: Viewport) {
        shapes.color = COL_OVERLAY
        shapes.rect(camLeft(camera, viewport), camBot(camera, viewport),
            viewport.worldWidth, viewport.worldHeight)
    }

    // ── Pause overlay ─────────────────────────────────────────────────────────

    /**
     * Draws the pause panel with level info and slider label.
     * Must be called inside an open `batch.begin()` block.
     */
    fun drawPauseOverlay(camera: OrthographicCamera, sessionAttempts: Int, levelKey: String?) {
        ensurePanel()
        val px = panelX(camera)
        val py = panelY(camera)
        val shadow = 2f * uiScale

        panelTexture?.let { batch.draw(it, px, py) }

        // Level name
        val name = levelData?.name ?: "Level"
        pauseFont.data.setScale(1.1f * uiScale)
        layout.setText(pauseFont, name)
        var x = px + panelW / 2f - layout.width / 2f
        var y = py + panelH - 18f * uiScale
        drawShadowText(name, x, y, COL_HEADING, shadow)

        var sy = y - layout.height - 22f * uiScale
        if (levelKey != null) {
            val p = game.progressManager.getOrCreate(levelKey)

            pauseFont.data.setScale(0.68f * uiScale)
            val best = "Personal Best: " + p?.bestPercent + "%"
            layout.setText(pauseFont, best)
            x = px + panelW / 2f - layout.width / 2f
            drawShadowText(best, x, sy, COL_LABEL, shadow)

            sy -= layout.height + 14f * uiScale
            val att = "Total: " + p?.totalAttempts + "   Session: " + sessionAttempts
            layout.setText(pauseFont, att)
            x = px + panelW / 2f - layout.width / 2f
            drawShadowText(att, x, sy, COL_DIM, shadow)
        }

        // Buttons
        if (backRegion != null) batch.draw(backRegion, backX(camera), backY(camera), btnSize * 0.9f, btnSize * 0.9f)
        if (resumeRegion != null) batch.draw(resumeRegion, resumeX(camera), backY(camera), btnSize * 0.9f, btnSize * 0.9f)

        // Volume label
        val sliderY = sliderY(camera)
        pauseFont.data.setScale(0.58f * uiScale)
        layout.setText(pauseFont, "Volume")
        x = px + panelW / 2f - layout.width / 2f
        y = sliderY + layout.height + 12f * uiScale
        drawShadowText("Volume", x, y, COL_LABEL, shadow)

        // Volume percentage
        val vol = game.settingsManager.musicVolume
        pauseFont.data.setScale(0.48f * uiScale)
        val volPct = round(vol * 100f).toInt().toString() + "%"
        layout.setText(pauseFont, volPct)
        x = sliderTrackX(camera) - layout.width - 12f * uiScale
        y = sliderY + layout.height / 2f
        drawShadowText(volPct, x, y, COL_DIM, shadow)

        // Button labels
        pauseFont.data.setScale(0.5f * uiScale)
        val labelY = backY(camera) - 6f * uiScale
        layout.setText(pauseFont, "Back")
        x = backX(camera) + btnSize * 0.9f / 2f - layout.width / 2f
        drawShadowText("Back", x, labelY, COL_DIM, shadow)

        layout.setText(pauseFont, "Resume")
        x = resumeX(camera) + btnSize * 0.9f / 2f - layout.width / 2f
        drawShadowText("Resume", x, labelY, COL_DIM, shadow)

        pauseFont.data.setScale(1f)
    }

    /**
     * Draws the volume slider track, fill, and thumb.
     * Manages its own GL blend and ShapeRenderer calls — call outside batch.
     */
    fun drawPauseSlider(camera: OrthographicCamera) {
        val sliderY = sliderY(camera)
        val vol = game.settingsManager.musicVolume
        val tx = sliderTrackX(camera)
        val tw = sliderTrackW()
        val trackH = 5f * uiScale
        val thumbR = 10f * uiScale
        val fillW = tw * vol

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = COL_TRACK
        shapes.rect(tx, sliderY - trackH / 2f, tw, trackH)
        shapes.color = COL_FILL
        if (fillW > 0) shapes.rect(tx, sliderY - trackH / 2f, fillW, trackH)
        shapes.color = COL_THUMB
        shapes.circle(tx + fillW, sliderY, thumbR, 24)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    // ── Complete overlay ──────────────────────────────────────────────────────

    /**
     * Draws the level-complete panel.
     * Must be called inside an open `batch.begin()` block.
     */
    fun drawCompleteOverlay(camera: OrthographicCamera, sessionAttempts: Int, levelKey: String?) {
        ensurePanel()
        val px = panelX(camera)
        val py = panelY(camera)
        val shadow = 2f * uiScale

        panelTexture?.let { batch.draw(it, px, py) }

        pauseFont.data.setScale(1.25f * uiScale)
        layout.setText(pauseFont, "LEVEL COMPLETE")
        var x = px + panelW / 2f - layout.width / 2f
        val y = py + panelH - 22f * uiScale
        drawShadowText("LEVEL COMPLETE", x, y, COL_HEADING, shadow)

        var sy = y - layout.height - 35f * uiScale
        if (levelKey != null) {
            val p = game.progressManager.getOrCreate(levelKey)

            pauseFont.data.setScale(0.85f * uiScale)
            val total = "Total Attempts: " + p?.totalAttempts
            layout.setText(pauseFont, total)
            x = px + panelW / 2f - layout.width / 2f
            drawShadowText(total, x, sy, COL_LABEL, shadow)

            sy -= layout.height + 20f * uiScale
            val session = "Session Attempts: " + sessionAttempts
            layout.setText(pauseFont, session)
            x = px + panelW / 2f - layout.width / 2f
            drawShadowText(session, x, sy, COL_DIM, shadow)
        }

        if (backRegion != null) batch.draw(backRegion, backX(camera), backY(camera), btnSize * 0.9f, btnSize * 0.9f)
        if (resumeRegion != null) batch.draw(resumeRegion, resumeX(camera), backY(camera), btnSize * 0.9f, btnSize * 0.9f)

        pauseFont.data.setScale(0.52f * uiScale)
        val labelY = backY(camera) - 6f * uiScale
        layout.setText(pauseFont, "Menu")
        x = backX(camera) + btnSize * 0.9f / 2f - layout.width / 2f
        drawShadowText("Menu", x, labelY, COL_DIM, shadow)

        layout.setText(pauseFont, "Replay")
        x = resumeX(camera) + btnSize * 0.9f / 2f - layout.width / 2f
        drawShadowText("Replay", x, labelY, COL_DIM, shadow)

        pauseFont.data.setScale(1f)
    }

    // ── Hit testing ───────────────────────────────────────────────────────────

    fun hitsBackButton(tx: Float, ty: Float, camera: OrthographicCamera): Boolean {
        return hits(tx, ty, backX(camera), backY(camera), btnSize * 0.9f, btnSize)
    }

    fun hitsResumeButton(tx: Float, ty: Float, camera: OrthographicCamera): Boolean {
        return hits(tx, ty, resumeX(camera), backY(camera), btnSize * 0.9f, btnSize)
    }

    fun hitsSlider(t: Vector2, camera: OrthographicCamera): Boolean {
        val tx = sliderTrackX(camera)
        val tw = sliderTrackW()
        val ty = sliderY(camera)
        return t.x in (tx - 16f)..(tx + tw + 16f) && t.y in (ty - 16f)..(ty + 16f)
    }

    // ── Slider interaction ────────────────────────────────────────────────────

    fun beginSliderDrag() { isSliderDragging = true }
    fun endSliderDrag() { isSliderDragging = false }

    /**
     * Updates music volume from a dragged world X coordinate.
     *
     * @param worldX The unprojected world X coordinate of the touch.
     */
    fun updateSliderFromDrag(worldX: Float, camera: OrthographicCamera) {
        val tsx = sliderTrackX(camera)
        val tsw = sliderTrackW()
        val vol = max(0f, min(1f, (worldX - tsx) / tsw))
        game.settingsManager.musicVolume = vol
        game.soundManager.setMusicVolume(vol)
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private fun panelX(c: OrthographicCamera): Float { return c.position.x - panelW / 2f }
    private fun panelY(c: OrthographicCamera): Float { return c.position.y - panelH / 2f }
    private fun resumeX(c: OrthographicCamera): Float { return c.position.x + 16f }
    private fun backX(c: OrthographicCamera): Float { return c.position.x - btnSize - 16f }
    private fun backY(c: OrthographicCamera): Float { return panelY(c) + 20f }
    fun getSliderTrackX(c: OrthographicCamera): Float { return panelX(c) + panelW * 0.18f }
    private fun sliderTrackX(c: OrthographicCamera): Float { return getSliderTrackX(c) }
    fun getSliderTrackW(): Float { return panelW * 0.64f }
    private fun sliderTrackW(): Float { return getSliderTrackW() }
    private fun sliderY(c: OrthographicCamera): Float { return panelY(c) + btnSize + 38f }

    // ── Internals ─────────────────────────────────────────────────────────────

    private fun ensurePanel() {
        val tw = panelW.toInt()
        val th = panelH.toInt()
        if (panelTexture == null || tw != lastPanelW || th != lastPanelH) {
            panelTexture?.dispose()
            panelTexture = createRoundedRect(tw, th, (24 * uiScale).toInt(), COL_PANEL)
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
