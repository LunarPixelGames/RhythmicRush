package io.github.msameer0.rhythmicrush.game;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;

import java.util.ArrayList;

public class GameWorld {
    private AbstractPlayer player;
    private float groundY = 50;
    private ArrayList<AbstractPortal> portals;

    private float scrollSpeed = 200f; // units per second

    public GameWorld() {
        player = new Cube(100, groundY);
        portals = new ArrayList<>();
    }

    public void update(float delta) {
        player.update(delta, groundY);

        // check portal collisions
        for (AbstractPortal portal : portals) {
            player = portal.tryTouch(player);
        }

        // move portals behind player to simulate forward movement
        for (AbstractPortal portal : portals) {
            portal.updatePosition(scrollSpeed, delta);
        }

        // remove offscreen portals
        portals.removeIf(portal -> portal.getX() + portal.getWidth() < 0);
    }

    public AbstractPlayer getPlayer() { return player; }

    public float getGroundY() { return groundY; }

    public ArrayList<AbstractPortal> getPortals() { return portals; }

    public void addPortal(AbstractPortal portal) { portals.add(portal); }
}
