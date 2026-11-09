package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sitesync.data.local.entities.PendingSyncEntity;

import java.util.List;

@Dao
public interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PendingSyncEntity pending);

    @Delete
    void delete(PendingSyncEntity pending);

    @Query("SELECT * FROM pending_sync_queue ORDER BY timestamp ASC")
    List<PendingSyncEntity> getAllPending();

    @Query("SELECT * FROM pending_sync_queue ORDER BY timestamp ASC")
    List<PendingSyncEntity> getPendingMutationsSync();

    @Query("SELECT * FROM pending_sync_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    List<PendingSyncEntity> getPendingUnsyncedMutations();

    @Query("SELECT COUNT(*) FROM pending_sync_queue WHERE status = 'PENDING'")
    LiveData<Integer> getPendingCountLiveData();

    @Query("SELECT COUNT(*) FROM pending_sync_queue WHERE status = 'PENDING'")
    int getPendingCount();

    @Query("UPDATE pending_sync_queue SET status = :status WHERE id = :id")
    void updateStatus(String id, String status);

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM pending_sync_queue")
    void clearAll();
}
