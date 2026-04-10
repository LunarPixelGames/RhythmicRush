package io.github.msameer0.rhythmicrush.game.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer

/**
 * Draws debug hitbox overlays for all active game entities.
 *
 * Extracted from [GameRenderer] so the main render path stays clean and
 * this debug-only code can be stripped easily for release builds if needed.
 *
 * Requires an open GL blend state before [draw] is called; it manages
 * its own [ShapeRenderer] begin/end calls internally.
 */
class HitboxRenderer(private val world: GameWorld, private val shape: ShapeRenderer) {

    companion object {
        // Filled colours
        private val HB_PLAYER_FILL = Color(1.0f, 0.9f, 0.0f, 0.75f)
        private val HB_HAZARD_FILL = Color(1.0f, 0.2f, 0.2f, 0.75f)
        private val HB_BLOCK_FILL = Color(0.2f, 0.5f, 1.0f, 0.75f)
        private val HB_PORTAL_FILL = Color(0.2f, 1.0f, 0.4f, 0.75f)
        private val HB_ORB_FILL = Color(1.0f, 0.9f, 0.1f, 0.55f)

        // Outline colours
        private val HB_PLAYER_LINE = Color(1.0f, 0.9f, 0.0f, 1.0f)
        private val HB_HAZARD_LINE = Color(1.0f, 0.2f, 0.2f, 1.0f)
        private val HB_BLOCK_LINE = Color(0.2f, 0.5f, 1.0f, 1.0f)
        private val HB_PORTAL_LINE = Color(0.2f, 1.0f, 0.4f, 1.0f)
        private val HB_ORB_LINE = Color(1.0f, 0.9f, 0.1f, 1.0f)
    }

    /**
     * Draws filled and outlined hitboxes for all entities in the world.
     *
     * @param camera The projection camera to use.
     * @param player The current player instance.
     */
    fun draw(camera: OrthographicCamera, player: AbstractPlayer) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shape.projectionMatrix = camera.combined

        drawFilled(player)
        drawOutlines(player)

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    // ── Filled pass ───────────────────────────────────────────────────────────

    private fun drawFilled(player: AbstractPlayer) {
        shape.begin(ShapeRenderer.ShapeType.Filled)

        shape.color = HB_BLOCK_FILL
        for (b in world.blocks) {
            if (b is Slope) drawFilledSlope(b)
            else shape.rect(b.x, b.y, b.width, b.height)
        }

        shape.color = HB_PORTAL_FILL
        for (p in world.portals) {
            val r = p.bounds
            shape.rect(r.x, r.y, r.width, r.height)
        }

        shape.color = HB_HAZARD_FILL
        for (h in world.hazards) {
            when (h.type) {
                AbstractHazard.HazardType.SPIKE -> {
                    val r = (h as Spike).hitbox
                    shape.rect(r.x, r.y, r.width, r.height)
                }
                AbstractHazard.HazardType.HALF_SPIKE -> {
                    val r = (h as HalfSpike).hitbox
                    shape.rect(r.x, r.y, r.width, r.height)
                }
                else -> shape.rect(h.x, h.y, h.width, h.height)
            }
        }

        shape.color = HB_ORB_FILL
        val cullStart = world.orbCull
        for (i in cullStart until world.orbs.size) {
            val orb = world.orbs.get(i)
            val r = orb.bounds
            shape.rect(r.x, r.y, r.width, r.height)
        }

        shape.color = HB_PLAYER_FILL
        val pb = player.bounds
        shape.rect(pb.x, pb.y, pb.width, pb.height)

        shape.setColor(1.0f, 1.0f, 1.0f, 0.5f)
        val radius = player.width * 0.5f * Slope.CIRCLE_RATIO
        shape.circle(pb.x + pb.width * 0.5f, pb.y + pb.height * 0.5f, radius)

        shape.end()
    }

    private fun drawFilledSlope(s: Slope) {
        val rot = (s.rotation.toInt() % 360 + 360) % 360
        val x = s.x
        val y = s.y
        val w = s.width
        val h = s.height
        when (rot) {
            0 -> shape.triangle(x, y, x + w, y, x + w, y + h)
            90 -> shape.triangle(x, y + h, x + w, y + h, x + w, y)
            180 -> shape.triangle(x, y, x, y + h, x + w, y + h)
            270 -> shape.triangle(x, y, x, y + h, x + w, y)
            else -> shape.rect(x, y, w, h)
        }
    }

    // ── Outline pass ──────────────────────────────────────────────────────────

    private fun drawOutlines(player: AbstractPlayer) {
        shape.begin(ShapeRenderer.ShapeType.Line)

        shape.color = HB_BLOCK_LINE
        for (b in world.blocks) {
            if (b is Slope) drawOutlineSlope(b)
            else shape.rect(b.x, b.y, b.width, b.height)
        }

        shape.color = HB_PORTAL_LINE
        for (p in world.portals) {
            val r = p.bounds
            shape.rect(r.x, r.y, r.width, r.height)
        }

        shape.color = HB_HAZARD_LINE
        for (h in world.hazards) {
            when (h.type) {
                AbstractHazard.HazardType.SPIKE -> {
                    val r = (h as Spike).hitbox
                    shape.rect(r.x, r.y, r.width, r.height)
                }
                AbstractHazard.HazardType.HALF_SPIKE -> {
                    val r = (h as HalfSpike).hitbox
                    shape.rect(r.x, r.y, r.width, r.height)
                }
                AbstractHazard.HazardType.SAW_BLADE -> {
                    val saw = h as SawBlade
                    shape.circle(
                        saw.x + saw.diameter / 2f,
                        saw.y + saw.diameter / 2f,
                        saw.diameter / 2f, 32
                    )
                }
                else -> {}
            }
        }

        shape.color = HB_ORB_LINE
        val cullStart = world.orbCull
        for (i in cullStart until world.orbs.size) {
            val orb = world.orbs.get(i)
            val r = orb.bounds
            shape.rect(r.x, r.y, r.width, r.height)
        }

        shape.color = HB_PLAYER_LINE
        val pb = player.bounds
        shape.rect(pb.x, pb.y, pb.width, pb.height)

        shape.setColor(1.0f, 1.0f, 1.0f, 0.8f)
        val radius = player.width * 0.5f * Slope.CIRCLE_RATIO
        shape.circle(pb.x + pb.width * 0.5f, pb.y + pb.height * 0.5f, radius)

        shape.end()
    }

    private fun drawOutlineSlope(s: Slope) {
        val rot = (s.rotation.toInt() % 360 + 360) % 360
        val x = s.x
        val y = s.y
        val w = s.width
        val h = s.height
        val line = s.getSlopeLine()
        val solidCX: Float
        val solidCY: Float
        when (rot) {
            0 -> {
                solidCX = x + w
                solidCY = y
            }
            90 -> {
                solidCX = x + w
                solidCY = y + h
            }
            180 -> {
                solidCX = x
                solidCY = y + h
            }
            else -> {
                solidCX = x
                solidCY = y
            }
        }
        shape.triangle(line[0], line[1], line[2], line[3], solidCX, solidCY)
    }
}
