package com.example.mini_projet;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class pour gérer les uploads Cloudinary
 */
public class CloudinaryHelper {

    private static final String TAG = "CloudinaryHelper";
    private static final String CLOUD_NAME = "dbwruk8kt";
    private static final String UPLOAD_PRESET = "AutoFix";

    private static boolean isInitialized = false;

    /**
     * Initialise Cloudinary MediaManager (à appeler une seule fois)
     */
    public static void initialize(Context context) {
        if (!isInitialized) {
            try {
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", CLOUD_NAME);
                MediaManager.init(context, config);
                isInitialized = true;
                Log.d(TAG, "Cloudinary initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Cloudinary", e);
            }
        }
    }

    /**
     * Upload une image vers Cloudinary
     * 
     * @param context  Context de l'application
     * @param imageUri URI de l'image à uploader
     * @param callback Callback pour gérer le résultat
     */
    public static void uploadImage(Context context, Uri imageUri, CloudinaryUploadCallback callback) {
        // S'assurer que Cloudinary est initialisé
        initialize(context);

        // Options d'upload
        Map<String, Object> options = new HashMap<>();
        options.put("upload_preset", UPLOAD_PRESET);
        options.put("folder", "profile_images"); // Optionnel: organiser les images dans un dossier

        Log.d(TAG, "Starting upload to Cloudinary...");

        // Lancer l'upload
        MediaManager.get()
                .upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", "profile_images")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                        if (callback != null) {
                            callback.onUploadStart();
                        }
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        Log.d(TAG, "Upload progress: " + progress + "%");
                        if (callback != null) {
                            callback.onUploadProgress(progress);
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        Log.d(TAG, "Upload successful! URL: " + imageUrl);
                        if (callback != null) {
                            callback.onUploadSuccess(imageUrl);
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload error: " + error.getDescription());
                        if (callback != null) {
                            callback.onUploadError(error.getDescription());
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }

    /**
     * Interface de callback pour l'upload
     */
    public interface CloudinaryUploadCallback {
        void onUploadStart();

        void onUploadProgress(int progress);

        void onUploadSuccess(String imageUrl);

        void onUploadError(String errorMessage);
    }
}
