package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "worker_join_requests")
public class WorkerJoinRequestEntity implements Serializable {

    @PrimaryKey
    @NonNull
    private String id;
    private String workerId;
    private String workerName;
    private String workerPhone;
    private String workerTrade; // e.g. "Mason", "Electrician", "Steel Fixer", "Tile Mistry"
    private String projectCode;
    private String status; // "PENDING", "APPROVED", "REJECTED"
    private long requestedAt;
    private long reviewedAt;

    public WorkerJoinRequestEntity() {
        this.id = "";
        this.status = "PENDING";
        this.requestedAt = System.currentTimeMillis();
    }

    public WorkerJoinRequestEntity(@NonNull String id, String workerId, String workerName,
                                   String workerPhone, String workerTrade, String projectCode,
                                   String status, long requestedAt) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.workerPhone = workerPhone;
        this.workerTrade = workerTrade;
        this.projectCode = projectCode;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getWorkerPhone() { return workerPhone; }
    public void setWorkerPhone(String workerPhone) { this.workerPhone = workerPhone; }

    public String getWorkerTrade() { return workerTrade; }
    public void setWorkerTrade(String workerTrade) { this.workerTrade = workerTrade; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }

    public long getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }
}
