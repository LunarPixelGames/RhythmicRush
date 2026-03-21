package io.github.msameer0.rhythmicrush.lwjgl3.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;

import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelSerializer;

import com.badlogic.gdx.utils.Array;

/**
 * PC-only level editor screen.
 *
 * Controls:
 *   LEFT / RIGHT arrow          — cycle through object palette
 *   TAB                         — cycle block type
 *   Q / E                       — rotate spike
 *   G                           — toggle grid snap
 *   Left click (empty space)    — place object at cursor
 *   Left click (on object)      — select that object (deselect others)
 *   SHIFT + Left click          — add/remove object from selection
 *   Left click drag (empty)     — rubber-band multi-select
 *   WASD                        — move selected objects one grid cell
 *   CTRL + C                    — copy selected objects
 *   CTRL + V                    — paste (pasted objects become new selection, offset by 1 grid)
 *   CTRL + A                    — select all
 *   ESC                         — deselect all
 *   DEL                         — delete selected objects
 *   ALT + Left click            — edit color trigger under cursor
 *   CTRL + Right click          — delete object(s) under cursor (or all selected)
 *   Right click + drag          — pan camera
 *   SHIFT + `                   — open music selector
 *   `                           — play / pause music
 *   CTRL + `                    — stop music (resets position + editor colors)
 *   CTRL + S                    — save level
 *   CTRL + O                    — load level
 *   ENTER                       — launch playtest
 */
public class LevelEditorScreen implements Screen {

    private EditorObjectRenderer objectRenderer;
    private int   blockTypeIndex       = 0;
    private float currentSpikeRotation = 0f;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float GROUND_Y           = 50f;
    private static final float SCROLL_SPEED       = 320f;
    private static final float GRID_SIZE          = 50f;
    private static final float OBJECT_SIZE        = 50f;
    private static final Color SPAWN_LINE_COLOR   = new Color(1f,  0.2f,  0.2f,  1f);
    private static final Color MUSIC_LINE_COLOR   = new Color(0.2f, 1f,   0.4f,  0.85f);
    private static final Color TRIGGER_LINE_COLOR = new Color(1f,  0.85f, 0.2f,  0.8f);
    private static final Color GRID_COLOR         = new Color(1f,  1f,    1f,    0.08f);
    private static final Color SELECTION_COLOR    = new Color(0.3f, 0.8f,  1f,    0.9f);
    private static final Color SELECTION_FILL     = new Color(0.3f, 0.8f,  1f,    0.12f);
    private static final Color RUBBER_COLOR       = new Color(0.4f, 0.9f,  1f,    0.6f);

    // WASD key-repeat tuning
    private static final float WASD_INITIAL_DELAY = 0.28f;
    private static final float WASD_REPEAT_RATE   = 0.07f;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final PaletteEntry[] PALETTE = {
        new PaletteEntry("Block",         "block",         new Color(0.55f, 0.55f, 0.85f, 1f)),
        new PaletteEntry("Spike",         "spike",         new Color(0.85f, 0.25f, 0.25f, 1f)),
        new PaletteEntry("Cube Portal",   "cube_portal",   new Color(0.25f, 0.85f, 0.55f, 1f)),
        new PaletteEntry("Ship Portal",   "ship_portal",   new Color(0.25f, 0.55f, 0.85f, 1f)),
        new PaletteEntry("Color Trigger", "color_trigger", new Color(1f,    0.85f, 0.2f,  1f)),
        new PaletteEntry("Pulse Trigger", "pulse_trigger", new Color(1f,    0.4f,  0.8f,  1f)),
    };

    // ── Editor state ──────────────────────────────────────────────────────────
    private final LevelEditorApp app;
    private int     paletteIndex = 0;
    private boolean snapToGrid   = true;

    private final Array<LevelData.ObjectEntry> placed = new Array<>();
    private LevelData levelMeta = new LevelData();

    // ── Selection ─────────────────────────────────────────────────────────────
    private final Array<LevelData.ObjectEntry>  selection = new Array<>();
    private final Array<LevelData.ObjectEntry> clipboard = new Array<>();

    // Rubber-band drag
    private boolean rubberBanding = false;
    private float   rubberStartX, rubberStartY;   // world coords at drag start
    private float   rubberEndX,   rubberEndY;

    // WASD repeat state
    private int   wasdDx = 0, wasdDy = 0;
    private float wasdTimer = 0f;

    // ── Camera / pan ──────────────────────────────────────────────────────────
    private OrthographicCamera camera;
    private float   cameraX    = 0f;
    private boolean panning    = false;
    private float   lastMouseX;

    // ── Music ─────────────────────────────────────────────────────────────────
    private Music         music;
    private boolean       musicSelectorOpen = false;
    private StringBuilder musicInputBuffer  = new StringBuilder();

    // ── Editor preview colors ─────────────────────────────────────────────────
    private Color baseEditorBgColor     = new Color(0.1f,  0.1f,  0.18f, 1f);
    private Color baseEditorGroundColor = new Color(0.09f, 0.13f, 0.24f, 1f);
    private Color editorBgColor     = new Color(baseEditorBgColor);
    private Color editorGroundColor = new Color(baseEditorGroundColor);
    private EditorColorFade bgFade     = null;
    private EditorColorFade groundFade = null;
    private EditorColorPulse bgPulse     = null;
    private EditorColorPulse groundPulse = null;
    private final Array<LevelData.ObjectEntry> firedTriggers = new Array<>();

