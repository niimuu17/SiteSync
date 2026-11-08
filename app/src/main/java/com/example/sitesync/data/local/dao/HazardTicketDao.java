package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sitesync.data.local.entities.HazardTicketEntity;

import java.util.List;

@Dao
public interface HazardTicketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HazardTicketEntity hazard);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HazardTicketEntity> hazards);

    @Update
    void update(HazardTicketEntity hazard);

    @Delete
    void delete(HazardTicketEntity hazard);

    @Query("SELECT * FROM hazard_tickets ORDER BY reportedAt DESC")
    LiveData<List<HazardTicketEntity>> getAllHazards();

    @Query("SELECT * FROM hazard_tickets ORDER BY reportedAt DESC")
    List<HazardTicketEntity> getAllHazardsSync();

    @Query("SELECT * FROM hazard_tickets WHERE status = :status ORDER BY reportedAt DESC")
    LiveData<List<HazardTicketEntity>> getHazardsByStatus(String status);

    @Query("SELECT * FROM hazard_tickets WHERE status = :status ORDER BY reportedAt DESC")
    List<HazardTicketEntity> getHazardsByStatusSync(String status);

    @Query("UPDATE hazard_tickets SET status = 'RESOLVED', resolutionNotes = :notes, resolvedAt = :timestamp, isSynced = 0 WHERE id = :id")
    void markAsResolved(String id, String notes, long timestamp);

    @Query("SELECT COUNT(*) FROM hazard_tickets WHERE status = 'OPEN'")
    int getOpenHazardCount();
}
