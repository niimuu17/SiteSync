package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sitesync.data.local.entities.InventoryLogEntity;

import java.util.List;

@Dao
public interface InventoryLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InventoryLogEntity log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<InventoryLogEntity> logs);

    @Query("SELECT * FROM inventory_logs ORDER BY timestamp DESC LIMIT 20")
    LiveData<List<InventoryLogEntity>> getRecentLogs();

    @Query("SELECT * FROM inventory_logs ORDER BY timestamp DESC LIMIT 20")
    List<InventoryLogEntity> getRecentLogsSync();
}
