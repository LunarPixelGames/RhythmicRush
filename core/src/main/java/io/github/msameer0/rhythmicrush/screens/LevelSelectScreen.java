package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
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

    private ShapeRenderer shapeRenderer;

    private float backX, backY, backW, backH;

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
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 480, camera);
        viewport.apply();

        backW = 100;
        backH = 40;
        backX = 20;
        backY = viewport.getWorldHeight() - backH - 20;

        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Level current = levels.get(selectedLevel);

        //difficulty on left
        font.draw(batch, current.difficulty, 200, 300);

        //level name on right
        font.draw(batch, current.name, 400, 300);

        //progress below
        font.draw(batch, "Progress: " + current.progress + "%", 250, 200);

        batch.end();

        //draw placeholder arrows
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.rect(50, 240, 50, 50);
        shapeRenderer.rect(700, 240, 50, 50);
        shapeRenderer.end();

        //draw back button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(backX, backY, backW, backH);
        shapeRenderer.end();

        batch.begin();
        font.draw(batch, "BACK", backX + 20, backY + 25);
        batch.end();

        handleInput();
    }

    private void handleInput() {
        //keyboard support
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT)) {
            selectedLevel = (selectedLevel - 1 + levels.size()) % levels.size();
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            selectedLevel = (selectedLevel + 1) % levels.size();
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            return;
        }

        //mouse and touch inputs on buttons
        if (!Gdx.input.justTouched()) return;

        Vector2 touch = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(touch);

        float x = touch.x;
        float y = touch.y;

        //left arrow
        if (x >= 50 && x <= 100 && y >= 240 && y <= 290) {
            selectedLevel = (selectedLevel - 1 + levels.size()) % levels.size();
        }

        //right arrow
        if (x >= 700 && x <= 750 && y >= 240 && y <= 290) {
            selectedLevel = (selectedLevel + 1) % levels.size();
        }

        //back button
        if (x >= backX && x <= backX + backW &&
            y >= backY && y <= backY + backH) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}

    @Override
    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
    }
}
