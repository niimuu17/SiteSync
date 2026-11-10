package com.example.sitesync.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        ActivityEntity.class,
        InspectionEntity.class,
        DocumentEntity.class,
        IssueEntity.class,
        DailyReportEntity.class,
        com.example.sitesync.data.local.entities.LocalActivityEntity.class,
        com.example.sitesync.data.local.entities.LocalInspectionEntity.class,
        com.example.sitesync.data.local.entities.LocalReportEntity.class,
        com.example.sitesync.data.local.entities.TaskEntity.class,
        com.example.sitesync.data.local.entities.InventoryItemEntity.class,
        com.example.sitesync.data.local.entities.InventoryLogEntity.class,
        com.example.sitesync.data.local.entities.InventoryItem.class,
        com.example.sitesync.data.local.entities.StockTransaction.class,
        com.example.sitesync.data.local.entities.RecordedReceipt.class,
        com.example.sitesync.data.local.entities.HazardTicketEntity.class,
        com.example.sitesync.data.local.entities.PendingSyncEntity.class,
        com.example.sitesync.data.local.entities.ProjectEntity.class,
        com.example.sitesync.data.local.entities.WorkerJoinRequestEntity.class,
        com.example.sitesync.data.local.entities.SiteChatMessageEntity.class,
        com.example.sitesync.data.local.entities.SiteEvidencePhotoEntity.class,
        com.example.sitesync.data.local.entities.WorkerAttendanceEntity.class,
        com.example.sitesync.data.local.entities.RequiredMaterialItemEntity.class
}, version = 9, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ActivityDao activityDao();
    public abstract InspectionDao inspectionDao();
    public abstract DocumentDao documentDao();
    public abstract IssueDao issueDao();
    public abstract DailyReportDao dailyReportDao();
    public abstract com.example.sitesync.data.local.dao.TaskDao taskDao();
    public abstract com.example.sitesync.data.local.dao.InventoryDao inventoryDao();
    public abstract com.example.sitesync.data.local.dao.InventoryLogDao inventoryLogDao();
    public abstract com.example.sitesync.data.local.dao.InventoryItemDao inventoryItemDao();
    public abstract com.example.sitesync.data.local.dao.StockTransactionDao stockTransactionDao();
    public abstract com.example.sitesync.data.local.dao.RecordedReceiptDao recordedReceiptDao();
    public abstract com.example.sitesync.data.local.dao.HazardTicketDao hazardTicketDao();
    public abstract com.example.sitesync.data.local.dao.PendingSyncDao pendingSyncDao();
    public abstract com.example.sitesync.data.local.dao.ProjectDao projectDao();
    public abstract com.example.sitesync.data.local.dao.WorkerJoinRequestDao workerJoinRequestDao();
    public abstract com.example.sitesync.data.local.dao.SiteChatMessageDao siteChatMessageDao();
    public abstract com.example.sitesync.data.local.dao.SiteEvidencePhotoDao siteEvidencePhotoDao();
    public abstract com.example.sitesync.data.local.dao.WorkerAttendanceDao workerAttendanceDao();
    public abstract com.example.sitesync.data.local.dao.RequiredMaterialItemDao requiredMaterialItemDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getInstance(final Context context) {
        return getDatabase(context);
    }

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "sitesync_enterprise.db"
                    )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
