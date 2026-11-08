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
        com.example.sitesync.data.local.entities.LocalReportEntity.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ActivityDao activityDao();
    public abstract InspectionDao inspectionDao();
    public abstract DocumentDao documentDao();
    public abstract IssueDao issueDao();
    public abstract DailyReportDao dailyReportDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

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
