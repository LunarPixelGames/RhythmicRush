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
        return (Math.round(rotation / 90f) * 90 % 360 + 360) % 360;
    }

    public float[] getSlopeLine() {
        int rot = normaliseRot();
        if (rot == 0 || rot == 180) {
            return new float[]{ x, y, x + width, y + height };
        } else {
            return new float[]{ x, y + height, x + width, y };
        }
    }

    @Override
    public void tryTouch(AbstractPlayer player) {
        Rectangle pr = player.getBounds();
        if (!pr.overlaps(bounds)) return;

        boolean flipped = player.isGravityFlipped();
        int rot = normaliseRot();
        float scrollSpeed = player.getWorld() != null ? player.getWorld().getScrollSpeed() : 320f;

        float pL = pr.x, pR = pr.x + pr.width;
        float pB = pr.y, pT = pr.y + pr.height;
        float bL = bounds.x, bR = bounds.x + bounds.width;
        float bB = bounds.y, bT = bounds.y + bounds.height;
        float ratio = bounds.height / bounds.width;

        float overlapSlope = 0;
        float overlapFlatX = 0;
        float overlapFlatY = 0;
        float slopeY = 0;
        boolean resolveSlopeUp = true;
        int flatXType = 0; // 1 = Left, 2 = Right
        int flatYType = 0; // 3 = Bottom, 4 = Top

        // --- 1. CALCULATE EXACT SAT OVERLAPS ---
        if (rot == 0) { // Solid BR (Bottom-Right)
            float checkX = Math.max(bL, Math.min(bR, pR));
            slopeY = bB + (checkX - bL) * ratio;
            overlapSlope = slopeY - pB;
            resolveSlopeUp = true;
            overlapFlatX = bR - pL; flatXType = 2; // Right flat wall
            overlapFlatY = pT - bB; flatYType = 3; // Bottom flat wall
        } else if (rot == 90) { // Solid TR (Top-Right)
            float checkX = Math.max(bL, Math.min(bR, pR));
            slopeY = bT - (checkX - bL) * ratio;
            overlapSlope = pT - slopeY;
            resolveSlopeUp = false;
            overlapFlatX = bR - pL; flatXType = 2; // Right flat wall
            overlapFlatY = bT - pB; flatYType = 4; // Top flat wall
        } else if (rot == 180) { // Solid TL (Top-Left)
            float checkX = Math.max(bL, Math.min(bR, pL));
            slopeY = bB + (checkX - bL) * ratio;
            overlapSlope = pT - slopeY;
            resolveSlopeUp = false;
            overlapFlatX = pR - bL; flatXType = 1; // Left flat wall
            overlapFlatY = bT - pB; flatYType = 4; // Top flat wall
        } else if (rot == 270) { // Solid BL (Bottom-Left)
            float checkX = Math.max(bL, Math.min(bR, pL));
            slopeY = bT - (checkX - bL) * ratio;
            overlapSlope = slopeY - pB;
            resolveSlopeUp = true;
            overlapFlatX = pR - bL; flatXType = 1; // Left flat wall
            overlapFlatY = pT - bB; flatYType = 3; // Bottom flat wall
        }

        // If completely in the empty half of the block, ignore collision completely!
        if (overlapSlope <= 0) return;

        // --- 2. DETERMINE WHICH FACE WAS HIT ---
        float minOverlap = Math.min(overlapSlope, Math.min(overlapFlatX, overlapFlatY));

        // --- 3. RESOLVE COLLISION ---
        if (minOverlap == overlapFlatX) {
            // Hit the flat vertical wall -> Death
            kill(player);
            return;
        }
        else if (minOverlap == overlapFlatY) {
            // Hit the flat horizontal wall -> Land or Bonk
            if (flatYType == 3 && player.getVelocityY() >= 0) { // Bottom wall
                player.setY(bB - pr.height);
                player.setVelocityY(0);
                if (flipped) player.setGrounded(true);
            } else if (flatYType == 4 && player.getVelocityY() <= 0) { // Top wall
                player.setY(bT);
                player.setVelocityY(0);
                if (!flipped) player.setGrounded(true);
            }
            return;
        }

        // --- 4. RIDE THE DIAGONAL SLOPE ---
        if (resolveSlopeUp) player.setY(slopeY);
        else                player.setY(slopeY - pr.height);

        boolean isFloorSlope = (rot == 0 || rot == 270);

        if (!flipped && isFloorSlope) {
            player.setGrounded(true);
            player.setCurrentSlopeRotation(rot == 0 ? 45f : -45f);
            float inducedVy = scrollSpeed * ratio * (rot == 0 ? 1 : -1);
            if (rot == 0 && player.getVelocityY() < inducedVy) player.setVelocityY(inducedVy);
            else if (rot == 270) player.setVelocityY(inducedVy);
        }
        else if (flipped && !isFloorSlope) {
            player.setGrounded(true);
            player.setCurrentSlopeRotation(rot == 180 ? 45f : -45f);
            float inducedVy = scrollSpeed * ratio * (rot == 180 ? 1 : -1);
            if (rot == 180 && player.getVelocityY() < inducedVy) player.setVelocityY(inducedVy);
            else if (rot == 90) player.setVelocityY(inducedVy);
        }
        else {
            // Cross cases (e.g., normal gravity player bouncing their head off a ceiling slope)
            if (resolveSlopeUp && player.getVelocityY() < 0) player.setVelocityY(0);
            else if (!resolveSlopeUp && player.getVelocityY() > 0) player.setVelocityY(0);
        }
    }

    private void kill(AbstractPlayer player) {
        GameWorld world = player.getWorld();
        if (world != null) world.playerDied();
    }
}
