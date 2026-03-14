package io.github.msameer0.rhythmicrush.game.gameplay.hazards;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

public class Spike extends AbstractHazard {

    private static final float PLAYER_SIZE     = 50f;
    private static final float TEXTURE_SIZE    = 50f;
    private static final float HITBOX_W        = PLAYER_SIZE * 0.25f;  // 12.5
    private static final float HITBOX_H        = PLAYER_SIZE * 0.5f;   // 25
    private static final float HITBOX_CENTER_X = (TEXTURE_SIZE - HITBOX_W) / 2f; // 18.75

    private final float rotation; // 0=up, 90=right, 180=down, 270=left
    private Rectangle spikeHitbox;

    public Spike(float x, float y) {
        this(x, y, 0f);
    }

    public Spike(float x, float y, float rotation) {
        super(x, y, TEXTURE_SIZE, TEXTURE_SIZE);
        this.rotation = rotation;
        spikeHitbox = new Rectangle();
        updateHitbox();
    }

    private void updateHitbox() {
        // hitbox shifts depending on which way the spike points
        // so the narrow part always aligns with the tip
        switch (Math.round(rotation) % 360) {
            case 90:  // pointing right — hitbox on right side, tall and narrow
                spikeHitbox.set(x + TEXTURE_SIZE - HITBOX_H, y + HITBOX_CENTER_X, HITBOX_H, HITBOX_W);
                break;
            case 180: // pointing down (ceiling spike) — hitbox at top
                spikeHitbox.set(x + HITBOX_CENTER_X, y + TEXTURE_SIZE - HITBOX_H, HITBOX_W, HITBOX_H);
                break;
            case 270: // pointing left — hitbox on left side
                spikeHitbox.set(x, y + HITBOX_CENTER_X, HITBOX_H, HITBOX_W);
                break;
            default:  // 0 — pointing up (ground spike)
                spikeHitbox.set(x + HITBOX_CENTER_X, y, HITBOX_W, HITBOX_H);
                break;
        }
    }

    @Override
    public void updatePosition(float scrollSpeed, float delta) {
        super.updatePosition(scrollSpeed, delta);
        updateHitbox();
    }

    @Override
    public void onTouch(AbstractPlayer player) {
        if (spikeHitbox.overlaps(player.getBounds())) {
            player.getWorld().playerDied();
        }
    }

    public float getRotation() { return rotation; }
    public Rectangle getHitbox() { return spikeHitbox; }
}
