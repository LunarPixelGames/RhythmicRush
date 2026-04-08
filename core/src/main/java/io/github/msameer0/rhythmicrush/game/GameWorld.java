package io.github.msameer0.rhythmicrush.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.utils.Array;

import io.github.msameer0.rhythmicrush.game.engine.Tickable;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Slope;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.HalfSpike;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.SawBlade;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.AbstractOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.BlackOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.BlueOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.GreenOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.PinkOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.RedOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.orbs.YellowOrb;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.GravityPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.MiniPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.registries.Registries;
import io.github.msameer0.rhythmicrush.game.trigger.AbstractTrigger;
import io.github.msameer0.rhythmicrush.game.trigger.ColorTrigger;
import io.github.msameer0.rhythmicrush.game.trigger.PulseTrigger;

/**
 * Manages core game logic, entity lifecycle, and state for a level.
 *
 * <p>Pool management is delegated to {@link WorldPoolManager}. Color transition
 * state is delegated to {@link ColorStateManager}, which is also the right
 * hook point for TarsosDSP audio-reactive pulses when you add them later.</p>
 */
public class GameWorld implements Tickable {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float COLLISION_LOOKAHEAD = 1400f;
    private static final float POST_END_DELAY      = 2f;

    // ── Sub-systems ───────────────────────────────────────────────────────────
    private final WorldPoolManager pools  = new WorldPoolManager();
    private final ColorStateManager colors = new ColorStateManager();

    // ── Player ────────────────────────────────────────────────────────────────
    private AbstractPlayer player;
    private final float groundY     = 50f;
    private final float scrollSpeed = 320f;

    // ── Game state ────────────────────────────────────────────────────────────
    private boolean playerDead    = false;
    private boolean levelComplete = false;
    private float   worldScrolled = 0f;
    private float   levelEndX     = 0f;
    private float   postEndTimer  = -1f;
    private float   cullX         = 0f;

    // ── Cull / start indices ──────────────────────────────────────────────────
    private int blockCull  = 0, blockStart  = 0;
    private int hazardCull = 0, hazardStart = 0;
    private int portalCull = 0, portalStart = 0;
    private int orbCull    = 0, orbStart    = 0;
    private int triggerIdx = 0;

    // ── Entity arrays ─────────────────────────────────────────────────────────
    private final Array<AbstractPortal> portals = new Array<>();
    private final Array<AbstractHazard> hazards = new Array<>();
    private final Array<Block>          blocks  = new Array<>();
    private final Array<AbstractOrb>    orbs    = new Array<>();

    // Typed subsets for pool-freeing and fast typed access
    private final Array<Spike>         activeSpikes         = new Array<>();
    private final Array<HalfSpike>     activeHalfSpikes     = new Array<>();
    private final Array<SawBlade>      activeSawBlades      = new Array<>();
    private final Array<CubePortal>    activeCubePortals    = new Array<>();
    private final Array<ShipPortal>    activeShipPortals    = new Array<>();
    private final Array<GravityPortal> activeGravityPortals = new Array<>();
    private final Array<MiniPortal>    activeMiniPortals    = new Array<>();

    // ── Triggers ──────────────────────────────────────────────────────────────
    private final Array<AbstractTrigger> triggers = new Array<>();
    private LevelData currentLevelData = null;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameWorld() {
        player = pools.obtainCube().init(100, groundY);
        player.setWorld(this);
        Gdx.app.log("GameWorld", "Player initialized.");
    }

    // ── Player helpers ────────────────────────────────────────────────────────

    public AbstractPlayer obtainPlayer(String typeId) {
        if ("cube".equals(typeId)) return pools.obtainCube();
        if ("ship".equals(typeId)) return pools.obtainShip();
        return Registries.PLAYERS.create(typeId);
    }

    public void setPlayer(AbstractPlayer next) {
        freePlayer();
        player = next;
        next.setWorld(this);
    }

    private void freePlayer() {
        if (player != null) pools.freePlayer(player);
        player = null;
    }

    // ── Color / visual delegates ──────────────────────────────────────────────

    public void startBgFade(Color target, float duration)     { colors.startBgFade(target, duration); }
    public void startGroundFade(Color target, float duration) { colors.startGroundFade(target, duration); }

    public void startBgPulse(Color target, float fadeIn, float hold, float fadeOut) {
        colors.startBgPulse(target, fadeIn, hold, fadeOut);
    }
    public void startGroundPulse(Color target, float fadeIn, float hold, float fadeOut) {
        colors.startGroundPulse(target, fadeIn, hold, fadeOut);
    }

