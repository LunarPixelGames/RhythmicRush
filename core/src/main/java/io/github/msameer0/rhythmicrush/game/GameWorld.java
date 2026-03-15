package io.github.msameer0.rhythmicrush.game;

import com.badlogic.gdx.graphics.Color;
import io.github.msameer0.rhythmicrush.game.engine.ObjectPool;
import io.github.msameer0.rhythmicrush.game.engine.Tickable;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship;
import io.github.msameer0.rhythmicrush.game.level.LevelData;

import java.util.ArrayList;

public class GameWorld implements Tickable {

    // ── Core state ────────────────────────────────────────────────────────────
    private AbstractPlayer player;
    private float   groundY     = 50f;
    private float   scrollSpeed = 320f;
    private boolean playerDead  = false;
    private float   cullX       = 0f;

    // ── Object lists ──────────────────────────────────────────────────────────
    private final ArrayList<AbstractPortal> portals = new ArrayList<>();
    private final ArrayList<AbstractHazard> hazards = new ArrayList<>();
    private final ArrayList<Block>          blocks  = new ArrayList<>();

    // ── Object pools — survive for the lifetime of GameWorld ─────────────────
    // On every reset we free all active objects back into the pools so the next
    // loadLevel() can reuse them without any heap allocation.
    private final ObjectPool<Block> blockPool = new ObjectPool<Block>() {
        @Override protected Block create()        { return new Block(); }
        @Override protected void  reset(Block b)  { /* init() handles state */ }
    };
    private final ObjectPool<Spike> spikePool = new ObjectPool<Spike>() {
        @Override protected Spike create()        { return new Spike(); }
        @Override protected void  reset(Spike s)  { /* init() handles state */ }
    };
    private final ObjectPool<CubePortal> cubePortalPool = new ObjectPool<CubePortal>() {
        @Override protected CubePortal create()           { return new CubePortal(); }
        @Override protected void       reset(CubePortal p){ /* init() handles state */ }
    };
    private final ObjectPool<ShipPortal> shipPortalPool = new ObjectPool<ShipPortal>() {
        @Override protected ShipPortal create()           { return new ShipPortal(); }
        @Override protected void       reset(ShipPortal p){ /* init() handles state */ }
    };
    private final ObjectPool<Cube> cubePool = new ObjectPool<Cube>() {
        @Override protected Cube create()       { return new Cube(); }
        @Override protected void reset(Cube c)  { /* init() handles state */ }
    };
    private final ObjectPool<Ship> shipPool = new ObjectPool<Ship>() {
        @Override protected Ship create()       { return new Ship(); }
        @Override protected void reset(Ship s)  { /* init() handles state */ }
    };

    // ── Color triggers ────────────────────────────────────────────────────────
    private static class ColorTrigger {
        final float  worldX;
        final Color  targetBg;
        final Color  targetGround;
        final float  fadeDuration;
        boolean fired = false;

        ColorTrigger(float worldX, Color targetBg, Color targetGround, float fadeDuration) {
            this.worldX       = worldX;
            this.targetBg     = targetBg;
            this.targetGround = targetGround;
            this.fadeDuration = fadeDuration;
        }
    }

    private static class ColorFade {
        Color from, to;
        float duration, elapsed;

        ColorFade(Color from, Color to, float duration) {
            this.from     = new Color(from);
            this.to       = new Color(to);
            this.duration = duration;
            this.elapsed  = 0f;
        }
    }

    private final ArrayList<ColorTrigger> colorTriggers = new ArrayList<>();
    private ColorFade bgFade     = null;
    private ColorFade groundFade = null;

    // ── Level / progress ──────────────────────────────────────────────────────
    private LevelData currentLevelData = null;
    private float     levelEndX        = 0f;
    private float     worldScrolled    = 0f;
    private float     postEndTimer     = -1f;
    private boolean   levelComplete    = false;
    private static final float POST_END_DELAY = 2f;

