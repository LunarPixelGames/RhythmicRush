package io.github.msameer0.rhythmicrush.game;

import com.badlogic.gdx.utils.Array;

import io.github.msameer0.rhythmicrush.game.engine.ObjectPool;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
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

/**
 * Owns and manages all {@link ObjectPool} instances used by {@link GameWorld}.
 *
 * <p>Centralises pool declarations, obtain helpers, and free-all cleanup so
 * that {@code GameWorld} itself can focus on gameplay logic rather than memory
 * management boilerplate.</p>
 */
public class WorldPoolManager {

    // ── Pools ─────────────────────────────────────────────────────────────────

    private final ObjectPool<Block> blockPool = new ObjectPool<Block>() {
        @Override protected Block create()       { return new Block(); }
        @Override protected void  reset(Block b) { b.reset(); }
    };
    private final ObjectPool<Slope> slopePool = new ObjectPool<Slope>() {
        @Override protected Slope create()       { return new Slope(); }
        @Override protected void  reset(Slope s) { s.reset(); }
    };
    private final ObjectPool<Spike>     spikePool     = new ObjectPool<Spike>()     { @Override protected Spike     create() { return new Spike();     } @Override protected void reset(Spike     s) {} };
    private final ObjectPool<HalfSpike> halfSpikePool = new ObjectPool<HalfSpike>() { @Override protected HalfSpike create() { return new HalfSpike(); } @Override protected void reset(HalfSpike s) {} };
    private final ObjectPool<SawBlade>  sawBladePool  = new ObjectPool<SawBlade>()  { @Override protected SawBlade  create() { return new SawBlade();  } @Override protected void reset(SawBlade  s) {} };

    private final ObjectPool<CubePortal>    cubePortalPool    = new ObjectPool<CubePortal>()    { @Override protected CubePortal    create() { return new CubePortal();    } @Override protected void reset(CubePortal    p) {} };
    private final ObjectPool<ShipPortal>    shipPortalPool    = new ObjectPool<ShipPortal>()    { @Override protected ShipPortal    create() { return new ShipPortal();    } @Override protected void reset(ShipPortal    p) {} };
    private final ObjectPool<GravityPortal> gravityPortalPool = new ObjectPool<GravityPortal>() { @Override protected GravityPortal create() { return new GravityPortal(); } @Override protected void reset(GravityPortal p) {} };
    private final ObjectPool<MiniPortal>    miniPortalPool    = new ObjectPool<MiniPortal>()    { @Override protected MiniPortal    create() { return new MiniPortal();    } @Override protected void reset(MiniPortal    p) {} };

    private final ObjectPool<Cube> cubePool = new ObjectPool<Cube>() { @Override protected Cube create() { return new Cube(); } @Override protected void reset(Cube c) {} };
    private final ObjectPool<Ship> shipPool = new ObjectPool<Ship>() { @Override protected Ship create() { return new Ship(); } @Override protected void reset(Ship s) {} };

    private final ObjectPool<YellowOrb> yellowOrbPool = new ObjectPool<YellowOrb>() { @Override protected YellowOrb create() { return new YellowOrb(); } @Override protected void reset(YellowOrb o) { o.reset(); } };
    private final ObjectPool<BlueOrb>   blueOrbPool   = new ObjectPool<BlueOrb>()   { @Override protected BlueOrb   create() { return new BlueOrb();   } @Override protected void reset(BlueOrb   o) { o.reset(); } };
    private final ObjectPool<PinkOrb>   pinkOrbPool   = new ObjectPool<PinkOrb>()   { @Override protected PinkOrb   create() { return new PinkOrb();   } @Override protected void reset(PinkOrb   o) { o.reset(); } };
    private final ObjectPool<RedOrb>    redOrbPool    = new ObjectPool<RedOrb>()    { @Override protected RedOrb    create() { return new RedOrb();    } @Override protected void reset(RedOrb    o) { o.reset(); } };
    private final ObjectPool<BlackOrb>  blackOrbPool  = new ObjectPool<BlackOrb>()  { @Override protected BlackOrb  create() { return new BlackOrb();  } @Override protected void reset(BlackOrb  o) { o.reset(); } };
    private final ObjectPool<GreenOrb>  greenOrbPool  = new ObjectPool<GreenOrb>()  { @Override protected GreenOrb  create() { return new GreenOrb();  } @Override protected void reset(GreenOrb  o) { o.reset(); } };

    // ── Obtain helpers ────────────────────────────────────────────────────────

    public Block      obtainBlock()     { return blockPool.obtain(); }
    public Slope      obtainSlope()     { return slopePool.obtain(); }
    public Spike      obtainSpike()     { return spikePool.obtain(); }
    public HalfSpike  obtainHalfSpike() { return halfSpikePool.obtain(); }
    public SawBlade   obtainSawBlade()  { return sawBladePool.obtain(); }

