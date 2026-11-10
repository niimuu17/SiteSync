package com.example.sitesync.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Entity tracking recorded delivery receipts, challans, and cash memos.
 * Used to enforce strict deduplication and prevent double entry of materials into inventory.
 */
@Entity(
        tableName = "recorded_receipts",
        indices = {
                @Index(value = {"challanNumber"}, unique = false),
                @Index(value = {"receiptFingerprint"}, unique = false),
                @Index(value = {"imageHash"}, unique = false)
        }
)
public class RecordedReceipt {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String challanNumber;
    private String supplierName;
    private String projectId;
    private String receiptDate;
    private double totalAmount;
    private int itemCount;
    private long recordedAt;
    private String receiptFingerprint;
    private String imageHash;
    private String notes;

    public RecordedReceipt() {
    }

    @Ignore
    public RecordedReceipt(String challanNumber, String supplierName, String projectId,
                           String receiptDate, double totalAmount, int itemCount,
                           long recordedAt, String receiptFingerprint, String imageHash, String notes) {
        this.challanNumber = challanNumber != null ? challanNumber.trim() : "";
        this.supplierName = supplierName != null ? supplierName.trim() : "";
        this.projectId = projectId != null ? projectId : "Primary Site";
        this.receiptDate = receiptDate;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
        this.recordedAt = recordedAt > 0 ? recordedAt : System.currentTimeMillis();
        this.receiptFingerprint = receiptFingerprint;
        this.imageHash = imageHash;
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getChallanNumber() {
        return challanNumber;
    }

    public void setChallanNumber(String challanNumber) {
        this.challanNumber = challanNumber;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(String receiptDate) {
        this.receiptDate = receiptDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public long getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(long recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getReceiptFingerprint() {
        return receiptFingerprint;
    }

    public void setReceiptFingerprint(String receiptFingerprint) {
        this.receiptFingerprint = receiptFingerprint;
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFormattedRecordedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US);
        return sdf.format(new Date(recordedAt));
    }
}
