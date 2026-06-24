package io.github.msameer0.rhythmicrush.lwjgl3.account

import com.badlogic.gdx.utils.JsonValue
import io.github.msameer0.rhythmicrush.account.AccountCallback
import io.github.msameer0.rhythmicrush.account.AccountClient
import io.github.msameer0.rhythmicrush.account.AccountConfiguration
import io.github.msameer0.rhythmicrush.account.AccountErrorCode
import io.github.msameer0.rhythmicrush.account.AccountMergePreview
import io.github.msameer0.rhythmicrush.account.AccountOperation
import io.github.msameer0.rhythmicrush.account.AccountOperationError
import io.github.msameer0.rhythmicrush.account.AccountProfile
import io.github.msameer0.rhythmicrush.account.CloudProgress
import io.github.msameer0.rhythmicrush.account.LeaderboardEntry
import io.github.msameer0.rhythmicrush.account.LeaderboardSnapshot
import io.github.msameer0.rhythmicrush.account.LevelCloudProgress
import io.github.msameer0.rhythmicrush.account.PlatformAccountCapabilities
import io.github.msameer0.rhythmicrush.account.SimpleAccountOperation
import io.github.msameer0.rhythmicrush.account.SyncRequest
import io.github.msameer0.rhythmicrush.account.SyncResult
import io.github.msameer0.rhythmicrush.account.deliverFailure
import io.github.msameer0.rhythmicrush.account.deliverSuccess
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DesktopAccountClient(
    private val configuration: AccountConfiguration,
    private val tokenStore: DesktopSecureTokenStore = DesktopSecureTokenStore.create()
) : AccountClient {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "rhythmic-rush-desktop-account").apply { isDaemon = true }
    }
    private val firebase = DesktopFirebaseAuth(configuration)
    private val backend = DesktopBackendClient(configuration.backendBaseUrl)
    private val tokenKey =
        "${configuration.environment}:${configuration.firebaseProjectId}:refresh-token"

    @Volatile
    private var session: DesktopSession? = null

    override val capabilities = PlatformAccountCapabilities(
        playGamesLogin = false,
        emailLogin = true,
        secureTokenStorage = tokenStore.persistent,
        appCheck = false,
        providerLinking = true
    )

    override fun restoreSession(callback: AccountCallback<AccountProfile?>): AccountOperation =
        execute(callback) {
            val stored = tokenStore.load(tokenKey)
            if (stored == null) {
                null
            } else {
                try {
                    val refreshed = firebase.refresh(stored)
                    updateSession(refreshed)
                    profile(refreshed)
                } catch (exception: DesktopAuthException) {
                    if (exception.error.code == AccountErrorCode.AUTH_INVALID) {
                        clearSession()
                        null
                    } else {
                        throw exception
                    }
                }
            }
        }

    override fun registerEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation {
        val passwordText = String(password)
        password.fill('\u0000')
        return execute(callback) {
            val created = firebase.register(email.trim(), passwordText)
            updateSession(created)
            runCatching { firebase.sendVerification(created.idToken) }
            profile(created)
        }
    }

    override fun loginEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation {
        val passwordText = String(password)
        password.fill('\u0000')
        return execute(callback) {
            val loggedIn = firebase.login(email.trim(), passwordText)
            updateSession(loggedIn)
            profile(loggedIn)
        }
    }

    override fun loginPlayGames(callback: AccountCallback<AccountProfile>): AccountOperation =
        unavailable(callback, "Play Games login is available on Android.")

    override fun logout(callback: AccountCallback<Unit>): AccountOperation {
        val operation = SimpleAccountOperation()
        clearSession()
        deliverSuccess(operation, callback, Unit)
        return operation
    }

    override fun sendEmailVerification(callback: AccountCallback<Unit>): AccountOperation =
        execute(callback) {
            firebase.sendVerification(requireSession().idToken)
        }

    override fun refreshProfile(callback: AccountCallback<AccountProfile>): AccountOperation =
        execute(callback) {
            val active = refreshSession(force = true)
            profile(active)
        }

    override fun sendPasswordReset(
        email: String,
        callback: AccountCallback<Unit>
    ): AccountOperation = execute(callback) {
        firebase.sendPasswordReset(email.trim())
    }

    override fun reserveUsername(
        username: String,
        idempotencyKey: String,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation = execute(callback) {
        val active = refreshSession(force = true)
        backend.request(
            "POST",
            "/v1/usernames/reserve",
            active.idToken,
            JsonPayload.encode(mapOf("username" to username)),
            idempotencyKey
        )
        profile(active)
    }

    override fun linkEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation {
        val passwordText = String(password)
        password.fill('\u0000')
        return execute(callback) {
            val linked = firebase.linkEmail(
                refreshSession().idToken,
                email.trim(),
                passwordText
            )
            updateSession(linked)
            runCatching { firebase.sendVerification(linked.idToken) }
            profile(linked)
        }
    }

    override fun linkPlayGames(callback: AccountCallback<AccountProfile>): AccountOperation =
        unavailable(callback, "Link Play Games from the Android version.")

    override fun fetchProgress(callback: AccountCallback<CloudProgress>): AccountOperation =
        execute(callback) {
            parseProgress(
                backend.request(
                    "POST",
                    "/v1/progress/fetch",
                    refreshSession().idToken,
                    "{}"
                )
            )
        }

    override fun uploadProgress(
        request: SyncRequest,
        callback: AccountCallback<SyncResult>
    ): AccountOperation = execute(callback) {
        val levels = request.levels.map {
            mapOf(
                "levelId" to it.levelId,
                "bestPercent" to it.bestPercent,
                "totalAttempts" to it.totalAttempts,
                "completed" to it.completed
            )
        }
        val body = linkedMapOf<String, Any?>(
            "schemaVersion" to request.schemaVersion,
            "contentVersion" to request.contentVersion,
            "deviceId" to request.deviceId,
            "lastKnownRevision" to request.lastKnownRevision,
            "levels" to levels
        )
        request.legacyCoinFloor?.let { body["legacyCoinFloor"] = it }
        val response = backend.request(
            "POST",
            "/v1/progress/upload",
            refreshSession().idToken,
            JsonPayload.encode(body),
            request.idempotencyKey
        )
        SyncResult(
            progress = parseProgress(response.get("progress")),
            levelsImproved = response.getInt("levelsImproved", 0),
            attemptsAdded = response.getInt("attemptsAdded", 0),
            rewardsReconciled = response.getBoolean("rewardsReconciled", false)
        )
    }

    override fun fetchLeaderboard(
        forceRefresh: Boolean,
        callback: AccountCallback<LeaderboardSnapshot>
    ): AccountOperation = execute(callback) {
        parseLeaderboard(
            backend.request(
                "GET",
                "/v1/leaderboard?refresh=$forceRefresh",
                refreshSession().idToken
            )
        )
    }

    override fun beginMerge(
        secondaryCredential: String,
        callback: AccountCallback<AccountMergePreview>
    ): AccountOperation = unavailable(callback, "Account merge is not available yet.")

    override fun confirmMerge(
        ticketId: String,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation = unavailable(callback, "Account merge is not available yet.")

    override fun deleteAccount(callback: AccountCallback<Unit>): AccountOperation =
        execute(callback) {
            val active = refreshSession()
            backend.request(
                "DELETE",
                "/v1/account",
                active.idToken,
                """{"confirm":true}"""
            )
            try {
                firebase.delete(active.idToken)
            } catch (_: Exception) {
                // Cloud data is already wiped. Sign out locally even if Firebase refuses deletion.
            }
            clearSession()
        }

    override fun dispose() {
        executor.shutdownNow()
    }

    private fun profile(active: DesktopSession): AccountProfile {
        val backendProfile = backend.request("GET", "/v1/profile", active.idToken)
        val username = backendProfile.getString("username", null)
        return firebase.lookup(active.idToken, username)
    }

    @Synchronized
    private fun requireSession(): DesktopSession {
        val active = session ?: throw DesktopAuthException(
            AccountOperationError(AccountErrorCode.AUTH_REQUIRED, "Please sign in first.")
        )
        return if (active.needsRefresh()) refreshSession() else active
    }

    @Synchronized
    private fun refreshSession(force: Boolean = false): DesktopSession {
        val current = session ?: tokenStore.load(tokenKey)?.let {
            DesktopSession("", it, "", 0L)
        } ?: throw DesktopAuthException(
            AccountOperationError(AccountErrorCode.AUTH_REQUIRED, "Please sign in first.")
        )
        if (!force && !current.needsRefresh() && current.idToken.isNotBlank()) return current
        val refreshed = firebase.refresh(current.refreshToken)
        updateSession(refreshed)
        return refreshed
    }

    @Synchronized
    private fun updateSession(value: DesktopSession) {
        session = value
        tokenStore.save(tokenKey, value.refreshToken)
    }

    @Synchronized
    private fun clearSession() {
        session = null
        tokenStore.delete(tokenKey)
    }

    private fun parseProgress(data: JsonValue?): CloudProgress {
        requireNotNull(data) { "Progress response is missing." }
        val levels = mutableListOf<LevelCloudProgress>()
        var level = data.get("levels")?.child
        while (level != null) {
            levels.add(
                LevelCloudProgress(
                    levelId = level.getString("levelId"),
                    bestPercent = level.getInt("bestPercent", 0),
                    totalAttempts = level.getInt("attempts", 0),
                    completed = level.getBoolean("completed", false)
                )
            )
            level = level.next
        }
        return CloudProgress(
            schemaVersion = data.getInt("schemaVersion", 1),
            contentVersion = data.getInt("contentVersion", 1),
            revision = data.getLong("revision", 0L),
            coins = data.getInt("coins", 0),
            points = data.getInt("points", 0),
            completedLevels = data.getInt("completedLevels", 0),
            levels = levels,
            updatedAt = data.getLong("updatedAt", 0L)
        )
    }

    private fun parseLeaderboard(data: JsonValue): LeaderboardSnapshot {
        val entries = mutableListOf<LeaderboardEntry>()
        var entry = data.get("entries")?.child
        while (entry != null) {
            entries.add(parseLeaderboardEntry(entry))
            entry = entry.next
        }
        return LeaderboardSnapshot(
            entries = entries,
            currentPlayer = data.get("currentPlayer")?.let {
                if (it.isNull) null else parseLeaderboardEntry(it, true)
            },
            generatedAt = data.getLong("generatedAt", 0L),
            nextRefreshAt = data.getLong("nextRefreshAt", 0L),
            currentPlayerStatus = data.getString("currentPlayerStatus", "unranked")
        )
    }

    private fun parseLeaderboardEntry(
        data: JsonValue,
        currentPlayer: Boolean = data.getBoolean("currentPlayer", false)
    ) = LeaderboardEntry(
        rank = data.getInt("rank", 0),
        username = data.getString("username", ""),
        points = data.getInt("points", 0),
        completedLevels = data.getInt("completedLevels", 0),
        currentPlayer = currentPlayer
    )

    private fun <T> execute(callback: AccountCallback<T>, action: () -> T): AccountOperation {
        val operation = SimpleAccountOperation()
        executor.execute {
            if (operation.isCancelled) return@execute
            try {
                deliverSuccess(operation, callback, action())
            } catch (throwable: Throwable) {
                deliverFailure(operation, callback, mapError(throwable))
            }
        }
        return operation
    }

    private fun <T> unavailable(
        callback: AccountCallback<T>,
        message: String
    ): AccountOperation {
        val operation = SimpleAccountOperation()
        deliverFailure(
            operation,
            callback,
            AccountOperationError(AccountErrorCode.UNAVAILABLE, message)
        )
        return operation
    }

    private fun mapError(throwable: Throwable): AccountOperationError = when (throwable) {
        is DesktopAuthException -> throwable.error
        is DesktopBackendException -> throwable.error
        is IOException -> AccountOperationError(
            AccountErrorCode.NETWORK,
            "Could not reach the account service.",
            retryable = true,
            diagnosticMessage = throwable.javaClass.simpleName
        )
        else -> AccountOperationError(
            AccountErrorCode.UNKNOWN,
            throwable.message ?: "The account operation failed.",
            diagnosticMessage = throwable.javaClass.simpleName
        )
    }
}
