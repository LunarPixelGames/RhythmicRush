package io.github.msameer0.rhythmicrush.account.fake

import io.github.msameer0.rhythmicrush.account.AccountCallback
import io.github.msameer0.rhythmicrush.account.AccountClient
import io.github.msameer0.rhythmicrush.account.AccountErrorCode
import io.github.msameer0.rhythmicrush.account.AccountMergePreview
import io.github.msameer0.rhythmicrush.account.AccountOperation
import io.github.msameer0.rhythmicrush.account.AccountOperationError
import io.github.msameer0.rhythmicrush.account.AccountProfile
import io.github.msameer0.rhythmicrush.account.CloudProgress
import io.github.msameer0.rhythmicrush.account.LeaderboardSnapshot
import io.github.msameer0.rhythmicrush.account.PlatformAccountCapabilities
import io.github.msameer0.rhythmicrush.account.SimpleAccountOperation
import io.github.msameer0.rhythmicrush.account.SyncRequest
import io.github.msameer0.rhythmicrush.account.SyncResult
import io.github.msameer0.rhythmicrush.account.deliverFailure
import io.github.msameer0.rhythmicrush.account.deliverSuccess

class FakeAccountClient : AccountClient {
    override var capabilities = PlatformAccountCapabilities(emailLogin = true)
    var currentProfile: AccountProfile? = null
    var cloudProgress: CloudProgress? = null
    var leaderboard: LeaderboardSnapshot? = null
    var nextError: AccountOperationError? = null

    private fun <T> respond(callback: AccountCallback<T>, value: () -> T): AccountOperation {
        val operation = SimpleAccountOperation()
        val error = nextError
        nextError = null
        if (error != null) {
            deliverFailure(operation, callback, error)
        } else {
            try {
                deliverSuccess(operation, callback, value())
            } catch (exception: Exception) {
                deliverFailure(
                    operation,
                    callback,
                    AccountOperationError(
                        AccountErrorCode.UNKNOWN,
                        exception.message ?: "Fake account operation failed."
                    )
                )
            }
        }
        return operation
    }

    override fun restoreSession(callback: AccountCallback<AccountProfile?>) =
        respond(callback) { currentProfile }

    override fun registerEmail(email: String, password: CharArray, callback: AccountCallback<AccountProfile>) =
        respond(callback) { requireProfile() }
    override fun loginEmail(email: String, password: CharArray, callback: AccountCallback<AccountProfile>) =
        respond(callback) { requireProfile() }
    override fun loginPlayGames(callback: AccountCallback<AccountProfile>) =
        respond(callback) { requireProfile() }
    override fun logout(callback: AccountCallback<Unit>) = respond(callback) {
        currentProfile = null
    }
    override fun sendEmailVerification(callback: AccountCallback<Unit>) = respond(callback) { Unit }
    override fun refreshProfile(callback: AccountCallback<AccountProfile>) =
        respond(callback) { requireProfile() }
    override fun sendPasswordReset(email: String, callback: AccountCallback<Unit>) =
        respond(callback) { Unit }
    override fun reserveUsername(
        username: String,
        idempotencyKey: String,
        callback: AccountCallback<AccountProfile>
    ) = respond(callback) {
        requireProfile().copy(username = username).also { currentProfile = it }
    }
    override fun linkEmail(email: String, password: CharArray, callback: AccountCallback<AccountProfile>) =
        respond(callback) { requireProfile() }
    override fun linkPlayGames(callback: AccountCallback<AccountProfile>) =
        respond(callback) { requireProfile() }
    override fun fetchProgress(callback: AccountCallback<CloudProgress>) =
        respond(callback) { requireNotNull(cloudProgress) }
    override fun uploadProgress(request: SyncRequest, callback: AccountCallback<SyncResult>) =
        respond(callback) { SyncResult(requireNotNull(cloudProgress), 0, 0, false) }
    override fun fetchLeaderboard(forceRefresh: Boolean, callback: AccountCallback<LeaderboardSnapshot>) =
        respond(callback) { requireNotNull(leaderboard) }
    override fun beginMerge(secondaryCredential: String, callback: AccountCallback<AccountMergePreview>) =
        respond(callback) { error("No fake merge preview configured.") }
    override fun confirmMerge(ticketId: String, callback: AccountCallback<AccountProfile>) =
        respond(callback) { requireProfile() }
    override fun deleteAccount(callback: AccountCallback<Unit>) = respond(callback) {
        currentProfile = null
    }

    private fun requireProfile(): AccountProfile =
        requireNotNull(currentProfile) { "No fake profile configured." }
}
