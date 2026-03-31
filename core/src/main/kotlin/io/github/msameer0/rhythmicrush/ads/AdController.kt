package io.github.msameer0.rhythmicrush.ads

interface AdController {
    fun showInterstitialAd()
    fun isAdLoaded(): Boolean
    fun showBannerAd(show: Boolean)
}
