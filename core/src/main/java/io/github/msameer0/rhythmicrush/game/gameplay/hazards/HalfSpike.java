package io.github.msameer0.rhythmicrush.game.gameplay.hazards;

import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * Represents a spike hazard within the game environment.
 * <p>
 * A spike is a lethal obstacle that triggers a player's death upon collision.
 * It maintains a specialized hitbox that is smaller than its visual texture
 * and automatically adjusts its orientation based on the spike's rotation
 * (supporting 0, 90, 180, and 270 degrees).
 * </p>
 */
@Registry(id = "half_spike")
public class HalfSpike extends AbstractHazard {

    private static final float PLAYER_SIZE = 50f;
    private static final float TEXTURE_SIZE = 50f;
    private static final float HITBOX_W = PLAYER_SIZE * 0.25f;
    private static final float HITBOX_H = PLAYER_SIZE * 0.2f;
    private static final float HITBOX_CENTER_X = (TEXTURE_SIZE - HITBOX_W) / 2f;

    private float rotation;
    private final Rectangle spikeHitbox;

    /**
     * Constructs a new Spike instance with default values and initializes its specialized hitbox.
     */
    public HalfSpike() {
        super(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        this.type = HazardType.HALF_SPIKE;
        spikeHitbox = new Rectangle();
    }

    /**
     * Constructs a new Spike at the specified position with a default rotation of 0 degrees.
     *
     * @param x the x-coordinate of the spike
     * @param y the y-coordinate of the spike
     */
    public HalfSpike(float x, float y) {
        this(x, y, 0f);
        this.type = HazardType.HALF_SPIKE;
    }

    /**
     * Constructs a new Spike at the specified coordinates with a given rotation.
     *
     * @param x        the x-coordinate of the spike
     * @param y        the y-coordinate of the spike
     * @param rotation the rotation of the spike in degrees
     */
    public HalfSpike(float x, float y, float rotation) {
        super(x, y, TEXTURE_SIZE, TEXTURE_SIZE);
        this.type = HazardType.HALF_SPIKE;
        this.rotation = rotation;
        spikeHitbox = new Rectangle();
        updateHitbox();
    }

    /**
     * Initializes or resets the spike's state with the specified position and rotation.
     * This is typically used for object pooling to avoid frequent allocations.
     *
     * @param x        the new x-coordinate of the spike
     * @param y        the new y-coordinate of the spike
     * @param rotation the new rotation of the spike in degrees
     * @return this {@code Spike} instance for method chaining
     */
    public HalfSpike init(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.type = HazardType.HALF_SPIKE;
        bounds.setPosition(x, y);
        updateHitbox();
        return this;
    }

    /**
     * Updates the position and dimensions of the internal spike hitbox based on the current
     * rotation. This method aligns the lethal area of the spike with its visual orientation
     * by calculating the appropriate offset and bounds for 0, 90, 180, and 270 degrees.
     */
    private void updateHitbox() {
        switch ((Math.round(rotation / 90f) * 90 % 360 + 360) % 360) {
            case 90: // 90 CCW = Pointing LEFT (Base on RIGHT)
                spikeHitbox.set(x + TEXTURE_SIZE - HITBOX_H, y + HITBOX_CENTER_X, HITBOX_H, HITBOX_W);
                break;
            case 180: // 180 CCW = Pointing DOWN (Base on TOP)
                spikeHitbox.set(x + HITBOX_CENTER_X, y + TEXTURE_SIZE - HITBOX_H, HITBOX_W, HITBOX_H);
                break;
            case 270: // 270 CCW = Pointing RIGHT (Base on LEFT)
                spikeHitbox.set(x, y + HITBOX_CENTER_X, HITBOX_H, HITBOX_W);
                break;
            default: // 0 CCW = Pointing UP (Base on BOTTOM)
                spikeHitbox.set(x + HITBOX_CENTER_X, y, HITBOX_W, HITBOX_H);
                break;
        }
    }

    /**
     * Updates the spike's position based on the scroll speed and delta time,
     * and refreshes its specialized hitbox to match the new coordinates.
     *
     * @param scrollSpeed the horizontal speed at which the game environment scrolls
     * @param delta       the time elapsed since the last frame in seconds
     */
    @Override
    public void updatePosition(float scrollSpeed, float delta) {
        super.updatePosition(scrollSpeed, delta);
        updateHitbox();
    }

    /**
     * Handles the interaction when a player instance touches this spike.
     * Checks for a collision between the spike's specialized lethal hitbox and the
     * player's bounds, triggering the player's death if an overlap is detected.
     *
     * @param player the player instance interacting with this hazard
     */
    @Override
    public void onTouch(AbstractPlayer player) {
        if (spikeHitbox.overlaps(player.getBounds())) {
            player.getWorld().playerDied();
        }
    }

    /**
     * Gets the current rotation of the spike in degrees.
     *
     * @return the rotation of the spike
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Gets the specialized lethal hitbox of the spike.
     * Unlike the standard bounds, this rectangle is smaller than the texture and
     * is adjusted based on the spike's rotation to ensure accurate collision detection.
     *
     * @return the {@link Rectangle} representing the current lethal area of the spike
     */
    public Rectangle getHitbox() {
        return spikeHitbox;
    }
}
