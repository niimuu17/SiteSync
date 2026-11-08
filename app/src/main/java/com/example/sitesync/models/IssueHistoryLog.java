package com.example.sitesync.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Model representing an audit event in the lifecycle of an RFI / technical issue
 * (e.g., status changes, re-assignments, resolutions).
 */
public class IssueHistoryLog implements Serializable {
    private final String logId;
    private final String action;
    private final String timestamp;
    private final String performedBy;

    public IssueHistoryLog(String logId, String action, String timestamp, String performedBy) {
        this.logId = logId;
        this.action = action != null ? action : "Updated Issue";
        this.timestamp = timestamp;
        this.performedBy = performedBy != null ? performedBy : "Field Engineer";
    }

    public String getLogId() {
        return logId;
    }

    public String getAction() {
        return action;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("logId", logId);
        json.put("action", action);
        json.put("timestamp", timestamp);
        json.put("performedBy", performedBy);
        return json;
    }

    public static IssueHistoryLog fromJSONObject(JSONObject json) throws JSONException {
        return new IssueHistoryLog(
                json.optString("logId", "HIST-" + System.currentTimeMillis()),
                json.optString("action", "Updated Issue"),
                json.optString("timestamp", "Recent"),
                json.optString("performedBy", "Field Engineer")
        );
    }
}
