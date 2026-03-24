package io.github.msameer0.rhythmicrush.game.gameplay.blocks;

import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * A 45-degree slope block with GD-style inner circle hitbox for smooth slope riding.
 *
 * <p>Like the original GD, the player has two effective hitboxes:
 * <ul>
 *   <li><b>Outer AABB</b> — used for wall/ceiling/floor death on non-slope faces</li>
 *   <li><b>Inner circle</b> — used for slope surface contact, inscribed inside
 *       the player square. Makes slopes feel smooth — the circle rolls onto the
 *       slope rather than the flat bottom snapping to it.</li>
 * </ul>
 *
 * <p>Supported rotations:
 * <pre>
 *   rotation=0   (/)  floor slope, rising left→right
 *   rotation=90  (\)  floor slope, falling left→right
 *   rotation=180 (\)  ceiling slope (gravity-flipped)
 *   rotation=270 (/)  ceiling slope (gravity-flipped)
 * </pre>
 *
 * <p>The active slope surface never kills. All other faces (wall, flat bottom/top)
 * behave identically to a regular block and will kill the player on side impact.</p>
 */
@Registry(id = "slope")
public class Slope extends Block {

    /**
     * Ratio of the inner circle radius to the player's half-width.
     * 0.75 matches approximately what GD uses.
     */
    private static final float CIRCLE_RATIO = 0.75f;

    private float rotation;

    // ── Constructors / pooling ────────────────────────────────────────────────

    public Slope() { super(); }

    public Slope(float x, float y, float size, float rotation) {
        super(x, y, size, BlockType.DEFAULT);
        this.rotation = rotation;
    }

    public Slope init(float x, float y, float size, BlockType type, float rotation) {
        super.init(x, y, size, type);
        this.rotation = rotation;
        return this;
    }

    public float getRotation() { return rotation; }

    // ── Slope geometry ────────────────────────────────────────────────────────

    /** Returns [x1,y1, x2,y2] endpoints of the active slope surface line. */
    private float[] getSlopeLine() {
        int rot = normaliseRot();
        switch (rot) {
            case 0:   return new float[]{ x,         y,          x + width, y + height }; // /
            case 90:  return new float[]{ x,         y + height, x + width, y          }; // \
            case 180: return new float[]{ x,         y + height, x + width, y          }; // \ ceiling
            case 270: return new float[]{ x,         y,          x + width, y + height }; // / ceiling
            default:  return new float[]{ x,         y,          x + width, y + height };
        }
    }

    /**
     * Returns the outward surface normal [nx, ny] pointing away from the solid body.
     * For floor slopes this points generally upward; for ceiling slopes downward.
     */
    private float[] getSlopeNormal(boolean flipped) {
        float inv = 0.70710678f; // 1/√2
        int rot = normaliseRot();
        if (!flipped) {
            if (rot == 0)  return new float[]{ -inv,  inv }; // / floor normal: up-left
            if (rot == 90) return new float[]{  inv,  inv }; // \ floor normal: up-right
        } else {
            if (rot == 180) return new float[]{ -inv, -inv }; // \ ceiling normal: down-left
            if (rot == 270) return new float[]{  inv, -inv }; // / ceiling normal: down-right
        }
        return new float[]{ 0f, flipped ? -1f : 1f };
    }

    /**
     * Returns whether a given world-X position is within the "slope zone" —
     * the horizontal span where the active sloped surface actually exists.
     * Outside this zone the block behaves as a solid wall.
     */
    private boolean inSlopeZone(float worldX) {
        return worldX >= x && worldX <= x + width;
    }

    private int normaliseRot() {
        return ((int) rotation % 360 + 360) % 360;
    }

    // ── Circle-vs-segment math ────────────────────────────────────────────────

    private static float[] closestPointOnSegment(
        float px, float py,
        float ax, float ay, float bx, float by) {
        float abx = bx - ax, aby = by - ay;
        float lenSq = abx * abx + aby * aby;
        if (lenSq < 1e-6f) return new float[]{ ax, ay };
        float t = Math.max(0f, Math.min(1f,
            ((px - ax) * abx + (py - ay) * aby) / lenSq));
        return new float[]{ ax + t * abx, ay + t * aby };
    }

    // ── tryTouch ──────────────────────────────────────────────────────────────