    /** Called once per rendered frame (not per physics tick). */
    public void updateVisuals(float delta) {
        if (playerDead || levelComplete) return;
        colors.update(delta);
    }

    // ── Level loading ─────────────────────────────────────────────────────────

    public void loadLevel(LevelData data) {
        loadLevel(data, 0f, true);
    }

    public void fastForwardTo(float scrolled) {
        worldScrolled = scrolled;
        if (currentLevelData != null) loadLevel(currentLevelData, scrolled, false);
    }

    public void loadLevel(LevelData data, float startScrolled, boolean resetPlayer) {
        Gdx.app.log("GameWorld", "Loading level: " + data.getName() + " at scrolled=" + startScrolled);
        currentLevelData = data;
        freeAllActiveObjects();

        triggers.clear();
        colors.cancelTransitions();
        playerDead    = false;
        levelComplete = false;
        worldScrolled = startScrolled;
        postEndTimer  = -1f;

        // Base colors from level data
        String bg  = (data.getBgColor()     != null && !data.getBgColor().isEmpty())     ? data.getBgColor()     : "1a1a2e";
        String gnd = (data.getGroundColor() != null && !data.getGroundColor().isEmpty()) ? data.getGroundColor() : "16213e";
        colors.setBaseBgColor(hexToColor(bg));
        colors.setBaseGroundColor(hexToColor(gnd));
        colors.setBackgroundColor(hexToColor(bg));
        colors.setGroundColor(hexToColor(gnd));

        // Spawn all level objects
        for (LevelData.ObjectEntry e : data.getObjects()) {
            float rx = e.getX() - startScrolled;
            spawnObject(e, rx, startScrolled);
        }

        levelEndX = data.getLevelEndX();

        // Sort all arrays by X so binary-search culling works correctly
        blocks.sort((a, b2)   -> Float.compare(a.getX(), b2.getX()));
        hazards.sort((a, b2)  -> Float.compare(a.getX(), b2.getX()));
        portals.sort((a, b2)  -> Float.compare(a.getX(), b2.getX()));
        orbs.sort((a, b2)     -> Float.compare(a.getX(), b2.getX()));
        triggers.sort((a, b2) -> Float.compare(a.getWorldX(), b2.getWorldX()));

        // Fast-forward trigger index past already-passed triggers
        triggerIdx = 0;
        float playerWorldX = 100f + startScrolled;
        while (triggerIdx < triggers.size && triggers.get(triggerIdx).getWorldX() <= playerWorldX)
            triggerIdx++;

        if (resetPlayer) {
            freePlayer();
            player = obtainPlayer("cube").init(100, groundY);
            player.worldX = 100f + worldScrolled;
            player.setWorld(this);
        }
    }

    /** Dispatches a single {@link LevelData.ObjectEntry} to the correct spawn path. */
    private void spawnObject(LevelData.ObjectEntry e, float rx, float startScrolled) {
        if (Registries.BLOCKS.has(e.getType())) {
            if (e.getX() + e.getSize() < startScrolled - 200) return;
            BlockType bt = resolveBlockType(e.getBlockType());
            if ("slope".equals(e.getType()))
                blocks.add(pools.obtainSlope().init(rx, e.getY(), e.getSize(), bt, e.getRotation()));
            else
                blocks.add(pools.obtainBlock().init(rx, e.getY(), e.getSize(), bt));

        } else if (Registries.HAZARDS.has(e.getType())) {
            float hW = "saw_blade".equals(e.getType()) ? e.getSize() : 50f;
            if (e.getX() + hW < startScrolled - 100) return;
            spawnHazard(e, rx);

        } else if (Registries.PORTALS.has(e.getType())) {
            if (e.getX() + 50f < startScrolled - 100) return;
            spawnPortal(e, rx);

        } else if (Registries.ORBS.has(e.getType())) {
            if (e.getX() + e.getSize() < startScrolled - 100) return;
            spawnOrb(e, rx);

        } else if (Registries.TRIGGERS.has(e.getType())) {
            spawnTrigger(e);
        }
    }

