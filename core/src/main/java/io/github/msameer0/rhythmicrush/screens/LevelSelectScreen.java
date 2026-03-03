package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.screens.util.Level;

public class LevelSelectScreen implements Screen {
    private final RhythmicRushGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private ArrayList<Level> levels;
    private int selectedLevel = 0;

    private BitmapFont font;

    // Texture atlases
    private TextureAtlas levelAtlas, arrowAtlas;
    private TextureRegion backButton, leftArrow, rightArrow;
    private TextureRegion[] difficultyTextures;

    // scaled positions/sizes
    private float backX, backY, backW, backH;
    private float leftX, leftY, leftW, leftH;
    private float rightX, rightY, rightW, rightH;
    private float diffX, diffY, diffW, diffH;

    public LevelSelectScreen(RhythmicRushGame game) {
        this.game = game;
        this.batch = game.getBatch();

        //placeholder levels
        levels = new ArrayList<>();
        levels.add(new Level("Level 1", "Easy", 0));
        levels.add(new Level("Level 2", "Medium", 0));
        levels.add(new Level("Level 3", "Hard", 0));
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 480, camera);
        viewport.apply();

        font = new BitmapFont();

        // Only load one atlas
        levelAtlas = new TextureAtlas("level_select_atlases/level_select.atlas");

        // get textures from the single atlas
        backButton = levelAtlas.findRegion("back");
        leftArrow = levelAtlas.findRegion("left_arrow");
        rightArrow = levelAtlas.findRegion("right_arrow");

        difficultyTextures = new TextureRegion[]{
            levelAtlas.findRegion("1_diff"),
            levelAtlas.findRegion("2_diff"),
            levelAtlas.findRegion("3_diff"),
            levelAtlas.findRegion("4_diff")
        };

        updateScaledSizes();
    }

    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        //back button top left corner
        backW = vw * 0.08f;
        backH = backW;
        backX = 10;
        backY = vh - backH - 10;

        //arrows
        leftW = vw * 0.08f;
        leftH = leftW;
        leftX = 50;
        leftY = vh / 2f - leftH / 2f;

        rightW = vw * 0.08f;
        rightH = rightW;
        rightX = vw - rightW - 50;
        rightY = vh / 2f - rightH / 2f;

        //difficulty image
        diffW = vw * 0.15f;
        diffH = diffW;
        diffX = vw / 2f - diffW / 2f;
        diffY = vh / 2f - diffH / 2f + 50; // slightly above center for name/progress
    }

    @Override
    public void render(float delta) {
        //clear screen
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Level current = levels.get(selectedLevel);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        //draw buttons & difficulty
        batch.draw(backButton, backX, backY, backW, backH);
        batch.draw(leftArrow, leftX, leftY, leftW, leftH);
        batch.draw(rightArrow, rightX, rightY, rightW, rightH);

        //difficulty texture
        int diffIndex = Math.min(3, selectedLevel); // match levels 0..3
        batch.draw(difficultyTextures[diffIndex], diffX, diffY, diffW, diffH);

        //text: level name and progress
        font.draw(batch, current.name, viewport.getWorldWidth() / 2f - 30, diffY - 20);
        font.draw(batch, "Progress: " + current.progress + "%", viewport.getWorldWidth() / 2f - 40, diffY - 50);

        batch.end();

        handleInput();
    }

    private void handleInput() {
        boolean justTouched = Gdx.input.justTouched();
        int mouseX = Gdx.input.getX();
        int mouseY = (int) (viewport.getWorldHeight() - Gdx.input.getY());

        //keyboard input
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            selectedLevel = (selectedLevel - 1 + levels.size()) % levels.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            selectedLevel = (selectedLevel + 1) % levels.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new io.github.msameer0.rhythmicrush.screens.MainMenuScreen(game));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            //TODO: enter level
        }

        if (Gdx.input.justTouched()) {
            Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touch); //converts to viewport world coords
            float x = touch.x;
            float y = touch.y;

            //left arrow
            if (x >= leftX && x <= leftX + leftW &&
                y >= leftY && y <= leftY + leftH) {
                selectedLevel = (selectedLevel - 1 + levels.size()) % levels.size();
            }

            //right arrow
            if (x >= rightX && x <= rightX + rightW &&
                y >= rightY && y <= rightY + rightH) {
                selectedLevel = (selectedLevel + 1) % levels.size();
            }

            //back button
            if (x >= backX && x <= backX + backW &&
                y >= backY && y <= backY + backH) {
                game.setScreen(new MainMenuScreen(game));
            }

            // TODO: handle pressing the level itself
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateScaledSizes();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        font.dispose();
        levelAtlas.dispose();
        arrowAtlas.dispose();
    }
}
