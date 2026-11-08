package com.example.sitesync.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.DailyReportDao;
import com.example.sitesync.data.local.DailyReportEntity;
import com.example.sitesync.models.DailySiteReport;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Single-Source-of-Truth Repository for Daily Site Reports (DSR).
 * Backed by Room Database local cache and Cloud Firestore real-time snapshot sync.
 */
public class DailyReportRepository {

    private static final String TAG = "DailyReportRepository";
    private static final String COLLECTION_REPORTS = "daily_reports";

    private final DailyReportDao dailyReportDao;
    private final FirebaseFirestore firestore;

    public DailyReportRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.dailyReportDao = db.dailyReportDao();
        this.firestore = FirebaseSyncManager.getInstance(context).getFirestore();

        ensureInitialData();
        attachFirestoreSnapshotListener();
    }

    private void attachFirestoreSnapshotListener() {
        if (firestore == null) return;
        firestore.collection(COLLECTION_REPORTS)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Firestore daily reports listen error: " + e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            List<DailyReportEntity> remoteEntities = new ArrayList<>();
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String rawJson = doc.getString("rawJson");
                                if (rawJson != null && !rawJson.isEmpty()) {
                                    try {
                                        DailySiteReport report = DailySiteReport.fromJSONObject(new JSONObject(rawJson));
                                        remoteEntities.add(DailyReportEntity.fromModel(report));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            if (!remoteEntities.isEmpty()) {
                                dailyReportDao.insertAll(remoteEntities);
                            }
                        });
                    }
                });
    }

    public LiveData<List<DailySiteReport>> getAllReportsLive() {
        return Transformations.map(dailyReportDao.getAllLive(), entities -> {
            List<DailySiteReport> list = new ArrayList<>();
            for (DailyReportEntity e : entities) {
                list.add(e.toModel());
            }
            return list;
        });
    }

    public List<DailySiteReport> getAllReports() {
        List<DailyReportEntity> entities = dailyReportDao.getAllSync();
        List<DailySiteReport> list = new ArrayList<>();
        for (DailyReportEntity e : entities) {
            list.add(e.toModel());
        }
        return list;
    }

    public DailySiteReport getReportById(String id) {
        if (id == null) return null;
        DailyReportEntity entity = dailyReportDao.getById(id);
        return entity != null ? entity.toModel() : null;
    }

    public List<DailySiteReport> getReportsByStatus(String status) {
        if (status == null || status.equalsIgnoreCase("All")) {
            return getAllReports();
        }
        List<DailySiteReport> filtered = new ArrayList<>();
        for (DailySiteReport report : getAllReports()) {
            if (status.equalsIgnoreCase(report.getStatus())) {
                filtered.add(report);
            }
        }
        return filtered;
    }

    public boolean addReport(DailySiteReport report) {
        if (report == null || report.getId() == null) return false;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            dailyReportDao.insertOrUpdate(DailyReportEntity.fromModel(report));
        });

        if (firestore != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", report.getId());
            data.put("projectName", report.getProjectName());
            data.put("reportDate", report.getReportDate());
            data.put("preparedBy", report.getPreparedBy());
            data.put("status", report.getStatus());
            try {
                data.put("rawJson", report.toJSONObject().toString());
            } catch (Exception ignored) {
            }
            data.put("lastUpdated", System.currentTimeMillis());

            firestore.collection(COLLECTION_REPORTS)
                    .document(report.getId())
                    .set(data);
        }
        return true;
    }

    public void saveReport(DailySiteReport report) {
        addReport(report);
    }

    public boolean updateReport(DailySiteReport report) {
        return addReport(report);
    }

    public boolean submitReport(String reportId, String submittedAt) {
        DailySiteReport report = getReportById(reportId);
        if (report == null) return false;
        report.setStatus(DailySiteReport.STATUS_SUBMITTED);
        report.setSubmittedAt(submittedAt != null ? submittedAt : "Submitted");
        return addReport(report);
    }

    public boolean deleteReport(String id) {
        if (id == null) return false;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dailyReportDao.deleteById(id);
        });
        if (firestore != null) {
            firestore.collection(COLLECTION_REPORTS).document(id).delete();
        }
        return true;
    }

    private void ensureInitialData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<DailyReportEntity> existing = dailyReportDao.getAllSync();
            if (existing == null || existing.isEmpty()) {
                List<DailyReportEntity> initial = new ArrayList<>();

                // Seed 1: DSR-101
                DailySiteReport r1 = new DailySiteReport(
                        "DSR-101", "Metro Central Tower", "Nov 06, 2026", "Alex Chen (PE)",
                        DailySiteReport.STATUS_SUBMITTED, "Clear • 22°C • Wind 8 km/h",
                        "Completed column alignment on core grid 4-8. Concrete cure test 32 MPa verified.",
                        "48 On-Site Workers (12 Steel Erectors, 14 Rebar Fixers, 8 Masons, 14 Helpers)",
                        "Ready-mix C35 concrete: 42 cu m | Grade 60 Rebar: 18 tons",
                        "1x Tower Crane 50t, 2x Concrete Boom Pumps, 1x Scissor Lift",
                        "Minor material delivery delay due to gate congestion (30 mins). Resolved.",
                        "", "Erect scaffolding on Level 15 east facade.", "Nov 06, 2026", "Nov 06, 2026 05:45 PM"
                );
                initial.add(DailyReportEntity.fromModel(r1));

                // Seed 2: DSR-102
                DailySiteReport r2 = new DailySiteReport(
                        "DSR-102", "Metro Central Tower", "Nov 07, 2026", "Alex Chen (PE)",
                        DailySiteReport.STATUS_DRAFT, "Partly Cloudy • 19°C",
                        "Sleeve penetrations marked for HVAC riser shafts on levels 1 to 3.",
                        "36 On-Site Workers", "Galvanized sheet metal ducts, fire collars",
                        "1x Tower Crane, 2x Core Drill Machines",
                        "Awaiting final approved revision 4.8.2 before drilling.",
                        "", "Begin coring once consultant approval is signed.", "Nov 07, 2026", ""
                );
                initial.add(DailyReportEntity.fromModel(r2));

                dailyReportDao.insertAll(initial);
            }
        });
    }
}
