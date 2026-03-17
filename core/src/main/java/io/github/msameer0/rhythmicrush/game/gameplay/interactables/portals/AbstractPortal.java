package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * Represents the base abstract class for all interactable portals in the game.
 * <p>
 * This class provides the foundational logic for portal movement, collision detection,
 * and state management. Portals are designed to be interactable objects that trigger
 * a transformation or effect on the player upon contact.
 * </p>
 * <p>
 */
public abstract class AbstractPortal {
    public enum PortalType {
        CUBE,
        SHIP,
        OTHER
    }

    protected PortalType type;
    protected float x, y;
    protected float width = 50, height = 100;
    protected Rectangle bounds;
    protected boolean used = false;

    /**
     * Constructs an AbstractPortal at the specified coordinates and initializes its collision bounds.
     *
     * @param x the initial x-coordinate of the portal
     * @param y the initial y-coordinate of the portal
     */
    public AbstractPortal(float x, float y) {
        this.x = x;
        this.y = y;
        bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Default constructor for AbstractPortal.
     * Initializes the collision bounds rectangle without setting specific coordinates.
     */
    public AbstractPortal() {
        bounds = new Rectangle();
    }

    /**
     * Initializes or resets the portal with the specified coordinates.
     * <p>
     * This method sets the portal's position, resets its "used" state to false,
     * and updates its collision bounds. It returns the instance to allow for method chaining.
     * </p>
     *
     * @param x the new x-coordinate of the portal
     * @param y the new y-coordinate of the portal
     * @return this {@code AbstractPortal} instance for chaining
     */
    public AbstractPortal init(float x, float y) {
        this.x = x;
        this.y = y;
        this.used = false;
        bounds.set(x, y, width, height);
        return this;
    }

    /**
     * Updates the portal's horizontal position based on the game's scroll speed and the time delta.
     * This effectively moves the portal across the screen and updates its collision bounds.
     *
     * @param scrollSpeed the speed at which the level scrolls
     * @param delta the time elapsed since the last frame in seconds
     */
    public void updatePosition(float scrollSpeed, float delta) {
        x -= scrollSpeed * delta;
        updateBounds();
    }

    /**
     * Gets the collision boundaries of the portal.
     *
     * @return a {@code Rectangle} representing the portal's bounds
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Attempts to interact with the player. If the portal has not been used and
     * the player's bounds overlap with the portal's bounds, the portal is marked
     * as used and the interaction logic is triggered.
     *
     * @param player the player instance to check for collision
     * @return the player instance after the interaction, which may be transformed
     *         by the {@link #onTouch(AbstractPlayer)} method, or the original
     *         player if no interaction occurred
     */
    public AbstractPlayer tryTouch(AbstractPlayer player) {
        if (!used && player.getBounds().overlaps(bounds)) {
            used = true;
            return onTouch(player);
        }
        return player;
    }

    /**
     * Defines the specific interaction logic when a player touches the portal.
     * Subclasses should implement this method to apply transformations or state changes to the player.
     *
     * @param player the player instance that collided with the portal
     * @return the resulting player instance after the portal's effect is applied
     */
    public abstract AbstractPlayer onTouch(AbstractPlayer player);

    /**
     * Synchronizes the position of the portal's collision bounds with its current
     * x and y coordinates.
     */
    public void updateBounds() {
        bounds.setPosition(x, y);
    }

    /**
     * Gets the current x-coordinate of the portal.
     *
     * @return the x-coordinate of the portal
     */
    public float getX() {
        return x;
    }

    /**
     * Gets the current y-coordinate of the portal.
     *
     * @return the y-coordinate of the portal
     */
    public float getY() {
        return y;
    }

    /**
     * Gets the width of the portal.
     *
     * @return the width of the portal
     */
    public float getWidth() {
        return width;
    }

    /**
     * Gets the height of the portal.
     *
     * @return the height of the portal
     */
    public float getHeight() {
        return height;
    }

    public PortalType getType() {
        return type;
    }

    /**
     * Checks whether the portal has already been interacted with.
     *
     * @return {@code true} if the portal has been triggered; {@code false} otherwise
     */
    public boolean isUsed() {
        return used;
    }
}
