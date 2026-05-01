package com.nighttech.dailybazar.ui.bottomsheet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nighttech.dailybazar.R;
import com.nighttech.dailybazar.databinding.BottomSheetAddMarketItemBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AddMarketItemBottomSheet
 *
 * Allows admins to add a new product to the "marketItems" Firestore collection.
 *
 * Firestore document shape written:
 * {
 *   name:      "Tomato",
 *   price:     "Rs. 120/kg",
 *   imageUrl:  "https://firebasestorage.googleapis.com/...",
 *   isTrendUp: true,
 *   category:  "Vegetable"
 * }
 *
 * MarketFragment's existing real-time snapshot listener picks up the new
 * document automatically — no manual refresh required.
 *
 * Layout file required: res/layout/bottom_sheet_add_market_item.xml
 * Needs the following view IDs:
 *   ivItemImage, flImageContainer, tilItemName, etItemName,
 *   tilItemPrice, etItemPrice, tilItemCategory, etItemCategory,
 *   switchTrendUp, btnSaveItem, progressBar
 */
public class AddMarketItemBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AddMarketItemBottomSheet";

    private BottomSheetAddMarketItemBinding binding;
    private Uri selectedImageUri = null;

    // ─── Image Picker ──────────────────────────────────────────────────────
    // Use ACTION_GET_CONTENT (not ACTION_OPEN_DOCUMENT) — universally compatible
    // and avoids the persistent URI permission issues that break putFile() on
    // some launchers / Android 13+ devices.

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
                                        .placeholder(R.drawable.ic_storefront)
                                        .centerCrop()
                                        .into(binding.ivItemImage);
                            }
                        }
                    });

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddMarketItemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.flImageContainer.setOnClickListener(v -> openImagePicker());
        binding.btnSaveItem.setOnClickListener(v -> attemptSave());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ─── Image Picker ────────────────────────────────────────────────────────

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Product Image"));
    }

    // ─── Save Flow ───────────────────────────────────────────────────────────

    private void attemptSave() {
        if (binding == null) return;

        String name     = safeText(binding.etItemName);
        String price    = safeText(binding.etItemPrice);
        String category = safeText(binding.etItemCategory);
        boolean trendUp = binding.switchTrendUp.isChecked();

        boolean valid = true;

        if (name.isEmpty()) {
            binding.tilItemName.setError("Product name is required");
            valid = false;
        } else {
            binding.tilItemName.setError(null);
        }
        if (price.isEmpty()) {
            binding.tilItemPrice.setError("Price is required");
            valid = false;
        } else {
            binding.tilItemPrice.setError(null);
        }
        if (category.isEmpty()) {
            binding.tilItemCategory.setError("Category is required");
            valid = false;
        } else {
            binding.tilItemCategory.setError(null);
        }

        if (!valid) return;

        setLoading(true);

        if (selectedImageUri != null) {
            uploadImageThenSave(name, price, category, trendUp);
        } else {
            saveToFirestore(name, price, "", category, trendUp);
        }
    }

    // ─── Firebase Storage Upload ─────────────────────────────────────────────
    //
    // FIX: Use putStream() + ContentResolver.openInputStream() instead of
    // putFile(uri). putFile() calls openFileDescriptor() internally, which
    // fails with SecurityException or null on many Android 13+ devices.
    // Opening the InputStream ourselves sidesteps all URI permission problems.

    private void uploadImageThenSave(String name, String price, String category, boolean trendUp) {
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
            handleError("Could not open image. Please try another photo.");
            return;
        }

        final InputStream stream = inputStream;

        // Use a unique filename per product so re-uploads don't collide
        String filename = "market_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference()
                .child(filename);

        fileRef.putStream(stream)
                .addOnSuccessListener(snapshot -> {
                    try { stream.close(); } catch (IOException ignored) {}
                    fileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                if (!isAdded() || binding == null) return;
                                saveToFirestore(name, price, uri.toString(), category, trendUp);
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded() || binding == null) return;
                                handleError("Could not get image URL: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    try { stream.close(); } catch (IOException ignored) {}
                    if (!isAdded() || binding == null) return;
                    handleError("Image upload failed: " + e.getMessage());
                });
    }

    // ─── Firestore Write ─────────────────────────────────────────────────────

    private void saveToFirestore(String name, String price, String imageUrl,
                                 String category, boolean trendUp) {
        Map<String, Object> item = new HashMap<>();
        item.put("name",      name);
        item.put("price",     price);
        item.put("imageUrl",  imageUrl);
        item.put("category",  category);
        item.put("isTrendUp", trendUp);

        FirebaseFirestore.getInstance()
                .collection("marketItems")
                // Auto-generate document ID so concurrent adds don't overwrite each other
                .add(item)
                .addOnSuccessListener(ref -> {
                    if (!isAdded() || binding == null) return;
                    setLoading(false);
                    binding.btnSaveItem.setText("Added ✓");
                    // MarketFragment's snapshot listener picks up the new doc automatically.
                    // Dismiss after a short delay so the user sees the confirmation.
                    binding.getRoot().postDelayed(() -> {
                        if (isAdded()) dismiss();
                    }, 700);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    handleError("Save failed: " + e.getMessage());
                });
    }

    // ─── UI Helpers ──────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveItem.setEnabled(!loading);
        if (loading) binding.btnSaveItem.setText("Saving…");
    }

    private void handleError(String message) {
        setLoading(false);
        if (binding != null) {
            binding.btnSaveItem.setEnabled(true);
            binding.btnSaveItem.setText("Try Again");
        }
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private String safeText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}