package com.nighttech.dailybazar.ui.bottomsheet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.BottomSheetEditProfileBinding;

import java.util.HashMap;
import java.util.Map;

public class EditProfileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "EditProfileBottomSheet";

    private BottomSheetEditProfileBinding binding;
    private Uri selectedImageUri = null;
    private OnProfileSaveListener saveListener;

    public interface OnProfileSaveListener {
        void onSave(String newName, String newPref, String newImageUrl);
    }

    public void setOnSaveListener(OnProfileSaveListener listener) {
        this.saveListener = listener;
    }

    public static EditProfileBottomSheet newInstance(String name, String pref, String phone) {
        EditProfileBottomSheet sheet = new EditProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("pref", pref);
        args.putString("phone", phone);
        sheet.setArguments(args);
        return sheet;
    }

    // --- Image Picker ---
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (binding != null && isAdded()) {
                        Glide.with(requireContext())
                                .load(selectedImageUri)
                                .placeholder(R.drawable.ic_profile)
                                .circleCrop()
                                .into(binding.ivEditAvatar);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetEditProfileBinding.inflate(inflater, container, false);
        initCloudinary();
        return binding.getRoot();
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dwwz8f5jd"); // Your Cloud Name
            MediaManager.init(requireContext(), config);
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            binding.etEditName.setText(args.getString("name", ""));
            binding.etEditPhone.setText(args.getString("phone", ""));
            binding.etEditPref.setText(args.getString("pref", ""));
        }

        loadCurrentAvatar();

        binding.flAvatarContainer.setOnClickListener(v -> openImagePicker());
        binding.btnSaveProfile.setOnClickListener(v -> startSaveProcess());
    }

    private void loadCurrentAvatar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get().addOnSuccessListener(doc -> {
                    if (!isAdded() || binding == null) return;
                    String imageUrl = doc.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(requireContext()).load(imageUrl).placeholder(R.drawable.ic_profile).circleCrop().into(binding.ivEditAvatar);
                    } else if (user.getPhotoUrl() != null) {
                        Glide.with(requireContext()).load(user.getPhotoUrl()).placeholder(R.drawable.ic_profile).circleCrop().into(binding.ivEditAvatar);
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Profile Photo"));
    }

    private void startSaveProcess() {
        if (binding == null) return;

        String newName = binding.etEditName.getText().toString().trim();
        String newPhone = binding.etEditPhone.getText().toString().trim();
        String newPref = binding.etEditPref.getText().toString().trim();

        if (newName.isEmpty()) {
            binding.etEditName.setError("Name is required");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        setLoading(true);

        if (selectedImageUri != null) {
            // Upload to Cloudinary
            MediaManager.get().upload(selectedImageUri)
                    .unsigned("daily_bazar_preset") // Ensure this exists in your Cloudinary Settings
                    .callback(new UploadCallback() {
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            updateFirestore(user, newName, newPref, newPhone, imageUrl);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            handleError("Cloudinary Upload Failed: " + error.getDescription());
                        }

                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override public void onStart(String requestId) {}
                    }).dispatch();
        } else {
            updateFirestore(user, newName, newPref, newPhone, null);
        }
    }

    private void updateFirestore(FirebaseUser user, String name, String pref, String phone, @Nullable String photoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("fullName", name); // Keeping both in sync
        updates.put("marketPreference", pref);
        updates.put("phone", phone);
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        if (photoUrl != null) {
            updates.put("profileImageUrl", photoUrl);
        }

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || binding == null) return;

                    // Update Auth display name
                    user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());

                    handleSuccess(name, pref, photoUrl);
                })
                .addOnFailureListener(e -> handleError("Firestore Sync Failed: " + e.getMessage()));
    }

    private void handleSuccess(String name, String pref, @Nullable String photoUrl) {
        setLoading(false);
        binding.btnSaveProfile.setText("Saved ✓");
        if (saveListener != null) {
            saveListener.onSave(name, pref, photoUrl != null ? photoUrl : "");
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) dismiss();
        }, 600);
    }

    private void handleError(String message) {
        setLoading(false);
        binding.btnSaveProfile.setText("Try Again");
        if (isAdded()) Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveProfile.setEnabled(!loading);
        if (loading) binding.btnSaveProfile.setText("Saving...");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}