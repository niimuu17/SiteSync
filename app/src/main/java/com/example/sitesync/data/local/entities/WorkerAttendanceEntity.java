package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "worker_attendances")
public class WorkerAttendanceEntity implements Serializable {

    @PrimaryKey
    @NonNull
    private String id;
    private String workerId;
    private String workerName;
    private String projectCode;
    private long clockInTime;
    private long clockOutTime;
    private double clockInLatitude;
    private double clockInLongitude;
    private boolean isClockedIn;
    private String shiftStatus; // "ON_DUTY", "COMPLETED", "ABSENT"

    public WorkerAttendanceEntity() {
        this.id = "";
        this.shiftStatus = "ON_DUTY";
    }

    public WorkerAttendanceEntity(@NonNull String id, String workerId, String workerName,
                                  String projectCode, long clockInTime, double clockInLatitude,
                                  double clockInLongitude) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.projectCode = projectCode;
        this.clockInTime = clockInTime;
        this.clockInLatitude = clockInLatitude;
        this.clockInLongitude = clockInLongitude;
        this.isClockedIn = true;
        this.shiftStatus = "ON_DUTY";
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public long getClockInTime() { return clockInTime; }
    public void setClockInTime(long clockInTime) { this.clockInTime = clockInTime; }

    public long getClockOutTime() { return clockOutTime; }
    public void setClockOutTime(long clockOutTime) { this.clockOutTime = clockOutTime; }

    public double getClockInLatitude() { return clockInLatitude; }
    public void setClockInLatitude(double clockInLatitude) { this.clockInLatitude = clockInLatitude; }

    public double getClockInLongitude() { return clockInLongitude; }
    public void setClockInLongitude(double clockInLongitude) { this.clockInLongitude = clockInLongitude; }

    public boolean isClockedIn() { return isClockedIn; }
    public void setClockedIn(boolean clockedIn) { isClockedIn = clockedIn; }

    public String getShiftStatus() { return shiftStatus; }
    public void setShiftStatus(String shiftStatus) { this.shiftStatus = shiftStatus; }
}
