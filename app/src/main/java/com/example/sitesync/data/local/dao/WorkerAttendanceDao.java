package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.sitesync.data.local.entities.WorkerAttendanceEntity;

import java.util.List;

@Dao
public interface WorkerAttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkerAttendanceEntity attendance);

    @Update
    void update(WorkerAttendanceEntity attendance);

    @Query("SELECT * FROM worker_attendances WHERE workerId = :workerId ORDER BY clockInTime DESC LIMIT 1")
    WorkerAttendanceEntity getLatestAttendanceForWorker(String workerId);

    @Query("SELECT * FROM worker_attendances WHERE workerId = :workerId ORDER BY clockInTime DESC LIMIT 1")
    LiveData<WorkerAttendanceEntity> getLatestAttendanceForWorkerLive(String workerId);

    @Query("SELECT * FROM worker_attendances WHERE projectCode = :projectCode ORDER BY clockInTime DESC")
    LiveData<List<WorkerAttendanceEntity>> getProjectAttendancesLive(String projectCode);

    @Query("SELECT * FROM worker_attendances WHERE projectCode = :projectCode ORDER BY clockInTime DESC")
    List<WorkerAttendanceEntity> getProjectAttendancesSync(String projectCode);

    @Query("UPDATE worker_attendances SET clockOutTime = :clockOutTime, isClockedIn = 0, shiftStatus = 'COMPLETED' WHERE id = :id")
    void clockOut(String id, long clockOutTime);
}
