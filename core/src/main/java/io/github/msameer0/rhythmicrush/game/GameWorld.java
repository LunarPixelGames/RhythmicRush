package io.github.msameer0.rhythmicrush.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import io.github.msameer0.rhythmicrush.game.engine.ObjectPool;
import io.github.msameer0.rhythmicrush.game.engine.Tickable;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.GravityPortal;
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
 *
 * <p>The GameWorld handles:
 * <ul>
 *   <li>Level loading and object instantiation from {@link LevelData}</li>
 *   <li>Object pooling for performance optimization of blocks, hazards, and players</li>
 *   <li>Collision detection and interaction between the player and world objects</li>
 *   <li>Background and ground color transitions via triggers</li>
 *   <li>World scrolling, culling of off-screen objects, and level completion tracking</li>
 * </ul>
 *
 * <p>It implements {@link Tickable} to integrate with the main game loop.</p>
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

    private int blockCull = 0;
    private int hazardCull = 0;
    private int portalCull = 0;

    private static final float COLLISION_LOOKAHEAD = 1400f;

    private final Array<AbstractPortal> portals = new Array<>();
    private final Array<AbstractHazard> hazards = new Array<>();
    private final Array<Block> blocks = new Array<>();

    private final ObjectPool<Block> blockPool = new ObjectPool<Block>() {
        @Override
        protected Block create() {
            return new Block();
        }

        @Override
        protected void reset(Block b) {
        }
    };
    private final ObjectPool<Spike> spikePool = new ObjectPool<Spike>() {
        @Override
        protected Spike create() {
            return new Spike();
        }

        @Override
        protected void reset(Spike s) {
        }
    };
    private final ObjectPool<CubePortal> cubePortalPool = new ObjectPool<CubePortal>() {
        @Override
        protected CubePortal create() {
            return new CubePortal();
        }

        @Override
        protected void reset(CubePortal p) {
        }
    };
    private final ObjectPool<ShipPortal> shipPortalPool = new ObjectPool<ShipPortal>() {
        @Override
        protected ShipPortal create() {
            return new ShipPortal();
        }

        @Override
        protected void reset(ShipPortal p) {
        }
    };
    private final ObjectPool<GravityPortal> gravityPortalPool = new ObjectPool<GravityPortal>() {
        @Override
        protected GravityPortal create() {
            return new GravityPortal();
        }

        @Override
        protected void reset(GravityPortal p) {
        }
    };
    private final ObjectPool<Cube> cubePool = new ObjectPool<Cube>() {
        @Override
        protected Cube create() {
            return new Cube();
        }

        @Override
        protected void reset(Cube c) {
        }
    };
    private final ObjectPool<Ship> shipPool = new ObjectPool<Ship>() {
        @Override
        protected Ship create() {
            return new Ship();
        }

        @Override
        protected void reset(Ship s) {
        }
    };

    /**
     * Internal state tracker for managing a linear color transition (fade).
     *
     * <p>This class stores the source and target colors, the total duration,
     * and the elapsed time to allow {@link GameWorld} to interpolate between
     * colors during a frame-by-frame update.</p>
     */
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

    private final Array<Spike> activeSpikes = new Array<>();
    private final Array<CubePortal> activeCubePortals = new Array<>();
    private final Array<ShipPortal> activeShipPortals = new Array<>();
    private final Array<GravityPortal> activeGravityPortals = new Array<>();


    /**
     * Constructs a new GameWorld and initializes the default player state.
     *
     * <p>This constructor obtains a {@link Cube} player from the object pool,
     * positions it at the starting coordinates, and establishes the bidirectional
     * link between the player and this world.</p>
     */
    public GameWorld() {
        player = cubePool.obtain().init(100, groundY);
        player.setWorld(this);
        Gdx.app.log("GameWorld", "Player initialized.");
    }


    /**
     * Retrieves a {@link Cube} instance from the object pool and initializes it
     * at the specified coordinates.
     *
     * <p>This method also establishes the bidirectional link between the new
     * player instance and this game world.</p>
     *
     * @param x        the initial horizontal position of the cube
     * @param y        the initial vertical position of the cube
     * @param vy       the initial vertical velocity
     * @param jumpHeld the initial jump input state
     * @return an initialized {@code Cube} instance linked to this world
     */
    public Cube obtainCube(float x, float y, float vy, boolean jumpHeld) {
        Cube c = cubePool.obtain().init(x, y, vy, jumpHeld);
        c.setWorld(this);
        return c;
    }

    public Cube obtainCube(float x, float y) {
        return obtainCube(x, y, 0, false);
    }

    /**
     * Retrieves a {@link Ship} instance from the object pool and initializes it
     * at the specified coordinates.
     *
     * <p>This method also establishes the bidirectional link between the new
     * player instance and this game world.</p>
     *
     * @param x        the initial horizontal position of the ship
     * @param y        the initial vertical position of the ship
     * @param vy       the initial vertical velocity
     * @param jumpHeld the initial jump input state
     * @return an initialized {@code Ship} instance linked to this world
     */
    public Ship obtainShip(float x, float y, float vy, boolean jumpHeld) {
        Ship s = shipPool.obtain().init(x, y, vy, jumpHeld);
        s.setWorld(this);
        return s;
    }

    public Ship obtainShip(float x, float y) {
        return obtainShip(x, y, 0, false);
    }

    public void startBgFade(Color target, float duration) {
        bgFade.init(baseBgColor, target, duration);
    }

    public void startGroundFade(Color target, float duration) {
        groundFade.init(baseGroundColor, target, duration);
    }

    public void startBgPulse(Color target, float fadeIn, float hold, float fadeOut) {
        bgPulse.init(target, fadeIn, hold, fadeOut);
    }

    public void startGroundPulse(Color target, float fadeIn, float hold, float fadeOut) {
        groundPulse.init(target, fadeIn, hold, fadeOut);
    }

    public AbstractPlayer obtainPlayer(String typeId) {
        if ("cube".equals(typeId)) return cubePool.obtain();
        if ("ship".equals(typeId)) return shipPool.obtain();
        return Registries.PLAYERS.create(typeId);
    }

    public void loadLevel(LevelData data) {
        Gdx.app.log("GameWorld", "Loading level: " + data.name);
        currentLevelData = data;
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

        String bg = (data.bgColor != null && !data.bgColor.isEmpty()) ? data.bgColor : "1a1a2e";
        String gnd = (data.groundColor != null && !data.groundColor.isEmpty()) ? data.groundColor : "16213e";
        baseBgColor.set(hexToColor(bg));
        baseGroundColor.set(hexToColor(gnd));
        backgroundColor.set(baseBgColor);
        groundColor.set(baseGroundColor);

        for (LevelData.ObjectEntry e : data.objects) {
            if (Registries.BLOCKS.has(e.type)) {
                BlockType bt = BlockType.DEFAULT;
                if (e.blockType != null) {
                    for (BlockType t : BlockType.values())
                        if (t.textureName.equals(e.blockType)) {
                            bt = t;
                            break;
                        }
                }
                Block b = blockPool.obtain().init(e.x, e.y, e.size, bt);
                blocks.add(b);
            } else if (Registries.HAZARDS.has(e.type)) {
                if ("spike".equals(e.type)) {
                    Spike s = spikePool.obtain().init(e.x, e.y, e.rotation);
                    hazards.add(s);
                    activeSpikes.add(s);
                }
            } else if (Registries.PORTALS.has(e.type)) {
                AbstractPortal p = null;
                if ("cube_portal".equals(e.type)) {
                    p = cubePortalPool.obtain();
                    activeCubePortals.add((CubePortal) p);
                } else if ("ship_portal".equals(e.type)) {
                    p = shipPortalPool.obtain();
                    activeShipPortals.add((ShipPortal) p);
                } else if ("gravity_portal".equals(e.type)) {
                    p = gravityPortalPool.obtain();
                    ((GravityPortal) p).init(e.x, e.y, e.flipped);
                    activeGravityPortals.add((GravityPortal) p);
                }
                if (p != null) {
                    if (!(p instanceof GravityPortal)) p.init(e.x, e.y);
                    portals.add(p);
                }
            } else if (Registries.TRIGGERS.has(e.type)) {
                AbstractTrigger trigger = Registries.TRIGGERS.create(e.type);
                if (trigger instanceof ColorTrigger) {
                    Color targetBg = (e.triggerBgColor != null && !e.triggerBgColor.isEmpty())
                        ? hexToColor(e.triggerBgColor) : null;
                    Color targetGround = (e.triggerGroundColor != null && !e.triggerGroundColor.isEmpty())
                        ? hexToColor(e.triggerGroundColor) : null;
                    ((ColorTrigger) trigger).init(e.x, targetBg, targetGround, e.fadeDuration);
                } else if (trigger instanceof PulseTrigger) {
                    Color pulseBg = (e.pulseBgColor != null && !e.pulseBgColor.isEmpty())
                        ? hexToColor(e.pulseBgColor) : null;
                    Color pulseGround = (e.pulseGroundColor != null && !e.pulseGroundColor.isEmpty())
                        ? hexToColor(e.pulseGroundColor) : null;
                    ((PulseTrigger) trigger).init(e.x, pulseBg, pulseGround, e.fadeInTime, e.holdTime, e.fadeOutTime);
                }
                triggers.add(trigger);
            }
        }

        levelEndX = data.getLevelEndX();
        blockStart = 0;
        hazardStart = 0;
        portalStart = 0;
        blockCull = 0;
        hazardCull = 0;
        portalCull = 0;

        blocks.sort((a, b2) -> Float.compare(a.getX(), b2.getX()));
        hazards.sort((a, b2) -> Float.compare(a.getX(), b2.getX()));
        portals.sort((a, b2) -> Float.compare(a.getX(), b2.getX()));

        freePlayer();
        player = obtainPlayer("cube").init(100, groundY);
        player.setWorld(this);
        Gdx.app.log("GameWorld", "Player initialized.");
    }


    /**
     * Resets the game world to its initial state.
     *
     * <p>If {@link LevelData} is currently loaded, this method re-invokes {@link #loadLevel(LevelData)}
     * to perform a full re-initialization of the level layout and properties. Otherwise, it
     * manually clears all active objects, resets player status, clears color transitions,
     * and restores the player to the starting cube state at the base coordinates.</p>
     */
    public void reset() {
        if (currentLevelData != null) loadLevel(currentLevelData);
        else {
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

    /**
     * Releases the current player instance back to its respective object pool.
     *
     * <p>This method checks the type of the active player (e.g., {@link Cube} or {@link Ship}),
     * returns it to the appropriate {@link ObjectPool} to facilitate memory reuse,
     * and nullifies the player reference.</p>
     */
    private void freePlayer() {
        if (player instanceof Cube) cubePool.free((Cube) player);
        else if (player instanceof Ship) shipPool.free((Ship) player);
        player = null;
    }

    /**
     * Releases all currently active game objects—including blocks, hazards, and portals—back
     * to their respective object pools.
     *
     * <p>This method performs a complete cleanup of the entity lists and ensures that
     * pooled resources are available for reuse, preventing memory leaks and minimizing
     * allocations during level transitions or resets.</p>
     */
    private void freeAllActiveObjects() {
        blockPool.freeAll(blocks);

        for (Spike s : activeSpikes) spikePool.free(s);
        activeSpikes.clear();
        hazards.clear();

        for (CubePortal p : activeCubePortals) cubePortalPool.free(p);
        activeCubePortals.clear();
        for (ShipPortal p : activeShipPortals) shipPortalPool.free(p);
        activeShipPortals.clear();
        for (GravityPortal p : activeGravityPortals) gravityPortalPool.free(p);
        activeGravityPortals.clear();
        portals.clear();
    }


    /**
     * Handles player input by updating the jump state of the current player character.
     *
     * <p>This method is typically called by the input processor to signal whether
     * the primary action button (e.g., jump or fly) is currently being held down.</p>
     *
     * @param held {@code true} if the input is currently pressed, {@code false} otherwise
     */
    @Override
    public void onInput(boolean held) {
        if (player != null) player.setJumpHeld(held);
    }

    /**
     * Advances the game state by a single time step.
     *
     * <p>This method acts as the entry point for the world's logic during the game loop,
     * delegating the physics updates, collision checks, and state transitions to
     * the {@link #update(float)} method.</p>
     *
     * @param delta the time elapsed since the last frame in seconds
     */
    @Override
    public void tick(float delta) {
        update(delta);
    }


    /**
     * Updates the game world state for a single frame.
     *
     * <p>This method performs the following operations:
     * <ul>
     *   <li>Exits early if the player is dead or the level is already complete.</li>
     *   <li>Updates player physics and positions of all active game objects (blocks, hazards, portals).</li>
     *   <li>Optimizes collision detection by updating search indices and checking only objects within
     *       the {@code COLLISION_LOOKAHEAD} range.</li>
     *   <li>Handles interactions between the player and world objects, including portal transitions,
     *       hazard collisions, and block physics.</li>
     *   <li>Processes {@link ColorTrigger} logic to initiate background and ground color fades.</li>
     *   <li>Interpolates active color fades using linear interpolation (lerp).</li>
     *   <li>Performs object culling by tracking indices of off-screen objects and returning them to their pools.</li>
     *   <li>Tracks level progress and manages the post-end delay before marking the level as complete.</li>
     * </ul>
     *
     * @param delta the time elapsed since the last update in seconds
     */
    public void update(float delta) {
        if (playerDead || levelComplete) return;

        player.update(delta, groundY);

        for (int i = portalCull; i < portals.size; i++)
            portals.get(i).updatePosition(scrollSpeed, delta);
        for (int i = hazardCull; i < hazards.size; i++)
            hazards.get(i).updatePosition(scrollSpeed, delta);
        for (int i = blockCull; i < blocks.size; i++)
            blocks.get(i).updatePosition(scrollSpeed, delta);

        final float px = player.x;
        final float rangeMin = px - 300f;
        final float rangeMax = px + COLLISION_LOOKAHEAD;

        if (blockStart < blockCull) blockStart = blockCull;
        if (hazardStart < hazardCull) hazardStart = hazardCull;
        if (portalStart < portalCull) portalStart = portalCull;

        while (blockStart < blocks.size && blocks.get(blockStart).getX() + blocks.get(blockStart).getWidth() < rangeMin)
            blockStart++;
        while (hazardStart < hazards.size && hazards.get(hazardStart).getX() + hazards.get(hazardStart).getWidth() < rangeMin)
            hazardStart++;
        while (portalStart < portals.size && portals.get(portalStart).getX() + portals.get(portalStart).getWidth() < rangeMin)
            portalStart++;

        for (int i = portalStart; i < portals.size; i++) {
            AbstractPortal portal = portals.get(i);
            if (portal.getX() > rangeMax) break;
            if (portal.tryTouch(player)) {
                if (portal instanceof GravityPortal) {
                    player.setGravityFlipped(!player.isGravityFlipped());
                } else {
                    AbstractPlayer next = null;
                    if (portal instanceof CubePortal)
                        next = obtainPlayer("cube").init(player.getX(), player.getY());
                    else if (portal instanceof ShipPortal)
                        next = obtainPlayer("ship").init(player.getX(), player.getY());

                    if (next != null) {
                        next.setWorld(this);
                        next.copyState(player);
                        freePlayer();
                        player = next;
                    }
                }
            }
        }

        for (int i = hazardStart; i < hazards.size; i++) {
            AbstractHazard h = hazards.get(i);
            if (h.getX() > rangeMax) break;
            h.tryTouch(player);
        }

        for (int i = blockStart; i < blocks.size; i++) {
            Block b = blocks.get(i);
            if (b.getX() > rangeMax) break;
            b.tryTouch(player);
        }

        player.tryJump();

        worldScrolled += scrollSpeed * delta;

        float playerWorldX = 100f + worldScrolled;
        for (AbstractTrigger t : triggers) {
            if (!t.fired && playerWorldX >= t.worldX) {
                t.fired = true;
                t.fire(this);
            }
        }

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

        // Final colors for rendering
        backgroundColor.set(baseBgColor);
        if (bgPulse.active) {
            float intensity = bgPulse.getIntensity();
            backgroundColor.lerp(bgPulse.target, intensity);
        }

        groundColor.set(baseGroundColor);
        if (groundPulse.active) {
            float intensity = groundPulse.getIntensity();
            groundColor.lerp(groundPulse.target, intensity);
        }

        while (blockCull < blocks.size) {
            Block b = blocks.get(blockCull);
            if (b.getX() + b.getWidth() >= cullX - 200) break;
            blockPool.free(b);
            blockCull++;
        }
        while (hazardCull < hazards.size) {
            AbstractHazard h = hazards.get(hazardCull);
            if (h.getX() + h.getWidth() >= cullX) break;
            if (h instanceof Spike) {
                spikePool.free((Spike) h);
                activeSpikes.removeValue((Spike) h, true);
            }
            hazardCull++;
        }
        while (portalCull < portals.size) {
            AbstractPortal p = portals.get(portalCull);
            if (p.getX() + p.getWidth() >= cullX) break;
            if (p instanceof CubePortal) {
                cubePortalPool.free((CubePortal) p);
                activeCubePortals.removeValue((CubePortal) p, true);
            } else if (p instanceof ShipPortal) {
                shipPortalPool.free((ShipPortal) p);
                activeShipPortals.removeValue((ShipPortal) p, true);
            } else if (p instanceof GravityPortal) {
                gravityPortalPool.free((GravityPortal) p);
                activeGravityPortals.removeValue((GravityPortal) p, true);
            }
            portalCull++;
        }

        if (levelEndX > 0 && worldScrolled >= levelEndX) {
            if (postEndTimer < 0) postEndTimer = 0f;
        }
        if (postEndTimer >= 0) {
            postEndTimer += delta;
            if (postEndTimer >= POST_END_DELAY) {
                if (!levelComplete) {
                    Gdx.app.log("GameWorld", "Level completed!");
                    levelComplete = true;
                }
            }
        }
    }


    /**
     * Calculates the current completion progress of the level.
     *
     * <p>The progress is determined by the ratio of the total horizontal distance scrolled
     * to the total length of the level. The value is clamped between 0.0 and 1.0.</p>
     *
     * @return a float between 0.0 (start) and 1.0 (end) representing the completion percentage
     */
    public float getProgress() {
        if (levelEndX <= 0) return 0f;
        return Math.min(worldScrolled / levelEndX, 1f);
    }


    /**
     * Adds an {@link AbstractPortal} to the game world's list of active portals.
     *
     * <p>Portals added via this method are tracked for positioning updates and
     * collision detection with the player during the game loop.</p>
     *
     * @param portal the portal instance to be added to the world
     */
    public void addPortal(AbstractPortal portal) {
        portals.add(portal);
    }

    /**
     * Adds a hazard to the game world.
     *
     * <p>Once added, the hazard will be managed by the world's update loop,
     * including position updates, collision detection against the player,
     * and automatic culling when it moves off-screen.</p>
     *
     * @param hazard the {@link AbstractHazard} instance to be added to the world
     */
    public void addHazard(AbstractHazard hazard) {
        hazards.add(hazard);
    }

    /**
     * Adds a {@link Block} to the game world's list of active blocks.
     *
     * <p>Blocks added via this method are tracked for positioning updates,
     * collision detection, and physics interactions with the player
     * during the game loop.</p>
     *
     * @param block the block instance to be added to the world
     */
    public void addBlock(Block block) {
        blocks.add(block);
    }


    /**
     * Returns the current player instance active in the game world.
     *
     * <p>This can be any subclass of {@link AbstractPlayer}, such as a {@link Cube}
     * or a {@link Ship}, depending on the current game state or portals touched.</p>
     *
     * @return the current {@code AbstractPlayer} instance
     */
    public AbstractPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the vertical Y-coordinate of the ground level.
     *
     * <p>This value serves as the baseline for player movement, collision detection,
     * and the placement of ground-level objects within the game world.</p>
     *
     * @return the Y-coordinate representing the ground level
     */
    public float getGroundY() {
        return groundY;
    }

    /**
     * Checks whether the player has died during the current level attempt.
     *
     * <p>A player is typically marked as dead when they collide with a hazard
     * or an obstacle that terminates the game session.</p>
     *
     * @return {@code true} if the player is dead, {@code false} otherwise
     */
    public boolean isPlayerDead() {
        return playerDead;
    }

    /**
     * Checks whether the level has been successfully completed.
     *
     * <p>The level is marked as complete once the player has passed the end coordinate
     * and the subsequent post-completion delay period has elapsed.</p>
     *
     * @return {@code true} if the level is finished, {@code false} otherwise
     */
    public boolean isLevelComplete() {
        return levelComplete;
    }

    public void setLevelComplete(boolean levelComplete) {
        this.levelComplete = levelComplete;
    }

    /**
     * Retrieves the list of all active portals currently managed by the game world.
     *
     * <p>This list includes all {@link AbstractPortal} instances, such as cube and ship
     * portals, that are currently being updated for movement and collision detection.</p>
     *
     * @return an {@code Array} containing the active portals in the world
     */
    public Array<AbstractPortal> getPortals() {
        return portals;
    }

    /**
     * Retrieves the list of all active hazards currently managed by the game world.
     *
     * <p>This list includes all {@link AbstractHazard} instances, such as spikes,
     * that are currently being updated for movement and collision detection
     * against the player.</p>
     *
     * @return an {@code Array} containing the active hazards in the world
     */
    public Array<AbstractHazard> getHazards() {
        return hazards;
    }

    public float getWorldScrolled() { return worldScrolled; }

    /**
     * Retrieves the list of all active blocks currently managed by the game world.
     *
     * <p>This list contains all {@link Block} instances that are currently being
     * updated for movement, collision detection, and physics interactions
     * with the player.</p>
     *
     * @return an {@code Array} containing the active blocks in the world
     */
    public Array<Block> getBlocks() {
        return blocks;
    }

    /**
     * Retrieves the current background color of the game world.
     *
     * <p>This color may change dynamically during gameplay if the player triggers
     * a {@link ColorTrigger}, resulting in a smooth transition between colors.</p>
     *
     * @return the {@link Color} currently used for the world background
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Retrieves the current color of the ground in the game world.
     *
     * <p>This color is subject to dynamic transitions during gameplay if a
     * {@link ColorTrigger} is activated by the player's position, causing
     * a smooth fade from the previous color to a new target color.</p>
     *
     * @return the {@link Color} currently used to render the ground
     */
    public Color getGroundColor() {
        return groundColor;
    }

    /**
     * Updates the game state to indicate that the player has died.
     *
     * <p>This method is typically called when the player character collides with a
     * hazard or an obstacle, effectively halting game progression and triggering
     * any necessary game-over or reset logic.</p>
     */
    public void playerDied() {
        if (!playerDead) {
            Gdx.app.log("GameWorld", "Player died.");
            playerDead = true;
        }
    }

    /**
     * Sets the horizontal threshold for object culling.
     *
     * <p>Game objects (blocks, hazards, and portals) whose horizontal position plus
     * their width falls below this value are considered off-screen. These objects
     * are automatically removed from the active lists and returned to their
     * respective object pools to optimize memory and performance.</p>
     *
     * @param x the horizontal X-coordinate to use as the culling boundary
     */
    public void setCullX(float x) {
        cullX = x;
    }

    /**
     * Performs linear interpolation between two values.
     *
     * <p>This method calculates a value between {@code a} and {@code b} based on the
     * interpolation factor {@code t}. When {@code t} is 0.0, it returns {@code a};
     * when {@code t} is 1.0, it returns {@code b}.</p>
     *
     * @param a the starting value
     * @param b the ending value
     * @param t the interpolation factor, typically between 0.0 and 1.0
     * @return the interpolated value
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Converts a hexadecimal color string into a {@link Color} object.
     *
     * <p>The method supports hex strings with or without a leading '#' character.
     * If the input string is null or empty, it defaults to solid black. The input
     * is parsed as a 24-bit RGB value, and the alpha channel is set to 1.0 (fully opaque).</p>
     *
     * @param hex the hexadecimal string representing the color (e.g., "#FF0000" or "FFFFFF")
     * @return a {@code Color} object corresponding to the provided hex value
     */
    public static Color hexToColor(String hex) {
        if (hex == null || hex.isEmpty()) return new Color(0, 0, 0, 1);
        if (hex.startsWith("#")) hex = hex.substring(1);
        long val = Long.parseLong(hex, 16);
        float r = ((val >> 16) & 0xFF) / 255f;
        float g = ((val >> 8) & 0xFF) / 255f;
        float b = (val & 0xFF) / 255f;
        return new Color(r, g, b, 1f);
    }
}
