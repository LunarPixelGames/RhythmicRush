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

    public float gravity;
    protected PlayerType type;
    public float x, y;
    public float worldX;
    public float width = 50, height = 50;
    public float velocityY = 0;
    public Rectangle bounds;
    protected boolean gravityFlipped = false;
    protected boolean mini = false;
    protected float currentSlopeRotation = 0f;

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
        float oldWidth = this.width;
        float oldHeight = this.height;

        if (mini && !this.mini) {
            this.mini = true;
            this.width = 25;
            this.height = 25;

            this.x += (oldWidth - this.width) / 2f;
            this.y += (oldHeight - this.height) / 2f;
        }
        else if (!mini && this.mini) {
            this.mini = false;
            this.width = 50;
            this.height = 50;

            this.x -= (this.width - oldWidth) / 2f;

            if (gravityFlipped) {
                this.y -= (this.height - oldHeight);
            }
        }

        bounds.setSize(width, height);
        updateBounds();
    }

    public float getGravity() {
        return gravity;
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

    public float getWorldX() {
        return worldX;
    }

    public void setWorldX(float worldX) {
        this.worldX = worldX;
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

    public float getCurrentSlopeRotation() {
        return currentSlopeRotation;
    }

    public void setCurrentSlopeRotation(float rot) {
        this.currentSlopeRotation = rot;
    }

    public abstract void copyState(AbstractPlayer other);
}
