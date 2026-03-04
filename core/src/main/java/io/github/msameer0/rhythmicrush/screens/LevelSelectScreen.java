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

import java.util.ArrayList;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.screens.util.Level;

public class LevelSelectScreen extends AbstractScreen {

    private static final float PANEL_CORNER_RADIUS = 40f;

    private ArrayList<Level> levels;
    private int selectedLevel = 0;

    private BitmapFont font;
    private GlyphLayout layout;

    private TextureAtlas levelAtlas;
    private TextureRegion backButton, leftArrow, rightArrow;
    private TextureRegion[] difficultyTextures;

    private Texture panelTexture;
    private int lastPanelW = -1;
    private int lastPanelH = -1;

    private float backX, backY, backW, backH;
    private float leftX, leftY, leftW, leftH;
    private float rightX, rightY, rightW, rightH;

    private float panelX, panelY, panelW, panelH;

    public LevelSelectScreen(RhythmicRushGame game) {
        super(game);

        levels = new ArrayList<>();
        levels.add(new Level("Level 1", "Easy", 0));
        levels.add(new Level("Level 2", "Medium", 0));
        levels.add(new Level("Level 3", "Hard", 0));
    }

    @Override
    public void show() {
        layout = new GlyphLayout();

        //load atlas
        levelAtlas = new TextureAtlas("level_select_atlases/level_select.atlas");
        backButton = levelAtlas.findRegion("back");
        leftArrow = levelAtlas.findRegion("left_arrow");
        rightArrow = levelAtlas.findRegion("right_arrow");

        difficultyTextures = new TextureRegion[]{
            levelAtlas.findRegion("1_diff"),
            levelAtlas.findRegion("2_diff"),
            levelAtlas.findRegion("3_diff"),
            levelAtlas.findRegion("4_diff")
        };

        try {
            //try loading original font
            FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(Gdx.files.internal("fonts/zendots-regular.ttf"));

            FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 48; //adjust as needed
            parameter.magFilter = Texture.TextureFilter.Linear;
            parameter.minFilter = Texture.TextureFilter.Linear;

            font = generator.generateFont(parameter);
            generator.dispose();
        } catch (Exception e) {
            //if something goes wrong, fallback to default font
            font = new BitmapFont();
        }

        updateScaledSizes();
    }

    private Texture createRoundedRectangleTexture(int width, int height, int radius, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        pixmap.setColor(color);

        //center rectangles
        pixmap.fillRectangle(radius, 0, width - 2 * radius, height);
        pixmap.fillRectangle(0, radius, width, height - 2 * radius);

        //corners
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(width - radius, radius, radius);
        pixmap.fillCircle(radius, height - radius, radius);
        pixmap.fillCircle(width - radius, height - radius, radius);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        backW = vw * 0.08f;
        backH = backW;
        backX = 10;
        backY = vh - backH - 10;

        leftW = vw * 0.08f;
        leftH = leftW;
        leftX = 10;
        leftY = vh / 2f - leftH / 2f;

        rightW = vw * 0.08f;
        rightH = rightW;
        rightX = vw - rightW - 10;
        rightY = vh / 2f - rightH / 2f;

        panelW = vw * 0.6f;
        panelH = vh * 0.28f;
        panelX = vw / 2f - panelW / 2f;
        panelY = vh / 2f - panelH / 2f + 40;
    }

    @Override
    protected void update(float delta) {
        handleInput();
    }

    @Override
    protected void draw() {
        //clear screen
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        Level current = levels.get(selectedLevel);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();

        //panel
        int texW = (int) panelW;
        int texH = (int) panelH;

        if (panelTexture == null || texW != lastPanelW || texH != lastPanelH) {
            if (panelTexture != null) panelTexture.dispose();

            panelTexture = createRoundedRectangleTexture(
                texW,
                texH,
                (int) (PANEL_CORNER_RADIUS * (panelW / 800f)),
                new Color(0.2f, 0.2f, 0.28f, 1f)
            );

            panelTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            lastPanelW = texW;
            lastPanelH = texH;
        }

        //draw nav buttons
        game.getBatch().draw(backButton, backX, backY, backW, backH);
        game.getBatch().draw(leftArrow, leftX, leftY, leftW, leftH);
        game.getBatch().draw(rightArrow, rightX, rightY, rightW, rightH);

        //draw panel
        game.getBatch().draw(panelTexture, panelX, panelY);

        //draw icon and level name
        int diffIndex = Math.min(difficultyTextures.length - 1, selectedLevel);
        TextureRegion diffRegion = difficultyTextures[diffIndex];

        float iconSize = panelH * 0.55f;
        float spacing = panelW * 0.05f;

        font.getData().setScale(0.85f);
        GlyphLayout layout = new GlyphLayout(font, current.name);

        float totalWidth = iconSize + spacing + layout.width;
        float startX = panelX + (panelW - totalWidth) / 2f;

        float iconX = startX;
        float iconY = panelY + panelH / 2f - iconSize / 2f;

        float textX = iconX + iconSize + spacing;
        float textY = panelY + panelH / 2f + layout.height / 2f;

        game.getBatch().draw(diffRegion, iconX, iconY, iconSize, iconSize);
        font.draw(game.getBatch(), current.name, textX, textY);

        //progress text
        String progressText = "Progress: " + current.progress + "%";
        font.getData().setScale(0.575f);
        layout.setText(font, progressText);
        font.draw(game.getBatch(), progressText, viewport.getWorldWidth() / 2f - layout.width / 2f, panelY - 25);
        font.getData().setScale(1f);

        game.getBatch().end();
    }

    private void handleInput() {
        if (Gdx.input.justTouched()) {
            Vector2 touch = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            float x = touch.x;
            float y = touch.y;

            if (x >= leftX && x <= leftX + leftW &&
                y >= leftY && y <= leftY + leftH) {
                selectedLevel = (selectedLevel - 1 + levels.size()) % levels.size();
            }

            if (x >= rightX && x <= rightX + rightW &&
                y >= rightY && y <= rightY + rightH) {
                selectedLevel = (selectedLevel + 1) % levels.size();
            }

            if (x >= backX && x <= backX + backW &&
                y >= backY && y <= backY + backH) {
                game.setScreen(new MainMenuScreen(game));
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            selectedLevel = (selectedLevel - 1 + levels.size()) % levels.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            selectedLevel = (selectedLevel + 1) % levels.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

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
