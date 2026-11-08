package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.sitesync.models.ConstructionActivity;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Room Entity for Construction Milestones and Tasks.
 */
@Entity(tableName = "local_activities")
public class LocalActivityEntity {

    @PrimaryKey
    @NonNull
    public String id = "";
    public String projectName;
    public String activityName;
    public String location;
    public int progressPercentage;
    public String status;
    public String remarks;
    public String photoUri;
    public String rawJson;
    public long lastUpdated;

    public LocalActivityEntity() {}

    public static LocalActivityEntity fromModel(ConstructionActivity model) {
        LocalActivityEntity entity = new LocalActivityEntity();
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
