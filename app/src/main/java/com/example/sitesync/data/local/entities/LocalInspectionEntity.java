package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.sitesync.models.SiteInspection;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Room Entity for QA/QC Site Inspections.
 */
@Entity(tableName = "local_inspections")
public class LocalInspectionEntity {

    @PrimaryKey
    @NonNull
    public String id = "";
    public String projectName;
    public String title;
    public String location;
    public String status;
    public String inspectorName;
    public String remarks;
    public String photoUri;
    public String rawJson;
    public long lastUpdated;

    public LocalInspectionEntity() {}

    public static LocalInspectionEntity fromModel(SiteInspection model) {
        LocalInspectionEntity entity = new LocalInspectionEntity();
        entity.id = model.getId() != null ? model.getId() : "";
        entity.projectName = model.getProjectName();
        entity.title = model.getInspectionType();
        entity.location = model.getLocation();
        entity.status = model.getStatus();
        entity.inspectorName = model.getInspectorName();
        entity.remarks = model.getRemarks();
        entity.photoUri = model.getPhotoUri();
        try {
            entity.rawJson = model.toJSONObject().toString();
        } catch (Exception e) {
            entity.rawJson = "";
        }
        entity.lastUpdated = System.currentTimeMillis();
        return entity;
    }

    public SiteInspection toModel() {
        if (rawJson != null && !rawJson.isEmpty()) {
            try {
                return SiteInspection.fromJSONObject(new JSONObject(rawJson));
            } catch (Exception ignored) {
            }
        }
        return new SiteInspection(
                id, projectName, location, title, "Today",
                status != null ? status : SiteInspection.STATUS_PENDING,
                "", remarks, inspectorName, photoUri,
                new ArrayList<>(), new ArrayList<>()
        );
    }
}
