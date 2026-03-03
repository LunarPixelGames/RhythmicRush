package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;

public class CubePortal extends AbstractPortal {

    public CubePortal(float x, float y) {
        super(x, y);
    }

    @Override
    public AbstractPlayer onTouch(AbstractPlayer player) {
        // transform player to Cube at current position
        return new Cube(player.getX(), player.getY());
    }
}
