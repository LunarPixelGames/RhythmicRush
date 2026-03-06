package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;

public class MainMenuScreen extends AbstractScreen {

    private TextureRegion title, startButton, settingsButton;
    private Color bgColor;

    private float titleX, titleY, titleW, titleH;
    private float startX, startY, startW, startH;
    private float settingsX, settingsY, settingsW, settingsH;

    public MainMenuScreen(RhythmicRushGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show(); // starts menu music

        // pull regions from shared atlas — no load, no dispose here
        title          = game.getAtlasManager().getMenuAtlas().findRegion("title");
        startButton    = game.getAtlasManager().getMenuAtlas().findRegion("start_button");
        settingsButton = game.getAtlasManager().getMenuAtlas().findRegion("settings_button");

        Random rand = new Random();
        bgColor = new Color(
            0.2f + 0.6f * rand.nextFloat(),
            0.2f + 0.6f * rand.nextFloat(),
            0.2f + 0.6f * rand.nextFloat(), 1f);

        updateScaledSizes();
    }

    private void updateScaledSizes() {
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        float maxTitleWidth = vw * 0.9f;
        float titleScale    = (maxTitleWidth / title.getRegionWidth()) * 0.675f;
        titleW = title.getRegionWidth()  * titleScale;
        titleH = title.getRegionHeight() * titleScale;
        titleX = vw / 2f - titleW / 2f;
        titleY = vh - titleH - 20 + 30;

        float maxStartW  = vw * 0.25f * 0.75f;
        float startScale = maxStartW / startButton.getRegionWidth();
        startW = startButton.getRegionWidth()  * startScale;
        startH = startButton.getRegionHeight() * startScale;
        startX = vw / 2f - startW / 2f;
        startY = vh / 2f - startH / 2f;

        float maxSettingsW  = vw * 0.1f * 0.85f;
        float settingsScale = maxSettingsW / settingsButton.getRegionWidth();
        settingsW = settingsButton.getRegionWidth()  * settingsScale;
        settingsH = settingsButton.getRegionHeight() * settingsScale;
        settingsX = 20;
        settingsY = 20 - 10;
    }

    @Override
    protected void update(float delta) {
        handleInput();
    }

    @Override
    protected void draw() {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1f);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        game.getBatch().draw(title,          titleX,    titleY,    titleW,    titleH);
        game.getBatch().draw(startButton,    startX,    startY,    startW,    startH);
        game.getBatch().draw(settingsButton, settingsX, settingsY, settingsW, settingsH);
        game.getBatch().end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))  game.setScreen(new LevelSelectScreen(game));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0))  game.setScreen(new GameScreen(game));

        if (!Gdx.input.justTouched()) return;
        Vector2 touch = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(touch);
        float x = touch.x, y = touch.y;

        if (x >= startX && x <= startX + startW && y >= startY && y <= startY + startH)
            game.setScreen(new LevelSelectScreen(game));
        if (x >= settingsX && x <= settingsX + settingsW && y >= settingsY && y <= settingsY + settingsH) {
            // TODO: settings
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateScaledSizes();
    }

    @Override
    public void dispose() {
        // nothing — atlas owned by AtlasManager
    }
}
