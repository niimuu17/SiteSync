package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "site_chat_messages")
public class SiteChatMessageEntity implements Serializable {

    @PrimaryKey
    @NonNull
    private String id;
    private String projectCode;
    private String senderId;
    private String senderName;
    private String senderRole; // "PROJECT_MANAGER", "SITE_SUPERVISOR", "ENGINEER", "WORKER"
    private String messageText;
    private long timestamp;
    private boolean isUrgent;
    private String mediaType; // "TEXT", "IMAGE", "AUDIO"
    private String mediaUrl;

    public SiteChatMessageEntity() {
        this.id = "";
        this.timestamp = System.currentTimeMillis();
    }

    public SiteChatMessageEntity(@NonNull String id, String projectCode, String senderId,
                                 String senderName, String senderRole, String messageText,
                                 long timestamp, boolean isUrgent) {
        this.id = id;
        this.projectCode = projectCode;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.isUrgent = isUrgent;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isUrgent() { return isUrgent; }
    public void setUrgent(boolean urgent) { isUrgent = urgent; }

    public String getMediaType() { return mediaType != null ? mediaType : "TEXT"; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}
