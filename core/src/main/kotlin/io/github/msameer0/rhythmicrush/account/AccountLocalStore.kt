package io.github.msameer0.rhythmicrush.account

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import java.util.UUID

class AccountLocalStore {
    private val json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)
        setUsePrototypes(false)
    }

    fun loadOrCreateDeviceId(): String {
        val data = loadMeta()
        if (data.deviceId.isBlank()) {
            data.deviceId = UUID.randomUUID().toString()
            saveMeta(data)
        }
        return data.deviceId
    }

    fun loadMeta(): AccountMetaData =
        read(ACCOUNT_META_PATH, AccountMetaData::class.java) ?: AccountMetaData()

    fun saveMeta(data: AccountMetaData) {
        write(ACCOUNT_META_PATH, data)
    }

    fun loadLeaderboard(): LeaderboardSnapshot? {
        val data = read(LEADERBOARD_CACHE_PATH, LeaderboardCacheData::class.java) ?: return null
        if (data.generatedAt <= 0L) return null
        val entries = data.entries.map {
            LeaderboardEntry(
                rank = it.rank,
                username = it.username,
                points = it.points,
                completedLevels = it.completedLevels,
                currentPlayer = it.currentPlayer
            )
        }
        val current = data.currentPlayer?.let {
            LeaderboardEntry(
                rank = it.rank,
                username = it.username,
                points = it.points,
                completedLevels = it.completedLevels,
                currentPlayer = true
            )
        }
        return LeaderboardSnapshot(
            entries,
            current,
            data.generatedAt,
            data.nextRefreshAt,
            data.currentPlayerStatus
        )
    }

    fun saveLeaderboard(snapshot: LeaderboardSnapshot) {
        val data = LeaderboardCacheData()
        data.generatedAt = snapshot.generatedAt
        data.nextRefreshAt = snapshot.nextRefreshAt
        data.currentPlayerStatus = snapshot.currentPlayerStatus
        data.entries = snapshot.entries.map { LeaderboardEntryData.from(it) }.toMutableList()
        data.currentPlayer = snapshot.currentPlayer?.let { LeaderboardEntryData.from(it) }
        write(LEADERBOARD_CACHE_PATH, data)
    }

    fun loadPendingSyncRequests(): List<SyncRequest> {
        val data = read(SYNC_QUEUE_PATH, SyncQueueData::class.java) ?: return emptyList()
        return data.requests.map { request ->
            SyncRequest(
                schemaVersion = request.schemaVersion,
                contentVersion = request.contentVersion,
                deviceId = request.deviceId,
                idempotencyKey = request.idempotencyKey,
                lastKnownRevision = request.lastKnownRevision,
                levels = request.levels.map { level ->
                    LevelCloudProgress(
                        levelId = level.levelId,
                        bestPercent = level.bestPercent,
                        totalAttempts = level.totalAttempts,
                        completed = level.completed
                    )
                },
                legacyCoinFloor = request.legacyCoinFloor
            )
        }
    }

    fun savePendingSyncRequests(requests: Collection<SyncRequest>) {
        val data = SyncQueueData()
        data.requests = requests
            .distinctBy { it.idempotencyKey }
            .map { SyncRequestData.from(it) }
            .toMutableList()
        write(SYNC_QUEUE_PATH, data)
    }

    private fun <T> read(path: String, type: Class<T>): T? {
        return try {
            val file = Gdx.files.local(path)
            if (!file.exists()) null else json.fromJson(type, file)
        } catch (exception: Exception) {
            Gdx.app.error("AccountLocalStore", "Could not read $path: ${exception.message}")
            null
        }
    }

    private fun write(path: String, value: Any) {
        try {
            val file = Gdx.files.local(path)
            file.parent().mkdirs()
            val temporary = Gdx.files.local("$path.tmp")
            temporary.writeString(json.prettyPrint(value), false, "UTF-8")
            replace(temporary, file)
        } catch (exception: Exception) {
            Gdx.app.error("AccountLocalStore", "Could not write $path: ${exception.message}")
        }
    }

    private fun replace(temporary: FileHandle, destination: FileHandle) {
        if (destination.exists()) destination.delete()
        temporary.moveTo(destination)
    }

    class AccountMetaData {
        var schemaVersion: Int = 1
        var deviceId: String = ""
        var lastCloudRevision: Long = 0L
        var lastSuccessfulSyncAt: Long = 0L
        var contentVersion: Int = 1
    }

    class LeaderboardCacheData {
        var entries: MutableList<LeaderboardEntryData> = mutableListOf()
        var currentPlayer: LeaderboardEntryData? = null
        var generatedAt: Long = 0L
        var nextRefreshAt: Long = 0L
        var currentPlayerStatus: String = "unranked"
    }

    class LeaderboardEntryData {
        var rank: Int = 0
        var username: String = ""
        var points: Int = 0
        var completedLevels: Int = 0
        var currentPlayer: Boolean = false

        companion object {
            fun from(entry: LeaderboardEntry) = LeaderboardEntryData().apply {
                rank = entry.rank
                username = entry.username
                points = entry.points
                completedLevels = entry.completedLevels
                currentPlayer = entry.currentPlayer
            }
        }
    }

    class SyncQueueData {
        var requests: MutableList<SyncRequestData> = mutableListOf()
    }

    class SyncRequestData {
        var schemaVersion: Int = 1
        var contentVersion: Int = 1
        var deviceId: String = ""
        var idempotencyKey: String = ""
        var lastKnownRevision: Long = 0L
        var levels: MutableList<LevelProgressData> = mutableListOf()
        var legacyCoinFloor: Int? = null

        companion object {
            fun from(request: SyncRequest) = SyncRequestData().apply {
                schemaVersion = request.schemaVersion
                contentVersion = request.contentVersion
                deviceId = request.deviceId
                idempotencyKey = request.idempotencyKey
                lastKnownRevision = request.lastKnownRevision
                levels = request.levels.map { LevelProgressData.from(it) }.toMutableList()
                legacyCoinFloor = request.legacyCoinFloor
            }
        }
    }

    class LevelProgressData {
        var levelId: String = ""
        var bestPercent: Int = 0
        var totalAttempts: Int = 0
        var completed: Boolean = false

        companion object {
            fun from(progress: LevelCloudProgress) = LevelProgressData().apply {
                levelId = progress.levelId
                bestPercent = progress.bestPercent
                totalAttempts = progress.totalAttempts
                completed = progress.completed
            }
        }
    }

    companion object {
        const val ACCOUNT_META_PATH = "saves/account_meta.json"
        const val LEADERBOARD_CACHE_PATH = "saves/leaderboard_cache.json"
        const val SYNC_QUEUE_PATH = "saves/sync_queue.json"
    }
}
