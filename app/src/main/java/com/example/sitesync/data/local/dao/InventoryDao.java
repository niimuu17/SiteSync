package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sitesync.data.local.entities.InventoryItemEntity;

import java.util.List;

@Dao
public interface InventoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InventoryItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<InventoryItemEntity> items);

    @Update
    void update(InventoryItemEntity item);

    @Delete
    void delete(InventoryItemEntity item);

    @Query("SELECT * FROM inventory ORDER BY name ASC")
    LiveData<List<InventoryItemEntity>> getAllInventory();

    @Query("SELECT * FROM inventory ORDER BY name ASC")
    List<InventoryItemEntity> getAllInventorySync();

    @Query("SELECT * FROM inventory WHERE currentStock < minThreshold")
    LiveData<List<InventoryItemEntity>> getLowStockItems();

    @Query("SELECT * FROM inventory WHERE id = :id LIMIT 1")
    InventoryItemEntity getItemById(String id);

    @Query("UPDATE inventory SET currentStock = currentStock + :delta, lastUpdated = :timestamp, isSynced = 0 WHERE id = :id")
    void adjustStock(String id, double delta, long timestamp);
}
