package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelSerializer;

public class LevelSelectScreen extends AbstractScreen {

    private static final float PANEL_CORNER_RADIUS = 40f;

    // ── Level entries loaded from assets/levels/ ──────────────────────────────
    private static class LevelEntry {
        final LevelData data;
        final int       index;
        LevelEntry(LevelData data, int index) {
            this.data  = data;
            this.index = index;
        }
    }

    private ArrayList<LevelEntry> levels = new ArrayList<>();
    private int selectedLevel = 0;

    // ── UI ────────────────────────────────────────────────────────────────────
    private BitmapFont   font;
    private GlyphLayout  layout;

    private TextureAtlas  levelAtlas;
    private TextureRegion backButton, leftArrow, rightArrow;

    /**
     * Difficulty textures indexed 0–4:
     *   0 = easy      (1_diff)
     *   1 = normal    (2_diff)
     *   2 = hard      (3_diff)
     *   3 = insane    (4_diff)
     *   4 = extreme   (5_diff) — may be null if texture not yet added
     */
    private TextureRegion[] difficultyTextures;

    private Texture panelTexture;
    private int lastPanelW = -1, lastPanelH = -1;

    private float backX, backY, backW, backH;
    private float leftX, leftY, leftW, leftH;
    private float rightX, rightY, rightW, rightH;
    private float panelX, panelY, panelW, panelH;

    // ─────────────────────────────────────────────────────────────────────────

