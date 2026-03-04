package io.github.msameer0.rhythmicrush.game;

import com.badlogic.gdx.graphics.Color;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.level.LevelData;

import java.util.ArrayList;

public class GameWorld {

    // ── Core state ────────────────────────────────────────────────────────────
    private AbstractPlayer player;
    private float groundY      = 50f;
    private float scrollSpeed  = 200f;
    private boolean playerDead = false;

    // ── Object lists ──────────────────────────────────────────────────────────
    private ArrayList<AbstractPortal> portals = new ArrayList<>();
    private ArrayList<AbstractHazard> hazards = new ArrayList<>();
    private ArrayList<Block>          blocks  = new ArrayList<>();

    // ── Level / progress ──────────────────────────────────────────────────────
    /**
     * World-space X of the rightmost object's right edge at spawn time.
     * Objects scroll left at scrollSpeed, so the player "reaches" the end when
     * the world has scrolled by levelEndX units — i.e. after levelEndX / scrollSpeed seconds.
     */
    private float levelEndX        = 0f;   // set when a level is loaded
    private float worldScrolled    = 0f;   // cumulative distance scrolled so far
    private float postEndTimer     = -1f;  // counts up after player passes end; -1 = not started
    private static final float POST_END_DELAY = 2f;
    private boolean levelComplete  = false;

    // ── Visuals (from level metadata) ─────────────────────────────────────────
    private Color backgroundColor = new Color(0.1f, 0.1f, 0.18f, 1f);
    private Color groundColor     = new Color(0.09f, 0.13f, 0.24f, 1f);

    // ─────────────────────────────────────────────────────────────────────────

    public GameWorld() {
        player = new Cube(100, groundY);
        player.setWorld(this);
    }

    // ── Level loading ─────────────────────────────────────────────────────────

    public void loadLevel(LevelData data) {
        // clear existing objects
        portals.clear();
        hazards.clear();
        blocks.clear();
        playerDead    = false;
        levelComplete = false;
        worldScrolled = 0f;
        postEndTimer  = -1f;

        // spawn objects
        for (LevelData.ObjectEntry e : data.objects) {
            switch (e.type) {
                case "block":
                    blocks.add(new Block(e.x, e.y, e.size));
                    break;
                case "spike":
                    hazards.add(new Spike(e.x, e.y));
                    break;
                case "cube_portal":
                    portals.add(new CubePortal(e.x, e.y));
                    break;
                case "ship_portal":
                    portals.add(new ShipPortal(e.x, e.y));
                    break;
                default:
                    // unknown type — skip gracefully (extensible)
                    break;
            }
        }

        levelEndX = data.getLevelEndX();

        // parse colors from hex
        if (data.bgColor != null && !data.bgColor.isEmpty()) {
            backgroundColor = hexToColor(data.bgColor);
        }
        if (data.groundColor != null && !data.groundColor.isEmpty()) {
            groundColor = hexToColor(data.groundColor);
        }

        // reset player
        player = new Cube(100, groundY);
        player.setWorld(this);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(float delta) {
        if (playerDead || levelComplete) return;

        player.update(delta, groundY);

        // portals
        for (AbstractPortal portal : portals) {
            player = portal.tryTouch(player);
            player.setWorld(this);
        }

        // hazards
        for (AbstractHazard hazard : hazards) {
            hazard.tryTouch(player);
        }

        // blocks
        for (Block block : blocks) {
            block.tryTouch(player);
        }

        player.tryJump();

        // scroll everything
        for (AbstractPortal portal : portals)  portal.updatePosition(scrollSpeed, delta);
        for (AbstractHazard  hazard  : hazards) hazard.updatePosition(scrollSpeed, delta);
        for (Block           block   : blocks)  block.updatePosition(scrollSpeed, delta);

        worldScrolled += scrollSpeed * delta;

        // cull offscreen objects
        portals.removeIf(p -> p.getX() + p.getWidth() < 0);
        hazards.removeIf(h -> h.getX() + h.getWidth() < 0);
        // keep blocks in list (player may stand on them), cull only when well offscreen
        blocks.removeIf(b -> b.getX() + b.getWidth() < -200);

        // level end check
        if (levelEndX > 0 && worldScrolled >= levelEndX) {
            if (postEndTimer < 0) postEndTimer = 0f;
        }
        if (postEndTimer >= 0) {
            postEndTimer += delta;
            if (postEndTimer >= POST_END_DELAY) {
                levelComplete = true;
            }
        }
    }

    // ── Progress (0.0 – 1.0) ─────────────────────────────────────────────────

    /**
     * Returns how far through the level the player is, as a 0–1 fraction.
     * Returns 0 if no level is loaded (levelEndX == 0).
     */
    public float getProgress() {
        if (levelEndX <= 0) return 0f;
        return Math.min(worldScrolled / levelEndX, 1f);
    }

    // ── Add helpers (debug / editor playtest) ─────────────────────────────────

    public void addPortal(AbstractPortal portal) { portals.add(portal); }
    public void addHazard(AbstractHazard hazard) { hazards.add(hazard); }
    public void addBlock(Block block)            { blocks.add(block);   }

    // ── Getters ───────────────────────────────────────────────────────────────

    public AbstractPlayer getPlayer()              { return player; }
    public float          getGroundY()             { return groundY; }
    public boolean        isPlayerDead()           { return playerDead; }
    public boolean        isLevelComplete()        { return levelComplete; }
    public ArrayList<AbstractPortal> getPortals()  { return portals; }
    public ArrayList<AbstractHazard> getHazards()  { return hazards; }
    public ArrayList<Block>          getBlocks()   { return blocks; }
    public Color          getBackgroundColor()     { return backgroundColor; }
    public Color          getGroundColor()         { return groundColor; }

    public void playerDied() { playerDead = true; }

    // ── Util ──────────────────────────────────────────────────────────────────

    private static Color hexToColor(String hex) {
        // strip leading # if present
        if (hex.startsWith("#")) hex = hex.substring(1);
        long val = Long.parseLong(hex, 16);
        float r = ((val >> 16) & 0xFF) / 255f;
        float g = ((val >>  8) & 0xFF) / 255f;
        float b = ( val        & 0xFF) / 255f;
        return new Color(r, g, b, 1f);
    }
}
