package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;

public class CubePortal extends AbstractPortal {

    public CubePortal(float x, float y) { super(x, y); }

    /** No-arg constructor for pooling — call init() before use. */
    public CubePortal() { super(); }

    @Override
    public AbstractPlayer onTouch(AbstractPlayer player) {
        // Player instance is obtained from GameWorld's cube pool
        return player.getWorld().obtainCube(player.getX(), player.getY());
    }
}
