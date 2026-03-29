package io.github.msameer0.rhythmicrush.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;
import io.github.msameer0.rhythmicrush.android.ads.AndroidAdController;
import io.github.msameer0.rhythmicrush.android.update.AndroidUpdateManager;

public class AndroidLauncher extends AndroidApplication {

    private AndroidAdController adController;
    private AndroidUpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateManager = new AndroidUpdateManager(this);
        adController = new AndroidAdController(this);

        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;

        RelativeLayout layout = new RelativeLayout(this);
        View gameView = initializeForView(new RhythmicRushGame(adController, updateManager), configuration);
        layout.addView(gameView);

        adController.initialize(layout);
        setContentView(layout);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AndroidUpdateManager.UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Gdx.app.log("UpdateTest", "User cancelled or update failed");
            }
        }
    }

    @Override
    protected void onPause() {
        adController.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        updateManager.onStop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adController.onResume();
        updateManager.onResume();
    }

    @Override
    protected void onDestroy() {
        adController.onDestroy();
        updateManager.onDestroy();
        super.onDestroy();
    }
}
