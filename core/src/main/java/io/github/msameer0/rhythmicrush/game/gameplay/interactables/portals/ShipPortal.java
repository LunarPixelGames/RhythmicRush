package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship;

public class ShipPortal extends AbstractPortal {

    public ShipPortal(float x, float y) {
        super(x, y);
    }

    @Override
    public AbstractPlayer onTouch(AbstractPlayer player) {
        // transform player to Ship at current position
        return new Ship(player.getX(), player.getY());
    }
}
