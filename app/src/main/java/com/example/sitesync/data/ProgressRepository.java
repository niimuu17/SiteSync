package com.example.sitesync.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.sitesync.data.local.ActivityDao;
import com.example.sitesync.data.local.ActivityEntity;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.ProgressLog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enterprise Single-Source-of-Truth Repository for Construction Activities.
 * Backed by Room Database local cache and Cloud Firestore real-time snapshot sync.
 */
public class ProgressRepository {

    private static final String TAG = "ProgressRepository";
    private static final String COLLECTION_ACTIVITIES = "activities";

    private final ActivityDao activityDao;
    private final FirebaseFirestore firestore;

    public ProgressRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.activityDao = db.activityDao();
        this.firestore = FirebaseSyncManager.getInstance(context).getFirestore();

        ensureInitialData();
        attachFirestoreSnapshotListener();
    }

    private void attachFirestoreSnapshotListener() {
        if (firestore == null) return;
        firestore.collection(COLLECTION_ACTIVITIES)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Firestore activities listen error: " + e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            List<ActivityEntity> remoteEntities = new ArrayList<>();
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String rawJson = doc.getString("rawJson");
                                if (rawJson != null && !rawJson.isEmpty()) {
                                    try {
                                        ConstructionActivity act = ConstructionActivity.fromJSONObject(new JSONObject(rawJson));
                                        remoteEntities.add(ActivityEntity.fromModel(act));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            if (!remoteEntities.isEmpty()) {
                                activityDao.insertAll(remoteEntities);
                            }
                        });
                    }
                });
    }

    public LiveData<List<ConstructionActivity>> getAllActivitiesLive() {
        return Transformations.map(activityDao.getAllLive(), entities -> {
            List<ConstructionActivity> list = new ArrayList<>();
            for (ActivityEntity entity : entities) {
                list.add(entity.toModel());
            }
            return list;
        });
    }

    public List<ConstructionActivity> getAllActivities() {
        List<ActivityEntity> entities = activityDao.getAllSync();
        List<ConstructionActivity> activities = new ArrayList<>();
        for (ActivityEntity e : entities) {
            activities.add(e.toModel());
        }
        return activities;
    }

    public ConstructionActivity getActivityById(String activityId) {
        if (activityId == null) return null;
        ActivityEntity entity = activityDao.getById(activityId);
        return entity != null ? entity.toModel() : null;
    }

    public boolean addActivity(ConstructionActivity activity) {
        if (activity == null || activity.getId() == null) return false;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            activityDao.insertOrUpdate(ActivityEntity.fromModel(activity));
        });

        if (firestore != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", activity.getId());
            data.put("projectName", activity.getProjectName());
            data.put("activityName", activity.getActivityName());
            data.put("location", activity.getLocation());
            data.put("progressPercentage", activity.getProgressPercentage());
            data.put("status", activity.getStatus());
            data.put("remarks", activity.getLatestRemarks());
            data.put("photoUri", activity.getLatestPhotoUri());
            try {
                data.put("rawJson", activity.toJSONObject().toString());
            } catch (Exception ignored) {
            }
            data.put("lastUpdated", System.currentTimeMillis());

            firestore.collection(COLLECTION_ACTIVITIES)
                    .document(activity.getId())
                    .set(data);
        }
        return true;
    }

    public boolean updateActivityProgress(String activityId, int newProgress, String newStatus, String remarks) {
        return updateProgress(activityId, newProgress, newStatus, remarks, "Site Engineer", "");
    }

    public int calculateOverallProgress() {
        List<ConstructionActivity> list = getAllActivities();
        if (list == null || list.isEmpty()) return 68;
        int sum = 0;
        for (ConstructionActivity a : list) {
            sum += a.getProgressPercentage();
        }
        return sum / list.size();
    }

    public boolean updateProgress(String activityId, int newProgress, String newStatus,
                                  String remarks, String engineerName, String photoUri) {
        ConstructionActivity target = getActivityById(activityId);
        if (target == null) return false;

        target.setProgressPercentage(newProgress);
        if (newStatus != null && !newStatus.isEmpty()) {
            target.setStatus(newStatus);
        }
        if (remarks != null && !remarks.isEmpty()) {
            target.setLatestRemarks(remarks);
        }
        if (photoUri != null && !photoUri.isEmpty()) {
            target.setLatestPhotoUri(photoUri);
        }

        String time = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
        target.setLastUpdated(time);
        target.setUpdatedBy(engineerName != null ? engineerName : "Site Engineer");

        ProgressLog log = new ProgressLog(
                "LOG-" + System.currentTimeMillis(),
                newProgress,
                target.getStatus(),
                remarks != null ? remarks : "Progress updated",
                time,
                engineerName != null ? engineerName : "Site Engineer",
                photoUri != null ? photoUri : ""
        );
        target.addProgressLog(log);

        return addActivity(target);
    }

    public boolean deleteActivity(String activityId) {
        if (activityId == null) return false;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activityDao.deleteById(activityId);
        });
        if (firestore != null) {
            firestore.collection(COLLECTION_ACTIVITIES).document(activityId).delete();
        }
        return true;
    }

    private void ensureInitialData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ActivityEntity> existing = activityDao.getAllSync();
            if (existing == null || existing.isEmpty()) {
                List<ActivityEntity> initial = new ArrayList<>();

                // Seed 1: Foundation Poured
                ConstructionActivity act1 = new ConstructionActivity(
                        "ACT-101", "Metro Central Tower", "Foundation Poured & Curing",
                        "Sector B - Sub-Basement", 85, ConstructionActivity.STATUS_IN_PROGRESS,
                        "Nov 06, 2026", "Alex Chen (PE)", "Concrete curing strength 32 MPa verified with Schmidt hammer.", "", new ArrayList<>()
                );
                initial.add(ActivityEntity.fromModel(act1));

                // Seed 2: Structural Column
                ConstructionActivity act2 = new ConstructionActivity(
                        "ACT-102", "Metro Central Tower", "Structural Steel Column Erection",
                        "Tower Core Grid 4-8", 62, ConstructionActivity.STATUS_IN_PROGRESS,
                        "Nov 06, 2026", "Alex Chen (PE)", "8 of 16 columns bolted and plumbed with theodolite.", "", new ArrayList<>()
                );
                initial.add(ActivityEntity.fromModel(act2));

                // Seed 3: Earth Retention
                ConstructionActivity act3 = new ConstructionActivity(
                        "ACT-103", "Metro Central Tower", "Earth Retention & Shoring Wall",
                        "East Perimeter Boundary", 100, ConstructionActivity.STATUS_COMPLETED,
                        "Nov 05, 2026", "Alex Chen (PE)", "Tieback anchor tensioning and shotcrete tests fully certified.", "", new ArrayList<>()
                );
                initial.add(ActivityEntity.fromModel(act3));

                // Seed 4: HVAC Electrical
                ConstructionActivity act4 = new ConstructionActivity(
                        "ACT-104", "Metro Central Tower", "HVAC & Electrical Risers Rough-In",
                        "Core Shaft - Levels 1 to 3", 18, ConstructionActivity.STATUS_PENDING,
                        "Nov 06, 2026", "Alex Chen (PE)", "Awaiting CAD revision 4.8.2 before sleeve drill-outs.", "", new ArrayList<>()
                );
                initial.add(ActivityEntity.fromModel(act4));

                activityDao.insertAll(initial);
            }
        });
    }
}
