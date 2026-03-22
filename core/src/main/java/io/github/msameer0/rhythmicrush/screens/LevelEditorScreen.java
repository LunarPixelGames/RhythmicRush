package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.game.GameWorld;
import io.github.msameer0.rhythmicrush.game.engine.FixedTickEngine;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelSerializer;
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer;
import io.github.msameer0.rhythmicrush.game.registries.Registries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry-driven level editor.
 *
 * <p>Layout: top bar (New/Save/Play/music/zoom), left canvas (pan/zoom/place/select),
 * right sidebar (collapsible registry tabs).
 *
 * <p>Tabs are built dynamically from {@link Registries} — adding a new registered
 * type automatically shows it in the panel.
 *
 * <p>Playtest runs the real {@link GameWorld} + {@link GameRenderer}.
 * A fading dot trail records the player path and persists after playtest ends.
 */
public class LevelEditorScreen extends AbstractScreen {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final float SIDEBAR_W  = 260f;
    private static final float TOPBAR_H   = 48f;
    private static final float GRID_SIZE  = 50f;
    private static final float ITEM_PAD   = 6f;
    private static final float ITEM_SIZE  = 48f;
    private static final float TAB_H      = 34f;
    private static final float CAM_SPEED  = 400f;
    private static final float CAMERA_X_OFFSET = 425f; // must match GameRenderer

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color C_BG       = new Color(0.10f, 0.10f, 0.14f, 1f);
    private static final Color C_CANVAS   = new Color(0.13f, 0.13f, 0.18f, 1f);
    private static final Color C_SIDEBAR  = new Color(0.09f, 0.09f, 0.13f, 1f);
    private static final Color C_TOPBAR   = new Color(0.07f, 0.07f, 0.10f, 1f);
    private static final Color C_GRID     = new Color(1f,    1f,    1f,    0.06f);
    private static final Color C_GROUND   = new Color(0.09f, 0.13f, 0.24f, 1f);
    private static final Color C_TAB_ON   = new Color(0.20f, 0.45f, 0.85f, 1f);
    private static final Color C_TAB_OFF  = new Color(0.16f, 0.16f, 0.22f, 1f);
    private static final Color C_ITEM_HOV = new Color(0.28f, 0.28f, 0.38f, 1f);
    private static final Color C_ITEM_SEL = new Color(0.20f, 0.45f, 0.85f, 0.70f);
    private static final Color C_BTN      = new Color(0.18f, 0.18f, 0.26f, 1f);
    private static final Color C_BTN_HOV  = new Color(0.28f, 0.48f, 0.78f, 1f);
    private static final Color C_BTN_PLAY = new Color(0.20f, 0.65f, 0.35f, 1f);
    private static final Color C_BTN_STOP = new Color(0.75f, 0.25f, 0.25f, 1f);
    private static final Color C_SEL_OUT  = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color C_TRAIL    = new Color(0.35f, 1.00f, 0.55f, 0.80f);

    // ── Rendering ─────────────────────────────────────────────────────────────
    private OrthographicCamera uiCam;
    private ScreenViewport     uiViewport;
    private ShapeRenderer      shapes;
    private BitmapFont         font;
    private GlyphLayout        layout;

    // Texture regions for edit-mode rendering
    private TextureRegion[] blockRegions;
    private TextureRegion   spikeRegion;
    private TextureRegion   cubePortalRegion;
    private TextureRegion   shipPortalRegion;

    // ── Canvas state ──────────────────────────────────────────────────────────
    private float   camX = 400f, camY = 200f;
    private float   zoom = 1.0f;
    private boolean panning = false;
    private float   panStartX, panStartY, panCamX0, panCamY0;

    // Rubber-band selection
    private boolean rubberBanding = false;
    private float   rbStartWX, rbStartWY;

    // ── Level data ────────────────────────────────────────────────────────────
    private LevelData                          levelData;
    private final Array<LevelData.ObjectEntry> selection = new Array<>();
    private String                             savePath  = null;

    // Block type cycling (TAB key)
    private int selectedBlockTypeIdx = 0;

    // Music file list
    private final List<String> musicFiles   = new ArrayList<>();
    private int                musicFileIdx = -1;
    private Music              levelMusic   = null;

    // WASD key-repeat for moving selection
    private float wasdHeld  = 0f;
    private int   wasdDir   = 0;
    private static final float WASD_INITIAL = 0.25f;
    private static final float WASD_REPEAT  = 0.07f;

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private static class Tab {
        String             label;
        List<String>       ids    = new ArrayList<>();
        Map<String, Color> colors = new LinkedHashMap<>();
        boolean            collapsed  = false;
        String             selectedId = null;
    }

    private final List<Tab> tabs          = new ArrayList<>();
    private float           sidebarScroll = 0f;
    private String          placementId   = null;

    // ── Playtest ──────────────────────────────────────────────────────────────
    private boolean            playtesting = false;
    private GameWorld          ptWorld;
    private GameRenderer       ptRenderer;
    private FixedTickEngine    ptEngine;
    private OrthographicCamera ptCam;
    private boolean            lastJump    = false;

    // Trail — stores absolute world coords (player.x + worldScrolled, player.y).
    // Persists after playtest so edit mode shows last run path.
    private static final int TRAIL_SAMPLE = 3;
    private final FloatArray trailWX      = new FloatArray();
    private final FloatArray trailWY      = new FloatArray();
    private int              trailTick    = 0;
    private boolean          trailHasData = false;

    // ── Inline property panel ─────────────────────────────────────────────────
    private boolean               propPanelOpen  = false;
    private LevelData.ObjectEntry propTarget     = null;
    private int                   propField      = 0;       // which field is focused
    private final StringBuilder[] propBuffers    = {
        new StringBuilder(), new StringBuilder(), new StringBuilder(),
        new StringBuilder(), new StringBuilder()
    };
    private String[]              propLabels     = {};
    private int                   propFieldCount = 0;

    private static final Color C_PROP_BG     = new Color(0.07f, 0.07f, 0.11f, 0.97f);
    private static final Color C_PROP_BORDER = new Color(0.35f, 0.65f, 1.00f, 1f);
    private static final Color C_PROP_ACTIVE = new Color(1.00f, 0.95f, 0.35f, 1f);
    private static final Color C_PROP_DIM    = new Color(1.00f, 1.00f, 1.00f, 0.50f);

    // ── Tmp ───────────────────────────────────────────────────────────────────
    private final Vector2 _tv = new Vector2();

    // ─────────────────────────────────────────────────────────────────────────

    public LevelEditorScreen(RhythmicRushGame game) {
        this(game, null);
    }

