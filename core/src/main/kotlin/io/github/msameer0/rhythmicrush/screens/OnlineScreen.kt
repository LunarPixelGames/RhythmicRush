package io.github.msameer0.rhythmicrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Application
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import io.github.msameer0.rhythmicrush.RhythmicRushGame
import io.github.msameer0.rhythmicrush.account.AccountCallback
import io.github.msameer0.rhythmicrush.account.AccountOperationError
import io.github.msameer0.rhythmicrush.account.AccountProfile
import io.github.msameer0.rhythmicrush.account.AccountState
import io.github.msameer0.rhythmicrush.account.AccountStatus
import io.github.msameer0.rhythmicrush.account.CloudProgress
import io.github.msameer0.rhythmicrush.account.LeaderboardSnapshot
import io.github.msameer0.rhythmicrush.account.LinkedProvider
import io.github.msameer0.rhythmicrush.account.SyncRequest
import io.github.msameer0.rhythmicrush.account.SyncResult
import io.github.msameer0.rhythmicrush.font.FontManager
import io.github.msameer0.rhythmicrush.game.level.PatternShape
import io.github.msameer0.rhythmicrush.game.level.ProgressManager
import io.github.msameer0.rhythmicrush.game.renderer.ProceduralBackground
import io.github.msameer0.rhythmicrush.input.TextInputType
import io.github.msameer0.rhythmicrush.ui.UI
import java.util.UUID
import kotlin.math.min

class OnlineScreen(game: RhythmicRushGame) : AbstractScreen(game), InputProcessor {
    private val shapes = ShapeRenderer()
    private val proceduralBackground = ProceduralBackground()
    private val layout = GlyphLayout()
    private val touch = Vector3()
    private val buttons = mutableListOf<HubButton>()
    private val backgroundShape = PatternShape.entries.random()
    private val backgroundSeed = MathUtils.random.nextInt()
    private val backgroundColor = Color(
        0.18f + MathUtils.random() * 0.32f,
        0.18f + MathUtils.random() * 0.32f,
        0.26f + MathUtils.random() * 0.34f,
        1f
    )
    private lateinit var titleFont: BitmapFont
    private lateinit var bodyFont: BitmapFont
    private lateinit var smallFont: BitmapFont
    private var backgroundScroll = 0f
    private var accountStatus: AccountStatus? = null
    private var statusMessage = ""
    private var statusColor = UI.TEXT_SECONDARY
    private var statusPopup: StatusPopup? = null
    private var pendingUsername = ""
    private var pendingEmail = ""
    private var deleteConfirmStage = 0
    private var promptState: PromptState? = null
    private var previousInputProcessor: InputProcessor? = null
    private var leaderboardPage = 0
    private var leaderboardRequested = false
    private var lastUiSecond = -1L

    private val accountListener: (AccountStatus) -> Unit = {
        accountStatus = it
        val state = it.state
        if (state == AccountState.SignedOut || state == AccountState.NeedsEmailVerification) {
            pendingUsername = ""
            deleteConfirmStage = 0
        }
        if (state is AccountState.RecoverableError) {
            showError(state.error.userMessage)
        }
        rebuildButtons()
        if (
            it.state == AccountState.SignedIn &&
            game.accountManager.cachedLeaderboard == null &&
            !leaderboardRequested
        ) {
            leaderboardRequested = true
            fetchLeaderboard(forceRefresh = false)
        }
    }

    override fun show() {
        super.show()
        titleFont = game.fontManager.getTitle(FontManager.SIZE_XLARGE)
        bodyFont = game.fontManager.getBody(FontManager.SIZE_LARGE)
        smallFont = game.fontManager.getBody(FontManager.SIZE_MEDIUM)
        game.accountManager.addListener(accountListener)
        previousInputProcessor = Gdx.input.inputProcessor
        Gdx.input.inputProcessor = this
        rebuildButtons()
    }

    override fun update(delta: Float) {
        backgroundScroll += 22f * delta
        val uiSecond = System.currentTimeMillis() / 1000L
        if (accountStatus?.profile != null && uiSecond != lastUiSecond) {
            lastUiSecond = uiSecond
            rebuildButtons()
        }
        if (statusPopup != null) {
            if (
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            ) {
                statusPopup = null
                return
            }
            handleStatusPopupTouch()
            return
        }
        if (promptState != null) {
            handlePromptTouch()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.screen = MainMenuScreen(game)
            return
        }
        if (Gdx.input.justTouched()) {
            touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(touch)
            buttons.lastOrNull { it.enabled && it.hits(touch.x, touch.y) }?.action?.invoke()
        }
    }

    override fun draw() {
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        drawBackground()

        val signedIn = accountStatus?.profile != null
        val panelW = if (signedIn) min(viewport.worldWidth - 72f, 1848f)
            else min(viewport.worldWidth * 0.84f, 1480f)
        val panelH = if (signedIn) min(viewport.worldHeight - 64f, 1016f)
            else min(viewport.worldHeight * 0.84f, 900f)
        val panelX = (viewport.worldWidth - panelW) / 2f
        val panelY = (viewport.worldHeight - panelH) / 2f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (signedIn) {
            drawDashboardShapes(dashboardLayout(panelX, panelY, panelW, panelH))
        } else {
            drawPanelWithShadow(
                shapes,
                panelX,
                panelY,
                panelW,
                panelH,
                34f,
                Color(UI.PANEL.r, UI.PANEL.g, UI.PANEL.b, 0.96f)
            )
        }
        buttons.forEach { button ->
            UI.filled(
                shapes,
                button.x,
                button.y,
                button.w,
                button.h,
                16f,
                when {
                    !button.enabled -> Color(0.34f, 0.34f, 0.44f, 1f)
                    button.danger -> UI.DANGER
                    button.secondary -> UI.PANEL_ELEVATED
                    else -> UI.LIME
                }
            )
        }
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        drawCentered(
            titleFont,
            if (signedIn) "GLOBAL LEADERBOARD" else "ONLINE",
            panelX + panelW / 2f,
            panelY + panelH - 45f,
            UI.YELLOW,
            if (signedIn) 0.86f else 1f
        )
        drawHeader(panelX, panelY, panelW, panelH)
        drawContent(panelX, panelY, panelW, panelH)
        buttons.forEach { button ->
            if (button.label.isNotBlank()) {
                drawCenteredFit(
                    smallFont,
                    button.label,
                    button.x + button.w / 2f,
                    button.y + button.h / 2f,
                    if (button.secondary || button.danger || !button.enabled) UI.TEXT else Color.BLACK,
                    0.95f,
                    0.58f,
                    button.w - 24f
                )
            }
        }
        if (statusMessage.isNotBlank()) {
            drawCentered(
                smallFont,
                statusMessage,
                panelX + panelW / 2f,
                panelY + if (signedIn) 18f else 36f,
                statusColor,
                0.62f
            )
        }
        game.batch.end()
        drawPrompt()
        drawStatusPopup()
    }

