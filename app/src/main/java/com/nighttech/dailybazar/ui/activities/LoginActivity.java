package com.nighttech.dailybazar.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivityLoginBinding;
import com.nighttech.dailybazar.ui.activities.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Draw behind system bars — status bar area belongs to the app
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        playEntranceAnimations();
        setupListeners();
    }

    /**
     * Adjusts the header's statusBarSpacer height to match the real
     * status bar so content sits below it naturally.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Push header content below status bar
            ViewGroup.LayoutParams spacerParams = binding.statusBarSpacer.getLayoutParams();
            spacerParams.height = systemBars.top;
            binding.statusBarSpacer.setLayoutParams(spacerParams);

            return insets;
        });
    }

    private void playEntranceAnimations() {
        // Header slides down
        binding.headerBand.setTranslationY(-60f);
        binding.headerBand.setAlpha(0f);
        binding.headerBand.animate()
                .translationY(0f).alpha(1f)
                .setDuration(500)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Card rises from below
        binding.cardLogin.setAlpha(0f);
        binding.cardLogin.setTranslationY(80f);
        binding.cardLogin.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(250).setDuration(550)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.btnGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.btnForgotPassword.setOnClickListener(v -> {
            // TODO: forgot password
        });
    }

    private void attemptLogin() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString().trim() : "";

        boolean valid = true;

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.login_email_error));
            shake(binding.tilEmail);
            valid = false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.login_password_error));
            shake(binding.tilPassword);
            valid = false;
        }

        if (valid) {
            // Animate button, then navigate
            binding.btnLogin.animate()
                    .scaleX(0.96f).scaleY(0.96f).setDuration(100)
                    .withEndAction(() ->
                            binding.btnLogin.animate().scaleX(1f).scaleY(1f).setDuration(100)
                                    .withEndAction(() -> {
                                        Intent i = new Intent(this, MainActivity.class);
                                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(i);
                                        overridePendingTransition(R.anim.fade_in, R.anim.fade_in);
                                    }).start()
                    ).start();
        }
    }

    private void shake(android.view.View view) {
        android.view.animation.Animation anim =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
        view.startAnimation(anim);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}