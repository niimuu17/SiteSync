package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sitesync.data.local.entities.RequiredMaterialItemEntity;

import java.util.List;

@Dao
public interface RequiredMaterialItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RequiredMaterialItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RequiredMaterialItemEntity> items);

    @Update
    void update(RequiredMaterialItemEntity item);

    @Query("SELECT * FROM required_materials WHERE projectCode = :projectCode AND status = 'PENDING' ORDER BY createdAt DESC")
    LiveData<List<RequiredMaterialItemEntity>> getPendingItemsForProjectLive(String projectCode);

    @Query("SELECT * FROM required_materials WHERE projectCode = :projectCode AND status = 'PENDING' ORDER BY createdAt DESC")
    List<RequiredMaterialItemEntity> getPendingItemsForProject(String projectCode);

    @Query("SELECT * FROM required_materials WHERE projectCode = :projectCode ORDER BY createdAt DESC")
    LiveData<List<RequiredMaterialItemEntity>> getAllItemsForProjectLive(String projectCode);

    @Query("UPDATE required_materials SET status = 'FULFILLED', fulfilledAt = :fulfilledAt, fulfilledChallanNumber = :challanNumber WHERE id = :id")
    void markItemFulfilled(String id, String challanNumber, long fulfilledAt);

    @Query("UPDATE required_materials SET status = 'FULFILLED', fulfilledAt = :fulfilledAt, fulfilledChallanNumber = :challanNumber WHERE projectCode = :projectCode AND status = 'PENDING' AND (LOWER(itemName) LIKE '%' || LOWER(:keyword) || '%' OR LOWER(:keyword) LIKE '%' || LOWER(itemName) || '%')")
    int fulfillMatchingItem(String projectCode, String keyword, String challanNumber, long fulfilledAt);

    @Query("DELETE FROM required_materials WHERE id = :id")
    void deleteItem(String id);

    @Query("DELETE FROM required_materials WHERE projectCode = :projectCode")
    void deleteByProject(String projectCode);
}
