package io.github.msameer0.rhythmicrush.android.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.msameer0.rhythmicrush.update.UpdateManager

/**
 * Android in-app update manager using Play Core Library.
 * Enforces mandatory updates with IMMEDIATE flow to block gameplay until update is complete.
 *
 * Update states:
 * - NOT_AVAILABLE: No update available
 * - AVAILABLE: Update available and can be downloaded
 * - DOWNLOADING: Update is being downloaded
 * - DOWNLOADED: Update ready to install (requires restart)
 * - INSTALLING: Update being installed
 */
class AndroidUpdateManager(private val activity: Activity) : UpdateManager {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private var updateAvailable = false
    private var updatePending = false
    private var lastCheckTime = 0L
    private var updateCheckInProgress = false

    private val installStateListener = InstallStateUpdatedListener { state ->
        Log.d(TAG, "Install state changed: ${state.installStatus()}")
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                // Update downloaded, complete installation immediately
                updatePending = true
                Log.i(TAG, "Update downloaded, completing installation")
                // Completing here triggers immediate restart
                try {
                    appUpdateManager.completeUpdate()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to complete update immediately", e)
                }
            }
            InstallStatus.FAILED -> {
                Log.e(TAG, "Update installation failed")
                updatePending = false
                // Retry immediately when update fails
                retryUpdateAfterDelay()
            }
            InstallStatus.CANCELED -> {
                Log.w(TAG, "Update was canceled by user, retrying")
                updatePending = false
                // Force retry if user cancels - don't allow deferral
                retryUpdateAfterDelay()
            }
            else -> {}
        }
    }

    /**
     * Check for updates from Play Store.
     * Uses IMMEDIATE flow to force users to update before playing.
     */
    override fun checkForUpdate() {
        if (updateCheckInProgress) {
            Log.d(TAG, "Update check already in progress")
            return
        }

        updateCheckInProgress = true
        appUpdateManager.registerListener(installStateListener)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            updateCheckInProgress = false
            lastCheckTime = System.currentTimeMillis()

            Log.d(TAG, "Update availability: ${info.updateAvailability()}")

            when {
                // Update available and can start IMMEDIATE flow
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                    Log.i(TAG, "Mandatory update available, starting IMMEDIATE flow")
                    updateAvailable = true
                    startImmediateUpdate(info)
                }

                // Fallback: If only FLEXIBLE is allowed, still use IMMEDIATE with fallback
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    Log.i(TAG, "Update available (FLEXIBLE mode), attempting IMMEDIATE")
                    updateAvailable = true
                    startImmediateUpdate(info)
                }

                // Download was completed in previous session
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    Log.i(TAG, "Update already downloaded, completing installation")
                    updatePending = true
                    try {
                        appUpdateManager.completeUpdate()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to complete downloaded update", e)
                    }
                }

                else -> {
                    Log.d(TAG, "No update action needed")
                    updateAvailable = false
                }
            }
        }.addOnFailureListener { exception ->
            updateCheckInProgress = false
            Log.e(TAG, "Update check failed", exception)
            // Retry after a delay even if check fails
            retryUpdateAfterDelay()
        }
    }

    /**
     * Force an immediate update, blocking all gameplay.
     * Called when mandatory update is required.
     */
    override fun forceUpdate() {
        Log.i(TAG, "Force update triggered")
        checkForUpdate()
    }

    /**
     * Returns true if an update is pending installation.
     */
    override fun isUpdatePending(): Boolean = updatePending

    /**
     * Returns true if an update is available but not yet downloaded.
     */
    override fun isUpdateAvailable(): Boolean = updateAvailable

    /**
     * Called when activity resumes - check if update was downloaded in background.
     */
    override fun onResume() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                Log.i(TAG, "Update found on resume, completing installation")
                updatePending = true
                try {
                    appUpdateManager.completeUpdate()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to complete update on resume", e)
                }
            }
        }
    }

    /**
     * Called when activity stops.
     */
    override fun onStop() {
        // Intentionally not completing update here to avoid killing the app unexpectedly
        Log.d(TAG, "onStop called")
    }

    /**
     * Called when activity is destroyed.
     */
    override fun onDestroy() {
        try {
            appUpdateManager.unregisterListener(installStateListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister listener", e)
        }
    }

    private fun startImmediateUpdate(info: com.google.android.play.core.appupdate.AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                UPDATE_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start update flow", e)
            retryUpdateAfterDelay()
        }
    }

    private fun retryUpdateAfterDelay() {
        // Schedule retry after 5 seconds
        activity.window?.decorView?.postDelayed({
            Log.d(TAG, "Retrying update check after delay")
            checkForUpdate()
        }, RETRY_DELAY_MS)
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 100
        private const val TAG = "AndroidUpdateManager"
        private const val RETRY_DELAY_MS = 5000L // 5 seconds
    }
}
