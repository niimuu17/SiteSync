package com.example.sitesync.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a site construction activity with progress tracking,
 * site photos, status lifecycle, and update audit history.
 */
public class ConstructionActivity implements Serializable {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED = "Completed";

    private final String id;
    private String projectName;
    private String activityName;
    private String location;
    private int progressPercentage;
    private String status;
    private String lastUpdated;
    private String updatedBy;
    private String latestRemarks;
    private String latestPhotoUri;
    private final List<ProgressLog> progressHistory;

    public ConstructionActivity(String id, String projectName, String activityName,
                                String location, int progressPercentage, String status,
                                String lastUpdated, String updatedBy, String latestRemarks,
                                String latestPhotoUri, List<ProgressLog> progressHistory) {
        this.id = id;
        this.projectName = projectName;
        this.activityName = activityName;
        this.location = location;
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
        this.status = status != null ? status : STATUS_IN_PROGRESS;
        this.lastUpdated = lastUpdated;
        this.updatedBy = updatedBy;
        this.latestRemarks = latestRemarks != null ? latestRemarks : "";
        this.latestPhotoUri = latestPhotoUri != null ? latestPhotoUri : "";
        this.progressHistory = progressHistory != null ? progressHistory : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getLatestRemarks() {
        return latestRemarks;
    }

    public void setLatestRemarks(String latestRemarks) {
        this.latestRemarks = latestRemarks;
    }

    public String getLatestPhotoUri() {
        return latestPhotoUri;
    }

    public void setLatestPhotoUri(String latestPhotoUri) {
        this.latestPhotoUri = latestPhotoUri;
    }

    public List<ProgressLog> getProgressHistory() {
        return progressHistory;
    }

    public void addProgressLog(ProgressLog log) {
        if (log != null) {
            // Newest logs appear at top of timeline
            this.progressHistory.add(0, log);
        }
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("projectName", projectName);
        json.put("activityName", activityName);
        json.put("location", location);
        json.put("progressPercentage", progressPercentage);
        json.put("status", status);
        json.put("lastUpdated", lastUpdated);
        json.put("updatedBy", updatedBy);
        json.put("latestRemarks", latestRemarks);
        json.put("latestPhotoUri", latestPhotoUri);

        JSONArray historyArray = new JSONArray();
        for (ProgressLog log : progressHistory) {
            historyArray.put(log.toJSONObject());
        }
        json.put("progressHistory", historyArray);

        return json;
    }

    public static ConstructionActivity fromJSONObject(JSONObject json) throws JSONException {
        List<ProgressLog> history = new ArrayList<>();
        JSONArray historyArray = json.optJSONArray("progressHistory");
        if (historyArray != null) {
            for (int i = 0; i < historyArray.length(); i++) {
                history.add(ProgressLog.fromJSONObject(historyArray.getJSONObject(i)));
            }
        }

        return new ConstructionActivity(
                json.optString("id", "ACT-" + System.currentTimeMillis()),
                json.optString("projectName", "Metro Tower Phase 2"),
                json.optString("activityName", "Site Construction Activity"),
                json.optString("location", "Main Site"),
                json.optInt("progressPercentage", 0),
                json.optString("status", STATUS_PENDING),
                json.optString("lastUpdated", "Just now"),
                json.optString("updatedBy", "Field Engineer"),
                json.optString("latestRemarks", ""),
                json.optString("latestPhotoUri", ""),
                history
        );
    }
}
