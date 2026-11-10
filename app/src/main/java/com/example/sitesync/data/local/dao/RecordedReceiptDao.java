package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sitesync.data.local.entities.RecordedReceipt;

import java.util.List;

@Dao
public interface RecordedReceiptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecordedReceipt receipt);

    @Query("SELECT * FROM recorded_receipts WHERE LOWER(TRIM(challanNumber)) = LOWER(TRIM(:challanNumber)) LIMIT 1")
    RecordedReceipt findReceiptByChallan(String challanNumber);

    @Query("SELECT * FROM recorded_receipts WHERE receiptFingerprint = :fingerprint LIMIT 1")
    RecordedReceipt findReceiptByFingerprint(String fingerprint);

    @Query("SELECT * FROM recorded_receipts WHERE imageHash IS NOT NULL AND TRIM(imageHash) != '' AND imageHash = :imageHash LIMIT 1")
    RecordedReceipt findReceiptByImageHash(String imageHash);

    @Query("SELECT * FROM recorded_receipts WHERE " +
           "(TRIM(:challanNumber) != '' AND LOWER(TRIM(challanNumber)) = LOWER(TRIM(:challanNumber))) " +
           "OR (TRIM(:fingerprint) != '' AND receiptFingerprint = :fingerprint) " +
           "OR (TRIM(:imageHash) != '' AND imageHash = :imageHash) " +
           "ORDER BY recordedAt DESC LIMIT 1")
    RecordedReceipt findDuplicate(String challanNumber, String fingerprint, String imageHash);

    @Query("SELECT * FROM recorded_receipts WHERE projectId = :projectCode AND (" +
           "(TRIM(:challanNumber) != '' AND LOWER(TRIM(challanNumber)) = LOWER(TRIM(:challanNumber))) " +
           "OR (TRIM(:fingerprint) != '' AND receiptFingerprint = :fingerprint) " +
           "OR (TRIM(:imageHash) != '' AND imageHash = :imageHash)) " +
           "ORDER BY recordedAt DESC LIMIT 1")
    RecordedReceipt findDuplicateForProject(String projectCode, String challanNumber, String fingerprint, String imageHash);

    @Query("SELECT * FROM recorded_receipts ORDER BY recordedAt DESC")
    LiveData<List<RecordedReceipt>> getAllReceiptsLiveData();

    @Query("SELECT * FROM recorded_receipts ORDER BY recordedAt DESC")
    List<RecordedReceipt> getAllReceiptsSync();

    @Query("SELECT COUNT(*) FROM recorded_receipts")
    int getReceiptCount();

    @Query("DELETE FROM recorded_receipts")
    void deleteAll();
}
