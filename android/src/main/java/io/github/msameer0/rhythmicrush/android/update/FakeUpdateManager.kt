package io.github.msameer0.rhythmicrush.android.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.msameer0.rhythmicrush.update.UpdateManager

class FakeUpdateManager(private val activity: Activity) : UpdateManager {
    private val fakeAppUpdateManager = FakeAppUpdateManager(activity)

    override fun checkForUpdate() {
        //simulate a fake update available
        fakeAppUpdateManager.setUpdateAvailable(999)

        fakeAppUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                fakeAppUpdateManager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    AndroidUpdateManager.UPDATE_REQUEST_CODE
                )

                if (fakeAppUpdateManager.isConfirmationDialogVisible) {
                    fakeAppUpdateManager.userAcceptsUpdate()
                    fakeAppUpdateManager.downloadStarts()
                    fakeAppUpdateManager.downloadCompletes()
                    fakeAppUpdateManager.completeUpdate()
                    fakeAppUpdateManager.installCompletes()
                }

                //simulate user accept and download complete
                //fakeAppUpdateManager.userAcceptsUpdate()
                //fakeAppUpdateManager.downloadStarts()
                //fakeAppUpdateManager.downloadCompletes()
            }
        }
    }
}
