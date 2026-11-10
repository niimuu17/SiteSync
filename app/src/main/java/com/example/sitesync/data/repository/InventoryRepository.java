package com.example.sitesync.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.dao.InventoryItemDao;
import com.example.sitesync.data.local.dao.RecordedReceiptDao;
import com.example.sitesync.data.local.dao.StockTransactionDao;
import com.example.sitesync.data.local.entities.InventoryItem;
import com.example.sitesync.data.local.entities.RecordedReceipt;
import com.example.sitesync.data.local.entities.StockTransaction;
import com.example.sitesync.hardware.ChallanOcrVisionClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enterprise Repository managing Inventory Items and Stock Transactions.
 * Ensures ACID compliance with atomic Room transactions for stock ins, outs, and challan deliveries.
 */
public class InventoryRepository {

    private static volatile InventoryRepository INSTANCE;
    private final AppDatabase db;
    private final InventoryItemDao itemDao;
    private final StockTransactionDao transactionDao;
    private final RecordedReceiptDao recordedReceiptDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public static class DuplicateCheckResult {
        public final boolean isDuplicate;
        public final String reason;
        public final String matchedChallanNumber;
        public final String matchedSupplier;
        public final long matchedRecordedAt;
        public final double matchedAmount;

        public DuplicateCheckResult(boolean isDuplicate, String reason, String matchedChallanNumber,
                                    String matchedSupplier, long matchedRecordedAt, double matchedAmount) {
            this.isDuplicate = isDuplicate;
            this.reason = reason;
            this.matchedChallanNumber = matchedChallanNumber;
            this.matchedSupplier = matchedSupplier;
            this.matchedRecordedAt = matchedRecordedAt;
            this.matchedAmount = matchedAmount;
        }

        public static DuplicateCheckResult unique() {
            return new DuplicateCheckResult(false, null, null, null, 0, 0);
        }

        public static DuplicateCheckResult duplicate(String reason, String challanNo, String supplier, long recordedAt, double amount) {
            return new DuplicateCheckResult(true, reason, challanNo, supplier, recordedAt, amount);
        }
    }

