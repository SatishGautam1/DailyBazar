package com.nighttech.dailybazar.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2600L;
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hideAll();
        new Handler(Looper.getMainLooper()).postDelayed(this::playEntranceAnimations, 100);

        // FIX: Check if user is already signed in and route accordingly
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Class<?> destination = (currentUser != null) ? MainActivity.class : LoginActivity.class;

            Intent intent = new Intent(SplashActivity.this, destination);
            // If going to MainActivity, clear the back stack so Back doesn't return to splash
            if (currentUser != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            startActivity(intent);
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
        binding.ivLogo.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(650)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();

        binding.tvAppName.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(400).setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        binding.dividerGold.animate()
                .alpha(1f).scaleX(1f)
                .setStartDelay(650).setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        binding.tvTagline.animate()
                .alpha(0.72f)
                .setStartDelay(800).setDuration(450)
                .start();

        binding.bottomSection.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(950).setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}