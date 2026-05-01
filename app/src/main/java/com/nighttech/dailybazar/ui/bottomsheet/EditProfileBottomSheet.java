package com.nighttech.dailybazar.ui.bottomsheet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nighttech.dailybazar.databinding.BottomSheetEditProfileBinding;

import java.util.HashMap;
import java.util.Map;

public class EditProfileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "EditProfileBottomSheet";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_PREF = "arg_pref";

    private BottomSheetEditProfileBinding binding;
    private OnSaveListener saveListener;

    public interface OnSaveListener {
        void onSaved(String name, String preference);
    }

    public static EditProfileBottomSheet newInstance(String name, String pref) {
        EditProfileBottomSheet sheet = new EditProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_PREF, pref);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }

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

        // Pre-fill with current values
        if (getArguments() != null) {
            binding.etEditName.setText(getArguments().getString(ARG_NAME, ""));
            binding.etEditPref.setText(getArguments().getString(ARG_PREF, ""));
        }

        binding.btnSaveProfile.setOnClickListener(v -> attemptSave());
        binding.btnCancelEdit.setOnClickListener(v -> dismiss());
    }

    private void attemptSave() {
        String name = safeText(binding.etEditName);
        String pref = safeText(binding.etEditPref);

        binding.tilEditName.setError(null);
        if (TextUtils.isEmpty(name)) {
            binding.tilEditName.setError("Name cannot be empty");
            return;
        }

        // Save to Firestore
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) { dismiss(); return; }

        binding.btnSaveProfile.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("marketPreference", pref);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (saveListener != null) saveListener.onSaved(name, pref);
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    binding.btnSaveProfile.setEnabled(true);
                    Toast.makeText(getContext(),
                            "Save failed. Check connection.", Toast.LENGTH_SHORT).show();
                });
    }

    private String safeText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}