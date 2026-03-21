package io.github.msameer0.rhythmicrush.game.trigger;

import com.badlogic.gdx.graphics.Color;
import io.github.msameer0.rhythmicrush.game.GameWorld;

/**
 * Trigger that pulses background and ground colors.
 */
public class PulseTrigger extends AbstractTrigger {
    public final Color pulseBg;
    public final Color pulseGround;
    public final float fadeInTime;
    public final float holdTime;
    public final float fadeOutTime;

    public PulseTrigger(float worldX, Color pulseBg, Color pulseGround, float fadeInTime, float holdTime, float fadeOutTime) {
        super(worldX);
        this.pulseBg = pulseBg;
        this.pulseGround = pulseGround;
        this.fadeInTime = fadeInTime;
        this.holdTime = holdTime;
        this.fadeOutTime = fadeOutTime;
    }

    @Override
    public void fire(GameWorld world) {
        if (pulseBg != null) {
            world.startBgPulse(pulseBg, fadeInTime, holdTime, fadeOutTime);
        }
        if (pulseGround != null) {
            world.startGroundPulse(pulseGround, fadeInTime, holdTime, fadeOutTime);
        }
    }
}
