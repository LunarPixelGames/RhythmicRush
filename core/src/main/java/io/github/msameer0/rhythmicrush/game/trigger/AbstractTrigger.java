package io.github.msameer0.rhythmicrush.game.trigger;

import io.github.msameer0.rhythmicrush.game.GameWorld;

/**
 * Base class for all triggers that activate based on the player's X position.
 */
public abstract class AbstractTrigger {
    public final float worldX;
    public boolean fired = false;

    public AbstractTrigger(float worldX) {
        this.worldX = worldX;
    }

    /**
     * Called when the trigger is activated.
     * @param world The game world instance.
     */
    public abstract void fire(GameWorld world);

    /**
     * Resets the trigger to its initial state.
     */
    public void reset() {
        fired = false;
    }
}
