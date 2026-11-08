package com.example.sitesync.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Model representing an entry in an activity's progress audit history.
 */
public class ProgressLog implements Serializable {
    private final String logId;
    private final int percentage;
    private final String status;
    private final String remarks;
    private final String timestamp;
    private final String updatedBy;
    private final String photoUri;

    public ProgressLog(String logId, int percentage, String status, String remarks,
                       String timestamp, String updatedBy, String photoUri) {
        this.logId = logId;
        this.percentage = percentage;
        this.status = status;
        this.remarks = remarks != null ? remarks : "";
        this.timestamp = timestamp;
        this.updatedBy = updatedBy != null ? updatedBy : "Field Engineer";
        this.photoUri = photoUri != null ? photoUri : "";
    }

    public String getLogId() {
        return logId;
    }

    public int getPercentage() {
        return percentage;
    }

    public String getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("logId", logId);
        json.put("percentage", percentage);
        json.put("status", status);
        json.put("remarks", remarks);
        json.put("timestamp", timestamp);
        json.put("updatedBy", updatedBy);
        json.put("photoUri", photoUri);
        return json;
    }

    public static ProgressLog fromJSONObject(JSONObject json) throws JSONException {
        return new ProgressLog(
                json.optString("logId", String.valueOf(System.currentTimeMillis())),
                json.optInt("percentage", 0),
                json.optString("status", "In Progress"),
                json.optString("remarks", ""),
                json.optString("timestamp", "Recent"),
                json.optString("updatedBy", "Field Engineer"),
                json.optString("photoUri", "")
        );
    }
}
