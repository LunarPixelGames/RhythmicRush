package io.github.msameer0.rhythmicrush.game.trigger

import com.badlogic.gdx.graphics.Color
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "color_trigger")
class ColorTrigger : AbstractTrigger {
    var targetBg : Color
    var targetGround : Color
    var fadeDuration : Float

    constructor() : super() {
        this.targetBg = Color(0f, 0f, 0f, 0f)
        this.targetGround = Color(0f, 0f, 0f, 0f)
        this.fadeDuration = 0f
    }

    constructor(worldX : Float, targetBg : Color, targetGround : Color, fadeDuration : Float) : super(worldX) {
        this.targetBg = targetBg
        this.targetGround = targetGround
        this.fadeDuration = fadeDuration
    }

    fun init(
        worldX: Float,
        targetBg: Color?,
        targetGround: Color?,
        fadeDuration: Float
    ): ColorTrigger {
        this.worldX = worldX
        this.targetBg = targetBg!!
        this.targetGround = targetGround!!
        this.fadeDuration = fadeDuration
        this.fired = false
        return this
    }

    override fun fire(world: GameWorld) {
        world.startBgFade(targetBg, fadeDuration)
        world.startGroundFade(targetGround, fadeDuration)
    }
}
