package io.github.msameer0.rhythmicrush.game;

import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;

import java.util.ArrayList;

public class GameWorld {
    private AbstractPlayer player;
    private float groundY = 50;
    private ArrayList<AbstractPortal> portals;
    private ArrayList<AbstractHazard> hazards;

    private float scrollSpeed = 200f; // units per second

    private boolean playerDead = false;

    public GameWorld() {
        player = new Cube(100, groundY);
        player.setWorld(this);
        portals = new ArrayList<>();
        hazards = new ArrayList<>();
    }

    public void update(float delta) {
        player.update(delta, groundY);

        // check portal collisions
        for (AbstractPortal portal : portals) {
            player = portal.tryTouch(player);
            player.setWorld(this);
        }

        // move portals behind player to simulate forward movement
        for (AbstractPortal portal : portals) {
            portal.updatePosition(scrollSpeed, delta);
        }

        //check hazard collisions
        for (AbstractHazard hazard : hazards) {
            hazard.tryTouch(player);
        }

        //move hazards behind
        for (AbstractHazard hazard : hazards) {
            hazard.updatePosition(scrollSpeed, delta);
        }

        // remove offscreen portals
        portals.removeIf(portal -> portal.getX() + portal.getWidth() < 0);
        //remove offscreen hazards
        hazards.removeIf(h -> h.getX() + h.getWidth() < 0);
    }

    public AbstractPlayer getPlayer() { return player; }

    public float getGroundY() { return groundY; }

    public void playerDied() {
        playerDead = true;
    }

    public boolean isPlayerDead() {
        return playerDead;
    }

    public ArrayList<AbstractPortal> getPortals() { return portals; }
    public ArrayList<AbstractHazard> getHazards() {
        return hazards;
    }

    public void addPortal(AbstractPortal portal) { portals.add(portal); }

    public void addHazard(AbstractHazard hazard) {
        hazards.add(hazard);
    }
}
