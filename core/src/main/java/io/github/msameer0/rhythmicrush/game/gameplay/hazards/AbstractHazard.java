package io.github.msameer0.rhythmicrush.game.gameplay.hazards;

import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * Represents a base class for all hazards within the game.
 */
public abstract class AbstractHazard {
    public enum HazardType {
        SPIKE,
        HALF_SPIKE,
        SAW_BLADE
    }

    protected HazardType type;
    protected float x, y;
    protected float width, height;
    protected Rectangle bounds;

    /**
     * Constructs a new AbstractHazard with the specified position and dimensions.
     *
     * @param x      the initial x-coordinate of the hazard
     * @param y      the initial y-coordinate of the hazard
     * @param width  the width of the hazard
     * @param height the height of the hazard
     */
    public AbstractHazard(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Updates the horizontal position of the hazard based on the scroll speed and the time elapsed.
     * This method moves the hazard to the left and refreshes its collision boundaries.
     *
     * @param scrollSpeed the speed at which the hazard scrolls across the screen
     * @param delta the time elapsed since the last update
     */
    public void updatePosition(float scrollSpeed, float delta) {
        x -= scrollSpeed * delta;
        updateBounds();
    }

    /**
     * Gets the collision boundaries of the hazard.
     *
     * @return a {@link Rectangle} representing the current bounds of the hazard
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Synchronizes the collision boundaries with the current x and y coordinates.
     */
    protected void updateBounds() {
        bounds.setPosition(x, y);
    }

    /**
     * Gets the current x-coordinate of the hazard.
     *
     * @return the x-coordinate of the hazard
     */
    public float getX() {
        return x;
    }

    /**
     * Gets the current y-coordinate of the hazard.
     *
     * @return the y-coordinate of the hazard
     */
    public float getY() {
        return y;
    }

    /**
     * Gets the width of the hazard.
     *
     * @return the width of the hazard
     */
    public float getWidth() {
        return width;
    }

    /**
     * Gets the height of the hazard.
     *
     * @return the height of the hazard
     */
    public float getHeight() {
        return height;
    }

    public HazardType getType() {
        return type;
    }

    /**
     * Checks if the specified player is colliding with this hazard.
     * If a collision is detected, the {@link #onTouch(AbstractPlayer)} method is invoked.
     *
     * @param player the player to check for collision
     */
    public void tryTouch(AbstractPlayer player) {
        if (player.getBounds().overlaps(bounds)) {
            onTouch(player);
        }
    }

    /**
     * Called when the player comes into contact with this hazard.
     * Subclasses should implement this to define the specific effect or interaction
     * that occurs when the player touches the hazard.
     *
     * @param player the player instance that collided with this hazard
     */
    public abstract void onTouch(AbstractPlayer player);
}
