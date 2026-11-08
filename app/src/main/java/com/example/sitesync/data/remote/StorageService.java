package com.example.sitesync.data.remote;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

/**
 * Firebase Storage Service for forensic milestone photos, CAD vector blueprints,
 * and hands-free voice note backup files.
 */
public class StorageService {

    private static final String TAG = "StorageService";
    private static volatile StorageService instance;

    private final FirebaseStorage storage;

    public interface OnUploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String errorMessage);
    }

    private StorageService() {
        FirebaseStorage inst;
        try {
            inst = FirebaseStorage.getInstance();
        } catch (Exception e) {
            inst = null;
        }
        this.storage = inst;
    }

    public static StorageService getInstance() {
        if (instance == null) {
            synchronized (StorageService.class) {
                if (instance == null) {
                    instance = new StorageService();
                }
            }
        }
        return instance;
    }

    public void uploadFile(String folder, File localFile, OnUploadCallback callback) {
        if (localFile == null || !localFile.exists()) {
            callback.onError("Local file does not exist.");
            return;
        }

        if (storage == null) {
            // Local fallback path
            callback.onSuccess(Uri.fromFile(localFile).toString());
            return;
        }

        try {
            StorageReference ref = storage.getReference().child(folder).child(localFile.getName());
            UploadTask uploadTask = ref.putFile(Uri.fromFile(localFile));

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    callback.onSuccess(uri.toString());
                }).addOnFailureListener(e -> callback.onSuccess(Uri.fromFile(localFile).toString()));
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Storage upload failed, keeping local file: " + e.getMessage());
                callback.onSuccess(Uri.fromFile(localFile).toString());
            });
        } catch (Exception e) {
            callback.onSuccess(Uri.fromFile(localFile).toString());
        }
    }
}
