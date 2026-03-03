package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Random;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;

public class MainMenuScreen implements Screen {
    private final RhythmicRushGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private TextureAtlas atlas;
    private TextureRegion title, startButton, settingsButton;

    private Color bgColor;

    //scaled positions and sizes
    private float titleX, titleY, titleW, titleH;
    private float startX, startY, startW, startH;
    private float settingsX, settingsY, settingsW, settingsH;

    public MainMenuScreen(RhythmicRushGame game) {
        this.game = game;
        this.batch = game.getBatch();
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 480, camera);
        viewport.apply();

        //load atlas
        atlas = new TextureAtlas("menu.atlas");
        title = atlas.findRegion("title");
        startButton = atlas.findRegion("start_button");
        settingsButton = atlas.findRegion("settings_button");

        //random background color
        Random rand = new Random();
        bgColor = new Color(0.2f + 0.6f*rand.nextFloat(),
            0.2f + 0.6f*rand.nextFloat(),
            0.2f + 0.6f*rand.nextFloat(), 1);

        //initialize scaled sizes/positions
        updateScaledSizes();
    }

    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        //title scale to 90% of screen width, then multiply by 0.675, move slightly up
        float maxTitleWidth = vw * 0.9f;
        float titleScale = (maxTitleWidth / title.getRegionWidth()) * 0.675f;
        titleW = title.getRegionWidth() * titleScale;
        titleH = title.getRegionHeight() * titleScale;
        titleX = vw / 2f - titleW / 2f;
        titleY = vh - titleH - 20 + 30; // move 30 pixels upward

        //start button 25% of screen width, 0.75 scale
        float maxStartW = vw * 0.25f * 0.75f;
        float startScale = maxStartW / startButton.getRegionWidth();
        startW = startButton.getRegionWidth() * startScale;
        startH = startButton.getRegionHeight() * startScale;
        startX = vw / 2f - startW / 2f;
        startY = vh / 2f - startH / 2f;

        //settings button 10% of screen width, 0.85 scale, nudged slightly down
        float maxSettingsW = vw * 0.1f * 0.85f;
        float settingsScale = maxSettingsW / settingsButton.getRegionWidth();
        settingsW = settingsButton.getRegionWidth() * settingsScale;
        settingsH = settingsButton.getRegionHeight() * settingsScale;
        settingsX = 20;
        settingsY = 20 - 10; // nudge 10 pixels down
    }

    @Override
    public void render(float delta) {
        //clear screen with random bg
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        //draw scaled textures
        batch.draw(title, titleX, titleY, titleW, titleH);
        batch.draw(startButton, startX, startY, startW, startH);
        batch.draw(settingsButton, settingsX, settingsY, settingsW, settingsH);

        batch.end();

        handleInput();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new LevelSelectScreen(game));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        if (!Gdx.input.justTouched()) return;

        Vector2 touch = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(touch);

        float x = touch.x;
        float y = touch.y;

        if (x >= startX && x <= startX + startW &&
            y >= startY && y <= startY + startH) {
            game.setScreen(new LevelSelectScreen(game));
        }

        if (x >= settingsX && x <= settingsX + settingsW &&
            y >= settingsY && y <= settingsY + settingsH) {
            // settings
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateScaledSizes(); //recalc positions on resize
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        atlas.dispose();
    }
}
