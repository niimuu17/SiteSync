package com.example.sitesync.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.sitesync.models.DailySiteReport;

import org.json.JSONObject;

@Entity(tableName = "daily_reports")
public class DailyReportEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String projectName;
    public String reportDate;
    public String preparedBy;
    public String status;
    public String weather;
    public String completedWork;
    public String manpower;
    public String materialsUsed;
    public String equipment;
    public String siteProblems;
    public String photoUri;
    public String nextDayPlans;
    public String createdAt;
    public String submittedAt;
    public String rawJson;
    public long lastUpdated;

    public DailyReportEntity() {
        this.id = "";
    }

    public static DailyReportEntity fromModel(DailySiteReport model) {
        DailyReportEntity entity = new DailyReportEntity();
        entity.id = model.getId() != null ? model.getId() : "";
        entity.projectName = model.getProjectName();
        entity.reportDate = model.getReportDate();
        entity.preparedBy = model.getPreparedBy();
        entity.status = model.getStatus();
        entity.weather = model.getWeather();
        entity.completedWork = model.getCompletedWork();
        entity.manpower = model.getManpower();
        entity.materialsUsed = model.getMaterialsUsed();
        entity.equipment = model.getEquipment();
        entity.siteProblems = model.getSiteProblems();
        entity.photoUri = model.getPhotoUri();
        entity.nextDayPlans = model.getNextDayPlans();
        entity.createdAt = model.getCreatedAt();
        entity.submittedAt = model.getSubmittedAt();
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
                id, projectName, reportDate, preparedBy, status, weather,
                completedWork, manpower, materialsUsed, equipment, siteProblems,
                photoUri, nextDayPlans, createdAt, submittedAt
        );
    }
}
