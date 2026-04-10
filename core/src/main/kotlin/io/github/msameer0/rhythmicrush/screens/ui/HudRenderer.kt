package io.github.msameer0.rhythmicrush.screens.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.game.GameWorld
import com.badlogic.gdx.math.MathUtils

class HudRenderer(
    private val game: RhythmicRushGame,
    private val world: GameWorld,
    private val font: BitmapFont,
    private val shapes: ShapeRenderer,
    private val batch: SpriteBatch
) {

    companion object {
        private const val POPUP_FADE_IN = 0.25f
        private const val POPUP_HOLD = 1.20f
        private const val POPUP_FADE_OUT = 0.45f
        private const val POPUP_TOTAL = POPUP_FADE_IN + POPUP_HOLD + POPUP_FADE_OUT

        private const val PAUSE_BTN = 44f

        private val COL_FILL = Color(0.35f, 0.65f, 1.00f, 1f)
        private val COL_HEADING = Color(1f, 0.85f, 0.35f, 1f)
        private val HUD_ATTEMPT = Color(1f, 1f, 1f, 0.85f)
        private val HUD_BEST = Color(1f, 1f, 1f, 0.55f)
        private val HUD_FPS = Color(1f, 1f, 1f, 0.45f)
    }

    private val glyphLayout = GlyphLayout()
    private val sb = java.lang.StringBuilder(32)

    private var popupTimer = -1f
    private var popupBestPct = 0

    fun update(delta: Float) {
        if (popupTimer >= 0f) {
            popupTimer += delta
            if (popupTimer >= POPUP_TOTAL) popupTimer = -1f
        }
    }

    fun showNewBestPopup(bestPct: Int) {
        popupTimer = 0f
        popupBestPct = bestPct
    }

    fun hideNewBestPopup() {
        popupTimer = -1f
    }

    fun drawProgressBarShapes(camera: OrthographicCamera, viewport: Viewport) {
        val progress = world.progress
        if (progress <= 0f) return
        val s = game.settingsManager
        if (!s.showProgressBar) return

        val barW = viewport.worldWidth * 0.625f * 0.55f
        val barH = 10f
        val gap = 14f
        val lineY = camTop(camera, viewport) - (s.uiPadding + 6f)

        var textW = 0f
        if (s.showPercentage) {
            sb.setLength(0)
            sb.append(MathUtils.round(progress * 100f)).append('%')
            font.data.setScale(1.2f)
            glyphLayout.setText(font, sb, Color.WHITE, 0f, Align.left, false)
            textW = glyphLayout.width
        }

        val totalW = (if (s.showPercentage) textW + gap else 0f) + barW
        val startX = camera.position.x - totalW / 2f
        val r = barH / 2f
        val fillW = barW * progress

        shapes.color = Color(0.2f, 0.2f, 0.2f, 0.55f)
        drawRoundedRect(startX, lineY - barH / 2f, barW, barH, r)

        if (fillW >= barH) {
            shapes.color = COL_FILL
            drawRoundedRect(startX, lineY - barH / 2f, fillW, barH, r)
        } else if (fillW > 0) {
            shapes.color = COL_FILL
            shapes.rect(startX, lineY - barH / 2f, fillW, barH)
        }
    }

    fun drawPauseButtonShapes(camera: OrthographicCamera, viewport: Viewport) {
        val cx = pauseCircleCX(camera, viewport)
        val cy = pauseCircleCY(camera, viewport)
        val r = PAUSE_BTN / 2f

        shapes.color = Color(0.2f, 0.2f, 0.2f, 0.75f)
        shapes.circle(cx, cy, r, 32)

        val barW = r * 0.22f
        val barH = r * 0.75f
        val gap = r * 0.18f
        shapes.color = Color(1f, 1f, 1f, 0.9f)
        shapes.rect(cx - gap - barW, cy - barH / 2f, barW, barH)
        shapes.rect(cx + gap, cy - barH / 2f, barW, barH)
    }

    fun drawProgressBarText(camera: OrthographicCamera, viewport: Viewport, levelKey: String?) {
        val progress = world.progress
        if (progress <= 0f) return
        val s = game.settingsManager
        if (!s.showPercentage) return

        val pct = MathUtils.round(progress * 100f)
        val barW = viewport.worldWidth * 0.625f * 0.55f
        val gap = 14f
        val lineY = camTop(camera, viewport) - (s.uiPadding + 6f)

        sb.setLength(0)
        sb.append(pct).append('%')
        font.data.setScale(1.2f)
        glyphLayout.setText(font, sb, Color.WHITE, 0f, Align.left, false)
        val textW = glyphLayout.width
        val textH = glyphLayout.height

        val totalW = textW + (if (s.showProgressBar) gap + barW else 0f)
        val startX = camera.position.x - totalW / 2f
        val textDrawX = if (s.showProgressBar) startX + barW + gap else startX

        var isPB = false
        if (levelKey != null) {
            val p = game.progressManager.getOrCreate(levelKey)
            isPB = pct > (p?.bestPercent ?: 0)
        }
        val textColor = if (isPB) COL_HEADING else Color.WHITE

        font.setColor(0f, 0f, 0f, textColor.a * 0.4f)
        font.draw(batch, sb, textDrawX + 2f, lineY + textH / 2f - 2f)
        font.color = textColor
        font.draw(batch, sb, textDrawX, lineY + textH / 2f)
        font.data.setScale(1f)
    }

    fun drawSessionAttemptsText(
        camera: OrthographicCamera, viewport: Viewport,
        sessionAttempts: Int, levelKey: String?
    ) {
        val s = game.settingsManager
        val p = s.uiPadding
        val left = camLeft(camera, viewport) + p
        val top = camTop(camera, viewport) - p
        val shadowOffset = 2f
        var nextY = top

        if (s.showAttempts) {
            sb.setLength(0)
            sb.append("Attempt  ").append(sessionAttempts)
            font.setColor(0f, 0f, 0f, HUD_ATTEMPT.a * 0.4f)
            font.draw(batch, sb, left + shadowOffset, nextY - shadowOffset)
            font.color = HUD_ATTEMPT
            font.draw(batch, sb, left, nextY)
            nextY -= 26f
        }

        if (s.showBest && levelKey != null) {
            val lp = game.progressManager.getOrCreate(levelKey)
            sb.setLength(0)
            sb.append("Best  ").append(lp?.bestPercent ?: 0).append('%')
            font.setColor(0f, 0f, 0f, HUD_BEST.a * 0.4f)
            font.draw(batch, sb, left + shadowOffset, nextY - shadowOffset)
            font.color = HUD_BEST
            font.draw(batch, sb, left, nextY)
            nextY -= 26f
        }

        if (s.showFps) {
            sb.setLength(0)
            sb.append("FPS  ").append(Gdx.graphics.framesPerSecond)
            font.setColor(0f, 0f, 0f, HUD_FPS.a * 0.4f)
            font.draw(batch, sb, left + shadowOffset, nextY - shadowOffset)
            font.color = HUD_FPS
            font.draw(batch, sb, left, nextY)
        }
    }

    fun drawNewBestPopup(camera: OrthographicCamera) {
        if (popupTimer < 0f) return

        var alpha: Float
        var scale: Float
        if (popupTimer < POPUP_FADE_IN) {
            val t = popupTimer / POPUP_FADE_IN
            alpha = t
            scale = 1.0f + 0.8f * t
        } else if (popupTimer < POPUP_FADE_IN + POPUP_HOLD) {
            val t = (popupTimer - POPUP_FADE_IN) / POPUP_HOLD
            alpha = 1f
            scale = 1.8f - 0.4f * t
        } else {
            val t = (popupTimer - POPUP_FADE_IN - POPUP_HOLD) / POPUP_FADE_OUT
            alpha = 1f - t
            scale = 1.4f * (1f - t)
        }
        alpha = alpha.coerceIn(0f, 1f)
        scale = scale.coerceAtLeast(0f)

        val cx = camera.position.x
        val cy = camera.position.y

        font.data.setScale(scale)
        sb.setLength(0)
        sb.append("NEW BEST")
        glyphLayout.setText(font, sb)
        val textH = glyphLayout.height
        val textY = cy + textH / 2f

        font.setColor(0f, 0f, 0f, alpha * 0.4f)
        font.draw(batch, sb, cx - glyphLayout.width / 2f + 2f, textY - 2f)
        font.setColor(COL_HEADING.r, COL_HEADING.g, COL_HEADING.b, alpha)
        font.draw(batch, sb, cx - glyphLayout.width / 2f, textY)

        font.data.setScale(scale * 0.6f)
        sb.setLength(0)
        sb.append(popupBestPct).append('%')
        glyphLayout.setText(font, sb)
        val pctY = textY - textH - 5f

        font.setColor(0f, 0f, 0f, alpha * 0.85f * 0.4f)
        font.draw(batch, sb, cx - glyphLayout.width / 2f + 2f, pctY - 2f)
        font.setColor(1f, 1f, 1f, alpha * 0.85f)
        font.draw(batch, sb, cx - glyphLayout.width / 2f, pctY)

        font.data.setScale(1f)
        font.color = Color.WHITE
    }

    fun hitsPauseButton(tx: Float, ty: Float, camera: OrthographicCamera, viewport: Viewport): Boolean {
        val cx = pauseCircleCX(camera, viewport)
        val cy = pauseCircleCY(camera, viewport)
        val r = PAUSE_BTN / 2f + 8f
        val dx = tx - cx
        val dy = ty - cy
        return dx * dx + dy * dy <= r * r
    }

    private fun camTop(camera: OrthographicCamera, viewport: Viewport): Float {
        return camera.position.y + viewport.worldHeight / 2f
    }

    private fun camLeft(camera: OrthographicCamera, viewport: Viewport): Float {
        return camera.position.x - viewport.worldWidth / 2f
    }

    private fun pauseCircleCX(camera: OrthographicCamera, viewport: Viewport): Float {
        return camera.position.x + viewport.worldWidth / 2f - PAUSE_BTN / 2f - (game.settingsManager.uiPadding + 2f)
    }

    private fun pauseCircleCY(camera: OrthographicCamera, viewport: Viewport): Float {
        return camera.position.y + viewport.worldHeight / 2f - PAUSE_BTN / 2f - (game.settingsManager.uiPadding + 2f)
    }

    private fun drawRoundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        shapes.rect(x + r, y, w - 2 * r, h)
        shapes.rect(x, y + r, r, h - 2 * r)
        shapes.rect(x + w - r, y + r, r, h - 2 * r)
        shapes.circle(x + r, y + r, r, 16)
        shapes.circle(x + w - r, y + r, r, 16)
        shapes.circle(x + r, y + h - r, r, 16)
        shapes.circle(x + w - r, y + h - r, r, 16)
    }
}
