package com.example.sitesync.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Model representing an individual quality control checklist verification item.
 */
public class InspectionChecklistItem implements Serializable {
    private final String id;
    private final String title;
    private boolean passed;
    private String notes;

    public InspectionChecklistItem(String id, String title, boolean passed, String notes) {
        this.id = id;
        this.title = title;
        this.passed = passed;
        this.notes = notes != null ? notes : "";
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("passed", passed);
        json.put("notes", notes);
        return json;
    }

    public static InspectionChecklistItem fromJSONObject(JSONObject json) throws JSONException {
        return new InspectionChecklistItem(
                json.optString("id", "CHK-" + System.currentTimeMillis()),
                json.optString("title", "Checklist Item"),
                json.optBoolean("passed", false),
                json.optString("notes", "")
        );
    }
}
