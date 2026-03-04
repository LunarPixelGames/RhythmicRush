package io.github.msameer0.rhythmicrush.lwjgl3.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.badlogic.gdx.graphics.GL20;

import io.github.msameer0.rhythmicrush.game.level.LevelData;
import io.github.msameer0.rhythmicrush.game.level.LevelSerializer;

/**
 * Standalone launcher for the Level Editor.
 * Run this class directly from the lwjgl3 module — it is NOT part of the game.
 *
 * Pressing ENTER inside the editor opens a SECOND window that runs a playtest
 * using the same GameScreen used in the real game.
 */
public class LevelEditorApp extends ApplicationAdapter {

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Rhythmic Rush — Level Editor");
        config.setWindowedMode(1280, 720);
        config.setResizable(true);
        config.useVsync(true);

        System.setProperty("user.dir", System.getProperty("user.dir").replace("\\lwjgl3", ""));

        new Lwjgl3Application(new LevelEditorApp(), config);
    }

    // ── App lifecycle ─────────────────────────────────────────────────────────

    private LevelEditorScreen editorScreen;

    @Override
    public void create() {
        editorScreen = new LevelEditorScreen(this);
        editorScreen.show();
    }

    @Override
    public void render() {
        editorScreen.render(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void resize(int width, int height) {
        editorScreen.resize(width, height);
    }

    @Override
    public void dispose() {
        editorScreen.dispose();
    }

    // ── Playtest window ───────────────────────────────────────────────────────

    // in LevelEditorApp:
    private Lwjgl3Window playtestWindow = null;

    public void launchPlaytest(LevelData levelData) {
        // close previous playtest window if still open
        if (playtestWindow != null) {
            try { playtestWindow.closeWindow(); } catch (Exception ignored) {}
            playtestWindow = null;
        }

        Lwjgl3WindowConfiguration cfg = new Lwjgl3WindowConfiguration();
        cfg.setTitle("Playtest — " + levelData.name);
        cfg.setWindowedMode(1280, 720);

        // get the Lwjgl3Application instance to create a new window on it
        Lwjgl3Application lwjgl3App = (Lwjgl3Application) Gdx.app;

        playtestWindow = lwjgl3App.newWindow(new PlaytestListener(levelData), cfg);
    }

    // ── Inner playtest application ────────────────────────────────────────────

    private class PlaytestListener extends ApplicationAdapter {

        private final LevelData levelData;
        private io.github.msameer0.rhythmicrush.game.GameWorld world;
        private io.github.msameer0.rhythmicrush.game.renderer.GameRenderer renderer;
        private com.badlogic.gdx.graphics.OrthographicCamera camera;
        private com.badlogic.gdx.graphics.g2d.SpriteBatch batch;
        private com.badlogic.gdx.graphics.g2d.BitmapFont font;

        PlaytestListener(LevelData levelData) {
            this.levelData = levelData;
        }

        @Override
        public void create() {
            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();

            camera = new com.badlogic.gdx.graphics.OrthographicCamera(w, h);
            camera.position.set(w / 2f, h / 2f, 0);
            camera.update();

            batch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
            font  = new com.badlogic.gdx.graphics.g2d.BitmapFont();
            font.getData().setScale(1.5f);

            world    = new io.github.msameer0.rhythmicrush.game.GameWorld();
            renderer = new io.github.msameer0.rhythmicrush.game.renderer.GameRenderer(world, camera, batch);
            world.loadLevel(levelData);
        }

        @Override
        public void render() {
            // clear each frame — fixes the trailing ghost frames
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.18f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);

            boolean jump =
                Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SPACE) ||
                    Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP)    ||
                    Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)     ||
                    Gdx.input.isTouched();
            world.getPlayer().setJumpHeld(jump);

            // ESC closes just this window, not the whole app
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
                playtestWindow.closeWindow();
                return;
            }

            world.update(delta);
            renderer.render();

            float progress = world.getProgress();
            if (progress > 0f) {
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                font.draw(batch, Math.round(progress * 100f) + "%",
                    Gdx.graphics.getWidth() / 2f - 20,
                    Gdx.graphics.getHeight() - 12);
                batch.end();
            }

            if (world.isPlayerDead() || world.isLevelComplete()) {
                playtestWindow.closeWindow();
            }
        }

        @Override
        public void dispose() {
            batch.dispose();
            font.dispose();
        }
    }
}
