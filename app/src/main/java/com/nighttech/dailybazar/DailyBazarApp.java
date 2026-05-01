package com.nighttech.dailybazar;

import android.app.Application;

import com.nighttech.dailybazar.util.CloudinaryHelper;

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
        CloudinaryHelper.init(this);
    }
}