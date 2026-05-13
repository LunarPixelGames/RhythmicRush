package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals

import io.github.msameer0.rhythmicrush.game.registries.Registry

/**
 * A portal that flips the direction of gravity for the player.
 */
@Registry(id = "gravity_portal")
class GravityPortal : AbstractPortal {
    constructor(x: Float, y: Float) : super(x, y) {
        this.type = PortalType.GRAVITY
        this.height = 200f
    }

    constructor() : super() {
        this.type = PortalType.GRAVITY
        this.height = 200f
    }

    override fun init(x: Float, y: Float, rotation: Float): GravityPortal {
        super.init(x, y, rotation)
        this.type = PortalType.GRAVITY
        this.height = 200f
        return this
    }

    override fun init(x: Float, y: Float): GravityPortal {
        return init(x, y, 0f)
    }
}
