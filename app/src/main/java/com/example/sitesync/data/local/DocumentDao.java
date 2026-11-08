package com.example.sitesync.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastUpdated DESC")
    LiveData<List<DocumentEntity>> getAllLive();

    @Query("SELECT * FROM documents ORDER BY lastUpdated DESC")
    List<DocumentEntity> getAllSync();

    @Query("SELECT * FROM documents WHERE category = :category ORDER BY lastUpdated DESC")
    LiveData<List<DocumentEntity>> getByCategoryLive(String category);

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    DocumentEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DocumentEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DocumentEntity> entities);

    @Delete
    void delete(DocumentEntity entity);

    @Query("DELETE FROM documents WHERE id = :id")
    void deleteById(String id);
}