    public interface ActionCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    private InventoryRepository(Context context) {
        this.db = AppDatabase.getDatabase(context);
        this.itemDao = db.inventoryItemDao();
        this.transactionDao = db.stockTransactionDao();
        this.recordedReceiptDao = db.recordedReceiptDao();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static InventoryRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (InventoryRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InventoryRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<InventoryItem>> getAllItems() {
        return itemDao.getAllItemsLiveData();
    }

    public LiveData<List<InventoryItem>> getItemsForProject(String projectCode) {
        String pCode = (projectCode != null && !projectCode.isEmpty()) ? projectCode : "PRJ-DHAKA-101";
        return itemDao.getItemsForProjectLiveData(pCode);
    }

    public LiveData<List<StockTransaction>> getAllTransactions() {
        return transactionDao.getAllTransactionsLiveData();
    }

    public LiveData<List<StockTransaction>> getTransactionsForProject(String projectCode) {
        String pCode = (projectCode != null && !projectCode.isEmpty()) ? projectCode : "PRJ-DHAKA-101";
        return transactionDao.getTransactionsForProjectLiveData(pCode);
    }

    public LiveData<List<com.example.sitesync.data.local.entities.RequiredMaterialItemEntity>> getPendingRequiredMaterials(String projectCode) {
        String pCode = (projectCode != null && !projectCode.isEmpty()) ? projectCode : "PRJ-DHAKA-101";
        return db.requiredMaterialItemDao().getPendingItemsForProjectLive(pCode);
    }

    public LiveData<List<StockTransaction>> getTransactionsByType(String type) {
        return transactionDao.getTransactionsByTypeLiveData(type);
    }

    /**
     * Seeds initial standard Bangladesh construction materials if inventory is empty.
     */
    public void seedDefaultMaterialsIfEmpty(ActionCallback<Boolean> callback) {
        seedDefaultMaterialsForProjectIfEmpty("PRJ-DHAKA-101", callback);
    }

    public void seedDefaultMaterialsForProjectIfEmpty(String projectCode, ActionCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String pCode = (projectCode != null && !projectCode.isEmpty()) ? projectCode : "PRJ-DHAKA-101";
                if (itemDao.getItemCountForProject(pCode) > 0) {
                    deliverSuccess(callback, false);
                    return;
                }

                long now = System.currentTimeMillis();
                long oneDayAgo = now - 86400000L;
                long twoDaysAgo = now - 172800000L;
                long threeDaysAgo = now - 259200000L;

                List<InventoryItem> items = new ArrayList<>();
                items.add(new InventoryItem(pCode, "BSRM 500W Deformed Bar (16mm)", "STEEL", "TON", 35.0, 12.0, 92000.0, 5.0, now));
                items.add(new InventoryItem(pCode, "Shah Cement Special (Composite)", "CEMENT", "BAG", 850.0, 420.0, 560.0, 150.0, now));
                items.add(new InventoryItem(pCode, "Sylhet Sand (2.5 FM Coarse)", "AGGREGATE_SAND", "CFT", 4500.0, 2100.0, 48.0, 600.0, now));
                items.add(new InventoryItem(pCode, "Crushed Stone Aggregate (20mm)", "AGGREGATE_SAND", "CFT", 3200.0, 1400.0, 230.0, 500.0, now));
                items.add(new InventoryItem(pCode, "First Class Auto Bricks (Gas Burned)", "BRICK", "PCS", 22000.0, 9500.0, 14.0, 3000.0, now));

                itemDao.insertAll(items);

                // Re-fetch inserted items to acquire generated IDs for transaction seeding
                List<InventoryItem> inserted = itemDao.getItemsForProjectSync(pCode);
                List<StockTransaction> transactions = new ArrayList<>();

                for (InventoryItem item : inserted) {
                    // Seed initial delivery transaction
                    transactions.add(new StockTransaction(
                            item.getId(),
                            item.getItemName(),
                            pCode,
                            StockTransaction.TYPE_STOCK_IN,
                            item.getTotalStockIn(),
                            item.getUnit(),
                            item.getAvgUnitPrice(),
                            item.getTotalStockIn() * item.getAvgUnitPrice(),
                            "CH-INIT-" + item.getId(),
                            "Apex Construction Supplies Ltd",
                            threeDaysAgo,
                            "Initial Foundation Material Delivery"
                    ));

                    // Seed consumption transaction
                    if (item.getTotalStockOut() > 0) {
                        transactions.add(new StockTransaction(
                                item.getId(),
                                item.getItemName(),
                                pCode,
                                StockTransaction.TYPE_STOCK_OUT,
                                item.getTotalStockOut(),
                                item.getUnit(),
                                item.getAvgUnitPrice(),
                                item.getTotalStockOut() * item.getAvgUnitPrice(),
                                "",
                                "Site Eng. Elena / Substructure Team",
                                oneDayAgo,
                                "Used in basement slab and column casting"
                        ));
                    }
                }

                transactionDao.insertAll(transactions);
                deliverSuccess(callback, true);

            } catch (Exception e) {
                deliverError(callback, e);
            }
        });
    }

    /**
     * Atomically records a single stock-in movement.
     */
    public void recordStockIn(long itemId, String itemName, String category, String unit,
                              String projectId, double quantity, double unitPrice,
                              String challanNumber, String supplier, String notes,
                              ActionCallback<StockTransaction> callback) {
        executor.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    InventoryItem item = null;
                    if (itemId > 0) {
                        item = itemDao.getItemById(itemId);
                    }
                    if (item == null && itemName != null) {
                        item = itemDao.getItemByName(itemName);
                    }

                    long now = System.currentTimeMillis();

                    if (item == null) {
                        item = new InventoryItem(itemName, category != null ? category : "GENERAL",
                                unit != null ? unit : "UNIT", quantity, 0, unitPrice, 10.0, now);
                        long newId = itemDao.insert(item);
                        item.setId(newId);
                    } else {
                        double oldStockIn = item.getTotalStockIn();
                        double oldUnitPrice = item.getAvgUnitPrice();
                        double newStockIn = oldStockIn + quantity;

                        // Weighted average unit price
                        double newAvgPrice = ((oldStockIn * oldUnitPrice) + (quantity * unitPrice)) / (newStockIn > 0 ? newStockIn : 1.0);
                        item.setTotalStockIn(newStockIn);
                        item.setAvgUnitPrice(newAvgPrice);
                        item.setLastUpdated(now);
                        itemDao.update(item);
                    }

                    StockTransaction tx = new StockTransaction(
                            item.getId(),
                            item.getItemName(),
                            projectId != null ? projectId : "Primary Site",
                            StockTransaction.TYPE_STOCK_IN,
                            quantity,
                            item.getUnit(),
                            unitPrice,
                            quantity * unitPrice,
                            challanNumber != null ? challanNumber : "",
                            supplier != null ? supplier : "Direct Vendor",
                            now,
                            notes != null ? notes : "Delivery Received & Verified"
                    );

                    long txId = transactionDao.insert(tx);
                    tx.setId(txId);
                    deliverSuccess(callback, tx);
                });
            } catch (Exception e) {
                deliverError(callback, e);
            }
        });
    }

    /**
     * Atomically records a single stock-out movement.
     */
    public void recordStockOut(long itemId, String projectId, double quantity,
                               String receiver, String notes,
                               ActionCallback<StockTransaction> callback) {
        executor.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    InventoryItem item = itemDao.getItemById(itemId);
                    if (item == null) {
                        throw new IllegalArgumentException("Inventory item with ID " + itemId + " not found");
                    }

                    if (quantity > item.getCurrentBalance()) {
                        throw new IllegalStateException("Requested stock-out (" + quantity + " " + item.getUnit()
                                + ") exceeds available balance (" + item.getCurrentBalance() + " " + item.getUnit() + ")");
                    }

                    long now = System.currentTimeMillis();
                    item.setTotalStockOut(item.getTotalStockOut() + quantity);
                    item.setLastUpdated(now);
                    itemDao.update(item);

                    StockTransaction tx = new StockTransaction(
                            item.getId(),
                            item.getItemName(),
                            projectId != null ? projectId : "Primary Site",
                            StockTransaction.TYPE_STOCK_OUT,
                            quantity,
                            item.getUnit(),
                            item.getAvgUnitPrice(),
                            quantity * item.getAvgUnitPrice(),
                            "",
                            receiver != null ? receiver : "Site Work Crew",
                            now,
                            notes != null ? notes : "Dispatched for construction works"
                    );

                    long txId = transactionDao.insert(tx);
                    tx.setId(txId);
                    deliverSuccess(callback, tx);
                });
            } catch (Exception e) {
                deliverError(callback, e);
            }
        });
    }

    /**
     * Checks if a receipt/challan has already been entered into the inventory ledger.
     * Prevents double entry by matching Challan Number, Receipt Content Fingerprint, or Image Hash.
     */
    public void checkDuplicateReceipt(String challanNumber, String fingerprint, String imageHash,
                                      ActionCallback<DuplicateCheckResult> callback) {
        executor.execute(() -> {
            try {
                String ch = challanNumber != null ? challanNumber.trim() : "";
                String fp = fingerprint != null ? fingerprint.trim() : "";
                String img = imageHash != null ? imageHash.trim() : "";

                // 1. Check RecordedReceipt table
                RecordedReceipt duplicate = recordedReceiptDao.findDuplicate(ch, fp, img);
                if (duplicate != null) {
                    String reason = "Challan #" + duplicate.getChallanNumber()
                            + " (" + duplicate.getSupplierName() + ") was already entered into inventory on "
                            + duplicate.getFormattedRecordedDate();
                    deliverSuccess(callback, DuplicateCheckResult.duplicate(
                            reason, duplicate.getChallanNumber(), duplicate.getSupplierName(),
                            duplicate.getRecordedAt(), duplicate.getTotalAmount()));
                    return;
                }

                // 2. Check StockTransaction table for legacy/manually recorded challan numbers
                if (!ch.isEmpty()) {
                    StockTransaction existingTx = transactionDao.findTransactionByChallan(ch);
                    if (existingTx != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US);
                        String dateStr = sdf.format(new Date(existingTx.getTimestamp()));
                        String reason = "Challan #" + existingTx.getChallanNumber()
                                + " is already recorded in the stock ledger (Recorded: " + dateStr + ").";
                        deliverSuccess(callback, DuplicateCheckResult.duplicate(
                                reason, existingTx.getChallanNumber(), existingTx.getSupplierOrReceiver(),
                                existingTx.getTimestamp(), existingTx.getTotalCost()));
                        return;
                    }
                }

                deliverSuccess(callback, DuplicateCheckResult.unique());
            } catch (Exception e) {
                deliverError(callback, e);
            }
        });
    }

    /**
     * Backward-compatible overload for recordChallanDelivery.
     */
    public void recordChallanDelivery(String projectId, String supplier, String challanNumber,
                                      long timestamp, List<ChallanOcrVisionClient.ChallanItem> items,
                                      ActionCallback<Integer> callback) {
        recordChallanDelivery(projectId, supplier, challanNumber, null, null, null, timestamp, items, callback);
    }

    /**
     * Atomically commits a parsed delivery challan into inventory items and ledger.
     * Enforces strict deduplication: aborts with an exception if this receipt was already entered.
     */
    public void recordChallanDelivery(String projectId, String supplier, String challanNumber,
                                      String receiptDate, String fingerprint, String imageHash,
                                      long timestamp, List<ChallanOcrVisionClient.ChallanItem> items,
                                      ActionCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    long txTime = timestamp > 0 ? timestamp : System.currentTimeMillis();
                    String effChallan = challanNumber != null ? challanNumber.trim() : "";
                    String effFp = fingerprint != null ? fingerprint.trim() : "";
                    String effImg = imageHash != null ? imageHash.trim() : "";

                    // Strict Deduplication Check: Prevent Double Entry
                    RecordedReceipt existing = recordedReceiptDao.findDuplicate(effChallan, effFp, effImg);
                    if (existing != null) {
                        throw new IllegalStateException("Double entry blocked: Challan #" + existing.getChallanNumber()
                                + " has already been recorded in inventory on " + existing.getFormattedRecordedDate());
                    }

                    if (!effChallan.isEmpty()) {
                        StockTransaction txMatch = transactionDao.findTransactionByChallan(effChallan);
                        if (txMatch != null) {
                            throw new IllegalStateException("Double entry blocked: Challan #" + effChallan
                                    + " already exists in stock transaction ledger.");
                        }
                    }

                    int insertedCount = 0;
                    double totalBilledVal = 0.0;

                    String targetProject = (projectId != null && !projectId.isEmpty()) ? projectId : "PRJ-DHAKA-101";

                    for (ChallanOcrVisionClient.ChallanItem ci : items) {
                        totalBilledVal += ci.totalPrice;
                        InventoryItem item = itemDao.findBestMatchItem(targetProject, ci.materialName);

                        if (item == null) {
                            item = new InventoryItem(
                                    targetProject,
                                    ci.materialName,
                                    ci.category != null ? ci.category : "GENERAL",
                                    ci.unit != null ? ci.unit : "UNIT",
                                    ci.quantity,
                                    0.0,
                                    ci.unitPrice,
                                    ci.quantity * 0.2, // Default reorder level
                                    txTime
                            );
                            if (ci.imageUrl != null) {
                                item.setImageUrl(ci.imageUrl);
                            }
                            long newId = itemDao.insert(item);
                            item.setId(newId);
                        } else {
                            double oldStockIn = item.getTotalStockIn();
                            double oldUnitPrice = item.getAvgUnitPrice();
                            double newStockIn = oldStockIn + ci.quantity;
                            double newAvgPrice = ((oldStockIn * oldUnitPrice) + (ci.quantity * ci.unitPrice)) / (newStockIn > 0 ? newStockIn : 1.0);

                            item.setTotalStockIn(newStockIn);
                            item.setAvgUnitPrice(newAvgPrice);
                            item.setLastUpdated(txTime);
                            itemDao.update(item);
                        }

                        StockTransaction tx = new StockTransaction(
                                item.getId(),
                                item.getItemName(),
                                targetProject,
                                StockTransaction.TYPE_STOCK_IN,
                                ci.quantity,
                                item.getUnit(),
                                ci.unitPrice,
                                ci.totalPrice,
                                effChallan,
                                supplier != null ? supplier : "Delivery Supplier",
                                txTime,
                                "Parsed from Challan OCR / Vision AI"
                        );

                        transactionDao.insert(tx);
                        insertedCount++;

                        // Automatic Fulfillment: If this item was requested in the chat, mark as fulfilled!
                        try {
                            db.requiredMaterialItemDao().fulfillMatchingItem(targetProject, ci.materialName, effChallan, txTime);
                        } catch (Exception ex) {
                            Log.w("InventoryRepo", "Fulfillment check note: " + ex.getMessage());
                        }
                    }

                    // Record the receipt to permanently prevent double entry
                    String effectiveDate = receiptDate != null ? receiptDate :
                            new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(txTime));
                    RecordedReceipt receipt = new RecordedReceipt(
                            effChallan,
                            supplier != null ? supplier : "Supplier",
                            projectId != null ? projectId : "Primary Site",
                            effectiveDate,
                            totalBilledVal,
                            items.size(),
                            txTime,
                            effFp,
                            effImg,
                            "Verified via Challan OCR"
                    );
                    recordedReceiptDao.insert(receipt);

                    deliverSuccess(callback, insertedCount);
                });
            } catch (Exception e) {
                deliverError(callback, e);
            }
        });
    }

    public void getItemValuationSummary(ActionCallback<ValuationSummary> callback) {
        executor.execute(() -> {
            try {
                Double totalValuation = itemDao.getTotalValuation();
                Double stockInCost = transactionDao.getTotalStockInCost();
                Double stockOutCost = transactionDao.getTotalStockOutCost();
                List<InventoryItem> lowStock = itemDao.getLowStockItems();

                ValuationSummary summary = new ValuationSummary(
                        totalValuation != null ? totalValuation : 0.0,
                        stockInCost != null ? stockInCost : 0.0,
                        stockOutCost != null ? stockOutCost : 0.0,
                        lowStock != null ? lowStock.size() : 0
                );
                deliverSuccess(callback, summary);
            } catch (Exception e) {
                deliverError(callback, e);
            }
        });
    }

    public static class ValuationSummary {
        public final double totalValuation;
        public final double totalStockInCost;
        public final double totalStockOutCost;
        public final int lowStockCount;

        public ValuationSummary(double totalValuation, double totalStockInCost, double totalStockOutCost, int lowStockCount) {
            this.totalValuation = totalValuation;
            this.totalStockInCost = totalStockInCost;
            this.totalStockOutCost = totalStockOutCost;
            this.lowStockCount = lowStockCount;
        }
    }

    private <T> void deliverSuccess(ActionCallback<T> callback, T result) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(result));
        }
    }

    private <T> void deliverError(ActionCallback<T> callback, Exception e) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(e));
        }
    }
}
