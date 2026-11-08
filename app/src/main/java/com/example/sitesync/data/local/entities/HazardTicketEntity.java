package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hazard_tickets")
public class HazardTicketEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String category; // "Electrical", "Structural", "Scaffolding", etc.
    private String description;
    private String priority; // "LOW", "MEDIUM", "URGENT"
    private String status;   // "OPEN", "RESOLVED"
    private String locationName;
    private String photoPath;
    private String reportedBy;
    private long reportedAt;
    private String resolutionNotes;
    private long resolvedAt;
    private boolean isSynced;

    public HazardTicketEntity(@NonNull String id, String category, String description,
                              String priority, String status, String locationName,
                              String photoPath, String reportedBy, long reportedAt,
                              String resolutionNotes, long resolvedAt, boolean isSynced) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.locationName = locationName;
        this.photoPath = photoPath;
        this.reportedBy = reportedBy;
        this.reportedAt = reportedAt;
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = resolvedAt;
        this.isSynced = isSynced;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    public long getReportedAt() { return reportedAt; }
    public void setReportedAt(long reportedAt) { this.reportedAt = reportedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public long getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(long resolvedAt) { this.resolvedAt = resolvedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
}
