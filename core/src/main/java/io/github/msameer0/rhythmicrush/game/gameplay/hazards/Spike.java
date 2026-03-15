package io.github.msameer0.rhythmicrush.game.gameplay.hazards;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

public class Spike extends AbstractHazard {

    private static final float PLAYER_SIZE     = 50f;
    private static final float TEXTURE_SIZE    = 50f;
    private static final float HITBOX_W        = PLAYER_SIZE * 0.25f;  // 12.5
    private static final float HITBOX_H        = PLAYER_SIZE * 0.5f;   // 25
    private static final float HITBOX_CENTER_X = (TEXTURE_SIZE - HITBOX_W) / 2f; // 18.75

    // Not final so it can be reset when the instance is reused from the pool
    private float rotation;
    private Rectangle spikeHitbox;

    /** No-arg constructor for pooling — call init() before use. */
    public Spike() {
        super(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        spikeHitbox = new Rectangle();
    }

    public Spike(float x, float y) {
        this(x, y, 0f);
    }

    public Spike(float x, float y, float rotation) {
        super(x, y, TEXTURE_SIZE, TEXTURE_SIZE);
        this.rotation = rotation;
        spikeHitbox = new Rectangle();
        updateHitbox();
    }

    /** Reinitialise this spike for reuse from the pool. */
    public Spike init(float x, float y, float rotation) {
        this.x        = x;
        this.y        = y;
        this.rotation = rotation;
        bounds.setPosition(x, y);
        updateHitbox();
        return this;
    }

    private void updateHitbox() {
        switch (Math.round(rotation) % 360) {
            case 90:
                spikeHitbox.set(x + TEXTURE_SIZE - HITBOX_H, y + HITBOX_CENTER_X, HITBOX_H, HITBOX_W);
                break;
            case 180:
                spikeHitbox.set(x + HITBOX_CENTER_X, y + TEXTURE_SIZE - HITBOX_H, HITBOX_W, HITBOX_H);
                break;
            case 270:
                spikeHitbox.set(x, y + HITBOX_CENTER_X, HITBOX_H, HITBOX_W);
                break;
            default:
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

    public float getRotation()    { return rotation; }
    public Rectangle getHitbox()  { return spikeHitbox; }
}
