package com.example.sitesync.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Detailed ledger entry for stock movements.
 * Captures deliveries (Stock In), site usage/pouring (Stock Out), transfers, and waste.
 */
@Entity(tableName = "stock_transactions")
public class StockTransaction {

    public static final String TYPE_STOCK_IN = "STOCK_IN_CHALLAN";
    public static final String TYPE_STOCK_OUT = "STOCK_OUT_CONSUMPTION";
    public static final String TYPE_TRANSFER = "SITE_TRANSFER";
    public static final String TYPE_WASTE = "DAMAGE_WASTE";

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long itemId;
    private String itemName;
    private String projectId;
    private String type; // STOCK_IN_CHALLAN, STOCK_OUT_CONSUMPTION, SITE_TRANSFER, DAMAGE_WASTE
    private double quantity;
    private String unit;
    private double unitPrice;
    private double totalCost;
    private String challanNumber;
    private String supplierOrReceiver;
    private long timestamp;
    private String notes;

    public StockTransaction() {
    }

    @Ignore
    public StockTransaction(long itemId, String itemName, String projectId, String type,
                            double quantity, String unit, double unitPrice, double totalCost,
                            String challanNumber, String supplierOrReceiver, long timestamp, String notes) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.projectId = projectId;
        this.type = type;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.totalCost = totalCost;
        this.challanNumber = challanNumber;
        this.supplierOrReceiver = supplierOrReceiver;
        this.timestamp = timestamp;
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public String getChallanNumber() {
        return challanNumber;
    }

    public void setChallanNumber(String challanNumber) {
        this.challanNumber = challanNumber;
    }

    public String getSupplierOrReceiver() {
        return supplierOrReceiver;
    }

    public void setSupplierOrReceiver(String supplierOrReceiver) {
        this.supplierOrReceiver = supplierOrReceiver;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isStockIn() {
        return TYPE_STOCK_IN.equalsIgnoreCase(type);
    }
}