    private static class EditorColorFade {
        final Color from; final Color to; final float duration; float elapsed = 0f;
        EditorColorFade(Color from, Color to, float duration) {
            this.from = new Color(from); this.to = new Color(to); this.duration = duration;
        }
    }

    private static class EditorColorPulse {
        final Color target; final float fadeIn, hold, fadeOut; float elapsed = 0f;
        EditorColorPulse(Color target, float fadeIn, float hold, float fadeOut) {
            this.target = new Color(target); this.fadeIn = fadeIn; this.hold = hold; this.fadeOut = fadeOut;
        }
        float getIntensity() {
            if (elapsed < fadeIn) return fadeIn > 0 ? elapsed / fadeIn : 1f;
            if (elapsed < fadeIn + hold) return 1f;
            if (elapsed < fadeIn + hold + fadeOut) return fadeOut > 0 ? 1f - (elapsed - fadeIn - hold) / fadeOut : 0f;
            return 0f;
        }
        boolean isFinished() { return elapsed >= fadeIn + hold + fadeOut; }
    }

    // ── Color trigger edit menu ───────────────────────────────────────────────

    private boolean               editMenuOpen  = false;
    private LevelData.ObjectEntry editTarget    = null;
    private int                   editField     = 0;
    private StringBuilder editBgInput    = new StringBuilder();
    private StringBuilder editGndInput   = new StringBuilder();
    private StringBuilder editFadeInput  = new StringBuilder();
    private StringBuilder editHoldInput  = new StringBuilder();
    private StringBuilder editFadeInInput  = new StringBuilder();
    private StringBuilder editFadeOutInput = new StringBuilder();

    // ── Rendering ─────────────────────────────────────────────────────────────

    private SpriteBatch   batch;
    private ShapeRenderer shapes;
    private BitmapFont    font;

    // ── Save/Load prompt ──────────────────────────────────────────────────────
    private boolean       filePromptOpen   = false;
    private boolean       filePromptIsSave = false;
    private StringBuilder fileBuffer       = new StringBuilder();

    // ─────────────────────────────────────────────────────────────────────────

    public LevelEditorScreen(LevelEditorApp app) {
        this.app = app;
    }

    @Override
    public void show() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera = new OrthographicCamera(w, h);
        camera.position.set(w / 2f, h / 2f, 0);
        camera.update();

        batch  = new SpriteBatch();
        shapes = new ShapeRenderer();
        font   = new BitmapFont();
        font.getData().setScale(1.3f);

