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

import java.util.ArrayList;
import java.util.List;

/**
 * PC-only level editor screen.
 *
 * Controls:
 *   LEFT / RIGHT arrow       — cycle through object palette
 *   TAB                      — cycle block type
 *   Q / E                    — rotate spike
 *   G                        — toggle grid snap
 *   Left click               — place object at cursor
 *   ALT + Left click         — edit color trigger under cursor
 *   CTRL + Right click       — delete object under cursor
 *   Right click + drag       — pan camera
 *   SHIFT + `                — open music selector
 *   `                        — play / pause music
 *   CTRL + `                 — stop music (resets position + editor colors)
 *   CTRL + S                 — save level
 *   CTRL + O                 — load level
 *   ENTER                    — launch playtest
 */
public class LevelEditorScreen implements Screen {

    private EditorObjectRenderer objectRenderer;
    private int   blockTypeIndex       = 0;
    private float currentSpikeRotation = 0f;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float GROUND_Y           = 50f;
    private static final float SCROLL_SPEED       = 300f;
    private static final float GRID_SIZE          = 50f;
    private static final float OBJECT_SIZE        = 50f;
    private static final Color SPAWN_LINE_COLOR   = new Color(1f,  0.2f,  0.2f,  1f);
    private static final Color MUSIC_LINE_COLOR   = new Color(0.2f, 1f,   0.4f,  0.85f);
    private static final Color TRIGGER_LINE_COLOR = new Color(1f,  0.85f, 0.2f,  0.8f);
    private static final Color GRID_COLOR         = new Color(1f,  1f,    1f,    0.08f);

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final PaletteEntry[] PALETTE = {
        new PaletteEntry("Block",         "block",         new Color(0.55f, 0.55f, 0.85f, 1f)),
        new PaletteEntry("Spike",         "spike",         new Color(0.85f, 0.25f, 0.25f, 1f)),
        new PaletteEntry("Cube Portal",   "cube_portal",   new Color(0.25f, 0.85f, 0.55f, 1f)),
        new PaletteEntry("Ship Portal",   "ship_portal",   new Color(0.25f, 0.55f, 0.85f, 1f)),
        new PaletteEntry("Color Trigger", "color_trigger", new Color(1f,    0.85f, 0.2f,  1f)),
    };

    // ── Editor state ──────────────────────────────────────────────────────────
    private final LevelEditorApp app;
    private int     paletteIndex = 0;
    private boolean snapToGrid   = true;

    private final List<LevelData.ObjectEntry> placed = new ArrayList<>();
    private LevelData levelMeta = new LevelData();

    // ── Camera / pan ──────────────────────────────────────────────────────────
    private OrthographicCamera camera;
    private float   cameraX    = 0f;
    private boolean panning    = false;
    private float   lastMouseX;

    // ── Music ─────────────────────────────────────────────────────────────────
    private Music         music;
    private boolean       musicSelectorOpen = false;
    private StringBuilder musicInputBuffer  = new StringBuilder();

    // ── Editor preview colors (lerped as music line passes triggers) ──────────
    private Color editorBgColor     = new Color(0.1f,  0.1f,  0.18f, 1f);
    private Color editorGroundColor = new Color(0.09f, 0.13f, 0.24f, 1f);
    private EditorColorFade bgFade     = null;
    private EditorColorFade groundFade = null;
    private final List<LevelData.ObjectEntry> firedTriggers = new ArrayList<>();

    private static class EditorColorFade {
        final Color from; final Color to; final float duration; float elapsed = 0f;
        EditorColorFade(Color from, Color to, float duration) {
            this.from = new Color(from); this.to = new Color(to); this.duration = duration;
        }
    }

    // ── Color trigger edit menu ───────────────────────────────────────────────
    // Opened via ALT + left click on an existing color trigger.
    // The entry is edited in-place — no separate "pending" object needed.
    private boolean              editMenuOpen   = false;
    private LevelData.ObjectEntry editTarget    = null; // the trigger being edited
    /** 0 = bgColor, 1 = groundColor, 2 = fadeDuration */
    private int           editField      = 0;
    private StringBuilder editBgInput    = new StringBuilder();
    private StringBuilder editGndInput   = new StringBuilder();
    private StringBuilder editFadeInput  = new StringBuilder();

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

