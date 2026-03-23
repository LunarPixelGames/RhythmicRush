package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * A portal that toggles the player's mini state upon contact.
 */
@Registry(id = "mini_portal")
public class MiniPortal extends AbstractPortal {

    public MiniPortal(float x, float y) {
        super(x, y);
        this.type = PortalType.MINI;
    }

    public MiniPortal() {
        super();
        this.type = PortalType.MINI;
    }

    @Override
    public MiniPortal init(float x, float y) {
        super.init(x, y);
        return this;
    }
}
