package io.github.msameer0.rhythmicrush.account

class UnavailableAccountClient(
    private val reason: String = "Accounts are not available on this build."
) : AccountClient {
    override val capabilities = PlatformAccountCapabilities()

    private fun <T> unavailable(callback: AccountCallback<T>): AccountOperation {
        val operation = SimpleAccountOperation()
        deliverFailure(
            operation,
            callback,
            AccountOperationError(AccountErrorCode.UNAVAILABLE, reason)
        )
        return operation
    }

    override fun restoreSession(callback: AccountCallback<AccountProfile?>) = unavailable(callback)
    override fun registerEmail(email: String, password: CharArray, callback: AccountCallback<AccountProfile>) =
        unavailable(callback)
    override fun loginEmail(email: String, password: CharArray, callback: AccountCallback<AccountProfile>) =
        unavailable(callback)
    override fun loginPlayGames(callback: AccountCallback<AccountProfile>) = unavailable(callback)
    override fun logout(callback: AccountCallback<Unit>) = unavailable(callback)
    override fun sendEmailVerification(callback: AccountCallback<Unit>) = unavailable(callback)
    override fun refreshProfile(callback: AccountCallback<AccountProfile>) = unavailable(callback)
    override fun sendPasswordReset(email: String, callback: AccountCallback<Unit>) =
        unavailable(callback)
    override fun reserveUsername(
        username: String,
        idempotencyKey: String,
        callback: AccountCallback<AccountProfile>
    ) = unavailable(callback)
    override fun linkEmail(email: String, password: CharArray, callback: AccountCallback<AccountProfile>) =
        unavailable(callback)
    override fun linkPlayGames(callback: AccountCallback<AccountProfile>) = unavailable(callback)
    override fun fetchProgress(callback: AccountCallback<CloudProgress>) = unavailable(callback)
    override fun uploadProgress(request: SyncRequest, callback: AccountCallback<SyncResult>) =
        unavailable(callback)
    override fun fetchLeaderboard(forceRefresh: Boolean, callback: AccountCallback<LeaderboardSnapshot>) =
        unavailable(callback)
    override fun beginMerge(secondaryCredential: String, callback: AccountCallback<AccountMergePreview>) =
        unavailable(callback)
    override fun confirmMerge(ticketId: String, callback: AccountCallback<AccountProfile>) =
        unavailable(callback)
    override fun deleteAccount(callback: AccountCallback<Unit>) = unavailable(callback)
}
