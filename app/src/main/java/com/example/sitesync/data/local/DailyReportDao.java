package com.example.sitesync.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DailyReportDao {
    @Query("SELECT * FROM daily_reports ORDER BY lastUpdated DESC")
    LiveData<List<DailyReportEntity>> getAllLive();

    @Query("SELECT * FROM daily_reports ORDER BY lastUpdated DESC")
    List<DailyReportEntity> getAllSync();

    @Query("SELECT * FROM daily_reports WHERE id = :id LIMIT 1")
    DailyReportEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DailyReportEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DailyReportEntity> entities);

    @Delete
    void delete(DailyReportEntity entity);

    @Query("DELETE FROM daily_reports WHERE id = :id")
    void deleteById(String id);
}
