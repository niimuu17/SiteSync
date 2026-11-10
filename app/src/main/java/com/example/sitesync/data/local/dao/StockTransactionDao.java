package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sitesync.data.local.entities.StockTransaction;

import java.util.List;

@Dao
public interface StockTransactionDao {

    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    LiveData<List<StockTransaction>> getAllTransactionsLiveData();

    @Query("SELECT * FROM stock_transactions WHERE projectId = :projectCode ORDER BY timestamp DESC")
    LiveData<List<StockTransaction>> getTransactionsForProjectLiveData(String projectCode);

    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    List<StockTransaction> getAllTransactionsSync();

    @Query("SELECT * FROM stock_transactions WHERE projectId = :projectCode ORDER BY timestamp DESC")
    List<StockTransaction> getTransactionsForProjectSync(String projectCode);

    @Query("SELECT * FROM stock_transactions WHERE type = :type ORDER BY timestamp DESC")
    LiveData<List<StockTransaction>> getTransactionsByTypeLiveData(String type);

    @Query("SELECT * FROM stock_transactions WHERE projectId = :projectCode AND type = :type ORDER BY timestamp DESC")
    LiveData<List<StockTransaction>> getTransactionsByTypeForProjectLiveData(String projectCode, String type);

    @Query("SELECT * FROM stock_transactions WHERE itemId = :itemId ORDER BY timestamp DESC")
    List<StockTransaction> getTransactionsByItem(long itemId);

    @Query("SELECT * FROM stock_transactions WHERE type = :type ORDER BY timestamp DESC")
    List<StockTransaction> getTransactionsByTypeSync(String type);

    @Query("SELECT SUM(totalCost) FROM stock_transactions WHERE type = 'STOCK_IN_CHALLAN'")
    Double getTotalStockInCost();

    @Query("SELECT SUM(totalCost) FROM stock_transactions WHERE type = 'STOCK_OUT_CONSUMPTION'")
    Double getTotalStockOutCost();

    @Query("SELECT * FROM stock_transactions WHERE type = 'STOCK_IN_CHALLAN' AND challanNumber IS NOT NULL AND TRIM(challanNumber) != '' AND LOWER(TRIM(challanNumber)) = LOWER(TRIM(:challanNumber)) ORDER BY timestamp DESC LIMIT 1")
    StockTransaction findTransactionByChallan(String challanNumber);

    @Query("SELECT * FROM stock_transactions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    List<StockTransaction> getTransactionsSince(long sinceTimestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StockTransaction transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<StockTransaction> transactions);

    @Query("DELETE FROM stock_transactions")
    void deleteAll();
}
