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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.ActivitySignupBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private static final int TOOLBAR_HEIGHT_DP = 56;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        applyWindowInsets();
        playEntranceAnimations();
        setupListeners();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            float density = getResources().getDisplayMetrics().density;
            int toolbarPx = (int) (TOOLBAR_HEIGHT_DP * density);
            int borderPx  = (int) (2 * density);

            ViewGroup.LayoutParams spacer = binding.statusBarSpacer.getLayoutParams();
            spacer.height = systemBars.top;
            binding.statusBarSpacer.setLayoutParams(spacer);

            ViewGroup.LayoutParams formSpacer = binding.formTopSpacer.getLayoutParams();
            formSpacer.height = systemBars.top + toolbarPx + borderPx + (int) (8 * density);
            binding.formTopSpacer.setLayoutParams(formSpacer);

            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            binding.formContainer.setPadding(
                    binding.formContainer.getPaddingLeft(),
                    binding.formContainer.getPaddingTop(),
                    binding.formContainer.getPaddingRight(),
                    imeInsets.bottom > 0
                            ? imeInsets.bottom + (int) (16 * density)
                            : systemBars.bottom + (int) (48 * density)
            );

            return windowInsets;
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnGoToLogin.setOnClickListener(v -> navigateBack());
        binding.btnCreateAccount.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        binding.tilFullName.setError(null);
        binding.tilPhone.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        String name     = safeText(binding.etFullName);
        String phone    = safeText(binding.etPhone);
        String email    = safeText(binding.etEmail);
        String password = safeText(binding.etPassword);
        String confirm  = safeText(binding.etConfirmPassword);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            binding.tilFullName.setError(getString(R.string.signup_name_error));
            shake(binding.tilFullName);
            valid = false;
        }
        if (phone.length() != 10) {
            binding.tilPhone.setError(getString(R.string.signup_phone_error));
            shake(binding.tilPhone);
            valid = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.signup_email_error));
            shake(binding.tilEmail);
            valid = false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.signup_password_error));
            shake(binding.tilPassword);
            valid = false;
        }
        if (!password.equals(confirm)) {
            binding.tilConfirmPassword.setError(getString(R.string.signup_confirm_password_error));
            shake(binding.tilConfirmPassword);
            valid = false;
        }

        if (valid) {
            binding.btnCreateAccount.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserToFirestore(user, name, phone, email);
                            }
                        } else {
                            binding.btnCreateAccount.setEnabled(true);
                            String error = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Auth Failed";
                            Toast.makeText(SignUpActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveUserToFirestore(FirebaseUser user, String name, String phone, String email) {
        // Update Firebase Auth display name
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(profileUpdates);

        // FIX: Save BOTH "name" and "fullName" fields so both ProfileFragment and
        // older code that reads "fullName" work without a migration.
        // FIX: Save "memberSince" as a formatted string so ProfileFragment can display it.
        String memberSince = new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date());

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid",         user.getUid());
        userData.put("name",        name);       // Primary field read by ProfileFragment
        userData.put("fullName",    name);       // Legacy fallback
        userData.put("phone",       phone);
        userData.put("email",       email);
        userData.put("role",        "user");
        userData.put("memberSince", memberSince);
        userData.put("status",      "active");

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> updateUI(user))
                .addOnFailureListener(e -> {
                    binding.btnCreateAccount.setEnabled(true);
                    Toast.makeText(this, "Firestore Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void navigateBack() {
        onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private String safeText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void shake(android.view.View view) {
        android.view.animation.Animation anim =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
        view.startAnimation(anim);
    }

    private void playEntranceAnimations() {
        binding.topBar.setTranslationY(-50f);
        binding.topBar.setAlpha(0f);
        binding.topBar.animate()
                .translationY(0f).alpha(1f)
                .setDuration(380)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        binding.formContainer.setAlpha(0f);
        binding.formContainer.setTranslationY(48f);
        binding.formContainer.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(300).setDuration(480)
                .setInterpolator(new DecelerateInterpolator(1.4f))
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}