package io.github.msameer0.rhythmicrush.game.gameplay.hazards;

import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;
import io.github.msameer0.rhythmicrush.game.registries.Registry;

/**
 * A circular spinning hazard. Its texture is a 256×256 image whose natural
 * radius equals one player-width (50 units). The collision shape is a true
 * circle, and the sprite rotates continuously at a configurable speed.
 *
 * <p>Size is expressed as the <em>diameter</em> of the saw in world units,
 * matching the convention used for blocks (one tile = 50 units). The default
 * size of 100 therefore gives a radius of 50 — exactly one player-width.</p>
 *
 * <p>Supports object pooling via the no-arg constructor and {@link #init}.</p>
 */
@Registry(id = "saw_blade")
public class SawBlade extends AbstractHazard {

    /** Default diameter in world units (= 2 × player width). */
    public static final float DEFAULT_SIZE = 100f;

    /**
     * How fast the blade spins, in degrees per second.
     * Positive = counter-clockwise (LibGDX convention).
     * Negative = clockwise.
     */
    public float degreesPerSecond = 120f;

    /** Accumulated visual rotation in degrees. Driven by {@link #updatePosition}. */
    private float visualRotation = 0f;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Full constructor used when creating directly (not from pool). */
    public SawBlade(float x, float y, float diameter, float degreesPerSecond) {
        super(x, y, diameter, diameter);
        this.type = HazardType.SAW_BLADE;
        this.degreesPerSecond = degreesPerSecond;
    }

    /** No-arg constructor for object pooling — call {@link #init} before use. */
    public SawBlade() {
        super(0, 0, DEFAULT_SIZE, DEFAULT_SIZE);
        this.type = HazardType.SAW_BLADE;
    }

    // ── Pool init ─────────────────────────────────────────────────────────────

    /**
     * Reinitialises this instance for reuse from the pool.
     *
     * @param x              world X of the saw centre's bottom-left bounding corner
     * @param y              world Y of the saw centre's bottom-left bounding corner
     * @param diameter       diameter in world units (width = height = diameter)
     * @param degreesPerSec  spin speed — positive = CCW, negative = CW
     */
    public SawBlade init(float x, float y, float diameter, float degreesPerSec) {
        this.x = x;
        this.y = y;
        this.width  = diameter;
        this.height = diameter;
        this.degreesPerSecond = degreesPerSec;
        this.visualRotation = 0f;
        bounds.set(x, y, diameter, diameter);
        return this;
    }

    /** Convenience init with default spin speed. */
    public SawBlade init(float x, float y, float diameter) {
        return init(x, y, diameter, degreesPerSecond);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Scrolls the blade leftward and advances the visual rotation.
     * Called every physics tick by GameWorld.
     */
    @Override
    public void updatePosition(float scrollSpeed, float delta) {
        super.updatePosition(scrollSpeed, delta);
        // rotation is now handled per render frame in tickVisualRotation()
    }

    // ── Collision ─────────────────────────────────────────────────────────────

    /**
     * Overrides the default AABB overlap check with a true circle–rectangle test.
     *
     * <p>The blade's hitbox is a circle centred on the sprite. The player's hitbox
     * is a rectangle. We find the closest point on the rectangle to the circle centre
     * and kill the player if it lies within the circle's radius.</p>
     */
    @Override
    public void tryTouch(AbstractPlayer player) {
        float radius = width * 0.5f;
        float cx = x + radius;
        float cy = y + radius;

        com.badlogic.gdx.math.Rectangle pr = player.getBounds();

        // Closest point on the player AABB to the circle centre
        float closestX = Math.max(pr.x, Math.min(cx, pr.x + pr.width));
        float closestY = Math.max(pr.y, Math.min(cy, pr.y + pr.height));

        float dx = cx - closestX;
        float dy = cy - closestY;

        if (dx * dx + dy * dy <= radius * radius) {
            onTouch(player);
        }
    }

    /** Called once per rendered frame (not per physics tick) to keep the spin smooth. */
    public void tickVisualRotation(float delta) {
        visualRotation += degreesPerSecond * delta;
        visualRotation = ((visualRotation % 360f) + 360f) % 360f;
    }

    @Override
    public void onTouch(AbstractPlayer player) {
        if (player.getWorld() != null) {
            player.getWorld().playerDied();
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Current visual rotation in degrees (use this in the renderer). */
    public float getVisualRotation() {
        return visualRotation;
    }

    /** Diameter of the blade in world units. */
    public float getDiameter() {
        return width; // width == height == diameter
    }
}
