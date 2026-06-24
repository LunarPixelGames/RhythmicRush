package io.github.msameer0.rhythmicrush.account

import com.badlogic.gdx.Gdx
import java.util.concurrent.CopyOnWriteArrayList

class AccountManager(
    private val client: AccountClient,
    private val configuration: AccountConfiguration,
    private val localStore: AccountLocalStore = AccountLocalStore()
) {
    private val listeners = CopyOnWriteArrayList<(AccountStatus) -> Unit>()
    private val pendingSyncRequests = localStore.loadPendingSyncRequests()
        .associateByTo(linkedMapOf<String, SyncRequest>()) { it.idempotencyKey }
    private var activeOperation: AccountOperation? = null
    private var initialized = false

    val deviceId: String = localStore.loadOrCreateDeviceId()
    var state: AccountState = AccountState.Unavailable
        private set
    var profile: AccountProfile? = null
        private set
    var cachedLeaderboard: LeaderboardSnapshot? = localStore.loadLeaderboard()
        private set
    var lastSuccessfulSyncAt: Long? =
        localStore.loadMeta().lastSuccessfulSyncAt.takeIf { it > 0L }
        private set
    val lastCloudRevision: Long
        get() = localStore.loadMeta().lastCloudRevision

    val capabilities: PlatformAccountCapabilities
        get() = client.capabilities

    fun initialize() {
        if (initialized) return
        initialized = true

        if (!configuration.isValid) {
            updateState(AccountState.Unavailable)
            return
        }

        updateState(AccountState.Authenticating)
        activeOperation = client.restoreSession(object : AccountCallback<AccountProfile?> {
            override fun onSuccess(value: AccountProfile?) {
                activeOperation = null
                profile = value
                updateState(if (value == null) AccountState.SignedOut else stateFor(value))
            }

            override fun onFailure(error: AccountOperationError) {
                activeOperation = null
                profile = null
                if (error.code == AccountErrorCode.UNAVAILABLE) {
                    updateState(AccountState.Unavailable)
                } else {
                    updateState(
                        AccountState.RecoverableError(AccountState.SignedOut, mapError(error))
                    )
                }
            }
        })
    }

    fun addListener(listener: (AccountStatus) -> Unit) {
        listeners.add(listener)
        listener(status())
    }

    fun removeListener(listener: (AccountStatus) -> Unit) {
        listeners.remove(listener)
    }

    fun registerEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ) {
        runProfileOperation(AccountState.Authenticating, callback) {
            client.registerEmail(email, password, it)
        }
    }

    fun loginEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ) {
        runProfileOperation(AccountState.Authenticating, callback) {
            client.loginEmail(email, password, it)
        }
    }

    fun loginPlayGames(callback: AccountCallback<AccountProfile>) {
        runProfileOperation(AccountState.Authenticating, callback) {
            client.loginPlayGames(it)
        }
    }

    fun refreshProfile(callback: AccountCallback<AccountProfile>) {
        runProfileOperation(state, callback) {
            client.refreshProfile(it)
        }
    }

    fun sendEmailVerification(callback: AccountCallback<Unit>) {
        runUnitOperation(state, callback) {
            client.sendEmailVerification(it)
        }
    }

    fun sendPasswordReset(email: String, callback: AccountCallback<Unit>) {
        runUnitOperation(state, callback) {
            client.sendPasswordReset(email, it)
        }
    }

    fun reserveUsername(username: String, callback: AccountCallback<AccountProfile>) {
        runProfileOperation(state, callback) {
            client.reserveUsername(username, java.util.UUID.randomUUID().toString(), it)
        }
    }

    fun linkEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ) {
        runProfileOperation(state, callback) {
            client.linkEmail(email, password, it)
        }
    }

    fun linkPlayGames(callback: AccountCallback<AccountProfile>) {
        runProfileOperation(state, callback) {
            client.linkPlayGames(it)
        }
    }

    fun logout(callback: AccountCallback<Unit>) {
        if (!beginOperation(AccountState.Authenticating, callback)) return
        activeOperation = client.logout(object : AccountCallback<Unit> {
            override fun onSuccess(value: Unit) {
                activeOperation = null
                profile = null
                updateState(AccountState.SignedOut)
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: AccountOperationError) {
                activeOperation = null
                val mapped = mapError(error)
                updateState(AccountState.RecoverableError(state, mapped))
                callback.onFailure(mapped)
            }
        })
    }

    fun fetchProgress(callback: AccountCallback<CloudProgress>) {
        if (!beginOperation(AccountState.Syncing, callback)) return
        activeOperation = client.fetchProgress(object : AccountCallback<CloudProgress> {
            override fun onSuccess(value: CloudProgress) {
                activeOperation = null
                markSyncSuccessful(value.revision)
                updateState(profile?.let { stateFor(it) } ?: AccountState.SignedOut)
                callback.onSuccess(value)
            }

            override fun onFailure(error: AccountOperationError) {
                activeOperation = null
                val mapped = mapError(error)
                updateState(AccountState.RecoverableError(state, mapped))
                callback.onFailure(mapped)
            }
        })
    }

    fun uploadProgress(request: SyncRequest, callback: AccountCallback<SyncResult>) {
        if (!beginOperation(AccountState.Syncing, callback)) return
        activeOperation = client.uploadProgress(request, object : AccountCallback<SyncResult> {
            override fun onSuccess(value: SyncResult) {
                activeOperation = null
                clearQueuedSyncs()
                markSyncSuccessful(value.progress.revision)
                updateState(profile?.let { stateFor(it) } ?: AccountState.SignedOut)
                callback.onSuccess(value)
            }

            override fun onFailure(error: AccountOperationError) {
                activeOperation = null
                queueSync(request)
                val mapped = mapError(error)
                updateState(AccountState.RecoverableError(state, mapped))
                callback.onFailure(mapped)
            }
        })
    }

    fun deleteAccount(callback: AccountCallback<Unit>) {
        if (!beginOperation(AccountState.Deleting, callback)) return
        activeOperation = client.deleteAccount(object : AccountCallback<Unit> {
            override fun onSuccess(value: Unit) {
                activeOperation = null
                profile = null
                updateState(AccountState.SignedOut)
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: AccountOperationError) {
                activeOperation = null
                val mapped = mapError(error)
                updateState(AccountState.RecoverableError(AccountState.SignedIn, mapped))
                callback.onFailure(mapped)
            }
        })
    }

    fun fetchLeaderboard(forceRefresh: Boolean, callback: AccountCallback<LeaderboardSnapshot>) {
        val now = System.currentTimeMillis()
        val cached = cachedLeaderboard
        if (forceRefresh && cached != null && now < cached.nextRefreshAt) {
            callback.onFailure(
                AccountOperationError(
                    AccountErrorCode.RATE_LIMITED,
                    "Leaderboard refresh is available in a few minutes.",
                    retryable = true,
                    retryAfterSeconds = ((cached.nextRefreshAt - now + 999L) / 1000L).toInt()
                )
            )
            return
        }
        if (activeOperation != null) {
            callback.onFailure(busyError())
            return
        }

        activeOperation = client.fetchLeaderboard(
            forceRefresh,
            object : AccountCallback<LeaderboardSnapshot> {
                override fun onSuccess(value: LeaderboardSnapshot) {
                    activeOperation = null
                    cachedLeaderboard = value
                    localStore.saveLeaderboard(value)
                    notifyListeners()
                    callback.onSuccess(value)
                }

                override fun onFailure(error: AccountOperationError) {
                    activeOperation = null
                    callback.onFailure(mapError(error))
                }
            }
        )
    }

    fun markSyncSuccessful(revision: Long) {
        val now = System.currentTimeMillis()
        val meta = localStore.loadMeta()
        meta.lastCloudRevision = revision
        meta.lastSuccessfulSyncAt = now
        meta.contentVersion = configuration.contentVersion
        localStore.saveMeta(meta)
        lastSuccessfulSyncAt = now
        notifyListeners()
    }

    fun queueSync(request: SyncRequest) {
        if (request.idempotencyKey.isBlank()) return
        pendingSyncRequests[request.idempotencyKey] = request
        localStore.savePendingSyncRequests(pendingSyncRequests.values)
        notifyListeners()
    }

    fun completeQueuedOperation(operationId: String) {
        if (pendingSyncRequests.remove(operationId) != null) {
            localStore.savePendingSyncRequests(pendingSyncRequests.values)
            notifyListeners()
        }
    }

    fun clearQueuedSyncs() {
        if (pendingSyncRequests.isEmpty()) return
        pendingSyncRequests.clear()
        localStore.savePendingSyncRequests(emptyList())
        notifyListeners()
    }

    fun dispose() {
        activeOperation?.cancel()
        activeOperation = null
        listeners.clear()
        client.dispose()
    }

    private fun runProfileOperation(
        operationState: AccountState,
        callback: AccountCallback<AccountProfile>,
        start: (AccountCallback<AccountProfile>) -> AccountOperation
    ) {
        if (!beginOperation(operationState, callback)) return
        activeOperation = start(object : AccountCallback<AccountProfile> {
            override fun onSuccess(value: AccountProfile) {
                activeOperation = null
                profile = value
                updateState(stateFor(value))
                callback.onSuccess(value)
            }

            override fun onFailure(error: AccountOperationError) {
                activeOperation = null
                val mapped = mapError(error)
                updateState(AccountState.RecoverableError(AccountState.SignedOut, mapped))
                callback.onFailure(mapped)
            }
        })
    }

    private fun runUnitOperation(
        operationState: AccountState,
        callback: AccountCallback<Unit>,
        start: (AccountCallback<Unit>) -> AccountOperation
    ) {
        if (!beginOperation(operationState, callback)) return
        activeOperation = start(object : AccountCallback<Unit> {
            override fun onSuccess(value: Unit) {
                activeOperation = null
                updateState(profile?.let { stateFor(it) } ?: AccountState.SignedOut)
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: AccountOperationError) {
                activeOperation = null
                val mapped = mapError(error)
                updateState(AccountState.RecoverableError(operationState, mapped))
                callback.onFailure(mapped)
            }
        })
    }

    private fun <T> beginOperation(
        operationState: AccountState,
        callback: AccountCallback<T>
    ): Boolean {
        if (state == AccountState.Unavailable) {
            callback.onFailure(
                AccountOperationError(
                    AccountErrorCode.UNAVAILABLE,
                    "Accounts are unavailable on this build."
                )
            )
            return false
        }
        if (activeOperation != null) {
            callback.onFailure(busyError())
            return false
        }
        updateState(operationState)
        return true
    }

    private fun stateFor(value: AccountProfile): AccountState = when {
        value.email != null && !value.emailVerified -> AccountState.NeedsEmailVerification
        value.username.isNullOrBlank() -> AccountState.NeedsUsername
        else -> AccountState.SignedIn
    }

    private fun updateState(value: AccountState) {
        state = value
        Gdx.app.log("AccountManager", "Account state: ${value.javaClass.simpleName}")
        notifyListeners()
    }

    private fun notifyListeners() {
        val snapshot = status()
        listeners.forEach { it(snapshot) }
    }

    private fun status() = AccountStatus(
        state = state,
        profile = profile,
        lastSuccessfulSyncAt = lastSuccessfulSyncAt,
        pendingUploadCount = pendingSyncRequests.size,
        leaderboardNextRefreshAt = cachedLeaderboard?.nextRefreshAt ?: 0L
    )

    private fun busyError() = AccountOperationError(
        AccountErrorCode.CONFLICT,
        "Another account operation is already in progress.",
        retryable = true
    )

    private fun mapError(error: AccountOperationError): AccountOperationError {
        if (error.userMessage.isNotBlank()) return error
        return error.copy(userMessage = when (error.code) {
            AccountErrorCode.NETWORK -> "Could not reach the account service."
            AccountErrorCode.AUTH_REQUIRED,
            AccountErrorCode.AUTH_INVALID -> "Please sign in again."
            AccountErrorCode.RATE_LIMITED -> "Please wait before trying again."
            AccountErrorCode.UPDATE_REQUIRED -> "Update the game to continue using online features."
            AccountErrorCode.UNAVAILABLE -> "Accounts are unavailable on this build."
            else -> "The account operation could not be completed."
        })
    }
}
