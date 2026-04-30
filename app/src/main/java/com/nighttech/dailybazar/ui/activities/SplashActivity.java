package com.nighttech.dailybazar.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2600L;
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Draw behind status bar — the green theme background covers it seamlessly
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Pre-hide all animated elements so they start invisible
        hideAll();

        // Staggered entrance: logo → name → divider → tagline → bottom
        new Handler(Looper.getMainLooper()).postDelayed(this::playEntranceAnimations, 100);

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_in);
            finish();
        }, SPLASH_DELAY_MS);
    }

    private void hideAll() {
        binding.ivLogo.setAlpha(0f);
        binding.ivLogo.setScaleX(0.4f);
        binding.ivLogo.setScaleY(0.4f);

        binding.tvAppName.setAlpha(0f);
        binding.tvAppName.setTranslationY(30f);

        binding.dividerGold.setAlpha(0f);
        binding.dividerGold.setScaleX(0f);

        binding.tvTagline.setAlpha(0f);

        binding.bottomSection.setAlpha(0f);
        binding.bottomSection.setTranslationY(20f);
    }

    private void playEntranceAnimations() {
        // 1. Logo: zoom in with overshoot bounce
        binding.ivLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(650)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();

        // 2. App name: slides up and fades in
        binding.tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(400)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 3. Gold divider: expands from center
        binding.dividerGold.animate()
                .alpha(1f)
                .scaleX(1f)
                .setStartDelay(650)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 4. Tagline: fades in gently
        binding.tvTagline.animate()
                .alpha(0.72f)
                .setStartDelay(800)
                .setDuration(450)
                .start();

        // 5. Bottom section: slides up and fades in
        binding.bottomSection.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(950)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}