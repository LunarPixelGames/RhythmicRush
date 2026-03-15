package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

public abstract class AbstractPortal {
    protected float x, y;
    protected float width = 50, height = 100;
    protected Rectangle bounds;
    protected boolean used = false;

    public AbstractPortal(float x, float y) {
        this.x = x;
        this.y = y;
        bounds = new Rectangle(x, y, width, height);
    }

    /** No-arg constructor for pooling — call init() before use. */
    public AbstractPortal() {
        bounds = new Rectangle();
    }

    /** Reinitialise this portal for reuse from the pool. */
    public AbstractPortal init(float x, float y) {
        this.x    = x;
        this.y    = y;
        this.used = false;
        bounds.set(x, y, width, height);
        return this;
    }

    public void updatePosition(float scrollSpeed, float delta) {
        x -= scrollSpeed * delta;
        updateBounds();
    }

    public Rectangle getBounds() { return bounds; }

    public AbstractPlayer tryTouch(AbstractPlayer player) {
        if (!used && player.getBounds().overlaps(bounds)) {
            used = true;
            return onTouch(player);
        }
        return player;
    }

    public abstract AbstractPlayer onTouch(AbstractPlayer player);

    public void updateBounds() { bounds.setPosition(x, y); }

    public float getX()      { return x; }
    public float getY()      { return y; }
    public float getWidth()  { return width; }
    public float getHeight() { return height; }
    public boolean isUsed()  { return used; }
}
