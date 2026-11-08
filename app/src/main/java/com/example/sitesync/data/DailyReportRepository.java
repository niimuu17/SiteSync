package com.example.sitesync.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sitesync.models.DailySiteReport;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository providing offline persistence for Daily Site Reports (DSR).
 * Stores drafts, submitted reports, manpower, materials, equipment, and next-day plans.
 */
public class DailyReportRepository {

    private static final String PREF_NAME = "SiteSync_Daily_Report_Prefs";
    private static final String KEY_REPORTS = "key_daily_reports";

    private final SharedPreferences preferences;

    public DailyReportRepository(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureInitialData();
    }

    public List<DailySiteReport> getAllReports() {
        List<DailySiteReport> list = new ArrayList<>();
        String json = preferences.getString(KEY_REPORTS, null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    list.add(DailySiteReport.fromJSONObject(array.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public DailySiteReport getReportById(String id) {
        if (id == null) return null;
        for (DailySiteReport report : getAllReports()) {
            if (id.equals(report.getId())) {
                return report;
            }
        }
        return null;
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

    public List<DailySiteReport> searchReports(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllReports();
        }
        String lowerQuery = query.toLowerCase().trim();
        List<DailySiteReport> filtered = new ArrayList<>();
        for (DailySiteReport report : getAllReports()) {
            boolean matches = (report.getId() != null && report.getId().toLowerCase().contains(lowerQuery))
                    || (report.getProjectName() != null && report.getProjectName().toLowerCase().contains(lowerQuery))
                    || (report.getReportDate() != null && report.getReportDate().toLowerCase().contains(lowerQuery))
                    || (report.getCompletedWork() != null && report.getCompletedWork().toLowerCase().contains(lowerQuery))
                    || (report.getManpower() != null && report.getManpower().toLowerCase().contains(lowerQuery))
                    || (report.getMaterialsUsed() != null && report.getMaterialsUsed().toLowerCase().contains(lowerQuery))
                    || (report.getEquipment() != null && report.getEquipment().toLowerCase().contains(lowerQuery))
                    || (report.getStatus() != null && report.getStatus().toLowerCase().contains(lowerQuery));
            if (matches) {
                filtered.add(report);
            }
        }
        return filtered;
    }

    public void saveReport(DailySiteReport report) {
        List<DailySiteReport> list = getAllReports();
        int existingIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(report.getId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            list.set(existingIndex, report);
        } else {
            list.add(0, report); // Newest at top
        }

        saveAll(list);
    }

    public boolean deleteReport(String id) {
        List<DailySiteReport> list = getAllReports();
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            saveAll(list);
        }
        return removed;
    }

    private void saveAll(List<DailySiteReport> list) {
        JSONArray array = new JSONArray();
        for (DailySiteReport report : list) {
            try {
                array.put(report.toJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        preferences.edit().putString(KEY_REPORTS, array.toString()).apply();
    }

    private void ensureInitialData() {
        if (!preferences.contains(KEY_REPORTS)) {
            List<DailySiteReport> seed = new ArrayList<>();

            // Report 1: Submitted (Nov 06)
            DailySiteReport r1 = new DailySiteReport(
                    "DSR-2026-11-06",
                    "Metro Central Tower",
                    "Nov 06, 2026",
                    "Elena Rostova (PE)",
                    DailySiteReport.STATUS_SUBMITTED,
                    "Sunny, 28°C / Clear Skies",
                    "Completed concrete pour for 4th floor suspended slab grid line A1-D6 (45 m³). Continued formwork stripping on level 3 shear walls. MEP conduit layout verified by QA inspector.",
                    "14 Masons, 10 Rebar Barbenders, 4 Electricians, 2 QA Inspectors, 1 Site Engineer",
                    "45 m³ C35 Ready-Mix Concrete, 5.8 Tons T20/T25 Rebar, 25L Curing Compound",
                    "1 Potain Tower Crane, 1 Putzmeister 36m Boom Pump, 3 Concrete Vibrators",
                    "Minor 30-minute delay waiting for transit mixer batch #3. Pour completed without cold joint.",
                    null,
                    "Apply curing burlap to 4th-floor slab, begin column rebar cage tying C-1 to C-8, conduct slump tests for next batch.",
                    "Nov 06, 2026 17:30",
                    "Nov 06, 2026 18:00"
            );
            seed.add(r1);

            // Report 2: Submitted (Nov 05)
            DailySiteReport r2 = new DailySiteReport(
                    "DSR-2026-11-05",
                    "Metro Central Tower",
                    "Nov 05, 2026",
                    "Elena Rostova (PE)",
                    DailySiteReport.STATUS_SUBMITTED,
                    "Partly Cloudy, 26°C",
                    "Finalized bottom and top rebar placement for level 4 slab. Installed electrical conduits and plumbing sleeves. Pre-pour inspection passed with Lead Consultant.",
                    "12 Barbenders, 6 Formwork Carpenters, 3 Plumbers, 1 Site Engineer",
                    "8.2 Tons High-Yield Deformed Rebar, 120 PVC Conduit Pipes (25mm)",
                    "1 Tower Crane, 2 Bar Bending Machines, 1 Mobile Generator",
                    "None reported. All pre-pour quality checkpoints approved.",
                    null,
                    "Mobilize concrete pump team at 07:30 AM for slab pour.",
                    "Nov 05, 2026 17:15",
                    "Nov 05, 2026 17:45"
            );
            seed.add(r2);

            // Report 3: Draft (Nov 07)
            DailySiteReport r3 = new DailySiteReport(
                    "DSR-2026-11-07",
                    "Skyline Business Park",
                    "Nov 07, 2026",
                    "Elena Rostova (PE)",
                    DailySiteReport.STATUS_DRAFT,
                    "Morning Overcast, 24°C",
                    "Excavation and blinding concrete completed for Foundation Pit #5.",
                    "6 Earthwork Operators, 4 General Labors, 1 Site Engineer",
                    "12 m³ Lean Concrete C15",
                    "2 Komatsu Excavators, 3 Tipper Trucks, 1 Dewatering Pump",
                    "Groundwater seepage observed in west corner pit. Dewatering sump pump mobilized.",
                    null,
                    "Set up timber shuttering and waterproofing membrane installation.",
                    "Nov 07, 2026 09:30",
                    ""
            );
            seed.add(r3);

            saveAll(seed);
        }
    }
}
