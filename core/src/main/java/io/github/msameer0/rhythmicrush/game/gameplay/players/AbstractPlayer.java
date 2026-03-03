package io.github.msameer0.rhythmicrush.game.gameplay.players;

import com.badlogic.gdx.math.Rectangle;

public abstract class AbstractPlayer {
    public float x, y;
    public float width = 50, height = 50;
    public float velocityY = 0;
    public Rectangle bounds;

    public AbstractPlayer(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        bounds = new Rectangle(x, y, width, height);
    }

    public abstract void update(float delta, float groundY);

    public abstract void jump();

    /** Handle continuous jump input (holding) */
    public abstract void setJumpHeld(boolean held);

    public Rectangle getBounds() {
        return bounds;
    }

    protected void updateBounds() {
        bounds.setPosition(x, y);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
