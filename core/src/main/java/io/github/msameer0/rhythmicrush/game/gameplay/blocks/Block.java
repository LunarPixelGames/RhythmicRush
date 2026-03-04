package io.github.msameer0.rhythmicrush.game.gameplay.blocks;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

public class Block {

    protected float x, y;
    protected float width, height;
    protected Rectangle bounds;
    protected BlockType type;

    public Block(float x, float y, float size) {
        this(x, y, size, BlockType.DEFAULT);
    }

    public Block(float x, float y, float size, BlockType type) {
        this.x = x;
        this.y = y;
        this.width = size;
        this.height = size;
        this.type = type;
        bounds = new Rectangle(x, y, width, height);
    }

    public void updatePosition(float scrollSpeed, float delta) {
        x -= scrollSpeed * delta;
        updateBounds();
    }

    public Rectangle getBounds() { return bounds; }

    public void updateBounds() {
        bounds.setPosition(x, y);
    }

    //player collision logic
    //safe to land on top, kills otherwise
    public void tryTouch(AbstractPlayer player) {
        Rectangle playerRect = player.getBounds();
        if (!playerRect.overlaps(bounds)) return;

        float playerBottom = playerRect.y;
        float playerTop    = playerRect.y + playerRect.height;
        float playerLeft   = playerRect.x;
        float playerRight  = playerRect.x + playerRect.width;

        float blockTop    = bounds.y + bounds.height;
        float blockBottom = bounds.y;
        float blockLeft   = bounds.x;
        float blockRight  = bounds.x + bounds.width;

        float overlapFromTop    = playerTop    - blockBottom;
        float overlapFromBottom = blockTop     - playerBottom;
        float overlapFromLeft   = playerRight  - blockLeft;
        float overlapFromRight  = blockRight   - playerLeft;

        //collision side check
        float minOverlap = Math.min(Math.min(overlapFromTop, overlapFromBottom),
            Math.min(overlapFromLeft, overlapFromRight));

        //safe landing on top
        if (minOverlap == overlapFromBottom && player.velocityY <= 0) {
            player.setY(blockTop);
            player.setVelocityY(0);
            player.setGrounded(true);
            return;
        }

        //safe safe underside check
        if (minOverlap == overlapFromTop && player.velocityY >= 0 && player.isSafeFromBelow()) {
            player.setY(blockBottom - player.height);
            player.setVelocityY(0);
            return;
        }

        //death check for under and sides
        //50% hitbox, 25% margin on each axis
        float hMargin = playerRect.width  * 0.25f;
        float vMargin = playerRect.height * 0.25f;

        float innerLeft   = playerLeft   + hMargin;
        float innerRight  = playerRight  - hMargin;
        float innerBottom = playerBottom + vMargin;
        float innerTop    = playerTop    - vMargin;

        boolean innerOverlapsH = innerRight  > blockLeft && innerLeft   < blockRight;
        boolean innerOverlapsV = innerTop    > blockBottom && innerBottom < blockTop;

        if (innerOverlapsH && innerOverlapsV) {
            GameWorld world = player.getWorld();
            if (world != null) {
                world.playerDied();
            }
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public BlockType getType() { return type; }
}
