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
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * PC-only level editor screen.
 *
 * Controls:
 *   LEFT / RIGHT arrow    — cycle through object palette
 *   G                     — toggle grid snap
 *   Left click            — place object at cursor
 *   Right click + drag    — pan camera
 *   SHIFT + `             — open music selector (type filename, Enter to confirm)
 *   `  (backtick)         — play / pause music
 *   CTRL + S              — save level JSON (asks for filename via console; swap for file dialog if desired)
 *   CTRL + O              — load level JSON (asks for filename via console)
 *   ENTER                 — launch playtest window
 */
public class LevelEditorScreen implements Screen {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float GROUND_Y       = 50f;
    private static final float SCROLL_SPEED   = 200f; // must match GameWorld
    private static final float GRID_SIZE      = 50f;  // one player-width cell
    private static final float OBJECT_SIZE    = 50f;
    private static final Color SPAWN_LINE_COLOR  = new Color(1f, 0.2f, 0.2f, 1f);
    private static final Color MUSIC_LINE_COLOR  = new Color(0.2f, 1f, 0.4f, 0.85f);
    private static final Color GROUND_COLOR      = new Color(0.09f, 0.13f, 0.24f, 1f);
    private static final Color BG_COLOR          = new Color(0.1f, 0.1f, 0.18f, 1f);
    private static final Color GRID_COLOR        = new Color(1f, 1f, 1f, 0.08f);

    // ── Palette definition ────────────────────────────────────────────────────
    /**
     * Add new types here — everything else adapts automatically.
     * Each entry: display name, LevelData type string, color for preview rect.
     */
    private static final PaletteEntry[] PALETTE = {
        new PaletteEntry("Block",       "block",       new Color(0.55f, 0.55f, 0.85f, 1f)),
        new PaletteEntry("Spike",       "spike",       new Color(0.85f, 0.25f, 0.25f, 1f)),
        new PaletteEntry("Cube Portal", "cube_portal", new Color(0.25f, 0.85f, 0.55f, 1f)),
        new PaletteEntry("Ship Portal", "ship_portal", new Color(0.25f, 0.55f, 0.85f, 1f)),
    };

    // ── Editor state ──────────────────────────────────────────────────────────
    private final LevelEditorApp app;
    private int            paletteIndex  = 0;
    private boolean        snapToGrid    = true;

    private final List<LevelData.ObjectEntry> placed = new ArrayList<>();
    private LevelData levelMeta = new LevelData(); // stores colors, song, name

    // ── Camera / pan ──────────────────────────────────────────────────────────
    private OrthographicCamera camera;
    private float  cameraX       = 0f; // world-space left edge of view
    private boolean panning      = false;
    private float   lastMouseX;

    // ── Music ─────────────────────────────────────────────────────────────────
    private Music  music;
    private boolean musicSelectorOpen = false;
    private StringBuilder musicInputBuffer = new StringBuilder();

    // ── Rendering ─────────────────────────────────────────────────────────────
    private SpriteBatch   batch;
    private ShapeRenderer shapes;
    private BitmapFont    font;

    // ── Save/Load filename prompt (simple in-editor text input) ───────────────
    private boolean filePromptOpen    = false;
    private boolean filePromptIsSave  = false;
    private StringBuilder fileBuffer  = new StringBuilder();

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

