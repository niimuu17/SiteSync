package com.example.sitesync.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Model representing an audit attempt (initial inspection or re-inspection) in the inspection history.
 */
public class InspectionRecord implements Serializable {
    private final String recordId;
    private final int attemptNumber;
    private final String date;
    private final String inspectorName;
    private final String status;
    private final String observations;
    private final String remarks;
    private final String photoUri;
    private final String checklistSummary;

    public InspectionRecord(String recordId, int attemptNumber, String date,
                            String inspectorName, String status, String observations,
                            String remarks, String photoUri, String checklistSummary) {
        this.recordId = recordId;
        this.attemptNumber = attemptNumber;
        this.date = date;
        this.inspectorName = inspectorName != null ? inspectorName : "Field Engineer";
        this.status = status != null ? status : "Pending";
        this.observations = observations != null ? observations : "";
        this.remarks = remarks != null ? remarks : "";
        this.photoUri = photoUri != null ? photoUri : "";
        this.checklistSummary = checklistSummary != null ? checklistSummary : "";
    }

    public String getRecordId() {
        return recordId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getDate() {
        return date;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public String getStatus() {
        return status;
    }

    public String getObservations() {
        return observations;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public String getChecklistSummary() {
        return checklistSummary;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("recordId", recordId);
        json.put("attemptNumber", attemptNumber);
        json.put("date", date);
        json.put("inspectorName", inspectorName);
        json.put("status", status);
        json.put("observations", observations);
        json.put("remarks", remarks);
        json.put("photoUri", photoUri);
        json.put("checklistSummary", checklistSummary);
        return json;
    }

    public static InspectionRecord fromJSONObject(JSONObject json) throws JSONException {
        return new InspectionRecord(
                json.optString("recordId", "REC-" + System.currentTimeMillis()),
                json.optInt("attemptNumber", 1),
                json.optString("date", "Recent"),
                json.optString("inspectorName", "Field Engineer"),
                json.optString("status", "Pending"),
                json.optString("observations", ""),
                json.optString("remarks", ""),
                json.optString("photoUri", ""),
                json.optString("checklistSummary", "")
        );
    }
}
