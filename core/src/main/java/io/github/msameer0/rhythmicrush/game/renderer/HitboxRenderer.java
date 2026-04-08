package io.github.msameer0.rhythmicrush.game.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.AbstractOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * Draws debug hitbox overlays for all active game entities.
 *
 * <p>Extracted from {@link GameRenderer} so the main render path stays clean and
 * this debug-only code can be stripped easily for release builds if needed.</p>
 *
 * <p>Requires an open GL blend state before {@link #draw} is called; it manages
 * its own {@link ShapeRenderer} begin/end calls internally.</p>
 */
public class HitboxRenderer {

    // Filled colours
    private static final Color HB_PLAYER_FILL = new Color(1.0f, 0.9f, 0.0f, 0.75f);
    private static final Color HB_HAZARD_FILL = new Color(1.0f, 0.2f, 0.2f, 0.75f);
    private static final Color HB_BLOCK_FILL  = new Color(0.2f, 0.5f, 1.0f, 0.75f);
    private static final Color HB_PORTAL_FILL = new Color(0.2f, 1.0f, 0.4f, 0.75f);
    private static final Color HB_ORB_FILL    = new Color(1.0f, 0.9f, 0.1f, 0.55f);

    // Outline colours
    private static final Color HB_PLAYER_LINE = new Color(1.0f, 0.9f, 0.0f, 1.0f);
    private static final Color HB_HAZARD_LINE = new Color(1.0f, 0.2f, 0.2f, 1.0f);
    private static final Color HB_BLOCK_LINE  = new Color(0.2f, 0.5f, 1.0f, 1.0f);
    private static final Color HB_PORTAL_LINE = new Color(0.2f, 1.0f, 0.4f, 1.0f);
    private static final Color HB_ORB_LINE    = new Color(1.0f, 0.9f, 0.1f, 1.0f);

    private final GameWorld world;
    private final ShapeRenderer shape;

    public HitboxRenderer(GameWorld world, ShapeRenderer shape) {
        this.world = world;
        this.shape = shape;
    }

    /**
     * Draws filled and outlined hitboxes for all entities in the world.
     *
     * @param camera The projection camera to use.
     * @param player The current player instance.
     */
    public void draw(OrthographicCamera camera, AbstractPlayer player) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(camera.combined);

        drawFilled(player);
        drawOutlines(player);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Filled pass ───────────────────────────────────────────────────────────

    private void drawFilled(AbstractPlayer player) {
        shape.begin(ShapeRenderer.ShapeType.Filled);

        shape.setColor(HB_BLOCK_FILL);
        for (Block b : world.getBlocks()) {
            if (b instanceof Slope) drawFilledSlope((Slope) b);
            else shape.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        }

        shape.setColor(HB_PORTAL_FILL);
        for (AbstractPortal p : world.getPortals()) {
            Rectangle r = p.getBounds();
            shape.rect(r.x, r.y, r.width, r.height);
        }

        shape.setColor(HB_HAZARD_FILL);
        for (AbstractHazard h : world.getHazards()) {
            if (h.getType() == AbstractHazard.HazardType.SPIKE) {
                Rectangle r = ((Spike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else if (h.getType() == AbstractHazard.HazardType.HALF_SPIKE) {
                Rectangle r = ((HalfSpike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else {
                shape.rect(h.getX(), h.getY(), h.getWidth(), h.getHeight());
            }
        }

        shape.setColor(HB_ORB_FILL);
        int cullStart = world.getOrbCull();
        for (int i = cullStart; i < world.getOrbs().size; i++) {
            AbstractOrb orb = world.getOrbs().get(i);
            Rectangle r = orb.getBounds();
            shape.circle(r.x + r.width / 2f, r.y + r.height / 2f, r.width / 2f, 24);
        }

        shape.setColor(HB_PLAYER_FILL);
        Rectangle pb = player.getBounds();
        shape.rect(pb.x, pb.y, pb.width, pb.height);

        shape.setColor(1.0f, 1.0f, 1.0f, 0.5f);
        float radius = player.width * 0.5f * Slope.CIRCLE_RATIO;
        shape.circle(pb.x + pb.width * 0.5f, pb.y + pb.height * 0.5f, radius);

        shape.end();
    }

    private void drawFilledSlope(Slope s) {
        float rot = ((int) s.getRotation() % 360 + 360) % 360;
        float x = s.getX(), y = s.getY(), w = s.getWidth(), h = s.getHeight();
        if      (rot == 0)   shape.triangle(x,     y,     x + w, y,     x + w, y + h);
        else if (rot == 90)  shape.triangle(x,     y + h, x + w, y + h, x + w, y    );
        else if (rot == 180) shape.triangle(x,     y,     x,     y + h, x + w, y + h);
        else if (rot == 270) shape.triangle(x,     y,     x,     y + h, x + w, y    );
        else                 shape.rect(x, y, w, h);
    }

    // ── Outline pass ──────────────────────────────────────────────────────────

    private void drawOutlines(AbstractPlayer player) {
        shape.begin(ShapeRenderer.ShapeType.Line);

        shape.setColor(HB_BLOCK_LINE);
        for (Block b : world.getBlocks()) {
            if (b instanceof Slope) drawOutlineSlope((Slope) b);
            else shape.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        }

        shape.setColor(HB_PORTAL_LINE);
        for (AbstractPortal p : world.getPortals()) {
            Rectangle r = p.getBounds();
            shape.rect(r.x, r.y, r.width, r.height);
        }

        shape.setColor(HB_HAZARD_LINE);
        for (AbstractHazard h : world.getHazards()) {
            if (h.getType() == AbstractHazard.HazardType.SPIKE) {
                Rectangle r = ((Spike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else if (h.getType() == AbstractHazard.HazardType.HALF_SPIKE) {
                Rectangle r = ((HalfSpike) h).getHitbox();
                shape.rect(r.x, r.y, r.width, r.height);
            } else if (h.getType() == AbstractHazard.HazardType.SAW_BLADE) {
                SawBlade saw = (SawBlade) h;
                shape.circle(
                    saw.getX() + saw.getDiameter() / 2f,
                    saw.getY() + saw.getDiameter() / 2f,
                    saw.getDiameter() / 2f, 32);
            }
        }

        shape.setColor(HB_ORB_LINE);
        int cullStart = world.getOrbCull();
        for (int i = cullStart; i < world.getOrbs().size; i++) {
            AbstractOrb orb = world.getOrbs().get(i);
            Rectangle r = orb.getBounds();
            shape.circle(r.x + r.width / 2f, r.y + r.height / 2f, r.width / 2f, 24);
        }

        shape.setColor(HB_PLAYER_LINE);
        Rectangle pb = player.getBounds();
        shape.rect(pb.x, pb.y, pb.width, pb.height);

        shape.setColor(1.0f, 1.0f, 1.0f, 0.8f);
        float radius = player.width * 0.5f * Slope.CIRCLE_RATIO;
        shape.circle(pb.x + pb.width * 0.5f, pb.y + pb.height * 0.5f, radius);

        shape.end();
    }

    private void drawOutlineSlope(Slope s) {
        float rot = ((int) s.getRotation() % 360 + 360) % 360;
        float x = s.getX(), y = s.getY(), w = s.getWidth(), h = s.getHeight();
        float[] line = s.getSlopeLine();
        float solidCX, solidCY;
        if      (rot == 0)   { solidCX = x + w; solidCY = y;     }
        else if (rot == 90)  { solidCX = x + w; solidCY = y + h; }
        else if (rot == 180) { solidCX = x;     solidCY = y + h; }
        else                 { solidCX = x;     solidCY = y;     }
        shape.triangle(line[0], line[1], line[2], line[3], solidCX, solidCY);
    }
}
