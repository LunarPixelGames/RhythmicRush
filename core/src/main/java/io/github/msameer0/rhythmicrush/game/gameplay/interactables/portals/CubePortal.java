package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * A portal that transforms the player into a cube-based movement mode upon contact.
 * <p>
 * When triggered, this portal replaces the current player instance with a cube player
 * retrieved from the world's object pool.
 */
public class CubePortal extends AbstractPortal {

    /**
     * Constructs a new CubePortal at the specified coordinates.
     *
     * @param x the x-coordinate of the portal's position
     * @param y the y-coordinate of the portal's position
     */
    public CubePortal(float x, float y) {
        super(x, y);
        this.type = PortalType.CUBE;
    }

    /**
     * No-arg constructor for pooling — call init() before use.
     */
    public CubePortal() {
        super();
        this.type = PortalType.CUBE;
    }

    @Override
    public CubePortal init(float x, float y) {
        super.init(x, y);
        this.type = PortalType.CUBE;
        return this;
    }

    /**
     * Handles the interaction when a player touches the portal, transforming
     * the player into a cube-based movement mode.
     *
     * @param player the player instance that touched the portal
     * @return the new player instance with cube physics applied
     */
    @Override
    public AbstractPlayer onTouch(AbstractPlayer player) {
        return player.getWorld().obtainCube(player.getX(), player.getY(), player.getVelocityY(), player.isJumpHeld());
    }
}
