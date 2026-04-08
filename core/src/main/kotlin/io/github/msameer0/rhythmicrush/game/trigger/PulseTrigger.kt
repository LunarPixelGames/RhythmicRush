package io.github.msameer0.rhythmicrush.game.trigger

import com.badlogic.gdx.graphics.Color
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "pulse_trigger")
class PulseTrigger : AbstractTrigger {
    var pulseBg: Color
    var pulseGround: Color
    var fadeInTime: Float = 0f
    var holdTime: Float = 0f
    var fadeOutTime: Float = 0f

    constructor() : super() {
        this.pulseBg = Color(0f, 0f, 0f, 0f)
        this.pulseGround = Color(0f, 0f, 0f, 0f)
        this.fadeInTime = 0f
        this.holdTime = 0f
        this.fadeOutTime = 0f
    }

    constructor(worldX: Float, pulseBg: Color, pulseGround: Color, fadeInTime: Float, holdTime: Float, fadeOutTime: Float) : super(worldX) {
        this.pulseBg = pulseBg
        this.pulseGround = pulseGround
        this.fadeInTime = fadeInTime
        this.holdTime = holdTime
        this.fadeOutTime = fadeOutTime
    }

    fun init(
        worldX: Float, pulseBg: Color?, pulseGround: Color?,
        fadeInTime: Float, holdTime: Float, fadeOutTime: Float
    ): PulseTrigger {
        this.worldX = worldX
        this.pulseBg = pulseBg!!
        this.pulseGround = pulseGround!!
        this.fadeInTime = fadeInTime
        this.holdTime = holdTime
        this.fadeOutTime = fadeOutTime
        this.fired = false
        return this
    }

    public override fun fire(world: GameWorld) {
        world.startBgPulse(pulseBg, fadeInTime, holdTime, fadeOutTime)
        world.startGroundPulse(
            pulseGround,
            fadeInTime,
            holdTime,
            fadeOutTime
        )
    }
}
