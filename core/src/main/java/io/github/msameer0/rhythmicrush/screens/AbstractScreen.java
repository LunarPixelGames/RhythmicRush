package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;

public abstract class AbstractScreen implements Screen {

    protected final RhythmicRushGame game;
    protected OrthographicCamera camera;
    protected Viewport viewport;

    public AbstractScreen(RhythmicRushGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(800, 480, camera);

        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        viewport.apply();
    }

    @Override
    public void render(float delta) {
        //clear screen (shared by all screens)
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        update(delta);
        draw();
    }

    protected abstract void update(float delta);
    protected abstract void draw();

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void show() {
        game.getSoundManager().playMenuMusic();
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {}
}
