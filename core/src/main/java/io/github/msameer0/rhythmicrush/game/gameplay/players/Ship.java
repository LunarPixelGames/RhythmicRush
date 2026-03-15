package io.github.msameer0.rhythmicrush.game.gameplay.players;

public class Ship extends AbstractPlayer {

    private boolean flyHeld = false;

    public float maxUpSpeed   =  400f;
    public float maxDownSpeed = -500f;
    public float accel        = 1000f;
    public float decel        =  800f;

    private float groundY = 50f;

    public Ship(float startX, float startY) { super(startX, startY); }

    /** No-arg constructor for pooling — call init() before use. */
    public Ship() { super(0, 0); }

    /** Reinitialise this Ship for reuse from the pool. */
    public Ship init(float startX, float startY) {
        x         = startX;
        y         = startY;
        velocityY = 0;
        flyHeld   = false;
        world     = null;
        bounds.setPosition(x, y);
        return this;
    }

    @Override
    public void update(float delta, float groundY) {
        this.groundY = groundY;
        if (flyHeld) {
            velocityY += accel * delta;
            if (velocityY > maxUpSpeed) velocityY = maxUpSpeed;
        } else {
            velocityY -= decel * delta;
            if (velocityY < maxDownSpeed) velocityY = maxDownSpeed;
        }
        y += velocityY * delta;
        if (y < groundY) { y = groundY; velocityY = 0; }
        updateBounds();
    }

    @Override public void jump()                    { /* not used */ }
    @Override public void setJumpHeld(boolean held) { flyHeld = held; }
    @Override public boolean isSafeFromBelow()      { return true; }
}
