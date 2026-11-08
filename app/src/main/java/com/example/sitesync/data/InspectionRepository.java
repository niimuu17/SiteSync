package com.example.sitesync.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.InspectionDao;
import com.example.sitesync.data.local.InspectionEntity;
import com.example.sitesync.models.InspectionChecklistItem;
import com.example.sitesync.models.InspectionRecord;
import com.example.sitesync.models.SiteInspection;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enterprise Single-Source-of-Truth Repository for Site Quality Inspections.
 * Backed by Room Database local cache and Cloud Firestore real-time snapshot sync.
 */
public class InspectionRepository {

    private static final String TAG = "InspectionRepository";
    private static final String COLLECTION_INSPECTIONS = "inspections";

    private final InspectionDao inspectionDao;
    private final FirebaseFirestore firestore;

    public InspectionRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.inspectionDao = db.inspectionDao();
        this.firestore = FirebaseSyncManager.getInstance(context).getFirestore();

        ensureInitialData();
        attachFirestoreSnapshotListener();
    }

    private void attachFirestoreSnapshotListener() {
        if (firestore == null) return;
        firestore.collection(COLLECTION_INSPECTIONS)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Firestore inspections listen error: " + e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            List<InspectionEntity> remoteEntities = new ArrayList<>();
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String rawJson = doc.getString("rawJson");
                                if (rawJson != null && !rawJson.isEmpty()) {
                                    try {
                                        SiteInspection insp = SiteInspection.fromJSONObject(new org.json.JSONObject(rawJson));
                                        remoteEntities.add(InspectionEntity.fromModel(insp));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            if (!remoteEntities.isEmpty()) {
                                inspectionDao.insertAll(remoteEntities);
                            }
                        });
                    }
                });
    }

    public LiveData<List<SiteInspection>> getAllInspectionsLive() {
        return Transformations.map(inspectionDao.getAllLive(), entities -> {
            List<SiteInspection> list = new ArrayList<>();
            for (InspectionEntity e : entities) {
                list.add(e.toModel());
            }
            return list;
        });
    }

    public List<SiteInspection> getAllInspections() {
        List<InspectionEntity> entities = inspectionDao.getAllSync();
        List<SiteInspection> list = new ArrayList<>();
        for (InspectionEntity e : entities) {
            list.add(e.toModel());
        }
        return list;
    }

    public SiteInspection getInspectionById(String id) {
        if (id == null) return null;
        InspectionEntity entity = inspectionDao.getById(id);
        return entity != null ? entity.toModel() : null;
    }

    public boolean addInspection(SiteInspection inspection) {
        if (inspection == null || inspection.getId() == null) return false;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            inspectionDao.insertOrUpdate(InspectionEntity.fromModel(inspection));
        });

        if (firestore != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", inspection.getId());
            data.put("projectName", inspection.getProjectName());
            data.put("location", inspection.getLocation());
            data.put("inspectionType", inspection.getInspectionType());
            data.put("date", inspection.getDate());
            data.put("status", inspection.getStatus());
            data.put("observations", inspection.getObservations());
            data.put("remarks", inspection.getRemarks());
            data.put("inspectorName", inspection.getInspectorName());
            data.put("photoUri", inspection.getPhotoUri());
            try {
                data.put("rawJson", inspection.toJSONObject().toString());
            } catch (Exception ignored) {
            }
            data.put("lastUpdated", System.currentTimeMillis());

            firestore.collection(COLLECTION_INSPECTIONS)
                    .document(inspection.getId())
                    .set(data);
        }
        return true;
    }

    public boolean updateInspection(SiteInspection inspection) {
        return addInspection(inspection);
    }

    public boolean conductReInspection(String inspectionId, String newStatus,
                                       List<InspectionChecklistItem> updatedChecklist,
                                       String observations, String remarks,
                                       String photoUri, String inspectorName) {
        SiteInspection inspection = getInspectionById(inspectionId);
        if (inspection == null) return false;

        String timestamp = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
        int attemptNumber = inspection.getHistory().size() + 1;

        inspection.setStatus(newStatus);
        inspection.setDate(timestamp);
        inspection.setInspectorName(inspectorName != null ? inspectorName : "Field Engineer");
        if (observations != null && !observations.isEmpty()) {
            inspection.setObservations(observations);
        }
        if (remarks != null && !remarks.isEmpty()) {
            inspection.setRemarks(remarks);
        }
        if (photoUri != null && !photoUri.isEmpty()) {
            inspection.setPhotoUri(photoUri);
        }
        if (updatedChecklist != null) {
            inspection.getChecklist().clear();
            inspection.getChecklist().addAll(updatedChecklist);
        }

        int passedCount = inspection.getPassedChecklistCount();
        int totalCount = inspection.getTotalChecklistCount();
        String summary = "Checklist: " + passedCount + "/" + totalCount + " items verified";

        InspectionRecord record = new InspectionRecord(
                "REC-" + System.currentTimeMillis(),
                attemptNumber,
                timestamp,
                inspectorName,
                newStatus,
                observations,
                remarks,
                photoUri,
                summary
        );
        inspection.addHistoryRecord(record);

        return addInspection(inspection);
    }

    private void ensureInitialData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<InspectionEntity> existing = inspectionDao.getAllSync();
            if (existing == null || existing.isEmpty()) {
                List<InspectionEntity> initial = new ArrayList<>();

                // Seed 1: Structural Steel
                List<InspectionChecklistItem> c1 = new ArrayList<>();
                c1.add(new InspectionChecklistItem("c1", "High-strength bolt torque calibrated", true, "320 Nm calibrated"));
                c1.add(new InspectionChecklistItem("c2", "Weld bead ultrasonic testing certified", true, "UT zero defects"));
                c1.add(new InspectionChecklistItem("c3", "Fireproofing paint thickness checked", true, "650 micron dry film"));

                SiteInspection insp1 = new SiteInspection(
                        "INS-101", "Metro Central Tower", "Level 14 • Column Grid C", "Structural Steel",
                        "Nov 06, 2026 - 11:30 AM", SiteInspection.STATUS_PASSED,
                        "Flange connections aligned within 1.5mm tolerance.",
                        "Approved for composite floor deck placement.",
                        "Elena Rostova (PE)", "", c1, null
                );
                initial.add(InspectionEntity.fromModel(insp1));

                // Seed 2: Concrete Pour Slump
                List<InspectionChecklistItem> c2 = new ArrayList<>();
                c2.add(new InspectionChecklistItem("c4", "Slump cone test (120mm target)", true, "115mm recorded"));
                c2.add(new InspectionChecklistItem("c5", "Air content check (4.5%)", true, "4.2% verified"));
                c2.add(new InspectionChecklistItem("c6", "Vibrator compaction coverage", false, "Honeycombing detected on north riser"));

                SiteInspection insp2 = new SiteInspection(
                        "INS-102", "Metro Central Tower", "Basement B2 Shear Wall", "Concrete Pour & Slump",
                        "Nov 05, 2026 - 03:15 PM", SiteInspection.STATUS_FAILED,
                        "Surface voids observed on lower 400mm section.",
                        "Requires mechanical pressure grouting and re-inspection.",
                        "Marcus Vance", "", c2, null
                );
                initial.add(InspectionEntity.fromModel(insp2));

                inspectionDao.insertAll(initial);
            }
        });
    }
}
