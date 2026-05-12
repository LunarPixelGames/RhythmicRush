package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.font.FontManager
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.engine.FixedTickEngine
import io.github.msameer0.rhythmicrush.game.level.LevelData
import io.github.msameer0.rhythmicrush.game.renderer.GameRenderer
import io.github.msameer0.rhythmicrush.screens.ui.HudRenderer
import io.github.msameer0.rhythmicrush.screens.ui.OverlayUI
import kotlin.math.min

/**
 * The main gameplay screen where the game world is updated and rendered.
 */
class GameScreen @JvmOverloads constructor(
    game: RhythmicRushGame,
    private val levelData: LevelData?,
    private val levelIndex: Int,
    private val isPracticeMode: Boolean = false
) : AbstractScreen(game) {

    companion object {
        private const val DEATH_PAUSE_DURATION = 0.75f
        private const val END_DELAY_TOTAL = 2.0f
        private const val END_MUSIC_FADE_START = 1.0f

        private const val AD_COOLDOWN_MS = 60_000L
        private var lastAdTimeMillis = 0L
    }

    private val world = GameWorld()
    private val engine = FixedTickEngine(world)
    private val renderer: GameRenderer
    private val music = MusicController(game, levelData)
    private val hud: HudRenderer
    private val overlay: OverlayUI
    private val practice: PracticeManager? = if (isPracticeMode) PracticeManager(world) else null

    private val gameCamera = OrthographicCamera()
    private val gameViewport = ExtendViewport(1920f, 1080f, gameCamera)

    private val shapes = ShapeRenderer()

    private var bgTexture: Texture? = null
    private var levelKey: String? = null
    private var loudnessMap: FloatArray? = null
    private val loudnessSampleRate = 50

    private var sessionAttempts = 0
    private var hitboxesActive = false

    private var paused = false
    private var deathPaused = false
    private var deathTimer = 0f
    private var levelEndingSequence = false
    private var levelEndTimer = 0f
    private var levelCompletedState = false
    private var lastDelta = 0f
    private var lastJumpHeld = false
    private var ignoreInputUntilRelease = false

    private val _unprojectTmp = Vector3()

    private val gameInputProcessor = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            _unprojectTmp.set(screenX.toFloat(), screenY.toFloat(), 0f)
            gameCamera.unproject(_unprojectTmp)
            val tx = _unprojectTmp.x
            val ty = _unprojectTmp.y

            if (paused) {
                if (overlay.hitsBackButton(tx, ty, gameCamera)) {
                    exitToLevelSelect()
                    return true
                }
                if (overlay.hitsResumeButton(tx, ty, gameCamera)) {
                    setPaused(false)
                    ignoreInputUntilRelease = true
                    return true
                }
                if (overlay.hitsSlider(Vector2(tx, ty), gameCamera)) {
                    overlay.beginSliderDrag()
                    return true
                }
                return false
            }

            if (levelCompletedState) {
                if (overlay.hitsBackButton(tx, ty, gameCamera)) {
                    exitToLevelSelect()
                    return true
                }
                if (overlay.hitsResumeButton(tx, ty, gameCamera)) {
                    triggerRestart()
                    ignoreInputUntilRelease = true
                    return true
                }
                return false
            }

            if (hud.hitsPauseButton(tx, ty, gameCamera, gameViewport)) {
                setPaused(true)
                return true
            }

            if (isPracticeMode && practice != null) {
                practice.updateButtonCoords(
                    camCX(), camBot(),
                    game.settingsManager.uiPadding,
                    overlay.uiScale,
                    72f * overlay.uiScale
                )
                if (practice.hitsPlus(tx, ty)) {
                    placeCheckpoint()
                    return true
                }
                if (practice.hitsMinus(tx, ty)) {
                    removeLastCheckpoint()
                    return true
                }
            }
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (paused && overlay.isSliderDragging) {
                _unprojectTmp.set(screenX.toFloat(), screenY.toFloat(), 0f)
                gameCamera.unproject(_unprojectTmp)
                overlay.updateSliderFromDrag(_unprojectTmp.x, gameCamera)
                if (music.getMusic() != null) {
                    music.setVolume(game.settingsManager.musicVolume)
                }
                return true
            }
            return false
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (overlay.isSliderDragging) {
                overlay.endSliderDrag()
                game.settingsManager.save()
                return true
            }
            return false
        }
    }

    init {
        gameViewport.update(Gdx.graphics.width, Gdx.graphics.height, true)

        renderer = GameRenderer(world, gameCamera, game.batch, game.settingsManager, game.atlasManager)

        val font = game.fontManager.get(FontManager.SIZE_SMALL)
        val pauseFont = game.fontManager.get(FontManager.SIZE_SMALL)

        hud = HudRenderer(game, world, font, shapes, game.batch)

        val resumeRegion = game.atlasManager.menuAtlas.findRegion("start_button")
        val backRegion = game.atlasManager.levelSelectAtlas.findRegion("back")
        overlay =
            OverlayUI(game, levelData, pauseFont, shapes, game.batch, resumeRegion, backRegion)

        if (levelData != null) {
            world.loadLevel(levelData)
            levelKey = levelData.fileName
            recordAttempt()
            updateBgTexture()
        }

        hitboxesActive = game.settingsManager.showHitboxes
    }

    override fun show() {
        overlay.updateScale()
        game.soundManager.stopMenuMusic()
        
        val mFile = levelData?.musicFile
        if (mFile != null && mFile.isNotEmpty()) {
            try {
                var fh = Gdx.files.internal("musics/$mFile")
                if (!fh.exists()) fh = Gdx.files.local("assets/musics/$mFile")
                if (fh.exists()) {
                    Gdx.app.log("GameScreen", "Analyzing audio loudness map...")
                    loudnessMap = io.github.msameer0.rhythmicrush.game.engine.LoudnessAnalyzer().analyze(fh, loudnessSampleRate)
                    Gdx.app.log("GameScreen", "Analysis complete (size: ${loudnessMap?.size})")
                }
            } catch (e: Exception) {
                Gdx.app.error("GameScreen", "Failed to analyze audio: ${e.message}")
            }
        }

        music.start()
        if (game.settingsManager.lockCursorInGame) {
            Gdx.input.isCursorCatched = true
        }
        Gdx.input.inputProcessor = gameInputProcessor
    }

    override fun resize(width: Int, height: Int) {
        overlay.updateScale()
        gameViewport.update(width, height, true)
    }

    override fun hide() {
        Gdx.input.isCursorCatched = false
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        super.dispose()
        renderer.dispose()
        bgTexture?.dispose()
        shapes.dispose()
        overlay.dispose()
        music.stopAndDispose()
    }

    override fun update(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (levelCompletedState) {
                exitToLevelSelect()
            } else if (paused) {
                setPaused(false)
            } else {
                setPaused(true)
            }
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && paused) {
            setPaused(false)
            return
        }
        if (paused || levelCompletedState) return

        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !deathPaused) {
            triggerRespawn()
            return
        }

        if (deathPaused) {
            deathTimer += delta
            hud.update(delta)
            if (deathTimer >= DEATH_PAUSE_DURATION) triggerRespawn()
            return
        }

        lastDelta = delta

        if (music.isFading) {
            if (music.updateFade(delta)) {
                world.reset()
                engine.reset()
                game.screen = LevelSelectScreen(game, levelIndex)
            }
            return
        }

        if (isPracticeMode) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) placeCheckpoint()
            if (Gdx.input.isKeyJustPressed(Input.Keys.X)) removeLastCheckpoint()
        }

        handleInput()
        engine.update(delta)

        if (world.isPlayerDead) {
            recordDeath()
            music.stopAndDispose()
            deathPaused = true
            deathTimer = 0f
            lastDelta = 0f
            engine.reset()
            if (game.settingsManager.showHitboxesOnDeath) hitboxesActive = true
        }

        if (world.isLevelComplete && !levelEndingSequence && !levelCompletedState) {
            recordComplete()
            levelEndingSequence = true
            levelEndTimer = 0f
        }

        if (levelEndingSequence && !levelCompletedState) {
            levelEndTimer += delta
            if (levelEndTimer >= END_MUSIC_FADE_START) {
                val fadeDuration = END_DELAY_TOTAL - END_MUSIC_FADE_START
                val fadeProgress = min((levelEndTimer - END_MUSIC_FADE_START) / fadeDuration, 1f)
                music.applyFadeProgress(fadeProgress)
            }
            if (levelEndTimer >= END_DELAY_TOTAL) {
                levelCompletedState = true
                checkAndShowAd(1.0f)
                music.stopAndDispose()
                Gdx.input.isCursorCatched = false
            }
        }

        // Keep visuals and camera in sync before drawing
        val musicPos = music.levelMusic?.position ?: 0f
        
        // Update loudness tracking
        if (loudnessMap != null) {
            val idx = (musicPos * loudnessSampleRate).toInt()
            if (idx >= 0 && idx < (loudnessMap?.size ?: 0)) {
                val intensity = loudnessMap!![idx]
                world.updateLoudness(intensity)
                if (Gdx.graphics.frameId % 60L == 0L) {
                    Gdx.app.log("GameScreen", "Pos: $musicPos, Idx: $idx, Intensity: $intensity")
                }
            } else {
                world.updateLoudness(0f)
            }
        }
        
        world.updateVisuals(delta)
        world.player?.let { renderer.updateCamera(it) }
    }

    override fun draw() {
        gameViewport.apply()

        if (isPracticeMode && practice != null) {
            practice.updateButtonCoords(
                camCX(), camBot(),
                game.settingsManager.uiPadding,
                overlay.uiScale,
                72f * overlay.uiScale
            )
        }

        val bg = world.backgroundColor
        if (bgTexture == null) {
            Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f)
        } else {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        renderer.render(lastDelta, paused, hitboxesActive, bgTexture, bg, world.currentLoudness)

        game.batch.projectionMatrix = gameCamera.combined
        shapes.projectionMatrix = gameCamera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        hud.drawProgressBarShapes(gameCamera, gameViewport)
        hud.drawPauseButtonShapes(gameCamera, gameViewport)

        if (isPracticeMode && practice != null) {
            val opacity = game.settingsManager.practiceButtonOpacity
            practice.drawButtonShapes(shapes, opacity)
            practice.drawCheckpoints(shapes, gameCamera)
        }

        if (paused || levelCompletedState) {
            overlay.drawDimOverlay(gameCamera, gameViewport)
        }

        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.begin()

        hud.drawProgressBarText(gameCamera, gameViewport, levelKey)
        hud.drawSessionAttemptsText(gameCamera, gameViewport, sessionAttempts, levelKey)
        hud.drawNewBestPopup(gameCamera)

        if (isPracticeMode) drawPracticeButtonText()

        if (paused) {
            overlay.drawPauseOverlay(gameCamera, sessionAttempts, levelKey)
        } else if (levelCompletedState) {
            overlay.drawCompleteOverlay(gameCamera, sessionAttempts, levelKey)
        }

        game.batch.end()

        if (paused) overlay.drawPauseSlider(gameCamera)
    }

    private fun drawPracticeButtonText() {
        if (practice == null) return

        val opacity = game.settingsManager.practiceButtonOpacity
        val uiScale = overlay.uiScale
        val btnSize = practice.btnSize
        val font = game.fontManager.get(FontManager.SIZE_SMALL)
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout()

        font?.data?.setScale(1.5f * uiScale)
        layout.setText(font, "+")
        val plusX = practice.plusX + (btnSize - layout.width) / 2f
        val plusY = practice.plusY + (btnSize + layout.height) / 2f
        layout.setText(font, "-")
        val minusX = practice.minusX + (btnSize - layout.width) / 2f
        val minusY = practice.minusY + (btnSize + layout.height) / 2f

        font?.setColor(0f, 0f, 0f, 0.4f * opacity)
        font?.draw(game.batch, "+", plusX + 2f * uiScale, plusY - 2f * uiScale)
        font?.draw(game.batch, "-", minusX + 2f * uiScale, minusY - 2f * uiScale)
        font?.setColor(1f, 1f, 1f, opacity)
        font?.draw(game.batch, "+", plusX, plusY)
        font?.draw(game.batch, "-", minusX, minusY)
        font?.data?.setScale(1f)
    }

    private fun setPaused(p: Boolean) {
        paused = p
        if (!p) overlay.endSliderDrag()
        if (p) music.pause() else music.resume()
        if (game.settingsManager.lockCursorInGame) {
            Gdx.input.isCursorCatched = !paused
        }
    }

    private fun triggerRestart() {
        levelCompletedState = false
        levelEndingSequence = false
        levelEndTimer = 0f
        paused = false
        lastJumpHeld = false
        hud.hideNewBestPopup()
        music.stopAndDispose()
        world.reset()
        engine.reset()
        music.start()
        recordAttempt()
        if (game.settingsManager.lockCursorInGame) {
            Gdx.input.isCursorCatched = true
        }
    }

    private fun updateBgTexture() {
        bgTexture?.dispose()
        bgTexture = null
        if (world.bgImage.isNotEmpty()) {
            val fh = Gdx.files.internal("game/bg/${world.bgImage}")
            if (fh.exists()) {
                bgTexture = Texture(fh)
                bgTexture?.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            } else {
                val localFh = Gdx.files.local("assets/game/bg/${world.bgImage}")
                if (localFh.exists()) {
                    bgTexture = Texture(localFh)
                    bgTexture?.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                }
            }
        }
    }

    private fun triggerRespawn() {
        if (isPracticeMode && practice?.hasCheckpoints() == true) {
            respawnAtCheckpoint()
            return
        }
        recordDeath()
        music.stopAndDispose()
        deathPaused = false
        deathTimer = 0f
        levelCompletedState = false
        levelEndingSequence = false
        levelEndTimer = 0f
        paused = false
        lastDelta = 0f
        lastJumpHeld = false
        world.reset()
        engine.reset()
        music.start()
        recordAttempt()
        hitboxesActive = game.settingsManager.showHitboxes
        hud.hideNewBestPopup()
    }

    private fun respawnAtCheckpoint() {
        if (practice == null) return

        deathPaused = false
        deathTimer = 0f
        levelCompletedState = false
        levelEndingSequence = false
        levelEndTimer = 0f
        paused = false
        lastDelta = 0f
        lastJumpHeld = false

        val musicOffset = practice.applyLatestCheckpoint()
        engine.reset()
        music.stopAndDispose()
        music.start(musicOffset)
        recordAttempt()
        hitboxesActive = game.settingsManager.showHitboxes
        hud.hideNewBestPopup()
    }

    private fun placeCheckpoint() {
        if (isPracticeMode) practice?.placeCheckpoint()
    }

    private fun removeLastCheckpoint() {
        if (!isPracticeMode) return
        val stillHasCheckpoints = practice?.removeLastCheckpoint() ?: false
        if (deathPaused && stillHasCheckpoints) triggerRespawn()
    }

    private fun exitToLevelSelect() {
        music.stopAndDispose()
        game.screen = LevelSelectScreen(game, levelIndex)
    }

    private fun recordAttempt() {
        sessionAttempts++
        val key = levelKey ?: return
        val p = game.progressManager.getOrCreate(key)
        p.totalAttempts = p.totalAttempts + 1
        game.progressManager.save()
    }

    private fun recordDeath() {
        val key = levelKey
        if (key == null || isPracticeMode) return
        val pct = MathUtils.round(world.progress * 100f)
        val p = game.progressManager.getOrCreate(key)
        if (pct > p.bestPercent) {
            p.bestPercent = pct
            game.progressManager.save()
            hud.showNewBestPopup(pct)
            checkAndShowAd(pct / 100f)
        }
    }

    private fun recordComplete() {
        val key = levelKey
        if (key == null || isPracticeMode) return
        val p = game.progressManager.getOrCreate(key)
        p.bestPercent = 100
        game.progressManager.save()
    }

    private fun handleInput() {
        if (ignoreInputUntilRelease) {
            if (!Gdx.input.isTouched) ignoreInputUntilRelease = false
            return
        }
        val jump = Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyPressed(Input.Keys.W) ||
            Gdx.input.isKeyPressed(Input.Keys.UP) ||
            Gdx.input.isTouched

        if (jump != lastJumpHeld) {
            engine.queueInput(jump, engine.accumulator)
            lastJumpHeld = jump
        }
    }

    private fun checkAndShowAd(adChance: Float) {
        if (TimeUtils.timeSinceMillis(lastAdTimeMillis) < AD_COOLDOWN_MS) return
        if (MathUtils.randomBoolean(adChance) && game.adController != null) {
            // game.getAdController().showInterstitialAd(); // TODO: ADS
            lastAdTimeMillis = TimeUtils.millis()
        }
    }

    private fun camCX(): Float {
        return gameCamera.position.x
    }

    private fun camBot(): Float {
        return gameCamera.position.y - gameViewport.worldHeight / 2f
    }
}
