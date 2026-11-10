package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sitesync.data.local.entities.InventoryItem;

import java.util.List;

@Dao
public interface InventoryItemDao {

    @Query("SELECT * FROM inventory_items ORDER BY itemName ASC")
    LiveData<List<InventoryItem>> getAllItemsLiveData();

    @Query("SELECT * FROM inventory_items WHERE projectCode = :projectCode ORDER BY itemName ASC")
    LiveData<List<InventoryItem>> getItemsForProjectLiveData(String projectCode);

    @Query("SELECT * FROM inventory_items ORDER BY itemName ASC")
    List<InventoryItem> getAllItemsSync();

    @Query("SELECT * FROM inventory_items WHERE projectCode = :projectCode ORDER BY itemName ASC")
    List<InventoryItem> getItemsForProjectSync(String projectCode);

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    InventoryItem getItemById(long id);

    @Query("SELECT * FROM inventory_items WHERE LOWER(itemName) = LOWER(:name) LIMIT 1")
    InventoryItem getItemByName(String name);

    @Query("SELECT * FROM inventory_items WHERE projectCode = :projectCode AND LOWER(itemName) = LOWER(:name) LIMIT 1")
    InventoryItem getItemByName(String projectCode, String name);

    @Query("SELECT * FROM inventory_items WHERE LOWER(itemName) LIKE '%' || LOWER(:query) || '%' LIMIT 1")
    InventoryItem findBestMatchItem(String query);

    @Query("SELECT * FROM inventory_items WHERE projectCode = :projectCode AND LOWER(itemName) LIKE '%' || LOWER(:query) || '%' LIMIT 1")
    InventoryItem findBestMatchItem(String projectCode, String query);

    @Query("SELECT * FROM inventory_items WHERE currentBalance <= reorderLevel")
    List<InventoryItem> getLowStockItems();

    @Query("SELECT * FROM inventory_items WHERE projectCode = :projectCode AND currentBalance <= reorderLevel")
    List<InventoryItem> getLowStockItems(String projectCode);

    @Query("SELECT COUNT(*) FROM inventory_items")
    int getItemCount();

    @Query("SELECT COUNT(*) FROM inventory_items WHERE projectCode = :projectCode")
    int getItemCountForProject(String projectCode);

    @Query("SELECT SUM(currentBalance * avgUnitPrice) FROM inventory_items")
    Double getTotalValuation();

    @Query("SELECT SUM(currentBalance * avgUnitPrice) FROM inventory_items WHERE projectCode = :projectCode")
    Double getTotalValuationForProject(String projectCode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(InventoryItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<InventoryItem> items);

    @Update
    void update(InventoryItem item);

    @Delete
    void delete(InventoryItem item);

    @Query("DELETE FROM inventory_items")
    void deleteAll();
}