    private fun drawHeader(panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        val status = accountStatus ?: return
        val right = when (status.state) {
            AccountState.Unavailable -> "UNAVAILABLE"
            AccountState.SignedOut -> "SIGNED OUT"
            AccountState.Authenticating -> "SIGNING IN..."
            AccountState.Syncing -> "SYNCING..."
            AccountState.Deleting -> "DELETING..."
            is AccountState.RecoverableError -> "CONNECTION ISSUE"
            else -> status.profile?.username ?: "ACCOUNT"
        }
        if (status.profile != null) {
            val dashboard = dashboardLayout(panelX, panelY, panelW, panelH)
            drawCentered(
                bodyFont,
                right,
                dashboard.rightX + dashboard.rightW / 2f,
                panelY + panelH - 54f,
                UI.TEXT_SECONDARY,
                0.70f
            )
        } else {
            bodyFont.data.setScale(0.68f)
            layout.setText(bodyFont, right)
            drawText(
                bodyFont,
                right,
                panelX + panelW - layout.width - 36f,
                panelY + panelH - 54f,
                UI.TEXT_SECONDARY,
                0.68f
            )
        }
    }

    private fun drawContent(panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        val status = accountStatus ?: return
        val centerX = panelX + panelW / 2f
        when (val state = status.state) {
            AccountState.Unavailable -> {
                drawCentered(bodyFont, "Online services are unavailable on this build.", centerX, panelY + panelH * 0.75f, UI.TEXT, 0.8f)
            }
            AccountState.SignedOut -> drawSignedOut(panelX, panelY, panelW, panelH)
            AccountState.NeedsEmailVerification -> {
                drawCentered(bodyFont, "VERIFY YOUR EMAIL", centerX, panelY + panelH * 0.82f, UI.YELLOW, 1.04f)
                drawCenteredFit(smallFont, status.profile?.email ?: "", centerX, panelY + panelH * 0.735f, UI.TEXT, 0.82f, 0.52f, panelW - 160f)
                drawCenteredFit(smallFont, "Open the verification email, then refresh your account.", centerX, panelY + panelH * 0.665f, UI.TEXT_SECONDARY, 0.78f, 0.54f, panelW - 140f)
                drawCentered(smallFont, "If you cannot find it, check your spam folder.", centerX, panelY + panelH * 0.605f, UI.YELLOW, 0.74f)
            }
            AccountState.NeedsUsername -> {
                drawCentered(bodyFont, "CHOOSE YOUR USERNAME", centerX, panelY + panelH * 0.82f, UI.YELLOW, 1.04f)
                drawCenteredFit(smallFont, pendingUsername.ifBlank { "3-20 letters, numbers, or underscores" }, centerX, panelY + panelH * 0.72f, UI.TEXT, 0.86f, 0.58f, panelW - 160f)
            }
            AccountState.SignedIn,
            AccountState.Syncing,
            AccountState.Deleting -> drawSignedIn(panelX, panelY, panelW, panelH)
            is AccountState.RecoverableError -> {
                if (status.profile != null) {
                    drawSignedIn(panelX, panelY, panelW, panelH)
                } else {
                    drawCentered(bodyFont, "SOMETHING WENT WRONG", centerX, panelY + panelH * 0.82f, UI.DANGER, 0.86f)
                    drawCentered(smallFont, state.error.userMessage, centerX, panelY + panelH * 0.72f, UI.TEXT, 0.68f)
                }
            }
            AccountState.Authenticating -> drawCentered(bodyFont, "CONNECTING...", centerX, panelY + panelH * 0.75f, UI.TEXT, 0.86f)
            AccountState.MergeRequired,
            AccountState.MergeInProgress -> drawCentered(bodyFont, "ACCOUNT MERGE REQUIRED", centerX, panelY + panelH * 0.75f, UI.YELLOW, 0.86f)
        }
    }

    private fun drawSignedOut(panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        val centerX = panelX + panelW / 2f
        drawCentered(bodyFont, "YOUR RHYTHMIC RUSH ACCOUNT", centerX, panelY + panelH * 0.82f, UI.TEXT, 1.02f)
        drawCenteredFit(
            smallFont,
            "Sync progress between phone and PC, restore saves, and join the global leaderboard.",
            centerX,
            panelY + panelH * 0.735f,
            UI.TEXT_SECONDARY,
            0.76f,
            0.54f,
            panelW - 120f
        )
        if (pendingEmail.isNotBlank()) {
            drawCenteredFit(smallFont, pendingEmail, centerX, panelY + panelH * 0.675f, UI.BLUE, 0.70f, 0.48f, panelW - 160f)
        }
        drawCenteredFit(
            smallFont,
            "New here? Register with email, or continue with Play Games on Android.",
            centerX,
            panelY + 96f,
            UI.TEXT_SECONDARY,
            0.74f,
            0.54f,
            panelW - 120f
        )
    }

