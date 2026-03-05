package io.github.msameer0.rhythmicrush.game.gameplay.hazards;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

public class Spike extends AbstractHazard {

    // hitbox is 1/4 player width, 1/2 player height, centered in the grid cell
    private static final float PLAYER_SIZE    = 50f;
    private static final float TEXTURE_SIZE   = 50f;
    private static final float HITBOX_W       = PLAYER_SIZE * 0.25f;           // 12.5
    private static final float HITBOX_H       = PLAYER_SIZE * 0.5f;            // 25
    private static final float HITBOX_OFFSET_X = (TEXTURE_SIZE - HITBOX_W) / 2f; // 18.75 — centers hitbox in cell
    private static final float HITBOX_OFFSET_Y = 0f;                           // flush with ground

    private Rectangle spikeHitbox;

    public Spike(float x, float y) {
        super(x, y, TEXTURE_SIZE, TEXTURE_SIZE); // full grid cell for texture
        spikeHitbox = new Rectangle(x + HITBOX_OFFSET_X, y + HITBOX_OFFSET_Y, HITBOX_W, HITBOX_H);
    }

    @Override
    public void updatePosition(float scrollSpeed, float delta) {
        super.updatePosition(scrollSpeed, delta);
        spikeHitbox.setPosition(x + HITBOX_OFFSET_X, y + HITBOX_OFFSET_Y);
    }

    @Override
    public void onTouch(AbstractPlayer player) {
        // use the tighter hitbox for death check, not the full bounds
        if (spikeHitbox.overlaps(player.getBounds())) {
            player.getWorld().playerDied();
        }
    }

    public Rectangle getHitbox() { return spikeHitbox; }
}
