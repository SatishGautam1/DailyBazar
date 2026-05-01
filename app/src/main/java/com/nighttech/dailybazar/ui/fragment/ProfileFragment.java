package com.nighttech.dailybazar.ui.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.FragmentProfileBinding;
import com.nighttech.dailybazar.ui.activities.LoginActivity;
import com.nighttech.dailybazar.ui.bottomsheet.EditProfileBottomSheet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    // ── KYC Status Constants — must match Firestore values exactly ────────────
    private static final String KYC_UNVERIFIED = "UNVERIFIED";
    private static final String KYC_PENDING    = "PENDING";
    private static final String KYC_VERIFIED   = "VERIFIED";
    private static final String KYC_REJECTED   = "REJECTED";

    // ── Colors (resolved at runtime to support dark/light themes) ────────────
    private static final String COLOR_GREEN = "#4CAF50";
    private static final String COLOR_RED   = "#F44336";

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userRef;
    private FirebaseUser currentUser;

    /** Real-time listener — removed in onDestroyView to prevent leaks. */
    private ListenerRegistration profileListener;

    // Cached values for passing to the bottom-sheet
    private String currentName    = "";
    private String currentPhone   = "";
    private String currentAddress = "";

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

        mAuth        = FirebaseAuth.getInstance();
        db           = FirebaseFirestore.getInstance();
        currentUser  = mAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = db.collection("users").document(currentUser.getUid());
        }

        setupSwipeRefresh();
        setupButtons();

        // Seed the email field immediately (no network call needed)
        if (currentUser != null && binding.tvProfileEmail != null) {
            String email = currentUser.getEmail();
            binding.tvProfileEmail.setText(email != null && !email.isEmpty() ? email : "—");
        }

        // Attach real-time listener so that admin-side status changes reflect instantly
        attachRealtimeListener();
    }

    @Override
    public void onDestroyView() {
        // Detach the Firestore listener to avoid callbacks against a null binding
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
        super.onDestroyView();
        binding = null;
    }

    // ── Firestore real-time listener ──────────────────────────────────────────

    /**
     * Attaches a snapshot listener so any admin change to kycStatus is pushed
     * to the UI immediately without requiring a manual refresh.
     */
    private void attachRealtimeListener() {
        if (currentUser == null || userRef == null) {
            stopRefreshing();
            return;
        }

        profileListener = userRef.addSnapshotListener((snapshot, error) -> {
            stopRefreshing();
            if (!isAdded() || binding == null) return;

            if (error != null) {
                Toast.makeText(getContext(),
                        "Could not load profile: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                populateUI(snapshot);
                updateLastSynced();
            } else {
                // Document does not exist yet — show defaults
                binding.tvProfileName.setText("User");
                binding.tvProfilePhone.setText("Not set");
                binding.tvProfileAddress.setText("Not set");
                setDocumentStatus(binding.tvStatusCitizenship, null);
                setDocumentStatus(binding.tvStatusNid,         null);
                updateKycUI(KYC_UNVERIFIED, null);
            }
        });
    }

    /** One-shot fetch used by swipe-to-refresh (the listener already keeps data fresh). */
    private void loadUserData() {
        if (userRef == null) { stopRefreshing(); return; }
        // The real-time listener already keeps data fresh;
        // swipe-to-refresh just forces a get() to confirm connectivity.
        userRef.get()
                .addOnSuccessListener(doc -> {
                    stopRefreshing();
                    if (!isAdded() || binding == null) return;
                    if (doc.exists()) {
                        populateUI(doc);
                        updateLastSynced();
                    }
                })
                .addOnFailureListener(e -> {
                    stopRefreshing();
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(getContext(),
                            "Refresh failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── UI population ─────────────────────────────────────────────────────────

    private void populateUI(DocumentSnapshot doc) {
        if (binding == null) return;

        // ── Name ──
        String name = doc.getString("name");
        if (name == null || name.isEmpty()) name = doc.getString("fullName");
        currentName = (name != null) ? name : "";
        binding.tvProfileName.setText(currentName.isEmpty() ? "User" : currentName);

        // ── Contact details ──
        currentPhone   = orEmpty(doc.getString("phone"));
        currentAddress = orEmpty(doc.getString("address"));
        binding.tvProfilePhone.setText(currentPhone.isEmpty()   ? "Not set" : currentPhone);
        binding.tvProfileAddress.setText(currentAddress.isEmpty() ? "Not set" : currentAddress);

        // ── Document status ──
        setDocumentStatus(binding.tvStatusCitizenship, doc.getString("citizenshipUrl"));
        setDocumentStatus(binding.tvStatusNid,         doc.getString("nidUrl"));

        // ── Profile image ──
        String imageUrl = doc.getString("profileImageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadProfileImage(imageUrl);
        }

        // ── KYC badge + button ──
        String kycStatus = doc.getString("kycStatus");
        if (kycStatus == null || kycStatus.isEmpty()) kycStatus = KYC_UNVERIFIED;
        String rejectionReason = doc.getString("rejectionReason"); // set by admin
        updateKycUI(kycStatus, rejectionReason);
    }

    /**
     * Sets the document-status TextView text and colour.
     *
     * @param textView the status label view
     * @param url      the stored Cloudinary URL, or null/empty if missing
     */
    private void setDocumentStatus(@Nullable TextView textView, @Nullable String url) {
        if (textView == null) return;
        boolean present = url != null && !url.trim().isEmpty();
        textView.setText(present ? "Uploaded ✓" : "Missing");
        textView.setTextColor(Color.parseColor(present ? COLOR_GREEN : COLOR_RED));
    }

    /**
     * Updates the KYC chip, action button text/state, and the rejection reason card.
     *
     * @param status          one of UNVERIFIED / PENDING / VERIFIED / REJECTED
     * @param rejectionReason plain-text reason supplied by admin; may be null
     */
    private void updateKycUI(@NonNull String status, @Nullable String rejectionReason) {
        if (binding == null) return;

        // Hide rejection card by default
        binding.cardRejectionReason.setVisibility(View.GONE);

        switch (status) {

            case KYC_PENDING:
                setChipAppearance("#FF9800", "KYC Pending ⏳");
                binding.btnEditProfile.setText("Awaiting Admin Approval");
                binding.btnEditProfile.setEnabled(false);
                break;

            case KYC_VERIFIED:
                setChipAppearance("#4CAF50", "✓ Verified Member");
                binding.btnEditProfile.setText("Update KYC Documents");
                binding.btnEditProfile.setEnabled(true);
                break;

            case KYC_REJECTED:
                setChipAppearance("#F44336", "KYC Rejected ✗");
                binding.btnEditProfile.setText("Re-upload KYC Documents");
                binding.btnEditProfile.setEnabled(true);
                // Show rejection reason card if the admin supplied a reason
                if (rejectionReason != null && !rejectionReason.trim().isEmpty()) {
                    binding.cardRejectionReason.setVisibility(View.VISIBLE);
                    binding.tvRejectionReason.setText(rejectionReason.trim());
                }
                break;

            case KYC_UNVERIFIED:
            default:
                setChipAppearance("#757575", "Unverified");
                binding.btnEditProfile.setText("Upload KYC Documents");
                binding.btnEditProfile.setEnabled(true);
                break;
        }
    }

    /** Applies a background colour and label to the KYC chip. */
    private void setChipAppearance(@NonNull String hexColor, @NonNull String label) {
        if (binding == null) return;
        binding.chipKycStatus.setChipBackgroundColor(
                ColorStateList.valueOf(Color.parseColor(hexColor)));
        binding.chipKycStatus.setText(label);
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    private void loadProfileImage(@NonNull String url) {
        if (!isAdded() || getContext() == null || binding == null) return;
        Glide.with(requireContext())
                .load(url)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivBigAvatar);
    }

    // ── SwipeRefresh ──────────────────────────────────────────────────────────

    private void setupSwipeRefresh() {
        binding.swipeRefreshProfile.setColorSchemeResources(R.color.brand_primary);
        binding.swipeRefreshProfile.setOnRefreshListener(this::loadUserData);
    }

    private void stopRefreshing() {
        if (binding != null) binding.swipeRefreshProfile.setRefreshing(false);
    }

    // ── Last Synced ───────────────────────────────────────────────────────────

    private void updateLastSynced() {
        if (binding == null) return;
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        binding.tvLastSynced.setText("Last synced: " + time);
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private void setupButtons() {
        binding.btnEditProfile.setOnClickListener(v -> openKycBottomSheet());

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

    private void openKycBottomSheet() {
        if (!isAdded()) return;

        EditProfileBottomSheet sheet = EditProfileBottomSheet.newInstance(
                currentName, currentAddress, currentPhone);

        sheet.setOnSaveListener((newName, newAddress, newAvatarUrl) -> {
            // The real-time listener will automatically refresh the UI
            // once Firestore confirms the write. No manual reload needed.
        });

        sheet.show(getChildFragmentManager(), EditProfileBottomSheet.TAG);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @NonNull
    private static String orEmpty(@Nullable String value) {
        return value != null ? value : "";
    }
}