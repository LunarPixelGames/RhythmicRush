package io.github.msameer0.rhythmicrush.game.gameplay.players;

public class Cube extends AbstractPlayer {
    public float gravity      = -1800f;
    public float jumpVelocity =  600f;
    private boolean jumpHeld   = false;
    private boolean isGrounded = false;

    public Cube(float startX, float groundY) { super(startX, groundY); }

    /** No-arg constructor for pooling — call init() before use. */
    public Cube() { super(0, 0); }

    /** Reinitialise this Cube for reuse from the pool. */
    public Cube init(float startX, float startY) {
        x          = startX;
        y          = startY;
        velocityY  = 0;
        isGrounded = false;
        jumpHeld   = false;
        world      = null;
        bounds.setPosition(x, y);
        return this;
    }

    @Override
    public void update(float delta, float groundY) {
        isGrounded = false;
        velocityY += gravity * delta;
        y += velocityY * delta;
        if (y <= groundY) { y = groundY; velocityY = 0; isGrounded = true; }
        updateBounds();
    }

    @Override public void jump() {
        if (isGrounded) { velocityY = jumpVelocity; isGrounded = false; }
    }

    @Override public void setJumpHeld(boolean held) { this.jumpHeld = held; }
    @Override public void setGrounded(boolean g)    { this.isGrounded = g; }
    @Override public void tryJump()                 { if (jumpHeld && isGrounded) jump(); }
    @Override public boolean isGrounded()           { return isGrounded; }
}
