package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals

import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "gravity_portal")
class GravityPortal : AbstractPortal {
    constructor(x: Float, y: Float) : super(x, y) {
        this.type = PortalType.GRAVITY
    }

    constructor() : super() {
        this.type = PortalType.GRAVITY
    }

    override fun init(x: Float, y: Float, rotation: Float): GravityPortal {
        super.init(x, y, rotation)
        this.type = PortalType.GRAVITY
        return this
    }

    override fun init(x: Float, y: Float): GravityPortal {
        return init(x, y, 0f)
    }
}