        objectRenderer = new EditorObjectRenderer(batch, shapes, camera);
        Gdx.input.setInputProcessor(new EditorInputAdapter());
    }

    // ── Main loop ─────────────────────────────────────────────────────────────

    @Override
    public void render(float delta) {
        updateEditorColors(delta);

        Gdx.gl.glClearColor(editorBgColor.r, editorBgColor.g, editorBgColor.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleKeyboard(delta);

        camera.position.x = cameraX + Gdx.graphics.getWidth() / 2f;
        camera.update();

        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawGrid();
        drawGround();
        drawSpawnLine();
        drawPlacedObjects();
        drawSelectionHighlights();
        drawRubberBand();
        drawColorTriggerLines();
        drawMusicLine();
        drawCursorPreview();
        drawHUD();

        if (musicSelectorOpen) drawMusicSelector();
        if (filePromptOpen)    drawFilePrompt();
        if (editMenuOpen)      drawEditMenu();
    }

    // ── Editor color preview ──────────────────────────────────────────────────

    private void updateEditorColors(float delta) {
        if (music == null || !music.isPlaying()) return;
        float musicX = 100f + music.getPosition() * SCROLL_SPEED;
        for (LevelData.ObjectEntry e : placed) {
            if (!"color_trigger".equals(e.type) && !"pulse_trigger".equals(e.type)) continue;
            if (firedTriggers.contains(e, true)) continue;
            if (musicX >= e.x + OBJECT_SIZE / 2f) {
                firedTriggers.add(e);
                if ("color_trigger".equals(e.type)) {
                    if (e.triggerBgColor != null && !e.triggerBgColor.isEmpty())
                        bgFade = new EditorColorFade(baseEditorBgColor, GameWorld.hexToColor(e.triggerBgColor), e.fadeDuration);
                    if (e.triggerGroundColor != null && !e.triggerGroundColor.isEmpty())
                        groundFade = new EditorColorFade(baseEditorGroundColor, GameWorld.hexToColor(e.triggerGroundColor), e.fadeDuration);
                } else if ("pulse_trigger".equals(e.type)) {
                    if (e.pulseBgColor != null && !e.pulseBgColor.isEmpty())
                        bgPulse = new EditorColorPulse(GameWorld.hexToColor(e.pulseBgColor), e.fadeInTime, e.holdTime, e.fadeOutTime);
                    if (e.pulseGroundColor != null && !e.pulseGroundColor.isEmpty())
                        groundPulse = new EditorColorPulse(GameWorld.hexToColor(e.pulseGroundColor), e.fadeInTime, e.holdTime, e.fadeOutTime);
                }
            }
        }
        if (bgFade != null) {
            bgFade.elapsed += delta;
            float t = Math.min(bgFade.elapsed / bgFade.duration, 1f);
            baseEditorBgColor.set(lerp(bgFade.from.r,bgFade.to.r,t), lerp(bgFade.from.g,bgFade.to.g,t), lerp(bgFade.from.b,bgFade.to.b,t), 1f);
            if (t >= 1f) bgFade = null;
        }
        if (groundFade != null) {
            groundFade.elapsed += delta;
            float t = Math.min(groundFade.elapsed / groundFade.duration, 1f);
            baseEditorGroundColor.set(lerp(groundFade.from.r,groundFade.to.r,t), lerp(groundFade.from.g,groundFade.to.g,t), lerp(groundFade.from.b,groundFade.to.b,t), 1f);
            if (t >= 1f) groundFade = null;
        }

        if (bgPulse != null) {
            bgPulse.elapsed += delta;
            if (bgPulse.isFinished()) bgPulse = null;
        }
        if (groundPulse != null) {
            groundPulse.elapsed += delta;
            if (groundPulse.isFinished()) groundPulse = null;
        }

        editorBgColor.set(baseEditorBgColor);
        if (bgPulse != null) editorBgColor.lerp(bgPulse.target, bgPulse.getIntensity());

        editorGroundColor.set(baseEditorGroundColor);
        if (groundPulse != null) editorGroundColor.lerp(groundPulse.target, groundPulse.getIntensity());
    }

    private void resetEditorColors() {
        String bg  = (levelMeta.bgColor     != null && !levelMeta.bgColor.isEmpty())     ? levelMeta.bgColor     : "1a1a2e";
        String gnd = (levelMeta.groundColor != null && !levelMeta.groundColor.isEmpty()) ? levelMeta.groundColor : "16213e";
        baseEditorBgColor.set(GameWorld.hexToColor(bg));
        baseEditorGroundColor.set(GameWorld.hexToColor(gnd));
        editorBgColor.set(baseEditorBgColor);
        editorGroundColor.set(baseEditorGroundColor);
        bgFade = null; groundFade = null;
        bgPulse = null; groundPulse = null;
        firedTriggers.clear();
    }

    // ── Keyboard handling ─────────────────────────────────────────────────────

    private void handleKeyboard(float delta) {
        if (musicSelectorOpen || filePromptOpen || editMenuOpen) return;

        boolean ctrl  = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)  || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        // Palette / spike rotation — only meaningful when nothing selected
        if (selection.size == 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB))
                blockTypeIndex = (blockTypeIndex + 1) % BlockType.values().length;
            if (PALETTE[paletteIndex].type.equals("spike")) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) currentSpikeRotation = (currentSpikeRotation + 90f) % 360f;
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) currentSpikeRotation = (currentSpikeRotation - 90f + 360f) % 360f;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) paletteIndex = (paletteIndex + 1) % PALETTE.length;
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  paletteIndex = (paletteIndex - 1 + PALETTE.length) % PALETTE.length;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) snapToGrid = !snapToGrid;

        // Music shortcuts
        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            if (shift)     { musicSelectorOpen = true; musicInputBuffer.setLength(0); }
            else if (ctrl) { if (music != null) { music.stop(); resetEditorColors(); } }
            else           { toggleMusic(); }
        }

        // Save / Load
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.S)) { filePromptOpen = true; filePromptIsSave = true;  fileBuffer.setLength(0); }
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.O)) { filePromptOpen = true; filePromptIsSave = false; fileBuffer.setLength(0); }

        // Copy / Paste
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.C)) copySelection();
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.V)) pasteClipboard();

        // Select all
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selection.clear();
            selection.addAll(placed);
        }

        // Deselect
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) selection.clear();

        // Delete selected
        if (selection.size > 0 &&
            (Gdx.input.isKeyJustPressed(Input.Keys.DEL) || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL))) {
            placed.removeAll(selection, true);
            selection.clear();
        }

        // WASD move selected objects
        if (selection.size > 0) {
            int newDx = 0, newDy = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) newDx =  1;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) newDx = -1;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) newDy =  1;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) newDy = -1;

            if (newDx != wasdDx || newDy != wasdDy) {
                // New direction — fire once immediately
                wasdDx = newDx; wasdDy = newDy; wasdTimer = 0f;
                if (wasdDx != 0 || wasdDy != 0) moveSelection(wasdDx, wasdDy);
            } else if (wasdDx != 0 || wasdDy != 0) {
                // Held — apply repeat delay then repeat rate
                wasdTimer += delta;
                if (wasdTimer >= WASD_INITIAL_DELAY) {
                    float timeSinceInitial = wasdTimer - WASD_INITIAL_DELAY;
                    int steps = (int)(timeSinceInitial / WASD_REPEAT_RATE);
                    if (steps > 0) {
                        for (int i = 0; i < steps; i++) moveSelection(wasdDx, wasdDy);
                        wasdTimer -= steps * WASD_REPEAT_RATE;
                    }
                }
            } else {
                wasdDx = 0; wasdDy = 0; wasdTimer = 0f;
            }
        } else {
            wasdDx = 0; wasdDy = 0; wasdTimer = 0f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) launchPlaytest();
    }

    // ── Selection helpers ─────────────────────────────────────────────────────

    private void moveSelection(int dx, int dy) {
        for (LevelData.ObjectEntry e : selection) {
            e.x += dx * GRID_SIZE;
            e.y += dy * GRID_SIZE;
        }
    }

    private void copySelection() {
        clipboard.clear();
        for (LevelData.ObjectEntry e : selection) clipboard.add(deepCopy(e));
    }

    private void pasteClipboard() {
        if (clipboard.size == 0) return;
        selection.clear();
        for (LevelData.ObjectEntry src : clipboard) {
            LevelData.ObjectEntry copy = deepCopy(src);
            copy.x += GRID_SIZE;
            copy.y += GRID_SIZE;
            placed.add(copy);
            selection.add(copy);
        }
    }

    private LevelData.ObjectEntry deepCopy(LevelData.ObjectEntry e) {
        LevelData.ObjectEntry c = new LevelData.ObjectEntry(e.type, e.x, e.y, e.size);
        c.blockType          = e.blockType;
        c.rotation           = e.rotation;
        c.triggerBgColor     = e.triggerBgColor;
        c.triggerGroundColor = e.triggerGroundColor;
        c.fadeDuration       = e.fadeDuration;

        // Pulse fields
        c.pulseBgColor       = e.pulseBgColor;
        c.pulseGroundColor   = e.pulseGroundColor;
        c.fadeInTime         = e.fadeInTime;
        c.holdTime           = e.holdTime;
        c.fadeOutTime        = e.fadeOutTime;

        return c;
    }

    /** Returns the topmost placed object containing world point (wx, wy), or null. */
    private LevelData.ObjectEntry objectAt(float wx, float wy) {
        for (int i = placed.size - 1; i >= 0; i--) {
            LevelData.ObjectEntry e = placed.get(i);
            if (wx >= e.x && wx <= e.x + e.size && wy >= e.y && wy <= e.y + e.size) return e;
        }
        return null;
    }

    /** Finish rubber-band: select all objects fully contained in the band rect. */
    private void finalizeRubberBand() {
        float x1 = Math.min(rubberStartX, rubberEndX);
        float y1 = Math.min(rubberStartY, rubberEndY);
        float x2 = Math.max(rubberStartX, rubberEndX);
        float y2 = Math.max(rubberStartY, rubberEndY);
        if (Math.abs(x2 - x1) < 5 || Math.abs(y2 - y1) < 5) return; // too small = not a drag
        selection.clear();
        for (LevelData.ObjectEntry e : placed) {
            if (e.x >= x1 && e.x + e.size <= x2 && e.y >= y1 && e.y + e.size <= y2)
                selection.add(e);
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void drawGrid() {
        float viewW  = Gdx.graphics.getWidth();
        float viewH  = Gdx.graphics.getHeight();
        float startX = snapX(cameraX) - GRID_SIZE;
        float endX   = cameraX + viewW + GRID_SIZE;
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(GRID_COLOR);
        for (float gx = startX; gx <= endX; gx += GRID_SIZE) shapes.line(gx, 0, gx, viewH);
        for (float gy = 0; gy <= viewH; gy += GRID_SIZE)     shapes.line(cameraX, gy, cameraX + viewW, gy);
        shapes.end();
    }

    private void drawGround() {
        float viewW = Gdx.graphics.getWidth();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(editorGroundColor);
        shapes.rect(cameraX, 0, viewW, GROUND_Y);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.4f, 0.5f, 0.8f, 1f));
        shapes.line(cameraX, GROUND_Y, cameraX + viewW, GROUND_Y);
        shapes.end();
    }

    private void drawSpawnLine() {
        float viewH = Gdx.graphics.getHeight();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(SPAWN_LINE_COLOR);
        shapes.line(100f, 0, 100f, viewH);
        shapes.end();
        batch.begin();
        font.setColor(SPAWN_LINE_COLOR);
        font.draw(batch, "SPAWN", 104f, viewH - 8);
        batch.end();
    }

    private void drawPlacedObjects() {
        objectRenderer.draw(placed);
    }

    private void drawSelectionHighlights() {
        if (selection.size == 0) return;

        // Translucent fill over selected objects
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(SELECTION_FILL);
        for (LevelData.ObjectEntry e : selection) shapes.rect(e.x, e.y, e.size, e.size);
        shapes.end();

        // Bright cyan outline
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(SELECTION_COLOR);
        for (LevelData.ObjectEntry e : selection) shapes.rect(e.x, e.y, e.size, e.size);
        shapes.end();

        // Label
        batch.begin();
        font.setColor(SELECTION_COLOR);
        if (selection.size == 1) {
            LevelData.ObjectEntry e = selection.first();
            String label = e.type + (e.blockType != null ? " [" + e.blockType + "]" : "");
            font.draw(batch, label, e.x, e.y + e.size + 18);
        } else {
            float minX = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (LevelData.ObjectEntry e : selection) {
                if (e.x < minX) minX = e.x;
                if (e.y + e.size > maxY) maxY = e.y + e.size;
            }
            font.draw(batch, selection.size + " selected", minX, maxY + 18);
        }
        batch.end();
    }

    private void drawRubberBand() {
        if (!rubberBanding) return;
        float x1 = Math.min(rubberStartX, rubberEndX);
        float y1 = Math.min(rubberStartY, rubberEndY);
        float w  = Math.abs(rubberEndX - rubberStartX);
        float h  = Math.abs(rubberEndY - rubberStartY);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(RUBBER_COLOR.r, RUBBER_COLOR.g, RUBBER_COLOR.b, 0.1f);
        shapes.rect(x1, y1, w, h);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(RUBBER_COLOR);
        shapes.rect(x1, y1, w, h);
        shapes.end();
    }

    private void drawColorTriggerLines() {
        float viewH = Gdx.graphics.getHeight();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (LevelData.ObjectEntry e : placed) {
            if (!"color_trigger".equals(e.type) && !"pulse_trigger".equals(e.type)) continue;
            shapes.setColor("pulse_trigger".equals(e.type) ? new Color(1f, 0.4f, 0.8f, 0.8f) : TRIGGER_LINE_COLOR);
            float cx = e.x + OBJECT_SIZE / 2f;
            shapes.line(cx, 0, cx, viewH);
        }
        shapes.end();
        batch.begin();
        for (LevelData.ObjectEntry e : placed) {
            if (!"color_trigger".equals(e.type) && !"pulse_trigger".equals(e.type)) continue;
            float cx = e.x + OBJECT_SIZE / 2f;
            font.setColor(e == editTarget ? new Color(1f, 1f, 0.4f, 1f) : 
                ("pulse_trigger".equals(e.type) ? new Color(1f, 0.4f, 0.8f, 1f) : TRIGGER_LINE_COLOR));
            
            StringBuilder label = new StringBuilder();
            if ("color_trigger".equals(e.type)) {
                label.append("COL");
                if (e.triggerBgColor     != null && !e.triggerBgColor.isEmpty())    label.append(" BG:#").append(e.triggerBgColor);
                if (e.triggerGroundColor != null && !e.triggerGroundColor.isEmpty()) label.append(" GND:#").append(e.triggerGroundColor);
                font.draw(batch, label.toString(), cx + 4, viewH - 28);
                font.draw(batch, e.fadeDuration + "s", cx + 4, viewH - 50);
            } else {
                label.append("PULSE");
                if (e.pulseBgColor     != null && !e.pulseBgColor.isEmpty())    label.append(" BG:#").append(e.pulseBgColor);
                if (e.pulseGroundColor != null && !e.pulseGroundColor.isEmpty()) label.append(" GND:#").append(e.pulseGroundColor);
                font.draw(batch, label.toString(), cx + 4, viewH - 28);
                font.draw(batch, e.fadeInTime + "/" + e.holdTime + "/" + e.fadeOutTime + "s", cx + 4, viewH - 50);
            }
        }
        batch.end();
    }

    private void drawMusicLine() {
        if (music == null) return;
        float worldX = 100f + music.getPosition() * SCROLL_SPEED;
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(MUSIC_LINE_COLOR);
        shapes.line(worldX, 0, worldX, Gdx.graphics.getHeight());
        shapes.end();
        batch.begin();
        font.setColor(MUSIC_LINE_COLOR);
        font.draw(batch, String.format("%.1fs", music.getPosition()), worldX + 4, GROUND_Y + 60);
        batch.end();
    }

    private void drawCursorPreview() {
        if (selection.size > 0) return; // hide preview when objects are selected
        float[] wp = cursorWorldPos();
        objectRenderer.drawCursorPreview(wp[0], wp[1], OBJECT_SIZE,
            PALETTE[paletteIndex].type, BlockType.values()[blockTypeIndex], currentSpikeRotation);
    }

    private void drawHUD() {
        float screenH = Gdx.graphics.getHeight();
        batch.begin();

        if (selection.size == 0) {
            font.setColor(Color.WHITE);
            String paletteLabel = "[ " + PALETTE[paletteIndex].name;
            if (PALETTE[paletteIndex].type.equals("block"))
                paletteLabel += " | " + BlockType.values()[blockTypeIndex].textureName;
            paletteLabel += " ]  (←/→ cycle,  TAB block type)";
            font.draw(batch, paletteLabel, cameraX + 10, screenH - 10);
        } else {
            font.setColor(SELECTION_COLOR);
            font.draw(batch,
                selection.size + " selected  |  WASD move  |  CTRL+C copy  |  CTRL+V paste  |  DEL delete  |  ESC deselect",
                cameraX + 10, screenH - 10);
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "Grid Snap: " + (snapToGrid ? "ON" : "OFF") + "  (G)", cameraX + 10, screenH - 34);

        String musicStatus = music == null ? "No music"
            : (music.isPlaying() ? "♪ Playing: " : "⏸ Paused: ") + levelMeta.musicFile;
        font.draw(batch, musicStatus, cameraX + 10, screenH - 58);

        if (selection.size == 0 && PALETTE[paletteIndex].type.equals("spike"))
            font.draw(batch, "Rotation: " + (int) currentSpikeRotation + "°  (Q/E)", cameraX + 10, screenH - 82);

        if (selection.size == 0 && PALETTE[paletteIndex].type.equals("color_trigger")) {
            font.setColor(TRIGGER_LINE_COLOR);
            font.draw(batch, "Left click = place  |  ALT+Left click on trigger = edit", cameraX + 10, screenH - 82);
        }

        font.setColor(new Color(1, 1, 1, 0.5f));
        font.draw(batch,
            "CTRL+S Save  CTRL+O Load  CTRL+A Select All  ENTER Playtest  ` Play/Pause  SHIFT+` Music  CTRL+` Stop",
            cameraX + 10, 28);
        batch.end();
    }

    private void drawMusicSelector() {
        float cx = cameraX + Gdx.graphics.getWidth() / 2f - 250;
        float cy = Gdx.graphics.getHeight() / 2f - 30;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, 0.85f);
        shapes.rect(cx, cy, 500, 60);
        shapes.end();
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Music file (assets/musics/): " + musicInputBuffer + "_", cx + 8, cy + 44);
        font.setColor(1, 1, 1, 0.5f);
        font.draw(batch, "ENTER to confirm, ESC to cancel", cx + 8, cy + 18);
        batch.end();
    }

    private void drawFilePrompt() {
        float cx = cameraX + Gdx.graphics.getWidth() / 2f - 250;
        float cy = Gdx.graphics.getHeight() / 2f - 30;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, 0.85f);
        shapes.rect(cx, cy, 500, 60);
        shapes.end();
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, (filePromptIsSave ? "Save to: " : "Load from: ") + fileBuffer + "_", cx + 8, cy + 44);
        font.setColor(1, 1, 1, 0.5f);
        font.draw(batch, "ENTER to confirm, ESC to cancel", cx + 8, cy + 18);
        batch.end();
    }

    private void drawEditMenu() {
        if (editTarget == null) return;
        boolean isPulse = "pulse_trigger".equals(editTarget.type);
        float cx = cameraX + Gdx.graphics.getWidth() / 2f - 280;
        float cy = Gdx.graphics.getHeight() / 2f - (isPulse ? 120 : 65);
        float pw = 560, ph = isPulse ? 250 : 140;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.07f, 0.07f, 0.11f, 0.97f));
        shapes.rect(cx, cy, pw, ph);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(TRIGGER_LINE_COLOR);
        shapes.rect(cx, cy, pw, ph);
        shapes.end();

        if (!isPulse) {
            drawSwatch(cx + 520, cy + ph - 38, editBgInput.toString());
            drawSwatch(cx + 520, cy + ph - 66, editGndInput.toString());

            String[] labels = {
                "BG color hex     (blank = no change): ",
                "Ground color hex (blank = no change): ",
                "Fade seconds:                         "
            };
            StringBuilder[] bufs = { editBgInput, editGndInput, editFadeInput };

            batch.begin();
            font.setColor(TRIGGER_LINE_COLOR);
            font.draw(batch, "Edit Color Trigger — TAB next field  |  ENTER confirm  |  ESC cancel", cx + 8, cy + ph - 8);
            for (int i = 0; i < 3; i++) {
                font.setColor(i == editField ? new Color(1f, 0.95f, 0.35f, 1f) : new Color(1f, 1f, 1f, 0.55f));
                font.draw(batch, labels[i] + bufs[i] + (i == editField ? "_" : ""), cx + 8, cy + ph - 32 - i * 30);
            }
            batch.end();
        } else {
            drawSwatch(cx + 520, cy + ph - 38, editBgInput.toString());
            drawSwatch(cx + 520, cy + ph - 66, editGndInput.toString());

            String[] labels = {
                "Pulse BG color hex     (blank = none): ",
                "Pulse Ground color hex (blank = none): ",
                "Fade In seconds:                       ",
                "Hold seconds:                          ",
                "Fade Out seconds:                      "
            };
            StringBuilder[] bufs = { editBgInput, editGndInput, editFadeInInput, editHoldInput, editFadeOutInput };

            batch.begin();
            font.setColor(TRIGGER_LINE_COLOR);
            font.draw(batch, "Edit Pulse Trigger — TAB next field  |  ENTER confirm  |  ESC cancel", cx + 8, cy + ph - 8);
            for (int i = 0; i < 5; i++) {
                font.setColor(i == editField ? new Color(1f, 0.95f, 0.35f, 1f) : new Color(1f, 1f, 1f, 0.55f));
                font.draw(batch, labels[i] + bufs[i] + (i == editField ? "_" : ""), cx + 8, cy + ph - 32 - i * 30);
            }
            batch.end();
        }
    }

    private void drawSwatch(float x, float y, String hex) {
        if (hex == null || hex.length() < 6) return;
        try {
            Color c = GameWorld.hexToColor(hex.replace("#", ""));
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(c); shapes.rect(x, y - 12, 14, 14); shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.WHITE); shapes.rect(x, y - 12, 14, 14); shapes.end();
        } catch (Exception ignored) {}
    }

    // ── Object placement / edit ───────────────────────────────────────────────

    private void placeObjectAtCursor() {
        float[] wp  = cursorWorldPos();
        String  type = PALETTE[paletteIndex].type;
        LevelData.ObjectEntry entry = new LevelData.ObjectEntry(type, wp[0], wp[1], OBJECT_SIZE);
        if (type.equals("block")) entry.blockType = BlockType.values()[blockTypeIndex].textureName;
        if (type.equals("spike")) entry.rotation  = currentSpikeRotation;
        placed.add(entry);
    }

    private void tryOpenEditMenu() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);
        for (int i = placed.size - 1; i >= 0; i--) {
            LevelData.ObjectEntry e = placed.get(i);
            if (!"color_trigger".equals(e.type) && !"pulse_trigger".equals(e.type)) continue;
            if (v.x >= e.x && v.x <= e.x + e.size && v.y >= e.y && v.y <= e.y + e.size) {
                editTarget = e; editField = 0;
                editBgInput.setLength(0); editGndInput.setLength(0);
                editFadeInput.setLength(0); editFadeInInput.setLength(0);
                editHoldInput.setLength(0); editFadeOutInput.setLength(0);

                if ("color_trigger".equals(e.type)) {
                    if (e.triggerBgColor     != null) editBgInput.append(e.triggerBgColor);
                    if (e.triggerGroundColor != null) editGndInput.append(e.triggerGroundColor);
                    editFadeInput.append(e.fadeDuration);
                } else if ("pulse_trigger".equals(e.type)) {
                    if (e.pulseBgColor     != null) editBgInput.append(e.pulseBgColor);
                    if (e.pulseGroundColor != null) editGndInput.append(e.pulseGroundColor);
                    editFadeInInput.append(e.fadeInTime);
                    editHoldInput.append(e.holdTime);
                    editFadeOutInput.append(e.fadeOutTime);
                }
                editMenuOpen = true;
                return;
            }
        }
    }

    private void confirmEdit() {
        if (editTarget == null) { editMenuOpen = false; return; }
        String bg  = editBgInput.toString().trim().replace("#", "");
        String gnd = editGndInput.toString().trim().replace("#", "");

        if ("color_trigger".equals(editTarget.type)) {
            editTarget.triggerBgColor     = bg.isEmpty()  ? null : bg;
            editTarget.triggerGroundColor = gnd.isEmpty() ? null : gnd;
            try { editTarget.fadeDuration = Float.parseFloat(editFadeInput.toString().trim()); }
            catch (NumberFormatException ignored) { editTarget.fadeDuration = 1f; }
        } else if ("pulse_trigger".equals(editTarget.type)) {
            editTarget.pulseBgColor     = bg.isEmpty()  ? null : bg;
            editTarget.pulseGroundColor = gnd.isEmpty() ? null : gnd;
            try { editTarget.fadeInTime = Float.parseFloat(editFadeInInput.toString().trim()); }
            catch (NumberFormatException ignored) { editTarget.fadeInTime = 0.1f; }
            try { editTarget.holdTime = Float.parseFloat(editHoldInput.toString().trim()); }
            catch (NumberFormatException ignored) { editTarget.holdTime = 0.2f; }
            try { editTarget.fadeOutTime = Float.parseFloat(editFadeOutInput.toString().trim()); }
            catch (NumberFormatException ignored) { editTarget.fadeOutTime = 0.5f; }
        }
        editMenuOpen = false; editTarget = null;
    }

    // ── Music ─────────────────────────────────────────────────────────────────

    private void loadMusic(String filename) {
        if (music != null) { music.stop(); music.dispose(); }
        resetEditorColors();
        try {
            FileHandle fh = Gdx.files.internal("musics/" + filename);
            if (!fh.exists()) fh = Gdx.files.local("assets/musics/" + filename);
            if (!fh.exists()) fh = Gdx.files.absolute(filename);
            if (!fh.exists()) { Gdx.app.error("Editor", "Music not found: " + filename); return; }
            music = Gdx.audio.newMusic(fh);
            music.setLooping(false);
            levelMeta.musicFile = filename;
        } catch (Exception ex) {
            Gdx.app.error("Editor", "Could not load music: " + ex.getMessage());
        }
    }

    private void toggleMusic() {
        if (music == null) return;
        if (music.isPlaying()) music.pause(); else music.play();
    }

    // ── Playtest / Save / Load ────────────────────────────────────────────────

    private void launchPlaytest() { app.launchPlaytest(buildLevelData()); }

    private LevelData buildLevelData() {
        LevelData data = new LevelData();
        data.name        = levelMeta.name;
        data.musicFile   = levelMeta.musicFile;
        data.bgColor     = levelMeta.bgColor;
        data.groundColor = levelMeta.groundColor;
        data.objects     = new Array<>(placed);
        return data;
    }

    private void saveLevel(String filename) {
        if (!filename.endsWith(".json")) filename += ".json";
        FileHandle fh = Gdx.files.local(filename);
        LevelSerializer.save(buildLevelData(), fh);
        Gdx.app.log("Editor", "Saved: " + fh.path());
    }

    private void loadLevel(String filename) {
        if (!filename.endsWith(".json")) filename += ".json";
        FileHandle fh = Gdx.files.local(filename);
        if (!fh.exists()) { Gdx.app.error("Editor", "Not found: " + fh.path()); return; }
        LevelData data = LevelSerializer.load(fh);
        placed.clear(); selection.clear();
        placed.addAll(data.objects);
        levelMeta = data;
        resetEditorColors();
        if (data.musicFile != null && !data.musicFile.isEmpty()) loadMusic(data.musicFile);
        Gdx.app.log("Editor", "Loaded: " + fh.path() + " (" + placed.size + " objects)");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float[] cursorWorldPos() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);
        return new float[]{ snapToGrid ? snapX(v.x) : v.x, snapToGrid ? snapY(v.y) : v.y };
    }

    private float[] screenToWorld(int sx, int sy) {
        Vector3 v = new Vector3(sx, sy, 0);
        camera.unproject(v);
        return new float[]{ v.x, v.y };
    }

    private float snapX(float x) { return (float)(Math.floor(x / GRID_SIZE) * GRID_SIZE); }
    private float snapY(float y) { return (float)(Math.floor(y / GRID_SIZE) * GRID_SIZE); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private void deleteAtCursor() {
        if (selection.size > 0) { placed.removeAll(selection, true); selection.clear(); return; }
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);
        for (int i = placed.size - 1; i >= 0; i--) {
            LevelData.ObjectEntry e = placed.get(i);
            if (v.x >= e.x && v.x <= e.x + e.size && v.y >= e.y && v.y <= e.y + e.size) {
                placed.removeIndex(i); return;
            }
        }
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    @Override public void resize(int w, int h) { camera.viewportWidth = w; camera.viewportHeight = h; camera.update(); }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        batch.dispose(); shapes.dispose(); font.dispose();
        if (music != null) music.dispose();
        objectRenderer.dispose();
    }

    // ── Input adapter ─────────────────────────────────────────────────────────

    private class EditorInputAdapter extends InputAdapter {

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.RIGHT) {
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
                    deleteAtCursor(); return true;
                }
                panning = true; lastMouseX = screenX; return true;
            }

            if (button == Input.Buttons.LEFT && !musicSelectorOpen && !filePromptOpen && !editMenuOpen) {
                boolean alt   = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)   || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
                boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

                if (alt) { tryOpenEditMenu(); return true; }

                float[] wp  = screenToWorld(screenX, screenY);
                LevelData.ObjectEntry hit = objectAt(wp[0], wp[1]);

                if (hit != null) {
                    // Clicked on an existing object
                    if (shift) {
                        if (selection.contains(hit, true)) selection.removeValue(hit, true);
                        else                         selection.add(hit);
                    } else {
                        if (!selection.contains(hit, true)) { selection.clear(); selection.add(hit); }
                        // Already in selection: allow WASD to move without deselecting
                    }
                } else {
                    // Clicked on empty space — start rubber-band
                    if (!shift) selection.clear();
                    rubberBanding = true;
                    rubberStartX = wp[0]; rubberStartY = wp[1];
                    rubberEndX   = wp[0]; rubberEndY   = wp[1];
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (panning) { cameraX -= (screenX - lastMouseX); lastMouseX = screenX; return true; }
            if (rubberBanding) {
                float[] wp = screenToWorld(screenX, screenY);
                rubberEndX = wp[0]; rubberEndY = wp[1]; return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.RIGHT) { panning = false; return true; }
            if (button == Input.Buttons.LEFT && rubberBanding) {
                float[] wp = screenToWorld(screenX, screenY);
                rubberEndX = wp[0]; rubberEndY = wp[1];
                finalizeRubberBand();
                rubberBanding = false;
                // If band was tiny (just a tap on empty space) and produced no selection → place object
                if (selection.size == 0) placeObjectAtCursor();
                return true;
            }
            return false;
        }

        @Override
        public boolean keyTyped(char c) {
            if (musicSelectorOpen) {
                if      (c == '\r' || c == '\n') { String f = musicInputBuffer.toString().trim(); if (!f.isEmpty()) loadMusic(f); musicSelectorOpen = false; }
                else if (c == 27)                { musicSelectorOpen = false; }
                else if (c == '\b')              { if (musicInputBuffer.length() > 0) musicInputBuffer.deleteCharAt(musicInputBuffer.length()-1); }
                else                             { musicInputBuffer.append(c); }
                return true;
            }
            if (filePromptOpen) {
                if      (c == '\r' || c == '\n') { String f = fileBuffer.toString().trim(); if (!f.isEmpty()) { if (filePromptIsSave) saveLevel(f); else loadLevel(f); } filePromptOpen = false; }
                else if (c == 27)                { filePromptOpen = false; }
                else if (c == '\b')              { if (fileBuffer.length() > 0) fileBuffer.deleteCharAt(fileBuffer.length()-1); }
                else                             { fileBuffer.append(c); }
                return true;
            }
            if (editMenuOpen) {
                if      (c == '\r' || c == '\n') { confirmEdit(); return true; }
                else if (c == 27)                { editMenuOpen = false; editTarget = null; return true; }
                else if (c == '\t')              { 
                    int maxFields = "pulse_trigger".equals(editTarget.type) ? 5 : 3;
                    editField = (editField + 1) % maxFields; 
                    return true; 
                }
                
                StringBuilder active;
                if ("pulse_trigger".equals(editTarget.type)) {
                    StringBuilder[] bufs = { editBgInput, editGndInput, editFadeInInput, editHoldInput, editFadeOutInput };
                    active = bufs[editField];
                } else {
                    StringBuilder[] bufs = { editBgInput, editGndInput, editFadeInput };
                    active = bufs[editField];
                }

                if (c == '\b') { if (active.length() > 0) active.deleteCharAt(active.length()-1); }
                else if (Character.isDefined(c) && !Character.isISOControl(c)) { active.append(c); }
                return true;
            }
            return false;
        }
    }

    // ── Palette entry ─────────────────────────────────────────────────────────

    private static class PaletteEntry {
        final String name; final String type; final Color color;
        PaletteEntry(String n, String t, Color c) { name = n; type = t; color = c; }
    }
}