    @Override
    public void tryTouch(AbstractPlayer player) {
        Rectangle pr = player.getBounds();
        if (!pr.overlaps(bounds)) return;

        boolean flipped = player.isGravityFlipped();
        int rot = normaliseRot();

        boolean isFloorSlope   = (rot == 0 || rot == 90);
        boolean isCeilingSlope = (rot == 180 || rot == 270);

        // ── 1. Try slope surface contact (inner circle) ───────────────────────
        boolean slopeHandled = false;

        if ((!flipped && isFloorSlope) || (flipped && isCeilingSlope)) {
            float radius = player.width * 0.5f * CIRCLE_RATIO;
            float cx = pr.x + pr.width  * 0.5f;
            float cy = pr.y + pr.height * 0.5f;

            float[] line    = getSlopeLine();
            float[] closest = closestPointOnSegment(cx, cy,
                line[0], line[1], line[2], line[3]);

            float dx   = cx - closest[0];
            float dy   = cy - closest[1];
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < radius) {
                float[] normal = getSlopeNormal(flipped);
                float nx = normal[0], ny = normal[1];
                float side = dx * nx + dy * ny;

                // Only push if the circle is on the correct (exterior) side
                if (side >= 0) {
                    float penetration = radius - dist;
                    // For a 45-degree slope, vertical push needed is penetration / sin(45)
                    float pushY = penetration / Math.abs(ny);

                    // Calculate the induced vertical velocity from moving along the slope
                    // Vy / Vx = -nx / ny (for a 45-degree slope, this is either 1 or -1)
                    float scrollSpeed = player.getWorld() != null ? player.getWorld().getScrollSpeed() : 320f;
                    float inducedVy = -(nx / ny) * scrollSpeed;

                    if (!flipped && ny > 0) {
                        player.setY(player.y + pushY);
                        // Maintain the slope's vertical velocity so they "fly off" the edge
                        if (player.getVelocityY() < inducedVy) player.setVelocityY(inducedVy);
                        player.setGrounded(true);
                        slopeHandled = true;
                    } else if (flipped && ny < 0) {
                        player.setY(player.y - pushY);
                        // For flipped gravity, they are on the ceiling — inducedVy will be negative for a "falling" slope
                        if (player.getVelocityY() > inducedVy) player.setVelocityY(inducedVy);
                        player.setGrounded(true);
                        slopeHandled = true;
                    }
                }
            }
        }

        if (slopeHandled) return;

        // ── 2. Non-slope face: behave like a regular block ────────────────────
        // We get here when the player overlaps the AABB but the circle didn't
        // touch the slope surface — meaning they hit the wall face or flat face.
        //
        // The "safe" region is the triangle under/over the slope line.
        // We check if the player center is in the air part of the block's AABB.

        float playerCenterX = pr.x + pr.width  * 0.5f;
        float playerCenterY = pr.y + pr.height * 0.5f;

        // Clamp center X to slope zone to find the vertical boundary at the closest point
        float checkX = Math.max(x, Math.min(x + width, playerCenterX));
        float t = (checkX - x) / width;
        float slopeYAtCenter;

        if (rot == 0 || rot == 270) slopeYAtCenter = y + t * height;         // /
        else                        slopeYAtCenter = y + (1f - t) * height; // \

        if (rot == 0 || rot == 90) { // Floor slopes
            if (playerCenterY > slopeYAtCenter) return; // Safe in air above floor slope
        } else { // Ceiling slopes
            if (playerCenterY < slopeYAtCenter) return; // Safe in air below ceiling slope
        }

        // Apply standard block collision (wall / flat face)
        applyBlockCollision(player, pr);
    }

    /**
     * Standard AABB collision resolution — identical to {@link Block#tryTouch}
     * but without the death on side impact replaced by playerDied.
     * Wall/side hits on slopes always kill just like regular blocks.
     */
    private void applyBlockCollision(AbstractPlayer player, Rectangle playerRect) {
        float playerBottom = playerRect.y;
        float playerTop    = playerRect.y + playerRect.height;
        float playerLeft   = playerRect.x;
        float playerRight  = playerRect.x + playerRect.width;

        float blockTop    = bounds.y + bounds.height;
        float blockBottom = bounds.y;
        float blockLeft   = bounds.x;
        float blockRight  = bounds.x + bounds.width;

        float overlapTop    = blockTop    - playerBottom;
        float overlapBottom = playerTop   - blockBottom;
        float overlapLeft   = playerRight - blockLeft;
        float overlapRight  = blockRight  - playerLeft;

        float minOverlap = overlapTop;
        if (overlapBottom < minOverlap) minOverlap = overlapBottom;
        if (overlapLeft   < minOverlap) minOverlap = overlapLeft;
        if (overlapRight  < minOverlap) minOverlap = overlapRight;

        boolean flipped = player.isGravityFlipped();

        if (!flipped) {
            if (minOverlap == overlapTop && player.velocityY <= 0) {
                player.setY(blockTop);
                player.setVelocityY(0);
                player.setGrounded(true);
                return;
            }
            if (minOverlap == overlapBottom && player.velocityY >= 0 && player.isSafeFromBelow()) {
                player.setY(blockBottom - player.height);
                player.setVelocityY(0);
                return;
            }
        } else {
            if (minOverlap == overlapBottom && player.velocityY >= 0) {
                player.setY(blockBottom - player.height);
                player.setVelocityY(0);
                player.setGrounded(true);
                return;
            }
            if (minOverlap == overlapTop && player.velocityY <= 0 && player.isSafeFromBelow()) {
                player.setY(blockTop);
                player.setVelocityY(0);
                return;
            }
        }

        // Side impact — kill the player
        float hMargin = playerRect.width  * 0.25f;
        float vMargin = playerRect.height * 0.25f;
        if (playerRight  - hMargin > blockLeft  &&
            playerLeft   + hMargin < blockRight &&
            playerTop    - vMargin > blockBottom &&
            playerBottom + vMargin < blockTop) {
            GameWorld world = player.getWorld();
            if (world != null) world.playerDied();
        }
    }
}
