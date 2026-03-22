package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import javax.sound.sampled.Port;

import io.github.msameer0.rhythmicrush.game.registries.Registry;

@Registry(id="upside_down_portal")
public class UpsideDownPortal extends AbstractPortal {
    public UpsideDownPortal(float x, float y) {
        super(x, y);
        this.type = PortalType.GRAVITY;
    }

    public UpsideDownPortal() {
        super();
        this.type = PortalType.GRAVITY;
    }

    @Override
    public UpsideDownPortal init(float x, float y) {
        super.init(x, y);
        this.type = PortalType.GRAVITY;
        return this;
    }
}
