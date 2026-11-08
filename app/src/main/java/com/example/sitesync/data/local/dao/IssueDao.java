package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sitesync.data.local.IssueEntity;

import java.util.List;

@Dao
public interface IssueDao {
    @Query("SELECT * FROM issues ORDER BY lastUpdated DESC")
    LiveData<List<IssueEntity>> getAllLive();

    @Query("SELECT * FROM issues ORDER BY lastUpdated DESC")
    List<IssueEntity> getAllSync();

    @Query("SELECT * FROM issues WHERE id = :id LIMIT 1")
    IssueEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(IssueEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<IssueEntity> entities);

    @Delete
    void delete(IssueEntity entity);
}
