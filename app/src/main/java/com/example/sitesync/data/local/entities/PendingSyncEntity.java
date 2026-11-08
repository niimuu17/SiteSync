package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_sync_queue")
public class PendingSyncEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String entityType; // "TASK", "INVENTORY", "LOG", "HAZARD"
    private String entityId;
    private String action;     // "CREATE", "UPDATE", "DELETE"
    private String payloadJson;
    private long timestamp;

    public PendingSyncEntity(@NonNull String id, String entityType, String entityId,
                             String action, String payloadJson, long timestamp) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.payloadJson = payloadJson;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
