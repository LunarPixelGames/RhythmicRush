package io.github.msameer0.rhythmicrush.game.gameplay.players;

public class Cube extends AbstractPlayer {
    public float gravity = -1055f;
    public float jumpVelocity = 425f;
    private boolean jumpHeld = false;

    public Cube(float startX, float groundY) {
        super(startX, groundY);
    }

    @Override
    public void update(float delta, float groundY) {
        velocityY += gravity * delta;
        y += velocityY * delta;

        if (y <= groundY) {
            y = groundY;
            velocityY = 0;
        }

        updateBounds();

        // Continuous jump if holding input
        if (jumpHeld && velocityY == 0) {
            jump();
        }
    }

    @Override
    public void jump() {
        if (velocityY == 0) {
            velocityY = jumpVelocity;
        }
    }

    @Override
    public void setJumpHeld(boolean held) {
        this.jumpHeld = held;
    }
}
