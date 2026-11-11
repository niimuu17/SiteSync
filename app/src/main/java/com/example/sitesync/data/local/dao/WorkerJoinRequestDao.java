package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sitesync.data.local.entities.WorkerJoinRequestEntity;

import java.util.List;

@Dao
public interface WorkerJoinRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkerJoinRequestEntity request);

    @Update
    void update(WorkerJoinRequestEntity request);

    @Query("SELECT * FROM worker_join_requests WHERE projectCode = :projectCode ORDER BY requestedAt DESC")
    LiveData<List<WorkerJoinRequestEntity>> getRequestsForProjectLive(String projectCode);

    @Query("SELECT * FROM worker_join_requests WHERE projectCode = :projectCode ORDER BY requestedAt DESC")
    List<WorkerJoinRequestEntity> getRequestsForProject(String projectCode);

    @Query("SELECT * FROM worker_join_requests WHERE projectCode = :projectCode AND status = 'PENDING' ORDER BY requestedAt DESC")
    LiveData<List<WorkerJoinRequestEntity>> getPendingRequestsForProjectLive(String projectCode);

    @Query("SELECT * FROM worker_join_requests WHERE projectCode = :projectCode AND status = 'PENDING' ORDER BY requestedAt DESC")
    List<WorkerJoinRequestEntity> getPendingRequestsForProject(String projectCode);

    @Query("UPDATE worker_join_requests SET status = :status, reviewedAt = :reviewedAt WHERE id = :requestId")
    void updateStatus(String requestId, String status, long reviewedAt);

    @Query("UPDATE worker_join_requests SET status = :status WHERE id = :requestId")
    void updateStatus(String requestId, String status);

    @Query("SELECT * FROM worker_join_requests WHERE workerId = :workerId LIMIT 1")
    WorkerJoinRequestEntity getRequestByWorkerId(String workerId);
}
