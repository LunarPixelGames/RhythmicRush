package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.players.AbstractPlayer;

/**
 * Manages practice-mode checkpoints: placement, removal, drawing, and respawning.
 *
 * <p>GameScreen creates one of these only when {@code isPracticeMode == true} and
 * delegates all checkpoint-related logic here, keeping the screen class lean.</p>
 */
public class PracticeManager {

    // ── Checkpoint snapshot ───────────────────────────────────────────────────

    private static class CheckpointState {
        float worldScrolled;
        float playerX;
        float playerWorldX;
        AbstractPlayer.PlayerType playerType;
        float playerY, playerVY;
        boolean gravityFlipped, mini;
        float slopeRotation;
        int triggerIdx;
        final Color baseBgColor     = new Color();
        final Color baseGroundColor = new Color();
        final Color backgroundColor = new Color();
        final Color groundColor     = new Color();
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final GameWorld world;
    private final Array<CheckpointState> checkpoints = new Array<>();

    public PracticeManager(GameWorld world) {
        this.world = world;
    }

    // ── Placement ─────────────────────────────────────────────────────────────

    /** Saves the current world/player state as a new checkpoint. */
    public void placeCheckpoint() {
        CheckpointState s = new CheckpointState();
        s.worldScrolled = world.getWorldScrolled();

        AbstractPlayer p = world.getPlayer();
        s.playerX       = p.getX();
        s.playerWorldX  = p.getWorldX();
        s.playerType    = p.getType();
        s.playerY       = p.getY();
        s.playerVY      = p.getVelocityY();
        s.gravityFlipped = p.isGravityFlipped();
        s.mini          = p.isMini();
        s.slopeRotation = p.getCurrentSlopeRotation();
        s.triggerIdx    = world.getTriggerIdx();

        s.baseBgColor.set(world.getBaseBgColor());
        s.baseGroundColor.set(world.getBaseGroundColor());
        s.backgroundColor.set(world.getBackgroundColor());
        s.groundColor.set(world.getGroundColor());

        checkpoints.add(s);
    }

    /**
     * Removes the most-recently placed checkpoint.
     *
     * @return true if there are still checkpoints remaining after removal.
     */
    public boolean removeLastCheckpoint() {
        if (checkpoints.size > 0) checkpoints.removeIndex(checkpoints.size - 1);
        return checkpoints.size > 0;
    }

    public boolean hasCheckpoints() {
        return checkpoints.size > 0;
    }

    // ── Respawn ───────────────────────────────────────────────────────────────

    /**
     * Restores the world and player to the most recent checkpoint state.
     *
     * <p>The caller is responsible for resetting timers, restarting music at the
     * returned offset, and calling {@code engine.reset()} afterwards.</p>
     *
     * @return The music start-time offset in seconds for the respawn position,
     *         or 0 if no checkpoints exist (caller should do a full respawn).
     */
    public float applyLatestCheckpoint() {
        if (checkpoints.size == 0) return 0f;
        CheckpointState s = checkpoints.peek();

        world.fastForwardTo(s.worldScrolled);
        world.setWorldScrolled(s.worldScrolled);
        world.setBaseBgColor(s.baseBgColor);
        world.setBaseGroundColor(s.baseGroundColor);
        world.setBackgroundColor(s.backgroundColor);
        world.setGroundColor(s.groundColor);
        world.setTriggerIdx(s.triggerIdx);

        AbstractPlayer p = world.obtainPlayer(
            s.playerType == AbstractPlayer.PlayerType.CUBE ? "cube" : "ship");
        p.init(s.playerX, s.playerY, s.playerVY, false);
        p.worldX = s.playerWorldX;
        p.setGravityFlipped(s.gravityFlipped);
        p.setMini(s.mini);
        p.setCurrentSlopeRotation(s.slopeRotation);
        world.setPlayer(p);

        return s.worldScrolled / world.getScrollSpeed();
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    /**
     * Draws diamond markers above each checkpoint position.
     * Must be called inside an open {@code shapes.begin(Filled)} block.
     *
     * @param shapes   The shape renderer to draw with.
     * @param camera   The current game camera (unused directly but available for future culling).
     */
    public void drawCheckpoints(ShapeRenderer shapes, OrthographicCamera camera) {
        if (checkpoints.size == 0) return;

        float scroll = world.getWorldScrolled();
        float hw = 12f, hh = 18f;

        for (CheckpointState s : checkpoints) {
            float dx = s.playerX - (scroll - s.worldScrolled);
            float dy = s.playerY + 25f;

            shapes.setColor(0.1f, 0.4f, 0.1f, 0.8f);
            drawDiamond(shapes, dx, dy, hw + 2f, hh + 2f);

            shapes.setColor(0.6f, 1.0f, 0.2f, 0.9f);
            drawDiamond(shapes, dx, dy, hw, hh);
        }
    }

    // ── Practice button layout ────────────────────────────────────────────────

    private float btnSize;
    private float plusX, plusY, minusX, minusY;

    /**
     * Recalculates practice button positions. Call whenever the camera or scale changes.
     *
     * @param camCX    Camera center X.
     * @param camBot   Camera bottom Y.
     * @param uiPad    UI padding from settings.
     * @param uiScale  Current UI scale factor.
     * @param btnSize  Practice button size for this platform.
     */
    public void updateButtonCoords(float camCX, float camBot, float uiPad,
                                   float uiScale, float btnSize) {
        this.btnSize = btnSize;
        float spacing = 35f * uiScale;
        float totalW  = btnSize * 2 + spacing;
        plusX  = camCX - totalW / 2f;
        plusY  = camBot + uiPad;
        minusX = plusX + btnSize + spacing;
        minusY = plusY;
    }

    /**
     * Draws the + and - practice button backgrounds.
     * Must be called inside an open {@code shapes.begin(Filled)} block.
     */
    public void drawButtonShapes(ShapeRenderer shapes, float opacity) {
        shapes.setColor(0.15f, 0.15f, 0.15f, 0.6f * opacity);
        shapes.rect(plusX,  plusY,  btnSize, btnSize);
        shapes.rect(minusX, minusY, btnSize, btnSize);
    }

    /** Whether a world-space touch lands on the '+' (add checkpoint) button. */
    public boolean hitsPlus(float tx, float ty) {
        return hits(tx, ty, plusX, plusY, btnSize, btnSize);
    }

    /** Whether a world-space touch lands on the '-' (remove checkpoint) button. */
    public boolean hitsMinus(float tx, float ty) {
        return hits(tx, ty, minusX, minusY, btnSize, btnSize);
    }

    public float getPlusX()  { return plusX; }
    public float getPlusY()  { return plusY; }
    public float getMinusX() { return minusX; }
    public float getMinusY() { return minusY; }
    public float getBtnSize() { return btnSize; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void drawDiamond(ShapeRenderer shapes, float x, float y, float hw, float hh) {
        shapes.triangle(x, y + hh, x - hw, y, x + hw, y);
        shapes.triangle(x, y - hh, x - hw, y, x + hw, y);
    }

    private static boolean hits(float tx, float ty, float x, float y, float w, float h) {
        return tx >= x && tx <= x + w && ty >= y && ty <= y + h;
    }
}
