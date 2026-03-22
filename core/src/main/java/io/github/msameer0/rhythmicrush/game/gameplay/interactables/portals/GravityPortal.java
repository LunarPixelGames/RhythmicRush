package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * A portal that flips the player's gravity upon contact.
 */
@Registry(id = "gravity_portal")
public class GravityPortal extends AbstractPortal {

    private boolean flipped;

    /**
     * Constructs a new GravityPortal at the specified coordinates.
     *
     * @param x       the x-coordinate of the portal's position
     * @param y       the y-coordinate of the portal's position
     * @param flipped whether this portal flips gravity to upside down (true) or normal (false)
     */
    public GravityPortal(float x, float y, boolean flipped) {
        super(x, y);
        this.type = PortalType.GRAVITY;
        this.flipped = flipped;
    }

    /**
     * No-arg constructor for pooling.
     */
    public GravityPortal() {
        super();
        this.type = PortalType.GRAVITY;
    }

    public GravityPortal init(float x, float y, boolean flipped) {
        super.init(x, y);
        this.flipped = flipped;
        return this;
    }

    public boolean isFlipped() {
        return flipped;
    }
}