    private fun drawSignedIn(panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        val status = accountStatus ?: return
        val profile = status.profile ?: return
        val dashboard = dashboardLayout(panelX, panelY, panelW, panelH)
        val leftPadding = 28f
        val leftX = dashboard.leftX + leftPadding
        val leftTextW = dashboard.leftW - leftPadding * 2f
        val topY = dashboard.leftY + dashboard.contentH - 56f
        drawTextFit(smallFont, "PLAYER", leftX, topY, UI.TEXT_SECONDARY, 0.74f, 0.58f, leftTextW)
        drawTextFit(bodyFont, profile.username ?: "Player", leftX, topY - 44f, UI.TEXT, 1.18f, 0.72f, leftTextW)
        drawTextFit(
            smallFont,
            profile.email ?: "Play Games account",
            leftX,
            topY - 88f,
            UI.TEXT_SECONDARY,
            if (profile.email == null) 0.76f else 0.68f,
            0.42f,
            leftTextW
        )

        val snapshot = game.accountManager.cachedLeaderboard
        val own = snapshot?.currentPlayer
        drawStat(leftX, topY - 164f, leftTextW, "GLOBAL RANK", own?.rank?.let { "#$it" } ?: "--", UI.YELLOW)
        drawStat(leftX, topY - 272f, leftTextW, "POINTS", own?.points?.toString() ?: game.progressManager.points.toString(), UI.BLUE)
        drawStat(leftX, topY - 380f, leftTextW, "LEVELS COMPLETED", own?.completedLevels?.toString() ?: "--", UI.LIME)

        drawTextFit(smallFont, "ACCOUNT", leftX, topY - 492f, UI.TEXT_SECONDARY, 0.72f, 0.58f, leftTextW)
        val providerText = when {
            profile.providers.contains(LinkedProvider.PLAY_GAMES) &&
                profile.providers.contains(LinkedProvider.EMAIL_PASSWORD) -> "Play Games + Email"
            profile.providers.contains(LinkedProvider.PLAY_GAMES) -> "Google Play Games"
            else -> "Email and password"
        }
        drawTextFit(smallFont, providerText, leftX, topY - 536f, UI.TEXT, 0.76f, 0.48f, leftTextW)
        drawTextFit(
            smallFont,
            if (profile.email == null || profile.emailVerified) "Verified" else "Email not verified",
            leftX,
            topY - 580f,
            if (profile.email == null || profile.emailVerified) UI.LIME else UI.YELLOW,
            0.72f,
            0.48f,
            leftTextW
        )
        drawTextFit(
            smallFont,
            if (status.pendingUploadCount == 0) "Cloud save up to date"
            else "${status.pendingUploadCount} upload(s) queued",
            leftX,
            topY - 624f,
            if (status.pendingUploadCount == 0) UI.LIME else UI.YELLOW,
            0.70f,
            0.46f,
            leftTextW
        )

        val rankingStatus = when (snapshot?.currentPlayerStatus) {
            "ranked" -> "Visible on leaderboard"
            "banned" -> "Leaderboard access restricted"
            "ineligible" -> "Not currently eligible"
            else -> "Upload progress to get ranked"
        }
        drawTextFit(smallFont, rankingStatus, leftX, topY - 680f, when {
            snapshot?.currentPlayerStatus == "ranked" -> UI.LIME
            snapshot?.currentPlayerStatus == "banned" -> UI.DANGER
            else -> UI.TEXT_SECONDARY
        }, 0.68f, 0.42f, leftTextW)

        val tableX = dashboard.centerX + 34f
        val tableRight = dashboard.centerX + dashboard.centerW - 34f
        val tableTop = dashboard.centerY + dashboard.contentH - 116f
        val rowsPerPage = leaderboardRowsPerPage(dashboard)
        drawText(smallFont, "RANK", tableX, tableTop, UI.TEXT_SECONDARY, 1.35f)
        drawText(smallFont, "PLAYER", tableX + 176f, tableTop, UI.TEXT_SECONDARY, 1.35f)
        drawRightAligned(smallFont, "COMPLETED", tableRight - 228f, tableTop, UI.TEXT_SECONDARY, 1.25f)
        drawRightAligned(smallFont, "POINTS", tableRight, tableTop, UI.TEXT_SECONDARY, 1.35f)

        if (snapshot == null || snapshot.entries.isEmpty()) {
            drawCentered(
                bodyFont,
                if (status.state == AccountState.Syncing) "LOADING RANKINGS..."
                else "NO RANKED PLAYERS YET",
                dashboard.centerX + dashboard.centerW / 2f,
                dashboard.centerY + dashboard.contentH / 2f,
                UI.TEXT_SECONDARY,
                0.72f
            )
        } else {
            val maxPage = maxLeaderboardPage(snapshot.entries.size, rowsPerPage)
            leaderboardPage = leaderboardPage.coerceIn(0, maxPage)
            val first = leaderboardPage * rowsPerPage
            snapshot.entries.drop(first).take(rowsPerPage).forEachIndexed { index, entry ->
                  val y = leaderboardRowTextY(dashboard, index)
                  val color = if (entry.currentPlayer) UI.LIME else UI.TEXT
                  drawText(smallFont, "#${entry.rank}", tableX + 18f, y, color, 1.35f)
                  drawTextFit(smallFont, entry.username, tableX + 176f, y, color, 1.35f, 0.82f, tableRight - tableX - 520f)
                  drawRightAligned(
                      smallFont,
                      entry.completedLevels.toString(),
                      tableRight - 228f,
                      y,
                      if (entry.currentPlayer) UI.TEXT else UI.TEXT_SECONDARY,
                      1.32f
                  )
                  drawRightAligned(
                      smallFont,
                      entry.points.toString(),
                      tableRight,
                      y,
                      if (entry.currentPlayer) UI.YELLOW else UI.BLUE,
                      1.39f
                  )
              }
            drawCentered(
                smallFont,
                "PAGE ${leaderboardPage + 1} / ${maxPage + 1}",
                dashboard.centerX + dashboard.centerW / 2f,
                dashboard.centerY + 42f,
                UI.TEXT_MUTED,
                0.56f
            )
        }

        drawCentered(
            smallFont,
            "ACTIONS",
            dashboard.rightX + dashboard.rightW / 2f,
            topY,
            UI.TEXT_SECONDARY,
            0.70f
        )
        val refreshText = snapshot?.let {
            val remaining = ((it.nextRefreshAt - System.currentTimeMillis()).coerceAtLeast(0L) + 999L) / 1000L
            if (remaining > 0) "Refresh in ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}"
            else "Refresh available"
        } ?: "Refresh available"
          drawCenteredFit(
              smallFont,
              refreshText,
              dashboard.rightX + dashboard.rightW / 2f,
              dashboard.rightY + 90f,
              UI.TEXT_SECONDARY,
              0.74f,
              0.46f,
              dashboard.rightW - 42f
          )
        val updated = snapshot?.generatedAt?.takeIf { it > 0L }?.let {
            val age = ((System.currentTimeMillis() - it).coerceAtLeast(0L) / 60000L)
            if (age == 0L) "Updated just now" else "Updated ${age}m ago"
        } ?: "No cached rankings"
          drawCenteredFit(
              smallFont,
              updated,
              dashboard.rightX + dashboard.rightW / 2f,
              dashboard.rightY + 54f,
              UI.TEXT_MUTED,
              0.62f,
              0.42f,
              dashboard.rightW - 42f
          )
    }

