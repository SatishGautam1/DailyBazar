package com.nighttech.dailybazar.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nighttech.dailybazar.databinding.FragmentProfileBinding;
import com.nighttech.dailybazar.ui.bottomsheet.EditProfileBottomSheet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userRef;

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

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userRef = db.collection("users").document(user.getUid());
        }

        setupSwipeRefresh();
        loadUserData();
        setupButtons();
    }

    // ── Swipe Refresh ────────────────────────────────────────────────────────

    private void setupSwipeRefresh() {
        if (getContext() != null) {
            binding.swipeRefreshProfile.setColorSchemeResources(
                    com.nighttech.dailybazar.R.color.brand_primary);
        }
        binding.swipeRefreshProfile.setOnRefreshListener(() -> loadUserData());
    }

    // ── Load Data ─────────────────────────────────────────────────────────────

    private void loadUserData() {
        if (userRef == null) {
            binding.swipeRefreshProfile.setRefreshing(false);
            return;
        }

        userRef.get()
                .addOnSuccessListener(doc -> {
                    binding.swipeRefreshProfile.setRefreshing(false);

                    if (doc.exists()) {
                        String name  = doc.getString("name");
                        String email = doc.getString("email");
                        String date  = doc.getString("createdAt");
                        String pref  = doc.getString("marketPreference");

                        binding.tvProfileName.setText(name != null ? name : "User");
                        binding.tvProfileEmail.setText(email != null ? email : "—");
                        binding.tvMemberDate.setText(date != null ? date : "—");
                        binding.tvMarketPreference.setText(
                                (pref != null && !pref.isEmpty()) ? pref : "Not set");

                        updateLastSynced();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.swipeRefreshProfile.setRefreshing(false);
                    Toast.makeText(getContext(),
                            "Error loading profile. Check connection.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLastSynced() {
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        binding.tvLastSynced.setText("Last synced: " + time);
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private void setupButtons() {
        binding.btnEditProfile.setOnClickListener(v -> openEditBottomSheet());

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            // Navigate back to LoginActivity
            if (getActivity() != null) {
                android.content.Intent intent = new android.content.Intent(
                        getActivity(),
                        com.nighttech.dailybazar.ui.activities.LoginActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().overridePendingTransition(
                        com.nighttech.dailybazar.R.anim.fade_in,
                        com.nighttech.dailybazar.R.anim.fade_in);
            }
        });
    }

    private void openEditBottomSheet() {
        // Fetch current values to pre-fill the sheet
        String currentName = binding.tvProfileName.getText().toString();
        String currentPref = binding.tvMarketPreference.getText().toString();

        EditProfileBottomSheet sheet = EditProfileBottomSheet.newInstance(
                currentName.equals("User") ? "" : currentName,
                currentPref.equals("Not set") ? "" : currentPref
        );

        // Result callback: refresh the profile after a successful save
        sheet.setOnSaveListener((name, pref) -> {
            binding.tvProfileName.setText(name);
            binding.tvMarketPreference.setText(pref.isEmpty() ? "Not set" : pref);
            updateLastSynced();
        });

        sheet.show(getChildFragmentManager(), EditProfileBottomSheet.TAG);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}