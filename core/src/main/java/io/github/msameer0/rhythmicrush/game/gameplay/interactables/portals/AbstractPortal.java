package io.github.msameer0.rhythmicrush.game.gameplay.interactables.portals;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

public abstract class AbstractPortal {
    protected float x, y;
    protected float width = 50, height = 100; // size of portal
    protected Rectangle bounds;
    protected boolean used = false;

    public AbstractPortal(float x, float y) {
        this.x = x;
        this.y = y;
        bounds = new Rectangle(x, y, width, height);
    }

    // Called every frame to move portal
    public void updatePosition(float scrollSpeed, float delta) {
        x -= scrollSpeed * delta;
        updateBounds(); // update collision rectangle
    }

    public Rectangle getBounds() {
        return bounds;
    }

    // Only apply once
    public AbstractPlayer tryTouch(AbstractPlayer player) {
        if (!used && player.getBounds().overlaps(bounds)) {
            used = true;
            return onTouch(player);
        }
        return player; // no change if already used
    }

    public abstract AbstractPlayer onTouch(AbstractPlayer player);

    public void updateBounds() {
        bounds.setPosition(x, y);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public boolean isUsed() { return used; }
}
