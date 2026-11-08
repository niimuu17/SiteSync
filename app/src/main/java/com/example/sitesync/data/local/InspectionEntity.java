package com.example.sitesync.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.sitesync.models.SiteInspection;

import org.json.JSONObject;

import java.util.ArrayList;

@Entity(tableName = "inspections")
public class InspectionEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String projectName;
    public String location;
    public String inspectionType;
    public String date;
    public String status;
    public String observations;
    public String remarks;
    public String inspectorName;
    public String photoUri;
    public String rawJson;
    public long lastUpdated;

    public InspectionEntity() {
        this.id = "";
    }

    public static InspectionEntity fromModel(SiteInspection model) {
        InspectionEntity entity = new InspectionEntity();
        entity.id = model.getId() != null ? model.getId() : "";
        entity.projectName = model.getProjectName();
        entity.location = model.getLocation();
        entity.inspectionType = model.getInspectionType();
        entity.date = model.getDate();
        entity.status = model.getStatus();
        entity.observations = model.getObservations();
        entity.remarks = model.getRemarks();
        entity.inspectorName = model.getInspectorName();
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
                id, projectName, location, inspectionType, date, status,
                observations, remarks, inspectorName, photoUri,
                new ArrayList<>(), new ArrayList<>()
        );
    }
}