    // ── Visuals ───────────────────────────────────────────────────────────────
    private Color backgroundColor = new Color(0.1f, 0.1f, 0.18f, 1f);
    private Color groundColor     = new Color(0.09f, 0.13f, 0.24f, 1f);

    // ── Separate lists to track which pool each hazard/portal came from ───────
    // When freeing we need to cast back to the concrete type.
    private final ArrayList<Spike>      activeSpikes      = new ArrayList<>();
    private final ArrayList<CubePortal> activeCubePortals = new ArrayList<>();
    private final ArrayList<ShipPortal> activeShipPortals = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public GameWorld() {
        player = cubePool.obtain().init(100, groundY);
        player.setWorld(this);
    }

    // ── Pool accessors (used by portals to create players) ────────────────────

    public Cube obtainCube(float x, float y) {
        Cube c = cubePool.obtain().init(x, y);
        c.setWorld(this);
        return c;
    }

    public Ship obtainShip(float x, float y) {
        Ship s = shipPool.obtain().init(x, y);
        s.setWorld(this);
        return s;
    }

    // ── Level loading ─────────────────────────────────────────────────────────

    public void loadLevel(LevelData data) {
        currentLevelData = data;
        freeAllActiveObjects();

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
                case "block": {
                    BlockType bt = BlockType.DEFAULT;
                    if (e.blockType != null) {
                        for (BlockType t : BlockType.values())
                            if (t.textureName.equals(e.blockType)) { bt = t; break; }
                    }
                    Block b = blockPool.obtain().init(e.x, e.y, e.size, bt);
                    blocks.add(b);
                    break;
                }
                case "spike": {
                    Spike s = spikePool.obtain().init(e.x, e.y, e.rotation);
                    hazards.add(s);
                    activeSpikes.add(s);
                    break;
                }
                case "cube_portal": {
                    CubePortal p = cubePortalPool.obtain();
                    p.init(e.x, e.y);
                    portals.add(p);
                    activeCubePortals.add(p);
                    break;
                }
                case "ship_portal": {
                    ShipPortal p = shipPortalPool.obtain();
                    p.init(e.x, e.y);
                    portals.add(p);
                    activeShipPortals.add(p);
                    break;
                }
                case "color_trigger": {
                    Color targetBg     = (e.triggerBgColor     != null && !e.triggerBgColor.isEmpty())
                        ? hexToColor(e.triggerBgColor) : null;
                    Color targetGround = (e.triggerGroundColor != null && !e.triggerGroundColor.isEmpty())
                        ? hexToColor(e.triggerGroundColor) : null;
                    colorTriggers.add(new ColorTrigger(e.x, targetBg, targetGround, e.fadeDuration));
                    break;
                }
            }
        }

        levelEndX = data.getLevelEndX();

        // Free previous player and get a fresh one from the pool
        freePlayer();
        player = cubePool.obtain().init(100, groundY);
        player.setWorld(this);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    public void reset() {
        if (currentLevelData != null) loadLevel(currentLevelData);
        else {
            freeAllActiveObjects();
            colorTriggers.clear();
            bgFade        = null;
            groundFade    = null;
            playerDead    = false;
            levelComplete = false;
            worldScrolled = 0f;
            postEndTimer  = -1f;
            levelEndX     = 0f;
            freePlayer();
            player = cubePool.obtain().init(100, groundY);
            player.setWorld(this);
        }
    }

    // ── Free helpers ──────────────────────────────────────────────────────────

    private void freePlayer() {
        if (player instanceof Cube) cubePool.free((Cube) player);
        else if (player instanceof Ship) shipPool.free((Ship) player);
        player = null;
    }

    private void freeAllActiveObjects() {
        blockPool.freeAll(blocks);

        // Free spikes back to spike pool
        for (Spike s : activeSpikes) spikePool.free(s);
        activeSpikes.clear();
        hazards.clear();

        // Free portals back to their respective pools
        for (CubePortal p : activeCubePortals) cubePortalPool.free(p);
        activeCubePortals.clear();
        for (ShipPortal p : activeShipPortals) shipPortalPool.free(p);
        activeShipPortals.clear();
        portals.clear();
    }

    // ── Tickable ──────────────────────────────────────────────────────────────

    @Override
    public void tick(float delta) { update(delta); }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(float delta) {
        if (playerDead || levelComplete) return;

        player.update(delta, groundY);

        // Portal touch — may swap player type, previous player freed to pool
        for (AbstractPortal portal : portals) {
            AbstractPlayer next = portal.tryTouch(player);
            if (next != player) {
                freePlayer();
                player = next;
            }
        }

        for (AbstractHazard hazard : hazards) hazard.tryTouch(player);
        for (Block block : blocks)           block.tryTouch(player);
        player.tryJump();

        for (AbstractPortal portal : portals) portal.updatePosition(scrollSpeed, delta);
        for (AbstractHazard hazard : hazards) hazard.updatePosition(scrollSpeed, delta);
        for (Block block : blocks)            block.updatePosition(scrollSpeed, delta);

        worldScrolled += scrollSpeed * delta;

        // Color triggers
        float playerWorldX = 100f + worldScrolled;
        for (ColorTrigger ct : colorTriggers) {
            if (!ct.fired && playerWorldX >= ct.worldX) {
                ct.fired = true;
                if (ct.targetBg     != null) bgFade     = new ColorFade(backgroundColor, ct.targetBg,     ct.fadeDuration);
                if (ct.targetGround != null) groundFade = new ColorFade(groundColor,     ct.targetGround, ct.fadeDuration);
            }
        }

        if (bgFade != null) {
            bgFade.elapsed += delta;
            float t = Math.min(bgFade.elapsed / bgFade.duration, 1f);
            backgroundColor.set(lerp(bgFade.from.r, bgFade.to.r, t), lerp(bgFade.from.g, bgFade.to.g, t),
                lerp(bgFade.from.b, bgFade.to.b, t), 1f);
            if (t >= 1f) bgFade = null;
        }
        if (groundFade != null) {
            groundFade.elapsed += delta;
            float t = Math.min(groundFade.elapsed / groundFade.duration, 1f);
            groundColor.set(lerp(groundFade.from.r, groundFade.to.r, t), lerp(groundFade.from.g, groundFade.to.g, t),
                lerp(groundFade.from.b, groundFade.to.b, t), 1f);
            if (t >= 1f) groundFade = null;
        }

        // Cull objects that have scrolled off the left edge
        // Return them to pools instead of letting GC collect them
        if (!blocks.isEmpty()) {
            for (int i = blocks.size() - 1; i >= 0; i--) {
                Block b = blocks.get(i);
                if (b.getX() + b.getWidth() < cullX - 200) {
                    blockPool.free(b);
                    blocks.remove(i);
                }
            }
        }
        if (!hazards.isEmpty()) {
            for (int i = hazards.size() - 1; i >= 0; i--) {
                AbstractHazard h = hazards.get(i);
                if (h.getX() + h.getWidth() < cullX) {
                    if (h instanceof Spike) {
                        spikePool.free((Spike) h);
                        activeSpikes.remove(h);
                    }
                    hazards.remove(i);
                }
            }
        }
        if (!portals.isEmpty()) {
            for (int i = portals.size() - 1; i >= 0; i--) {
                AbstractPortal p = portals.get(i);
                if (p.getX() + p.getWidth() < cullX) {
                    if (p instanceof CubePortal) {
                        cubePortalPool.free((CubePortal) p);
                        activeCubePortals.remove(p);
                    } else if (p instanceof ShipPortal) {
                        shipPortalPool.free((ShipPortal) p);
                        activeShipPortals.remove(p);
                    }
                    portals.remove(i);
                }
            }
        }

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
