package io.github.msameer0.rhythmicrush.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import io.github.msameer0.rhythmicrush.game.engine.ObjectPool;
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
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.registries.Registries;
import io.github.msameer0.rhythmicrush.game.trigger.AbstractTrigger;
import io.github.msameer0.rhythmicrush.game.trigger.ColorTrigger;
import io.github.msameer0.rhythmicrush.game.trigger.PulseTrigger;

import com.badlogic.gdx.utils.Array;

/**
 * Manages the core game logic, entity lifecycle, and state for a level in Rhythmic Rush.
 */
public class GameWorld implements Tickable {

    private AbstractPlayer player;
    private final float groundY = 50f;
    private final float scrollSpeed = 320f;
    private boolean playerDead = false;
    private float cullX = 0f;

    private int blockStart = 0;
    private int hazardStart = 0;
    private int portalStart = 0;
    private int orbStart = 0;

    private int blockCull = 0;
    private int hazardCull = 0;
    private int portalCull = 0;
    private int orbCull = 0;
    private int triggerIdx = 0;

    private static final float COLLISION_LOOKAHEAD = 1400f;

    private final Array<AbstractPortal> portals = new Array<>();
    private final Array<AbstractHazard> hazards = new Array<>();
    private final Array<Block> blocks = new Array<>();
    private final Array<AbstractOrb> orbs = new Array<>();

    // ── Object pools ────────────────────────────────────────────────────────────

    private final ObjectPool<Block> blockPool = new ObjectPool<Block>() {
        @Override protected Block create() { return new Block(); }
        @Override protected void reset(Block b) { b.reset(); }
    };
    private final ObjectPool<Slope> slopePool = new ObjectPool<Slope>() {
        @Override protected Slope create() { return new Slope(); }
        @Override protected void reset(Slope s) { s.reset(); }
    };
    private final ObjectPool<Spike> spikePool = new ObjectPool<Spike>() {
        @Override protected Spike create() { return new Spike(); }
        @Override protected void reset(Spike s) {}
    };
    private final ObjectPool<HalfSpike> halfSpikePool = new ObjectPool<HalfSpike>() {
        @Override protected HalfSpike create() { return new HalfSpike(); }
        @Override protected void reset(HalfSpike s) {}
    };
    private final ObjectPool<SawBlade> sawBladePool = new ObjectPool<SawBlade>() {
        @Override protected SawBlade create() { return new SawBlade(); }
        @Override protected void reset(SawBlade s) {}
    };
    private final ObjectPool<CubePortal> cubePortalPool = new ObjectPool<CubePortal>() {
        @Override protected CubePortal create() { return new CubePortal(); }
        @Override protected void reset(CubePortal p) {}
    };
    private final ObjectPool<ShipPortal> shipPortalPool = new ObjectPool<ShipPortal>() {
        @Override protected ShipPortal create() { return new ShipPortal(); }
        @Override protected void reset(ShipPortal p) {}
    };
    private final ObjectPool<GravityPortal> gravityPortalPool = new ObjectPool<GravityPortal>() {
        @Override protected GravityPortal create() { return new GravityPortal(); }
        @Override protected void reset(GravityPortal p) {}
    };
    private final ObjectPool<MiniPortal> miniPortalPool = new ObjectPool<MiniPortal>() {
        @Override protected MiniPortal create() { return new MiniPortal(); }
        @Override protected void reset(MiniPortal p) {}
    };
    private final ObjectPool<Cube> cubePool = new ObjectPool<Cube>() {
        @Override protected Cube create() { return new Cube(); }
        @Override protected void reset(Cube c) {}
    };
    private final ObjectPool<Ship> shipPool = new ObjectPool<Ship>() {
        @Override protected Ship create() { return new Ship(); }
        @Override protected void reset(Ship s) {}
    };
    private final ObjectPool<YellowOrb> yellowOrbPool = new ObjectPool<YellowOrb>() {
        @Override protected YellowOrb create() { return new YellowOrb(); }
        @Override protected void reset(YellowOrb o) { o.reset(); }
    };
    private final ObjectPool<BlueOrb> blueOrbPool = new ObjectPool<BlueOrb>() {
        @Override protected BlueOrb create() { return new BlueOrb(); }
        @Override protected void reset(BlueOrb o) {o.reset();}
    };
    private final ObjectPool<PinkOrb> pinkOrbPool = new ObjectPool<PinkOrb>() {
        @Override protected PinkOrb create() { return new PinkOrb(); }
        @Override protected void reset(PinkOrb o) { o.reset(); }
    };
    private final ObjectPool<RedOrb> redOrbPool = new ObjectPool<RedOrb>() {
        @Override protected RedOrb create() { return new RedOrb(); }
        @Override protected void reset(RedOrb o) { o.reset(); }
    };
    private final ObjectPool<BlackOrb> blackOrbPool = new ObjectPool<BlackOrb>() {
        @Override protected BlackOrb create() { return new BlackOrb(); }
        @Override protected void reset(BlackOrb o) { o.reset(); }
    };
    private final ObjectPool<GreenOrb> greenOrbPool = new ObjectPool<GreenOrb>() {
        @Override protected GreenOrb create() { return new GreenOrb(); }
        @Override protected void reset(GreenOrb o) { o.reset(); }
    };

