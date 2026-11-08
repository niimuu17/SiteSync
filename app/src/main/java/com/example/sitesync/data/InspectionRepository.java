package com.example.sitesync.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sitesync.models.InspectionChecklistItem;
import com.example.sitesync.models.InspectionRecord;
import com.example.sitesync.models.SiteInspection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Repository providing local persistence for Site Quality Control Inspections,
 * checklists, and re-inspection audit history using SharedPreferences JSON storage.
 */
public class InspectionRepository {

    private static final String PREF_NAME = "SiteSync_Inspection_Prefs";
    private static final String KEY_INSPECTIONS = "key_site_inspections";

    private final SharedPreferences preferences;

    public InspectionRepository(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureInitialData();
    }

    public List<SiteInspection> getAllInspections() {
        List<SiteInspection> list = new ArrayList<>();
        String json = preferences.getString(KEY_INSPECTIONS, null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    list.add(SiteInspection.fromJSONObject(array.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public SiteInspection getInspectionById(String id) {
        if (id == null) return null;
        for (SiteInspection item : getAllInspections()) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    public void addInspection(SiteInspection inspection) {
        List<SiteInspection> list = getAllInspections();
        list.add(0, inspection);
        saveInspections(list);
    }

    public boolean updateInspection(SiteInspection updatedInspection) {
        List<SiteInspection> list = getAllInspections();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(updatedInspection.getId())) {
                list.set(i, updatedInspection);
                found = true;
                break;
            }
        }
        if (found) {
            saveInspections(list);
        }
        return found;
    }

    /**
     * Conducts and logs a formal re-inspection on an existing site inspection.
     * Updates current status and appends a new audit record to the inspection's history.
     */
    public boolean conductReInspection(String inspectionId, String newStatus,
                                       List<InspectionChecklistItem> updatedChecklist,
                                       String observations, String remarks,
                                       String photoUri, String inspectorName) {
        List<SiteInspection> list = getAllInspections();
        String timestamp = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());

        for (int i = 0; i < list.size(); i++) {
            SiteInspection inspection = list.get(i);
            if (inspection.getId().equals(inspectionId)) {
                int attemptNumber = inspection.getHistory().size() + 1;

                // Update current state
                inspection.setStatus(newStatus);
                inspection.setDate(timestamp);
                inspection.setInspectorName(inspectorName);
                if (observations != null && !observations.isEmpty()) {
                    inspection.setObservations(observations);
                }
                if (remarks != null && !remarks.isEmpty()) {
                    inspection.setRemarks(remarks);
                }
                if (photoUri != null && !photoUri.isEmpty()) {
                    inspection.setPhotoUri(photoUri);
                }

                // Update checklist items if provided
                if (updatedChecklist != null) {
                    inspection.getChecklist().clear();
                    inspection.getChecklist().addAll(updatedChecklist);
                }

                // Checklist summary calculation
                int passedCount = inspection.getPassedChecklistCount();
                int totalCount = inspection.getTotalChecklistCount();
                String summary = "Checklist: " + passedCount + "/" + totalCount + " items verified";

                // Append re-inspection history record
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

                list.set(i, inspection);
                saveInspections(list);
                return true;
            }
        }
        return false;
    }

    private void saveInspections(List<SiteInspection> list) {
        try {
            JSONArray array = new JSONArray();
            for (SiteInspection item : list) {
                array.put(item.toJSONObject());
            }
            preferences.edit().putString(KEY_INSPECTIONS, array.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void ensureInitialData() {
        if (!preferences.contains(KEY_INSPECTIONS)) {
            List<SiteInspection> initial = new ArrayList<>();

            // 1. Passed Inspection
            List<InspectionChecklistItem> check1 = new ArrayList<>();
            check1.add(new InspectionChecklistItem("C-1", "Rebar spacing and diameter match structural drawings", true, "Grade 60 steel verified"));
            check1.add(new InspectionChecklistItem("C-2", "Concrete cover blocks (50mm) positioned and tied", true, "Uniform cover depth"));
            check1.add(new InspectionChecklistItem("C-3", "Formwork properly braced and sealed against slurry leakage", true, "Heavy-duty steel props locked"));
            check1.add(new InspectionChecklistItem("C-4", "Slump cone test equipment ready on site (120mm target)", true, "Calibrated cone ready"));

            List<InspectionRecord> hist1 = new ArrayList<>();
            hist1.add(new InspectionRecord(
                    "REC-101", 1, "Nov 05, 2026 - 10:30 AM", "Elena Rostova (PE)",
                    SiteInspection.STATUS_PASSED,
                    "All rebar lap lengths certified. Sub-grade vapor barrier intact.",
                    "Approved for continuous 300 m³ pour. Batching plant notified.",
                    "", "Checklist: 4/4 items verified"
            ));

            initial.add(new SiteInspection(
                    "INSP-101",
                    "Metro Tower Phase 2",
                    "Basement Level 2 - East Raft Section",
                    "Foundation Concrete Pre-Pour Audit",
                    "Nov 05, 2026 - 10:30 AM",
                    SiteInspection.STATUS_PASSED,
                    "All rebar lap lengths certified. Sub-grade vapor barrier intact.",
                    "Approved for continuous 300 m³ pour. Batching plant notified.",
                    "Elena Rostova (PE)",
                    "",
                    check1,
                    hist1
            ));

            // 2. Failed Inspection (Candidate for Re-Inspection!)
            List<InspectionChecklistItem> check2 = new ArrayList<>();
            check2.add(new InspectionChecklistItem("C-21", "Weld penetration test visually verified", true, "Fillet weld sound"));
            check2.add(new InspectionChecklistItem("C-22", "High-strength friction grip (HSFG) bolts torque checked (min 450 Nm)", false, "Flange bolts under-torqued (380 Nm)"));
            check2.add(new InspectionChecklistItem("C-23", "Splice plates clean of rust, scale, and oil", true, "Surface prep satisfactory"));
            check2.add(new InspectionChecklistItem("C-24", "Column vertical plumbness within ±3mm tolerance", true, "Theodolite check passed"));

            List<InspectionRecord> hist2 = new ArrayList<>();
            hist2.add(new InspectionRecord(
                    "REC-201", 1, "Nov 06, 2026 - 09:15 AM", "Elena Rostova (PE)",
                    SiteInspection.STATUS_FAILED,
                    "Bolts 4 and 6 on West flange torqued to only 380 Nm. Torque wrench requires recalibration.",
                    "FAILED - Retorque with calibrated torque wrench and call for Re-Inspection before deck slab placement.",
                    "", "Checklist: 3/4 items verified (1 Failed)"
            ));

            initial.add(new SiteInspection(
                    "INSP-102",
                    "Metro Tower Phase 2",
                    "Level 4 - Column Joint C-18",
                    "Structural Steel Welding & Bolt Torque Audit",
                    "Nov 06, 2026 - 09:15 AM",
                    SiteInspection.STATUS_FAILED,
                    "Bolts 4 and 6 on West flange torqued to only 380 Nm. Torque wrench requires recalibration.",
                    "FAILED - Retorque with calibrated torque wrench and call for Re-Inspection before deck slab placement.",
                    "Elena Rostova (PE)",
                    "",
                    check2,
                    hist2
            ));

            // 3. Pending Inspection
            List<InspectionChecklistItem> check3 = new ArrayList<>();
            check3.add(new InspectionChecklistItem("C-31", "Intumescent fire collars fitted to PVC pipes", true, "Certified collars attached"));
            check3.add(new InspectionChecklistItem("C-32", "Mineral wool packing density conforms to 2-hour rating", false, "Pending supplier cert"));
            check3.add(new InspectionChecklistItem("C-33", "Acoustic and smoke sealant applied smoothly", false, "Application in progress"));

            List<InspectionRecord> hist3 = new ArrayList<>();
            hist3.add(new InspectionRecord(
                    "REC-301", 1, "Nov 06, 2026 - 02:00 PM", "Elena Rostova (PE)",
                    SiteInspection.STATUS_PENDING,
                    "Awaiting delivery of certified fire sealant cartridges from supplier.",
                    "Scheduled for final sign-off tomorrow morning.",
                    "", "Checklist: 1/3 items verified"
            ));

            initial.add(new SiteInspection(
                    "INSP-103",
                    "Metro Tower Phase 2",
                    "Level 3 - Service Duct Riser",
                    "Firestopping & Penetration Seal Inspection",
                    "Nov 06, 2026 - 02:00 PM",
                    SiteInspection.STATUS_PENDING,
                    "Awaiting delivery of certified fire sealant cartridges from supplier.",
                    "Scheduled for final sign-off tomorrow morning.",
                    "Elena Rostova (PE)",
                    "",
                    check3,
                    hist3
            ));

            saveInspections(initial);
        }
    }
}
