package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.UUID;

/**
 * Entity representing construction materials requested by crew in the chat room
 * (e.g. "10 bag cement lagbe", "300 kg rod lagbe") extracted by LLM intelligence.
 * Tracked in the "Required Items" procurement list and automatically marked
 * fulfilled when matching delivery challans arrive via Vision OCR.
 */
@Entity(tableName = "required_materials")
public class RequiredMaterialItemEntity implements Serializable {

    @PrimaryKey
    @NonNull
    private String id;
    private String projectCode;
    private String itemName;
    private String category;
    private double quantity;
    private String unit;
    private String urgency; // "HIGH", "MEDIUM", "LOW"
    private String requestedBy;
    private String rawSentence;
    private String imageUrl;
    private String status; // "PENDING", "FULFILLED", "CANCELLED"
    private long createdAt;
    private long fulfilledAt;
    private String fulfilledChallanNumber;

    public RequiredMaterialItemEntity() {
        this.id = UUID.randomUUID().toString();
        this.status = "PENDING";
        this.createdAt = System.currentTimeMillis();
    }

    public RequiredMaterialItemEntity(@NonNull String id, String projectCode, String itemName,
                                      String category, double quantity, String unit,
                                      String urgency, String requestedBy, String rawSentence,
                                      String imageUrl) {
        this.id = id;
        this.projectCode = projectCode;
        this.itemName = itemName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.urgency = urgency != null ? urgency : "MEDIUM";
        this.requestedBy = requestedBy;
        this.rawSentence = rawSentence;
        this.imageUrl = imageUrl;
        this.status = "PENDING";
        this.createdAt = System.currentTimeMillis();
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getRawSentence() { return rawSentence; }
    public void setRawSentence(String rawSentence) { this.rawSentence = rawSentence; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(long fulfilledAt) { this.fulfilledAt = fulfilledAt; }

    public String getFulfilledChallanNumber() { return fulfilledChallanNumber; }
    public void setFulfilledChallanNumber(String fulfilledChallanNumber) { this.fulfilledChallanNumber = fulfilledChallanNumber; }
}
