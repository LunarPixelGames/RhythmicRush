package io.github.msameer0.rhythmicrush.game.gameplay.hazards;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

public class Spike extends AbstractHazard {

    private Rectangle spikeHitbox;

    public Spike(float x, float y) {
        //the full spike texture width/height
        super(x, y, 40, 40);

        //hitbox
        spikeHitbox = new Rectangle(x + 10, y, 30, 30); // adjust as needed
    }

    @Override
    public void updatePosition(float scrollSpeed, float delta) {
        super.updatePosition(scrollSpeed, delta);
        spikeHitbox.setPosition(x + 10, y); //keep hitbox aligned
    }

    @Override
    public void onTouch(AbstractPlayer player) {
        //death
        player.getWorld().playerDied();
    }

    public Rectangle getHitbox() {
        return spikeHitbox;
    }
}
