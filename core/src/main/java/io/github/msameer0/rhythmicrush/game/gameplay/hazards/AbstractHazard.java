package io.github.msameer0.rhythmicrush.game.gameplay.hazards;

import com.badlogic.gdx.math.Rectangle;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

public abstract class AbstractHazard {
    protected float x, y;
    protected float width, height;
    protected Rectangle bounds;

    public AbstractHazard(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        bounds = new Rectangle(x, y, width, height);
    }

    //move backwards
    public void updatePosition(float scrollSpeed, float delta) {
        x -= scrollSpeed * delta;
        updateBounds();
    }

    public Rectangle getBounds() {
        return bounds;
    }

    protected void updateBounds() {
        bounds.setPosition(x, y);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    //called every frame to check collision
    public void tryTouch(AbstractPlayer player) {
        if (player.getBounds().overlaps(bounds)) {
            onTouch(player);
        }
    }

    //calls on collision
    public abstract void onTouch(AbstractPlayer player);
}
