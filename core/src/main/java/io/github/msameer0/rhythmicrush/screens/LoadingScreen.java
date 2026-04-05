package io.github.msameer0.rhythmicrush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.atlas.AtlasManager;
import io.github.msameer0.rhythmicrush.audio.SoundManager;
import io.github.msameer0.rhythmicrush.font.FontManager;
import io.github.msameer0.rhythmicrush.game.level.LevelManager;
import io.github.msameer0.rhythmicrush.game.level.ProgressManager;
import io.github.msameer0.rhythmicrush.game.registries.Registries;
import io.github.msameer0.rhythmicrush.settings.SettingsManager;

/**
 * An enhanced loading screen that displays the game title, a progress bar,
 * and status text indicating what is currently being loaded.
 */
public class LoadingScreen extends AbstractScreen {

    private final ShapeRenderer shapeRenderer;
    private final GlyphLayout layout;
    private float progress = 0f;
    private int loadStep = 0;
    private static final int TOTAL_STEPS = 8;
    private boolean finished = false;
    private String statusText = "Initializing...";
    
    private TextureRegion titleRegion;
    private BitmapFont statusFont;

    public LoadingScreen(RhythmicRushGame game) {
        super(game);
        this.shapeRenderer = new ShapeRenderer();
        this.layout = new GlyphLayout();
    }

    @Override
    public void show() {
        // Overridden to prevent AbstractScreen from accessing uninitialized managers
    }

    @Override
    protected void update(float delta) {
        if (finished) return;

        switch (loadStep) {
            case 0:
                statusText = "Initializing Registries...";
                Registries.init();
                break;
            case 1:
                statusText = "Loading Settings...";
                game.setSettingsManager(new SettingsManager());
                break;
            case 2:
                statusText = "Loading Textures...";
                game.setAtlasManager(new AtlasManager());
                // Cache title region for drawing in next frames
                titleRegion = game.getAtlasManager().getMenuAtlas().findRegion("title");
                break;
            case 3:
                statusText = "Loading Fonts...";
                game.setFontManager(new FontManager());
                // Cache font for drawing status text in next frames
                statusFont = game.getFontManager().get(FontManager.SIZE_SMALL);
                break;
            case 4:
                statusText = "Loading Audio...";
                game.setSoundManager(new SoundManager());
                break;
            case 5:
                statusText = "Loading Progress...";
                game.setProgressManager(new ProgressManager());
                break;
            case 6:
                statusText = "Scanning Levels...";
                game.setLevelManager(new LevelManager());
                break;
            case 7:
                statusText = "Finalizing...";
                finalizeLoading();
                finished = true;
                break;
        }

        if (loadStep < TOTAL_STEPS) {
            loadStep++;
            progress = (float) loadStep / TOTAL_STEPS;
        }
    }

    private void finalizeLoading() {
        game.getSoundManager().setMusicVolume(game.getSettingsManager().getMusicVolume());
        game.getSettingsManager().applyFpsCap();
        game.getSettingsManager().applyVsync();

        if (game.getSettingsManager().getMenuMusicEnabled()) {
            game.getSoundManager().playMenuMusic();
        }

        game.setScreen(new MainMenuScreen(game));
    }

    @Override
    protected void draw() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();

        // 1. Pre-calculate dimensions for centering
        float barWidth = width * 0.6f;
        float barHeight = 10f;
        
        float titleW = 0, titleH = 0;
        if (titleRegion != null) {
            float maxTitleW = width * 0.7f;
            float titleScale = maxTitleW / titleRegion.getRegionWidth();
            titleW = titleRegion.getRegionWidth() * titleScale;
            titleH = titleRegion.getRegionHeight() * titleScale;
        }

        float textPadding = 22f;
        float titlePadding = (titleRegion != null) ? 25f : 0f;
        
        // Total height of the centered group
        float totalGroupH = titleH + titlePadding + barHeight + textPadding;
        float groupBottomY = (height - totalGroupH) / 2f;

        // Calculate specific Y positions
        float barY = groupBottomY + textPadding;
        float titleY = barY + barHeight + titlePadding;
        float textY = barY - 10f; // A bit below the bar

        // 2. Draw Title Image
        if (titleRegion != null) {
            game.getBatch().setProjectionMatrix(camera.combined);
            game.getBatch().begin();
            float titleX = (width - titleW) / 2f;
            game.getBatch().draw(titleRegion, titleX, titleY, titleW, titleH);
            game.getBatch().end();
        }

        // 3. Draw Progress Bar
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float barX = (width - barWidth) / 2;
        shapeRenderer.setColor(new Color(0.15f, 0.15f, 0.15f, 1f));
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.setColor(new Color(0.2f, 0.5f, 1f, 1f));
        shapeRenderer.rect(barX, barY, barWidth * progress, barHeight);
        shapeRenderer.end();

        // 4. Draw Status Text
        if (statusFont != null) {
            game.getBatch().begin();
            statusFont.getData().setScale(0.6f);
            layout.setText(statusFont, statusText);
            statusFont.setColor(Color.LIGHT_GRAY);
            statusFont.draw(game.getBatch(), statusText, (width - layout.width) / 2f, textY);
            statusFont.getData().setScale(1f);
            game.getBatch().end();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        shapeRenderer.dispose();
    }
}
