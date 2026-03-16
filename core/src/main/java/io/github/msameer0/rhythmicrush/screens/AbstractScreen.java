package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.window.WindowController;

public abstract class AbstractScreen implements Screen {

    protected final RhythmicRushGame game;
    protected OrthographicCamera camera;
    protected Viewport viewport;

    public AbstractScreen(RhythmicRushGame game) {
        this.game = game;
        camera   = new OrthographicCamera();
        viewport = new ExtendViewport(800, 480, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        viewport.apply();
    }

    @Override
    public void render(float delta) {
        handleWindowKeys();
        update(delta);
        draw();
    }

    private void handleWindowKeys() {
        WindowController wc = game.getWindowController();
        if (wc == null) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) wc.toggleFullscreen();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F10)) wc.maximizeWindow();
    }

    protected abstract void update(float delta);
    protected abstract void draw();

    @Override
    public void show() {
        game.getSoundManager().playMenuMusic();
    }

    @Override
    public void resize(int width, int height) {
        WindowController wc = game.getWindowController();
        if (wc != null) {
            wc.enforceAspectRatio(width, height);
            width  = Gdx.graphics.getWidth();
            height = Gdx.graphics.getHeight();
        }
        viewport.update(width, height, true);
    }

    @Override public void hide()    {}
    @Override public void pause()   {}
    @Override public void resume()  {}
    @Override public void dispose() {}
}
