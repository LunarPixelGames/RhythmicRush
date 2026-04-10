package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.atlas.AtlasManager
import io.github.msameer0.rhythmicrush.audio.SoundManager
import io.github.msameer0.rhythmicrush.font.FontManager
import io.github.msameer0.rhythmicrush.game.level.LevelManager
import io.github.msameer0.rhythmicrush.game.level.ProgressManager
import io.github.msameer0.rhythmicrush.game.registries.Registries
import io.github.msameer0.rhythmicrush.settings.SettingsManager

/**
 * An enhanced loading screen that displays the game title, a progress bar,
 * and status text indicating what is currently being loaded.
 */
class LoadingScreen(game: RhythmicRushGame) : AbstractScreen(game) {

    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()
    private var progress = 0f
    private var loadStep = 0
    private var finished = false
    private var statusText = "Initializing..."

    private var titleRegion: TextureRegion? = null
    private var statusFont: BitmapFont? = null

    companion object {
        private const val TOTAL_STEPS = 8
    }

    override fun show() {
        // Overridden to prevent AbstractScreen from accessing uninitialized managers
    }

    override fun update(delta: Float) {
        if (finished) return

        when (loadStep) {
            0 -> {
                statusText = "Initializing Registries..."
                Registries.init()
            }
            1 -> {
                statusText = "Loading Settings..."
                game.settingsManager = SettingsManager()
            }
            2 -> {
                statusText = "Loading Textures..."
                game.atlasManager = AtlasManager()
                // Cache title region for drawing in next frames
                titleRegion = game.atlasManager.menuAtlas.findRegion("title")
            }
            3 -> {
                statusText = "Loading Fonts..."
                game.fontManager = FontManager()
                // Cache font for drawing status text in next frames
                statusFont = game.fontManager.get(FontManager.SIZE_SMALL)
            }
            4 -> {
                statusText = "Loading Audio..."
                game.soundManager = SoundManager()
            }
            5 -> {
                statusText = "Loading Progress..."
                game.progressManager = ProgressManager()
            }
            6 -> {
                statusText = "Scanning Levels..."
                game.levelManager = LevelManager()
            }
            7 -> {
                statusText = "Finalizing..."
                finalizeLoading()
                finished = true
            }
        }

        if (loadStep < TOTAL_STEPS) {
            loadStep++
            progress = loadStep.toFloat() / TOTAL_STEPS
        }
    }

    private fun finalizeLoading() {
        game.soundManager.setMusicVolume(game.settingsManager.musicVolume)
        game.settingsManager.applyFpsCap()
        game.settingsManager.applyVsync()

        if (game.settingsManager.menuMusicEnabled) {
            game.soundManager.playMenuMusic()
        }

        game.screen = MainMenuScreen(game)
    }

    override fun draw() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val width = viewport.worldWidth
        val height = viewport.worldHeight

        // 1. Pre-calculate dimensions for centering
        val barWidth = width * 0.6f
        val barHeight = 10f

        var titleW = 0f
        var titleH = 0f
        titleRegion?.let { region ->
            val maxTitleW = width * 0.7f
            val titleScale = maxTitleW / region.regionWidth
            titleW = region.regionWidth * titleScale
            titleH = region.regionHeight * titleScale
        }

        val textPadding = 22f
        val titlePadding = if (titleRegion != null) 25f else 0f

        // Total height of the centered group
        val totalGroupH = titleH + titlePadding + barHeight + textPadding
        val groupBottomY = (height - totalGroupH) / 2f

        // Calculate specific Y positions
        val barY = groupBottomY + textPadding
        val titleY = barY + barHeight + titlePadding
        val textY = barY - 10f // A bit below the bar

        // 2. Draw Title Image
        titleRegion?.let { region ->
            game.batch.projectionMatrix = camera.combined
            game.batch.begin()
            val titleX = (width - titleW) / 2f
            game.batch.draw(region, titleX, titleY, titleW, titleH)
            game.batch.end()
        }

        // 3. Draw Progress Bar
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val barX = (width - barWidth) / 2f
        shapeRenderer.color = Color(0.15f, 0.15f, 0.15f, 1f)
        shapeRenderer.rect(barX, barY, barWidth, barHeight)
        shapeRenderer.color = Color(0.2f, 0.5f, 1f, 1f)
        shapeRenderer.rect(barX, barY, barWidth * progress, barHeight)
        shapeRenderer.end()

        // 4. Draw Status Text
        statusFont?.let { font ->
            game.batch.begin()
            font.data.setScale(0.6f)
            layout.setText(font, statusText)
            font.color = Color.LIGHT_GRAY
            font.draw(game.batch, statusText, (width - layout.width) / 2f, textY)
            font.data.setScale(1f)
            game.batch.end()
        }
    }

    override fun dispose() {
        super.dispose()
        shapeRenderer.dispose()
    }
}
