package io.github.msameer0.rhythmicrush.android;

import android.os.Bundle;
import android.app.ActivityManager;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.ads.AdController;

public class AndroidLauncher extends AndroidApplication implements AdController {

    private InterstitialAd mInterstitialAd;
    private final String INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712";

    // New Banner Variables
    private AdView adView;
    private final String BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, initializationStatus -> {
        });

        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;

        // 1. Create a RelativeLayout to hold both the Game and the Banner Ad
        RelativeLayout layout = new RelativeLayout(this);

        // 2. Initialize the Game View
        View gameView = initializeForView(new RhythmicRushGame(this), configuration);
        layout.addView(gameView);

        // 3. Initialize the Banner Ad View
        adView = new AdView(this);
        adView.setAdUnitId(BANNER_TEST_ID);
        adView.setAdSize(AdSize.BANNER);
        adView.setVisibility(View.GONE); // Hide it by default

        // 4. Position the Banner Ad at the BOTTOM of the screen
        RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        adParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        adParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layout.addView(adView, adParams);

        // 5. Set the entire layout as the active screen
        setContentView(layout);

        // Load Ads (if not a bot)
        if (!ActivityManager.isRunningInUserTestHarness()) {
            loadInterstitialAd();
            AdRequest bannerRequest = new AdRequest.Builder().build();
            adView.loadAd(bannerRequest);
        }
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_TEST_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
    }

    @Override
    public void showInterstitialAd() {
        runOnUiThread(() -> {
            if (mInterstitialAd != null) {
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mInterstitialAd = null;
                        if (!ActivityManager.isRunningInUserTestHarness()) loadInterstitialAd();
                    }
                });
                mInterstitialAd.show(AndroidLauncher.this);
            }
        });
    }

    @Override
    public boolean isAdLoaded() {
        return mInterstitialAd != null;
    }

    // NEW METHOD: Handle the Banner Visibility
    @Override
    public void showBannerAd(boolean show) {
        runOnUiThread(() -> {
            if (adView != null) {
                if (show) {
                    adView.setVisibility(View.VISIBLE);
                } else {
                    adView.setVisibility(View.GONE);
                }
            }
        });
    }
}
