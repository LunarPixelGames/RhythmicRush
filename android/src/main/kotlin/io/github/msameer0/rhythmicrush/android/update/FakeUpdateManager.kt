package io.github.msameer0.rhythmicrush.android.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.msameer0.rhythmicrush.update.UpdateManager

/**
 * Fake update manager for testing update flows without Play Store.
 * Simulates immediate update flow for testing mandatory update scenarios.
 */
class FakeUpdateManager(private val activity: Activity) : UpdateManager {
    private val fakeAppUpdateManager = FakeAppUpdateManager(activity)
    private var updateAvailable = false
    private var updatePending = false

    override fun checkForUpdate() {
        Log.d("FakeUpdateManager", "Simulating update check")
        // simulate an update available
        fakeAppUpdateManager.setUpdateAvailable(999)

        fakeAppUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                Log.d("FakeUpdateManager", "Simulating IMMEDIATE update flow")
                updateAvailable = true
                fakeAppUpdateManager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                    AndroidUpdateManager.UPDATE_REQUEST_CODE
                )

                if (fakeAppUpdateManager.isConfirmationDialogVisible) {
                    Log.d("FakeUpdateManager", "Confirming update and simulating download")
                    fakeAppUpdateManager.userAcceptsUpdate()
                    fakeAppUpdateManager.downloadStarts()
                    fakeAppUpdateManager.downloadCompletes()
                    updatePending = true
                    fakeAppUpdateManager.installCompletes()
                }
            }
        }
    }

    override fun forceUpdate() {
        Log.d("FakeUpdateManager", "Force update called")
        checkForUpdate()
    }

    override fun isUpdatePending(): Boolean = updatePending

    override fun isUpdateAvailable(): Boolean = updateAvailable

    override fun onResume() {
        Log.d("FakeUpdateManager", "onResume called")
    }

    override fun onStop() {
        Log.d("FakeUpdateManager", "onStop called")
    }

    override fun onDestroy() {
        Log.d("FakeUpdateManager", "onDestroy called")
    }
}
