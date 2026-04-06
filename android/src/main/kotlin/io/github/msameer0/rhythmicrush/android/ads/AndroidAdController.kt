package io.github.msameer0.rhythmicrush.android.ads

import android.app.Activity
import android.app.ActivityManager
import android.os.Build
import android.view.View
import android.widget.RelativeLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import io.github.msameer0.rhythmicrush.ads.AdController

class AndroidAdController(private val activity: Activity) : AdController {

    private var mInterstitialAd: InterstitialAd? = null
    private val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"

    private var adView: AdView? = null
    private val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"

    fun initialize(layout: RelativeLayout) {
        MobileAds.initialize(activity) {}

        adView = AdView(activity).apply {
            adUnitId = BANNER_TEST_ID
            setAdSize(AdSize.BANNER)
            visibility = View.GONE
        }

        val adParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        }
        layout.addView(adView, adParams)

        if (!isRunningInTestHarness()) {
            loadInterstitialAd()
            activity.runOnUiThread {
                adView?.loadAd(AdRequest.Builder().build())
            }
        }
    }

    private fun isRunningInTestHarness(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityManager.isRunningInUserTestHarness()
        } else {
            false
        }
    }

    private fun loadInterstitialAd() {
        InterstitialAd.load(
            activity,
            INTERSTITIAL_TEST_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    mInterstitialAd = null
                }
            }
        )
    }

    override fun showInterstitialAd() {
        activity.runOnUiThread {
            mInterstitialAd?.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        mInterstitialAd = null
                        if (!isRunningInTestHarness()) {
                            loadInterstitialAd()
                        }
                    }
                }
                show(activity)
            }
        }
    }

    override fun isAdLoaded(): Boolean = mInterstitialAd != null

    override fun showBannerAd(show: Boolean) {
        activity.runOnUiThread {
            adView?.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    fun onPause() = adView?.pause()
    fun onResume() = adView?.resume()
    fun onDestroy() = adView?.destroy()
}
