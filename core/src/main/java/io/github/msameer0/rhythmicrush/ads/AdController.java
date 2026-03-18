package io.github.msameer0.rhythmicrush.ads;

public interface AdController {
    void showInterstitialAd();
    boolean isAdLoaded();
    void showBannerAd(boolean show);
}
