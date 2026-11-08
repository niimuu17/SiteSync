package com.example.sitesync.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.ProgressLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Repository providing local persistence for construction site activities
 * and progress history using SharedPreferences JSON storage.
 */
public class ProgressRepository {

    private static final String PREF_NAME = "SiteSync_Progress_Prefs";
    private static final String KEY_ACTIVITIES = "key_construction_activities";

    private final SharedPreferences preferences;

    public ProgressRepository(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureInitialData();
    }

    /**
     * Retrieves all saved construction activities.
     */
    public List<ConstructionActivity> getAllActivities() {
        List<ConstructionActivity> activities = new ArrayList<>();
        String jsonString = preferences.getString(KEY_ACTIVITIES, null);

        if (jsonString != null && !jsonString.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    activities.add(ConstructionActivity.fromJSONObject(obj));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return activities;
    }

    /**
     * Retrieves a single activity by unique ID.
     */
    public ConstructionActivity getActivityById(String activityId) {
        if (activityId == null) return null;
        for (ConstructionActivity activity : getAllActivities()) {
            if (activityId.equals(activity.getId())) {
                return activity;
            }
        }
        return null;
    }

    /**
     * Updates an activity with a new progress percentage, status, remarks, and optional photo,
     * while automatically recording an audit log entry in the activity's history.
     */
    public boolean updateProgress(String activityId, int newPercentage, String newStatus,
                                  String remarks, String photoUri, String engineerName) {
        List<ConstructionActivity> list = getAllActivities();
        boolean updated = false;

        String formattedTimestamp = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());

        for (int i = 0; i < list.size(); i++) {
            ConstructionActivity act = list.get(i);
            if (act.getId().equals(activityId)) {
                act.setProgressPercentage(newPercentage);
                act.setStatus(newStatus);
                act.setLastUpdated(formattedTimestamp);
                act.setUpdatedBy(engineerName);
                if (remarks != null && !remarks.trim().isEmpty()) {
                    act.setLatestRemarks(remarks.trim());
                }
                if (photoUri != null && !photoUri.trim().isEmpty()) {
                    act.setLatestPhotoUri(photoUri.trim());
                }

                // Add to audit history
                ProgressLog log = new ProgressLog(
                        "LOG-" + System.currentTimeMillis(),
                        newPercentage,
                        newStatus,
                        remarks,
                        formattedTimestamp,
                        engineerName,
                        photoUri
                );
                act.addProgressLog(log);

                list.set(i, act);
                updated = true;
                break;
            }
        }

        if (updated) {
            saveActivities(list);
        }
        return updated;
    }

    /**
     * Adds a brand new construction activity.
     */
    public void addActivity(ConstructionActivity newActivity) {
        List<ConstructionActivity> list = getAllActivities();
        list.add(0, newActivity);
        saveActivities(list);
    }

    /**
     * Persists the activities list to SharedPreferences as a JSON array.
     */
    private void saveActivities(List<ConstructionActivity> list) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (ConstructionActivity act : list) {
                jsonArray.put(act.toJSONObject());
            }
            preferences.edit().putString(KEY_ACTIVITIES, jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Pre-populates realistic construction demo data if repository is empty.
     */
    private void ensureInitialData() {
        if (!preferences.contains(KEY_ACTIVITIES)) {
            List<ConstructionActivity> initialList = new ArrayList<>();

            // 1. Foundation Raft Concrete Pouring
            List<ProgressLog> history1 = new ArrayList<>();
            history1.add(new ProgressLog("L-101", 85, ConstructionActivity.STATUS_IN_PROGRESS,
                    "3rd pour completed. Surface vibrated and curing blankets positioned.",
                    "Nov 06, 2026 - 11:30 AM", "Elena Rostova (PE)", ""));
            history1.add(new ProgressLog("L-102", 55, ConstructionActivity.STATUS_IN_PROGRESS,
                    "Rebar grid inspection signed off by structural surveyor.",
                    "Nov 04, 2026 - 09:15 AM", "Elena Rostova (PE)", ""));
            history1.add(new ProgressLog("L-103", 20, ConstructionActivity.STATUS_PENDING,
                    "Excavation and sub-base blinding completed.",
                    "Nov 01, 2026 - 08:00 AM", "Elena Rostova (PE)", ""));

            initialList.add(new ConstructionActivity(
                    "ACT-101",
                    "Metro Tower Phase 2",
                    "Foundation Raft Concrete Pouring",
                    "Basement Level - Sector A",
                    85,
                    ConstructionActivity.STATUS_IN_PROGRESS,
                    "Nov 06, 2026 - 11:30 AM",
                    "Elena Rostova (PE)",
                    "3rd pour completed. Surface vibrated and curing blankets positioned.",
                    "",
                    history1
            ));

            // 2. Structural Steel Column Erection
            List<ProgressLog> history2 = new ArrayList<>();
            history2.add(new ProgressLog("L-201", 45, ConstructionActivity.STATUS_IN_PROGRESS,
                    "8 of 16 columns bolted and plumbed with theodolite.",
                    "Nov 06, 2026 - 02:45 PM", "Elena Rostova (PE)", ""));
            history2.add(new ProgressLog("L-202", 15, ConstructionActivity.STATUS_PENDING,
                    "Cranes rigged and base plate anchor grouting completed.",
                    "Nov 03, 2026 - 10:00 AM", "Elena Rostova (PE)", ""));

            initialList.add(new ConstructionActivity(
                    "ACT-102",
                    "Metro Tower Phase 2",
                    "Structural Steel Column Erection",
                    "Level 4 - North Quadrant",
                    45,
                    ConstructionActivity.STATUS_IN_PROGRESS,
                    "Nov 06, 2026 - 02:45 PM",
                    "Elena Rostova (PE)",
                    "8 of 16 columns bolted and plumbed with theodolite.",
                    "",
                    history2
            ));

            // 3. Earth Retention & Shoring Wall
            List<ProgressLog> history3 = new ArrayList<>();
            history3.add(new ProgressLog("L-301", 100, ConstructionActivity.STATUS_COMPLETED,
                    "Tieback anchor tensioning and shotcrete tests fully certified.",
                    "Nov 05, 2026 - 04:00 PM", "Elena Rostova (PE)", ""));

            initialList.add(new ConstructionActivity(
                    "ACT-103",
                    "Metro Tower Phase 2",
                    "Earth Retention & Shoring Wall",
                    "East Perimeter Boundary",
                    100,
                    ConstructionActivity.STATUS_COMPLETED,
                    "Nov 05, 2026 - 04:00 PM",
                    "Elena Rostova (PE)",
                    "Tieback anchor tensioning and shotcrete tests fully certified.",
                    "",
                    history3
            ));

            // 4. HVAC & Electrical Conduit Rough-In
            List<ProgressLog> history4 = new ArrayList<>();
            history4.add(new ProgressLog("L-401", 10, ConstructionActivity.STATUS_PENDING,
                    "Awaiting CAD revision 4.8.2 before sleeve drill-outs.",
                    "Nov 06, 2026 - 09:00 AM", "Elena Rostova (PE)", ""));

            initialList.add(new ConstructionActivity(
                    "ACT-104",
                    "Metro Tower Phase 2",
                    "HVAC & Electrical Risers Rough-In",
                    "Core Shaft - Levels 1 to 3",
                    10,
                    ConstructionActivity.STATUS_PENDING,
                    "Nov 06, 2026 - 09:00 AM",
                    "Elena Rostova (PE)",
                    "Awaiting CAD revision 4.8.2 before sleeve drill-outs.",
                    "",
                    history4
            ));

            saveActivities(initialList);
        }
    }
}
