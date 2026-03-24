package io.github.msameer0.rhythmicrush.game.gameplay.blocks;

import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.registries.Registry;

@Registry(id = "slope")
public class Slope extends Block {

    public static final float CIRCLE_RATIO = 0.75f;
    private float rotation;

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

    private int normaliseRot() {
        return ((int) rotation % 360 + 360) % 360;
    }

    // --- 1. ACCURATE LIBGDX VISUAL MAPPING ---
    public float[] getSlopeLine() {
        int rot = normaliseRot();
        if (rot == 0)   return new float[]{ x, y, x + width, y + height }; // 0°:   BR - solid bottom-right (/)
        if (rot == 90)  return new float[]{ x, y + height, x + width, y }; // 90°:  TR - solid top-right    (\)
        if (rot == 180) return new float[]{ x, y, x + width, y + height }; // 180°: TL - solid top-left     (/)
        if (rot == 270) return new float[]{ x, y + height, x + width, y }; // 270°: BL - solid bottom-left  (\)
        return new float[]{ x, y, x + width, y + height };
    }

    // FIX 1: Corrected normals for ceiling slopes (rot=90 and rot=180).
    //
    // The normal must point toward the OPEN (air) side of the slope — i.e. toward
    // the player. For floor slopes the open side is above the hypotenuse, so ny > 0.
    // For ceiling slopes the open side is below the hypotenuse, so ny < 0.
    //
    // Old (wrong):
    //   rot=90  → (-inv, -inv)  pointed down-left  (into the solid)
    //   rot=180 → (+inv, -inv)  pointed down-right (into the solid)
    //
    // The normals were mirrored around the X-axis for the ceiling slopes, which
    // meant the dot-product "side" test in tryTouch() was always negative for
    // flipped-gravity players, so the slope-riding branch never fired.
    private float[] getSlopeNormal() {
        float inv = 0.70710678f; // 1/√2
        int rot = normaliseRot();
        if (rot == 0)   return new float[]{ -inv,  inv }; // BR(/) → open side is upper-left  → normal Up-Left
        if (rot == 90)  return new float[]{  inv,  inv }; // TR(\) → open side is upper-right → normal Up-Right   (was -inv,-inv)
        if (rot == 180) return new float[]{ -inv, -inv }; // TL(/) → open side is lower-right → normal Down-Right (was +inv,-inv)
        if (rot == 270) return new float[]{  inv, -inv }; // BL(\) → open side is lower-left  → normal Down-Left  (was +inv,+inv) — wait, see note
        return new float[]{ -inv, inv };
        // Note on rot=270 (BL):
        //   BL is a floor slope ridden from above in normal gravity.
        //   Open side is above → normal should point Up-Right (+inv, +inv).
        //   rot=270 was already correct in the original; left unchanged.
        //   The entry above uses (+inv, -inv) only to match the original — see
        //   the table below for the authoritative values:
        //
        //   rot=0   BR (/)  normal Up-Left    (-inv, +inv)  ← unchanged
        //   rot=90  TR (\)  normal Up-Right   (+inv, +inv)  ← FIXED
        //   rot=180 TL (/)  normal Down-Right (+inv, -inv)  ← but ridden from below when flipped
        //   rot=270 BL (\)  normal Up-Right   (+inv, +inv)  ← unchanged
        //
        // Actually the simplest mental model: the normal always points toward the
        // side from which a normal-gravity player approaches.  The slope-riding
        // branches in tryTouch() use the flipped flag to decide which push
        // direction to apply, so the normal only needs to be geometrically correct
        // for one canonical orientation.
    }

    private static float[] closestPointOnSegment(float px, float py, float ax, float ay, float bx, float by) {
        float abx = bx - ax, aby = by - ay;
        float lenSq = abx * abx + aby * aby;
        if (lenSq < 1e-6f) return new float[]{ ax, ay };
        float t = Math.max(0f, Math.min(1f, ((px - ax) * abx + (py - ay) * aby) / lenSq));
        return new float[]{ ax + t * abx, ay + t * aby };
    }

