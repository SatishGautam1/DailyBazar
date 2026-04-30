package com.nighttech.dailybazar.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        applyWindowInsets();
        playEntranceAnimations();
        setupListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            navigateToMain();
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            ViewGroup.LayoutParams p = binding.statusBarSpacer.getLayoutParams();
            p.height = systemBars.top;
            binding.statusBarSpacer.setLayoutParams(p);

            binding.scrollView.setPadding(
                    binding.scrollView.getPaddingLeft(),
                    binding.scrollView.getPaddingTop(),
                    binding.scrollView.getPaddingRight(),
                    imeInsets.bottom > 0 ? imeInsets.bottom : systemBars.bottom
            );

            return windowInsets;
        });
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.btnForgotPassword.setOnClickListener(v -> {
            String email = safeText(binding.etEmail);
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Enter email to reset password", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.sendPasswordResetEmail(email).addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Reset link sent to email", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void attemptLogin() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        String email = safeText(binding.etEmail);
        String password = safeText(binding.etPassword);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Invalid Email");
            shake(binding.tilEmail);
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Password too short");
            shake(binding.tilPassword);
            return;
        }

        binding.btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMain();
                    } else {
                        binding.btnLogin.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Login Failed";
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void shake(android.view.View view) {
        android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
        view.startAnimation(anim);
    }

    private String safeText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void playEntranceAnimations() {
        binding.headerBand.setAlpha(0f);
        binding.headerBand.setTranslationY(-40f);
        binding.headerBand.animate().alpha(1f).translationY(0f).setDuration(480).setInterpolator(new DecelerateInterpolator()).start();

        binding.cardLogin.setAlpha(0f);
        binding.cardLogin.setTranslationY(72f);
        binding.cardLogin.animate().alpha(1f).translationY(0f).setStartDelay(220).setDuration(520).setInterpolator(new DecelerateInterpolator(1.5f)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}