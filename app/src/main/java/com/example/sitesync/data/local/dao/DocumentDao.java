package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sitesync.data.local.DocumentEntity;

import java.util.List;

@Dao
public interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastUpdated DESC")
    LiveData<List<DocumentEntity>> getAllLive();

    @Query("SELECT * FROM documents ORDER BY lastUpdated DESC")
    List<DocumentEntity> getAllSync();

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    DocumentEntity getById(String id);

    @Query("SELECT * FROM documents WHERE discipline = :discipline ORDER BY lastUpdated DESC")
    List<DocumentEntity> getByDiscipline(String discipline);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DocumentEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DocumentEntity> entities);

    @Delete
    void delete(DocumentEntity entity);
}
