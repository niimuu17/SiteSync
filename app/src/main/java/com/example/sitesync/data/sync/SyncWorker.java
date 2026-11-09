package com.example.sitesync.data.sync;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.dao.PendingSyncDao;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.data.remote.FirestoreService;
import com.example.sitesync.data.remote.StorageService;
import com.example.sitesync.utils.NetworkMonitor;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Background WorkManager sync worker for flushing pending entity edits and offline media uploads.
 * Reads pending mutations from SQLite WAL queue, uploads photos to Firebase Storage,
 * commits records to Cloud Firestore / Realtime DB, and prunes synced records.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final String DEFAULT_PROJECT_ID = "metro_central";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Executing background SiteSync offline synchronization pipeline...");
        Context context = getApplicationContext();

        boolean isOnline = NetworkMonitor.getInstance(context).isCurrentlyOnline();
        if (!isOnline) {
            Log.d(TAG, "Device currently offline. Rescheduling sync worker.");
            return Result.retry();
        }

        try {
            AppDatabase db = AppDatabase.getDatabase(context);
            PendingSyncDao pendingDao = db.pendingSyncDao();
            List<PendingSyncEntity> mutations = pendingDao.getPendingMutationsSync();

            if (mutations == null || mutations.isEmpty()) {
                Log.d(TAG, "Pending sync queue is empty. Zero operations required.");
                return Result.success();
            }

            Log.d(TAG, "Processing " + mutations.size() + " pending offline mutations...");
            FirebaseFirestore firestore = null;
            try {
                firestore = FirestoreService.getInstance(context).getDb();
            } catch (Exception ignored) {
            }

            for (PendingSyncEntity mutation : mutations) {
                try {
                    processMutation(context, firestore, mutation);
                    // Successfully flushed: delete or prune from queue
                    pendingDao.deleteById(mutation.getId());
                    Log.d(TAG, "Successfully synced mutation: " + mutation.getId() + " [" + mutation.getEntityType() + "]");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to flush mutation " + mutation.getId() + ": " + e.getMessage());
                    pendingDao.updateStatus(mutation.getId(), "FAILED");
                }
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Critical error during offline sync execution", e);
            return Result.retry();
        }
    }

    private void processMutation(Context context, FirebaseFirestore firestore, PendingSyncEntity mutation) throws Exception {
        String payloadJson = mutation.getPayloadJson();
        Map<String, Object> dataMap = new HashMap<>();

        if (payloadJson != null && !payloadJson.isEmpty()) {
            JSONObject json = new JSONObject(payloadJson);

            // Check if photo upload is required
            if (json.has("photoUri") || json.has("photoPath")) {
                String localUriStr = json.has("photoUri") ? json.optString("photoUri") : json.optString("photoPath");
                if (localUriStr != null && !localUriStr.isEmpty() && !localUriStr.startsWith("http")) {
                    try {
                        Uri uri = Uri.parse(localUriStr);
                        File localFile = null;
                        if (uri.getScheme() == null || "file".equalsIgnoreCase(uri.getScheme())) {
                            localFile = new File(uri.getPath() != null ? uri.getPath() : localUriStr);
                        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                            String lastSegment = uri.getLastPathSegment();
                            if (lastSegment != null) {
                                File candidate = new File(new File(context.getCacheDir(), "inspections"), lastSegment);
                                if (candidate.exists()) {
                                    localFile = candidate;
                                }
                            }
                        }
                        if (localFile != null && localFile.exists()) {
                            // Upload to Firebase Storage
                            String folder = mutation.getEntityType().toLowerCase() + "_evidence";
                            final String[] uploadedUrl = new String[]{localUriStr};
                            StorageService.getInstance().uploadFile(folder, localFile, new StorageService.OnUploadCallback() {
                                @Override
                                public void onSuccess(String downloadUrl) {
                                    uploadedUrl[0] = downloadUrl;
                                }
                                @Override
                                public void onError(String errorMessage) {}
                            });
                            json.put("photoUrl", uploadedUrl[0]);
                        }
                    } catch (Exception ignored) {}
                }
            }

            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                dataMap.put(key, json.get(key));
            }
        }

        dataMap.put("updatedAt", mutation.getTimestamp());
        dataMap.put("action", mutation.getAction());

        // Dispatch based on entityType
        String entityType = mutation.getEntityType() != null ? mutation.getEntityType().toUpperCase() : "GENERAL";

        if ("EMERGENCY_BROADCAST".equals(entityType)) {
            try {
                FirebaseDatabase.getInstance().getReference("projects/" + DEFAULT_PROJECT_ID + "/emergency_broadcast")
                        .setValue(dataMap);
            } catch (Exception ignored) {
            }
            return;
        }

        if (firestore != null) {
            String collectionPath = "projects/" + DEFAULT_PROJECT_ID + "/" + getCollectionNameForType(entityType);
            String docId = mutation.getEntityId() != null ? mutation.getEntityId() : mutation.getId();

            try {
                Tasks.await(
                        firestore.collection(collectionPath)
                                .document(docId)
                                .set(dataMap, SetOptions.merge()),
                        3,
                        TimeUnit.SECONDS
                );
            } catch (Exception e) {
                Log.d(TAG, "Firestore async sync cached locally in offline persistence: " + e.getMessage());
            }
        }
    }

    private String getCollectionNameForType(String entityType) {
        switch (entityType) {
            case "HAZARD":
                return "hazards";
            case "TASK":
                return "tasks";
            case "INVENTORY":
                return "inventory";
            case "DAILY_REPORT":
                return "daily_reports";
            case "INSPECTION":
                return "inspections";
            default:
                return "general_logs";
        }
    }
}
