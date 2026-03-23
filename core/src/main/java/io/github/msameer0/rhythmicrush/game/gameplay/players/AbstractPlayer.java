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
    public enum PlayerType {
        CUBE,
        SHIP
    }

    protected PlayerType type;
    public float x, y;
    public float width = 50, height = 50;
    public float velocityY = 0;
    public Rectangle bounds;
    protected boolean gravityFlipped = false;
    protected boolean mini = false;

    protected GameWorld world;

    public AbstractPlayer(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        bounds = new Rectangle(x, y, width, height);
    }

    public boolean isMini() {
        return mini;
    }

    public void setMini(boolean mini) {
        // Only apply the centering logic if we are switching FROM regular TO mini
        if (mini && !this.mini) {
            float oldWidth = this.width;
            float oldHeight = this.height;

            this.mini = true;
            this.width = 25;
            this.height = 25;

            // Shift x and y so the center point remains at the same world coordinate
            this.x += (oldWidth - this.width) / 2f;
            this.y += (oldHeight - this.height) / 2f;
        }
        // If going from mini to regular (or if state hasn't changed), use your original logic
        else if (!mini) {
            this.mini = false;
            this.width = 50;
            this.height = 50;
        }

        bounds.setSize(width, height);
        updateBounds(); // Ensure the rectangle follows the new x, y immediately
    }

    public abstract AbstractPlayer init(float startX, float startY, float velocityY, boolean flyHeld);
    public abstract AbstractPlayer init(float startX, float startY);

    public abstract void update(float delta, float groundY);

    public abstract void jump();

    /**
     * Handle continuous jump input (holding)
     */
    public abstract void setJumpHeld(boolean held);

    /**
     * Checks if the jump/fly input is currently being held.
     *
     * @return true if held, false otherwise
     */
    public abstract boolean isJumpHeld();

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

    public void setVelocityY(float vel) {
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

    public PlayerType getType() {
        return type;
    }

    public boolean isSafeFromBelow() {
        return false;
    }

    public boolean isGravityFlipped() {
        return gravityFlipped;
    }

    public void setGravityFlipped(boolean gravityFlipped) {
        this.gravityFlipped = gravityFlipped;
    }

    public abstract void copyState(AbstractPlayer other);
}
