package com.example.sitesync.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a Site Quality Control Inspection, containing location,
 * verified checklist items, observations, photo evidence, and re-inspection history.
 */
public class SiteInspection implements Serializable {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_PASSED = "Passed";
    public static final String STATUS_FAILED = "Failed";

    private final String id;
    private String projectName;
    private String location;
    private String inspectionType;
    private String date;
    private String status;
    private String observations;
    private String remarks;
    private String inspectorName;
    private String photoUri;
    private final List<InspectionChecklistItem> checklist;
    private final List<InspectionRecord> history;

    public SiteInspection(String id, String projectName, String location, String inspectionType,
                          String date, String status, String observations, String remarks,
                          String inspectorName, String photoUri,
                          List<InspectionChecklistItem> checklist,
                          List<InspectionRecord> history) {
        this.id = id;
        this.projectName = projectName;
        this.location = location;
        this.inspectionType = inspectionType;
        this.date = date;
        this.status = status != null ? status : STATUS_PENDING;
        this.observations = observations != null ? observations : "";
        this.remarks = remarks != null ? remarks : "";
        this.inspectorName = inspectorName != null ? inspectorName : "Field Engineer";
        this.photoUri = photoUri != null ? photoUri : "";
        this.checklist = checklist != null ? checklist : new ArrayList<>();
        this.history = history != null ? history : new ArrayList<>();
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInspectionType() {
        return inspectionType;
    }

    public void setInspectionType(String inspectionType) {
        this.inspectionType = inspectionType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public void setInspectorName(String inspectorName) {
        this.inspectorName = inspectorName;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public List<InspectionChecklistItem> getChecklist() {
        return checklist;
    }

    public List<InspectionRecord> getHistory() {
        return history;
    }

    public void addHistoryRecord(InspectionRecord record) {
        if (record != null) {
            // Newest records appear at the top
            history.add(0, record);
        }
    }

    public int getPassedChecklistCount() {
        int count = 0;
        for (InspectionChecklistItem item : checklist) {
            if (item.isPassed()) count++;
        }
        return count;
    }

    public int getTotalChecklistCount() {
        return checklist.size();
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("projectName", projectName);
        json.put("location", location);
        json.put("inspectionType", inspectionType);
        json.put("date", date);
        json.put("status", status);
        json.put("observations", observations);
        json.put("remarks", remarks);
        json.put("inspectorName", inspectorName);
        json.put("photoUri", photoUri);

        JSONArray checkArray = new JSONArray();
        for (InspectionChecklistItem item : checklist) {
            checkArray.put(item.toJSONObject());
        }
        json.put("checklist", checkArray);

        JSONArray histArray = new JSONArray();
        for (InspectionRecord record : history) {
            histArray.put(record.toJSONObject());
        }
        json.put("history", histArray);

        return json;
    }

    public static SiteInspection fromJSONObject(JSONObject json) throws JSONException {
        List<InspectionChecklistItem> checklistItems = new ArrayList<>();
        JSONArray checkArray = json.optJSONArray("checklist");
        if (checkArray != null) {
            for (int i = 0; i < checkArray.length(); i++) {
                checklistItems.add(InspectionChecklistItem.fromJSONObject(checkArray.getJSONObject(i)));
            }
        }

        List<InspectionRecord> historyRecords = new ArrayList<>();
        JSONArray histArray = json.optJSONArray("history");
        if (histArray != null) {
            for (int i = 0; i < histArray.length(); i++) {
                historyRecords.add(InspectionRecord.fromJSONObject(histArray.getJSONObject(i)));
            }
        }

        return new SiteInspection(
                json.optString("id", "INSP-" + System.currentTimeMillis()),
                json.optString("projectName", "Metro Tower Phase 2"),
                json.optString("location", "Main Site"),
                json.optString("inspectionType", "Structural Quality Check"),
                json.optString("date", "Recent"),
                json.optString("status", STATUS_PENDING),
                json.optString("observations", ""),
                json.optString("remarks", ""),
                json.optString("inspectorName", "Field Engineer"),
                json.optString("photoUri", ""),
                checklistItems,
                historyRecords
        );
    }
}
