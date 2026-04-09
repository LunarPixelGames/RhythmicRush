package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals

import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "mini_portal")
class MiniPortal : AbstractPortal {
    constructor(x: Float, y: Float) : super(x, y) {
        this.type = PortalType.MINI
    }

    constructor() : super() {
        this.type = PortalType.MINI
    }

    override fun init(x: Float, y: Float, rotation: Float): MiniPortal {
        super.init(x, y, rotation)
        this.type = PortalType.MINI
        return this
    }

    override fun init(x: Float, y: Float): MiniPortal {
        return init(x, y, 0f)
    }
}