    private fun rebuildButtons() {
        if (!::bodyFont.isInitialized) return
        buttons.clear()
        val signedIn = accountStatus?.profile != null
        val panelW = if (signedIn) min(viewport.worldWidth - 72f, 1848f)
            else min(viewport.worldWidth * 0.84f, 1480f)
        val panelH = if (signedIn) min(viewport.worldHeight - 64f, 1016f)
            else min(viewport.worldHeight * 0.84f, 900f)
        val panelX = (viewport.worldWidth - panelW) / 2f
        val panelY = (viewport.worldHeight - panelH) / 2f
        buttons += HubButton(panelX + 18f, panelY + panelH - 70f, 154f, 52f, "<  BACK", secondary = true) {
            game.screen = MainMenuScreen(game)
        }
        val status = accountStatus ?: return
        val state = status.state
        val busy = state == AccountState.Authenticating ||
            state == AccountState.Syncing ||
            state == AccountState.Deleting
        when (state) {
            AccountState.SignedOut -> buildSignedOutButtons(panelX, panelY, panelW, panelH, busy)
            AccountState.NeedsEmailVerification -> {
                addCenteredButtons(
                    panelX, panelY, panelW, panelH,
                    listOf(
                        ButtonSpec("RESEND EMAIL") { resendVerification() },
                        ButtonSpec("I VERIFIED - REFRESH") { refreshProfile() },
                        ButtonSpec("LOG OUT", secondary = true) { logout() }
                    )
                )
            }
            AccountState.NeedsUsername -> {
                addCenteredButtons(
                    panelX, panelY, panelW, panelH,
                    listOf(
                        ButtonSpec("ENTER USERNAME") { promptUsername() },
                        ButtonSpec("CONFIRM USERNAME", enabled = pendingUsername.isNotBlank()) {
                            reserveUsername()
                        },
                        ButtonSpec("LOG OUT", secondary = true) { logout() }
                    )
                )
            }
            AccountState.SignedIn,
            AccountState.Syncing,
            AccountState.Deleting -> buildSignedInButtons(panelX, panelY, panelW, panelH, busy)
            is AccountState.RecoverableError -> {
                if (status.profile != null) {
                    buildSignedInButtons(panelX, panelY, panelW, panelH, false)
                } else {
                    addCenteredButtons(
                        panelX, panelY, panelW, panelH,
                        listOf(
                            ButtonSpec("TRY PROFILE REFRESH") { refreshProfile() },
                            ButtonSpec("LOG OUT", secondary = true) { logout() }
                        )
                    )
                }
            }
            else -> Unit
        }
    }

    private fun buildSignedOutButtons(
        panelX: Float,
        panelY: Float,
        panelW: Float,
        panelH: Float,
        busy: Boolean
    ) {
        val specs = mutableListOf<ButtonSpec>()
        if (game.accountManager.capabilities.playGamesLogin) {
            specs += ButtonSpec("CONTINUE WITH PLAY GAMES", enabled = !busy) { loginPlayGames() }
        }
        if (game.accountManager.capabilities.emailLogin) {
            specs += ButtonSpec("SIGN IN WITH EMAIL", enabled = !busy) { startEmailLogin() }
            specs += ButtonSpec("REGISTER WITH EMAIL", enabled = !busy, secondary = true) {
                startEmailRegistration()
            }
            specs += ButtonSpec("RESET PASSWORD", enabled = !busy, secondary = true) {
                startPasswordReset()
            }
            specs += ButtonSpec("PRIVACY POLICY", enabled = !busy, secondary = true) {
                Gdx.net.openURI("https://lunarpixelgames.github.io/RhythmicRush/PRIVACY")
            }
        }
        addCenteredButtons(panelX, panelY, panelW, panelH, specs)
    }

    private fun buildSignedInButtons(
        panelX: Float,
        panelY: Float,
        panelW: Float,
        panelH: Float,
        busy: Boolean
    ) {
        val dashboard = dashboardLayout(panelX, panelY, panelW, panelH)
        val gap = 16f
        val h = 62f
        val labels = mutableListOf<ButtonSpec>()
        val profile = accountStatus?.profile
        if (
            profile != null &&
            game.accountManager.capabilities.providerLinking &&
            game.accountManager.capabilities.emailLogin &&
            LinkedProvider.EMAIL_PASSWORD !in profile.providers
        ) {
            labels += ButtonSpec("LINK EMAIL", enabled = !busy, secondary = true) {
                startEmailLink()
            }
        }
        if (
            profile != null &&
            game.accountManager.capabilities.providerLinking &&
            game.accountManager.capabilities.playGamesLogin &&
            LinkedProvider.PLAY_GAMES !in profile.providers
        ) {
            labels += ButtonSpec("LINK PLAY GAMES", enabled = !busy, secondary = true) {
                linkPlayGames()
            }
        }
        labels += listOf(
            ButtonSpec("UPLOAD SAVE", enabled = !busy) { uploadProgress() },
            ButtonSpec("FETCH SAVE", enabled = !busy) { fetchProgress() },
            ButtonSpec(
                "REFRESH",
                enabled = !busy && canRefreshLeaderboard(),
                secondary = true
            ) { fetchLeaderboard(forceRefresh = true) },
            ButtonSpec("LOG OUT", enabled = !busy, secondary = true) { logout() },
            ButtonSpec("DELETE ACCOUNT", enabled = !busy, danger = true) { confirmDelete() }
        )
        val w = dashboard.rightW - 48f
        val startY = dashboard.rightY + dashboard.contentH - 162f
        labels.forEachIndexed { index, spec ->
            buttons += HubButton(
                dashboard.rightX + 24f,
                startY - index * (h + gap),
                w,
                h,
                spec.label,
                spec.enabled,
                spec.secondary,
                spec.danger,
                spec.action
            )
        }

        val snapshot = game.accountManager.cachedLeaderboard
        val rowsPerPage = leaderboardRowsPerPage(dashboard)
        val maxPage = snapshot?.entries?.let {
            maxLeaderboardPage(it.size, rowsPerPage)
        } ?: 0
        val pageButtonW = 176f
        val pageY = dashboard.centerY + 18f
        buttons += HubButton(
            dashboard.centerX + 28f,
            pageY,
            pageButtonW,
            50f,
            "PREVIOUS",
            enabled = leaderboardPage > 0,
            secondary = true
        ) {
            leaderboardPage = (leaderboardPage - 1).coerceAtLeast(0)
            rebuildButtons()
        }
        buttons += HubButton(
            dashboard.centerX + dashboard.centerW - pageButtonW - 28f,
            pageY,
            pageButtonW,
            50f,
            "NEXT",
            enabled = leaderboardPage < maxPage,
            secondary = true
        ) {
            leaderboardPage = (leaderboardPage + 1).coerceAtMost(maxPage)
            rebuildButtons()
        }
    }

    private fun addCenteredButtons(
        panelX: Float,
        panelY: Float,
        panelW: Float,
        panelH: Float,
        specs: List<ButtonSpec>
    ) {
        val w = min(panelW * 0.42f, 560f)
        val h = 72f
        val gap = 22f
        val total = specs.size * h + (specs.size - 1) * gap
        val startY = panelY + panelH * 0.40f + total / 2f - h
        specs.forEachIndexed { index, spec ->
            buttons += HubButton(
                panelX + panelW / 2f - w / 2f,
                startY - index * (h + gap),
                w,
                h,
                spec.label,
                spec.enabled,
                spec.secondary,
                spec.danger,
                spec.action
            )
        }
    }

