package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * A portal that toggles the player's gravity upon contact.
 */
@Registry(id = "gravity_portal")
public class GravityPortal extends AbstractPortal {

    /**
     * Constructs a new GravityPortal at the specified coordinates.
     *
     * @param x       the x-coordinate of the portal's position
     * @param y       the y-coordinate of the portal's position
     */
    public GravityPortal(float x, float y) {
        super(x, y);
        this.type = PortalType.GRAVITY;
    }

    /**
     * No-arg constructor for pooling.
     */
    public GravityPortal() {
        super();
        this.type = PortalType.GRAVITY;
    }

    @Override
    public GravityPortal init(float x, float y) {
        super.init(x, y);
        return this;
    }
}
