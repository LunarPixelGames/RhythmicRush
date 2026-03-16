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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;

import io.github.msameer0.rhythmicrush.FontManager;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelProgress;
import io.github.msameer0.rhythmicrush.game.level.LevelSerializer;
import io.github.msameer0.rhythmicrush.ui.AnimatedButton;

public class LevelSelectScreen extends AbstractScreen {

    private static final float PANEL_CORNER_RADIUS = 40f;

    private static class LevelEntry {
        final LevelData data;
        final int       index;
        LevelEntry(LevelData data, int index) { this.data = data; this.index = index; }
    }

    private ArrayList<LevelEntry> levels = new ArrayList<>();
    private int selectedLevel = 0;

    // Font from shared FontManager — NOT disposed here
    private BitmapFont  font;
    // Single reusable GlyphLayout — set text each frame, no allocations
    private final GlyphLayout layout = new GlyphLayout();

    private TextureRegion   backButton, leftArrow, rightArrow;
    private TextureRegion[] difficultyTextures;

    private AnimatedButton btnBack, btnLeft, btnRight, btnPanel;

    private Texture panelTexture;
    private int lastPanelW = -1, lastPanelH = -1;

    private float panelX, panelY, panelW, panelH;

    public LevelSelectScreen(RhythmicRushGame game) {
        this(game, 0);
    }

    public LevelSelectScreen(RhythmicRushGame game, int initialIndex) {
        super(game);
        this.selectedLevel = initialIndex;
    }