    private fun startEmailLogin() {
        prompt("Email", pendingEmail, "you@example.com", TextInputType.EMAIL) { email ->
            pendingEmail = email.trim()
            prompt("Password", "", "Password", TextInputType.PASSWORD) { password ->
                setBusyMessage("Signing in...")
                game.accountManager.loginEmail(
                    pendingEmail,
                    password.toCharArray(),
                    profileCallback("Signed in.")
                )
            }
        }
    }

    private fun startEmailRegistration() {
        pendingUsername = ""
        prompt("Email", pendingEmail, "you@example.com", TextInputType.EMAIL) { email ->
            pendingEmail = email.trim()
            prompt("Password", "", "At least 6 characters", TextInputType.PASSWORD) { password ->
                prompt("Confirm password", "", "Repeat password", TextInputType.PASSWORD) confirmInput@ { confirm ->
                    if (password != confirm) {
                        showError("Passwords do not match.")
                        return@confirmInput
                    }
                    setBusyMessage("Creating account...")
                    game.accountManager.registerEmail(
                        pendingEmail,
                        password.toCharArray(),
                        profileCallback("Account created. Check your email and spam folder, verify it, then press refresh.")
                    )
                }
            }
        }
    }

    private fun startPasswordReset() {
        prompt("Reset password", pendingEmail, "Email address", TextInputType.EMAIL) { email ->
            pendingEmail = email.trim()
            setBusyMessage("Sending reset email...")
            game.accountManager.sendPasswordReset(pendingEmail, unitCallback("If the address exists, a reset email was sent."))
        }
    }

    private fun startEmailLink() {
        prompt("Link email", pendingEmail, "you@example.com", TextInputType.EMAIL) { email ->
            pendingEmail = email.trim()
            prompt("Password", "", "At least 6 characters", TextInputType.PASSWORD) { password ->
                prompt(
                    "Confirm password",
                    "",
                    "Repeat password",
                    TextInputType.PASSWORD
                ) confirmInput@ { confirm ->
                    if (password != confirm) {
                        showError("Passwords do not match.")
                        return@confirmInput
                    }
                    setBusyMessage("Linking email login...")
                    game.accountManager.linkEmail(
                        pendingEmail,
                        password.toCharArray(),
                        profileCallback(
                            "Email login linked. Check your inbox and spam folder to verify it."
                        )
                    )
                }
            }
        }
    }

    private fun loginPlayGames() {
        setBusyMessage("Opening Play Games...")
        game.accountManager.loginPlayGames(profileCallback("Connected with Play Games."))
    }

    private fun linkPlayGames() {
        setBusyMessage("Linking Play Games...")
        game.accountManager.linkPlayGames(profileCallback("Play Games linked."))
    }

    private fun resendVerification() {
        setBusyMessage("Sending verification email...")
        game.accountManager.sendEmailVerification(unitCallback("Verification email sent."))
    }

    private fun refreshProfile() {
        setBusyMessage("Refreshing account...")
        game.accountManager.refreshProfile(object : AccountCallback<AccountProfile> {
            override fun onSuccess(value: AccountProfile) {
                if (value.emailVerified && value.username.isNullOrBlank()) {
                    pendingUsername = ""
                    showSuccess("Email verified. Choose a username to finish setup.")
                    return
                }
                showSuccess("Account refreshed.")
            }

            override fun onFailure(error: AccountOperationError) = showError(error.userMessage)
        })
    }

    private fun promptUsername(after: (() -> Unit)? = null) {
        prompt(
            "Username",
            pendingUsername,
            "3-20 letters, numbers, underscore",
            TextInputType.DEFAULT
        ) {
            val clean = it.trim()
            if (!clean.matches(Regex("[A-Za-z0-9_]{3,20}"))) {
                showError("Username must be 3-20 letters, numbers, or underscores.")
            } else {
                pendingUsername = clean
                rebuildButtons()
                after?.invoke()
            }
        }
    }

    private fun reserveUsername() {
        if (pendingUsername.isBlank()) return
        val profile = accountStatus?.profile
        if (profile?.email != null && !profile.emailVerified) {
            pendingUsername = ""
            showError("Verify your email before choosing a username.")
            return
        }
        setBusyMessage("Saving username...")
        game.accountManager.reserveUsername(
            pendingUsername,
            profileCallback("Username saved.")
        )
    }

    private fun logout() {
        setBusyMessage("Signing out...")
        game.accountManager.logout(unitCallback("Signed out."))
    }

    private fun uploadProgress() {
        val request = SyncRequest(
            schemaVersion = ProgressManager.CURRENT_SCHEMA,
            contentVersion = 1,
            deviceId = game.accountManager.deviceId,
            idempotencyKey = UUID.randomUUID().toString(),
            lastKnownRevision = game.accountManager.lastCloudRevision,
            levels = game.progressManager.exportForSync(),
            legacyCoinFloor = game.progressManager.coins
        )
        setBusyMessage("Uploading progress...")
        game.accountManager.uploadProgress(request, object : AccountCallback<SyncResult> {
            override fun onSuccess(value: SyncResult) = showSuccess("Progress uploaded.")
            override fun onFailure(error: AccountOperationError) =
                showError("${error.userMessage} Saved safely for retry.")
        })
    }

    private fun fetchProgress() {
        setBusyMessage("Fetching progress...")
        game.accountManager.fetchProgress(object : AccountCallback<CloudProgress> {
            override fun onSuccess(value: CloudProgress) {
                game.progressManager.mergeCloud(value)
                showSuccess("Cloud progress merged safely.")
            }

            override fun onFailure(error: AccountOperationError) = showError(error.userMessage)
        })
    }

    private fun fetchLeaderboard(forceRefresh: Boolean = true) {
        setBusyMessage("Refreshing leaderboard...")
        game.accountManager.fetchLeaderboard(forceRefresh, object : AccountCallback<LeaderboardSnapshot> {
            override fun onSuccess(value: LeaderboardSnapshot) {
                val rowsPerPage = leaderboardRowsPerPage(currentSignedInDashboardLayout())
                leaderboardPage = leaderboardPage.coerceAtMost(
                    maxLeaderboardPage(value.entries.size, rowsPerPage)
                )
                showSuccess(if (forceRefresh) "Leaderboard updated." else "Leaderboard loaded.")
            }
            override fun onFailure(error: AccountOperationError) = showError(error.userMessage)
        })
    }

