package io.github.msameer0.rhythmicrush.game.gameplay.blocks;

import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * Represents a physical platform or obstacle within the game world.
 * <p>
 * The {@code Block} class manages its own position, dimensions, and collision bounds.
 * it provides logic for horizontal movement (scrolling) and complex collision
 * detection with {@link AbstractPlayer} instances, including landing on top,
 * bumping into the bottom, and triggering death upon side impacts.
 * </p>
 * <p>
 * This class is designed to be compatible with object pooling to optimize memory
 * usage during gameplay.
 * </p>
 */
public class Block {

    protected float x, y;
    protected float width, height;
    protected Rectangle bounds;
    protected BlockType type;

    /**
     * Constructs a new empty Block.
     * <p>
     * This no-arg constructor is primarily intended for object pooling.
     * The {@link #init(float, float, float, BlockType)} method must be called
     * before the block can be used in the game world.
     * </p>
     */
    public Block() {
        bounds = new Rectangle();
    }

    /**
     * Constructs a new Block with the specified position and size using the default block type.
     *
     * @param x    the x-coordinate of the block
     * @param y    the y-coordinate of the block
     * @param size the width and height of the block
     */
    public Block(float x, float y, float size) {
        this(x, y, size, BlockType.DEFAULT);
    }

    /**
     * Constructs a new Block with the specified position, size, and type.
     *
     * @param x    the x-coordinate of the block
     * @param y    the y-coordinate of the block
     * @param size the width and height of the block
     * @param type the type of the block defining its appearance or behavior
     */
    public Block(float x, float y, float size, BlockType type) {
        this.x = x;
        this.y = y;
        this.width = size;
        this.height = size;
        this.type = type;
        bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Reinitialise this block for reuse from the pool.
     */
    public Block init(float x, float y, float size, BlockType type) {
        this.x = x;
        this.y = y;
        this.width = size;
        this.height = size;
        this.type = type;
        bounds.set(x, y, width, height);
        return this;
    }

    public void updatePosition(float scrollSpeed, float delta) {
        x -= scrollSpeed * delta;
        updateBounds();
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void updateBounds() {
        bounds.setPosition(x, y);
    }

    /**
     * Handles collision detection and response between the player and this block.
     * <p>
     * This method calculates the overlap between the player's bounding box and the block's
     * bounds to determine the direction of impact. It supports the following behaviors:
     * <ul>
     *     <li><b>Landing:</b> If the player hits the top of the block while falling, they are
     *     snapped to the surface and marked as grounded.</li>
     */
    public void tryTouch(AbstractPlayer player) {
        Rectangle playerRect = player.getBounds();
        if (!playerRect.overlaps(bounds)) return;

        float playerBottom = playerRect.y;
        float playerTop = playerRect.y + playerRect.height;
        float playerLeft = playerRect.x;
        float playerRight = playerRect.x + playerRect.width;

        float blockTop = bounds.y + bounds.height;
        float blockBottom = bounds.y;
        float blockLeft = bounds.x;
        float blockRight = bounds.x + bounds.width;

        float overlapTop = blockTop - playerBottom;
        float overlapBottom = playerTop - blockBottom;
        float overlapLeft = playerRight - blockLeft;
        float overlapRight = blockRight - playerLeft;

        float minOverlap = overlapTop;
        if (overlapBottom < minOverlap) minOverlap = overlapBottom;
        if (overlapLeft < minOverlap) minOverlap = overlapLeft;
        if (overlapRight < minOverlap) minOverlap = overlapRight;

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

        float hMargin = playerRect.width * 0.25f;
        float vMargin = playerRect.height * 0.25f;

        if (playerRight - hMargin > blockLeft && playerLeft + hMargin < blockRight &&
            playerTop - vMargin > blockBottom && playerBottom + vMargin < blockTop) {
            GameWorld world = player.getWorld();
            if (world != null) world.playerDied();
        }
    }

    /**
     * Gets the x-coordinate of the block.
     *
     * @return the x-coordinate
     */
    public float getX() {
        return x;
    }

    /**
     * Returns the y-coordinate of the block.
     *
     * @return the y-coordinate of the block
     */
    public float getY() {
        return y;
    }

    /**
     * Returns the width of the block.
     *
     * @return the width of the block
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return the height of the block
     */
    public float getHeight() {
        return height;
    }

    /**
     * Gets the type of this block, which defines its appearance or behavior.
     *
     * @return the {@link BlockType} of the block
     */
    public BlockType getType() {
        return type;
    }
}
