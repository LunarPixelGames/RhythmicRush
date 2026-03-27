package io.github.msameer0.rhythmicrush.game.gameplay.blocks;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.registries.Registry;

@Registry(id = "slope")
public class Slope extends Block {

    public static final float CIRCLE_RATIO = 0.8f;
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

    @Override
    public void reset() {
        super.reset();
        this.rotation = 0f;
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

        float r  = (pr.width * 0.5f) * CIRCLE_RATIO;
        float cx = pr.x + pr.width  * 0.5f;
        float cy = pr.y + pr.height * 0.5f;

        float bL = bounds.x, bR = bounds.x + bounds.width;
        float bB = bounds.y, bT = bounds.y + bounds.height;
        float bW = bounds.width, bH = bounds.height;

        float margin = r * 2.0f;
        if (cx + r < bL - margin || cx - r > bR + margin || cy + r < bB - margin || cy - r > bT + margin) return;

        boolean flipped = player.isGravityFlipped();
        int rot = normaliseRot();
        float scrollSpeed = player.getWorld() != null ? player.getWorld().getScrollSpeed() : 320f;

        boolean isFloor   = (rot == 0 || rot == 270);
        boolean isCeiling = (rot == 90 || rot == 180);

        boolean isClimbing   = (!flipped && rot == 0) || (flipped && rot == 90);
        boolean isDescending = (!flipped && rot == 270) || (flipped && rot == 180);

        boolean isShip = player.getType() == AbstractPlayer.PlayerType.SHIP;

        if (!isShip) {
            if ((!flipped && !isFloor) || (flipped && !isCeiling)) return;
        }

        float lx1, ly1, lx2, ly2, nx, ny;
        float edgeLen = (float) Math.sqrt(bW * bW + bH * bH);

        if (rot == 0) {
            lx1 = bL; ly1 = bB; lx2 = bR; ly2 = bT;
            nx = -bH / edgeLen; ny = bW / edgeLen;
        } else if (rot == 90) {
            lx1 = bR; ly1 = bB; lx2 = bL; ly2 = bT;
            nx = bH / edgeLen; ny = bW / edgeLen;
        } else if (rot == 180) {
            lx1 = bR; ly1 = bT; lx2 = bL; ly2 = bB;
            nx = bH / edgeLen; ny = -bW / edgeLen;
        } else {
            lx1 = bL; ly1 = bT; lx2 = bR; ly2 = bB;
            nx = -bH / edgeLen; ny = -bW / edgeLen;
        }

        if (isCeiling && ny > 0) {
            nx = -nx;
            ny = -ny;
        } else if (isFloor && ny < 0) {
            nx = -nx;
            ny = -ny;
        }

        float dist = (cx - lx1) * nx + (cy - ly1) * ny;

        float snapTolerance = 0f;

        if (isDescending) {
            snapTolerance = r * 0.5f;
        }
        else if (isClimbing) {
            if (player.getCurrentSlopeRotation() != 0f) {
                snapTolerance = r * 0.5f;
            }
        }

        if (dist >= r + snapTolerance) return;
        if (dist < -r) return;

        float edgeDx = lx2 - lx1;
        float edgeDy = ly2 - ly1;
        float t = ((cx - lx1) * edgeDx + (cy - ly1) * edgeDy) / (edgeLen * edgeLen);
        if (t < -0.01f || t > 1.01f) return;

        float targetVy = -(scrollSpeed * nx) / ny;

        boolean isJumpingOff = false;

        boolean isFloorForPlayer = (!flipped && isFloor) || (flipped && isCeiling);

        if (isFloorForPlayer) {
            if (!flipped && player.getVelocityY() > Math.max(0, targetVy) + 1.5f) isJumpingOff = true;
            if (flipped && player.getVelocityY() < Math.min(0, targetVy) - 1.5f) isJumpingOff = true;
        }

        if (isJumpingOff) return;

        float penetration = r - dist;

        float pushOutY = penetration / ny;

        float newCy = cy + pushOutY;
        float offsetY = cy - player.y;
        player.setY(newCy - offsetY);

        if (isFloorForPlayer) {
            player.setGrounded(true);
        }

        player.setCurrentSlopeRotation(isClimbing ? 45f : -45f);

        if (isDescending) {
            player.setVelocityY(targetVy);
        }
        else if (isClimbing) {
            if (!flipped && player.getVelocityY() <= targetVy) {
                player.setVelocityY(targetVy);
            } else if (flipped && player.getVelocityY() >= targetVy) {
                player.setVelocityY(targetVy);
            }
        }
    }
}
