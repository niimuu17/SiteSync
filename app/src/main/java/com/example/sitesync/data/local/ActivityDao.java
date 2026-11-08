package com.example.sitesync.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY lastUpdated DESC")
    LiveData<List<ActivityEntity>> getAllLive();

    @Query("SELECT * FROM activities ORDER BY lastUpdated DESC")
    List<ActivityEntity> getAllSync();

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    ActivityEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ActivityEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ActivityEntity> entities);

    @Delete
    void delete(ActivityEntity entity);

    @Query("DELETE FROM activities WHERE id = :id")
    void deleteById(String id);
}
