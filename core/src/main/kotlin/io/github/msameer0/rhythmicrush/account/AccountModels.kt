package io.github.msameer0.rhythmicrush.account

enum class LinkedProvider {
    EMAIL_PASSWORD,
    PLAY_GAMES
}

data class PlatformAccountCapabilities(
    val playGamesLogin: Boolean = false,
    val emailLogin: Boolean = false,
    val secureTokenStorage: Boolean = false,
    val appCheck: Boolean = false,
    val providerLinking: Boolean = false
)

data class AccountProfile(
    val uid: String,
    val username: String?,
    val email: String?,
    val emailVerified: Boolean,
    val providers: Set<LinkedProvider>
)

data class LevelCloudProgress(
    val levelId: String,
    val bestPercent: Int,
    val totalAttempts: Int,
    val completed: Boolean
)

data class CloudProgress(
    val schemaVersion: Int,
    val contentVersion: Int,
    val revision: Long,
    val coins: Int,
    val points: Int,
    val completedLevels: Int,
    val levels: List<LevelCloudProgress>,
    val updatedAt: Long
)

data class SyncRequest(
    val schemaVersion: Int,
    val contentVersion: Int,
    val deviceId: String,
    val idempotencyKey: String,
    val lastKnownRevision: Long,
    val levels: List<LevelCloudProgress>,
    val legacyCoinFloor: Int?
)

data class SyncResult(
    val progress: CloudProgress,
    val levelsImproved: Int,
    val attemptsAdded: Int,
    val rewardsReconciled: Boolean
)

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val points: Int,
    val completedLevels: Int,
    val currentPlayer: Boolean
)

data class LeaderboardSnapshot(
    val entries: List<LeaderboardEntry>,
    val currentPlayer: LeaderboardEntry?,
    val generatedAt: Long,
    val nextRefreshAt: Long,
    val currentPlayerStatus: String = "unranked"
)

data class AccountMergePreview(
    val ticketId: String,
    val canonicalProfile: AccountProfile,
    val secondaryProfile: AccountProfile,
    val canonicalProgress: CloudProgress,
    val secondaryProgress: CloudProgress,
    val expiresAt: Long
)

enum class AccountErrorCode {
    UNAVAILABLE,
    CANCELLED,
    NETWORK,
    AUTH_REQUIRED,
    AUTH_INVALID,
    EMAIL_VERIFICATION_REQUIRED,
    USERNAME_REQUIRED,
    CONFLICT,
    RATE_LIMITED,
    VALIDATION,
    UPDATE_REQUIRED,
    SERVER,
    UNKNOWN
}

data class AccountOperationError(
    val code: AccountErrorCode,
    val userMessage: String,
    val retryable: Boolean = false,
    val retryAfterSeconds: Int? = null,
    val diagnosticMessage: String? = null
)

sealed class AccountState {
    object Unavailable : AccountState()
    object SignedOut : AccountState()
    object Authenticating : AccountState()
    object NeedsEmailVerification : AccountState()
    object NeedsUsername : AccountState()
    object SignedIn : AccountState()
    object Syncing : AccountState()
    object MergeRequired : AccountState()
    object MergeInProgress : AccountState()
    object Deleting : AccountState()
    data class RecoverableError(
        val previousState: AccountState,
        val error: AccountOperationError
    ) : AccountState()
}

data class AccountStatus(
    val state: AccountState,
    val profile: AccountProfile?,
    val lastSuccessfulSyncAt: Long?,
    val pendingUploadCount: Int,
    val leaderboardNextRefreshAt: Long
)