    // ── Active-object arrays (typed subsets for fast typed access) ───────────

    private final Array<Spike> activeSpikes = new Array<>();
    private final Array<HalfSpike> activeHalfSpikes = new Array<>();
    private final Array<SawBlade> activeSawBlades = new Array<>();
    private final Array<CubePortal> activeCubePortals = new Array<>();
    private final Array<ShipPortal> activeShipPortals = new Array<>();
    private final Array<GravityPortal> activeGravityPortals = new Array<>();
    private final Array<MiniPortal> activeMiniPortals = new Array<>();

    // ── Color state ──────────────────────────────────────────────────────────

    private static class ColorFade {
        final Color from = new Color();
        final Color to = new Color();
        float duration, elapsed;
        boolean active = false;

        void init(Color from, Color to, float duration) {
            this.from.set(from);
            this.to.set(to);
            this.duration = duration;
            this.elapsed = 0f;
            this.active = true;
        }
    }

    private static class ColorPulse {
        final Color target = new Color();
        float fadeIn, hold, fadeOut, elapsed;
        boolean active = false;

        void init(Color target, float fadeIn, float hold, float fadeOut) {
            this.target.set(target);
            this.fadeIn = fadeIn;
            this.hold = hold;
            this.fadeOut = fadeOut;
            this.elapsed = 0f;
            this.active = true;
        }

        float getIntensity() {
            if (!active) return 0f;
            if (elapsed < fadeIn) return fadeIn > 0 ? elapsed / fadeIn : 1f;
            if (elapsed < fadeIn + hold) return 1f;
            if (elapsed < fadeIn + hold + fadeOut)
                return fadeOut > 0 ? 1f - (elapsed - fadeIn - hold) / fadeOut : 0f;
            return 0f;
        }

        void update(float delta) {
            if (!active) return;
            elapsed += delta;
            if (elapsed >= fadeIn + hold + fadeOut) active = false;
        }
    }

    private final ColorFade bgFade = new ColorFade();
    private final ColorFade groundFade = new ColorFade();
    private final ColorPulse bgPulse = new ColorPulse();
    private final ColorPulse groundPulse = new ColorPulse();

    private final Array<AbstractTrigger> triggers = new Array<>();

    private LevelData currentLevelData = null;
    private float levelEndX = 0f;
    private float worldScrolled = 0f;
    private float postEndTimer = -1f;
    private boolean levelComplete = false;
    private static final float POST_END_DELAY = 2f;

    private Color baseBgColor = new Color(0.1f, 0.1f, 0.18f, 1f);
    private Color baseGroundColor = new Color(0.09f, 0.13f, 0.24f, 1f);
    private Color backgroundColor = new Color(baseBgColor);
    private Color groundColor = new Color(baseGroundColor);

    // ── Constructor ──────────────────────────────────────────────────────────

    public GameWorld() {
        player = cubePool.obtain().init(100, groundY);
        player.setWorld(this);
        Gdx.app.log("GameWorld", "Player initialized.");
    }

    // ── Player helpers ───────────────────────────────────────────────────────

    public Cube obtainCube(float x, float y, float vy, boolean jumpHeld) {
        Cube c = cubePool.obtain().init(x, y, vy, jumpHeld);
        c.setWorld(this);
        return c;
    }

    public Cube obtainCube(float x, float y) {
        return obtainCube(x, y, 0, false);
    }

