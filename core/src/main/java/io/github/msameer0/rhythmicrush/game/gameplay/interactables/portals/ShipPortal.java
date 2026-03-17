package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * A portal that transforms the player into a ship mode.
 * When touched, it triggers the world to replace the current player instance
 * with a ship-specific player instance at the same coordinates.
 */
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

    /**
     * Handles the interaction when a player touches the portal. Transforms the current
     * player into a ship entity at their current coordinates.
     *
     * @param player the player entity that touched the portal
     * @return the new ship-based player instance
     */
    @Override
    public AbstractPlayer onTouch(AbstractPlayer player) {
        return player.getWorld().obtainShip(player.getX(), player.getY());
    }
}
