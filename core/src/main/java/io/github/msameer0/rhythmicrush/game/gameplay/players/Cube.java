package io.github.msameer0.rhythmicrush.game.gameplay.players;

import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * Represents a specific player character implementation that follows a "Cube" mechanic.
 * The Cube is affected by gravity and can perform a single jump when grounded.
 * This class supports object pooling through its no-arg constructor and {@link #init(float, float)} method.
 *
 * <h3>Coyote time</h3>
 * After walking off a platform edge, the player has a short window ({@value COYOTE_TIME} seconds)
 * where they can still jump as if grounded — matching original GD behaviour.
 */
@Registry(id = "cube")
public class Cube extends AbstractPlayer {

    public float gravity      = -1800f;
    public float jumpVelocity =   600f;

    private boolean jumpHeld   = false;
    private boolean isGrounded = false;

    // ── Coyote time ───────────────────────────────────────────────────────────
    /** How long (in seconds) the player can still jump after leaving a platform. */
    private static final float COYOTE_TIME = 0.083f; // ~20 steps at 240 TPS, matches GD
    private float coyoteTimer = 0f;

    public Cube(float startX, float groundY) {
        super(startX, groundY);
        this.type = PlayerType.CUBE;
    }

    /** No-arg constructor for pooling — call init() before use. */
    public Cube() {
        super(0, 0);
        this.type = PlayerType.CUBE;
    }

    @Override
    public Cube init(float startX, float startY, float velocityY, boolean jumpHeld) {
        this.type           = PlayerType.CUBE;
        x                   = startX;
        y                   = startY;
        this.velocityY      = velocityY;
        isGrounded          = false;
        this.jumpHeld       = jumpHeld;
        this.gravityFlipped = false;
        coyoteTimer         = 0f;
        world               = null;
        bounds.setPosition(x, y);
        return this;
    }

    @Override
    public Cube init(float startX, float startY) {
        return init(startX, startY, 0, false);
    }

    @Override
    public void update(float delta, float groundY) {
        boolean wasGrounded = isGrounded;
        isGrounded = false;

        float effectiveGravity = gravityFlipped ? -gravity : gravity;
        velocityY += effectiveGravity * delta;
        y += velocityY * delta;

        if (!gravityFlipped) {
            if (y <= groundY) {
                y          = groundY;
                velocityY  = 0;
                isGrounded = true;
            }
        }

        // Coyote timer: reset to full window when grounded, count down when airborne
        if (isGrounded) {
            coyoteTimer = COYOTE_TIME;
        } else {
            if (wasGrounded) {
                // Just left the ground this step — keep the full window, start counting next step
            } else {
                coyoteTimer = Math.max(0f, coyoteTimer - delta);
            }
        }

        updateBounds();
    }

    /** True if the cube can jump — either grounded or within the coyote window. */
    private boolean canJump() {
        return isGrounded || coyoteTimer > 0f;
    }

    @Override
    public void jump() {
        if (canJump()) {
            velocityY   = gravityFlipped ? -jumpVelocity : jumpVelocity;
            isGrounded  = false;
            coyoteTimer = 0f; // consume the window so it can't fire twice
        }
    }

    @Override
    public void tryJump() {
        if (jumpHeld && canJump()) jump();
    }

    @Override
    public void setJumpHeld(boolean held) { this.jumpHeld = held; }

    @Override
    public boolean isJumpHeld() { return jumpHeld; }

    @Override
    public void setGrounded(boolean g) { this.isGrounded = g; }

    @Override
    public boolean isGrounded() { return isGrounded; }

    @Override
    public void copyState(AbstractPlayer other) {
        this.x              = other.x;
        this.y              = other.y;
        this.velocityY      = other.velocityY;
        this.isGrounded     = other.isGrounded();
        this.jumpHeld       = other.isJumpHeld();
        this.gravityFlipped = other.isGravityFlipped();
        this.coyoteTimer    = 0f; // don't carry coyote state across portal transitions
    }
}
