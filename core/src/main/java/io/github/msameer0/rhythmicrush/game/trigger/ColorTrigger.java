package io.github.msameer0.rhythmicrush.game.trigger;

import com.badlogic.gdx.graphics.Color;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * Trigger that changes background and ground colors.
 */
@Registry(id = "color_trigger")
public class ColorTrigger extends AbstractTrigger {
    public Color targetBg;
    public Color targetGround;
    public float fadeDuration;

    public ColorTrigger() {
        super();
        this.targetBg     = new Color(0, 0, 0, 0);
        this.targetGround = new Color(0, 0, 0, 0);
        this.fadeDuration = 0f;
    }

    public ColorTrigger(float worldX, Color targetBg, Color targetGround, float fadeDuration) {
        super(worldX);
        this.targetBg     = targetBg;
        this.targetGround = targetGround;
        this.fadeDuration = fadeDuration;
    }

    /**
     * Initialises this trigger for use — call after obtaining from registry/pool.
     */
    public ColorTrigger init(float worldX, Color targetBg, Color targetGround, float fadeDuration) {
        this.worldX       = worldX;
        this.targetBg     = targetBg;
        this.targetGround = targetGround;
        this.fadeDuration = fadeDuration;
        this.fired        = false;
        return this;
    }

    @Override
    public void fire(GameWorld world) {
        if (targetBg != null)     world.startBgFade(targetBg, fadeDuration);
        if (targetGround != null) world.startGroundFade(targetGround, fadeDuration);
    }
}
