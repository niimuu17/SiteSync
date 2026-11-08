package com.example.sitesync.data;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Firebase Manager configuring offline disk persistence,
 * real-time telemetry sockets, presence synchronization, and Firestore collections.
 */
public class FirebaseSyncManager {

    private static final String TAG = "FirebaseSyncManager";
    private static volatile FirebaseSyncManager instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseDatabase realtimeDatabase;

    private FirebaseSyncManager(Context context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            try {
                FirebaseApp.initializeApp(context);
            } catch (Exception e) {
                com.google.firebase.FirebaseOptions options = new com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:271128474711:android:0f847b005f552f397d9f12")
                        .setApiKey("AIzaSyAGgi9yHUzO4CFP-p63O4QMmYe7LcwJ7nc")
                        .setProjectId("sitesync-5af3b")
                        .setStorageBucket("sitesync-5af3b.firebasestorage.app")
                        .setDatabaseUrl("https://sitesync-5af3b-default-rtdb.firebaseio.com")
                        .build();
                FirebaseApp.initializeApp(context, options);
            }
        }

        FirebaseAuth authInstance;
        try {
            authInstance = FirebaseAuth.getInstance();
        } catch (Exception e) {
            authInstance = null;
        }
        this.auth = authInstance;

        // 1. Enable Firestore Offline Persistence
        this.firestore = FirebaseFirestore.getInstance();
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            firestore.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore Offline Persistence initialized successfully.");
        } catch (Exception e) {
            Log.w(TAG, "Firestore settings could not be reapplied: " + e.getMessage());
        }

        // 2. Enable Realtime Database Offline Disk Persistence
        FirebaseDatabase rtdbInstance = null;
        try {
            rtdbInstance = FirebaseDatabase.getInstance();
            rtdbInstance.setPersistenceEnabled(true);
            Log.d(TAG, "Realtime Database Offline Persistence enabled.");
        } catch (Exception e) {
            Log.w(TAG, "RTDB persistence already configured or cannot be set: " + e.getMessage());
            rtdbInstance = FirebaseDatabase.getInstance();
        }
        this.realtimeDatabase = rtdbInstance;
    }

    public static FirebaseSyncManager getInstance(Context context) {
        if (instance == null) {
            synchronized (FirebaseSyncManager.class) {
                if (instance == null) {
                    instance = new FirebaseSyncManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public FirebaseDatabase getRealtimeDatabase() {
        return realtimeDatabase;
    }

    /**
     * Publishes real-time engineer/staff site presence with automatic onDisconnect cleanup.
     */
    public void trackPresence(String projectId, String userId, String userName, String role) {
        if (userId == null || projectId == null || realtimeDatabase == null) return;

        DatabaseReference userPresenceRef = realtimeDatabase.getReference("presence")
                .child(projectId)
                .child(userId);

        Map<String, Object> presenceData = new HashMap<>();
        presenceData.put("online", true);
        presenceData.put("userName", userName != null ? userName : "Field Staff");
        presenceData.put("role", role != null ? role : "ENGINEER");
        presenceData.put("lastActive", System.currentTimeMillis());

        userPresenceRef.setValue(presenceData);
        userPresenceRef.child("online").onDisconnect().setValue(false);
        userPresenceRef.child("lastActive").onDisconnect().setValue(System.currentTimeMillis());
    }

    /**
     * Returns true if user has an active authenticated Firebase session.
     */
    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }
}
