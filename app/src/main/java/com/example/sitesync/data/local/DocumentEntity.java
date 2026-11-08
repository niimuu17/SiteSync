package com.example.sitesync.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.sitesync.models.SiteDocument;

import org.json.JSONObject;

@Entity(tableName = "documents")
public class DocumentEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String projectName;
    public String category;
    public String uploadDate;
    public String currentRevision;
    public boolean isApproved;
    public String uploadedBy;
    public String fileFormat;
    public String fileSize;
    public String fileUri;
    public String description;
    public String rawJson;
    public long lastUpdated;

    public DocumentEntity() {
        this.id = "";
    }

    public static DocumentEntity fromModel(SiteDocument model) {
        DocumentEntity entity = new DocumentEntity();
        entity.id = model.getId() != null ? model.getId() : "";
        entity.name = model.getName();
        entity.projectName = model.getProjectName();
        entity.category = model.getCategory();
        entity.uploadDate = model.getUploadDate();
        entity.currentRevision = model.getCurrentRevision();
        entity.isApproved = model.isApproved();
        entity.uploadedBy = model.getUploadedBy();
        entity.fileFormat = model.getFileFormat();
        entity.fileSize = model.getFileSize();
        entity.fileUri = model.getFileUri();
        entity.description = model.getDescription();
        try {
            entity.rawJson = model.toJSONObject().toString();
        } catch (Exception e) {
            entity.rawJson = "";
        }
        entity.lastUpdated = System.currentTimeMillis();
        return entity;
    }

    public SiteDocument toModel() {
        if (rawJson != null && !rawJson.isEmpty()) {
            try {
                return SiteDocument.fromJSONObject(new JSONObject(rawJson));
            } catch (Exception ignored) {
            }
        }
        return new SiteDocument(
                id, name, projectName, category, uploadDate, currentRevision,
                isApproved, uploadedBy, fileFormat, fileSize, fileUri, description, null
        );
    }
}
