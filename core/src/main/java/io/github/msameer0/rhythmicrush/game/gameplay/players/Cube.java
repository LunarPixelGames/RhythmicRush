package io.github.msameer0.rhythmicrush.game.gameplay.players;

import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * Represents a specific player character implementation that follows a "Cube" mechanic.
 * The Cube is affected by gravity and can perform a single jump when grounded.
 * This class supports object pooling through its no-arg constructor and {@link #init(float, float)} method.
 */
@Registry(id = "cube")
public class Cube extends AbstractPlayer {
    public float gravity = -1800f;
    public float jumpVelocity = 600f;
    private boolean jumpHeld = false;
    private boolean isGrounded = false;

    public Cube(float startX, float groundY) {
        super(startX, groundY);
        this.type = PlayerType.CUBE;
    }

    /**
     * No-arg constructor for pooling — call init() before use.
     */
    public Cube() {
        super(0, 0);
        this.type = PlayerType.CUBE;
    }

    /**
     * Reinitialise this Cube for reuse from the pool.
     */
    @Override
    public Cube init(float startX, float startY, float velocityY, boolean jumpHeld) {
        this.type = PlayerType.CUBE;
        x = startX;
        y = startY;
        this.velocityY = velocityY;
        isGrounded = false;
        this.jumpHeld = jumpHeld;
        this.gravityFlipped = false;
        world = null;
        bounds.setPosition(x, y);
        return this;
    }

    /**
     * Legacy init for backward compatibility.
     */
    @Override
    public Cube init(float startX, float startY) {
        return init(startX, startY, 0, false);
    }

    @Override
    public void update(float delta, float groundY) {
        isGrounded = false;
        float effectiveGravity = gravityFlipped ? -gravity : gravity;
        velocityY += effectiveGravity * delta;
        y += velocityY * delta;

        if (!gravityFlipped) {
            if (y <= groundY) {
                y = groundY;
                velocityY = 0;
                isGrounded = true;
            }
        } else {
            // If we want a ceiling limit, we could add it here.
            // For now, standard ground behavior is only for normal gravity.
        }
        updateBounds();
    }

    @Override
    public void jump() {
        if (isGrounded) {
            velocityY = gravityFlipped ? -jumpVelocity : jumpVelocity;
            isGrounded = false;
        }
    }

    @Override
    public void setJumpHeld(boolean held) {
        this.jumpHeld = held;
    }

    @Override
    public boolean isJumpHeld() {
        return jumpHeld;
    }

    @Override
    public void setGrounded(boolean g) {
        this.isGrounded = g;
    }

    @Override
    public void tryJump() {
        if (jumpHeld && isGrounded) jump();
    }

    @Override
    public boolean isGrounded() {
        return isGrounded;
    }

    @Override
    public void copyState(AbstractPlayer other) {
            this.x = other.x;
            this.y = other.y;
            this.velocityY = other.velocityY;
            this.isGrounded = other.isGrounded();
            this.jumpHeld = other.isJumpHeld();
            this.gravityFlipped = other.isGravityFlipped();
    }
}