    public Ship obtainShip(float x, float y, float vy, boolean jumpHeld) {
        Ship s = shipPool.obtain().init(x, y, vy, jumpHeld);
        s.setWorld(this);
        return s;
    }

    public Ship obtainShip(float x, float y) {
        return obtainShip(x, y, 0, false);
    }

    public AbstractPlayer obtainPlayer(String typeId) {
        if ("cube".equals(typeId)) return cubePool.obtain();
        if ("ship".equals(typeId)) return shipPool.obtain();
        return Registries.PLAYERS.create(typeId);
    }

    // ── Color helpers ────────────────────────────────────────────────────────

    public void startBgFade(Color target, float duration) { bgFade.init(baseBgColor, target, duration); }
    public void startGroundFade(Color target, float duration) { groundFade.init(baseGroundColor, target, duration); }
    public void startBgPulse(Color target, float fadeIn, float hold, float fadeOut) { bgPulse.init(target, fadeIn, hold, fadeOut); }
    public void startGroundPulse(Color target, float fadeIn, float hold, float fadeOut) { groundPulse.init(target, fadeIn, hold, fadeOut); }

    // ── Getters / setters ────────────────────────────────────────────────────

    public Color getBaseBgColor() { return baseBgColor; }
    public Color getBaseGroundColor() { return baseGroundColor; }
    public int getTriggerIdx() { return triggerIdx; }

    public void setBaseBgColor(Color c) { baseBgColor.set(c); }
    public void setBaseGroundColor(Color c) { baseGroundColor.set(c); }
    public void setBackgroundColor(Color c) { backgroundColor.set(c); }
    public void setGroundColor(Color c) { groundColor.set(c); }
    public void setTriggerIdx(int idx) { triggerIdx = idx; }
    public void setWorldScrolled(float s) { this.worldScrolled = s; }
    public void setLevelComplete(boolean levelComplete) { this.levelComplete = levelComplete; }

    public float getScrollSpeed() { return scrollSpeed; }
    public AbstractPlayer getPlayer() { return player; }
    public float getGroundY() { return groundY; }
    public boolean isPlayerDead() { return playerDead; }
    public boolean isLevelComplete() { return levelComplete; }
    public float getWorldScrolled() { return worldScrolled; }
    public float getProgress() {
        if (levelEndX <= 0) return 0f;
        return Math.min(worldScrolled / levelEndX, 1f);
    }

    public Array<AbstractPortal> getPortals() { return portals; }
    public Array<AbstractHazard> getHazards() { return hazards; }
    public Array<Block> getBlocks() { return blocks; }
    public Array<AbstractOrb> getOrbs() { return orbs; }
    public int getOrbCull() { return orbCull; }

    public Color getBackgroundColor() { return backgroundColor; }
    public Color getGroundColor() { return groundColor; }

    public void addPortal(AbstractPortal portal) { portals.add(portal); }
    public void addHazard(AbstractHazard hazard) { hazards.add(hazard); }
    public void addBlock(Block block) { blocks.add(block); }
    public void setCullX(float x) { cullX = x; }

    public void setPlayer(AbstractPlayer next) {
        freePlayer();
        this.player = next;
        next.setWorld(this);
    }

    // ── Level loading ────────────────────────────────────────────────────────

    public void fastForwardTo(float scrolled) {
        this.worldScrolled = scrolled;
        if (currentLevelData != null) loadLevel(currentLevelData, scrolled, false);
    }

    public void loadLevel(LevelData data) {
        loadLevel(data, 0f, true);
    }