    public LevelEditorScreen(RhythmicRushGame game, LevelData existing) {
        super(game);
        levelData = (existing != null) ? existing : new LevelData();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void show() {
        game.getSoundManager().stopMenuMusic();

        uiCam      = new OrthographicCamera();
        uiViewport = new ScreenViewport(uiCam);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        uiCam.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
        uiCam.update();

        shapes = new ShapeRenderer();
        font   = game.getFontManager().get(FontManager.SIZE_SMALL);
        layout = new GlyphLayout();

        // Load texture regions
        BlockType[] btypes = BlockType.values();
        blockRegions = new TextureRegion[btypes.length];
        for (BlockType bt : btypes)
            blockRegions[bt.ordinal()] = game.getAtlasManager().getBlocksAtlas().findRegion(bt.textureName);
        spikeRegion      = game.getAtlasManager().getSpikesAtlas().findRegion("spike");
        cubePortalRegion = game.getAtlasManager().getPortalsAtlas().findRegion("cube_portal");
        shipPortalRegion = game.getAtlasManager().getPortalsAtlas().findRegion("ship_portal");

        buildTabs();
        scanMusicFiles();

        // Sync musicFileIdx with existing level data
        if (levelData.musicFile != null && !levelData.musicFile.isEmpty()) {
            for (int i = 0; i < musicFiles.size(); i++) {
                if (musicFiles.get(i).equals(levelData.musicFile)) { musicFileIdx = i; break; }
            }
        }

        Gdx.input.setInputProcessor(new EditorInput());
    }

    @Override public void hide()    { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        shapes.dispose();
        stopPlaytest();
        stopAndDisposeMusic();
    }

    @Override
    public void resize(int width, int height) {
        uiViewport.update(width, height, false);
        uiCam.position.set(width / 2f, height / 2f, 0);
        uiCam.update();
    }

    // ── Tab building ──────────────────────────────────────────────────────────

    private void buildTabs() {
        tabs.clear();
        tabs.add(buildTab("Blocks",   Registries.BLOCKS,   new Color(0.30f, 0.50f, 0.90f, 1f)));
        tabs.add(buildTab("Hazards",  Registries.HAZARDS,  new Color(0.90f, 0.30f, 0.30f, 1f)));
        tabs.add(buildTab("Portals",  Registries.PORTALS,  new Color(0.30f, 0.90f, 0.50f, 1f)));
        tabs.add(buildTab("Triggers", Registries.TRIGGERS, new Color(0.90f, 0.80f, 0.30f, 1f)));
    }

    private Tab buildTab(String label,
                         io.github.msameer0.rhythmicrush.game.registries.GameRegistry<?> reg,
                         Color base) {
        Tab t = new Tab();
        t.label = label;
        for (String id : reg.getIds()) {
            t.ids.add(id);
            float h = (id.hashCode() & 0x7fffffff) / (float) Integer.MAX_VALUE;
            t.colors.put(id, new Color(
                MathUtils.clamp(base.r + h * 0.15f, 0f, 1f),
                MathUtils.clamp(base.g + h * 0.10f, 0f, 1f),
                MathUtils.clamp(base.b + h * 0.10f, 0f, 1f), 0.85f));
        }
        return t;
    }

    // ── Update / render ───────────────────────────────────────────────────────

    @Override
    public void render(float delta) {
        update(delta);
        draw();
    }

    @Override
    protected void update(float delta) {
        handleKeys(delta);
        if (playtesting) {
            tickPlaytest(delta);
        } else if (levelMusic != null && levelMusic.isPlaying()) {
            camX = levelMusic.getPosition() * 320f;
        }
    }

    @Override
    protected void draw() {
        Gdx.gl.glClearColor(C_BG.r, C_BG.g, C_BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        uiViewport.apply();
        uiCam.update();

        int   sw      = Gdx.graphics.getWidth();
        int   sh      = Gdx.graphics.getHeight();
        float canvasW = sw - SIDEBAR_W;
        float canvasH = sh - TOPBAR_H;

        drawTopBar(sw, sh);
        drawCanvas(canvasW, canvasH);
        drawSidebar(sw, sh, canvasH);

        // Playtest overlay (real GameRenderer into canvas viewport)
        if (playtesting && ptRenderer != null) {
            Gdx.gl.glViewport(0, (int) TOPBAR_H, (int) canvasW, (int) canvasH);
            ptRenderer.render(Gdx.graphics.getDeltaTime(), false, false);
            Gdx.gl.glViewport(0, 0, sw, sh);
            uiViewport.apply();
        }

        // Trail always drawn (during and after playtest) when data exists
        if (trailHasData) drawTrail(sh);
        if (propPanelOpen) drawPropertyPanel();
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private void drawTopBar(int sw, int sh) {
        float y = sh - TOPBAR_H;

        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(C_TOPBAR);
        shapes.rect(0, y, sw, TOPBAR_H);
        shapes.end();

        float bw = 68f, bh = TOPBAR_H - 12f, by = y + 6f;
        boolean musPlaying = levelMusic != null && levelMusic.isPlaying();
        drawBtn(8f,       by, bw, bh, musPlaying ? "Mus ⏸" : "Mus ▶", musPlaying);
        drawBtn(84f,      by, bw, bh, "Mus ⏹", false);

        float offX = 160f;
        drawBtn(offX + 8f,       by, bw, bh, "New",  false);
        drawBtn(offX + 84f,      by, bw, bh, "Save", false);
        drawBtn(offX + 160f,     by, bw, bh, playtesting ? "Stop" : "Play", playtesting);
        drawBtn(offX + 236f,     by, bw, bh, "Props", false);

        float zbw = 36f;
        float canvasW = sw - SIDEBAR_W;
        drawBtn(canvasW - zbw * 2 - 16f, by, zbw, bh, "-", false);
        drawBtn(canvasW - zbw - 8f,      by, zbw, bh, "+", false);

        game.getBatch().setProjectionMatrix(uiCam.combined);
        game.getBatch().begin();

        // Level name centered
        font.getData().setScale(0.72f);
        font.setColor(Color.WHITE);
        layout.setText(font, levelData.name);
        font.draw(game.getBatch(), levelData.name,
            sw / 2f - layout.width / 2f, y + TOPBAR_H / 2f + layout.height / 2f);

        // Block type indicator when block tool is armed
        if ("block".equals(placementId)) {
            String btLabel = "Block: " + BlockType.values()[selectedBlockTypeIdx].textureName + "  [TAB]";
            font.getData().setScale(0.48f);
            font.setColor(0.7f, 0.9f, 1f, 1f);
            layout.setText(font, btLabel);
            font.draw(game.getBatch(), btLabel, sw / 2f - layout.width / 2f, y + 10f);
        }

        // Music selector
        String musicLabel = musicFiles.isEmpty() ? "No music"
            : (musicFileIdx < 0 ? "♪  None" : "♪  " + musicFiles.get(musicFileIdx));
        font.getData().setScale(0.50f);
        font.setColor(0.8f, 0.75f, 1f, 1f);
        layout.setText(font, musicLabel);
        float musicX = canvasW - zbw * 2 - 24f - layout.width;
        font.draw(game.getBatch(), musicLabel, musicX, y + TOPBAR_H / 2f + layout.height / 2f);

        font.getData().setScale(0.40f);
        font.setColor(0.5f, 0.5f, 0.6f, 1f);
        String hint = "[ ]";
        layout.setText(font, hint);
        font.draw(game.getBatch(), hint, musicX - layout.width - 6f, y + TOPBAR_H / 2f + layout.height / 2f);

        font.getData().setScale(1f);
        game.getBatch().end();
    }

    private void drawBtn(float x, float y, float w, float h, String label, boolean active) {
        Vector2 m   = uiMouse();
        boolean hov = m.x >= x && m.x <= x + w && m.y >= y && m.y <= y + h;
        Color bg;
        if ("Stop".equals(label))  bg = active ? C_BTN_STOP : (hov ? C_BTN_HOV : C_BTN);
        else if ("Play".equals(label)) bg = hov ? C_BTN_HOV : C_BTN_PLAY;
        else                       bg = hov ? C_BTN_HOV : C_BTN;

        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(bg);
        shapes.rect(x, y, w, h);
        shapes.end();

        game.getBatch().setProjectionMatrix(uiCam.combined);
        game.getBatch().begin();
        font.getData().setScale(0.58f);
        font.setColor(Color.WHITE);
        layout.setText(font, label);
        font.draw(game.getBatch(), label,
            x + w / 2f - layout.width / 2f, y + h / 2f + layout.height / 2f);
        font.getData().setScale(1f);
        game.getBatch().end();
    }

    // ── Canvas ────────────────────────────────────────────────────────────────

    private void drawCanvas(float canvasW, float canvasH) {
        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(C_CANVAS);
        shapes.rect(0, 0, canvasW, canvasH);
        float groundSY = worldToSY(50f, canvasH);
        shapes.setColor(C_GROUND);
        shapes.rect(0, groundSY - 3, canvasW, 6);
        shapes.end();

        drawGrid(canvasW, canvasH);

        if (!playtesting) {
            drawObjects(canvasW, canvasH);
            drawSelectionOutlines(canvasW, canvasH);
            if (rubberBanding) drawRubberBand(canvasW, canvasH);
            if (placementId != null) drawGhost(canvasW, canvasH);
        }
    }

    private void drawGrid(float canvasW, float canvasH) {
        float sg = GRID_SIZE * zoom;
        if (sg < 5) return;

        float ox = canvasW / 2f - camX * zoom;
        float oy = canvasH / 2f - camY * zoom;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(C_GRID);
        for (float x = ((ox % sg) + sg) % sg; x < canvasW; x += sg) shapes.rect(x, 0, 1, canvasH);
        for (float y = ((oy % sg) + sg) % sg; y < canvasH; y += sg) shapes.rect(0, y, canvasW, 1);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawObjects(float canvasW, float canvasH) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Colored rects: triggers and anything without a texture
        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (LevelData.ObjectEntry e : levelData.objects) {
            if (regionFor(e) != null) continue;
            float sx = worldToSX(e.x, canvasW), sy = worldToSY(e.y, canvasH), dim = e.size * zoom;
            if (sx + dim < 0 || sx > canvasW || sy + dim < 0 || sy > canvasH) continue;
            Color c = typeColor(e.type);
            boolean sel = selection.contains(e, true);
            if (sel) shapes.setColor(Math.min(c.r * 1.5f, 1f), Math.min(c.g * 1.5f, 1f), Math.min(c.b * 1.5f, 1f), c.a);
            else     shapes.setColor(c);
            shapes.rect(sx, sy, dim, dim);
        }
        shapes.end();

        // Textured objects — all use rotation draw (rotation=0 means no rotation)
        game.getBatch().setProjectionMatrix(uiCam.combined);
        game.getBatch().begin();
        for (LevelData.ObjectEntry e : levelData.objects) {
            float sx = worldToSX(e.x, canvasW), sy = worldToSY(e.y, canvasH), dim = e.size * zoom;
            if (sx + dim < 0 || sx > canvasW || sy + dim < 0 || sy > canvasH) continue;
            TextureRegion region = regionFor(e);
            if (region == null) continue;
            boolean sel = selection.contains(e, true);
            game.getBatch().setColor(sel ? 1.3f : 1f, sel ? 1.3f : 1f, sel ? 1.3f : 1f, 1f);
            game.getBatch().draw(region, sx, sy, dim / 2f, dim / 2f, dim, dim, 1f, 1f, e.rotation);
        }
        game.getBatch().setColor(Color.WHITE);
        game.getBatch().end();

        // Trigger labels
        game.getBatch().setProjectionMatrix(uiCam.combined);
        game.getBatch().begin();
        font.getData().setScale(MathUtils.clamp(0.35f * zoom, 0.20f, 0.50f));
        font.setColor(1f, 1f, 1f, 0.85f);
        for (LevelData.ObjectEntry e : levelData.objects) {
            if (!Registries.TRIGGERS.has(e.type)) continue;
            float dim = e.size * zoom;
            if (dim < 16) continue;
            float sx = worldToSX(e.x, canvasW), sy = worldToSY(e.y, canvasH);
            if (sx + dim < 0 || sx > canvasW || sy + dim < 0 || sy > canvasH) continue;
            layout.setText(font, e.type);
            font.draw(game.getBatch(), e.type, sx + dim / 2f - layout.width / 2f, sy + dim / 2f + layout.height / 2f);
        }
        font.getData().setScale(1f);
        game.getBatch().end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSelectionOutlines(float canvasW, float canvasH) {
        if (selection.isEmpty()) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(C_SEL_OUT);
        for (LevelData.ObjectEntry e : selection) {
            float sx = worldToSX(e.x, canvasW) - 2, sy = worldToSY(e.y, canvasH) - 2;
            float dim = e.size * zoom + 4;
            shapes.rect(sx, sy, dim, dim);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawRubberBand(float canvasW, float canvasH) {
        Vector2 cur = canvasMouseWorld(canvasW, canvasH);
        float x1 = worldToSX(Math.min(rbStartWX, cur.x), canvasW);
        float y1 = worldToSY(Math.min(rbStartWY, cur.y), canvasH);
        float x2 = worldToSX(Math.max(rbStartWX, cur.x), canvasW);
        float y2 = worldToSY(Math.max(rbStartWY, cur.y), canvasH);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(C_SEL_OUT.r, C_SEL_OUT.g, C_SEL_OUT.b, 0.12f);
        shapes.rect(x1, y1, x2 - x1, y2 - y1);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(C_SEL_OUT);
        shapes.rect(x1, y1, x2 - x1, y2 - y1);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawGhost(float canvasW, float canvasH) {
        Vector2 w  = canvasMouseWorld(canvasW, canvasH);
        float   gx = snap(w.x), gy = snap(w.y);
        float   sx = worldToSX(gx, canvasW), sy = worldToSY(gy, canvasH);
        float   dim = GRID_SIZE * zoom;
        if (sx + dim < 0 || sx > canvasW) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        TextureRegion ghostRegion = placementRegion();
        if (ghostRegion != null) {
            game.getBatch().setProjectionMatrix(uiCam.combined);
            game.getBatch().begin();
            game.getBatch().setColor(1f, 1f, 1f, 0.45f);
            game.getBatch().draw(ghostRegion, sx, sy, dim / 2f, dim / 2f, dim, dim, 1f, 1f, 0);
            game.getBatch().setColor(Color.WHITE);
            game.getBatch().end();
        } else {
            Color c = typeColor(placementId);
            shapes.setProjectionMatrix(uiCam.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(c.r, c.g, c.b, 0.40f);
            shapes.rect(sx, sy, dim, dim);
            shapes.end();
        }

        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 1f, 1f, 0.70f);
        shapes.rect(sx, sy, dim, dim);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private void drawSidebar(int sw, int sh, float canvasH) {
        float sideX = sw - SIDEBAR_W;

        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(C_SIDEBAR);
        shapes.rect(sideX, 0, SIDEBAR_W, canvasH);
        shapes.end();

        float totalH = 0;
        for (Tab tab : tabs) totalH += TAB_H + (tab.collapsed ? 0 : tabContentH(tab));
        sidebarScroll = MathUtils.clamp(sidebarScroll, 0, Math.max(0, totalH - canvasH));

        Vector2 m    = uiMouse();
        float   curY = canvasH - sidebarScroll;
        for (Tab tab : tabs) curY = drawTab(tab, sideX, curY, canvasH, m);
    }

    private float drawTab(Tab tab, float sideX, float curY, float canvasH, Vector2 m) {
        float hTop = curY, hBot = curY - TAB_H;
        boolean hov = m.x >= sideX && m.x <= sideX + SIDEBAR_W && m.y >= hBot && m.y <= hTop;

        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(hov ? C_TAB_ON : C_TAB_OFF);
        shapes.rect(sideX, hBot, SIDEBAR_W, TAB_H);
        shapes.end();

        game.getBatch().setProjectionMatrix(uiCam.combined);
        game.getBatch().begin();
        font.getData().setScale(0.62f);
        font.setColor(Color.WHITE);
        String lbl = (tab.collapsed ? "▶  " : "▼  ") + tab.label;
        layout.setText(font, lbl);
        font.draw(game.getBatch(), lbl, sideX + 10f, hBot + TAB_H / 2f + layout.height / 2f);
        font.getData().setScale(1f);
        game.getBatch().end();

        curY = hBot;
        if (tab.collapsed) return curY;

        int   cols = Math.max(1, (int) ((SIDEBAR_W - ITEM_PAD) / (ITEM_SIZE + ITEM_PAD)));
        float cell = (SIDEBAR_W - ITEM_PAD * (cols + 1)) / cols;
        int   col  = 0;
        float rowY = curY;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for (String id : tab.ids) {
            if (col == 0) rowY -= cell + ITEM_PAD;
            float ix = sideX + ITEM_PAD + col * (cell + ITEM_PAD);
            float iy = rowY;

            if (iy + cell > 0 && iy < canvasH) {
                boolean itemHov = m.x >= ix && m.x <= ix + cell && m.y >= iy && m.y <= iy + cell;
                boolean itemSel = id.equals(tab.selectedId);

                shapes.setProjectionMatrix(uiCam.combined);
                shapes.begin(ShapeRenderer.ShapeType.Filled);
                if (itemSel)      shapes.setColor(C_ITEM_SEL);
                else if (itemHov) shapes.setColor(C_ITEM_HOV);
                else              shapes.setColor(tab.colors.getOrDefault(id, Color.GRAY));
                shapes.rect(ix, iy, cell, cell);
                shapes.end();

                if (itemSel) {
                    shapes.begin(ShapeRenderer.ShapeType.Line);
                    shapes.setColor(C_SEL_OUT);
                    shapes.rect(ix, iy, cell, cell);
                    shapes.end();
                }

                game.getBatch().setProjectionMatrix(uiCam.combined);
                game.getBatch().begin();
                font.getData().setScale(MathUtils.clamp(0.40f * (cell / ITEM_SIZE), 0.28f, 0.48f));
                font.setColor(Color.WHITE);
                String display = id.length() > 10 ? id.substring(0, 9) + "…" : id;
                layout.setText(font, display);
                font.draw(game.getBatch(), display,
                    ix + cell / 2f - layout.width / 2f, iy + cell / 2f + layout.height / 2f);
                font.getData().setScale(1f);
                game.getBatch().end();
            }

            col++;
            if (col >= cols) col = 0;
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
        if (col != 0) rowY -= cell + ITEM_PAD;
        return rowY - ITEM_PAD;
    }

    private float tabContentH(Tab tab) {
        if (tab.ids.isEmpty()) return 0;
        int   cols = Math.max(1, (int) ((SIDEBAR_W - ITEM_PAD) / (ITEM_SIZE + ITEM_PAD)));
        float cell = (SIDEBAR_W - ITEM_PAD * (cols + 1)) / cols;
        int   rows = (int) Math.ceil((double) tab.ids.size() / cols);
        return rows * (cell + ITEM_PAD) + ITEM_PAD;
    }

    // ── Trail ─────────────────────────────────────────────────────────────────

    private void drawTrail(int sh) {
        if (playtesting) return;
        if (!trailHasData || trailWX.size == 0) return;
        float canvasW = Gdx.graphics.getWidth() - SIDEBAR_W;
        float canvasH = sh - TOPBAR_H;
        float r       = 5f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Keep it a solid color so the whole line is visible
        shapes.setColor(C_TRAIL.r, C_TRAIL.g, C_TRAIL.b, C_TRAIL.a);

        for (int i = 0; i < trailWX.size; i++) {
            float absX = trailWX.get(i);
            float absY = trailWY.get(i);
            float sx, sy;

            if (playtesting && ptCam != null) {
                // FIXED MATH: Map world position to screen space based purely on camera position
                sx = absX - ptCam.position.x + canvasW / 2f;
                sy = absY - ptCam.position.y + canvasH / 2f;
            } else {
                // Edit mode (works correctly already)
                sx = worldToSX(absX, canvasW);
                sy = worldToSY(absY, canvasH);
            }

            // Cull points off-screen to keep performance up even with huge arrays
            if (sx < -r || sx > canvasW + r || sy < -r || sy > canvasH + r) continue;

            shapes.circle(sx, sy, r, 10);
        }

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────

    private void handleKeys(float delta) {

        if (propPanelOpen) return;
        // Playtest controls
        if (playtesting) {
            boolean jump = Gdx.input.isKeyPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyPressed(Input.Keys.UP)
                || Gdx.input.isTouched();
            if (jump != lastJump) {
                ptEngine.queueInput(jump, ptEngine.getAccumulator());
                lastJump = jump;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.F5)) stopPlaytest();
            return;
        }

        // TAB: cycle block type
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB) && "block".equals(placementId)) {
            selectedBlockTypeIdx = (selectedBlockTypeIdx + 1) % BlockType.values().length;
        }

        // [ / ]: cycle music
        if (!musicFiles.isEmpty()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
                musicFileIdx = (musicFileIdx - 1 + musicFiles.size()) % musicFiles.size();
                levelData.musicFile = musicFiles.get(musicFileIdx);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
                musicFileIdx = (musicFileIdx + 1) % musicFiles.size();
                levelData.musicFile = musicFiles.get(musicFileIdx);
            }
        }

        // Q / E: rotate selected objects ±45°
        if (!selection.isEmpty()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q))
                for (LevelData.ObjectEntry e : selection)
                    e.rotation = ((e.rotation + 45f) % 360f + 360f) % 360f;
            if (Gdx.input.isKeyJustPressed(Input.Keys.E))
                for (LevelData.ObjectEntry e : selection)
                    e.rotation = ((e.rotation - 45f) % 360f + 360f) % 360f;
        }

        // WASD: move selection or pan camera
        boolean wL = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean wR = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean wU = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean wD = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean anyWASD = wL || wR || wU || wD;

        if (!selection.isEmpty() && anyWASD && placementId == null) {
            int newDir = (wL ? 1 : 0) | (wR ? 2 : 0) | (wU ? 4 : 0) | (wD ? 8 : 0);
            if (newDir != wasdDir) { wasdDir = newDir; wasdHeld = 0f; }
            wasdHeld += delta;
            boolean firstFrame = wasdHeld == delta || wasdHeld <= delta + 0.001f;
            boolean repeat = wasdHeld > WASD_INITIAL
                && Math.floor((wasdHeld - WASD_INITIAL) / WASD_REPEAT)
                > Math.floor(((wasdHeld - delta) - WASD_INITIAL) / WASD_REPEAT);
            if (firstFrame || repeat) {
                float dx = wL ? -GRID_SIZE : wR ? GRID_SIZE : 0;
                float dy = wU ? GRID_SIZE : wD ? -GRID_SIZE : 0;
                for (LevelData.ObjectEntry e : selection) { e.x += dx; e.y += dy; }
            }
        } else {
            wasdDir = 0; wasdHeld = 0f;
            if (anyWASD) {
                float spd = CAM_SPEED * delta / zoom;
                if (wL) camX -= spd;
                if (wR) camX += spd;
                if (wU) camY += spd;
                if (wD) camY -= spd;
            }
        }

        // Delete
        if (Gdx.input.isKeyJustPressed(Input.Keys.DEL)
            || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            if (!selection.isEmpty()) {
                // Copy to avoid concurrent modification
                Array<LevelData.ObjectEntry> toDelete = new Array<>(selection);
                for (LevelData.ObjectEntry e : toDelete)
                    levelData.objects.removeValue(e, true);
                selection.clear();
            }
        }

        // P: Properties
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && !selection.isEmpty()) {
            showPropertyEditor(selection.first());
        }

        // Escape
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            placementId = null;
            for (Tab t : tabs) t.selectedId = null;
            selection.clear();
            rubberBanding = false;
        }

        // Ctrl+A
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            && Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selection.clear();
            selection.addAll(levelData.objects);
        }

        // Ctrl+S
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            && Gdx.input.isKeyJustPressed(Input.Keys.S)) saveLevel();

        // P: start playtest
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && selection.isEmpty()) startPlaytest();
    }

    // ── Playtest ──────────────────────────────────────────────────────────────

    private void startPlaytest() {
        if (playtesting) return;
        game.getSoundManager().stopMenuMusic();
        stopAndDisposeMusic();
        playtesting  = true;
        lastJump     = false;

        trailWX.clear();
        trailWY.clear();
        trailTick = 0;
        trailHasData = false;

        float canvasW = Gdx.graphics.getWidth() - SIDEBAR_W;
        float canvasH = Gdx.graphics.getHeight() - TOPBAR_H;

        ptCam = new OrthographicCamera();
        ptCam.setToOrtho(false, canvasW, canvasH);
        ptCam.update();

        ptWorld    = new GameWorld();
        ptRenderer = new GameRenderer(ptWorld, ptCam, game.getBatch(), game.getAtlasManager());
        ptEngine   = new FixedTickEngine(ptWorld);
        ptWorld.loadLevel(levelData);
    }

    private void tickPlaytest(float delta) {
        ptEngine.update(delta);

        trailTick++;
        if (trailTick >= TRAIL_SAMPLE && ptWorld.getPlayer() != null) {
            trailTick = 0;
            // --- NEW ---
            trailWX.add(ptWorld.getPlayer().x + ptWorld.getWorldScrolled() + ptWorld.getPlayer().width / 2f);
            trailWY.add(ptWorld.getPlayer().y + ptWorld.getPlayer().height / 2f);
            // -----------
            trailHasData = true;
        }

        if (ptWorld.isPlayerDead() || ptWorld.isLevelComplete()) stopPlaytest();
    }

    private void stopPlaytest() {
        if (!playtesting) return;
        playtesting = false;
        lastJump    = false;
        if (ptRenderer != null) { ptRenderer.dispose(); ptRenderer = null; }
        ptWorld = null; ptEngine = null;
        game.getSoundManager().playMenuMusic();
    }

    // ── Level I/O ─────────────────────────────────────────────────────────────

    private void newLevel() {
        stopPlaytest();
        stopAndDisposeMusic();
        levelData    = new LevelData();
        selection.clear(); savePath = null; placementId = null;

        // --- UPDATED TRAIL RESET ---
        trailHasData = false;
        trailWX.clear();
        trailWY.clear();
        trailTick = 0;
        // ---------------------------

        for (Tab t : tabs) t.selectedId = null;
    }

    private void saveLevel() {
        if (savePath == null)
            savePath = "assets/levels/" + levelData.name.replaceAll("\\s+", "_") + ".json";
        try {
            LevelSerializer.save(levelData, Gdx.files.local(savePath));
            Gdx.app.log("Editor", "Saved → " + savePath);
        } catch (Exception ex) {
            Gdx.app.error("Editor", "Save failed: " + ex.getMessage());
        }
    }

    private void scanMusicFiles() {
        musicFiles.clear();
        try {
            com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal("musics");
            if (dir.exists() && dir.isDirectory()) {
                for (com.badlogic.gdx.files.FileHandle f : dir.list()) {
                    String name = f.name();
                    if (name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav"))
                        musicFiles.add(name);
                }
                Collections.sort(musicFiles);
            }
        } catch (Exception ex) {
            Gdx.app.log("Editor", "Could not scan musics: " + ex.getMessage());
        }
        Gdx.app.log("Editor", "Found " + musicFiles.size() + " music file(s).");
    }

    private void placeObject(float wx, float wy) {
        LevelData.ObjectEntry e = new LevelData.ObjectEntry();
        e.type     = placementId;
        e.x        = wx;
        e.y        = wy;
        e.size     = GRID_SIZE;
        e.rotation = 0f;
        if (Registries.BLOCKS.has(placementId))
            e.blockType = BlockType.values()[selectedBlockTypeIdx].textureName;
        if ("color_trigger".equals(placementId))  e.fadeDuration = 1f;
        if ("pulse_trigger".equals(placementId)) {
            e.fadeInTime = 0.1f; e.holdTime = 0.2f; e.fadeOutTime = 0.5f;
        }
        levelData.objects.add(e);
        selection.clear();
        selection.add(e);
    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

    private float worldToSX(float wx, float canvasW) {
        return (wx - camX) * zoom + canvasW / 2f;
    }

    private float worldToSY(float wy, float canvasH) {
        return (wy - camY) * zoom + canvasH / 2f;
    }

    private Vector2 canvasMouseWorld(float canvasW, float canvasH) {
        float uiX = Gdx.input.getX();
        float uiY = Gdx.graphics.getHeight() - Gdx.input.getY() - TOPBAR_H;
        _tv.x = (uiX - canvasW / 2f) / zoom + camX;
        _tv.y = (uiY - canvasH / 2f) / zoom + camY;
        return _tv;
    }

    private float snap(float v) { return Math.round(v / GRID_SIZE) * GRID_SIZE; }

    private Vector2 uiMouse() {
        return new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
    }

    private Color typeColor(String id) {
        if (id == null) return Color.GRAY;
        if (Registries.BLOCKS.has(id))   return new Color(0.30f, 0.50f, 0.90f, 0.85f);
        if (Registries.HAZARDS.has(id))  return new Color(0.90f, 0.30f, 0.30f, 0.85f);
        if (Registries.PORTALS.has(id))  return new Color(0.30f, 0.90f, 0.50f, 0.85f);
        if ("color_trigger".equals(id)) return new Color(0.9f, 0.2f, 0.9f, 0.85f); // Pinkish for color
        if ("pulse_trigger".equals(id)) return new Color(0.9f, 0.9f, 0.2f, 0.85f); // Yellowish for pulse
        if (Registries.TRIGGERS.has(id)) return new Color(0.90f, 0.80f, 0.30f, 0.85f);
        return Color.GRAY;
    }

    /** TextureRegion for a placed object entry, or null if it should render as a rect. */
    private TextureRegion regionFor(LevelData.ObjectEntry e) {
        if ("spike".equals(e.type))       return spikeRegion;
        if ("cube_portal".equals(e.type)) return cubePortalRegion;
        if ("ship_portal".equals(e.type)) return shipPortalRegion;
        if (Registries.BLOCKS.has(e.type) && blockRegions != null) {
            BlockType bt = BlockType.DEFAULT;
            if (e.blockType != null)
                for (BlockType t : BlockType.values())
                    if (t.textureName.equals(e.blockType)) { bt = t; break; }
            return blockRegions[bt.ordinal()];
        }
        return null;
    }

    /** Ghost texture region for the current placement tool. */
    private TextureRegion placementRegion() {
        if (placementId == null) return null;
        LevelData.ObjectEntry tmp = new LevelData.ObjectEntry();
        tmp.type = placementId;
        if (Registries.BLOCKS.has(placementId))
            tmp.blockType = BlockType.values()[selectedBlockTypeIdx].textureName;
        return regionFor(tmp);
    }

    // ── Input adapter ─────────────────────────────────────────────────────────

    private class EditorInput extends InputAdapter {

        @Override
        public boolean keyTyped(char c) {
            if (!propPanelOpen) return false;
            if (c == '\r' || c == '\n') { confirmPropertyEdit(); return true; }
            if (c == 27)  { propPanelOpen = false; propTarget = null; return true; } // ESC
            if (c == '\t') { propField = (propField + 1) % propFieldCount; return true; }
            StringBuilder active = propBuffers[propField];
            if (c == '\b') { if (active.length() > 0) active.deleteCharAt(active.length() - 1); }
            else if (c >= 32) { active.append(c); }
            return true;
        }

        @Override
        public boolean touchDown(int sx, int sy, int pointer, int button) {
            if (propPanelOpen) return false;

            int   sw      = Gdx.graphics.getWidth();
            int   sh      = Gdx.graphics.getHeight();
            float canvasW = sw - SIDEBAR_W;
            float canvasH = sh - TOPBAR_H;
            Vector2 ui    = new Vector2(sx, sh - sy);

            // ── Top bar ───────────────────────────────────────────────────────
            float bw = 68f, bh = TOPBAR_H - 12f, by = sh - TOPBAR_H + 6f, zbw = 36f;
            if (ui.y >= by && ui.y <= by + bh) {
                // Music Controls
                if (ui.x >= 8f  && ui.x <= 8f + bw)  {
                    if (levelMusic != null && levelMusic.isPlaying()) levelMusic.pause();
                    else startEditorMusic(false);
                    return true;
                }
                if (ui.x >= 84f && ui.x <= 84f + bw) { stopAndDisposeMusic(); return true; }

                float offX = 160f;
                if (ui.x >= offX + 8f    && ui.x <= offX + 8f + bw)    { newLevel();   return true; }
                if (ui.x >= offX + 84f   && ui.x <= offX + 84f + bw)   { saveLevel();  return true; }
                if (ui.x >= offX + 160f  && ui.x <= offX + 160f + bw)  {
                    if (playtesting) stopPlaytest(); else startPlaytest();
                    return true;
                }
                if (ui.x >= offX + 236f && ui.x <= offX + 236f + bw) {
                    if (!selection.isEmpty()) showPropertyEditor(selection.first());
                    return true;
                }
                float zMinX  = canvasW - zbw * 2 - 16f;
                float zPlusX = canvasW - zbw - 8f;
                if (ui.x >= zMinX  && ui.x <= zMinX  + zbw) { zoom = MathUtils.clamp(zoom * 0.8f,  0.08f, 10f); return true; }
                if (ui.x >= zPlusX && ui.x <= zPlusX + zbw) { zoom = MathUtils.clamp(zoom * 1.25f, 0.08f, 10f); return true; }
            }

            // ── Sidebar ───────────────────────────────────────────────────────
            if (ui.x >= canvasW && ui.y < canvasH) {
                handleSidebarClick(ui, canvasW, canvasH);
                return true;
            }

            // ── Canvas ────────────────────────────────────────────────────────
            if (ui.y >= TOPBAR_H && ui.y <= canvasH + TOPBAR_H && ui.x < canvasW) {
                if (playtesting) return true;

                boolean altPan = button == Input.Buttons.MIDDLE
                    || Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT);
                if (altPan) {
                    panning   = true;
                    panStartX = sx; panStartY = sy;
                    panCamX0  = camX; panCamY0 = camY;
                    return true;
                }

                if (button == Input.Buttons.LEFT) {
                    float uiY = sh - sy - TOPBAR_H;
                    float wx  = (ui.x - canvasW / 2f) / zoom + camX;
                    float wy  = (uiY  - canvasH / 2f) / zoom + camY;

                    if (placementId != null) {
                        placeObject(snap(wx), snap(wy));
                    } else {
                        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                        LevelData.ObjectEntry hit = null;
                        for (int i = levelData.objects.size - 1; i >= 0; i--) {
                            LevelData.ObjectEntry e = levelData.objects.get(i);
                            if (wx >= e.x && wx <= e.x + e.size && wy >= e.y && wy <= e.y + e.size) {
                                hit = e; break;
                            }
                        }
                        if (hit != null) {
                            if (shift) {
                                if (selection.contains(hit, true)) selection.removeValue(hit, true);
                                else selection.add(hit);
                            } else {
                                if (!selection.contains(hit, true)) {
                                    selection.clear();
                                    selection.add(hit);
                                }
                            }
                        } else {
                            if (!shift) selection.clear();
                            rubberBanding = true;
                            rbStartWX = wx; rbStartWY = wy;
                        }
                    }
                    return true;
                }

                if (button == Input.Buttons.RIGHT) {
                    placementId = null;
                    for (Tab t : tabs) t.selectedId = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean touchDragged(int sx, int sy, int pointer) {
            if (panning) {
                camX = panCamX0 - (sx - panStartX) / zoom;
                camY = panCamY0 + (sy - panStartY) / zoom;
                return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int sx, int sy, int pointer, int button) {
            if (button == Input.Buttons.MIDDLE) { panning = false; return false; }
            if (button == Input.Buttons.LEFT && rubberBanding) {
                rubberBanding = false;
                float canvasW = Gdx.graphics.getWidth() - SIDEBAR_W;
                float canvasH = Gdx.graphics.getHeight() - TOPBAR_H;
                float uiY     = Gdx.graphics.getHeight() - sy - TOPBAR_H;
                float wx      = (sx           - canvasW / 2f) / zoom + camX;
                float wy      = (uiY          - canvasH / 2f) / zoom + camY;
                float minX = Math.min(rbStartWX, wx), maxX = Math.max(rbStartWX, wx);
                float minY = Math.min(rbStartWY, wy), maxY = Math.max(rbStartWY, wy);
                boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                if (!shift) selection.clear();
                for (LevelData.ObjectEntry e : levelData.objects) {
                    if (e.x + e.size > minX && e.x < maxX && e.y + e.size > minY && e.y < maxY)
                        if (!selection.contains(e, true)) selection.add(e);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            int     sw      = Gdx.graphics.getWidth();
            int     sh      = Gdx.graphics.getHeight();
            float   canvasW = sw - SIDEBAR_W;
            Vector2 ui      = new Vector2(Gdx.input.getX(), sh - Gdx.input.getY());

            if (ui.x >= canvasW) {
                sidebarScroll = MathUtils.clamp(sidebarScroll + amountY * 24f, 0f, 9999f);
            } else {
                float oldZoom = zoom;
                zoom = MathUtils.clamp(zoom * (1f - amountY * 0.12f), 0.08f, 10f);
                float canvasH = sh - TOPBAR_H;
                float uiY     = sh - Gdx.input.getY() - TOPBAR_H;
                float wxBefore = (ui.x - canvasW / 2f) / oldZoom + camX;
                float wyBefore = (uiY  - canvasH / 2f) / oldZoom + camY;
                float wxAfter  = (ui.x - canvasW / 2f) / zoom    + camX;
                float wyAfter  = (uiY  - canvasH / 2f) / zoom    + camY;
                camX += wxBefore - wxAfter;
                camY += wyBefore - wyAfter;
            }
            return true;
        }

        private void handleSidebarClick(Vector2 ui, float canvasW, float canvasH) {
            float curY = canvasH - sidebarScroll;
            for (Tab tab : tabs) {
                float hTop = curY, hBot = curY - TAB_H;
                if (ui.y >= hBot && ui.y <= hTop) { tab.collapsed = !tab.collapsed; return; }
                curY = hBot;
                if (tab.collapsed) continue;

                int   cols = Math.max(1, (int) ((SIDEBAR_W - ITEM_PAD) / (ITEM_SIZE + ITEM_PAD)));
                float cell = (SIDEBAR_W - ITEM_PAD * (cols + 1)) / cols;
                int   col  = 0;
                float rowY = curY;
                for (String id : tab.ids) {
                    if (col == 0) rowY -= cell + ITEM_PAD;
                    float ix = canvasW + ITEM_PAD + col * (cell + ITEM_PAD);
                    float iy = rowY;
                    if (ui.x >= ix && ui.x <= ix + cell && ui.y >= iy && ui.y <= iy + cell) {
                        if (id.equals(tab.selectedId)) {
                            tab.selectedId = null; placementId = null;
                        } else {
                            for (Tab t : tabs) t.selectedId = null;
                            tab.selectedId = id;
                            placementId    = id;
                            selection.clear();
                        }
                        return;
                    }
                    col++;
                    if (col >= cols) col = 0;
                }
                if (col != 0) rowY -= cell + ITEM_PAD;
                curY = rowY - ITEM_PAD;
            }
        }
    }

    private void showPropertyEditor(LevelData.ObjectEntry e) {
        propTarget = e;
        propField  = 0;
        for (StringBuilder sb : propBuffers) sb.setLength(0);

        if ("color_trigger".equals(e.type)) {
            propLabels     = new String[]{"BG Color (hex)", "Ground Color (hex)", "Fade Duration (s)"};
            propFieldCount = 3;
            propBuffers[0].append(e.triggerBgColor     != null ? e.triggerBgColor     : "1a1a2e");
            propBuffers[1].append(e.triggerGroundColor != null ? e.triggerGroundColor : "16213e");
            propBuffers[2].append(e.fadeDuration);
        } else if ("pulse_trigger".equals(e.type)) {
            propLabels     = new String[]{"Pulse BG (hex)", "Pulse Ground (hex)", "Fade In (s)", "Hold (s)", "Fade Out (s)"};
            propFieldCount = 5;
            propBuffers[0].append(e.pulseBgColor     != null ? e.pulseBgColor     : "1a1a2e");
            propBuffers[1].append(e.pulseGroundColor != null ? e.pulseGroundColor : "16213e");
            propBuffers[2].append(e.fadeInTime);
            propBuffers[3].append(e.holdTime);
            propBuffers[4].append(e.fadeOutTime);
        } else {
            propLabels     = new String[]{"Size", "Rotation (degrees)"};
            propFieldCount = 2;
            propBuffers[0].append(e.size);
            propBuffers[1].append(e.rotation);
        }
        propPanelOpen = true;
    }

    private void confirmPropertyEdit() {
        if (propTarget == null) { propPanelOpen = false; return; }
        if ("color_trigger".equals(propTarget.type)) {
            propTarget.triggerBgColor     = propBuffers[0].toString().trim().replace("#","");
            propTarget.triggerGroundColor = propBuffers[1].toString().trim().replace("#","");
            try { propTarget.fadeDuration = Float.parseFloat(propBuffers[2].toString().trim()); } catch (Exception ignored) {}
        } else if ("pulse_trigger".equals(propTarget.type)) {
            propTarget.pulseBgColor     = propBuffers[0].toString().trim().replace("#","");
            propTarget.pulseGroundColor = propBuffers[1].toString().trim().replace("#","");
            try { propTarget.fadeInTime  = Float.parseFloat(propBuffers[2].toString().trim()); } catch (Exception ignored) {}
            try { propTarget.holdTime    = Float.parseFloat(propBuffers[3].toString().trim()); } catch (Exception ignored) {}
            try { propTarget.fadeOutTime = Float.parseFloat(propBuffers[4].toString().trim()); } catch (Exception ignored) {}
        } else {
            try { propTarget.size     = Float.parseFloat(propBuffers[0].toString().trim()); } catch (Exception ignored) {}
            try { propTarget.rotation = Float.parseFloat(propBuffers[1].toString().trim()); } catch (Exception ignored) {}
        }
        propPanelOpen = false;
        propTarget    = null;
    }

    private void drawPropertyPanel() {
        if (!propPanelOpen || propTarget == null) return;
        int   sw = Gdx.graphics.getWidth();
        int   sh = Gdx.graphics.getHeight();
        float pw = 520f, ph = 32f + propFieldCount * 36f + 32f;
        float px = sw / 2f - pw / 2f;
        float py = sh / 2f - ph / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Panel background
        shapes.setProjectionMatrix(uiCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(C_PROP_BG);
        shapes.rect(px, py, pw, ph);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(C_PROP_BORDER);
        shapes.rect(px, py, pw, ph);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.getBatch().setProjectionMatrix(uiCam.combined);
        game.getBatch().begin();

        // Title
        font.getData().setScale(0.62f);
        font.setColor(C_PROP_BORDER);
        String title = "Properties: " + propTarget.type + "   [TAB next  |  ENTER confirm  |  ESC cancel]";
        layout.setText(font, title);
        font.draw(game.getBatch(), title, px + 12f, py + ph - 10f);

        // Fields
        for (int i = 0; i < propFieldCount; i++) {
            float fy = py + ph - 36f - i * 36f;
            boolean active = (i == propField);

            font.getData().setScale(0.56f);
            font.setColor(active ? C_PROP_ACTIVE : C_PROP_DIM);
            String label = propLabels[i] + ":  ";
            layout.setText(font, label);
            font.draw(game.getBatch(), label, px + 12f, fy);

            // Value + cursor
            String value = propBuffers[i].toString() + (active ? "_" : "");
            font.setColor(Color.WHITE);
            font.draw(game.getBatch(), value, px + 12f + layout.width, fy);

            // Color swatch for hex fields
            if (i < 2 && (propTarget.type.contains("trigger"))) {
                String hex = propBuffers[i].toString().trim().replace("#","");
                if (hex.length() >= 6) {
                    try {
                        Color swatch = GameWorld.hexToColor(hex);
                        game.getBatch().end();
                        shapes.begin(ShapeRenderer.ShapeType.Filled);
                        shapes.setColor(swatch);
                        shapes.rect(px + pw - 40f, fy - 14f, 24f, 18f);
                        shapes.end();
                        shapes.begin(ShapeRenderer.ShapeType.Line);
                        shapes.setColor(Color.WHITE);
                        shapes.rect(px + pw - 40f, fy - 14f, 24f, 18f);
                        shapes.end();
                        game.getBatch().setProjectionMatrix(uiCam.combined);
                        game.getBatch().begin();
                    } catch (Exception ignored) {}
                }
            }
        }

        font.getData().setScale(1f);
        game.getBatch().end();
    }

    private void startEditorMusic(boolean restart) {
        if (levelData.musicFile == null || levelData.musicFile.isEmpty()) return;
        try {
            if (levelMusic == null) {
                FileHandle fh = Gdx.files.internal("musics/" + levelData.musicFile);
                if (!fh.exists()) fh = Gdx.files.local("assets/musics/" + levelData.musicFile);
                if (fh.exists()) {
                    levelMusic = Gdx.audio.newMusic(fh);
                    levelMusic.setVolume(game.getSettingsManager().musicVolume);
                    levelMusic.setLooping(restart); // Looping if we want to playtest maybe? No, false.
                    levelMusic.setLooping(false);
                }
            }
            if (levelMusic != null) {
                if (restart) levelMusic.setPosition(0);
                else levelMusic.setPosition(Math.max(0, camX / 320f));
                levelMusic.play();
            }
        } catch (Exception ex) {
            Gdx.app.error("Editor", "Music error: " + ex.getMessage());
        }
    }

    private void stopAndDisposeMusic() {
        if (levelMusic != null) {
            levelMusic.stop();
            levelMusic.dispose();
            levelMusic = null;
        }
    }
}
