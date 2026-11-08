package com.example.sitesync.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface InspectionDao {
    @Query("SELECT * FROM inspections ORDER BY lastUpdated DESC")
    LiveData<List<InspectionEntity>> getAllLive();

    @Query("SELECT * FROM inspections ORDER BY lastUpdated DESC")
    List<InspectionEntity> getAllSync();

    @Query("SELECT * FROM inspections WHERE id = :id LIMIT 1")
    InspectionEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(InspectionEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<InspectionEntity> entities);

    @Delete
    void delete(InspectionEntity entity);

    @Query("DELETE FROM inspections WHERE id = :id")
    void deleteById(String id);
}
