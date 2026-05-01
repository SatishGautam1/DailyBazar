package com.nighttech.dailybazar.ui.bottomsheet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.nighttech.dailybazar.databinding.BottomSheetEditProfileBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ════════════════════════════════════════════════════════════════
 *  CLOUDINARY 401 FIX — READ BEFORE CHANGING ANYTHING
 * ════════════════════════════════════════════════════════════════
 *  HTTP 401 from Cloudinary always means one of three things:
 *
 *  1. Wrong cloud name  →  fix CLOUDINARY_CLOUD_NAME below
 *  2. Preset is SIGNED  →  go to Cloudinary Console →
 *                          Settings → Upload → find your preset →
 *                          change "Signing Mode" to UNSIGNED → Save
 *  3. Preset name typo  →  fix CLOUDINARY_UPLOAD_PRESET below
 *
 *  HOW TO FIND YOUR VALUES:
 *  • Cloud name  : https://console.cloudinary.com  (shown on Dashboard)
 *  • Preset name : Console → Settings → Upload → Upload presets
 *
 *  DO NOT add api_key / api_secret here — unsigned presets need neither.
 *  DO NOT pass .option("folder", ...) — folder routing requires a signed preset.
 * ════════════════════════════════════════════════════════════════
 */
public class EditProfileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG    = "EditProfileBottomSheet";
    private static final String LOG   = "KYC_Upload";

    // ── ★ CHANGE THESE TWO VALUES ★ ──────────────────────────────────────────
    // Both must exactly match what is shown in your Cloudinary dashboard.
    // They are case-sensitive.
    private static final String CLOUD_NAME    = "dwwz8f5jd";    // e.g. "dxyz1abc"
    private static final String UPLOAD_PRESET = "daily_bazar_preset"; // must be UNSIGNED
    // ─────────────────────────────────────────────────────────────────────────

    private BottomSheetEditProfileBinding binding;

    // Local URIs chosen by the user (null = not selected this session)
    private Uri avatarUri      = null;
    private Uri citizenshipUri = null;
    private Uri nidUri         = null;

    // Cloudinary URLs returned after successful uploads
    private String uploadedAvatarUrl      = null;
    private String uploadedCitizenshipUrl = null;
    private String uploadedNidUrl         = null;

    // Thread-safe: Cloudinary callbacks arrive on background threads
    private final AtomicInteger pendingUploads = new AtomicInteger(0);
    private final AtomicBoolean uploadFailed   = new AtomicBoolean(false);

    private OnProfileSaveListener saveListener;

    // ── Public API ────────────────────────────────────────────────────────────

    public interface OnProfileSaveListener {
        void onSave(String name, String address, @Nullable String newAvatarUrl);
    }

    public void setOnSaveListener(OnProfileSaveListener listener) {
        this.saveListener = listener;
    }

    public static EditProfileBottomSheet newInstance(
            @Nullable String name,
            @Nullable String address,
            @Nullable String phone) {

        EditProfileBottomSheet sheet = new EditProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString("name",    name    != null ? name    : "");
        args.putString("address", address != null ? address : "");
        args.putString("phone",   phone   != null ? phone   : "");
        sheet.setArguments(args);
        return sheet;
    }

    // ── Activity-result launchers ─────────────────────────────────────────────
    // Must be declared as fields (not inside onViewCreated) so they are registered
    // before the fragment's onStart — this is a strict AndroidX requirement.

    private final ActivityResultLauncher<Intent> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != Activity.RESULT_OK) return;
                if (r.getData() == null || r.getData().getData() == null) return;
                avatarUri = r.getData().getData();
                if (binding != null && isAdded())
                    Glide.with(this).load(avatarUri).circleCrop().into(binding.ivEditAvatar);
            });

    private final ActivityResultLauncher<Intent> pickCitizenship =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != Activity.RESULT_OK) return;
                if (r.getData() == null || r.getData().getData() == null) return;
                citizenshipUri = r.getData().getData();
                if (binding != null) {
                    binding.ivCitizenshipPreview.setImageURI(citizenshipUri);
                    binding.tvCitizenshipLabel.setText("Selected ✓");
                }
            });

    private final ActivityResultLauncher<Intent> pickNid =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != Activity.RESULT_OK) return;
                if (r.getData() == null || r.getData().getData() == null) return;
                nidUri = r.getData().getData();
                if (binding != null) {
                    binding.ivNidPreview.setImageURI(nidUri);
                    binding.tvNidLabel.setText("Selected ✓");
                }
            });

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

        initCloudinary(); // safe to call multiple times

        Bundle args = getArguments();
        if (args != null) {
            binding.etEditName.setText(args.getString("name", ""));
            binding.etEditPhone.setText(args.getString("phone", ""));
            binding.etEditAddress.setText(args.getString("address", ""));
        }

        binding.flAvatarContainer.setOnClickListener(v -> openPicker(pickAvatar));
        binding.cardUploadCitizenship.setOnClickListener(v -> openPicker(pickCitizenship));
        binding.cardUploadNid.setOnClickListener(v -> openPicker(pickNid));
        binding.btnSaveProfile.setOnClickListener(v -> validateAndUpload());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── Cloudinary init ───────────────────────────────────────────────────────

    /**
     * Initialises Cloudinary with ONLY cloud_name.
     *
     * WHY only cloud_name?
     *   Unsigned uploads do not need api_key or api_secret.
     *   Passing api_key here makes the SDK attempt signed authentication,
     *   which will cause 401 if your preset is unsigned.
     *
     * BETTER: move this block to your Application.onCreate() so it runs once:
     *
     *   Map<String, String> cfg = new HashMap<>();
     *   cfg.put("cloud_name", "YOUR_CLOUD_NAME");
     *   MediaManager.init(this, cfg);
     */
    private void initCloudinary() {
        if (getContext() == null) return;
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            // Do NOT add "api_key" or "api_secret" here for unsigned uploads
            MediaManager.init(requireContext().getApplicationContext(), config);
            Log.d(LOG, "Cloudinary initialised with cloud: " + CLOUD_NAME);
        } catch (IllegalStateException e) {
            Log.d(LOG, "Cloudinary already initialised — OK");
        } catch (Exception e) {
            Log.e(LOG, "Cloudinary init error: " + e.getMessage(), e);
        }
    }

    // ── Picker ────────────────────────────────────────────────────────────────

    private void openPicker(ActivityResultLauncher<Intent> launcher) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            launcher.launch(Intent.createChooser(i, "Select Image"));
        } catch (Exception e) {
            showToast("No gallery app found on this device.");
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateAndUpload() {
        if (binding == null) return;

        String name = safeText(binding.etEditName.getText());
        if (name.isEmpty()) {
            binding.tilEditName.setError("Full name is required");
            binding.etEditName.requestFocus();
            return;
        }
        binding.tilEditName.setError(null);

        String phone = safeText(binding.etEditPhone.getText());
        if (!phone.isEmpty() && phone.length() != 10) {
            binding.tilEditPhone.setError("Enter a valid 10-digit number");
            binding.etEditPhone.requestFocus();
            return;
        }
        binding.tilEditPhone.setError(null);

        // Reset state for this attempt
        uploadFailed.set(false);
        uploadedAvatarUrl      = null;
        uploadedCitizenshipUrl = null;
        uploadedNidUrl         = null;

        int count = 0;
        if (avatarUri      != null) count++;
        if (citizenshipUri != null) count++;
        if (nidUri         != null) count++;

        setLoading(true);

        if (count == 0) {
            updateFirestore(); // no images — just save text fields
        } else {
            pendingUploads.set(count);
            if (avatarUri      != null) upload(avatarUri,      "avatar");
            if (citizenshipUri != null) upload(citizenshipUri, "citizenship");
            if (nidUri         != null) upload(nidUri,         "nid");
        }
    }

    // ── Cloudinary upload ─────────────────────────────────────────────────────

    private void upload(Uri uri, String slot) {
        Log.d(LOG, "Starting upload for slot: " + slot + "  preset: " + UPLOAD_PRESET);

        try {
            MediaManager.get()
                    .upload(uri)
                    // unsigned() sends the preset name WITHOUT an API signature.
                    // If you get 401, your preset is still set to "Signed" in
                    // the Cloudinary Console → Settings → Upload → Upload presets.
                    .unsigned(UPLOAD_PRESET)
                    // ── DO NOT add .option("folder", ...) here ──
                    // folder routing requires a signed preset and will cause 401.
                    .callback(new UploadCallback() {

                        @Override public void onStart(String id) {
                            Log.d(LOG, "onStart: " + slot);
                        }

                        @Override public void onProgress(String id, long bytes, long total) { }

                        @Override
                        public void onSuccess(String id, Map resultData) {
                            Log.d(LOG, "onSuccess: " + slot);
                            postToMain(() -> {
                                if (uploadFailed.get()) return;

                                String url = resultData != null
                                        ? (String) resultData.get("secure_url") : null;

                                if (url == null || url.isEmpty()) {
                                    failUpload("Cloudinary returned no URL for: " + slot);
                                    return;
                                }

                                switch (slot) {
                                    case "avatar":      uploadedAvatarUrl      = url; break;
                                    case "citizenship": uploadedCitizenshipUrl = url; break;
                                    case "nid":         uploadedNidUrl         = url; break;
                                }

                                if (pendingUploads.decrementAndGet() == 0) {
                                    updateFirestore();
                                }
                            });
                        }

                        @Override
                        public void onError(String id, ErrorInfo error) {
                            // error.getDescription() contains the full JSON from Cloudinary,
                            // including the "error.message" field from the 401 response.
                            String desc = error != null ? error.getDescription() : "null";
                            Log.e(LOG, "onError [" + slot + "]: " + desc);

                            // Parse a helpful message for the user
                            String userMsg = friendlyCloudinaryError(desc, slot);
                            postToMain(() -> failUpload(userMsg));
                        }

                        @Override public void onReschedule(String id, ErrorInfo e) {
                            Log.w(LOG, "Rescheduled: " + slot);
                        }
                    })
                    .dispatch();

        } catch (Exception e) {
            Log.e(LOG, "upload() threw for slot=" + slot, e);
            failUpload("Upload could not start (" + slot + "): " + e.getMessage());
        }
    }

    /**
     * Translates Cloudinary error descriptions into user-friendly messages.
     * The raw description is also logged so you can debug the exact cause.
     */
    private String friendlyCloudinaryError(String rawDesc, String slot) {
        if (rawDesc == null) return "Unknown upload error for " + slot;

        String lower = rawDesc.toLowerCase();

        if (lower.contains("401") || lower.contains("unauthorized")) {
            return "Upload blocked (401). Fix: open Cloudinary Console → "
                    + "Settings → Upload → find preset \"" + UPLOAD_PRESET
                    + "\" → set Signing Mode to UNSIGNED → Save.";
        }
        if (lower.contains("404") || lower.contains("not found")) {
            return "Upload preset \"" + UPLOAD_PRESET + "\" not found on cloud \""
                    + CLOUD_NAME + "\". Check both values in EditProfileBottomSheet.java.";
        }
        if (lower.contains("invalid")) {
            return "Invalid request to Cloudinary. Check cloud name \""
                    + CLOUD_NAME + "\" is correct.";
        }
        return "Upload failed (" + slot + "): " + rawDesc;
    }

    /** Called once on the first upload error; suppresses duplicate toasts. */
    private void failUpload(String message) {
        if (uploadFailed.getAndSet(true)) return;
        handleError(message);
    }

    // ── Firestore write ───────────────────────────────────────────────────────

    /**
     * Writes profile data to Firestore.
     *
     * WHY we check docExists first:
     *   Firestore security rules treat  set(data, merge())  as an UPDATE when
     *   the document already exists, and as a CREATE when it does not.
     *   Our create rule does NOT allow 'role' — the update rule does NOT allow
     *   setting kycStatus to anything other than "PENDING".
     *
     *   By reading the document first we can:
     *   • On CREATE: omit kycStatus so the create rule passes cleanly,
     *     then immediately set it to PENDING in a second call that hits the
     *     update rule (which allows PENDING).
     *   • On UPDATE: include kycStatus = "PENDING" directly — allowed by rule.
     *
     *   SIMPLEST ALTERNATIVE: In Firebase Console → Firestore → Rules,
     *   paste the rules from firestore_rules_and_admin_guide.js  and the
     *   single set(data, merge()) call will work for both new and existing docs.
     */
    private void updateFirestore() {
        if (binding == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            handleError("Session expired — please log in again.");
            return;
        }

        String name    = safeText(binding.etEditName.getText());
        String phone   = safeText(binding.etEditPhone.getText());
        String address = safeText(binding.etEditAddress.getText());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        // Check whether the document exists so we pick the right write path
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || binding == null) return;

                    Map<String, Object> data = new HashMap<>();
                    data.put("name",        name);
                    data.put("phone",       phone);
                    data.put("address",     address);
                    data.put("lastUpdated", FieldValue.serverTimestamp());

                    if (uploadedAvatarUrl      != null) data.put("profileImageUrl", uploadedAvatarUrl);
                    if (uploadedCitizenshipUrl != null) data.put("citizenshipUrl",  uploadedCitizenshipUrl);
                    if (uploadedNidUrl         != null) data.put("nidUrl",          uploadedNidUrl);

                    if (!snapshot.exists()) {
                        // ── FIRST-TIME SAVE (CREATE) ──────────────────────────
                        // Do NOT include kycStatus here — the create rule blocks 'role'
                        // and we want the doc to be created cleanly first.
                        // After creation succeeds we immediately set kycStatus = PENDING
                        // via an update, which the update rule allows.
                        data.put("email", user.getEmail() != null ? user.getEmail() : "");

                        Log.d(LOG, "CREATE new user doc, fields: " + data.keySet());

                        db.collection("users").document(uid)
                                .set(data)                          // plain set = create
                                .addOnSuccessListener(v -> {
                                    if (!isAdded()) return;
                                    // Now set kycStatus = PENDING via update (allowed by rules)
                                    Map<String, Object> statusUpdate = new HashMap<>();
                                    statusUpdate.put("kycStatus", "PENDING");
                                    db.collection("users").document(uid)
                                            .update(statusUpdate)
                                            .addOnSuccessListener(v2 -> {
                                                if (isAdded()) handleSuccess(name, address);
                                            })
                                            .addOnFailureListener(e -> {
                                                // Profile was created; only kycStatus failed.
                                                // Still call success so the user isn't left hanging.
                                                Log.w(LOG, "kycStatus update failed (non-fatal): " + e.getMessage());
                                                if (isAdded()) handleSuccess(name, address);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(LOG, "Firestore CREATE failed", e);
                                    if (isAdded()) handleError("Could not save profile: " + e.getMessage());
                                });

                    } else {
                        // ── EXISTING DOC (UPDATE / MERGE) ─────────────────────
                        // kycStatus = "PENDING" is allowed by the update rule.
                        data.put("kycStatus", "PENDING");

                        Log.d(LOG, "UPDATE existing doc, fields: " + data.keySet());

                        db.collection("users").document(uid)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener(v -> {
                                    if (isAdded()) handleSuccess(name, address);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(LOG, "Firestore UPDATE failed", e);
                                    if (isAdded()) handleError("Could not save profile: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG, "Could not check doc existence", e);
                    if (isAdded()) handleError("Could not reach Firestore: " + e.getMessage());
                });
    }

    // ── Result handlers ───────────────────────────────────────────────────────

    private void handleSuccess(String name, String address) {
        setLoading(false);
        if (saveListener != null) saveListener.onSave(name, address, uploadedAvatarUrl);
        dismiss();
    }

    private void handleError(String msg) {
        setLoading(false);
        showToast(msg);
    }

    private void setLoading(boolean on) {
        if (binding == null) return;
        binding.progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        binding.btnSaveProfile.setEnabled(!on);
        binding.btnSaveProfile.setText(on ? "Uploading…" : "Submit for Verification");
        binding.flAvatarContainer.setEnabled(!on);
        binding.cardUploadCitizenship.setEnabled(!on);
        binding.cardUploadNid.setEnabled(!on);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Posts to the main thread only if the fragment is still alive. */
    private void postToMain(@NonNull Runnable action) {
        Activity a = getActivity();
        if (a == null || a.isFinishing() || !isAdded()) return;
        a.runOnUiThread(() -> { if (binding != null) action.run(); });
    }

    private void showToast(String msg) {
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    @NonNull
    private static String safeText(@Nullable android.text.Editable e) {
        return e != null ? e.toString().trim() : "";
    }
}