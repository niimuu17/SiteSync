package com.example.sitesync.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.sitesync.models.ConstructionActivity;

import org.json.JSONObject;

import java.util.ArrayList;

@Entity(tableName = "activities")
public class ActivityEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String projectName;
    public String activityName;
    public String location;
    public int progressPercentage;
    public String status;
    public String remarks;
    public String photoUri;
    public String rawJson;
    public long lastUpdated;

    public ActivityEntity() {
        this.id = "";
    }

    public static ActivityEntity fromModel(ConstructionActivity model) {
        ActivityEntity entity = new ActivityEntity();
        entity.id = model.getId() != null ? model.getId() : "";
        entity.projectName = model.getProjectName();
        entity.activityName = model.getActivityName();
        entity.location = model.getLocation();
        entity.progressPercentage = model.getProgressPercentage();
        entity.status = model.getStatus();
        entity.remarks = model.getLatestRemarks();
        entity.photoUri = model.getLatestPhotoUri();
        try {
            entity.rawJson = model.toJSONObject().toString();
        } catch (Exception e) {
            entity.rawJson = "";
        }
        entity.lastUpdated = System.currentTimeMillis();
        return entity;
    }

    public ConstructionActivity toModel() {
        if (rawJson != null && !rawJson.isEmpty()) {
            try {
                return ConstructionActivity.fromJSONObject(new JSONObject(rawJson));
            } catch (Exception ignored) {
            }
        }
        return new ConstructionActivity(
                id, projectName, activityName, location, progressPercentage,
                status, "Recent", "Site Engineer", remarks, photoUri, new ArrayList<>()
        );
    }
}
