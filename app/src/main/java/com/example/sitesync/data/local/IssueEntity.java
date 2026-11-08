package com.example.sitesync.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.sitesync.models.TechnicalIssue;

import org.json.JSONObject;

@Entity(tableName = "issues")
public class IssueEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public String description;
    public String projectName;
    public String siteLocation;
    public String priority;
    public String status;
    public String assignedPerson;
    public String createdBy;
    public String createdDate;
    public String photoUri;
    public String rawJson;
    public long lastUpdated;

    public IssueEntity() {
        this.id = "";
    }

    public static IssueEntity fromModel(TechnicalIssue model) {
        IssueEntity entity = new IssueEntity();
        entity.id = model.getId() != null ? model.getId() : "";
        entity.title = model.getTitle();
        entity.description = model.getDescription();
        entity.projectName = model.getProjectName();
        entity.siteLocation = model.getSiteLocation();
        entity.priority = model.getPriority();
        entity.status = model.getStatus();
        entity.assignedPerson = model.getAssignedPerson();
        entity.createdBy = model.getCreatedBy();
        entity.createdDate = model.getCreatedDate();
        entity.photoUri = model.getPhotoUri();
        try {
            entity.rawJson = model.toJSONObject().toString();
        } catch (Exception e) {
            entity.rawJson = "";
        }
        entity.lastUpdated = System.currentTimeMillis();
        return entity;
    }

    public TechnicalIssue toModel() {
        if (rawJson != null && !rawJson.isEmpty()) {
            try {
                return TechnicalIssue.fromJSONObject(new JSONObject(rawJson));
            } catch (Exception ignored) {
            }
        }
        return new TechnicalIssue(
                id, title, description, projectName, siteLocation, priority,
                status, assignedPerson, createdBy, createdDate, photoUri
        );
    }
}
