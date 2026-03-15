package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship;

public class ShipPortal extends AbstractPortal {

    public ShipPortal(float x, float y) { super(x, y); }

    /** No-arg constructor for pooling — call init() before use. */
    public ShipPortal() { super(); }

    @Override
    public AbstractPlayer onTouch(AbstractPlayer player) {
        // Player instance is obtained from GameWorld's ship pool
        return player.getWorld().obtainShip(player.getX(), player.getY());
    }
}
