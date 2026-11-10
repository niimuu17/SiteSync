package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "site_evidence_photos")
public class SiteEvidencePhotoEntity implements Serializable {

    @PrimaryKey
    @NonNull
    private String id;
    private String projectCode;
    private String localFilePath;
    private String cloudStorageUrl;
    private double latitude;
    private double longitude;
    private double elevationMeters;
    private String locationLabel;
    private String capturedBy;
    private String caption;
    private long timestamp;
    private boolean isUploadedToCloud;

    public SiteEvidencePhotoEntity() {
        this.id = "";
        this.timestamp = System.currentTimeMillis();
    }

    public SiteEvidencePhotoEntity(@NonNull String id, String projectCode, String localFilePath,
                                   String cloudStorageUrl, double latitude, double longitude,
                                   double elevationMeters, String locationLabel, String capturedBy,
                                   String caption, long timestamp, boolean isUploadedToCloud) {
        this.id = id;
        this.projectCode = projectCode;
        this.localFilePath = localFilePath;
        this.cloudStorageUrl = cloudStorageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevationMeters = elevationMeters;
        this.locationLabel = locationLabel;
        this.capturedBy = capturedBy;
        this.caption = caption;
        this.timestamp = timestamp;
        this.isUploadedToCloud = isUploadedToCloud;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public String getCloudStorageUrl() { return cloudStorageUrl; }
    public void setCloudStorageUrl(String cloudStorageUrl) { this.cloudStorageUrl = cloudStorageUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getElevationMeters() { return elevationMeters; }
    public void setElevationMeters(double elevationMeters) { this.elevationMeters = elevationMeters; }

    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }

    public String getCapturedBy() { return capturedBy; }
    public void setCapturedBy(String capturedBy) { this.capturedBy = capturedBy; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isUploadedToCloud() { return isUploadedToCloud; }
    public void setUploadedToCloud(boolean uploadedToCloud) { isUploadedToCloud = uploadedToCloud; }
}
