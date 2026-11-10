package com.example.sitesync;

import android.app.Application;
import android.util.Log;

import com.example.sitesync.data.FirebaseSyncManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Enterprise Application Entry Class.
 * Initializes Firebase with offline persistence and configuration fallbacks.
 */
public class SiteSyncApplication extends Application {

    private static final String TAG = "SiteSyncApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        initializeFirebaseSafely();
    }

    private void initializeFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this);
                    Log.d(TAG, "Firebase initialized via default provider.");
                } catch (Exception e) {
                    Log.w(TAG, "Default Firebase initialization missed options, using programmatic configuration.");
                    FirebaseOptions options = new FirebaseOptions.Builder()
                            .setApplicationId("1:271128474711:android:0f847b005f552f397d9f12")
                            .setApiKey("AIzaSyAGgi9yHUzO4CFP-p63O4QMmYe7LcwJ7nc")
                            .setProjectId("sitesync-5af3b")
                            .setStorageBucket("sitesync-5af3b.firebasestorage.app")
                            .setDatabaseUrl("https://sitesync-5af3b-default-rtdb.firebaseio.com")
                            .build();
                    FirebaseApp.initializeApp(this, options);
                }
            }

            // Pre-warm FirebaseSyncManager for offline persistence and RTDB
            FirebaseSyncManager.getInstance(this);
            Log.d(TAG, "SiteSync Firebase ecosystem initialized with offline persistence.");

            // Subscribe to Critical Hazard FCM Push Channels
            com.example.sitesync.notifications.SiteSyncFirebaseMessagingService.subscribeToSiteSafetyChannels();

            // Schedule WorkManager periodic offline background sync
            com.example.sitesync.data.sync.SyncManager.schedulePeriodicSync(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase ecosystem: " + e.getMessage(), e);
        }
    }
}
