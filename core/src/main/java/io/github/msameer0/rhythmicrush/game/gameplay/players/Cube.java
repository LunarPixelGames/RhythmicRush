package io.github.msameer0.rhythmicrush.game.gameplay.players;

public class Cube extends AbstractPlayer {
    public float gravity = -1100f;
    public float jumpVelocity = 465f;
    private boolean jumpHeld = false;
    private boolean isGrounded = false;

    public Cube(float startX, float groundY) {
        super(startX, groundY);
    }

    @Override
    public void update(float delta, float groundY) {
        isGrounded = false; // reset each frame, blocks/ground will set it back

        velocityY += gravity * delta;
        y += velocityY * delta;

        if (y <= groundY) {
            y = groundY;
            velocityY = 0;
            isGrounded = true;
        }

        updateBounds();
    }

    @Override
    public void jump() {
        if (isGrounded) {
            velocityY = jumpVelocity;
            isGrounded = false;
        }
    }

    @Override
    public void setJumpHeld(boolean held) {
        this.jumpHeld = held;
    }

    public void setGrounded(boolean grounded) {
        this.isGrounded = grounded;
    }

    @Override
    public void tryJump() {
        if (jumpHeld && isGrounded) {
            jump();
        }
    }

    public boolean isGrounded() {
        return isGrounded;
    }
}