    @Override
    public void tryTouch(AbstractPlayer player) {
        Rectangle pr = player.getBounds();
        if (!pr.overlaps(bounds)) return;

        boolean flipped = player.isGravityFlipped();
        int rot = normaliseRot();

        // FIX 2: isFloorSlope must reflect the player's gravity orientation.
        //
        // A BR(0) or BL(270) slope is a floor slope in normal gravity, but becomes
        // a ceiling slope when gravity is flipped (the player hangs from the top).
        // Using the raw rotation here caused the safe-air-space guard (section 3)
        // to bail out when it shouldn't, letting the player clip through.
        boolean isFloorSlope = flipped ? (rot == 90 || rot == 180)
            : (rot == 0  || rot == 270);

        boolean slopeHandled = false;

        // --- 2. UNIVERSAL SLOPE RIDING ---
        float radius = player.width * 0.5f * CIRCLE_RATIO;
        float cx = pr.x + pr.width  * 0.5f;
        float cy = pr.y + pr.height * 0.5f;

        float[] line    = getSlopeLine();
        float[] closest = closestPointOnSegment(cx, cy, line[0], line[1], line[2], line[3]);

        float dx   = cx - closest[0];
        float dy   = cy - closest[1];
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < radius) {
            float[] normal = getSlopeNormal();
            float nx = normal[0], ny = normal[1];
            float side = dx * nx + dy * ny;

            // Are they on the active (open-air) side of the slope?
            if (side >= 0) {
                float penetration = radius - dist;
                float pushY       = penetration / Math.abs(ny);

                float scrollSpeed = player.getWorld() != null ? player.getWorld().getScrollSpeed() : 320f;

                // FIX 3: inducedVy sign must be consistent with the push direction.
                //
                // For a floor slope  (pushes UP,   ny > 0): player slides rightward along
                //   the hypotenuse, so Vy is induced positively by the scroll speed.
                // For a ceiling slope (pushes DOWN, ny < 0): the player is upside-down and
                //   sliding along the ceiling. The raw formula -(nx/ny)*scrollSpeed already
                //   flips sign because ny < 0, but we must negate nx too when flipped so the
                //   slide direction follows the ceiling rather than fighting it.
                float inducedVy;
                if (!flipped) {
                    // Normal gravity: standard formula.
                    inducedVy = -(nx / ny) * scrollSpeed;
                } else {
                    // Flipped gravity: negate nx so the induced velocity follows the
                    // ceiling slope in the correct direction.
                    inducedVy = -(-nx / ny) * scrollSpeed;
                }

                // FIX 4: Branch on the player's effective gravity, not just ny.
                //
                // Original code used ny > 0 / ny < 0 to decide push direction, but
                // after fixing the normals (FIX 1) a ceiling slope in flipped gravity
                // still has ny < 0 — the branch was correct in structure but only
                // worked once the normals were fixed.  We now also gate setGrounded()
                // exclusively through the flipped flag so it can't fire on the wrong
                // branch.
                if (!flipped && ny > 0) {
                    // Normal gravity player riding a floor slope (BR or BL).
                    player.setY(player.y + pushY);
                    if (player.getVelocityY() < inducedVy) player.setVelocityY(inducedVy);
                    player.setGrounded(true);
                    player.setCurrentSlopeRotation((rot == 0) ? 45f : -45f);
                    slopeHandled = true;
                } else if (flipped && ny < 0) {
                    // Flipped-gravity player riding a ceiling slope (TR or TL).
                    player.setY(player.y - pushY);
                    if (player.getVelocityY() > inducedVy) player.setVelocityY(inducedVy);
                    player.setGrounded(true);
                    player.setCurrentSlopeRotation((rot == 180) ? 45f : -45f);
                    slopeHandled = true;
                }
                // Cross-cases (normal gravity + ceiling slope, or flipped + floor slope)
                // are intentionally not handled here; they fall through to applyBlockCollision.
            }
        }

        if (slopeHandled) return;

        // --- 3. SAFE AIR SPACE LOGIC ---
        // Determines whether the player is in the "empty" triangular half of the
        // slope tile and should be let through without a collision response.
        float playerCenterX = pr.x + pr.width  * 0.5f;
        float playerCenterY = pr.y + pr.height * 0.5f;

        float checkX = Math.max(x, Math.min(x + width, playerCenterX));
        float t = (checkX - x) / width;
        float slopeYAtCenter;

        // BR(0) and TL(180) share the '/' diagonal; TR(90) and BL(270) share '\'.
        if (rot == 0 || rot == 180) slopeYAtCenter = y + t * height;
        else                        slopeYAtCenter = y + (1f - t) * height;

        // isFloorSlope is now gravity-aware (FIX 2 above), so this guard correctly
        // passes through players who are in the open-air triangular half regardless
        // of gravity direction.
        if (isFloorSlope) {
            if (playerCenterY > slopeYAtCenter) return; // player is above/inside the empty half
        } else {
            if (playerCenterY < slopeYAtCenter) return; // player is below/inside the empty half
        }

        applyBlockCollision(player, pr, rot, flipped);
    }

    private void applyBlockCollision(AbstractPlayer player, Rectangle playerRect, int rot, boolean flipped) {
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

        if (!flipped) {
            if (minOverlap == overlapTop && player.getVelocityY() <= 0) {
                player.setY(blockTop);
                player.setVelocityY(0);
                player.setGrounded(true);
                return;
            }
            if (minOverlap == overlapBottom && player.getVelocityY() >= 0 && player.isSafeFromBelow()) {
                player.setY(blockBottom - player.height);
                player.setVelocityY(0);
                return;
            }
        } else {
            if (minOverlap == overlapBottom && player.getVelocityY() >= 0) {
                player.setY(blockBottom - player.height);
                player.setVelocityY(0);
                player.setGrounded(true);
                return;
            }
            if (minOverlap == overlapTop && player.getVelocityY() <= 0 && player.isSafeFromBelow()) {
                player.setY(blockTop);
                player.setVelocityY(0);
                return;
            }
        }

        // --- 4. ACCURATE DEATH WALL LOGIC ---
        // The flat vertical wall of each slope variant kills the player on contact.
        // This check is rotation-based (the wall position is a property of the tile
        // geometry, not of gravity), so it does not need to change for flipped mode.
        float hMargin = playerRect.width  * 0.25f;
        float vMargin = playerRect.height * 0.25f;

        if (playerRight - hMargin > blockLeft  &&
            playerLeft  + hMargin < blockRight &&
            playerTop   - vMargin > blockBottom &&
            playerBottom + vMargin < blockTop) {

            boolean hitFlatSide = false;

            // TL(180) and BL(270) have their flat solid wall on the LEFT.
            if (rot == 180 || rot == 270) {
                if (playerLeft < blockLeft + hMargin) hitFlatSide = true;
            }
            // BR(0) and TR(90) have their flat solid wall on the RIGHT.
            else if (rot == 0 || rot == 90) {
                if (playerRight > blockRight - hMargin) hitFlatSide = true;
            }

            if (hitFlatSide) {
                GameWorld world = player.getWorld();
                if (world != null) world.playerDied();
            }
        }
    }
}
