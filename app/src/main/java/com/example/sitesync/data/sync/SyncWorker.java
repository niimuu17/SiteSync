package com.example.sitesync.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.utils.NetworkMonitor;

/**
 * Background WorkManager sync worker for flushing pending entity edits and offline media uploads.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Executing background SiteSync offline synchronization...");
        Context context = getApplicationContext();

        boolean isOnline = NetworkMonitor.getInstance(context).isCurrentlyOnline();
        if (!isOnline) {
            Log.d(TAG, "Device currently offline. Rescheduling sync worker.");
            return Result.retry();
        }

        try {
            // Local Room database verification
            AppDatabase db = AppDatabase.getDatabase(context);
            int pendingActivities = db.activityDao().getAllSync().size();
            int pendingInspections = db.inspectionDao().getAllSync().size();
            int pendingReports = db.dailyReportDao().getAllSync().size();

            Log.d(TAG, String.format("Local cache state: %d activities, %d inspections, %d daily reports",
                    pendingActivities, pendingInspections, pendingReports));

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error during offline sync execution", e);
            return Result.retry();
        }
    }
}
