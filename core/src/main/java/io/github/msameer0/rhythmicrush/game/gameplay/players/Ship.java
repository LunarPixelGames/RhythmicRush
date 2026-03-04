package io.github.msameer0.rhythmicrush.game.gameplay.players;

public class Ship extends AbstractPlayer {

    private boolean flyHeld = false;

    public float maxUpSpeed = 400f;      // maximum upward speed
    public float maxDownSpeed = -500f;   // maximum downward speed
    public float accel = 1000f;          // acceleration per second when holding
    public float decel = 800f;           // deceleration per second when releasing

    private float groundY = 50;          // min Y (ground)

    public Ship(float startX, float startY) {
        super(startX, startY);
    }

    @Override
    public void update(float delta, float groundY) {
        this.groundY = groundY;

        if (flyHeld) {
            // accelerate upward
            velocityY += accel * delta;
            if (velocityY > maxUpSpeed) velocityY = maxUpSpeed;
        } else {
            // decelerate upward or accelerate downward
            velocityY -= decel * delta;
            if (velocityY < maxDownSpeed) velocityY = maxDownSpeed;
        }

        // apply vertical movement
        y += velocityY * delta;

        // prevent going below ground
        if (y < groundY) {
            y = groundY;
            velocityY = 0;
        }

        updateBounds();
    }

    @Override
    public void jump() {
        // not used for Ship
    }

    @Override
    public void setJumpHeld(boolean held) {
        flyHeld = held;
    }

    @Override
    public boolean isSafeFromBelow() {
        return true;
    }
}