    private void spawnHazard(LevelData.ObjectEntry e, float rx) {
        switch (e.getType()) {
            case "spike": {
                Spike s = pools.obtainSpike().init(rx, e.getY(), e.getRotation());
                hazards.add(s); activeSpikes.add(s);
                break;
            }
            case "half_spike": {
                HalfSpike hs = pools.obtainHalfSpike().init(rx, e.getY(), e.getRotation());
                hazards.add(hs); activeHalfSpikes.add(hs);
                break;
            }
            case "saw_blade": {
                SawBlade sb = pools.obtainSawBlade().init(rx, e.getY(), e.getSize(), e.getRotation());
                hazards.add(sb); activeSawBlades.add(sb);
                break;
            }
        }
    }

    private void spawnPortal(LevelData.ObjectEntry e, float rx) {
        AbstractPortal p = null;
        switch (e.getType()) {
            case "cube_portal":    p = pools.obtainCubePortal();    activeCubePortals.add((CubePortal) p);       break;
            case "ship_portal":    p = pools.obtainShipPortal();    activeShipPortals.add((ShipPortal) p);       break;
            case "gravity_portal": p = pools.obtainGravityPortal(); activeGravityPortals.add((GravityPortal) p); break;
            case "mini_portal":    p = pools.obtainMiniPortal();    activeMiniPortals.add((MiniPortal) p);       break;
        }
        if (p != null) { p.init(rx, e.getY()); portals.add(p); }
    }

    private void spawnOrb(LevelData.ObjectEntry e, float rx) {
        AbstractOrb orb = null;
        switch (e.getType()) {
            case "yellow_orb": { YellowOrb o = pools.obtainYellowOrb(); o.init(rx, e.getY()); orb = o; break; }
            case "blue_orb":   { BlueOrb o = pools.obtainBlueOrb();   o.init(rx, e.getY()); orb = o; break; }
            case "pink_orb":   { PinkOrb o = pools.obtainPinkOrb();   o.init(rx, e.getY()); orb = o; break; }
            case "red_orb":    { RedOrb o = pools.obtainRedOrb();    o.init(rx, e.getY()); orb = o; break; }
            case "black_orb":  { BlackOrb o = pools.obtainBlackOrb();  o.init(rx, e.getY()); orb = o; break; }
            case "green_orb":  { GreenOrb o = pools.obtainGreenOrb();  o.init(rx, e.getY()); orb = o; break; }
        }
        if (orb != null) orbs.add(orb);
    }

