package com.example.sitesync.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Data model representing a Daily Site Report (DSR) created by site engineers.
 * Captures construction progress, weather, manpower, materials, equipment, site issues, photos, and next-day schedule.
 */
public class DailySiteReport implements Serializable {

    public static final String STATUS_DRAFT = "Draft";
    public static final String STATUS_SUBMITTED = "Submitted";

    private String id;
    private String projectName;
    private String reportDate;
    private String preparedBy;
    private String status;
    private String weather;
    private String completedWork;
    private String manpower;
    private String materialsUsed;
    private String equipment;
    private String siteProblems;
    private String photoUri;
    private String nextDayPlans;
    private String createdAt;
    private String submittedAt;

    public DailySiteReport() {
        this.status = STATUS_DRAFT;
    }

    public DailySiteReport(String id, String projectName, String reportDate, String preparedBy,
                           String status, String weather, String completedWork, String manpower,
                           String materialsUsed, String equipment, String siteProblems,
                           String photoUri, String nextDayPlans, String createdAt, String submittedAt) {
        this.id = id;
        this.projectName = projectName;
        this.reportDate = reportDate;
        this.preparedBy = preparedBy != null ? preparedBy : "Site Engineer";
        this.status = status != null ? status : STATUS_DRAFT;
        this.weather = weather != null ? weather : "Sunny";
        this.completedWork = completedWork != null ? completedWork : "";
        this.manpower = manpower != null ? manpower : "";
        this.materialsUsed = materialsUsed != null ? materialsUsed : "";
        this.equipment = equipment != null ? equipment : "";
        this.siteProblems = siteProblems != null ? siteProblems : "None reported.";
        this.photoUri = photoUri;
        this.nextDayPlans = nextDayPlans != null ? nextDayPlans : "";
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getPreparedBy() {
        return preparedBy;
    }

    public void setPreparedBy(String preparedBy) {
        this.preparedBy = preparedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getCompletedWork() {
        return completedWork;
    }

    public void setCompletedWork(String completedWork) {
        this.completedWork = completedWork;
    }

    public String getManpower() {
        return manpower;
    }

    public void setManpower(String manpower) {
        this.manpower = manpower;
    }

    public String getMaterialsUsed() {
        return materialsUsed;
    }

    public void setMaterialsUsed(String materialsUsed) {
        this.materialsUsed = materialsUsed;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getSiteProblems() {
        return siteProblems;
    }

    public void setSiteProblems(String siteProblems) {
        this.siteProblems = siteProblems;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public String getNextDayPlans() {
        return nextDayPlans;
    }

    public void setNextDayPlans(String nextDayPlans) {
        this.nextDayPlans = nextDayPlans;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isSubmitted() {
        return STATUS_SUBMITTED.equalsIgnoreCase(status);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("projectName", projectName);
        json.put("reportDate", reportDate);
        json.put("preparedBy", preparedBy);
        json.put("status", status);
        json.put("weather", weather);
        json.put("completedWork", completedWork);
        json.put("manpower", manpower);
        json.put("materialsUsed", materialsUsed);
        json.put("equipment", equipment);
        json.put("siteProblems", siteProblems);
        json.put("photoUri", photoUri != null ? photoUri : "");
        json.put("nextDayPlans", nextDayPlans);
        json.put("createdAt", createdAt != null ? createdAt : "");
        json.put("submittedAt", submittedAt != null ? submittedAt : "");
        return json;
    }

    public static DailySiteReport fromJSONObject(JSONObject json) throws JSONException {
        DailySiteReport report = new DailySiteReport();
        report.setId(json.optString("id", ""));
        report.setProjectName(json.optString("projectName", ""));
        report.setReportDate(json.optString("reportDate", ""));
        report.setPreparedBy(json.optString("preparedBy", "Site Engineer"));
        report.setStatus(json.optString("status", STATUS_DRAFT));
        report.setWeather(json.optString("weather", "Sunny"));
        report.setCompletedWork(json.optString("completedWork", ""));
        report.setManpower(json.optString("manpower", ""));
        report.setMaterialsUsed(json.optString("materialsUsed", ""));
        report.setEquipment(json.optString("equipment", ""));
        report.setSiteProblems(json.optString("siteProblems", ""));
        report.setPhotoUri(json.optString("photoUri", null));
        report.setNextDayPlans(json.optString("nextDayPlans", ""));
        report.setCreatedAt(json.optString("createdAt", ""));
        report.setSubmittedAt(json.optString("submittedAt", ""));
        return report;
    }
}
