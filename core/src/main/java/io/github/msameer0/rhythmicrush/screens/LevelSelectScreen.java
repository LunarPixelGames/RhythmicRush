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

    private static class LevelEntry {
        final LevelData data;
        final int       index;
        LevelEntry(LevelData data, int index) { this.data = data; this.index = index; }
    }

    private ArrayList<LevelEntry> levels = new ArrayList<>();
    private int selectedLevel = 0;

    private BitmapFont  font;
    private GlyphLayout layout;

    private TextureRegion   backButton, leftArrow, rightArrow;
    private TextureRegion[] difficultyTextures;

    private Texture panelTexture;
    private int lastPanelW = -1, lastPanelH = -1;

    private float backX, backY, backW, backH;
    private float leftX, leftY, leftW, leftH;
    private float rightX, rightY, rightW, rightH;
    private float panelX, panelY, panelW, panelH;

    public LevelSelectScreen(RhythmicRushGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();

        layout = new GlyphLayout();

        // pull regions from shared atlas — no load/dispose here
        TextureAtlas atlas = game.getAtlasManager().getLevelSelectAtlas();
        backButton  = atlas.findRegion("back");
        leftArrow   = atlas.findRegion("left_arrow");
        rightArrow  = atlas.findRegion("right_arrow");

        difficultyTextures = new TextureRegion[]{
            atlas.findRegion("1_diff"),
            atlas.findRegion("2_diff"),
            atlas.findRegion("3_diff"),
            atlas.findRegion("4_diff"),
            atlas.findRegion("5_diff"), // may be null — handled gracefully
        };

        try {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/zendots-regular.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter p =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size      = 48;
            p.magFilter = Texture.TextureFilter.Linear;
            p.minFilter = Texture.TextureFilter.Linear;
            font = gen.generateFont(p);
            gen.dispose();
        } catch (Exception e) {
            font = new BitmapFont();
        }

        loadLevels();
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
        selectedLevel = 0;
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

        backW = vw * 0.08f; backH = backW; backX = 10; backY = vh - backH - 10;
        leftW = vw * 0.08f; leftH = leftW; leftX = 10; leftY = vh / 2f - leftH / 2f;
        rightW = vw * 0.08f; rightH = rightW;
        rightX = vw - rightW - 10; rightY = vh / 2f - rightH / 2f;
        panelW = vw * 0.6f; panelH = vh * 0.28f;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f + 40;
    }

    @Override
    protected void update(float delta) { handleInput(); }

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

        game.getBatch().draw(backButton,   backX,  backY,  backW,  backH);
        game.getBatch().draw(leftArrow,    leftX,  leftY,  leftW,  leftH);
        game.getBatch().draw(rightArrow,   rightX, rightY, rightW, rightH);
        game.getBatch().draw(panelTexture, panelX, panelY);

        TextureRegion diffRegion = difficultyTexture(current.difficulty);
        float iconSize = panelH * 0.55f;
        float spacing  = panelW * 0.05f;

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

        font.getData().setScale(0.55f);
        String diffLabel = current.difficulty != null
            ? current.difficulty.substring(0,1).toUpperCase() + current.difficulty.substring(1)
            : "Normal";
        GlyphLayout diffLayout = new GlyphLayout(font, diffLabel);
        font.draw(game.getBatch(), diffLabel,
            panelX + panelW / 2f - diffLayout.width / 2f, panelY + panelH * 0.3f);

        font.getData().setScale(0.5f);
        String counter = (selectedLevel + 1) + " / " + levels.size();
        GlyphLayout counterLayout = new GlyphLayout(font, counter);
        font.draw(game.getBatch(), counter,
            viewport.getWorldWidth() / 2f - counterLayout.width / 2f, panelY - 25);

        font.getData().setScale(1f);
        game.getBatch().end();
    }

    private void handleInput() {
        if (Gdx.input.justTouched()) {
            Vector2 touch = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            float x = touch.x, y = touch.y;
            if (x >= leftX  && x <= leftX  + leftW  && y >= leftY  && y <= leftY  + leftH)  navigate(-1);
            if (x >= rightX && x <= rightX + rightW && y >= rightY && y <= rightY + rightH) navigate(1);
            if (x >= backX  && x <= backX  + backW  && y >= backY  && y <= backY  + backH)
                game.setScreen(new MainMenuScreen(game));
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
        if (entry.index < 0) return;
        game.setScreen(new GameScreen(game, entry.data));
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
        font.dispose();
        if (panelTexture != null) panelTexture.dispose();
        // atlas NOT disposed — owned by AtlasManager
    }
}
