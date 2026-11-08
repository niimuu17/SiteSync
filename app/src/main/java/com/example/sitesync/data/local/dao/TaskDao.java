package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sitesync.data.local.entities.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TaskEntity task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TaskEntity> tasks);

    @Update
    void update(TaskEntity task);

    @Delete
    void delete(TaskEntity task);

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getAllTasks();

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    List<TaskEntity> getAllTasksSync();

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getTasksByStatus(String status);

    @Query("SELECT * FROM tasks WHERE status != 'COMPLETED' ORDER BY dueDate ASC LIMIT 3")
    LiveData<List<TaskEntity>> getUpcomingMilestones();

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    TaskEntity getTaskById(String id);

    @Query("UPDATE tasks SET status = :status, isSynced = 0 WHERE id = :id")
    void updateTaskStatus(String id, String status);

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'COMPLETED'")
    int getActiveTaskCount();
}