        Gdx.input.setInputProcessor(new EditorInputAdapter());
    }

    // ── Main loop ─────────────────────────────────────────────────────────────

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleKeyboard();

        camera.position.x = cameraX + Gdx.graphics.getWidth() / 2f;
        camera.update();

        // set both projection matrices ONCE per frame here
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawGrid();
        drawGround();       // now draws filled ground + line
        drawSpawnLine();
        drawPlacedObjects();
        drawMusicLine();
        drawCursorPreview();
        drawHUD();

        if (musicSelectorOpen) drawMusicSelector();
        if (filePromptOpen)    drawFilePrompt();
    }

    // ── Keyboard handling ─────────────────────────────────────────────────────

    private void handleKeyboard() {
        // text-input modes consume all other keys
        if (musicSelectorOpen || filePromptOpen) return;

        // palette cycling
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            paletteIndex = (paletteIndex + 1) % PALETTE.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            paletteIndex = (paletteIndex - 1 + PALETTE.length) % PALETTE.length;
        }

        // grid snap toggle
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            snapToGrid = !snapToGrid;
        }

        // music selector  SHIFT + `
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) &&
            Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            musicSelectorOpen = true;
            musicInputBuffer.setLength(0);
            return;
        }

        // play / pause music  `
        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            toggleMusic();
        }

        // save  CTRL + S
        if ((Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
            Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) &&
            Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            filePromptOpen   = true;
            filePromptIsSave = true;
            fileBuffer.setLength(0);
        }

        // load  CTRL + O
        if ((Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
            Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) &&
            Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            filePromptOpen   = true;
            filePromptIsSave = false;
            fileBuffer.setLength(0);
        }

        // playtest  ENTER
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            launchPlaytest();
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void drawGrid() {
        float viewW = Gdx.graphics.getWidth();
        float viewH = Gdx.graphics.getHeight();
        float startX = snapX(cameraX) - GRID_SIZE;
        float endX   = cameraX + viewW + GRID_SIZE;

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(GRID_COLOR);

        // vertical lines
        for (float gx = startX; gx <= endX; gx += GRID_SIZE) {
            shapes.line(gx, 0, gx, viewH);
        }
        // horizontal lines
        for (float gy = 0; gy <= viewH; gy += GRID_SIZE) {
            shapes.line(cameraX, gy, cameraX + viewW, gy);
        }
        shapes.end();
    }

    private void drawGround() {
        float viewW = Gdx.graphics.getWidth();
        // filled ground strip
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(GROUND_COLOR);
        shapes.rect(cameraX, 0, viewW, GROUND_Y);
        shapes.end();

        // ground line on top
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.4f, 0.5f, 0.8f, 1f));
        shapes.line(cameraX, GROUND_Y, cameraX + viewW, GROUND_Y);
        shapes.end();
    }

    private void drawSpawnLine() {
        float spawnWorldX = 100f;
        float viewH = Gdx.graphics.getHeight();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(SPAWN_LINE_COLOR);
        shapes.line(spawnWorldX, 0, spawnWorldX, viewH);
        shapes.end();

        // set projection before any font draw
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(SPAWN_LINE_COLOR);
        font.draw(batch, "SPAWN", spawnWorldX + 4, viewH - 8);
        batch.end();
    }

    private void drawPlacedObjects() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (LevelData.ObjectEntry e : placed) {
            Color c = colorFor(e.type);
            shapes.setColor(c);
            shapes.rect(e.x, e.y, e.size, e.size);
        }
        shapes.end();

        // labels
        batch.begin();
        font.getData().setScale(0.8f);
        for (LevelData.ObjectEntry e : placed) {
            font.setColor(Color.WHITE);
            font.draw(batch, labelFor(e.type), e.x + 2, e.y + e.size - 2);
        }
        font.getData().setScale(1.3f);
        batch.end();
    }

    private void drawMusicLine() {
        if (music == null) return;
        float elapsed  = music.getPosition();
        float worldX   = 100f + elapsed * SCROLL_SPEED; // spawn + distance scrolled at that time

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
        float wx = wp[0], wy = wp[1];

        Color c = new Color(colorFor(PALETTE[paletteIndex].type));
        c.a = 0.5f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(c);
        shapes.rect(wx, wy, OBJECT_SIZE, OBJECT_SIZE);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.WHITE);
        shapes.rect(wx, wy, OBJECT_SIZE, OBJECT_SIZE);
        shapes.end();
    }

    private void drawHUD() {
        float screenH = Gdx.graphics.getHeight();

        batch.begin();
        font.setColor(Color.WHITE);

        // current palette item
        font.draw(batch, "[ " + PALETTE[paletteIndex].name + " ]  (←/→ to cycle)",
            cameraX + 10, screenH - 10);

        // snap status
        font.draw(batch, "Grid Snap: " + (snapToGrid ? "ON" : "OFF") + "  (G)",
            cameraX + 10, screenH - 34);

        // music status
        String musicStatus = music == null ? "No music"
            : (music.isPlaying() ? "♪ Playing: " : "⏸ Paused: ") + levelMeta.musicFile;
        font.draw(batch, musicStatus, cameraX + 10, screenH - 58);

        // controls reminder
        font.setColor(new Color(1,1,1,0.5f));
        font.draw(batch, "CTRL+S Save  CTRL+O Load  ENTER Playtest  ` Play/Pause  SHIFT+` Select Music",
            cameraX + 10, 28);

        batch.end();
    }

    private void drawMusicSelector() {
        float cx = cameraX + Gdx.graphics.getWidth() / 2f - 250;
        float cy = Gdx.graphics.getHeight() / 2f - 30;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0,0,0,0.85f));
        shapes.rect(cx, cy, 500, 60);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Music file (assets/musics/): " + musicInputBuffer + "_", cx + 8, cy + 44);
        font.setColor(new Color(1,1,1,0.5f));
        font.draw(batch, "Press ENTER to confirm, ESC to cancel", cx + 8, cy + 18);
        batch.end();
    }

    private void drawFilePrompt() {
        float cx = cameraX + Gdx.graphics.getWidth() / 2f - 250;
        float cy = Gdx.graphics.getHeight() / 2f - 30;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0,0,0,0.85f));
        shapes.rect(cx, cy, 500, 60);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        String prompt = filePromptIsSave ? "Save to file: " : "Load from file: ";
        font.draw(batch, prompt + fileBuffer + "_", cx + 8, cy + 44);
        font.setColor(new Color(1,1,1,0.5f));
        font.draw(batch, "Press ENTER to confirm, ESC to cancel", cx + 8, cy + 18);
        batch.end();
    }

    // ── Object placement ──────────────────────────────────────────────────────

    private void placeObjectAtCursor() {
        float[] wp = cursorWorldPos();
        String type = PALETTE[paletteIndex].type;
        placed.add(new LevelData.ObjectEntry(type, wp[0], wp[1], OBJECT_SIZE));
    }

    // ── Music ─────────────────────────────────────────────────────────────────

    private void loadMusic(String filename) {
        if (music != null) { music.stop(); music.dispose(); }
        try {
            FileHandle fh = Gdx.files.internal("musics/" + filename);
            if (!fh.exists()) fh = Gdx.files.absolute(filename);
            music = Gdx.audio.newMusic(fh);
            music.setLooping(false);
            levelMeta.musicFile = filename;
        } catch (Exception ex) {
            System.err.println("[Editor] Could not load music: " + filename + " — " + ex.getMessage());
        }
    }

    private void toggleMusic() {
        if (music == null) return;
        if (music.isPlaying()) music.pause();
        else                   music.play();
    }

    // ── Playtest ──────────────────────────────────────────────────────────────

    private void launchPlaytest() {
        LevelData data = buildLevelData();
        app.launchPlaytest(data);
    }

    private LevelData buildLevelData() {
        LevelData data = new LevelData();
        data.name       = levelMeta.name;
        data.musicFile  = levelMeta.musicFile;
        data.bgColor    = levelMeta.bgColor;
        data.groundColor = levelMeta.groundColor;
        data.objects    = new ArrayList<>(placed);
        return data;
    }

    // ── Save / Load ───────────────────────────────────────────────────────────

    private void saveLevel(String filename) {
        if (!filename.endsWith(".json")) filename += ".json";
        LevelData data = buildLevelData();
        FileHandle fh  = Gdx.files.local(filename);
        LevelSerializer.save(data, fh);
        System.out.println("[Editor] Saved to: " + fh.path());
    }

    private void loadLevel(String filename) {
        if (!filename.endsWith(".json")) filename += ".json";
        FileHandle fh = Gdx.files.local(filename);
        if (!fh.exists()) {
            System.err.println("[Editor] File not found: " + fh.path());
            return;
        }
        LevelData data = LevelSerializer.load(fh);
        placed.clear();
        placed.addAll(data.objects);
        levelMeta = data;
        System.out.println("[Editor] Loaded: " + fh.path() + "  (" + placed.size() + " objects)");

        if (data.musicFile != null && !data.musicFile.isEmpty()) {
            loadMusic(data.musicFile);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns world-space [x, y] of the mouse cursor, optionally snapped. */
    private float[] cursorWorldPos() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);
        float wx = snapToGrid ? snapX(v.x) : v.x;
        float wy = snapToGrid ? snapY(v.y) : v.y;
        return new float[]{ wx, wy };
    }

    private float snapX(float x) { return (float)(Math.floor(x / GRID_SIZE) * GRID_SIZE); }
    private float snapY(float y) { return (float)(Math.floor(y / GRID_SIZE) * GRID_SIZE); }

    private Color colorFor(String type) {
        for (PaletteEntry e : PALETTE) if (e.type.equals(type)) return e.color;
        return Color.GRAY;
    }

    private String labelFor(String type) {
        for (PaletteEntry e : PALETTE) if (e.type.equals(type)) return e.name.substring(0,1);
        return "?";
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    @Override public void resize(int w, int h) {
        camera.viewportWidth  = w;
        camera.viewportHeight = h;
        camera.update();
    }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        if (music != null) music.dispose();
    }

    private void deleteObjectAtCursor() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);

        // iterate in reverse so we delete the topmost (last placed) object first
        for (int i = placed.size() - 1; i >= 0; i--) {
            LevelData.ObjectEntry e = placed.get(i);
            if (v.x >= e.x && v.x <= e.x + e.size &&
                v.y >= e.y && v.y <= e.y + e.size) {
                placed.remove(i);
                return; // only delete one at a time
            }
        }
    }

    // ── Input adapter ─────────────────────────────────────────────────────────

    private class EditorInputAdapter extends InputAdapter {

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.RIGHT) {
                // CTRL + right click = delete object under cursor
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
                    deleteObjectAtCursor();
                    return true;
                }
                panning   = true;
                lastMouseX = screenX;
                return true;
            }
            if (button == Input.Buttons.LEFT) {
                if (!musicSelectorOpen && !filePromptOpen) {
                    placeObjectAtCursor();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (panning) {
                float dx = screenX - lastMouseX;
                cameraX -= dx; // move world left when dragging right
                lastMouseX = screenX;
                return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.RIGHT) { panning = false; return true; }
            return false;
        }

        @Override
        public boolean keyTyped(char c) {
            // music selector input
            if (musicSelectorOpen) {
                if (c == '\r' || c == '\n') {
                    String file = musicInputBuffer.toString().trim();
                    if (!file.isEmpty()) loadMusic(file);
                    musicSelectorOpen = false;
                } else if (c == 27) { // ESC
                    musicSelectorOpen = false;
                } else if (c == '\b') {
                    if (musicInputBuffer.length() > 0)
                        musicInputBuffer.deleteCharAt(musicInputBuffer.length() - 1);
                } else {
                    musicInputBuffer.append(c);
                }
                return true;
            }

            // file prompt input
            if (filePromptOpen) {
                if (c == '\r' || c == '\n') {
                    String fname = fileBuffer.toString().trim();
                    if (!fname.isEmpty()) {
                        if (filePromptIsSave) saveLevel(fname);
                        else                  loadLevel(fname);
                    }
                    filePromptOpen = false;
                } else if (c == 27) {
                    filePromptOpen = false;
                } else if (c == '\b') {
                    if (fileBuffer.length() > 0)
                        fileBuffer.deleteCharAt(fileBuffer.length() - 1);
                } else {
                    fileBuffer.append(c);
                }
                return true;
            }

            return false;
        }
    }

    // ── Palette entry ─────────────────────────────────────────────────────────

    private static class PaletteEntry {
        final String name;
        final String type;
        final Color  color;
        PaletteEntry(String name, String type, Color color) {
            this.name  = name;
            this.type  = type;
            this.color = color;
        }
    }
}
