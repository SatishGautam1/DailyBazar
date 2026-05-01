package com.nighttech.dailybazar.ui.bottomsheet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.BottomSheetEditProfileBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "EditProfileBottomSheet";

    private BottomSheetEditProfileBinding binding;
    private Uri selectedImageUri = null;
    private OnProfileSaveListener saveListener;

    // ─── Callback ──────────────────────────────────────────────

    public interface OnProfileSaveListener {
        void onSave(String newName, String newPref, String newImageUrl);
    }

    public void setOnSaveListener(OnProfileSaveListener listener) {
        this.saveListener = listener;
    }

    // ─── Factory ───────────────────────────────────────────────

    public static EditProfileBottomSheet newInstance(String name, String pref, String phone) {
        EditProfileBottomSheet sheet = new EditProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("pref", pref);
        args.putString("phone", phone);
        sheet.setArguments(args);
        return sheet;
    }

    // ─── Image Picker ──────────────────────────────────────────
    //
    // KEY FIX: Use ACTION_GET_CONTENT (not ACTION_OPEN_DOCUMENT).
    // ACTION_OPEN_DOCUMENT grants a persistable URI permission that many
    // gallery / Files apps do NOT support, causing the upload to receive
    // a SecurityException or an invalid stream → "object does not exist".
    // ACTION_GET_CONTENT works universally and gives us a readable URI
    // for the duration of the process.

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {

                            selectedImageUri = result.getData().getData();

                            if (binding != null && isAdded()) {
                                Glide.with(requireContext())
                                        .load(selectedImageUri)
                                        .placeholder(R.drawable.ic_profile)
                                        .circleCrop()
                                        .into(binding.ivEditAvatar);
                            }
                        }
                    });

    // ─────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Pre-fill from arguments
        Bundle args = getArguments();
        if (args != null) {
            binding.etEditName.setText(args.getString("name", ""));
            binding.etEditPhone.setText(args.getString("phone", ""));
            binding.etEditPref.setText(args.getString("pref", ""));
        }

        // Load current avatar from Firebase Auth if available
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(requireContext())
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(binding.ivEditAvatar);
        }

        binding.flAvatarContainer.setOnClickListener(v -> openImagePicker());
        binding.btnSaveProfile.setOnClickListener(v -> startSaveProcess());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ─────────────────────────────────────────────────────────────
    //  Image Picker
    // ─────────────────────────────────────────────────────────────

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Profile Photo"));
    }

    // ─────────────────────────────────────────────────────────────
    //  Save Flow
    // ─────────────────────────────────────────────────────────────

    private void startSaveProcess() {
        if (binding == null) return;

        String newName  = binding.etEditName.getText().toString().trim();
        String newPhone = binding.etEditPhone.getText().toString().trim();
        String newPref  = binding.etEditPref.getText().toString().trim();

        if (newName.isEmpty()) {
            binding.etEditName.setError("Name is required");
            binding.etEditName.requestFocus();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showToast("Session expired. Please log in again.");
            return;
        }

        setLoading(true);

        if (selectedImageUri != null) {
            uploadImageThenSave(user, newName, newPref, newPhone);
        } else {
            updateFirestore(user, newName, newPref, newPhone, null);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Firebase Storage — Image Upload
    // ─────────────────────────────────────────────────────────────
    //
    //  KEY FIX: putStream() + ContentResolver.openInputStream() instead of putFile().
    //
    //  putFile(uri) internally calls ContentResolver.openFileDescriptor() which can
    //  fail with a SecurityException or return null on certain launchers / Android
    //  versions (especially Android 13+ with READ_MEDIA_IMAGES permission changes),
    //  causing the Storage SDK to throw "Object does not exist at location".
    //
    //  putStream() with an explicit InputStream bypasses all URI permission issues
    //  because we open the stream ourselves in the app's own process.

    private void uploadImageThenSave(FirebaseUser user,
                                     String name, String pref, String phone) {
        InputStream inputStream = null;
        try {
            inputStream = requireContext()
                    .getContentResolver()
                    .openInputStream(selectedImageUri);
        } catch (IOException e) {
            handleError("Cannot read image: " + e.getMessage());
            return;
        }

        if (inputStream == null) {
            handleError("Could not open image file. Please try another photo.");
            return;
        }

        final InputStream streamToUpload = inputStream;

        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images/" + user.getUid() + ".jpg");

        fileRef.putStream(streamToUpload)
                .addOnSuccessListener(taskSnapshot -> {
                    // Close the stream after upload completes
                    try { streamToUpload.close(); } catch (IOException ignored) {}

                    // Now fetch the public download URL
                    fileRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                if (!isAdded() || binding == null) return;
                                updateFirestore(user, name, pref, phone, downloadUri.toString());
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded() || binding == null) return;
                                handleError("Could not get image URL: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    try { streamToUpload.close(); } catch (IOException ignored) {}
                    if (!isAdded() || binding == null) return;
                    handleError("Image upload failed: " + e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────
    //  Firestore — Merge-Write User Document
    // ─────────────────────────────────────────────────────────────

    private void updateFirestore(FirebaseUser user,
                                 String name, String pref, String phone,
                                 @Nullable String photoUrl) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",            name);            // Canonical name field
        updates.put("fullName",        name);            // Also update fullName so old code stays in sync
        updates.put("marketPreference", pref);
        updates.put("phone",           phone);
        updates.put("lastUpdated",     FieldValue.serverTimestamp());

        if (photoUrl != null) {
            // Only overwrite profileImageUrl when the user actually picked a new photo
            updates.put("profileImageUrl", photoUrl);
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                // SetOptions.merge() = create the document if missing, or merge
                // fields into an existing one. Never wipes un-listed fields.
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || binding == null) return;

                    // Mirror name in Firebase Auth for quick reads elsewhere (non-critical)
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build());

                    handleSuccess(name, pref, photoUrl);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    handleError("Save failed: " + e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────
    //  Result Handlers
    // ─────────────────────────────────────────────────────────────

    private void handleSuccess(String name, String pref, @Nullable String photoUrl) {
        if (binding == null) return;
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
        if (binding == null) return;
        setLoading(false);
        binding.btnSaveProfile.setEnabled(true);
        binding.btnSaveProfile.setText("Try Again");
        showToast(message);
    }

    // ─────────────────────────────────────────────────────────────
    //  UI Helpers
    // ─────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveProfile.setEnabled(!loading);
        if (loading) binding.btnSaveProfile.setText("Saving…");
    }

    private void showToast(String msg) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
        }
    }
}