    private void spawnTrigger(LevelData.ObjectEntry e) {
        AbstractTrigger trigger = Registries.TRIGGERS.create(e.getType());
        if (trigger instanceof ColorTrigger) {
            Color targetBg     = parseOptionalHex(e.getTriggerBgColor());
            Color targetGround = parseOptionalHex(e.getTriggerGroundColor());
            ((ColorTrigger) trigger).init(e.getX(), targetBg, targetGround, e.getFadeDuration());
        } else if (trigger instanceof PulseTrigger) {
            Color pulseBg     = parseOptionalHex(e.getPulseBgColor());
            Color pulseGround = parseOptionalHex(e.getPulseGroundColor());
            ((PulseTrigger) trigger).init(e.getX(), pulseBg, pulseGround,
                e.getFadeInTime(), e.getHoldTime(), e.getFadeOutTime());
        }
        triggers.add(trigger);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    public void reset() {
        if (currentLevelData != null) {
            loadLevel(currentLevelData);
        } else {
            freeAllActiveObjects();
            triggers.clear();
            colors.reset();
            playerDead    = false;
            levelComplete = false;
            worldScrolled = 0f;
            postEndTimer  = -1f;
            levelEndX     = 0f;
            freePlayer();
            player = obtainPlayer("cube").init(100, groundY);
            player.setWorld(this);
        }
    }

    // ── Tickable ──────────────────────────────────────────────────────────────

    @Override
    public boolean onInput(boolean held) {
        if (player != null) {
            player.setJumpHeld(held);
            return player.isGrounded() || !held;
        }
        return true;
    }

    @Override
    public void tick(float delta) { update(delta); }

    // ── Main update ───────────────────────────────────────────────────────────

    public void update(float delta) {
        if (playerDead || levelComplete) return;

        player.update(delta, groundY);

        for (int i = portalCull; i < portals.size; i++) portals.get(i).updatePosition(scrollSpeed, delta);
        for (int i = hazardCull; i < hazards.size; i++) hazards.get(i).updatePosition(scrollSpeed, delta);
        for (int i = blockCull;  i < blocks.size;  i++) blocks.get(i).updatePosition(scrollSpeed, delta);
        for (int i = orbCull;    i < orbs.size;    i++) orbs.get(i).updatePosition(scrollSpeed, delta);

        final float px       = player.x;
        final float rangeMin = px - 300f;
        final float rangeMax = px + COLLISION_LOOKAHEAD;

        // Advance start indices
        if (blockStart  < blockCull)  blockStart  = blockCull;
        if (hazardStart < hazardCull) hazardStart = hazardCull;
        if (portalStart < portalCull) portalStart = portalCull;
        if (orbStart    < orbCull)    orbStart    = orbCull;

        while (blockStart  < blocks.size  && blocks.get(blockStart).getX()   + blocks.get(blockStart).getWidth()   < rangeMin) blockStart++;
        while (hazardStart < hazards.size && hazards.get(hazardStart).getX() + hazards.get(hazardStart).getWidth() < rangeMin) hazardStart++;
        while (portalStart < portals.size && portals.get(portalStart).getX() + portals.get(portalStart).getWidth() < rangeMin) portalStart++;
        while (orbStart    < orbs.size    && orbs.get(orbStart).getX()       + orbs.get(orbStart).getWidth()       < rangeMin) orbStart++;

        // Block collisions
        for (int i = blockStart; i < blocks.size; i++) {
            Block b = blocks.get(i);
            if (b.getX() > rangeMax) break;
            b.tryTouch(player);
        }

        // Portal interactions
        for (int i = portalStart; i < portals.size; i++) {
            AbstractPortal portal = portals.get(i);
            if (portal.getX() > rangeMax) break;
            if (!portal.tryTouch(player)) continue;
            handlePortalActivation(portal);
        }

        // Hazard collisions
        for (int i = hazardStart; i < hazards.size; i++) {
            AbstractHazard h = hazards.get(i);
            if (h.getX() > rangeMax) break;
            h.tryTouch(player);
        }

        // Orb interactions
        for (int i = orbStart; i < orbs.size; i++) {
            AbstractOrb orb = orbs.get(i);
            if (orb.getX() > rangeMax) break;
            if (orb.getBounds().overlaps(player.getBounds())) {
                if (player.isJumpHeld() && !player.isJumpConsumed()) orb.tryActivate(player);
            } else {
                orb.resetOverlap();
            }
        }

        player.tryJump();

        worldScrolled += scrollSpeed * delta;
        player.worldX  = 100f + worldScrolled;

        // Fire triggers
        while (triggerIdx < triggers.size) {
            AbstractTrigger t = triggers.get(triggerIdx);
            if (player.worldX < t.getWorldX()) break;
            t.setFired(true);
            t.fire(this);
            triggerIdx++;
        }

        player.postUpdate();

        // Level-end timer
        if (levelEndX > 0 && worldScrolled >= levelEndX && postEndTimer < 0) postEndTimer = 0f;
        if (postEndTimer >= 0) {
            postEndTimer += delta;
            if (postEndTimer >= POST_END_DELAY && !levelComplete) {
                Gdx.app.log("GameWorld", "Level completed!");
                levelComplete = true;
            }
        }
    }

    private void handlePortalActivation(AbstractPortal portal) {
        if (portal instanceof GravityPortal) {
            player.setGravityFlipped(!player.isGravityFlipped());
        } else if (portal instanceof MiniPortal) {
            player.setMini(!player.isMini());
        } else {
            AbstractPlayer next = null;
            if (portal instanceof CubePortal)
                next = obtainPlayer("cube").init(player.getX(), player.getY());
            else if (portal instanceof ShipPortal)
                next = obtainPlayer("ship").init(player.getX(), player.getY());
            if (next != null) {
                next.setWorld(this);
                next.copyState(player);
                next.x = player.x;
                next.setY(player.y);
                freePlayer();
                player = next;
            }
        }
    }

    // ── Culling ───────────────────────────────────────────────────────────────

    public void cull() {
        final float threshold = player.x - 500f;

        while (blockCull < blocks.size) {
            Block b = blocks.get(blockCull);
            if (b.getX() + b.getWidth() >= threshold - 200) break;
            pools.freeBlock(b);
            blockCull++;
        }

        while (hazardCull < hazards.size) {
            AbstractHazard h = hazards.get(hazardCull);
            if (h.getX() + h.getWidth() >= threshold) break;
            if (h instanceof Spike)          activeSpikes.removeValue((Spike) h, true);
            else if (h instanceof HalfSpike) activeHalfSpikes.removeValue((HalfSpike) h, true);
            else if (h instanceof SawBlade)  activeSawBlades.removeValue((SawBlade) h, true);
            pools.freeHazard(h);
            hazardCull++;
        }

        while (portalCull < portals.size) {
            AbstractPortal p = portals.get(portalCull);
            if (p.getX() + p.getWidth() >= threshold) break;
            if (p instanceof CubePortal)         activeCubePortals.removeValue((CubePortal) p, true);
            else if (p instanceof ShipPortal)    activeShipPortals.removeValue((ShipPortal) p, true);
            else if (p instanceof GravityPortal) activeGravityPortals.removeValue((GravityPortal) p, true);
            else if (p instanceof MiniPortal)    activeMiniPortals.removeValue((MiniPortal) p, true);
            pools.freePortal(p);
            portalCull++;
        }

        while (orbCull < orbs.size) {
            AbstractOrb o = orbs.get(orbCull);
            if (o.getX() + o.getWidth() >= threshold) break;
            pools.freeOrb(o);
            orbCull++;
        }
    }

    // ── Death ─────────────────────────────────────────────────────────────────

    public void playerDied() {
        if (!playerDead) {
            Gdx.app.log("GameWorld", "Player died.");
            playerDead = true;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void freeAllActiveObjects() {
        pools.freeAll(
            blocks, hazards, portals, orbs,
            activeSpikes, activeHalfSpikes, activeSawBlades,
            activeCubePortals, activeShipPortals, activeGravityPortals, activeMiniPortals,
            blockCull, hazardCull, portalCull, orbCull);
        blockCull  = 0; blockStart  = 0;
        hazardCull = 0; hazardStart = 0;
        portalCull = 0; portalStart = 0;
        orbCull    = 0; orbStart    = 0;
    }

    private static BlockType resolveBlockType(String textureName) {
        if (textureName != null)
            for (BlockType t : BlockType.values())
                if (t.textureName.equals(textureName)) return t;
        return BlockType.DEFAULT;
    }

    private static Color parseOptionalHex(String hex) {
        return (hex != null && !hex.isEmpty()) ? hexToColor(hex) : null;
    }

    public static Color hexToColor(String hex) {
        if (hex == null || hex.isEmpty()) return new Color(0, 0, 0, 1);
        if (hex.startsWith("#")) hex = hex.substring(1);
        long val = Long.parseLong(hex, 16);
        float r = ((val >> 16) & 0xFF) / 255f;
        float g = ((val >>  8) & 0xFF) / 255f;
        float b = (val         & 0xFF) / 255f;
        return new Color(r, g, b, 1f);
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public AbstractPlayer getPlayer()      { return player; }
    public float getGroundY()              { return groundY; }
    public float getScrollSpeed()          { return scrollSpeed; }
    public boolean isPlayerDead()          { return playerDead; }
    public boolean isLevelComplete()       { return levelComplete; }
    public float getWorldScrolled()        { return worldScrolled; }
    public int getTriggerIdx()             { return triggerIdx; }
    public int getOrbCull()                { return orbCull; }

    public float getProgress() {
        if (levelEndX <= 0) return 0f;
        return Math.min(worldScrolled / levelEndX, 1f);
    }

    public Array<AbstractPortal> getPortals() { return portals; }
    public Array<AbstractHazard> getHazards() { return hazards; }
    public Array<Block>          getBlocks()  { return blocks; }
    public Array<AbstractOrb>    getOrbs()    { return orbs; }

    // Color getters — forwarded from ColorStateManager
    public Color getBackgroundColor() { return colors.getBackgroundColor(); }
    public Color getGroundColor()     { return colors.getGroundColor(); }
    public Color getBaseBgColor()     { return colors.getBaseBgColor(); }
    public Color getBaseGroundColor() { return colors.getBaseGroundColor(); }

    // Color setters — forwarded from ColorStateManager
    public void setBackgroundColor(Color c) { colors.setBackgroundColor(c); }
    public void setGroundColor(Color c)     { colors.setGroundColor(c); }
    public void setBaseBgColor(Color c)     { colors.setBaseBgColor(c); }
    public void setBaseGroundColor(Color c) { colors.setBaseGroundColor(c); }

    public void setCullX(float x)             { cullX = x; }
    public void setWorldScrolled(float s)     { worldScrolled = s; }
    public void setTriggerIdx(int idx)        { triggerIdx = idx; }
    public void setLevelComplete(boolean lc)  { levelComplete = lc; }

    public void addPortal(AbstractPortal p)   { portals.add(p); }
    public void addHazard(AbstractHazard h)   { hazards.add(h); }
    public void addBlock(Block b)             { blocks.add(b); }
}
