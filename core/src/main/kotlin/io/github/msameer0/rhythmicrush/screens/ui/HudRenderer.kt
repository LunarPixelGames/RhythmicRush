package io.github.msameer0.rhythmicrush.screens.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.game.GameWorld

/**
 * Handles the rendering of the gameplay HUD, including the progress bar, attempts, and UI popups.
 */
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
        private val PROGRESS_TRACK = Color(0.2f, 0.2f, 0.2f, 0.55f)
        private val PAUSE_BUTTON = Color(0.2f, 0.2f, 0.2f, 0.75f)
        private val PAUSE_ICON = Color(1f, 1f, 1f, 0.9f)
    }

    private val glyphLayout = GlyphLayout()
    private val textBuilder = StringBuilder(32)

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
        val settings = game.settingsManager
        if (!settings.showProgressBar) return

        val barW = viewport.worldWidth * 0.625f * 0.55f
        val barH = 10f
        val gap = 14f
        val lineY = camTop(camera, viewport) - (settings.uiPadding + 6f)

        var textW = 0f
        if (settings.showPercentage) {
            textBuilder.setLength(0)
            textBuilder.append(MathUtils.round(progress * 100f)).append('%')
            font.data.setScale(1.2f)
            glyphLayout.setText(font, textBuilder, Color.WHITE, 0f, Align.left, false)
            textW = glyphLayout.width
        }

        val totalW = (if (settings.showPercentage) textW + gap else 0f) + barW
        val startX = camera.position.x - totalW / 2f
        val cornerRadius = barH / 2f
        val fillW = barW * progress

        shapes.color = PROGRESS_TRACK
        drawRoundedRect(startX, lineY - barH / 2f, barW, barH, cornerRadius)

        if (fillW >= barH) {
            shapes.color = COL_FILL
            drawRoundedRect(startX, lineY - barH / 2f, fillW, barH, cornerRadius)
        } else if (fillW > 0) {
            shapes.color = COL_FILL
            shapes.rect(startX, lineY - barH / 2f, fillW, barH)
        }
    }

    fun drawPauseButtonShapes(camera: OrthographicCamera, viewport: Viewport) {
        val centerX = pauseCircleCX(camera, viewport)
        val centerY = pauseCircleCY(camera, viewport)
        val radius = PAUSE_BTN / 2f

        shapes.color = PAUSE_BUTTON
        shapes.circle(centerX, centerY, radius, 32)

        val barWidth = radius * 0.22f
        val barHeight = radius * 0.75f
        val gap = radius * 0.18f
        shapes.color = PAUSE_ICON
        shapes.rect(
            centerX - gap - barWidth,
            centerY - barHeight / 2f,
            barWidth,
            barHeight
        )
        shapes.rect(centerX + gap, centerY - barHeight / 2f, barWidth, barHeight)
    }

    fun drawProgressBarText(camera: OrthographicCamera, viewport: Viewport, levelKey: String?) {
        val progress = world.progress
        if (progress <= 0f) return
        val settings = game.settingsManager
        if (!settings.showPercentage) return

        val pct = MathUtils.round(progress * 100f)
        val barW = viewport.worldWidth * 0.625f * 0.55f
        val gap = 14f
        val lineY = camTop(camera, viewport) - (settings.uiPadding + 6f)

        textBuilder.setLength(0)
        textBuilder.append(pct).append('%')
        font.data.setScale(1.2f)
        glyphLayout.setText(font, textBuilder, Color.WHITE, 0f, Align.left, false)
        val textW = glyphLayout.width
        val textH = glyphLayout.height

        val totalW = textW + (if (settings.showProgressBar) gap + barW else 0f)
        val startX = camera.position.x - totalW / 2f
        val textDrawX =
            if (settings.showProgressBar) startX + barW + gap else startX

        var isPersonalBest = false
        if (levelKey != null) {
            val levelProgress = game.progressManager.getOrCreate(levelKey)
            isPersonalBest = pct > levelProgress.bestPercent
        }
        val textColor = if (isPersonalBest) COL_HEADING else Color.WHITE

        font.setColor(0f, 0f, 0f, textColor.a * 0.4f)
        font.draw(batch, textBuilder, textDrawX + 2f, lineY + textH / 2f - 2f)
        font.color = textColor
        font.draw(batch, textBuilder, textDrawX, lineY + textH / 2f)
        font.data.setScale(1f)
    }

    fun drawSessionAttemptsText(
        camera: OrthographicCamera, viewport: Viewport,
        sessionAttempts: Int, levelKey: String?
    ) {
        val settings = game.settingsManager
        val padding = settings.uiPadding
        val left = camLeft(camera, viewport) + padding
        val top = camTop(camera, viewport) - padding
        val shadowOffset = 2f
        var nextY = top

        if (settings.showAttempts) {
            textBuilder.setLength(0)
            textBuilder.append("Attempt  ").append(sessionAttempts)
            font.setColor(0f, 0f, 0f, HUD_ATTEMPT.a * 0.4f)
            font.draw(batch, textBuilder, left + shadowOffset, nextY - shadowOffset)
            font.color = HUD_ATTEMPT
            font.draw(batch, textBuilder, left, nextY)
            nextY -= 26f
        }

        if (settings.showBest && levelKey != null) {
            val levelProgress = game.progressManager.getOrCreate(levelKey)
            textBuilder.setLength(0)
            textBuilder.append("Best  ").append(levelProgress.bestPercent).append('%')
            font.setColor(0f, 0f, 0f, HUD_BEST.a * 0.4f)
            font.draw(batch, textBuilder, left + shadowOffset, nextY - shadowOffset)
            font.color = HUD_BEST
            font.draw(batch, textBuilder, left, nextY)
            nextY -= 26f
        }

        if (settings.showFps) {
            textBuilder.setLength(0)
            textBuilder.append("FPS  ").append(Gdx.graphics.framesPerSecond)
            font.setColor(0f, 0f, 0f, HUD_FPS.a * 0.4f)
            font.draw(batch, textBuilder, left + shadowOffset, nextY - shadowOffset)
            font.color = HUD_FPS
            font.draw(batch, textBuilder, left, nextY)
        }
    }

    fun drawNewBestPopup(camera: OrthographicCamera) {
        if (popupTimer < 0f) return

        var alpha: Float
        var scale: Float
        if (popupTimer < POPUP_FADE_IN) {
            val fadeProgress = popupTimer / POPUP_FADE_IN
            alpha = fadeProgress
            scale = 1.0f + 0.8f * fadeProgress
        } else if (popupTimer < POPUP_FADE_IN + POPUP_HOLD) {
            val holdProgress = (popupTimer - POPUP_FADE_IN) / POPUP_HOLD
            alpha = 1f
            scale = 1.8f - 0.4f * holdProgress
        } else {
            val fadeProgress =
                (popupTimer - POPUP_FADE_IN - POPUP_HOLD) / POPUP_FADE_OUT
            alpha = 1f - fadeProgress
            scale = 1.4f * (1f - fadeProgress)
        }
        alpha = alpha.coerceIn(0f, 1f)
        scale = scale.coerceAtLeast(0f)

        val centerX = camera.position.x
        val centerY = camera.position.y

        font.data.setScale(scale)
        textBuilder.setLength(0)
        textBuilder.append("NEW BEST")
        glyphLayout.setText(font, textBuilder)
        val textH = glyphLayout.height
        val textY = centerY + textH / 2f

        font.setColor(0f, 0f, 0f, alpha * 0.4f)
        font.draw(
            batch,
            textBuilder,
            centerX - glyphLayout.width / 2f + 2f,
            textY - 2f
        )
        font.setColor(COL_HEADING.r, COL_HEADING.g, COL_HEADING.b, alpha)
        font.draw(batch, textBuilder, centerX - glyphLayout.width / 2f, textY)

        font.data.setScale(scale * 0.6f)
        textBuilder.setLength(0)
        textBuilder.append(popupBestPct).append('%')
        glyphLayout.setText(font, textBuilder)
        val pctY = textY - textH - 5f

        font.setColor(0f, 0f, 0f, alpha * 0.85f * 0.4f)
        font.draw(
            batch,
            textBuilder,
            centerX - glyphLayout.width / 2f + 2f,
            pctY - 2f
        )
        font.setColor(1f, 1f, 1f, alpha * 0.85f)
        font.draw(batch, textBuilder, centerX - glyphLayout.width / 2f, pctY)

        font.data.setScale(1f)
        font.color = Color.WHITE
    }

    fun hitsPauseButton(
        touchX: Float,
        touchY: Float,
        camera: OrthographicCamera,
        viewport: Viewport
    ): Boolean {
        val centerX = pauseCircleCX(camera, viewport)
        val centerY = pauseCircleCY(camera, viewport)
        val radius = PAUSE_BTN / 2f + 8f
        val offsetX = touchX - centerX
        val offsetY = touchY - centerY
        return offsetX * offsetX + offsetY * offsetY <= radius * radius
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
