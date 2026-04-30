package com.nighttech.dailybazar.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivitySplashBinding;
import com.nighttech.dailybazar.ui.activities.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2400L;
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        playEntranceAnimations();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_in);
            finish();
        }, SPLASH_DELAY_MS);
    }

    private void playEntranceAnimations() {
        // Logo card: scale from 0.6 → 1.0 + fade in
        binding.ivLogo.setAlpha(0f);
        binding.ivLogo.setScaleX(0.6f);
        binding.ivLogo.setScaleY(0.6f);
        binding.ivLogo.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(150).setDuration(600)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

        // App name slides up
        binding.tvAppName.setAlpha(0f);
        binding.tvAppName.setTranslationY(32f);
        binding.tvAppName.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(450).setDuration(500)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Gold divider expands width
        binding.dividerGold.setScaleX(0f);
        binding.dividerGold.animate()
                .scaleX(1f)
                .setStartDelay(700).setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Tagline
        binding.tvTagline.setAlpha(0f);
        binding.tvTagline.animate()
                .alpha(0.75f)
                .setStartDelay(800).setDuration(400)
                .start();

        // Bottom section fades in
        binding.bottomSection.setAlpha(0f);
        binding.bottomSection.animate()
                .alpha(1f)
                .setStartDelay(1000).setDuration(500)
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}