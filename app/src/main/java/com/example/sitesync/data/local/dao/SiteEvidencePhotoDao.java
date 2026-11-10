package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sitesync.data.local.entities.SiteEvidencePhotoEntity;

import java.util.List;

@Dao
public interface SiteEvidencePhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SiteEvidencePhotoEntity photo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SiteEvidencePhotoEntity> photos);

    @Query("SELECT * FROM site_evidence_photos WHERE projectCode = :projectCode ORDER BY timestamp DESC")
    LiveData<List<SiteEvidencePhotoEntity>> getPhotosForProjectLive(String projectCode);

    @Query("SELECT * FROM site_evidence_photos WHERE projectCode = :projectCode ORDER BY timestamp DESC")
    List<SiteEvidencePhotoEntity> getPhotosForProject(String projectCode);

    @Query("UPDATE site_evidence_photos SET cloudStorageUrl = :url, isUploadedToCloud = 1 WHERE id = :photoId")
    void markUploaded(String photoId, String url);

    @Query("SELECT COUNT(*) FROM site_evidence_photos WHERE projectCode = :projectCode")
    int getPhotoCount(String projectCode);
}
