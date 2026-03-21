package io.github.msameer0.rhythmicrush.game.trigger;

import com.badlogic.gdx.graphics.Color;
import io.github.msameer0.rhythmicrush.game.GameWorld;

/**
 * Trigger that changes background and ground colors.
 */
public class ColorTrigger extends AbstractTrigger {
    public final Color targetBg;
    public final Color targetGround;
    public final float fadeDuration;

    public ColorTrigger(float worldX, Color targetBg, Color targetGround, float fadeDuration) {
        super(worldX);
        this.targetBg = targetBg;
        this.targetGround = targetGround;
        this.fadeDuration = fadeDuration;
    }

    @Override
    public void fire(GameWorld world) {
        if (targetBg != null) {
            world.startBgFade(targetBg, fadeDuration);
        }
        if (targetGround != null) {
            world.startGroundFade(targetGround, fadeDuration);
        }
    }
}
