package io.github.msameer0.rhythmicrush.game.gameplay.players;

import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * Represents a specific player mode where the player controls a flying vehicle.
 * <p>
 * The Ship movement is characterized by continuous vertical acceleration when the
 * jump key is held and gravity-like deceleration when released. It features
 * terminal velocity constants for both upward and downward movement.
 * </p>
 */
@Registry(id = "ship")
public class Ship extends AbstractPlayer {

    private boolean flyHeld = false;

    public float maxUpSpeed = 400f;
    public float maxDownSpeed = -500f;
    public float accel = 1000f;
    public float decel = 800f;

    private float groundY = 50f;

    public Ship(float startX, float startY) {
        super(startX, startY);
        this.type = PlayerType.SHIP;
    }

    /**
     * No-arg constructor for pooling — call init() before use.
     */
    public Ship() {
        super(0, 0);
        this.type = PlayerType.SHIP;
    }

    /**
     * Reinitialise this Ship for reuse from the pool.
     */
    @Override
    public Ship init(float startX, float startY, float velocityY, boolean flyHeld) {
        this.type = PlayerType.SHIP;
        x = startX;
        y = startY;
        this.velocityY = velocityY;
        this.flyHeld = flyHeld;
        this.gravityFlipped = false;
        setMini(false);
        world = null;
        bounds.setPosition(x, y);
        return this;
    }

    /**
     * Legacy init for backward compatibility.
     */
    @Override
    public Ship init(float startX, float startY) {
        return init(startX, startY, 0, false);
    }

    @Override
    public void update(float delta, float groundY) {
        this.groundY = groundY;
        float effectiveAccel = mini ? accel * 1.3f : accel;
        float effectiveDecel = mini ? decel * 1.3f : decel;
        
        if (!gravityFlipped) {
            if (flyHeld) {
                velocityY += effectiveAccel * delta;
                if (velocityY > maxUpSpeed) velocityY = maxUpSpeed;
            } else {
                velocityY -= effectiveDecel * delta;
                if (velocityY < maxDownSpeed) velocityY = maxDownSpeed;
            }
        } else {
            if (flyHeld) {
                velocityY -= effectiveAccel * delta;
                if (velocityY < -maxUpSpeed) velocityY = -maxUpSpeed;
            } else {
                velocityY += effectiveDecel * delta;
                if (velocityY > -maxDownSpeed) velocityY = -maxDownSpeed;
            }
        }

        y += velocityY * delta;

        if (!gravityFlipped) {
            if (y < groundY) {
                y = groundY;
                velocityY = 0;
            }
        }
        updateBounds();
    }

    @Override
    public void jump() {}

    @Override
    public void setJumpHeld(boolean held) {
        flyHeld = held;
    }

    @Override
    public boolean isJumpHeld() {
        return flyHeld;
    }

    @Override
    public boolean isSafeFromBelow() {
        return true;
    }

    public float getGroundY() {
        return groundY;
    }

    public void setGroundY(float groundY) {
        this.groundY = groundY;
    }

    @Override
    public void copyState(AbstractPlayer other) {
        this.x = other.x;
        this.y = other.y;
        this.velocityY = other.velocityY;
        this.gravityFlipped = other.isGravityFlipped();
        this.flyHeld = other.isJumpHeld();
        setMini(other.isMini());
    }
}
