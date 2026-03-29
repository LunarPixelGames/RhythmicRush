package io.github.msameer0.rhythmicrush.android.update

import android.app.Activity
import android.widget.Toast
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.msameer0.rhythmicrush.update.UpdateManager

class AndroidUpdateManager(private val activity: Activity) : UpdateManager {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Update downloaded! Restart the app to apply it.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            InstallStatus.FAILED -> {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Update failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            InstallStatus.CANCELED -> {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Update cancelled.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {}
        }
    }

    override fun checkForUpdate() {
        appUpdateManager.registerListener(installStateListener)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        UPDATE_REQUEST_CODE
                    )
                }
                // If a download was completed in a previous session but never installed
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    activity.runOnUiThread {
                        Toast.makeText(
                            activity,
                            "Update ready! Restart the app to apply it.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun onResume() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            // If update was downloaded while app was in background, complete it
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Update ready! Restart the app to apply it.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun onDestroy() {
        appUpdateManager.unregisterListener(installStateListener)
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 100
    }
}
