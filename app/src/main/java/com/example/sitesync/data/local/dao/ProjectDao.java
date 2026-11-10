package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sitesync.data.local.entities.ProjectEntity;

import java.util.List;

@Dao
public interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProjectEntity project);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProjectEntity> projects);

    @Update
    void update(ProjectEntity project);

    @Delete
    void delete(ProjectEntity project);

    @Query("DELETE FROM projects WHERE id = :projectId OR projectCode = :projectId")
    void deleteById(String projectId);

    @Query("SELECT * FROM projects ORDER BY lastOpenedAt DESC")
    LiveData<List<ProjectEntity>> getAllProjectsLive();

    @Query("SELECT * FROM projects ORDER BY lastOpenedAt DESC")
    List<ProjectEntity> getAllProjects();

    @Query("SELECT * FROM projects ORDER BY lastOpenedAt DESC LIMIT :limit")
    LiveData<List<ProjectEntity>> getRecentProjectsLive(int limit);

    @Query("SELECT * FROM projects ORDER BY lastOpenedAt DESC LIMIT :limit")
    List<ProjectEntity> getRecentProjects(int limit);

    @Query("SELECT * FROM projects WHERE id = :id OR projectCode = :id LIMIT 1")
    ProjectEntity getProjectById(String id);

    @Query("SELECT * FROM projects WHERE id = :id OR projectCode = :id LIMIT 1")
    LiveData<ProjectEntity> getProjectByIdLive(String id);

    @Query("UPDATE projects SET lastOpenedAt = :timestamp WHERE id = :id OR projectCode = :id")
    void updateLastOpened(String id, long timestamp);

    @Query("UPDATE projects SET overallProgressPercent = :overall, civilProgressPercent = :civil, electricProgressPercent = :electric, plumbingProgressPercent = :plumbing, thaiGlassProgressPercent = :thaiGlass, tilesFlooringProgressPercent = :tiles, interiorPlasterProgressPercent = :interior WHERE id = :id OR projectCode = :id")
    void updateProgressMetrics(String id, float overall, float civil, float electric, float plumbing, float thaiGlass, float tiles, float interior);

    @Query("SELECT COUNT(*) FROM projects")
    int getProjectCount();
}
