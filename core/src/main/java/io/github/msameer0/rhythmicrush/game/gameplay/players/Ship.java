package io.github.msameer0.rhythmicrush.game.gameplay.players;

/**
 * Represents a specific player mode where the player controls a flying vehicle.
 * <p>
 * The Ship movement is characterized by continuous vertical acceleration when the
 * jump key is held and gravity-like deceleration when released. It features
 * terminal velocity constants for both upward and downward movement.
 * </p>
 */
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
    public Ship init(float startX, float startY) {
        this.type = PlayerType.SHIP;
        x = startX;
        y = startY;
        velocityY = 0;
        flyHeld = false;
        world = null;
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
        if (y < groundY) {
            y = groundY;
            velocityY = 0;
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
    public boolean isSafeFromBelow() {
        return true;
    }
}
