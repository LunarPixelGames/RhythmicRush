package io.github.msameer0.rhythmicrush.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Shared visual language for menu and gameplay overlays.
 *
 * Optional font assets:
 * assets/fonts/Orbitron-Bold.ttf
 * assets/fonts/Orbitron-SemiBold.ttf
 * assets/fonts/Rajdhani-Medium.ttf
 * assets/fonts/Rajdhani-SemiBold.ttf
 */
object NeonUI {
    val BACKGROUND = Color.valueOf("121323")
    val BACKGROUND_SECONDARY = Color.valueOf("171827")
    val PANEL = Color.valueOf("222338")
    val PANEL_ELEVATED = Color.valueOf("292A44")
    val BORDER = Color.valueOf("6F4DFF")
    val LIME = Color.valueOf("C8FF4D")
    val LIME_HOVER = Color.valueOf("DFFF77")
    val YELLOW = Color.valueOf("FFD94A")
    val BLUE = Color.valueOf("5AA2FF")
    val TEXT = Color.valueOf("F2F2F7")
    val TEXT_SECONDARY = Color.valueOf("B9B8C8")
    val TEXT_MUTED = Color.valueOf("7D7A91")
    val DANGER = Color.valueOf("FF5F6D")
    val OVERLAY = Color(0.025f, 0.025f, 0.08f, 0.78f)

    fun rounded(
        shapes: ShapeRenderer,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float
    ) {
        val r = radius.coerceAtMost(minOf(w, h) / 2f)
        shapes.rect(x + r, y, w - r * 2f, h)
        shapes.rect(x, y + r, r, h - r * 2f)
        shapes.rect(x + w - r, y + r, r, h - r * 2f)
        shapes.circle(x + r, y + r, r, 20)
        shapes.circle(x + w - r, y + r, r, 20)
        shapes.circle(x + r, y + h - r, r, 20)
        shapes.circle(x + w - r, y + h - r, r, 20)
    }

    fun filled(
        shapes: ShapeRenderer,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        color: Color
    ) {
        shapes.color = color
        rounded(shapes, x, y, w, h, radius)
    }

    fun outlined(
        shapes: ShapeRenderer,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        fill: Color = PANEL,
        border: Color = Color(BORDER.r, BORDER.g, BORDER.b, 0.55f),
        thickness: Float = 2f
    ) {
        filled(shapes, x, y, w, h, radius, border)
        filled(
            shapes,
            x + thickness,
            y + thickness,
            w - thickness * 2f,
            h - thickness * 2f,
            (radius - thickness).coerceAtLeast(1f),
            fill
        )
    }
}
