package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.sitesync.models.DailySiteReport;

import org.json.JSONObject;

/**
 * Room Entity for 5-Section Daily Site Reports.
 */
@Entity(tableName = "local_daily_reports")
public class LocalReportEntity {

    @PrimaryKey
    @NonNull
    public String id = "";
    public String projectName;
    public String reportDate;
    public String weather;
    public String completedWork;
    public String manpower;
    public String materialsUsed;
    public String equipment;
    public String siteProblems;
    public String nextDayPlans;
    public String preparedBy;
    public String photoUri;
    public String status;
    public String rawJson;
    public long lastUpdated;

    public LocalReportEntity() {}

    public static LocalReportEntity fromModel(DailySiteReport model) {
        LocalReportEntity entity = new LocalReportEntity();
        entity.id = model.getId() != null ? model.getId() : "";
        entity.projectName = model.getProjectName();
        entity.reportDate = model.getReportDate();
        entity.weather = model.getWeather();
        entity.completedWork = model.getCompletedWork();
        entity.manpower = model.getManpower();
        entity.materialsUsed = model.getMaterialsUsed();
        entity.equipment = model.getEquipment();
        entity.siteProblems = model.getSiteProblems();
        entity.nextDayPlans = model.getNextDayPlans();
        entity.preparedBy = model.getPreparedBy();
        entity.photoUri = model.getPhotoUri();
        entity.status = model.getStatus();
        try {
            entity.rawJson = model.toJSONObject().toString();
        } catch (Exception e) {
            entity.rawJson = "";
        }
        entity.lastUpdated = System.currentTimeMillis();
        return entity;
    }

    public DailySiteReport toModel() {
        if (rawJson != null && !rawJson.isEmpty()) {
            try {
                return DailySiteReport.fromJSONObject(new JSONObject(rawJson));
            } catch (Exception ignored) {
            }
        }
        return new DailySiteReport(
                id, projectName, reportDate, preparedBy,
                status != null ? status : DailySiteReport.STATUS_SUBMITTED,
                weather, completedWork, manpower, materialsUsed,
                equipment, siteProblems, photoUri, nextDayPlans,
                "", ""
        );
    }
}
