package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals

import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "ship_portal")
class ShipPortal : AbstractPortal {
    constructor(x: Float, y: Float) : super(x, y) {
        this.type = PortalType.SHIP
    }

    constructor() : super() {
        this.type = PortalType.SHIP
    }

    override fun init(x: Float, y: Float, rotation: Float): ShipPortal {
        super.init(x, y, rotation)
        this.type = PortalType.SHIP
        return this
    }

    override fun init(x: Float, y: Float): ShipPortal {
        return init(x, y, 0f)
    }
}
