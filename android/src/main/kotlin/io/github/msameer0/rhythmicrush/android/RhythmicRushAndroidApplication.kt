package io.github.msameer0.rhythmicrush.android

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.android.gms.games.PlayGamesSdk
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class RhythmicRushAndroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        PlayGamesSdk.initialize(this)
        FirebaseApp.initializeApp(this)

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val provider = if (isDebuggable) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(provider)
    }
}
