package com.nighttech.dailybazar.ui.activities;

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
import com.nighttech.dailybazar.databinding.ActivitySignupBinding;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        playEntranceAnimations();
        setupListeners();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Top bar spacer = status bar height so toolbar sits below it
            ViewGroup.LayoutParams topBarSpacer = binding.statusBarSpacer.getLayoutParams();
            topBarSpacer.height = systemBars.top;
            binding.statusBarSpacer.setLayoutParams(topBarSpacer);

            // Form top spacer = status bar + toolbar height
            int toolbarHeight = (int) (56 * getResources().getDisplayMetrics().density);
            ViewGroup.LayoutParams formSpacer = binding.formTopSpacer.getLayoutParams();
            formSpacer.height = systemBars.top + toolbarHeight + 8;
            binding.formTopSpacer.setLayoutParams(formSpacer);

            // Bottom padding for keyboard/nav bar
            binding.formContainer.setPadding(
                    binding.formContainer.getPaddingLeft(),
                    binding.formContainer.getPaddingTop(),
                    binding.formContainer.getPaddingRight(),
                    systemBars.bottom + (int)(48 * getResources().getDisplayMetrics().density)
            );

            return insets;
        });
    }

    private void playEntranceAnimations() {
        // Top bar drops in
        binding.topBar.setTranslationY(-40f);
        binding.topBar.setAlpha(0f);
        binding.topBar.animate()
                .translationY(0f).alpha(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Title slides up with delay
        binding.tvSignupTitle.setAlpha(0f);
        binding.tvSignupTitle.setTranslationY(24f);
        binding.tvSignupTitle.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(200).setDuration(450)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Form slides up
        binding.formContainer.setAlpha(0f);
        binding.formContainer.setTranslationY(40f);
        binding.formContainer.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(300).setDuration(500)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        binding.btnGoToLogin.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        binding.btnCreateAccount.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        // Reset all errors
        binding.tilFullName.setError(null);
        binding.tilPhone.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        String name     = getText(binding.etFullName);
        String phone    = getText(binding.etPhone);
        String email    = getText(binding.etEmail);
        String password = getText(binding.etPassword);
        String confirm  = getText(binding.etConfirmPassword);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            binding.tilFullName.setError(getString(R.string.signup_name_error));
            shake(binding.tilFullName); valid = false;
        }
        if (TextUtils.isEmpty(phone) || phone.length() != 10) {
            binding.tilPhone.setError(getString(R.string.signup_phone_error));
            shake(binding.tilPhone); valid = false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.signup_email_error));
            shake(binding.tilEmail); valid = false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.signup_password_error));
            shake(binding.tilPassword); valid = false;
        }
        if (!password.equals(confirm)) {
            binding.tilConfirmPassword.setError(getString(R.string.signup_confirm_password_error));
            shake(binding.tilConfirmPassword); valid = false;
        }

        if (valid) {
            // TODO: register with backend
        }
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
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