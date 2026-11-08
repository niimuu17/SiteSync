package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inventory_logs")
public class InventoryLogEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String materialId;
    private String materialName;
    private String type; // "STOCK_IN", "STOCK_OUT"
    private double quantity;
    private String unit;
    private String loggedBy;
    private String locationOrSupplier;
    private long timestamp;
    private boolean isSynced;

    public InventoryLogEntity(@NonNull String id, String materialId, String materialName,
                              String type, double quantity, String unit, String loggedBy,
                              String locationOrSupplier, long timestamp, boolean isSynced) {
        this.id = id;
        this.materialId = materialId;
        this.materialName = materialName;
        this.type = type;
        this.quantity = quantity;
        this.unit = unit;
        this.loggedBy = loggedBy;
        this.locationOrSupplier = locationOrSupplier;
        this.timestamp = timestamp;
        this.isSynced = isSynced;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getLoggedBy() { return loggedBy; }
    public void setLoggedBy(String loggedBy) { this.loggedBy = loggedBy; }

    public String getLocationOrSupplier() { return locationOrSupplier; }
    public void setLocationOrSupplier(String locationOrSupplier) { this.locationOrSupplier = locationOrSupplier; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
}
