package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * A portal that transforms the player into a ship mode.
 * When touched, it triggers the world to replace the current player instance
 * with a ship-specific player instance at the same coordinates.
 */
@Registry(id = "ship_portal")
public class ShipPortal extends AbstractPortal {

    /**
     * Constructs a new ShipPortal at the specified coordinates.
     *
     * @param x the x-coordinate of the portal
     * @param y the y-coordinate of the portal
     */
    public ShipPortal(float x, float y) {
        super(x, y);
        this.type = PortalType.SHIP;
    }

    /**
     * No-arg constructor for pooling — call init() before use.
     */
    public ShipPortal() {
        super();
        this.type = PortalType.SHIP;
    }

    @Override
    public ShipPortal init(float x, float y) {
        super.init(x, y);
        this.type = PortalType.SHIP;
        return this;
    }
}
