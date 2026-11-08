package com.example.sitesync.data.remote;

import android.content.Context;
import android.util.Log;

import com.example.sitesync.data.FirebaseSyncManager;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Cloud Firestore Service with offline disk persistence enabled.
 * Central access point for projects, milestones, QA checklists, drawings, and daily reports.
 */
public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private static volatile FirestoreService instance;

    private final FirebaseFirestore db;

    private FirestoreService(Context context) {
        FirebaseSyncManager sync = FirebaseSyncManager.getInstance(context);
        this.db = sync.getFirestore() != null ? sync.getFirestore() : FirebaseFirestore.getInstance();

        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            db.setFirestoreSettings(settings);
        } catch (Exception e) {
            Log.d(TAG, "Firestore settings initialized.");
        }
    }

    public static FirestoreService getInstance(Context context) {
        if (instance == null) {
            synchronized (FirestoreService.class) {
                if (instance == null) {
                    instance = new FirestoreService(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public CollectionReference getUsersCollection() {
        return db.collection("users");
    }

    public CollectionReference getActivitiesCollection() {
        return db.collection("activities");
    }

    public CollectionReference getInspectionsCollection() {
        return db.collection("inspections");
    }

    public CollectionReference getDocumentsCollection() {
        return db.collection("documents");
    }

    public CollectionReference getIssuesCollection() {
        return db.collection("issues");
    }

    public CollectionReference getDailyReportsCollection() {
        return db.collection("daily_reports");
    }
}