    @Override
    public void show() {
        super.show();

        // Use shared font — zero allocation, instant
        font = game.getFontManager().get(FontManager.SIZE_XLARGE);

        TextureAtlas atlas = game.getAtlasManager().getLevelSelectAtlas();
        backButton  = atlas.findRegion("back");
        leftArrow   = atlas.findRegion("left_arrow");
        rightArrow  = atlas.findRegion("right_arrow");

        difficultyTextures = new TextureRegion[]{
            atlas.findRegion("1_diff"),
            atlas.findRegion("2_diff"),
            atlas.findRegion("3_diff"),
            atlas.findRegion("4_diff"),
            atlas.findRegion("5_diff"),
        };

        loadLevels();
        btnBack  = new AnimatedButton(backButton,  0, 0, 0, 0, () -> game.setScreen(new MainMenuScreen(game)));
        btnLeft  = new AnimatedButton(leftArrow,   0, 0, 0, 0, () -> navigate(-1));
        btnRight = new AnimatedButton(rightArrow,  0, 0, 0, 0, () -> navigate(1));
        btnPanel = new AnimatedButton(null,        0, 0, 0, 0, this::playSelected);
        updateScaledSizes();
    }

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
            LevelData placeholder = new LevelData();
            placeholder.name       = "No Levels Found";
            placeholder.difficulty = "normal";
            levels.add(new LevelEntry(placeholder, -1));
        }
        // Clamp to valid range — preserves the initial index passed from GameScreen
        selectedLevel = Math.max(0, Math.min(selectedLevel, levels.size() - 1));
    }

    private int difficultyIndex(String difficulty) {
        if (difficulty == null) return 1;
        switch (difficulty.toLowerCase()) {
            case "easy":    return 0;
            case "normal":  return 1;
            case "hard":    return 2;
            case "insane":  return 3;
            case "extreme": return 4;
            default:        return 1;
        }
    }

    private TextureRegion difficultyTexture(String difficulty) {
        int idx = difficultyIndex(difficulty);
        TextureRegion r = difficultyTextures[idx];
        return r != null ? r : difficultyTextures[1];
    }

    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        float backW = vw * 0.08f, backH = backW;
        float leftW = vw * 0.08f, leftH = leftW;
        float rightW = vw * 0.08f, rightH = rightW;

        if (btnBack  != null) btnBack.setBounds(10, vh - backH - 10, backW, backH);
        if (btnLeft  != null) btnLeft.setBounds(10, vh / 2f - leftH / 2f, leftW, leftH);
        if (btnRight != null) btnRight.setBounds(vw - rightW - 10, vh / 2f - rightH / 2f, rightW, rightH);

        panelW = vw * 0.6f; panelH = vh * 0.28f;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f + 40;
        if (btnPanel != null) btnPanel.setBounds(panelX, panelY, panelW, panelH);
    }

    @Override
    protected void update(float delta) {
        btnBack.update(delta);
        btnLeft.update(delta);
        btnRight.update(delta);
        btnPanel.update(delta);
        handleInput();
    }

    @Override
    protected void draw() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        LevelData current = levels.get(selectedLevel).data;

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();

        int texW = (int) panelW, texH = (int) panelH;
        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();
            panelTexture = createRoundedRect(texW, texH,
                (int)(PANEL_CORNER_RADIUS * (panelW / 800f)),
                new Color(0.2f, 0.2f, 0.28f, 1f));
            panelTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            lastPanelW = texW; lastPanelH = texH;
        }

        game.getBatch().draw(panelTexture, panelX, panelY);
        btnBack.draw(game.getBatch());
        btnLeft.draw(game.getBatch());
        btnRight.draw(game.getBatch());

        TextureRegion diffRegion = difficultyTexture(current.difficulty);
        float iconSize = panelH * 0.55f;
        float spacing  = panelW * 0.05f;

        font.getData().setScale(0.85f);
        layout.setText(font, current.name);
        float nameW = layout.width, nameH = layout.height;
        float totalWidth = iconSize + spacing + nameW;
        float startX     = panelX + (panelW - totalWidth) / 2f;
        float iconX      = startX;
        float iconY      = panelY + panelH / 2f - iconSize / 2f + 10f;
        float textX      = iconX + iconSize + spacing;
        float textY      = panelY + panelH / 2f + nameH / 2f + 10f;

        game.getBatch().draw(diffRegion, iconX, iconY, iconSize, iconSize);
        font.draw(game.getBatch(), current.name, textX, textY);

        // Difficulty label
        font.getData().setScale(0.38f);
        String diffLabel = current.difficulty != null
            ? current.difficulty.substring(0,1).toUpperCase() + current.difficulty.substring(1)
            : "Normal";
        layout.setText(font, diffLabel);
        font.setColor(new Color(1f, 1f, 1f, 0.55f));
        font.draw(game.getBatch(), diffLabel, textX, textY - nameH - 4f);
        font.setColor(Color.WHITE);

        // Stats below panel
        String levelKey = levels.get(selectedLevel).index + ".json";
        LevelProgress progress = game.getProgressManager().getOrCreate(levelKey);

        font.getData().setScale(0.42f);
        String bestText     = "Best: " + progress.bestPercent + "%";
        String attemptsText = "Total Attempts: " + progress.totalAttempts;
        float statsX = panelX + panelW / 2f;

        layout.setText(font, bestText);
        font.setColor(new Color(1f, 1f, 1f, 0.8f));
        font.draw(game.getBatch(), bestText, statsX - layout.width / 2f, panelY - 18f);

        layout.setText(font, attemptsText);
        font.setColor(new Color(1f, 1f, 1f, 0.55f));
        font.draw(game.getBatch(), attemptsText, statsX - layout.width / 2f, panelY - 44f);

        // Level counter
        font.getData().setScale(0.35f);
        font.setColor(new Color(1f, 1f, 1f, 0.4f));
        String counter = (selectedLevel + 1) + " / " + levels.size();
        layout.setText(font, counter);
        font.draw(game.getBatch(), counter,
            viewport.getWorldWidth() / 2f - layout.width / 2f, 22f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        game.getBatch().end();
    }

    private void handleInput() {
        Vector2 t = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

        if (Gdx.input.justTouched()) {
            btnBack.onTouchDown(t.x, t.y);
            btnLeft.onTouchDown(t.x, t.y);
            btnRight.onTouchDown(t.x, t.y);
            btnPanel.onTouchDown(t.x, t.y);
        }
        if (!Gdx.input.isTouched()) {
            btnBack.onTouchUp(t.x, t.y);
            btnLeft.onTouchUp(t.x, t.y);
            btnRight.onTouchUp(t.x, t.y);
            btnPanel.onTouchUp(t.x, t.y);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))   navigate(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))  navigate(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))  playSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))  playSelected();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MainMenuScreen(game));
    }

    private void navigate(int dir) {
        selectedLevel = (selectedLevel + dir + levels.size()) % levels.size();
    }

    private void playSelected() {
        LevelEntry entry = levels.get(selectedLevel);
        if (entry.index < 0) return;
        game.setScreen(new GameScreen(game, entry.data, selectedLevel));
    }

    private Texture createRoundedRect(int w, int h, int r, Color color) {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0); pixmap.fill();
        pixmap.setColor(color);
        pixmap.fillRectangle(r, 0, w - 2*r, h);
        pixmap.fillRectangle(0, r, w, h - 2*r);
        pixmap.fillCircle(r, r, r); pixmap.fillCircle(w-r, r, r);
        pixmap.fillCircle(r, h-r, r); pixmap.fillCircle(w-r, h-r, r);
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateScaledSizes();
    }

    @Override
    public void dispose() {
        if (panelTexture != null) panelTexture.dispose();
        // font NOT disposed — owned by FontManager
        // atlas NOT disposed — owned by AtlasManager
    }
}