    public CubePortal    obtainCubePortal()    { return cubePortalPool.obtain(); }
    public ShipPortal    obtainShipPortal()    { return shipPortalPool.obtain(); }
    public GravityPortal obtainGravityPortal() { return gravityPortalPool.obtain(); }
    public MiniPortal    obtainMiniPortal()    { return miniPortalPool.obtain(); }

    public Cube obtainCube() { return cubePool.obtain(); }
    public Ship obtainShip() { return shipPool.obtain(); }

    public YellowOrb obtainYellowOrb() { return yellowOrbPool.obtain(); }
    public BlueOrb   obtainBlueOrb()   { return blueOrbPool.obtain(); }
    public PinkOrb   obtainPinkOrb()   { return pinkOrbPool.obtain(); }
    public RedOrb    obtainRedOrb()    { return redOrbPool.obtain(); }
    public BlackOrb  obtainBlackOrb()  { return blackOrbPool.obtain(); }
    public GreenOrb  obtainGreenOrb()  { return greenOrbPool.obtain(); }

    // ── Free helpers ──────────────────────────────────────────────────────────

    public void freePlayer(AbstractPlayer player) {
        if (player instanceof Cube) cubePool.free((Cube) player);
        else if (player instanceof Ship) shipPool.free((Ship) player);
    }

    public void freeBlock(Block b) {
        if (b instanceof Slope) slopePool.free((Slope) b);
        else blockPool.free(b);
    }

    public void freeHazard(AbstractHazard h) {
        if (h instanceof Spike)          spikePool.free((Spike) h);
        else if (h instanceof HalfSpike) halfSpikePool.free((HalfSpike) h);
        else if (h instanceof SawBlade)  sawBladePool.free((SawBlade) h);
    }

    public void freePortal(AbstractPortal p) {
        if (p instanceof CubePortal)         cubePortalPool.free((CubePortal) p);
        else if (p instanceof ShipPortal)    shipPortalPool.free((ShipPortal) p);
        else if (p instanceof GravityPortal) gravityPortalPool.free((GravityPortal) p);
        else if (p instanceof MiniPortal)    miniPortalPool.free((MiniPortal) p);
    }

    public void freeOrb(AbstractOrb o) {
        if (o instanceof YellowOrb) yellowOrbPool.free((YellowOrb) o);
        else if (o instanceof BlueOrb)   blueOrbPool.free((BlueOrb) o);
        else if (o instanceof PinkOrb)   pinkOrbPool.free((PinkOrb) o);
        else if (o instanceof RedOrb)    redOrbPool.free((RedOrb) o);
        else if (o instanceof BlackOrb)  blackOrbPool.free((BlackOrb) o);
        else if (o instanceof GreenOrb)  greenOrbPool.free((GreenOrb) o);
    }

    // ── Bulk free ─────────────────────────────────────────────────────────────

    /**
     * Frees all active entities back to their pools and clears the provided arrays.
     * Also resets the typed subset arrays for hazards and portals.
     *
     * @param blocks          Active block array (will be cleared).
     * @param hazards         Active hazard array (will be cleared).
     * @param portals         Active portal array (will be cleared).
     * @param orbs            Active orb array (will be cleared).
     * @param activeSpikes    Typed hazard subset (will be cleared).
     * @param activeHalfSpikes Typed hazard subset (will be cleared).
     * @param activeSawBlades Typed hazard subset (will be cleared).
     * @param activeCubePortals    Typed portal subset (will be cleared).
     * @param activeShipPortals    Typed portal subset (will be cleared).
     * @param activeGravityPortals Typed portal subset (will be cleared).
     * @param activeMiniPortals    Typed portal subset (will be cleared).
     * @param blockCull  Index from which active (non-culled) blocks start.
     * @param hazardCull Index from which active hazards start.
     * @param portalCull Index from which active portals start.
     * @param orbCull    Index from which active orbs start.
     */
    public void freeAll(
        Array<Block> blocks, Array<AbstractHazard> hazards,
        Array<AbstractPortal> portals, Array<AbstractOrb> orbs,
        Array<Spike> activeSpikes, Array<HalfSpike> activeHalfSpikes, Array<SawBlade> activeSawBlades,
        Array<CubePortal> activeCubePortals, Array<ShipPortal> activeShipPortals,
        Array<GravityPortal> activeGravityPortals, Array<MiniPortal> activeMiniPortals,
        int blockCull, int hazardCull, int portalCull, int orbCull
    ) {
        for (int i = blockCull;  i < blocks.size;  i++) freeBlock(blocks.get(i));
        blocks.clear();

        for (int i = hazardCull; i < hazards.size; i++) freeHazard(hazards.get(i));
        hazards.clear();
        activeSpikes.clear();
        activeHalfSpikes.clear();
        activeSawBlades.clear();

        for (int i = portalCull; i < portals.size; i++) freePortal(portals.get(i));
        portals.clear();
        activeCubePortals.clear();
        activeShipPortals.clear();
        activeGravityPortals.clear();
        activeMiniPortals.clear();

        for (int i = orbCull; i < orbs.size; i++) freeOrb(orbs.get(i));
        orbs.clear();
    }
}
