package com.nighttech.dailybazar;

import android.app.Application;

import com.cloudinary.android.MediaManager;
import com.nighttech.dailybazar.util.CloudinaryHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * DailyBazarApp — Application subclass.
 *
 * Register this in AndroidManifest.xml:
 *   <application
 *       android:name=".DailyBazarApp"
 *       ...>
 *
 * Initialises Cloudinary once at process start so every Activity/Fragment
 * can call CloudinaryHelper.uploadImage() without worrying about init order.
 */
public class DailyBazarApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Cloudinary — unsigned uploads only (no secret in client code)
        // Initialize Cloudinary once for the whole app
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dwwz8f5jd");
        MediaManager.init(this, config);
    }
}