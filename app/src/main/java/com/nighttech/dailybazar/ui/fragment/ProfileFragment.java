package com.nighttech.dailybazar.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.FragmentProfileBinding;
import com.nighttech.dailybazar.ui.activities.LoginActivity;
import com.nighttech.dailybazar.ui.bottomsheet.EditProfileBottomSheet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userRef;
    private FirebaseUser currentUser;

    // Cached values passed to the edit sheet
    private String currentName  = "";
    private String currentPhone = "";
    private String currentPref  = "";

    // ─────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth       = FirebaseAuth.getInstance();
        db          = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = db.collection("users").document(currentUser.getUid());
        }

        setupSwipeRefresh();
        loadUserData();
        setupButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ─────────────────────────────────────────────────────────────
    //  SwipeRefresh
    // ─────────────────────────────────────────────────────────────

    private void setupSwipeRefresh() {
        binding.swipeRefreshProfile.setColorSchemeResources(R.color.brand_primary);
        binding.swipeRefreshProfile.setOnRefreshListener(this::loadUserData);
    }

    // ─────────────────────────────────────────────────────────────
    //  Data Loading
    // ─────────────────────────────────────────────────────────────

    private void loadUserData() {
        if (currentUser == null || userRef == null) {
            stopRefreshing();
            return;
        }

        // Show email immediately from Auth (no Firestore round-trip needed)
        safeSetText(currentUser.getEmail(), binding.tvProfileEmail, "—");

        userRef.get()
                .addOnSuccessListener(doc -> {
                    stopRefreshing();
                    if (!isAdded() || binding == null) return;

                    if (doc.exists()) {
                        populateUI(doc);
                        updateLastSynced();
                    } else {
                        // Document not yet created — show safe defaults
                        binding.tvProfileName.setText("User");
                        binding.tvProfilePhone.setText("Not set");
                        binding.tvMemberDate.setText("—");
                        binding.tvMarketPreference.setText("Not set");
                    }
                })
                .addOnFailureListener(e -> {
                    stopRefreshing();
                    if (!isAdded() || binding == null) return;
                    // Show the real Firebase error so it's actionable
                    Toast.makeText(getContext(),
                            "Could not load profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Populates every view from a Firestore snapshot.
     *
     * KEY FIX: SignUpActivity originally wrote the name under "fullName",
     * but EditProfileBottomSheet writes it under "name". We read "name"
     * first and fall back to "fullName" so both old and new accounts work.
     */
    private void populateUI(DocumentSnapshot doc) {
        // Name — prefer "name" (written by EditProfile), fall back to "fullName" (written by SignUp)
        String name = doc.getString("name");
        if (name == null || name.isEmpty()) name = doc.getString("fullName");
        currentName = (name != null) ? name : "";

        currentPhone = or(doc.getString("phone"), "");
        currentPref  = or(doc.getString("marketPreference"), "");

        // "createdAt" is a server timestamp; SignUpActivity now stores a formatted string
        // under "memberSince". Fall back to the raw field if the new field isn't there yet.
        String memberSince = doc.getString("memberSince");
        if (memberSince == null || memberSince.isEmpty()) {
            memberSince = doc.getString("createdAt");
        }

        String imageUrl = doc.getString("profileImageUrl");

        binding.tvProfileName.setText(currentName.isEmpty() ? "User" : currentName);
        binding.tvProfilePhone.setText(currentPhone.isEmpty() ? "Not set" : currentPhone);
        binding.tvMemberDate.setText((memberSince != null && !memberSince.isEmpty()) ? memberSince : "—");
        binding.tvMarketPreference.setText(currentPref.isEmpty() ? "Not set" : currentPref);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadProfileImage(imageUrl);
        }
    }

    private void safeSetText(String value, android.widget.TextView tv, String fallback) {
        if (binding != null && tv != null) {
            tv.setText((value != null && !value.isEmpty()) ? value : fallback);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Image via Glide
    // ─────────────────────────────────────────────────────────────

    private void loadProfileImage(String url) {
        if (!isAdded() || getContext() == null || binding == null) return;
        Glide.with(requireContext())
                .load(url)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivBigAvatar);
    }

    // ─────────────────────────────────────────────────────────────
    //  Last Synced
    // ─────────────────────────────────────────────────────────────

    private void updateLastSynced() {
        if (binding == null) return;
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        binding.tvLastSynced.setText("Last synced: " + time);
    }

    // ─────────────────────────────────────────────────────────────
    //  Buttons
    // ─────────────────────────────────────────────────────────────

    private void setupButtons() {
        binding.btnEditProfile.setOnClickListener(v -> openEditBottomSheet());

        binding.btnLogout.setOnClickListener(v -> {
            binding.btnLogout.setText("Signing out…");
            binding.btnLogout.setEnabled(false);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mAuth.signOut();
                if (getActivity() != null && !getActivity().isFinishing()) {
                    startActivity(new Intent(getActivity(), LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    getActivity().finish();
                }
            }, 600);
        });
    }

    private void openEditBottomSheet() {
        EditProfileBottomSheet sheet = EditProfileBottomSheet.newInstance(
                currentName, currentPref, currentPhone);
        sheet.setOnSaveListener((newName, newPref, newImageUrl) -> loadUserData());
        sheet.show(getChildFragmentManager(), EditProfileBottomSheet.TAG);
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

    private void stopRefreshing() {
        if (binding != null) binding.swipeRefreshProfile.setRefreshing(false);
    }

    private static String or(@Nullable String value, String fallback) {
        return (value != null) ? value : fallback;
    }
}