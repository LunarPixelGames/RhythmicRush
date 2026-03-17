package io.github.msameer0.rhythmicrush.game.gameplay.players;

import com.badlogic.gdx.math.Rectangle;

import io.github.msameer0.rhythmicrush.game.GameWorld;

/**
 * Represents the base abstract class for all player entities in the game.
 * It defines the core properties such as position, dimensions, velocity, and collision bounds,
 * and provides the necessary structure for movement, jumping logic, and world interaction
 * that specific player implementations must provide.
 */
public abstract class AbstractPlayer {
    public float x, y;
    public float width = 50, height = 50;
    public float velocityY = 0;
    public Rectangle bounds;

    protected GameWorld world;

    public AbstractPlayer(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        bounds = new Rectangle(x, y, width, height);
    }

    public abstract void update(float delta, float groundY);

    public abstract void jump();

    /**
     * Handle continuous jump input (holding)
     */
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

    public void setWorld(GameWorld world) {
        this.world = world;
    }

    public GameWorld getWorld() {
        return world;
    }

    public void setY(float y) {
        this.y = y;
        updateBounds();
    }

    public void setVelocityY(int vel) {
        velocityY = vel;
    }

    public void setGrounded(boolean grounded) {
    }

    public void tryJump() {
    }

    public boolean isGrounded() {
        return false;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public boolean isSafeFromBelow() {
        return false;
    }
}
