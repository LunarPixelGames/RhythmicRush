package io.github.msameer0.rhythmicrush.game.trigger;

import com.badlogic.gdx.graphics.Color;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * Trigger that pulses background and ground colors.
 */
@Registry(id = "pulse_trigger")
public class PulseTrigger extends AbstractTrigger {
    public Color pulseBg;
    public Color pulseGround;
    public float fadeInTime;
    public float holdTime;
    public float fadeOutTime;

    public PulseTrigger() {
        super();
        this.pulseBg     = new Color(0, 0, 0, 0);
        this.pulseGround = new Color(0, 0, 0, 0);
        this.fadeInTime  = 0f;
        this.holdTime    = 0f;
        this.fadeOutTime = 0f;
    }

    public PulseTrigger(float worldX, Color pulseBg, Color pulseGround,
                        float fadeInTime, float holdTime, float fadeOutTime) {
        super(worldX);
        this.pulseBg     = pulseBg;
        this.pulseGround = pulseGround;
        this.fadeInTime  = fadeInTime;
        this.holdTime    = holdTime;
        this.fadeOutTime = fadeOutTime;
    }

    /**
     * Initialises this trigger for use — call after obtaining from registry/pool.
     */
    public PulseTrigger init(float worldX, Color pulseBg, Color pulseGround,
                             float fadeInTime, float holdTime, float fadeOutTime) {
        this.worldX      = worldX;
        this.pulseBg     = pulseBg;
        this.pulseGround = pulseGround;
        this.fadeInTime  = fadeInTime;
        this.holdTime    = holdTime;
        this.fadeOutTime = fadeOutTime;
        this.fired       = false;
        return this;
    }

    @Override
    public void fire(GameWorld world) {
        if (pulseBg != null)     world.startBgPulse(pulseBg, fadeInTime, holdTime, fadeOutTime);
        if (pulseGround != null) world.startGroundPulse(pulseGround, fadeInTime, holdTime, fadeOutTime);
    }
}
