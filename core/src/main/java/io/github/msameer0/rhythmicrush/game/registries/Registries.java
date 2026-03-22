package io.github.msameer0.rhythmicrush.game.registries;

import io.github.msameer0.rhythmicrush.game.gameplay.blocks.Block;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.AbstractHazard;
import io.github.msameer0.rhythmicrush.game.gameplay.hazards.Spike;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.AbstractPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.CubePortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.GravityPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals.ShipPortal;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Cube;
import io.github.msameer0.rhythmicrush.game.gameplay.players.Ship;
import io.github.msameer0.rhythmicrush.game.trigger.AbstractTrigger;
import io.github.msameer0.rhythmicrush.game.trigger.ColorTrigger;
import io.github.msameer0.rhythmicrush.game.trigger.PulseTrigger;

/**
 * Central access point for all game registries.
 */
public class Registries {
    public static final GameRegistry<Block> BLOCKS = new GameRegistry<>();
    public static final GameRegistry<AbstractHazard> HAZARDS = new GameRegistry<>();
    public static final GameRegistry<AbstractPortal> PORTALS = new GameRegistry<>();
    public static final GameRegistry<AbstractPlayer> PLAYERS = new GameRegistry<>();

    public static final GameRegistry<AbstractTrigger> TRIGGERS = new GameRegistry<>();

    /**
     * Initializes all registries and registers the default game elements.
     */
    public static void init() {
        BLOCKS.register(Block.class, Block::new);

        HAZARDS.register(Spike.class, Spike::new);

        PORTALS.register(CubePortal.class, () -> new CubePortal(0, 0));
        PORTALS.register(ShipPortal.class, () -> new ShipPortal(0, 0));
        PORTALS.register(GravityPortal.class, () -> new GravityPortal(0, 0, false));

        PLAYERS.register(Cube.class, () -> new Cube(0, 0));
        PLAYERS.register(Ship.class, () -> new Ship(0, 0));

        TRIGGERS.register(ColorTrigger.class, ColorTrigger::new);
        TRIGGERS.register(PulseTrigger.class, PulseTrigger::new);
    }
}
