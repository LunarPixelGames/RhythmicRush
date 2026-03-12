package io.github.msameer0.rhythmicrush.game;

import com.badlogic.gdx.graphics.Color;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
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
    private float   groundY     = 50f;
    private float   scrollSpeed = 300f;
    private boolean playerDead  = false;

    // set each frame by the renderer so culling matches the actual left screen edge
    private float cullX = 0f;

    // ── Object lists ──────────────────────────────────────────────────────────
    private ArrayList<AbstractPortal> portals = new ArrayList<>();
    private ArrayList<AbstractHazard> hazards = new ArrayList<>();
    private ArrayList<Block>          blocks  = new ArrayList<>();

    // ── Color triggers ────────────────────────────────────────────────────────

    /** Lightweight descriptor loaded from LevelData — never modified at runtime. */
    private static class ColorTrigger {
        final float  worldX;       // absolute world-space X (not scrolled)
        final Color  targetBg;     // null = don't change bg
        final Color  targetGround; // null = don't change ground
        final float  fadeDuration;
        boolean fired = false;

        ColorTrigger(float worldX, Color targetBg, Color targetGround, float fadeDuration) {
            this.worldX       = worldX;
            this.targetBg     = targetBg;
            this.targetGround = targetGround;
            this.fadeDuration = fadeDuration;
        }
    }

    /** Active fade in progress. One per color channel (bg / ground). */
    private static class ColorFade {
        Color from;
        Color to;
        float duration;
        float elapsed = 0f;

        ColorFade(Color from, Color to, float duration) {
            this.from     = new Color(from);
            this.to       = new Color(to);
            this.duration = duration;
        }
    }

    private ArrayList<ColorTrigger> colorTriggers = new ArrayList<>();
    private ColorFade bgFade     = null;
    private ColorFade groundFade = null;

    // ── Level / progress ──────────────────────────────────────────────────────
    private LevelData currentLevelData = null;

    private float   levelEndX     = 0f;
    private float   worldScrolled = 0f;
    private float   postEndTimer  = -1f;
    private static final float POST_END_DELAY = 2f;
    private boolean levelComplete = false;

    // ── Visuals ───────────────────────────────────────────────────────────────
    private Color backgroundColor = new Color(0.1f, 0.1f, 0.18f, 1f);
    private Color groundColor     = new Color(0.09f, 0.13f, 0.24f, 1f);

    // ─────────────────────────────────────────────────────────────────────────

    public GameWorld() {
        player = new Cube(100, groundY);
        player.setWorld(this);
    }

    // ── Level loading ─────────────────────────────────────────────────────────

    public void loadLevel(LevelData data) {
        currentLevelData = data;

        portals.clear();
        hazards.clear();
        blocks.clear();
        colorTriggers.clear();
        bgFade        = null;
        groundFade    = null;
        playerDead    = false;
        levelComplete = false;
        worldScrolled = 0f;
        postEndTimer  = -1f;

        if (data.bgColor != null && !data.bgColor.isEmpty())
            backgroundColor = hexToColor(data.bgColor);
        if (data.groundColor != null && !data.groundColor.isEmpty())
            groundColor = hexToColor(data.groundColor);

        for (LevelData.ObjectEntry e : data.objects) {
            switch (e.type) {
                case "block":
                    BlockType bt = BlockType.DEFAULT;
                    if (e.blockType != null) {
                        for (BlockType t : BlockType.values()) {
                            if (t.textureName.equals(e.blockType)) { bt = t; break; }
                        }
                    }
                    blocks.add(new Block(e.x, e.y, e.size, bt));
                    break;
                case "spike":
                    hazards.add(new Spike(e.x, e.y, e.rotation));
                    break;
                case "cube_portal":
                    portals.add(new CubePortal(e.x, e.y));
                    break;
                case "ship_portal":
                    portals.add(new ShipPortal(e.x, e.y));
                    break;
                case "color_trigger":
                    Color targetBg     = (e.triggerBgColor     != null && !e.triggerBgColor.isEmpty())
                        ? hexToColor(e.triggerBgColor) : null;
                    Color targetGround = (e.triggerGroundColor != null && !e.triggerGroundColor.isEmpty())
                        ? hexToColor(e.triggerGroundColor) : null;
                    colorTriggers.add(new ColorTrigger(e.x, targetBg, targetGround, e.fadeDuration));
                    break;
                default:
                    break;
            }
        }

        levelEndX = data.getLevelEndX();

        player = new Cube(100, groundY);
        player.setWorld(this);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    public void reset() {
        if (currentLevelData != null) {
            loadLevel(currentLevelData);
        } else {
            portals.clear();
            hazards.clear();
            blocks.clear();
            colorTriggers.clear();
            bgFade        = null;
            groundFade    = null;
            playerDead    = false;
            levelComplete = false;
            worldScrolled = 0f;
            postEndTimer  = -1f;
            levelEndX     = 0f;
            player = new Cube(100, groundY);
            player.setWorld(this);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(float delta) {
        if (playerDead || levelComplete) return;

        player.update(delta, groundY);

        for (AbstractPortal portal : portals) {
            player = portal.tryTouch(player);
            player.setWorld(this);
        }
        for (AbstractHazard hazard : hazards) hazard.tryTouch(player);
        for (Block block : blocks)           block.tryTouch(player);
        player.tryJump();

        for (AbstractPortal portal : portals) portal.updatePosition(scrollSpeed, delta);
        for (AbstractHazard hazard : hazards)  hazard.updatePosition(scrollSpeed, delta);
        for (Block block : blocks)             block.updatePosition(scrollSpeed, delta);

        worldScrolled += scrollSpeed * delta;

        // ── Color trigger check — fire when player's world x passes trigger x ──
        float playerWorldX = 100f + worldScrolled; // player is always at screen x=100, world scrolls
        for (ColorTrigger ct : colorTriggers) {
            if (!ct.fired && playerWorldX >= ct.worldX) {
                ct.fired = true;
                if (ct.targetBg != null)
                    bgFade = new ColorFade(backgroundColor, ct.targetBg, ct.fadeDuration);
                if (ct.targetGround != null)
                    groundFade = new ColorFade(groundColor, ct.targetGround, ct.fadeDuration);
            }
        }

        // ── Advance active color fades ─────────────────────────────────────────
        if (bgFade != null) {
            bgFade.elapsed += delta;
            float t = Math.min(bgFade.elapsed / bgFade.duration, 1f);
            backgroundColor.set(
                lerp(bgFade.from.r, bgFade.to.r, t),
                lerp(bgFade.from.g, bgFade.to.g, t),
                lerp(bgFade.from.b, bgFade.to.b, t),
                1f);
            if (t >= 1f) bgFade = null;
        }
        if (groundFade != null) {
            groundFade.elapsed += delta;
            float t = Math.min(groundFade.elapsed / groundFade.duration, 1f);
            groundColor.set(
                lerp(groundFade.from.r, groundFade.to.r, t),
                lerp(groundFade.from.g, groundFade.to.g, t),
                lerp(groundFade.from.b, groundFade.to.b, t),
                1f);
            if (t >= 1f) groundFade = null;
        }

        portals.removeIf(p -> p.getX() + p.getWidth() < cullX);
        hazards.removeIf(h -> h.getX() + h.getWidth() < cullX);
        blocks.removeIf(b -> b.getX() + b.getWidth() < cullX - 200);

        if (levelEndX > 0 && worldScrolled >= levelEndX) {
            if (postEndTimer < 0) postEndTimer = 0f;
        }
        if (postEndTimer >= 0) {
            postEndTimer += delta;
            if (postEndTimer >= POST_END_DELAY) levelComplete = true;
        }
    }

    // ── Progress ──────────────────────────────────────────────────────────────

    public float getProgress() {
        if (levelEndX <= 0) return 0f;
        return Math.min(worldScrolled / levelEndX, 1f);
    }

    // ── Add helpers ───────────────────────────────────────────────────────────

    public void addPortal(AbstractPortal portal) { portals.add(portal); }
    public void addHazard(AbstractHazard hazard) { hazards.add(hazard); }
    public void addBlock(Block block)            { blocks.add(block); }

    // ── Getters ───────────────────────────────────────────────────────────────

    public AbstractPlayer            getPlayer()          { return player; }
    public float                     getGroundY()         { return groundY; }
    public boolean                   isPlayerDead()       { return playerDead; }
    public boolean                   isLevelComplete()    { return levelComplete; }
    public ArrayList<AbstractPortal> getPortals()         { return portals; }
    public ArrayList<AbstractHazard> getHazards()         { return hazards; }
    public ArrayList<Block>          getBlocks()          { return blocks; }
    public Color                     getBackgroundColor() { return backgroundColor; }
    public Color                     getGroundColor()     { return groundColor; }
    public void playerDied()          { playerDead = true; }
    public void setCullX(float x)     { cullX = x; }

    // ── Util ──────────────────────────────────────────────────────────────────

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static Color hexToColor(String hex) {
        if (hex == null || hex.isEmpty()) return new Color(0, 0, 0, 1);
        if (hex.startsWith("#")) hex = hex.substring(1);
        long val = Long.parseLong(hex, 16);
        float r = ((val >> 16) & 0xFF) / 255f;
        float g = ((val >>  8) & 0xFF) / 255f;
        float b = ( val        & 0xFF) / 255f;
        return new Color(r, g, b, 1f);
    }
}