        handleKeyboard();

        camera.position.x = cameraX + Gdx.graphics.getWidth() / 2f;
        camera.update();

        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawGrid();
        drawGround();
        drawSpawnLine();
        drawPlacedObjects();
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
            if (!"color_trigger".equals(e.type)) continue;
            if (firedTriggers.contains(e))       continue;
            float triggerCX = e.x + OBJECT_SIZE / 2f;
            if (musicX >= triggerCX) {
                firedTriggers.add(e);
                if (e.triggerBgColor != null && !e.triggerBgColor.isEmpty())
                    bgFade = new EditorColorFade(editorBgColor,
                        GameWorld.hexToColor(e.triggerBgColor), e.fadeDuration);
                if (e.triggerGroundColor != null && !e.triggerGroundColor.isEmpty())
                    groundFade = new EditorColorFade(editorGroundColor,
                        GameWorld.hexToColor(e.triggerGroundColor), e.fadeDuration);
            }
        }

        if (bgFade != null) {
            bgFade.elapsed += delta;
            float t = Math.min(bgFade.elapsed / bgFade.duration, 1f);
            editorBgColor.set(lerp(bgFade.from.r, bgFade.to.r, t),
                lerp(bgFade.from.g, bgFade.to.g, t),
                lerp(bgFade.from.b, bgFade.to.b, t), 1f);
            if (t >= 1f) bgFade = null;
        }
        if (groundFade != null) {
            groundFade.elapsed += delta;
            float t = Math.min(groundFade.elapsed / groundFade.duration, 1f);
            editorGroundColor.set(lerp(groundFade.from.r, groundFade.to.r, t),
                lerp(groundFade.from.g, groundFade.to.g, t),
                lerp(groundFade.from.b, groundFade.to.b, t), 1f);
            if (t >= 1f) groundFade = null;
        }
    }

    private void resetEditorColors() {
        String bg  = (levelMeta.bgColor     != null && !levelMeta.bgColor.isEmpty())     ? levelMeta.bgColor     : "1a1a2e";
        String gnd = (levelMeta.groundColor != null && !levelMeta.groundColor.isEmpty()) ? levelMeta.groundColor : "16213e";
        editorBgColor.set(GameWorld.hexToColor(bg));
        editorGroundColor.set(GameWorld.hexToColor(gnd));
        bgFade = null; groundFade = null;
        firedTriggers.clear();
    }

    // ── Keyboard handling ─────────────────────────────────────────────────────

    private void handleKeyboard() {
        if (musicSelectorOpen || filePromptOpen || editMenuOpen) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB))
            blockTypeIndex = (blockTypeIndex + 1) % BlockType.values().length;

        if (PALETTE[paletteIndex].type.equals("spike")) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q))
                currentSpikeRotation = (currentSpikeRotation + 90f) % 360f;
            if (Gdx.input.isKeyJustPressed(Input.Keys.E))
                currentSpikeRotation = (currentSpikeRotation - 90f + 360f) % 360f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))
            paletteIndex = (paletteIndex + 1) % PALETTE.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))
            paletteIndex = (paletteIndex - 1 + PALETTE.length) % PALETTE.length;

        if (Gdx.input.isKeyJustPressed(Input.Keys.G))
            snapToGrid = !snapToGrid;

        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            boolean ctrl  = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)  || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
            boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            if (shift)      { musicSelectorOpen = true; musicInputBuffer.setLength(0); }
            else if (ctrl)  { if (music != null) { music.stop(); resetEditorColors(); } }
            else            { toggleMusic(); }
        }

        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.S)) { filePromptOpen = true; filePromptIsSave = true;  fileBuffer.setLength(0); }
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.O)) { filePromptOpen = true; filePromptIsSave = false; fileBuffer.setLength(0); }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) launchPlaytest();
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void drawGrid() {
        float viewW  = Gdx.graphics.getWidth();
        float viewH  = Gdx.graphics.getHeight();
        float startX = snapX(cameraX) - GRID_SIZE;
        float endX   = cameraX + viewW + GRID_SIZE;
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(GRID_COLOR);
        for (float gx = startX; gx <= endX; gx += GRID_SIZE)
            shapes.line(gx, 0, gx, viewH);
        for (float gy = 0; gy <= viewH; gy += GRID_SIZE)
            shapes.line(cameraX, gy, cameraX + viewW, gy);
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

    private void drawColorTriggerLines() {
        float viewH = Gdx.graphics.getHeight();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(TRIGGER_LINE_COLOR);
        for (LevelData.ObjectEntry e : placed) {
            if (!"color_trigger".equals(e.type)) continue;
            float cx = e.x + OBJECT_SIZE / 2f;
            shapes.line(cx, 0, cx, viewH);
        }
        shapes.end();

        batch.begin();
        for (LevelData.ObjectEntry e : placed) {
            if (!"color_trigger".equals(e.type)) continue;
            float cx = e.x + OBJECT_SIZE / 2f;

            // highlight the trigger being edited
            font.setColor(e == editTarget
                ? new Color(1f, 1f, 0.4f, 1f)
                : TRIGGER_LINE_COLOR);

            StringBuilder label = new StringBuilder("COL");
            if (e.triggerBgColor     != null && !e.triggerBgColor.isEmpty())     label.append(" BG:#").append(e.triggerBgColor);
            if (e.triggerGroundColor != null && !e.triggerGroundColor.isEmpty())  label.append(" GND:#").append(e.triggerGroundColor);
            font.draw(batch, label.toString(), cx + 4, viewH - 28);
            font.draw(batch, e.fadeDuration + "s", cx + 4, viewH - 50);
        }
        batch.end();
    }

    private void drawMusicLine() {
        if (music == null) return;
        float elapsed = music.getPosition();
        float worldX  = 100f + elapsed * SCROLL_SPEED;
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(MUSIC_LINE_COLOR);
        shapes.line(worldX, 0, worldX, Gdx.graphics.getHeight());
        shapes.end();
        batch.begin();
        font.setColor(MUSIC_LINE_COLOR);
        font.draw(batch, String.format("%.1fs", elapsed), worldX + 4, GROUND_Y + 60);
        batch.end();
    }

    private void drawCursorPreview() {
        float[] wp = cursorWorldPos();
        objectRenderer.drawCursorPreview(wp[0], wp[1], OBJECT_SIZE,
            PALETTE[paletteIndex].type, BlockType.values()[blockTypeIndex], currentSpikeRotation);
    }

    private void drawHUD() {
        float screenH = Gdx.graphics.getHeight();
        batch.begin();
        font.setColor(Color.WHITE);

        String paletteLabel = "[ " + PALETTE[paletteIndex].name;
        if (PALETTE[paletteIndex].type.equals("block"))
            paletteLabel += " | " + BlockType.values()[blockTypeIndex].textureName;
        paletteLabel += " ]  (←/→ cycle,  TAB block type)";
        font.draw(batch, paletteLabel, cameraX + 10, screenH - 10);
        font.draw(batch, "Grid Snap: " + (snapToGrid ? "ON" : "OFF") + "  (G)", cameraX + 10, screenH - 34);

        String musicStatus = music == null ? "No music"
            : (music.isPlaying() ? "♪ Playing: " : "⏸ Paused: ") + levelMeta.musicFile;
        font.draw(batch, musicStatus, cameraX + 10, screenH - 58);

        if (PALETTE[paletteIndex].type.equals("spike"))
            font.draw(batch, "Rotation: " + (int) currentSpikeRotation + "°  (Q/E)", cameraX + 10, screenH - 82);

        if (PALETTE[paletteIndex].type.equals("color_trigger")) {
            font.setColor(TRIGGER_LINE_COLOR);
            font.draw(batch, "Left click = place  |  ALT + Left click on trigger = edit", cameraX + 10, screenH - 82);
        }

        font.setColor(new Color(1, 1, 1, 0.5f));
        font.draw(batch, "CTRL+S Save  CTRL+O Load  ENTER Playtest  ` Play/Pause  SHIFT+` Music  CTRL+` Stop",
            cameraX + 10, 28);
        batch.end();
    }

    private void drawMusicSelector() {
        float cx = cameraX + Gdx.graphics.getWidth() / 2f - 250;
        float cy = Gdx.graphics.getHeight() / 2f - 30;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0, 0, 0, 0.85f));
        shapes.rect(cx, cy, 500, 60);
        shapes.end();
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Music file (assets/musics/): " + musicInputBuffer + "_", cx + 8, cy + 44);
        font.setColor(new Color(1, 1, 1, 0.5f));
        font.draw(batch, "ENTER to confirm, ESC to cancel", cx + 8, cy + 18);
        batch.end();
    }

    private void drawFilePrompt() {
        float cx = cameraX + Gdx.graphics.getWidth() / 2f - 250;
        float cy = Gdx.graphics.getHeight() / 2f - 30;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0, 0, 0, 0.85f));
        shapes.rect(cx, cy, 500, 60);
        shapes.end();
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, (filePromptIsSave ? "Save to: " : "Load from: ") + fileBuffer + "_", cx + 8, cy + 44);
        font.setColor(new Color(1, 1, 1, 0.5f));
        font.draw(batch, "ENTER to confirm, ESC to cancel", cx + 8, cy + 18);
        batch.end();
    }

    /** Edit menu — opened by ALT+click on an existing color trigger. */
    private void drawEditMenu() {
        float cx = cameraX + Gdx.graphics.getWidth() / 2f - 280;
        float cy = Gdx.graphics.getHeight() / 2f - 65;
        float pw = 560, ph = 140;

        // background panel
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.07f, 0.07f, 0.11f, 0.97f));
        shapes.rect(cx, cy, pw, ph);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(TRIGGER_LINE_COLOR);
        shapes.rect(cx, cy, pw, ph);
        shapes.end();

        // small color swatches next to BG / Ground labels if hex is valid
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
            font.setColor(i == editField
                ? new Color(1f, 0.95f, 0.35f, 1f)
                : new Color(1f, 1f,    1f,    0.55f));
            font.draw(batch, labels[i] + bufs[i] + (i == editField ? "_" : ""), cx + 8, cy + ph - 32 - i * 30);
        }
        batch.end();
    }

    /** Draws a 14x14 filled color swatch if the hex string is valid. */
    private void drawSwatch(float x, float y, String hex) {
        if (hex == null || hex.length() < 6) return;
        try {
            Color c = GameWorld.hexToColor(hex.replace("#", ""));
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(c);
            shapes.rect(x, y - 12, 14, 14);
            shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.WHITE);
            shapes.rect(x, y - 12, 14, 14);
            shapes.end();
        } catch (Exception ignored) {}
    }

    // ── Object placement / edit ───────────────────────────────────────────────

    private void placeObjectAtCursor() {
        float[] wp  = cursorWorldPos();
        String  type = PALETTE[paletteIndex].type;

        LevelData.ObjectEntry entry = new LevelData.ObjectEntry(type, wp[0], wp[1], OBJECT_SIZE);
        if (type.equals("block"))
            entry.blockType = BlockType.values()[blockTypeIndex].textureName;
        if (type.equals("spike"))
            entry.rotation = currentSpikeRotation;
        // color triggers placed with empty values — edit via ALT+click
        placed.add(entry);
    }

    /** Opens the edit menu for the color trigger under the cursor, if any. */
    private void tryOpenEditMenu() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);
        for (int i = placed.size() - 1; i >= 0; i--) {
            LevelData.ObjectEntry e = placed.get(i);
            if (!"color_trigger".equals(e.type)) continue;
            if (v.x >= e.x && v.x <= e.x + e.size && v.y >= e.y && v.y <= e.y + e.size) {
                editTarget = e;
                editField  = 0;
                editBgInput.setLength(0);
                editGndInput.setLength(0);
                editFadeInput.setLength(0);
                // pre-fill current values
                if (e.triggerBgColor     != null) editBgInput.append(e.triggerBgColor);
                if (e.triggerGroundColor != null) editGndInput.append(e.triggerGroundColor);
                editFadeInput.append(e.fadeDuration);
                editMenuOpen = true;
                return;
            }
        }
    }

    private void confirmEdit() {
        if (editTarget == null) { editMenuOpen = false; return; }

        String bg  = editBgInput.toString().trim().replace("#", "");
        String gnd = editGndInput.toString().trim().replace("#", "");
        editTarget.triggerBgColor     = bg.isEmpty()  ? null : bg;
        editTarget.triggerGroundColor = gnd.isEmpty() ? null : gnd;
        try {
            editTarget.fadeDuration = Float.parseFloat(editFadeInput.toString().trim());
        } catch (NumberFormatException ignored) {
            editTarget.fadeDuration = 1f;
        }

        editMenuOpen = false;
        editTarget   = null;
    }

    // ── Music ─────────────────────────────────────────────────────────────────

    private void loadMusic(String filename) {
        if (music != null) { music.stop(); music.dispose(); }
        resetEditorColors();
        try {
            FileHandle fh = Gdx.files.internal("musics/" + filename);
            if (!fh.exists()) fh = Gdx.files.local("assets/musics/" + filename);
            if (!fh.exists()) fh = Gdx.files.absolute(filename);
            if (!fh.exists()) { System.err.println("[Editor] Music not found: " + filename); return; }
            music = Gdx.audio.newMusic(fh);
            music.setLooping(false);
            levelMeta.musicFile = filename;
        } catch (Exception ex) {
            System.err.println("[Editor] Could not load music: " + ex.getMessage());
        }
    }

    private void toggleMusic() {
        if (music == null) return;
        if (music.isPlaying()) music.pause();
        else                   music.play();
    }

    // ── Playtest / Save / Load ────────────────────────────────────────────────

    private void launchPlaytest() { app.launchPlaytest(buildLevelData()); }

    private LevelData buildLevelData() {
        LevelData data   = new LevelData();
        data.name        = levelMeta.name;
        data.musicFile   = levelMeta.musicFile;
        data.bgColor     = levelMeta.bgColor;
        data.groundColor = levelMeta.groundColor;
        data.objects     = new ArrayList<>(placed);
        return data;
    }

    private void saveLevel(String filename) {
        if (!filename.endsWith(".json")) filename += ".json";
        FileHandle fh = Gdx.files.local(filename);
        LevelSerializer.save(buildLevelData(), fh);
        System.out.println("[Editor] Saved: " + fh.path());
    }

    private void loadLevel(String filename) {
        if (!filename.endsWith(".json")) filename += ".json";
        FileHandle fh = Gdx.files.local(filename);
        if (!fh.exists()) { System.err.println("[Editor] Not found: " + fh.path()); return; }
        LevelData data = LevelSerializer.load(fh);
        placed.clear();
        placed.addAll(data.objects);
        levelMeta = data;
        resetEditorColors();
        if (data.musicFile != null && !data.musicFile.isEmpty()) loadMusic(data.musicFile);
        System.out.println("[Editor] Loaded: " + fh.path() + " (" + placed.size() + " objects)");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float[] cursorWorldPos() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);
        float wx = snapToGrid ? snapX(v.x) : v.x;
        float wy = snapToGrid ? snapY(v.y) : v.y;
        return new float[]{ wx, wy };
    }

    private float snapX(float x) { return (float)(Math.floor(x / GRID_SIZE) * GRID_SIZE); }
    private float snapY(float y) { return (float)(Math.floor(y / GRID_SIZE) * GRID_SIZE); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private void deleteObjectAtCursor() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);
        for (int i = placed.size() - 1; i >= 0; i--) {
            LevelData.ObjectEntry e = placed.get(i);
            if (v.x >= e.x && v.x <= e.x + e.size && v.y >= e.y && v.y <= e.y + e.size) {
                placed.remove(i); return;
            }
        }
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    @Override public void resize(int w, int h) {
        camera.viewportWidth = w; camera.viewportHeight = h; camera.update();
    }
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
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
                    deleteObjectAtCursor(); return true;
                }
                panning = true; lastMouseX = screenX; return true;
            }

            if (button == Input.Buttons.LEFT && !musicSelectorOpen && !filePromptOpen && !editMenuOpen) {
                boolean alt = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
                if (alt) {
                    tryOpenEditMenu(); // ALT+click = edit existing trigger
                } else {
                    placeObjectAtCursor(); // normal click = place
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (panning) { cameraX -= (screenX - lastMouseX); lastMouseX = screenX; return true; }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.RIGHT) { panning = false; return true; }
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
                else if (c == '\t')              { editField = (editField + 1) % 3; return true; }

                StringBuilder active = editField == 0 ? editBgInput
                    : editField == 1 ? editGndInput : editFadeInput;
                if (c == '\b') { if (active.length() > 0) active.deleteCharAt(active.length()-1); }
                else           { active.append(c); }
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
