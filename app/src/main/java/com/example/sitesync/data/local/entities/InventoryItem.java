package com.example.sitesync.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Enterprise Construction Material Inventory Item.
 * Tracks material balances, categories, units, and reorder levels.
 */
@Entity(tableName = "inventory_items")
public class InventoryItem {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String projectCode; // Unique project scoping
    private String itemName;
    private String category; // "STEEL", "CEMENT", "AGGREGATE_SAND", "BRICK", "FINISHING", "ELECTRICAL"
    private String unit;     // "TON", "BAG", "CFT", "PCS"
    private double totalStockIn;
    private double totalStockOut;
    private double currentBalance;
    private double avgUnitPrice;
    private double reorderLevel;
    private long lastUpdated;
    private String imageUrl;
    private String localThumbnailPath;

    public InventoryItem() {
        this.projectCode = "PRJ-DHAKA-101";
    }

    @Ignore
    public InventoryItem(String projectCode, String itemName, String category, String unit, double totalStockIn,
                         double totalStockOut, double avgUnitPrice, double reorderLevel, long lastUpdated) {
        this.projectCode = projectCode != null && !projectCode.isEmpty() ? projectCode : "PRJ-DHAKA-101";
        this.itemName = itemName;
        this.category = category;
        this.unit = unit;
        this.totalStockIn = totalStockIn;
        this.totalStockOut = totalStockOut;
        this.currentBalance = totalStockIn - totalStockOut;
        this.avgUnitPrice = avgUnitPrice;
        this.reorderLevel = reorderLevel;
        this.lastUpdated = lastUpdated;
    }

    @Ignore
    public InventoryItem(String itemName, String category, String unit, double totalStockIn,
                         double totalStockOut, double avgUnitPrice, double reorderLevel, long lastUpdated) {
        this("PRJ-DHAKA-101", itemName, category, unit, totalStockIn, totalStockOut, avgUnitPrice, reorderLevel, lastUpdated);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getProjectCode() {
        return projectCode != null && !projectCode.isEmpty() ? projectCode : "PRJ-DHAKA-101";
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getTotalStockIn() {
        return totalStockIn;
    }

    public void setTotalStockIn(double totalStockIn) {
        this.totalStockIn = totalStockIn;
        this.currentBalance = this.totalStockIn - this.totalStockOut;
    }

    public double getTotalStockOut() {
        return totalStockOut;
    }

    public void setTotalStockOut(double totalStockOut) {
        this.totalStockOut = totalStockOut;
        this.currentBalance = this.totalStockIn - this.totalStockOut;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public double getAvgUnitPrice() {
        return avgUnitPrice;
    }

    public void setAvgUnitPrice(double avgUnitPrice) {
        this.avgUnitPrice = avgUnitPrice;
    }

    public double getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(double reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isLowStock() {
        return currentBalance <= reorderLevel;
    }

    public double getTotalValuation() {
        return Math.max(0, currentBalance) * avgUnitPrice;
    }

    public double getValuation() {
        return getTotalValuation();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLocalThumbnailPath() {
        return localThumbnailPath;
    }

    public void setLocalThumbnailPath(String localThumbnailPath) {
        this.localThumbnailPath = localThumbnailPath;
    }
}