    private fun confirmDelete() {
        val expected = accountStatus?.profile?.username ?: "DELETE"
        if (deleteConfirmStage == 0) {
            deleteConfirmStage = 1
            showError("Deleting is permanent. Press DELETE ACCOUNT again to continue.")
            return
        }
        if (deleteConfirmStage == 2) {
            prompt(
                "Final delete",
                "",
                "Type DELETE",
                TextInputType.DEFAULT
            ) finalInput@ {
                if (it.trim() != "DELETE") {
                    deleteConfirmStage = 0
                    showError("Final confirmation did not match.")
                    return@finalInput
                }
                deleteConfirmStage = 0
                setBusyMessage("Deleting account...")
                game.accountManager.deleteAccount(
                    unitCallback("Account deleted. Username is available again. Local progress was kept.")
                )
            }
            return
        }
        prompt(
            "Delete account",
            "",
            "Type $expected",
            TextInputType.DEFAULT
        ) deleteInput@ {
            if (it.trim() != expected) {
                showError("Confirmation did not match.")
                return@deleteInput
            }
            deleteConfirmStage = 2
            showError("Final warning. Press DELETE ACCOUNT once more, then type DELETE.")
        }
    }

    private fun profileCallback(success: String) = object : AccountCallback<AccountProfile> {
        override fun onSuccess(value: AccountProfile) = showSuccess(success)
        override fun onFailure(error: AccountOperationError) = showError(error.userMessage)
    }

    private fun unitCallback(success: String) = object : AccountCallback<Unit> {
        override fun onSuccess(value: Unit) = showSuccess(success)
        override fun onFailure(error: AccountOperationError) = showError(error.userMessage)
    }