    public LevelSelectScreen(RhythmicRushGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();

        layout = new GlyphLayout();

        // load atlas
        levelAtlas     = new TextureAtlas("level_select_atlases/level_select.atlas");
        backButton     = levelAtlas.findRegion("back");
        leftArrow      = levelAtlas.findRegion("left_arrow");
        rightArrow     = levelAtlas.findRegion("right_arrow");

        // difficulty textures — 5_diff may be null if not yet added
        difficultyTextures = new TextureRegion[] {
            levelAtlas.findRegion("1_diff"),
            levelAtlas.findRegion("2_diff"),
            levelAtlas.findRegion("3_diff"),
            levelAtlas.findRegion("4_diff"),
            levelAtlas.findRegion("5_diff"),  // may be null — handled in draw
        };

        // load font
        try {
            FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(Gdx.files.internal("fonts/zendots-regular.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter p =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size      = 48;
            p.magFilter = Texture.TextureFilter.Linear;
            p.minFilter = Texture.TextureFilter.Linear;
            font = generator.generateFont(p);
            generator.dispose();
        } catch (Exception e) {
            font = new BitmapFont();
        }

        loadLevels();
        updateScaledSizes();
    }

    // ── Dynamic level scanning ────────────────────────────────────────────────

    /**
     * Scans assets/levels/ for 0.json, 1.json, 2.json … stopping at the first
     * missing index. Levels are ordered by their filename index.
     */
    private void loadLevels() {
        levels.clear();
        int index = 0;
        while (true) {
            FileHandle fh = Gdx.files.internal("levels/" + index + ".json");
            if (!fh.exists()) break;
            try {
                LevelData data = LevelSerializer.load(fh);
                levels.add(new LevelEntry(data, index));
                index++;
            } catch (Exception e) {
                Gdx.app.error("LevelSelect", "Failed to load level " + index + ": " + e.getMessage());
                break;
            }
        }

        if (levels.isEmpty()) {
            // fallback: show a placeholder so the screen doesn't crash
            LevelData placeholder = new LevelData();
            placeholder.name       = "No Levels Found";
            placeholder.difficulty = "normal";
            levels.add(new LevelEntry(placeholder, -1));
        }

        selectedLevel = 0;
    }

    // ── Difficulty helpers ────────────────────────────────────────────────────

    /** Maps difficulty string → texture index (0–4). */
    private int difficultyIndex(String difficulty) {
        if (difficulty == null) return 1; // default to normal
        switch (difficulty.toLowerCase()) {
            case "easy":    return 0;
            case "normal":  return 1;
            case "hard":    return 2;
            case "insane":  return 3;
            case "extreme": return 4;
            default:        return 1;
        }
    }

    /** Returns the difficulty texture, falling back to normal if null. */
    private TextureRegion difficultyTexture(String difficulty) {
        int idx = difficultyIndex(difficulty);
        TextureRegion region = difficultyTextures[idx];
        if (region == null) {
            // fallback to normal (index 1) if texture not yet added
            region = difficultyTextures[1];
        }
        return region;
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        backW = vw * 0.08f; backH = backW;
        backX = 10; backY = vh - backH - 10;

        leftW = vw * 0.08f; leftH = leftW;
        leftX = 10; leftY = vh / 2f - leftH / 2f;

        rightW = vw * 0.08f; rightH = rightW;
        rightX = vw - rightW - 10; rightY = vh / 2f - rightH / 2f;

        panelW = vw * 0.6f; panelH = vh * 0.28f;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f + 40;
    }

    // ── Update / Draw ─────────────────────────────────────────────────────────

    @Override
    protected void update(float delta) {
        handleInput();
    }

    @Override
    protected void draw() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        LevelData current = levels.get(selectedLevel).data;

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();

        // rebuild panel texture if size changed
        int texW = (int) panelW, texH = (int) panelH;
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();
            panelTexture = createRoundedRectangleTexture(
                texW, texH,
                (int)(PANEL_CORNER_RADIUS * (panelW / 800f)),
                new Color(0.2f, 0.2f, 0.28f, 1f)
            );
            panelTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            lastPanelW = texW; lastPanelH = texH;
        }

        // nav buttons + panel
        game.getBatch().draw(backButton,  backX,  backY,  backW,  backH);
        game.getBatch().draw(leftArrow,   leftX,  leftY,  leftW,  leftH);
        game.getBatch().draw(rightArrow,  rightX, rightY, rightW, rightH);
        game.getBatch().draw(panelTexture, panelX, panelY);

        // difficulty icon + level name
        TextureRegion diffRegion = difficultyTexture(current.difficulty);
        float iconSize  = panelH * 0.55f;
        float spacing   = panelW * 0.05f;

        font.getData().setScale(0.85f);
        GlyphLayout nameLayout = new GlyphLayout(font, current.name);
        float totalWidth = iconSize + spacing + nameLayout.width;
        float startX     = panelX + (panelW - totalWidth) / 2f;
        float iconX      = startX;
        float iconY      = panelY + panelH / 2f - iconSize / 2f;
        float textX      = iconX + iconSize + spacing;
        float textY      = panelY + panelH / 2f + nameLayout.height / 2f;

        game.getBatch().draw(diffRegion, iconX, iconY, iconSize, iconSize);
        font.draw(game.getBatch(), current.name, textX, textY);

        // difficulty label below name
        font.getData().setScale(0.55f);
        String diffLabel = current.difficulty != null
            ? current.difficulty.substring(0,1).toUpperCase() + current.difficulty.substring(1)
            : "Normal";
        GlyphLayout diffLayout = new GlyphLayout(font, diffLabel);
        font.draw(game.getBatch(), diffLabel,
            panelX + panelW / 2f - diffLayout.width / 2f,
            panelY + panelH * 0.3f);

        // level counter  e.g. "1 / 3"
        font.getData().setScale(0.5f);
        String counter = (selectedLevel + 1) + " / " + levels.size();
        GlyphLayout counterLayout = new GlyphLayout(font, counter);
        font.draw(game.getBatch(), counter,
            viewport.getWorldWidth() / 2f - counterLayout.width / 2f,
            panelY - 25);

        font.getData().setScale(1f);
        game.getBatch().end();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleInput() {
        if (Gdx.input.justTouched()) {
            Vector2 touch = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            float x = touch.x, y = touch.y;

            if (x >= leftX && x <= leftX + leftW && y >= leftY && y <= leftY + leftH)
                navigate(-1);
            if (x >= rightX && x <= rightX + rightW && y >= rightY && y <= rightY + rightH)
                navigate(1);
            if (x >= backX && x <= backX + backW && y >= backY && y <= backY + backH)
                game.setScreen(new MainMenuScreen(game));

            // tap panel to play
            if (x >= panelX && x <= panelX + panelW && y >= panelY && y <= panelY + panelH)
                playSelected();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))   navigate(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))  navigate(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))  playSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MainMenuScreen(game));
    }

    private void navigate(int dir) {
        selectedLevel = (selectedLevel + dir + levels.size()) % levels.size();
    }

    private void playSelected() {
        LevelEntry entry = levels.get(selectedLevel);
        if (entry.index < 0) return; // placeholder, no real level to play
        game.setScreen(new GameScreen(game, entry.data));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Texture createRoundedRectangleTexture(int width, int height, int radius, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setColor(color);
        pixmap.fillRectangle(radius, 0, width - 2 * radius, height);
        pixmap.fillRectangle(0, radius, width, height - 2 * radius);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(width - radius, radius, radius);
        pixmap.fillCircle(radius, height - radius, radius);
        pixmap.fillCircle(width - radius, height - radius, radius);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateScaledSizes();
    }

    @Override
    public void dispose() {
        font.dispose();
        levelAtlas.dispose();
        if (panelTexture != null) panelTexture.dispose();
    }
}
