package com.example.sitesync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inventory")
public class InventoryItemEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String category; // "CEMENT", "STEEL", "AGGREGATE", "CHEMICALS"
    private double currentStock;
    private double minThreshold;
    private String unit;     // "Bags", "Tons", "cft", "Liters"
    private long lastUpdated;
    private boolean isSynced;

    public InventoryItemEntity(@NonNull String id, String name, String category, double currentStock,
                               double minThreshold, String unit, long lastUpdated, boolean isSynced) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.currentStock = currentStock;
        this.minThreshold = minThreshold;
        this.unit = unit;
        this.lastUpdated = lastUpdated;
        this.isSynced = isSynced;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getCurrentStock() { return currentStock; }
    public void setCurrentStock(double currentStock) { this.currentStock = currentStock; }

    public double getMinThreshold() { return minThreshold; }
    public void setMinThreshold(double minThreshold) { this.minThreshold = minThreshold; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public int getStockPercentage() {
        if (minThreshold <= 0) return 100;
        return (int) Math.round((currentStock / minThreshold) * 100.0);
    }

    public boolean isLowStock() {
        return currentStock < minThreshold;
    }
}
