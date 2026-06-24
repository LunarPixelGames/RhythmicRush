package io.github.msameer0.rhythmicrush.account

import com.badlogic.gdx.Gdx
import io.github.msameer0.rhythmicrush.game.level.ProgressManager
import java.util.UUID

class AccountSyncCoordinator(
    private val accountManager: AccountManager,
    private val progressManager: ProgressManager,
    private val contentVersion: Int
) {
    private var synchronizedUid: String? = null
    private var syncing = false

    private val listener: (AccountStatus) -> Unit = { status ->
        val uid = status.profile?.uid
        when {
            uid == null -> {
                synchronizedUid = null
                syncing = false
            }
            status.state == AccountState.SignedIn &&
                uid != synchronizedUid &&
                !syncing -> {
                synchronizedUid = uid
                syncNow()
            }
        }
    }

    fun start() {
        accountManager.addListener(listener)
    }

    fun syncNow(callback: AccountCallback<SyncResult>? = null) {
        if (syncing) return
        syncing = true
        val request = createRequest()
        accountManager.uploadProgress(request, object : AccountCallback<SyncResult> {
            override fun onSuccess(value: SyncResult) {
                syncing = false
                progressManager.mergeCloud(value.progress)
                callback?.onSuccess(value)
            }

            override fun onFailure(error: AccountOperationError) {
                syncing = false
                Gdx.app.log(
                    "AccountSync",
                    "Cloud upload queued: ${error.code}"
                )
                callback?.onFailure(error)
            }
        })
    }

    fun queueCurrentSnapshot() {
        if (accountManager.state == AccountState.SignedIn && !syncing) {
            syncNow()
        } else {
            accountManager.queueSync(createRequest())
        }
    }

    fun dispose() {
        accountManager.removeListener(listener)
    }

    private fun createRequest() = SyncRequest(
        schemaVersion = ProgressManager.CURRENT_SCHEMA,
        contentVersion = contentVersion,
        deviceId = accountManager.deviceId,
        idempotencyKey = UUID.randomUUID().toString(),
        lastKnownRevision = accountManager.lastCloudRevision,
        levels = progressManager.exportForSync(),
        legacyCoinFloor = progressManager.coins
    )
}
