package io.github.msameer0.rhythmicrush.ads

/**
 * Interface for controlling advertisement display and state across different platforms.
 */
interface AdController {
    fun showInterstitialAd()
    fun isAdLoaded(): Boolean
    fun showBannerAd(show: Boolean)
}
