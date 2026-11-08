package com.example.sitesync.data.remote;

import android.content.Context;
import android.util.Log;

import com.example.sitesync.data.FirebaseSyncManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Realtime Database Service for low-latency live telemetry,
 * real-time staff presence, and RFI / clarification live chat streams.
 */
public class RealtimeDatabaseService {

    private static final String TAG = "RealtimeDbService";
    private static volatile RealtimeDatabaseService instance;

    private final FirebaseDatabase rtdb;

    private RealtimeDatabaseService(Context context) {
        FirebaseSyncManager sync = FirebaseSyncManager.getInstance(context);
        this.rtdb = sync.getRealtimeDatabase() != null ? sync.getRealtimeDatabase() : FirebaseDatabase.getInstance();
    }

    public static RealtimeDatabaseService getInstance(Context context) {
        if (instance == null) {
            synchronized (RealtimeDatabaseService.class) {
                if (instance == null) {
                    instance = new RealtimeDatabaseService(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public FirebaseDatabase getRtdb() {
        return rtdb;
    }

    public DatabaseReference getPresenceRef(String projectId, String userId) {
        return rtdb.getReference("presence").child(projectId).child(userId);
    }

    public DatabaseReference getIssueCommentsRef(String issueId) {
        return rtdb.getReference("rfis").child(issueId).child("comments");
    }

    public DatabaseReference getTelemetryRef(String projectId) {
        return rtdb.getReference("telemetry").child(projectId);
    }

    public void updateTelemetry(String projectId, String key, Object value) {
        if (projectId == null || key == null) return;
        rtdb.getReference("telemetry").child(projectId).child(key).setValue(value);
    }
}
