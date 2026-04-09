package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals

import io.github.msameer0.rhythmicrush.game.registries.Registry

@Registry(id = "cube_portal")
class CubePortal : AbstractPortal {
    constructor(x: Float, y: Float) : super(x, y) {
        this.type = PortalType.CUBE
    }

    constructor() : super() {
        this.type = PortalType.CUBE
    }

    override fun init(x: Float, y: Float): CubePortal {
        super.init(x, y)
        this.type = PortalType.CUBE
        return this
    }
}
