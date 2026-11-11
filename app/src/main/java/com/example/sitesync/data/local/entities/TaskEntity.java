package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String projectCode;
    private String title;
    private String description;
    private String assignedTo;
    private String priority; // "LOW", "MEDIUM", "URGENT"
    private String status;   // "TODO", "IN_PROGRESS", "REVIEW", "COMPLETED"
    private long dueDate;
    private boolean reminderScheduled;
    private long createdAt;
    private boolean isSynced;

    public TaskEntity(@NonNull String id, String projectCode, String title, String description, String assignedTo,
                      String priority, String status, long dueDate, boolean reminderScheduled,
                      long createdAt, boolean isSynced) {
        this.id = id;
        this.projectCode = projectCode != null ? projectCode : "PRJ-DHAKA-101";
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.reminderScheduled = reminderScheduled;
        this.createdAt = createdAt;
        this.isSynced = isSynced;
    }

    @androidx.room.Ignore
    public TaskEntity(@NonNull String id, String title, String description, String assignedTo,
                      String priority, String status, long dueDate, boolean reminderScheduled,
                      long createdAt, boolean isSynced) {
        this(id, "PRJ-DHAKA-101", title, description, assignedTo, priority, status, dueDate, reminderScheduled, createdAt, isSynced);
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public boolean isReminderScheduled() { return reminderScheduled; }
    public void setReminderScheduled(boolean reminderScheduled) { this.reminderScheduled = reminderScheduled; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
}
