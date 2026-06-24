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
import io.github.msameer0.rhythmicrush.game.GameCamera
import io.github.msameer0.rhythmicrush.game.GameWorld
import io.github.msameer0.rhythmicrush.game.engine.FixedTickEngine
import io.github.msameer0.rhythmicrush.game.engine.LoudnessAnalyzer
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
    initialPracticeMode: Boolean = false
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
    private var practiceMode = initialPracticeMode
    private var practice: PracticeManager? = if (practiceMode) PracticeManager(world) else null

    private val gameCamera = OrthographicCamera()
    private val customCamera = GameCamera(gameCamera, world)
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
    private var pauseOverlayVisible = true
    private var lastDelta = 0f
    private var lastJumpHeld = false
    private var ignoreInputUntilRelease = false
    private var endWallSoundPlayed = false

    private val unprojectPosition = Vector3()
    private val sliderTouchPosition = Vector2()

    private val gameInputProcessor = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            unprojectPosition.set(screenX.toFloat(), screenY.toFloat(), 0f)
            gameCamera.unproject(unprojectPosition)
            val touchX = unprojectPosition.x
            val touchY = unprojectPosition.y

            if (paused) {
                if (
                    overlay.hitsPauseToggleButton(
                        touchX,
                        touchY,
                        gameCamera,
                        gameViewport
                    )
                ) {
                    pauseOverlayVisible = !pauseOverlayVisible
                    return true
                }
                if (!pauseOverlayVisible) return true
                when (overlay.hitPauseAction(touchX, touchY, gameCamera)) {
                    OverlayUI.PauseAction.RESTART -> {
                        triggerRestart()
                        ignoreInputUntilRelease = true
                        return true
                    }
                    OverlayUI.PauseAction.RESUME -> {
                        setPaused(false)
                        ignoreInputUntilRelease = true
                        return true
                    }
                    OverlayUI.PauseAction.PRACTICE -> {
                        togglePracticeMode()
                        ignoreInputUntilRelease = true
                        return true
                    }
                    OverlayUI.PauseAction.LEVEL_SELECT -> {
                        exitToLevelSelect()
                        return true
                    }
                    null -> Unit
                }
                sliderTouchPosition.set(touchX, touchY)
                val slider = overlay.hitSlider(sliderTouchPosition, gameCamera)
                if (slider != null) {
                    overlay.beginSliderDrag(slider)
                    return true
                }
                return false
            }

            if (levelCompletedState) {
                when (overlay.hitCompleteAction(touchX, touchY, gameCamera)) {
                    OverlayUI.CompleteAction.MENU -> {
                        exitToLevelSelect()
                        return true
                    }
                    OverlayUI.CompleteAction.PRIMARY -> {
                        launchNextLevel()
                        return true
                    }
                    OverlayUI.CompleteAction.REPLAY -> {
                        triggerRestart()
                        ignoreInputUntilRelease = true
                        return true
                    }
                    null -> Unit
                }
                return false
            }

            if (hud.hitsPauseButton(touchX, touchY, gameCamera, gameViewport)) {
                setPaused(true)
                return true
            }

            val currentPractice = practice
            if (practiceMode && currentPractice != null) {
                currentPractice.updateButtonCoords(
                    camCX(), camBot(),
                    game.settingsManager.uiPadding,
                    overlay.uiScale,
                    72f * overlay.uiScale
                )
                if (currentPractice.hitsPlus(touchX, touchY)) {
                    placeCheckpoint()
                    ignoreInputUntilRelease = true
                    return true
                }
                if (currentPractice.hitsMinus(touchX, touchY)) {
                    removeLastCheckpoint()
                    ignoreInputUntilRelease = true
                    return true
                }
            }
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (paused && overlay.activeSlider != null) {
                unprojectPosition.set(screenX.toFloat(), screenY.toFloat(), 0f)
                gameCamera.unproject(unprojectPosition)
                overlay.updateSliderFromDrag(unprojectPosition.x, gameCamera)
                if (music.getMusic() != null) {
                    music.setVolume(game.settingsManager.musicVolume)
                }
                return true
            }
            return false
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (overlay.activeSlider != null) {
                overlay.endSliderDrag()
                game.settingsManager.save()
                return true
            }
            return false
        }
    }

    init {
        gameViewport.update(Gdx.graphics.width, Gdx.graphics.height, true)

        renderer = GameRenderer(world, gameCamera, game.batch, game.settingsManager, game.atlasManager, customCamera)

        val font = game.fontManager.get(FontManager.SIZE_SMALL)
        val pauseTitleFont = game.fontManager.getTitle(FontManager.SIZE_LARGE)
        val pauseBodyFont = game.fontManager.getBody(FontManager.SIZE_SMALL)

        hud = HudRenderer(game, world, font, shapes, game.batch)

        val resumeRegion = game.atlasManager.menuAtlas.findRegion("start_button")
        val backRegion = game.atlasManager.levelSelectAtlas.findRegion("back")
        overlay =
            OverlayUI(game, levelData, pauseTitleFont, pauseBodyFont, shapes, game.batch, resumeRegion, backRegion)

        if (levelData != null) {
            world.loadLevel(levelData)
            levelKey = if (levelData.id >= 0) levelData.getProgressKey() else null
            recordAttempt()
            updateBgTexture()
        }

        hitboxesActive = game.settingsManager.showHitboxes
    }

    override fun show() {
        overlay.updateScale(gameViewport)
        game.soundManager.stopMenuMusic()
        
        val musicFile = levelData?.musicFile
        if (!musicFile.isNullOrEmpty()) {
            try {
                var fileHandle = Gdx.files.internal("musics/$musicFile")
                if (!fileHandle.exists()) {
                    fileHandle = Gdx.files.local("assets/musics/$musicFile")
                }
                if (fileHandle.exists()) {
                    Gdx.app.log("GameScreen", "Analyzing audio loudness map...")
                    loudnessMap =
                        LoudnessAnalyzer().analyze(fileHandle, loudnessSampleRate)
                    Gdx.app.log("GameScreen", "Analysis complete (size: ${loudnessMap?.size})")
                }
            } catch (exception: Exception) {
                Gdx.app.error(
                    "GameScreen",
                    "Failed to analyze audio: ${exception.message}"
                )
            }
        }

        music.start()
        if (game.settingsManager.lockCursorInGame) {
            Gdx.input.isCursorCatched = true
        }
        Gdx.input.inputProcessor = gameInputProcessor
    }

    override fun resize(width: Int, height: Int) {
        val oldX = gameCamera.position.x
        val oldY = gameCamera.position.y
        gameViewport.update(width, height, true)
        overlay.updateScale(gameViewport)
        gameCamera.position.set(oldX, oldY, 0f)
        gameCamera.update()
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
                exitToLevelSelect()
            } else {
                setPaused(true)
            }
            return
        }
        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) && paused) {
            setPaused(false)
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && paused) {
            triggerRestart()
            return
        }
        if (paused) return
        if (levelCompletedState) {
            customCamera.updateCompletionShake(delta)
            return
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !deathPaused) {
            triggerRespawn()
            return
        }

        if (deathPaused) {
            deathTimer += delta
            world.updateVisuals(delta)
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

        if (practiceMode) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) placeCheckpoint()
            if (Gdx.input.isKeyJustPressed(Input.Keys.X)) removeLastCheckpoint()
        }

        handleInput()
        
        world.player?.let { 
            customCamera.update(it, delta)
            world.boundaryTop = customCamera.getCeilingY()
            world.boundaryBottom = customCamera.getFloorY()
        }
        
        engine.update(delta)

        if (world.endCaptureActive) {
            if (!endWallSoundPlayed) {
                game.soundManager.playEndWallAbsorptionSound()
                endWallSoundPlayed = true
            }
            music.applyFadeProgress(world.endSequenceMusicFadeProgress)
        }

        if (world.isPlayerDead) {
            recordDeath()
            game.soundManager.playDeathSound()
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
            levelCompletedState = true
            checkAndShowAd(1.0f)
            music.stopAndDispose()
            Gdx.input.isCursorCatched = false
            customCamera.beginCompletionShake()
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

        val musicPosition = music.levelMusic?.position ?: 0f
        
        if (loudnessMap != null) {
            val sampleIndex = (musicPosition * loudnessSampleRate).toInt()
            if (sampleIndex >= 0 && sampleIndex < (loudnessMap?.size ?: 0)) {
                val intensity = loudnessMap!![sampleIndex]
                world.updateLoudness(intensity)
                if (Gdx.graphics.frameId % 60L == 0L) {
                    Gdx.app.log(
                        "GameScreen",
                        "Pos: $musicPosition, Idx: $sampleIndex, Intensity: $intensity"
                    )
                }
            } else {
                world.updateLoudness(0f)
            }
        }
        
        world.updateVisuals(delta)
    }

    override fun draw() {
        gameViewport.apply()

        val currentPractice = practice
        if (practiceMode && currentPractice != null) {
            currentPractice.updateButtonCoords(
                camCX(), camBot(),
                game.settingsManager.uiPadding,
                overlay.uiScale,
                72f * overlay.uiScale
            )
        }

        val bg = world.backgroundColor
        if (bgTexture == null || world.bgShape != null) {
            Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f)
        } else {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        renderer.render(lastDelta, paused, hitboxesActive, bgTexture, bg, world.currentLoudness)

        if (levelCompletedState) customCamera.clearShake()

        game.batch.projectionMatrix = gameCamera.combined
        shapes.projectionMatrix = gameCamera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        hud.drawProgressBarShapes(gameCamera, gameViewport)
        hud.drawPauseButtonShapes(gameCamera, gameViewport)

        if (practiceMode && currentPractice != null) {
            val opacity = game.settingsManager.practiceButtonOpacity
            currentPractice.drawButtonShapes(shapes, opacity)
            currentPractice.drawCheckpoints(shapes, gameCamera)
        }

        if (levelCompletedState || (paused && pauseOverlayVisible)) {
            overlay.drawDimOverlay(gameCamera, gameViewport)
        }
        if (paused && pauseOverlayVisible) {
            overlay.drawPausePanelShapes(gameCamera)
        } else if (levelCompletedState) {
            overlay.drawCompletePanelShapes(gameCamera)
        }
        if (paused) {
            overlay.drawPauseToggleButtonShapes(gameCamera, gameViewport, pauseOverlayVisible)
        }

        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.begin()

        hud.drawProgressBarText(gameCamera, gameViewport, levelKey)
        hud.drawSessionAttemptsText(gameCamera, gameViewport, sessionAttempts, levelKey)
        hud.drawNewBestPopup(gameCamera)

        if (practiceMode) drawPracticeButtonText()

        if (paused) {
            overlay.drawPauseToggleButtonText(gameCamera, gameViewport, pauseOverlayVisible)
            if (pauseOverlayVisible) {
                overlay.drawPauseOverlay(gameCamera, sessionAttempts, levelKey, practiceMode)
            }
        } else if (levelCompletedState) {
            overlay.drawCompleteOverlay(
                gameCamera,
                sessionAttempts,
                levelKey,
                levelIndex + 1 < game.levelManager.getLevels().size
            )
        }

        game.batch.end()

        if (paused && pauseOverlayVisible) overlay.drawPauseSliders(gameCamera)
    }

    private fun drawPracticeButtonText() {
        val currentPractice = practice ?: return

        val opacity = game.settingsManager.practiceButtonOpacity
        val uiScale = overlay.uiScale
        val btnSize = currentPractice.btnSize
        val font = game.fontManager.get(FontManager.SIZE_SMALL)
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout()

        font?.data?.setScale(1.5f * uiScale)
        layout.setText(font, "+")
        val plusX = currentPractice.plusX + (btnSize - layout.width) / 2f
        val plusY = currentPractice.plusY + (btnSize + layout.height) / 2f
        layout.setText(font, "-")
        val minusX = currentPractice.minusX + (btnSize - layout.width) / 2f
        val minusY = currentPractice.minusY + (btnSize + layout.height) / 2f

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
        if (p) pauseOverlayVisible = true
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
        endWallSoundPlayed = false
        paused = false
        lastJumpHeld = false
        hud.hideNewBestPopup()
        music.stopAndDispose()
        world.reset()
        renderer.reset()
        customCamera.reset()
        engine.reset()
        music.start()
        recordAttempt()
        if (game.settingsManager.lockCursorInGame) {
            Gdx.input.isCursorCatched = true
        }
    }

    private fun togglePracticeMode() {
        if (practiceMode) {
            practiceMode = false
            practice = null
            triggerRestart()
            return
        }

        practiceMode = true
        practice = PracticeManager(world).also { it.placeCheckpoint() }
        setPaused(false)
    }

    private fun updateBgTexture() {
        bgTexture?.dispose()
        bgTexture = null
        if (world.bgImage.isNotEmpty()) {
            val fileHandle = Gdx.files.internal("game/bg/${world.bgImage}")
            if (fileHandle.exists()) {
                bgTexture = Texture(fileHandle)
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
        if (practiceMode && practice?.hasCheckpoints() == true) {
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
        endWallSoundPlayed = false
        paused = false
        lastDelta = 0f
        lastJumpHeld = false
        world.reset()
        renderer.reset()
        customCamera.reset()
        engine.reset()
        music.start()
        recordAttempt()
        hitboxesActive = game.settingsManager.showHitboxes
        hud.hideNewBestPopup()
    }

    private fun respawnAtCheckpoint() {
        val currentPractice = practice ?: return

        deathPaused = false
        deathTimer = 0f
        levelCompletedState = false
        levelEndingSequence = false
        levelEndTimer = 0f
        endWallSoundPlayed = false
        paused = false
        lastDelta = 0f
        lastJumpHeld = false

        val musicOffset = currentPractice.applyLatestCheckpoint()
        renderer.reset()
        customCamera.reset()
        engine.reset()
        music.stopAndDispose()
        music.start(musicOffset)
        recordAttempt()
        hitboxesActive = game.settingsManager.showHitboxes
        hud.hideNewBestPopup()
    }

    private fun placeCheckpoint() {
        if (practiceMode) practice?.placeCheckpoint()
    }

    private fun removeLastCheckpoint() {
        if (!practiceMode) return
        val stillHasCheckpoints = practice?.removeLastCheckpoint() ?: false
        if (deathPaused && stillHasCheckpoints) triggerRespawn()
    }

    private fun exitToLevelSelect() {
        music.stopAndDispose()
        game.screen = LevelSelectScreen(game, levelIndex)
    }

    private fun launchNextLevel() {
        val levels = game.levelManager.getLevels()
        if (levelIndex + 1 < levels.size) {
            music.stopAndDispose()
            game.screen = GameScreen(game, levels[levelIndex + 1], levelIndex + 1)
        } else {
            triggerRestart()
        }
    }

    private fun recordAttempt() {
        sessionAttempts++
        val key = levelKey ?: return
        val levelProgress = game.progressManager.getOrCreate(key)
        levelProgress.totalAttempts++
        levelProgress.localDeviceAttempts++
        game.progressManager.save()
    }

    private fun recordDeath() {
        val key = levelKey
        if (key == null || practiceMode) return
        val percentage = MathUtils.round(world.progress * 100f)
        val levelProgress = game.progressManager.getOrCreate(key)
        if (percentage > levelProgress.bestPercent) {
            levelProgress.bestPercent = percentage
            game.progressManager.save()
            game.queueCloudProgressUpload()
            hud.showNewBestPopup(percentage)
            checkAndShowAd(percentage / 100f)
        }
    }

    private fun recordComplete() {
        val key = levelKey
        if (key == null || practiceMode) return
        val levelProgress = game.progressManager.getOrCreate(key)
        levelProgress.bestPercent = 100
        if (!levelProgress.completionRewardGranted) {
            awardCompletionRewards()
            levelProgress.completionRewardGranted = true
        }
        game.progressManager.save()
        game.queueCloudProgressUpload()
    }

    private fun awardCompletionRewards() {
        val difficulty = levelData?.difficulty?.lowercase() ?: "normal"
        val progress = game.progressManager
        when (difficulty) {
            "easy" -> {
                progress.coins += 50
                progress.points += 2
            }
            "normal" -> {
                progress.coins += 75
                progress.points += 3
            }
            "hard", "harder" -> {
                progress.coins += 125
                progress.points += 5
            }
            "insane" -> {
                progress.coins += 250
                progress.points += 7
            }
            "extreme", "demon" -> {
                progress.coins += 500
                progress.points += 10
            }
            else -> {
                progress.coins += 75
                progress.points += 3
            }
        }
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