    public void loadLevel(LevelData data, float startScrolled, boolean resetPlayer) {
        Gdx.app.log("GameWorld", "Loading level: " + data.getName() + " at scrolled=" + startScrolled);
        currentLevelData = data;
        freeAllActiveObjects();

        triggers.clear();
        bgFade.active = false;
        groundFade.active = false;
        bgPulse.active = false;
        groundPulse.active = false;
        playerDead = false;
        levelComplete = false;
        worldScrolled = startScrolled;
        postEndTimer = -1f;

        String bg  = (data.getBgColor() != null && !data.getBgColor().isEmpty())     ? data.getBgColor() : "1a1a2e";
        String gnd = (data.getGroundColor() != null && !data.getGroundColor().isEmpty()) ? data.getGroundColor() : "16213e";
        baseBgColor.set(hexToColor(bg));
        baseGroundColor.set(hexToColor(gnd));
        backgroundColor.set(baseBgColor);
        groundColor.set(baseGroundColor);

        for (LevelData.ObjectEntry e : data.getObjects()) {
            float rx = e.getX() - startScrolled;

            if (Registries.BLOCKS.has(e.getType())) {
                if (e.getX() + e.getSize() < startScrolled - 200) continue;
                BlockType bt = BlockType.DEFAULT;
                if (e.getBlockType() != null)
                    for (BlockType t : BlockType.values())
                        if (t.textureName.equals(e.getBlockType())) { bt = t; break; }
                if ("slope".equals(e.getType())) blocks.add(slopePool.obtain().init(rx, e.getY(), e.getSize(), bt, e.getRotation()));
                else                        blocks.add(blockPool.obtain().init(rx, e.getY(), e.getSize(), bt));

            } else if (Registries.HAZARDS.has(e.getType())) {
                float hW = "saw_blade".equals(e.getType()) ? e.getSize() : 50f;
                if (e.getX() + hW < startScrolled - 100) continue;
                if ("spike".equals(e.getType())) {
                    Spike s = spikePool.obtain().init(rx, e.getY(), e.getRotation());
                    hazards.add(s); activeSpikes.add(s);
                } else if ("half_spike".equals(e.getType())) {
                    HalfSpike hs = halfSpikePool.obtain().init(rx, e.getY(), e.getRotation());
                    hazards.add(hs); activeHalfSpikes.add(hs);
                } else if ("saw_blade".equals(e.getType())) {
                    SawBlade sb = sawBladePool.obtain().init(rx, e.getY(), e.getSize(), e.getRotation());
                    hazards.add(sb); activeSawBlades.add(sb);
                }

            } else if (Registries.PORTALS.has(e.getType())) {
                if (e.getX() + 50f < startScrolled - 100) continue;
                AbstractPortal p = null;
                if ("cube_portal".equals(e.getType()))    { p = cubePortalPool.obtain();    activeCubePortals.add((CubePortal) p); }
                else if ("ship_portal".equals(e.getType()))    { p = shipPortalPool.obtain();    activeShipPortals.add((ShipPortal) p); }
                else if ("gravity_portal".equals(e.getType())) { p = gravityPortalPool.obtain(); activeGravityPortals.add((GravityPortal) p); }
                else if ("mini_portal".equals(e.getType()))    { p = miniPortalPool.obtain();    activeMiniPortals.add((MiniPortal) p); }
                if (p != null) { p.init(rx, e.getY()); portals.add(p); }

            } else if (Registries.ORBS.has(e.getType())) {
                // Skip orbs that are already well behind the start point
                if (e.getX() + e.getSize() < startScrolled - 100) continue;
                AbstractOrb orb = null;
                if ("yellow_orb".equals(e.getType())) {
                    YellowOrb yo = yellowOrbPool.obtain();
                    yo.init(rx, e.getY());
                    orb = yo;
                } else if ("blue_orb".equals(e.getType())) {
                    BlueOrb bo = blueOrbPool.obtain();
                    bo.init(rx, e.getY());
                    orb = bo;
                } else if ("pink_orb".equals(e.getType())) {
                    PinkOrb po = pinkOrbPool.obtain();
                    po.init(rx, e.getY());
                    orb = po;
                } else if ("red_orb".equals(e.getType())) {
                    RedOrb ro = redOrbPool.obtain();
                    ro.init(rx, e.getY());
                    orb = ro;
                } else if ("black_orb".equals(e.getType())) {
                    BlackOrb bo = blackOrbPool.obtain();
                    bo.init(rx, e.getY());
                    orb = bo;
                } else if ("green_orb".equals(e.getType())) {
                    GreenOrb go = greenOrbPool.obtain();
                    go.init(rx, e.getY());
                    orb = go;
                }
                if (orb != null) orbs.add(orb);

            } else if (Registries.TRIGGERS.has(e.getType())) {
                AbstractTrigger trigger = Registries.TRIGGERS.create(e.getType());
                if (trigger instanceof ColorTrigger) {
                    Color targetBg     = (e.getTriggerBgColor() != null && !e.getTriggerBgColor().isEmpty())     ? hexToColor(e.getTriggerBgColor())     : null;
                    Color targetGround = (e.getTriggerGroundColor() != null && !e.getTriggerGroundColor().isEmpty()) ? hexToColor(e.getTriggerGroundColor()) : null;
                    ((ColorTrigger) trigger).init(e.getX(), targetBg, targetGround, e.getFadeDuration());
                } else if (trigger instanceof PulseTrigger) {
                    Color pulseBg     = (e.getPulseBgColor() != null && !e.getPulseBgColor().isEmpty())     ? hexToColor(e.getPulseBgColor())     : null;
                    Color pulseGround = (e.getPulseGroundColor() != null && !e.getPulseGroundColor().isEmpty()) ? hexToColor(e.getPulseGroundColor()) : null;
                    ((PulseTrigger) trigger).init(e.getX(), pulseBg, pulseGround, e.getFadeInTime(), e.getHoldTime(), e.getFadeOutTime());
                }
                triggers.add(trigger);
            }
        }

        levelEndX = data.getLevelEndX();

        blocks.sort((a, b2)   -> Float.compare(a.getX(), b2.getX()));
        hazards.sort((a, b2)  -> Float.compare(a.getX(), b2.getX()));
        portals.sort((a, b2)  -> Float.compare(a.getX(), b2.getX()));
        orbs.sort((a, b2)     -> Float.compare(a.getX(), b2.getX()));
        triggers.sort((a, b2) -> Float.compare(a.worldX,  b2.worldX));

        triggerIdx = 0;
        float playerWorldX = 100f + startScrolled;
        while (triggerIdx < triggers.size && triggers.get(triggerIdx).worldX <= playerWorldX)
            triggerIdx++;

        if (resetPlayer) {
            freePlayer();
            player = obtainPlayer("cube").init(100, groundY);
            player.worldX = 100f + worldScrolled;
            player.setWorld(this);
        }
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    public void reset() {
        if (currentLevelData != null) {
            loadLevel(currentLevelData);
        } else {
            freeAllActiveObjects();
            triggers.clear();
            bgFade.active = false;
            groundFade.active = false;
            bgPulse.active = false;
            groundPulse.active = false;
            playerDead = false;
            levelComplete = false;
            worldScrolled = 0f;
            postEndTimer = -1f;
            levelEndX = 0f;
            baseBgColor.set(0.1f, 0.1f, 0.18f, 1f);
            baseGroundColor.set(0.09f, 0.13f, 0.24f, 1f);
            backgroundColor.set(baseBgColor);
            groundColor.set(baseGroundColor);
            freePlayer();
            player = obtainPlayer("cube").init(100, groundY);
            player.setWorld(this);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void freePlayer() {
        if (player instanceof Cube) cubePool.free((Cube) player);
        else if (player instanceof Ship) shipPool.free((Ship) player);
        player = null;
    }

    private void freeAllActiveObjects() {
        for (int i = blockCull; i < blocks.size; i++) {
            Block b = blocks.get(i);
            if (b instanceof Slope) slopePool.free((Slope) b);
            else blockPool.free(b);
        }
        blocks.clear();

        for (int i = hazardCull; i < hazards.size; i++) {
            AbstractHazard h = hazards.get(i);
            if (h instanceof Spike)     spikePool.free((Spike) h);
            else if (h instanceof HalfSpike) halfSpikePool.free((HalfSpike) h);
            else if (h instanceof SawBlade)  sawBladePool.free((SawBlade) h);
        }
        hazards.clear();
        activeSpikes.clear();
        activeHalfSpikes.clear();
        activeSawBlades.clear();

        for (int i = portalCull; i < portals.size; i++) {
            AbstractPortal p = portals.get(i);
            if (p instanceof CubePortal)    cubePortalPool.free((CubePortal) p);
            else if (p instanceof ShipPortal)    shipPortalPool.free((ShipPortal) p);
            else if (p instanceof GravityPortal) gravityPortalPool.free((GravityPortal) p);
            else if (p instanceof MiniPortal)    miniPortalPool.free((MiniPortal) p);
        }
        portals.clear();
        activeCubePortals.clear();
        activeShipPortals.clear();
        activeGravityPortals.clear();
        activeMiniPortals.clear();

        // Free orbs
        for (int i = orbCull; i < orbs.size; i++) {
            AbstractOrb o = orbs.get(i);
            if (o instanceof YellowOrb) yellowOrbPool.free((YellowOrb) o);
            if (o instanceof BlueOrb) blueOrbPool.free((BlueOrb) o);
            if (o instanceof PinkOrb) pinkOrbPool.free((PinkOrb) o);
            if (o instanceof RedOrb) redOrbPool.free((RedOrb) o);
            if (o instanceof BlackOrb) blackOrbPool.free((BlackOrb) o);
            if (o instanceof GreenOrb) greenOrbPool.free((GreenOrb) o);
        }
        orbs.clear();

        blockCull  = 0; hazardCull = 0; portalCull = 0; orbCull = 0;
        blockStart = 0; hazardStart = 0; portalStart = 0; orbStart = 0;
    }

    // ── Tickable ─────────────────────────────────────────────────────────────

    @Override
    public boolean onInput(boolean held) {
        if (player != null) {
            player.setJumpHeld(held);
            return player.isGrounded() || !held;
        }
        return true;
    }

    @Override
    public void tick(float delta) {
        update(delta);
    }

    // ── Main update ──────────────────────────────────────────────────────────

    public void update(float delta) {
        if (playerDead || levelComplete) return;

        player.update(delta, groundY);

        // Position updates
        for (int i = portalCull;  i < portals.size;  i++) portals.get(i).updatePosition(scrollSpeed, delta);
        for (int i = hazardCull;  i < hazards.size;  i++) hazards.get(i).updatePosition(scrollSpeed, delta);
        for (int i = blockCull;   i < blocks.size;   i++) blocks.get(i).updatePosition(scrollSpeed, delta);
        for (int i = orbCull;     i < orbs.size;     i++) orbs.get(i).updatePosition(scrollSpeed, delta);

        final float px       = player.x;
        final float rangeMin = px - 300f;
        final float rangeMax = px + COLLISION_LOOKAHEAD;

        // Advance start indices past culled zone
        if (blockStart  < blockCull)  blockStart  = blockCull;
        if (hazardStart < hazardCull) hazardStart = hazardCull;
        if (portalStart < portalCull) portalStart = portalCull;
        if (orbStart    < orbCull)    orbStart    = orbCull;

        // Skip objects already behind the player
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
            if (portal.tryTouch(player)) {
                if (portal instanceof GravityPortal) {
                    player.setGravityFlipped(!player.isGravityFlipped());
                } else if (portal instanceof MiniPortal) {
                    player.setMini(!player.isMini());
                } else {
                    AbstractPlayer next = null;
                    if (portal instanceof CubePortal) next = obtainPlayer("cube").init(player.getX(), player.getY());
                    else if (portal instanceof ShipPortal) next = obtainPlayer("ship").init(player.getX(), player.getY());
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
        }

        // Hazard collisions
        for (int i = hazardStart; i < hazards.size; i++) {
            AbstractHazard h = hazards.get(i);
            if (h.getX() > rangeMax) break;
            h.tryTouch(player);
        }

        // Orb interactions — only activate when the player is holding jump
        for (int i = orbStart; i < orbs.size; i++) {
            AbstractOrb orb = orbs.get(i);
            if (orb.getX() > rangeMax) break;
            if (orb.getBounds().overlaps(player.getBounds())) {
                if (player.isJumpHeld() && !player.isJumpConsumed()) {
                    orb.tryActivate(player);
                }
            } else {
                // Player left the orb — allow multi-activate orbs to fire again next entry
                orb.resetOverlap();
            }
        }

        player.tryJump();

        worldScrolled += scrollSpeed * delta;
        player.worldX = 100f + worldScrolled;

        // Triggers
        while (triggerIdx < triggers.size) {
            AbstractTrigger t = triggers.get(triggerIdx);
            if (player.worldX < t.worldX) break;
            t.fired = true;
            t.fire(this);
            triggerIdx++;
        }

        player.postUpdate();

        // Level end
        if (levelEndX > 0 && worldScrolled >= levelEndX) {
            if (postEndTimer < 0) postEndTimer = 0f;
        }
        if (postEndTimer >= 0) {
            postEndTimer += delta;
            if (postEndTimer >= POST_END_DELAY && !levelComplete) {
                Gdx.app.log("GameWorld", "Level completed!");
                levelComplete = true;
            }
        }
    }

    // ── Culling ──────────────────────────────────────────────────────────────

    public void cull() {
        final float px = player.x;
        final float cullX = px - 500f;

        // Culling — blocks
        while (blockCull < blocks.size) {
            Block b = blocks.get(blockCull);
            if (b.getX() + b.getWidth() >= cullX - 200) break;
            if (b instanceof Slope) slopePool.free((Slope) b);
            else blockPool.free(b);
            blockCull++;
        }
        // Culling — hazards
        while (hazardCull < hazards.size) {
            AbstractHazard h = hazards.get(hazardCull);
            if (h.getX() + h.getWidth() >= cullX) break;
            if (h instanceof Spike)          { spikePool.free((Spike) h);         activeSpikes.removeValue((Spike) h, true); }
            else if (h instanceof HalfSpike) { halfSpikePool.free((HalfSpike) h); activeHalfSpikes.removeValue((HalfSpike) h, true); }
            else if (h instanceof SawBlade)  { sawBladePool.free((SawBlade) h);   activeSawBlades.removeValue((SawBlade) h, true); }
            hazardCull++;
        }
        // Culling — portals
        while (portalCull < portals.size) {
            AbstractPortal p = portals.get(portalCull);
            if (p.getX() + p.getWidth() >= cullX) break;
            if (p instanceof CubePortal)         { cubePortalPool.free((CubePortal) p);       activeCubePortals.removeValue((CubePortal) p, true); }
            else if (p instanceof ShipPortal)    { shipPortalPool.free((ShipPortal) p);       activeShipPortals.removeValue((ShipPortal) p, true); }
            else if (p instanceof GravityPortal) { gravityPortalPool.free((GravityPortal) p); activeGravityPortals.removeValue((GravityPortal) p, true); }
            else if (p instanceof MiniPortal)    { miniPortalPool.free((MiniPortal) p);       activeMiniPortals.removeValue((MiniPortal) p, true); }
            portalCull++;
        }
        // Culling — orbs
        while (orbCull < orbs.size) {
            AbstractOrb o = orbs.get(orbCull);
            if (o.getX() + o.getWidth() >= cullX) break;
            if (o instanceof YellowOrb) yellowOrbPool.free((YellowOrb) o);
            if (o instanceof BlueOrb) blueOrbPool.free((BlueOrb) o);
            if (o instanceof PinkOrb) pinkOrbPool.free((PinkOrb) o);
            if (o instanceof RedOrb) redOrbPool.free((RedOrb) o);
            if (o instanceof BlackOrb) blackOrbPool.free((BlackOrb) o);
            if (o instanceof GreenOrb) greenOrbPool.free((GreenOrb) o);
            orbCull++;
        }
    }

    /** Call once per rendered frame (not per physics tick) — updates visual color transitions. */
    public void updateVisuals(float delta) {
        if (playerDead || levelComplete) return;

        if (bgFade.active) {
            bgFade.elapsed += delta;
            float t = Math.min(bgFade.elapsed / bgFade.duration, 1f);
            baseBgColor.set(lerp(bgFade.from.r, bgFade.to.r, t), lerp(bgFade.from.g, bgFade.to.g, t),
                lerp(bgFade.from.b, bgFade.to.b, t), 1f);
            if (t >= 1f) bgFade.active = false;
        }
        if (groundFade.active) {
            groundFade.elapsed += delta;
            float t = Math.min(groundFade.elapsed / groundFade.duration, 1f);
            baseGroundColor.set(lerp(groundFade.from.r, groundFade.to.r, t), lerp(groundFade.from.g, groundFade.to.g, t),
                lerp(groundFade.from.b, groundFade.to.b, t), 1f);
            if (t >= 1f) groundFade.active = false;
        }

        bgPulse.update(delta);
        groundPulse.update(delta);

        backgroundColor.set(baseBgColor);
        if (bgPulse.active) backgroundColor.lerp(bgPulse.target, bgPulse.getIntensity());

        groundColor.set(baseGroundColor);
        if (groundPulse.active) groundColor.lerp(groundPulse.target, groundPulse.getIntensity());
    }

    public void playerDied() {
        if (!playerDead) {
            Gdx.app.log("GameWorld", "Player died.");
            playerDead = true;
        }
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static Color hexToColor(String hex) {
        if (hex == null || hex.isEmpty()) return new Color(0, 0, 0, 1);
        if (hex.startsWith("#")) hex = hex.substring(1);
        long val = Long.parseLong(hex, 16);
        float r = ((val >> 16) & 0xFF) / 255f;
        float g = ((val >> 8)  & 0xFF) / 255f;
        float b = (val & 0xFF)         / 255f;
        return new Color(r, g, b, 1f);
    }
}
