package com.nighttech.dailybazar.util;

import android.content.Context;
import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * CloudinaryHelper
 *
 * Singleton wrapper around the Cloudinary Android SDK.
 * Call CloudinaryHelper.init(context) once from Application.onCreate().
 *
 * Upload flow:
 *   CloudinaryHelper.uploadImage(uri, preset, callback)
 *       → unsigned upload to cloud "dwwz8f5jd"
 *       → callback.onSuccess returns the secure_url string
 *
 * Gradle dependency required:
 *   implementation 'com.cloudinary:cloudinary-android:2.5.0'
 */
public class CloudinaryHelper {

    private static final String CLOUD_NAME = "dwwz8f5jd";
    private static final String UPLOAD_PRESET = "daily_bazar_preset";

    private static boolean initialized = false;

    /** Call once from Application.onCreate() — safe to call multiple times. */
    public static void init(Context context) {
        if (initialized) return;
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            // No api_key / api_secret — unsigned uploads only
            MediaManager.init(context.getApplicationContext(), config);
            initialized = true;
        } catch (IllegalStateException e) {
            // Already initialized by a previous call (e.g. during testing)
            initialized = true;
        }
    }

    /**
     * Callback interface — called on the calling thread (usually a background
     * thread from the Cloudinary SDK; post to main thread if you touch UI).
     */
    public interface UploadListener {
        void onSuccess(String secureUrl);
        void onFailure(String errorMessage);
    }

    /**
     * Uploads a local image URI to Cloudinary using the unsigned preset.
     *
     * @param uri      Content URI from ACTION_GET_CONTENT (never ACTION_OPEN_DOCUMENT)
     * @param folder   Storage folder inside your Cloudinary account, e.g. "profile_images"
     * @param listener Result callback
     */
    public static void uploadImage(Uri uri, String folder, UploadListener listener) {
        if (!initialized) {
            listener.onFailure("Cloudinary not initialised. Call CloudinaryHelper.init(context) first.");
            return;
        }

        MediaManager.get()
                .upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", folder)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        if (url != null) {
                            listener.onSuccess(url);
                        } else {
                            listener.onFailure("Upload succeeded but no URL returned.");
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        listener.onFailure(error.getDescription());
                    }

                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {
                        listener.onFailure("Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }

    // Convenience overload — no folder (uploads to Cloudinary root)
    public static void uploadImage(Uri uri, UploadListener listener) {
        uploadImage(uri, "", listener);
    }
}