package io.github.msameer0.rhythmicrush.account

interface AccountClient {
    val capabilities: PlatformAccountCapabilities

    fun restoreSession(callback: AccountCallback<AccountProfile?>): AccountOperation
    fun registerEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation
    fun loginEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation
    fun loginPlayGames(callback: AccountCallback<AccountProfile>): AccountOperation
    fun logout(callback: AccountCallback<Unit>): AccountOperation
    fun sendEmailVerification(callback: AccountCallback<Unit>): AccountOperation
    fun refreshProfile(callback: AccountCallback<AccountProfile>): AccountOperation
    fun sendPasswordReset(email: String, callback: AccountCallback<Unit>): AccountOperation
    fun reserveUsername(
        username: String,
        idempotencyKey: String,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation
    fun linkEmail(
        email: String,
        password: CharArray,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation
    fun linkPlayGames(callback: AccountCallback<AccountProfile>): AccountOperation
    fun fetchProgress(callback: AccountCallback<CloudProgress>): AccountOperation
    fun uploadProgress(
        request: SyncRequest,
        callback: AccountCallback<SyncResult>
    ): AccountOperation
    fun fetchLeaderboard(
        forceRefresh: Boolean,
        callback: AccountCallback<LeaderboardSnapshot>
    ): AccountOperation
    fun beginMerge(
        secondaryCredential: String,
        callback: AccountCallback<AccountMergePreview>
    ): AccountOperation
    fun confirmMerge(
        ticketId: String,
        callback: AccountCallback<AccountProfile>
    ): AccountOperation
    fun deleteAccount(callback: AccountCallback<Unit>): AccountOperation
    fun dispose() {}
}