    private fun prompt(
        title: String,
        current: String,
        hint: String,
        type: TextInputType,
        accepted: (String) -> Unit
    ) {
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            promptState = PromptState(
                title = title,
                hint = hint,
                type = type,
                value = StringBuilder(current),
                openedAt = System.currentTimeMillis(),
                accepted = accepted
            )
        } else {
            game.textInputController.request(title, current, hint, type, accepted)
        }
    }

    private fun drawPrompt() {
        val prompt = promptState ?: return
        val canContinue = prompt.value.isNotBlank()
        val boxW = min(viewport.worldWidth * 0.48f, 760f)
        val boxH = 330f
        val boxX = (viewport.worldWidth - boxW) / 2f
        val boxY = (viewport.worldHeight - boxH) / 2f
        val fieldX = boxX + 48f
        val fieldY = boxY + 128f
        val fieldW = boxW - 96f
        val fieldH = 72f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.72f)
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        UI.outlined(shapes, boxX, boxY, boxW, boxH, 28f, UI.PANEL_ELEVATED)
        UI.outlined(shapes, fieldX, fieldY, fieldW, fieldH, 14f, UI.BACKGROUND_SECONDARY, UI.BLUE)
        val cancel = promptCancelBounds()
        val confirm = promptConfirmBounds()
        UI.filled(shapes, cancel[0], cancel[1], cancel[2], cancel[3], 14f, UI.PANEL)
        UI.filled(
            shapes,
            confirm[0],
            confirm[1],
            confirm[2],
            confirm[3],
            14f,
            if (canContinue) UI.LIME else Color(UI.TEXT_MUTED.r, UI.TEXT_MUTED.g, UI.TEXT_MUTED.b, 0.45f)
        )
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        val display = when {
            prompt.value.isEmpty() -> prompt.hint
            prompt.type == TextInputType.PASSWORD -> "*".repeat(prompt.value.length)
            else -> prompt.value.toString()
        }
        game.batch.begin()
        drawCentered(bodyFont, prompt.title.uppercase(), boxX + boxW / 2f, boxY + boxH - 54f, UI.YELLOW, 0.78f)
        drawText(
            smallFont,
            display + if (System.currentTimeMillis() / 500L % 2L == 0L) "|" else "",
            fieldX + 20f,
            fieldY + fieldH / 2f + 10f,
            if (prompt.value.isEmpty()) UI.TEXT_MUTED else UI.TEXT,
            0.68f
        )
        drawCentered(smallFont, "CANCEL", cancel[0] + cancel[2] / 2f, cancel[1] + cancel[3] / 2f, UI.TEXT, 0.62f)
        drawCentered(
            smallFont,
            "CONTINUE",
            confirm[0] + confirm[2] / 2f,
            confirm[1] + confirm[3] / 2f,
            if (canContinue) Color.BLACK else UI.TEXT_SECONDARY,
            0.62f
        )
        game.batch.end()
    }

    private fun handlePromptTouch() {
        if (!Gdx.input.justTouched()) return
        val prompt = promptState ?: return
        if (System.currentTimeMillis() - prompt.openedAt < PROMPT_CLICK_GUARD_MS) return
        touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(touch)
        val cancel = promptCancelBounds()
        val confirm = promptConfirmBounds()
        if (hits(touch.x, touch.y, cancel)) {
            promptState = null
        } else if (hits(touch.x, touch.y, confirm)) {
            submitPrompt()
        }
    }

    private fun drawStatusPopup() {
        val popup = statusPopup ?: return
        val boxW = min(viewport.worldWidth * 0.42f, 720f)
        val boxH = min(viewport.worldHeight * 0.25f, 300f).coerceAtLeast(240f)
        val boxX = (viewport.worldWidth - boxW) / 2f
        val boxY = (viewport.worldHeight - boxH) / 2f
        val ok = statusPopupOkBounds()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.62f)
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        UI.outlined(shapes, boxX, boxY, boxW, boxH, 30f, UI.PANEL_ELEVATED, popup.color, 2f)
        UI.filled(shapes, ok[0], ok[1], ok[2], ok[3], 16f, UI.LIME)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        drawCentered(bodyFont, popup.title, boxX + boxW / 2f, boxY + boxH - 66f, popup.color, 0.86f)
        drawCenteredFit(
            smallFont,
            popup.message,
            boxX + boxW / 2f,
            boxY + boxH / 2f + 6f,
            UI.TEXT,
            0.74f,
            0.48f,
            boxW - 84f
        )
        drawCentered(smallFont, "OK", ok[0] + ok[2] / 2f, ok[1] + ok[3] / 2f, Color.BLACK, 0.70f)
        game.batch.end()
    }

    private fun handleStatusPopupTouch() {
        if (!Gdx.input.justTouched()) return
        touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(touch)
        if (hits(touch.x, touch.y, statusPopupOkBounds())) {
            statusPopup = null
        }
    }

    private fun statusPopupOkBounds(): FloatArray {
        val boxW = min(viewport.worldWidth * 0.42f, 720f)
        val boxH = min(viewport.worldHeight * 0.25f, 300f).coerceAtLeast(240f)
        val boxX = (viewport.worldWidth - boxW) / 2f
        val boxY = (viewport.worldHeight - boxH) / 2f
        val okW = min(boxW * 0.38f, 240f)
        val okH = 62f
        return floatArrayOf(boxX + boxW / 2f - okW / 2f, boxY + 32f, okW, okH)
    }

    private fun promptCancelBounds(): FloatArray {
        val boxW = min(viewport.worldWidth * 0.48f, 760f)
        val boxH = 330f
        val boxX = (viewport.worldWidth - boxW) / 2f
        val boxY = (viewport.worldHeight - boxH) / 2f
        return floatArrayOf(boxX + 48f, boxY + 38f, (boxW - 114f) / 2f, 62f)
    }

    private fun promptConfirmBounds(): FloatArray {
        val cancel = promptCancelBounds()
        return floatArrayOf(cancel[0] + cancel[2] + 18f, cancel[1], cancel[2], cancel[3])
    }

    private fun hits(x: Float, y: Float, bounds: FloatArray): Boolean =
        x >= bounds[0] && x <= bounds[0] + bounds[2] &&
            y >= bounds[1] && y <= bounds[1] + bounds[3]

    private fun submitPrompt() {
        val prompt = promptState ?: return
        val value = prompt.value.toString()
        if (value.isBlank()) {
            showError("Enter a value before continuing.")
            return
        }
        promptState = null
        prompt.accepted(value)
    }

    private fun showSuccess(message: String) {
        statusMessage = ""
        statusPopup = StatusPopup("SUCCESS", message, UI.LIME)
        rebuildButtons()
    }

    private fun showError(message: String) {
        statusMessage = ""
        statusPopup = StatusPopup("NOTICE", message, UI.DANGER)
        rebuildButtons()
    }

    private fun setBusyMessage(message: String) {
        statusMessage = message
        statusColor = UI.TEXT_SECONDARY
        statusPopup = null
        rebuildButtons()
    }

    private fun dashboardLayout(
        panelX: Float,
        panelY: Float,
        panelW: Float,
        panelH: Float
    ): DashboardLayout {
        val gap = 22f
        val sideW = 294f
        val contentY = panelY + 48f
        val contentH = panelH - 136f
        val leftX = panelX
        val centerX = leftX + sideW + gap
        val centerW = panelW - sideW * 2f - gap * 2f
        val rightX = centerX + centerW + gap
        return DashboardLayout(
            leftX = leftX,
            centerX = centerX,
            rightX = rightX,
            leftY = contentY,
            centerY = contentY,
            rightY = contentY,
            leftW = sideW,
            centerW = centerW,
            rightW = sideW,
            contentH = contentH
        )
    }

    private fun currentSignedInDashboardLayout(): DashboardLayout {
        val panelW = min(viewport.worldWidth - 72f, 1848f)
        val panelH = min(viewport.worldHeight - 64f, 1016f)
        val panelX = (viewport.worldWidth - panelW) / 2f
        val panelY = (viewport.worldHeight - panelH) / 2f
        return dashboardLayout(panelX, panelY, panelW, panelH)
    }

    private fun leaderboardRowsPerPage(dashboard: DashboardLayout): Int {
        val firstRowTopPadding = 216f
        val bottomReserved = 112f
        return (((dashboard.contentH - firstRowTopPadding - bottomReserved) / LEADERBOARD_ROW_HEIGHT).toInt() + 1)
            .coerceAtLeast(1)
    }

    private fun leaderboardRowTextY(dashboard: DashboardLayout, index: Int): Float =
        dashboard.centerY + dashboard.contentH - 194f - index * LEADERBOARD_ROW_HEIGHT

    private fun leaderboardRowHighlightY(dashboard: DashboardLayout, index: Int): Float =
        leaderboardRowTextY(dashboard, index) - 48f

    private fun maxLeaderboardPage(entryCount: Int, rowsPerPage: Int): Int {
        if (entryCount <= 0) return 0
        return ((entryCount - 1) / rowsPerPage.coerceAtLeast(1)).coerceAtLeast(0)
    }

    private fun drawDashboardShapes(dashboard: DashboardLayout) {
        val panelColor = Color(UI.PANEL.r, UI.PANEL.g, UI.PANEL.b, 0.96f)
        drawPanelWithShadow(
            shapes,
            dashboard.leftX,
            dashboard.leftY,
            dashboard.leftW,
            dashboard.contentH,
            30f,
            panelColor
        )
        drawPanelWithShadow(
            shapes,
            dashboard.centerX,
            dashboard.centerY,
            dashboard.centerW,
            dashboard.contentH,
            42f,
            panelColor
        )
        drawPanelWithShadow(
            shapes,
            dashboard.rightX,
            dashboard.rightY,
            dashboard.rightW,
            dashboard.contentH,
            30f,
            panelColor
        )

        val snapshot = game.accountManager.cachedLeaderboard ?: return
        val rowsPerPage = leaderboardRowsPerPage(dashboard)
        leaderboardPage = leaderboardPage.coerceIn(0, maxLeaderboardPage(snapshot.entries.size, rowsPerPage))
        val first = leaderboardPage * rowsPerPage
        val rowX = dashboard.centerX + 24f
        val rowW = dashboard.centerW - 48f
        snapshot.entries.drop(first).take(rowsPerPage).forEachIndexed { index, entry ->
            val rowY = leaderboardRowHighlightY(dashboard, index)
            val fill = if (entry.currentPlayer) {
                Color(UI.PANEL_ELEVATED.r, UI.PANEL_ELEVATED.g, UI.PANEL_ELEVATED.b, 0.98f)
            } else if (index % 2 == 0) {
                Color(UI.PANEL_ELEVATED.r, UI.PANEL_ELEVATED.g, UI.PANEL_ELEVATED.b, 0.78f)
            } else {
                Color(
                    UI.BACKGROUND_SECONDARY.r,
                    UI.BACKGROUND_SECONDARY.g,
                    UI.BACKGROUND_SECONDARY.b,
                    0.62f
                )
            }
            if (entry.currentPlayer) {
                UI.outlined(
                    shapes,
                    rowX,
                    rowY,
                    rowW,
                    LEADERBOARD_ROW_HEIGHT - 14f,
                    18f,
                    fill,
                    UI.LIME,
                    2f
                )
            } else {
                UI.filled(
                    shapes,
                    rowX,
                    rowY,
                    rowW,
                    LEADERBOARD_ROW_HEIGHT - 14f,
                    18f,
                    fill
                )
            }
        }
    }

    private fun drawPanelWithShadow(
        shapes: ShapeRenderer,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        fill: Color
    ) {
        UI.filled(shapes, x + 8f, y - 10f, w, h, radius, Color(0f, 0f, 0f, 0.18f))
        UI.filled(shapes, x + 4f, y - 5f, w, h, radius, Color(0f, 0f, 0f, 0.12f))
        UI.filled(shapes, x, y, w, h, radius, fill)
    }

    private fun drawStat(x: Float, y: Float, width: Float, label: String, value: String, color: Color) {
        drawTextFit(smallFont, label, x, y, UI.TEXT_SECONDARY, 0.76f, 0.56f, width)
        drawTextFit(bodyFont, value, x, y - 48f, color, 1.82f, 0.92f, width)
    }

    private fun drawRightAligned(
        font: BitmapFont,
        text: String,
        rightX: Float,
        y: Float,
        color: Color,
        scale: Float
    ) {
        font.data.setScale(scale)
        layout.setText(font, text)
        drawText(font, text, rightX - layout.width, y, color, scale)
    }

    private fun canRefreshLeaderboard(): Boolean {
        val snapshot = game.accountManager.cachedLeaderboard ?: return true
        return System.currentTimeMillis() >= snapshot.nextRefreshAt
    }

    private fun drawBackground() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        proceduralBackground.render(
            shapes,
            backgroundShape,
            backgroundColor,
            backgroundSeed,
            backgroundScroll,
            0f,
            0f,
            viewport.worldWidth,
            viewport.worldHeight
        )
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawCentered(
        font: BitmapFont,
        text: String,
        centerX: Float,
        centerY: Float,
        color: Color,
        scale: Float
    ) {
        font.data.setScale(scale)
        layout.setText(font, text)
        drawTextWithShadow(font, text, centerX - layout.width / 2f, centerY + layout.height / 2f, color)
    }

    private fun drawCenteredFit(
        font: BitmapFont,
        text: String,
        centerX: Float,
        centerY: Float,
        color: Color,
        preferredScale: Float,
        minimumScale: Float,
        maxWidth: Float
    ) {
        font.data.setScale(preferredScale)
        layout.setText(font, text)
        val scale = if (layout.width > maxWidth && layout.width > 0f) {
            (preferredScale * maxWidth / layout.width).coerceIn(minimumScale, preferredScale)
        } else {
            preferredScale
        }
        drawCentered(font, text, centerX, centerY, color, scale)
    }

    private fun drawText(
        font: BitmapFont,
        text: String,
        x: Float,
        y: Float,
        color: Color,
        scale: Float
    ) {
        font.data.setScale(scale)
        drawTextWithShadow(font, text, x, y, color)
    }

    private fun drawTextFit(
        font: BitmapFont,
        text: String,
        x: Float,
        y: Float,
        color: Color,
        preferredScale: Float,
        minimumScale: Float,
        maxWidth: Float
    ) {
        font.data.setScale(preferredScale)
        layout.setText(font, text)
        val scale = if (layout.width > maxWidth && layout.width > 0f) {
            (preferredScale * maxWidth / layout.width).coerceIn(minimumScale, preferredScale)
        } else {
            preferredScale
        }
        drawText(font, text, x, y, color, scale)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        rebuildButtons()
    }

    override fun hide() {
        game.accountManager.removeListener(accountListener)
        if (Gdx.input.inputProcessor === this) {
            Gdx.input.inputProcessor = previousInputProcessor
        }
    }

    override fun dispose() {
        game.accountManager.removeListener(accountListener)
        if (Gdx.input.inputProcessor === this) {
            Gdx.input.inputProcessor = previousInputProcessor
        }
        shapes.dispose()
    }

    override fun keyDown(keycode: Int): Boolean {
        if (statusPopup != null) {
            when (keycode) {
                Input.Keys.ESCAPE,
                Input.Keys.ENTER,
                Input.Keys.NUMPAD_ENTER,
                Input.Keys.SPACE -> {
                    statusPopup = null
                    return true
                }
            }
            return true
        }
        val prompt = promptState ?: return false
        when (keycode) {
            Input.Keys.BACKSPACE -> {
                if (prompt.value.isNotEmpty()) prompt.value.deleteCharAt(prompt.value.lastIndex)
                return true
            }
            Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER -> {
                submitPrompt()
                return true
            }
            Input.Keys.ESCAPE -> {
                promptState = null
                return true
            }
            Input.Keys.V -> {
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
                ) {
                    val pasted = Gdx.app.clipboard.contents.orEmpty()
                    prompt.value.append(pasted.filter { it >= ' ' }.take(256))
                    return true
                }
            }
        }
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        if (statusPopup != null) return true
        val prompt = promptState ?: return false
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
            Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
        ) return true
        if (character >= ' ' && character != '\u007F' && prompt.value.length < 256) {
            prompt.value.append(character)
            return true
        }
        return false
    }

    override fun keyUp(keycode: Int) = false
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false
    override fun mouseMoved(screenX: Int, screenY: Int) = false
    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        val entries = game.accountManager.cachedLeaderboard?.entries ?: return false
        val rowsPerPage = leaderboardRowsPerPage(currentSignedInDashboardLayout())
        val maxPage = maxLeaderboardPage(entries.size, rowsPerPage)
        if (amountY > 0f) {
            leaderboardPage = (leaderboardPage + 1).coerceAtMost(maxPage)
        } else if (amountY < 0f) {
            leaderboardPage = (leaderboardPage - 1).coerceAtLeast(0)
        } else {
            return false
        }
        rebuildButtons()
        return true
    }

    private data class ButtonSpec(
        val label: String,
        val enabled: Boolean = true,
        val secondary: Boolean = false,
        val danger: Boolean = false,
        val action: () -> Unit
    )

    private data class HubButton(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val label: String,
        val enabled: Boolean = true,
        val secondary: Boolean = false,
        val danger: Boolean = false,
        val action: () -> Unit
    ) {
        fun hits(px: Float, py: Float): Boolean =
            px >= x && px <= x + w && py >= y && py <= y + h
    }

    private data class PromptState(
        val title: String,
        val hint: String,
        val type: TextInputType,
        val value: StringBuilder,
        val openedAt: Long,
        val accepted: (String) -> Unit
    )

    private data class StatusPopup(
        val title: String,
        val message: String,
        val color: Color
    )

    private data class DashboardLayout(
        val leftX: Float,
        val centerX: Float,
        val rightX: Float,
        val leftY: Float,
        val centerY: Float,
        val rightY: Float,
        val leftW: Float,
        val centerW: Float,
        val rightW: Float,
        val contentH: Float
    )

    companion object {
        private const val PROMPT_CLICK_GUARD_MS = 300L
        private const val LEADERBOARD_ROW_HEIGHT = 102f
    }
}
