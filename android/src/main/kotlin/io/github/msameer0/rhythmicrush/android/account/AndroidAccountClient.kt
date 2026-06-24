package io.github.msameer0.rhythmicrush.android.account

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.PlayGames
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PlayGamesAuthProvider
import io.github.msameer0.rhythmicrush.account.AccountCallback
import io.github.msameer0.rhythmicrush.account.AccountClient
import io.github.msameer0.rhythmicrush.account.AccountErrorCode
import io.github.msameer0.rhythmicrush.account.AccountMergePreview
import io.github.msameer0.rhythmicrush.account.AccountOperation
import io.github.msameer0.rhythmicrush.account.AccountOperationError
import io.github.msameer0.rhythmicrush.account.AccountProfile
import io.github.msameer0.rhythmicrush.account.CloudProgress
import io.github.msameer0.rhythmicrush.account.LeaderboardEntry
import io.github.msameer0.rhythmicrush.account.LeaderboardSnapshot
import io.github.msameer0.rhythmicrush.account.LevelCloudProgress
import io.github.msameer0.rhythmicrush.account.LinkedProvider
import io.github.msameer0.rhythmicrush.account.PlatformAccountCapabilities
import io.github.msameer0.rhythmicrush.account.SimpleAccountOperation
import io.github.msameer0.rhythmicrush.account.SyncRequest
import io.github.msameer0.rhythmicrush.account.SyncResult
import io.github.msameer0.rhythmicrush.account.deliverFailure
import io.github.msameer0.rhythmicrush.account.deliverSuccess
import io.github.msameer0.rhythmicrush.android.AndroidLauncher
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class AndroidAccountClient(
    private val activity: AndroidLauncher,
    backendBaseUrl: String,
    private val webClientId: String
) : AccountClient {
    private val auth = FirebaseAuth.getInstance()
    private val appCheck = FirebaseAppCheck.getInstance()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "rhythmic-rush-account").apply { isDaemon = true }
    }
    private val backend = AndroidBackendClient(backendBaseUrl, auth, appCheck, executor)

    override val capabilities = PlatformAccountCapabilities(
        playGamesLogin = true,
        emailLogin = true,
        secureTokenStorage = true,
        appCheck = true,
        providerLinking = true
    )

    override fun restoreSession(callback: AccountCallback<AccountProfile?>): AccountOperation {
        val operation = SimpleAccountOperation()
        val user = auth.currentUser
        if (user == null) {
            deliverSuccess(operation, callback, null)
        } else {
            loadProfile(operation, object : AccountCallback<AccountProfile> {
                override fun onSuccess(value: AccountProfile) {
                    deliverSuccess(operation, callback, value)
                }

                override fun onFailure(error: AccountOperationError) {
                    deliverFailure(operation, callback, error)
                }
            })
        }
        return operation
    }

    override fun registerEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation {
        val operation = SimpleAccountOperation()
        val passwordText = String(password)
        password.fill('\u0000')
        auth.createUserWithEmailAndPassword(email.trim(), passwordText)
            .addOnSuccessListener {
                auth.currentUser?.sendEmailVerification()
                loadProfile(operation, callback)
            }
            .addOnFailureListener { fail(operation, callback, it) }
        return operation
    }

    override fun loginEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation {
        val operation = SimpleAccountOperation()
        val passwordText = String(password)
        password.fill('\u0000')
        auth.signInWithEmailAndPassword(email.trim(), passwordText)
            .addOnSuccessListener { loadProfile(operation, callback) }
            .addOnFailureListener { fail(operation, callback, it) }
        return operation
    }

    override fun loginPlayGames(callback: AccountCallback<AccountProfile>): AccountOperation =
        playGamesCredential(link = false, callback = callback)

    override fun logout(callback: AccountCallback<Unit>): AccountOperation {
        val operation = SimpleAccountOperation()
        auth.signOut()
        deliverSuccess(operation, callback, Unit)
        return operation
    }

    override fun sendEmailVerification(callback: AccountCallback<Unit>): AccountOperation {
        val operation = SimpleAccountOperation()
        val user = auth.currentUser
        if (user == null) {
            deliverFailure(operation, callback, authRequired())
            return operation
        }
        user.sendEmailVerification()
            .addOnSuccessListener { deliverSuccess(operation, callback, Unit) }
            .addOnFailureListener { fail(operation, callback, it) }
        return operation
    }

    override fun refreshProfile(callback: AccountCallback<AccountProfile>): AccountOperation {
        val operation = SimpleAccountOperation()
        val user = auth.currentUser
        if (user == null) {
            deliverFailure(operation, callback, authRequired())
            return operation
        }
        user.reload()
            .addOnSuccessListener { loadProfile(operation, callback) }
            .addOnFailureListener { fail(operation, callback, it) }
        return operation
    }

    override fun sendPasswordReset(
        email: String,
        callback: AccountCallback<Unit>
    ): AccountOperation {
        val operation = SimpleAccountOperation()
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { deliverSuccess(operation, callback, Unit) }
            .addOnFailureListener { fail(operation, callback, it) }
        return operation
    }

    override fun reserveUsername(
        username: String,
        idempotencyKey: String,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation {
        val operation = SimpleAccountOperation()
        backend.request(
            "POST",
            "/v1/usernames/reserve",
            JSONObject().put("username", username),
            idempotencyKey,
            forceRefreshIdToken = true,
            operation = operation
        ) { result ->
            result.fold(
                onSuccess = {
                    val user = auth.currentUser
                    if (user == null) deliverFailure(operation, callback, authRequired())
                    else loadProfile(operation, callback)
                },
                onFailure = { deliverBackendFailure(operation, callback, it) }
            )
        }
        return operation
    }

    override fun linkEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation {
        val operation = SimpleAccountOperation()
        val user = auth.currentUser
        if (user == null) {
            password.fill('\u0000')
            deliverFailure(operation, callback, authRequired())
            return operation
        }
        val passwordText = String(password)
        password.fill('\u0000')
        user.linkWithCredential(EmailAuthProvider.getCredential(email.trim(), passwordText))
            .addOnSuccessListener {
                auth.currentUser?.sendEmailVerification()
                loadProfile(operation, callback)
            }
            .addOnFailureListener { fail(operation, callback, it) }
        return operation
    }

    override fun linkPlayGames(callback: AccountCallback<AccountProfile>): AccountOperation =
        playGamesCredential(link = true, callback = callback)

    override fun fetchProgress(callback: AccountCallback<CloudProgress>): AccountOperation {
        val operation = SimpleAccountOperation()
        backend.request(
            "POST",
            "/v1/progress/fetch",
            JSONObject(),
            operation = operation
        ) { result ->
            result.fold(
                onSuccess = { deliverSuccess(operation, callback, parseProgress(it)) },
                onFailure = { deliverBackendFailure(operation, callback, it) }
            )
        }
        return operation
    }

    override fun uploadProgress(
        request: SyncRequest,
        callback: AccountCallback<SyncResult>
    ): AccountOperation {
        val body = JSONObject()
            .put("schemaVersion", request.schemaVersion)
            .put("contentVersion", request.contentVersion)
            .put("deviceId", request.deviceId)
            .put("lastKnownRevision", request.lastKnownRevision)
            .put("levels", JSONArray(request.levels.map {
                JSONObject()
                    .put("levelId", it.levelId)
                    .put("bestPercent", it.bestPercent)
                    .put("totalAttempts", it.totalAttempts)
                    .put("completed", it.completed)
            }))
        request.legacyCoinFloor?.let { body.put("legacyCoinFloor", it) }

        val operation = SimpleAccountOperation()
        backend.request(
            "POST",
            "/v1/progress/upload",
            body,
            request.idempotencyKey,
            operation = operation
        ) { result ->
            result.fold(
                onSuccess = {
                    deliverSuccess(
                        operation,
                        callback,
                        SyncResult(
                            progress = parseProgress(it.getJSONObject("progress")),
                            levelsImproved = it.optInt("levelsImproved"),
                            attemptsAdded = it.optInt("attemptsAdded"),
                            rewardsReconciled = it.optBoolean("rewardsReconciled")
                        )
                    )
                },
                onFailure = { deliverBackendFailure(operation, callback, it) }
            )
        }
        return operation
    }

    override fun fetchLeaderboard(
        forceRefresh: Boolean,
        callback: AccountCallback<LeaderboardSnapshot>
    ): AccountOperation {
        val operation = SimpleAccountOperation()
        backend.request(
            "GET",
            "/v1/leaderboard?refresh=$forceRefresh",
            operation = operation
        ) { result ->
            result.fold(
                onSuccess = { deliverSuccess(operation, callback, parseLeaderboard(it)) },
                onFailure = { deliverBackendFailure(operation, callback, it) }
            )
        }
        return operation
    }

    override fun beginMerge(
        secondaryCredential: String,
        callback: AccountCallback<AccountMergePreview>
    ): AccountOperation = unsupported(callback, "Account merge is added after both login methods.")

    override fun confirmMerge(
        ticketId: String,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation = unsupported(callback, "Account merge is not available yet.")

    override fun deleteAccount(callback: AccountCallback<Unit>): AccountOperation {
        val operation = SimpleAccountOperation()
        backend.request(
            "DELETE",
            "/v1/account",
            JSONObject().put("confirm", true),
            operation = operation
        ) { result ->
            result.fold(
                onSuccess = {
                    val user = auth.currentUser
                    if (user == null) {
                        auth.signOut()
                        deliverSuccess(operation, callback, Unit)
                    } else {
                        user.delete()
                            .addOnSuccessListener {
                                auth.signOut()
                                deliverSuccess(operation, callback, Unit)
                            }
                            .addOnFailureListener {
                                auth.signOut()
                                deliverSuccess(operation, callback, Unit)
                            }
                    }
                },
                onFailure = { deliverBackendFailure(operation, callback, it) }
            )
        }
        return operation
    }

    override fun dispose() {
        executor.shutdownNow()
    }

    private fun playGamesCredential(
        link: Boolean,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation {
        val operation = SimpleAccountOperation()
        val signInClient = PlayGames.getGamesSignInClient(activity)
        signInClient.signIn()
            .addOnSuccessListener {
                signInClient.requestServerSideAccess(webClientId, false)
                    .addOnSuccessListener serverAccess@ { code ->
                        if (code.isNullOrBlank()) {
                            deliverFailure(
                                operation,
                                callback,
                                AccountOperationError(
                                    AccountErrorCode.AUTH_INVALID,
                                    "Play Games did not return an authentication code."
                                )
                            )
                            return@serverAccess
                        }

                        val credential = PlayGamesAuthProvider.getCredential(code)
                        val task = if (link) {
                            val user = auth.currentUser
                            if (user == null) {
                                deliverFailure(operation, callback, authRequired())
                                return@serverAccess
                            }
                            user.linkWithCredential(credential)
                        } else {
                            auth.signInWithCredential(credential)
                        }
                        task.addOnSuccessListener { loadProfile(operation, callback) }
                            .addOnFailureListener {
                                failWithStage(operation, callback, it, "Firebase Play Games exchange")
                            }
                    }
                    .addOnFailureListener {
                        failWithStage(operation, callback, it, "Play Games server access")
                    }
            }
            .addOnFailureListener {
                failWithStage(operation, callback, it, "Play Games sign-in")
            }
        return operation
    }

    private fun <T> unsupported(
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

    private fun loadProfile(
        operation: AccountOperation,
        callback: AccountCallback<AccountProfile>
    ) {
        backend.request("GET", "/v1/profile", forceRefreshIdToken = true) { result ->
            result.fold(
                onSuccess = { data ->
                    val firebaseUser = auth.currentUser
                    if (firebaseUser == null) {
                        deliverFailure(operation, callback, authRequired())
                    } else {
                        deliverSuccess(operation, callback, parseProfile(data, firebaseUser))
                    }
                },
                onFailure = { deliverBackendFailure(operation, callback, it) }
            )
        }
    }

    private fun parseProfile(data: JSONObject, user: FirebaseUser): AccountProfile {
        val providers = user.providerData.mapNotNull {
            when (it.providerId) {
                EmailAuthProvider.PROVIDER_ID -> LinkedProvider.EMAIL_PASSWORD
                PlayGamesAuthProvider.PROVIDER_ID -> LinkedProvider.PLAY_GAMES
                else -> null
            }
        }.toSet()
        return AccountProfile(
            uid = user.uid,
            username = data.optString("username").takeIf { it.isNotBlank() && it != "null" },
            email = user.email,
            emailVerified = user.isEmailVerified,
            providers = providers
        )
    }

    private fun parseProgress(data: JSONObject): CloudProgress {
        val levelsJson = data.optJSONArray("levels") ?: JSONArray()
        val levels = buildList {
            for (index in 0 until levelsJson.length()) {
                val level = levelsJson.getJSONObject(index)
                add(LevelCloudProgress(
                    levelId = level.getString("levelId"),
                    bestPercent = level.optInt("bestPercent"),
                    totalAttempts = level.optInt("attempts"),
                    completed = level.optBoolean("completed")
                ))
            }
        }
        return CloudProgress(
            schemaVersion = data.optInt("schemaVersion", 1),
            contentVersion = data.optInt("contentVersion", 1),
            revision = data.optLong("revision"),
            coins = data.optInt("coins"),
            points = data.optInt("points"),
            completedLevels = data.optInt("completedLevels"),
            levels = levels,
            updatedAt = data.optLong("updatedAt")
        )
    }

    private fun parseLeaderboard(data: JSONObject): LeaderboardSnapshot {
        val entriesJson = data.optJSONArray("entries") ?: JSONArray()
        val entries = buildList {
            for (index in 0 until entriesJson.length()) {
                add(parseLeaderboardEntry(entriesJson.getJSONObject(index)))
            }
        }
        return LeaderboardSnapshot(
            entries = entries,
            currentPlayer = data.optJSONObject("currentPlayer")?.let {
                parseLeaderboardEntry(it, currentPlayer = true)
            },
            generatedAt = data.optLong("generatedAt"),
            nextRefreshAt = data.optLong("nextRefreshAt"),
            currentPlayerStatus = data.optString("currentPlayerStatus", "unranked")
        )
    }

    private fun parseLeaderboardEntry(
        data: JSONObject,
        currentPlayer: Boolean = data.optBoolean("currentPlayer")
    ) = LeaderboardEntry(
        rank = data.optInt("rank"),
        username = data.optString("username"),
        points = data.optInt("points"),
        completedLevels = data.optInt("completedLevels"),
        currentPlayer = currentPlayer
    )

    private fun <T> deliverBackendFailure(
        operation: AccountOperation,
        callback: AccountCallback<T>,
        throwable: Throwable
    ) {
        val error = (throwable as? BackendException)?.accountError
            ?: AndroidBackendClient.mapThrowable(throwable)
        deliverFailure(operation, callback, error)
    }

    private fun <T> fail(
        operation: AccountOperation,
        callback: AccountCallback<T>,
        throwable: Throwable
    ) {
        deliverFailure(operation, callback, mapFirebaseError(throwable))
    }

    private fun <T> failWithStage(
        operation: AccountOperation,
        callback: AccountCallback<T>,
        throwable: Throwable,
        stage: String
    ) {
        val original = mapFirebaseError(throwable)
        val status = (throwable as? ApiException)?.statusCode
        val suffix = if (status != null) " (status $status)" else ""
        val message = if (original.code == AccountErrorCode.CONFLICT) {
            original.userMessage
        } else {
            "$stage failed$suffix."
        }
        deliverFailure(
            operation,
            callback,
            original.copy(
                userMessage = message,
                diagnosticMessage = "$stage: ${original.diagnosticMessage.orEmpty()}"
            )
        )
    }

    private fun mapFirebaseError(throwable: Throwable): AccountOperationError {
        return when (throwable) {
            is FirebaseAuthUserCollisionException -> AccountOperationError(
                AccountErrorCode.CONFLICT,
                "That login method is already connected to another Rhythmic Rush account. Sign in with the original method first, then use LINK EMAIL or LINK PLAY GAMES from the account screen.",
                diagnosticMessage = throwable.errorCode
            )
            is FirebaseAuthInvalidCredentialsException -> AccountOperationError(
                AccountErrorCode.AUTH_INVALID,
                "The email, password, or Play Games credential is invalid.",
                diagnosticMessage = throwable.errorCode
            )
            is FirebaseNetworkException -> AccountOperationError(
                AccountErrorCode.NETWORK,
                "Could not reach Firebase. Check your connection.",
                retryable = true
            )
            is ApiException -> AccountOperationError(
                if (throwable.statusCode == 12501) AccountErrorCode.CANCELLED
                else AccountErrorCode.AUTH_INVALID,
                if (throwable.statusCode == 12501) "Play Games sign-in was cancelled."
                else "Play Games sign-in failed.",
                diagnosticMessage = "Play Games status ${throwable.statusCode}"
            )
            is FirebaseAuthException -> AccountOperationError(
                AccountErrorCode.AUTH_INVALID,
                throwable.localizedMessage ?: "Firebase authentication failed.",
                diagnosticMessage = throwable.errorCode
            )
            else -> AndroidBackendClient.mapThrowable(throwable)
        }
    }

    private fun authRequired() = AccountOperationError(
        AccountErrorCode.AUTH_REQUIRED,
        "Please sign in first."
    )
}
