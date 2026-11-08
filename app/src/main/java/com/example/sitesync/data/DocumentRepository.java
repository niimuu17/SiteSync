package com.example.sitesync.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sitesync.models.DocumentRevision;
import com.example.sitesync.models.SiteDocument;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository providing local persistence for Construction Drawings & Engineering Documents
 * categorized as Architectural, Structural, Electrical, and Plumbing.
 */
public class DocumentRepository {

    private static final String PREF_NAME = "SiteSync_Document_Prefs";
    private static final String KEY_DOCUMENTS = "key_construction_documents";

    private final SharedPreferences preferences;

    public DocumentRepository(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureInitialData();
    }

    public List<SiteDocument> getAllDocuments() {
        List<SiteDocument> list = new ArrayList<>();
        String json = preferences.getString(KEY_DOCUMENTS, null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    list.add(SiteDocument.fromJSONObject(array.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public SiteDocument getDocumentById(String id) {
        if (id == null) return null;
        for (SiteDocument doc : getAllDocuments()) {
            if (id.equals(doc.getId())) {
                return doc;
            }
        }
        return null;
    }

    public List<SiteDocument> getDocumentsByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All")) {
            return getAllDocuments();
        }
        List<SiteDocument> filtered = new ArrayList<>();
        for (SiteDocument doc : getAllDocuments()) {
            if (category.equalsIgnoreCase(doc.getCategory())) {
                filtered.add(doc);
            }
        }
        return filtered;
    }

    public List<SiteDocument> searchDocuments(String query, String category) {
        List<SiteDocument> baseList = getDocumentsByCategory(category);
        if (query == null || query.trim().isEmpty()) {
            return baseList;
        }

        String lowerQuery = query.trim().toLowerCase();
        List<SiteDocument> results = new ArrayList<>();
        for (SiteDocument doc : baseList) {
            boolean matchesName = doc.getName() != null && doc.getName().toLowerCase().contains(lowerQuery);
            boolean matchesRevision = doc.getCurrentRevision() != null && doc.getCurrentRevision().toLowerCase().contains(lowerQuery);
            boolean matchesProject = doc.getProjectName() != null && doc.getProjectName().toLowerCase().contains(lowerQuery);

            if (matchesName || matchesRevision || matchesProject) {
                results.add(doc);
            }
        }
        return results;
    }

    public void addDocument(SiteDocument doc) {
        List<SiteDocument> list = getAllDocuments();
        list.add(0, doc);
        saveDocuments(list);
    }

    public boolean addRevisionToDocument(String docId, DocumentRevision revision) {
        List<SiteDocument> list = getAllDocuments();
        boolean updated = false;

        for (int i = 0; i < list.size(); i++) {
            SiteDocument doc = list.get(i);
            if (doc.getId().equals(docId)) {
                doc.addRevision(revision);
                list.set(i, doc);
                updated = true;
                break;
            }
        }

        if (updated) {
            saveDocuments(list);
        }
        return updated;
    }

    private void saveDocuments(List<SiteDocument> list) {
        try {
            JSONArray array = new JSONArray();
            for (SiteDocument doc : list) {
                array.put(doc.toJSONObject());
            }
            preferences.edit().putString(KEY_DOCUMENTS, array.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void ensureInitialData() {
        if (!preferences.contains(KEY_DOCUMENTS)) {
            List<SiteDocument> initial = new ArrayList<>();

            // 1. Structural Drawing
            List<DocumentRevision> revs1 = new ArrayList<>();
            revs1.add(new DocumentRevision("REV-101", "Rev 4.8.2", "Nov 06, 2026 - 11:00 AM",
                    "Elena Rostova (PE)", true, "Adjusted column starter bar lap splice offsets along Grid C-4.", "PDF • 5.8 MB", ""));
            revs1.add(new DocumentRevision("REV-102", "Rev 4.8.1", "Nov 02, 2026 - 03:15 PM",
                    "Elena Rostova (PE)", false, "Superseded - Initial raft reinforcement layout with 32mm rebars.", "PDF • 5.6 MB", ""));
            revs1.add(new DocumentRevision("REV-103", "Rev 4.0.0", "Oct 15, 2026 - 09:00 AM",
                    "Marcus Chen", false, "Superseded - Foundation preliminary schematic release.", "PDF • 4.9 MB", ""));

            initial.add(new SiteDocument(
                    "DOC-STR-101",
                    "Ground & Basement Raft Foundation Layout",
                    "Metro Tower Phase 2",
                    SiteDocument.CATEGORY_STRUCTURAL,
                    "Nov 06, 2026",
                    "Rev 4.8.2",
                    true,
                    "Elena Rostova (PE)",
                    "PDF",
                    "PDF • 5.8 MB",
                    "",
                    "Foundation slab rebar reinforcement schedules, column starter bars, and pile cap details.",
                    revs1
            ));

            // 2. Architectural Drawing
            List<DocumentRevision> revs2 = new ArrayList<>();
            revs2.add(new DocumentRevision("REV-201", "Rev 3.2.0", "Nov 05, 2026 - 02:30 PM",
                    "Elena Rostova (PE)", true, "Widened North egress exit corridor per municipal fire safety code.", "PDF • 8.4 MB", ""));
            revs2.add(new DocumentRevision("REV-202", "Rev 3.1.0", "Oct 28, 2026 - 10:00 AM",
                    "Robert Vance (PMP)", false, "Superseded - Core lift lobby revision.", "PDF • 8.1 MB", ""));

            initial.add(new SiteDocument(
                    "DOC-ARC-102",
                    "Typical Floor Core & Elevation Plan (Levels 2-12)",
                    "Metro Tower Phase 2",
                    SiteDocument.CATEGORY_ARCHITECTURAL,
                    "Nov 05, 2026",
                    "Rev 3.2.0",
                    true,
                    "Elena Rostova (PE)",
                    "PDF",
                    "PDF • 8.4 MB",
                    "",
                    "Architectural floor zoning, egress stairwells, curtain wall interfaces, and fire partition ratings.",
                    revs2
            ));

            // 3. Electrical Drawing
            List<DocumentRevision> revs3 = new ArrayList<>();
            revs3.add(new DocumentRevision("REV-301", "Rev 2.0.4", "Nov 04, 2026 - 04:15 PM",
                    "Elena Rostova (PE)", true, "Added 500kVA emergency backup diesel generator interlock switchgear.", "PDF • 3.7 MB", ""));
            revs3.add(new DocumentRevision("REV-302", "Rev 2.0.0", "Oct 18, 2026 - 01:20 PM",
                    "Marcus Chen", false, "Superseded - Initial single-line electrical schematic.", "PDF • 3.5 MB", ""));

            initial.add(new SiteDocument(
                    "DOC-ELE-103",
                    "High Voltage Substation & Switchboard Single-Line Diagram",
                    "Metro Tower Phase 2",
                    SiteDocument.CATEGORY_ELECTRICAL,
                    "Nov 04, 2026",
                    "Rev 2.0.4",
                    true,
                    "Elena Rostova (PE)",
                    "PDF",
                    "PDF • 3.7 MB",
                    "",
                    "11kV transformer schematics, busbar ratings, emergency generator transfer switches, and riser distribution.",
                    revs3
            ));

            // 4. Plumbing Drawing
            List<DocumentRevision> revs4 = new ArrayList<>();
            revs4.add(new DocumentRevision("REV-401", "Rev 1.5.0", "Nov 03, 2026 - 09:45 AM",
                    "Elena Rostova (PE)", true, "Re-routed main 150mm stormwater storm line around elevator sump pit.", "PDF • 4.1 MB", ""));

            initial.add(new SiteDocument(
                    "DOC-PLU-104",
                    "Domestic Water Supply & Stormwater Drainage Isometric",
                    "Metro Tower Phase 2",
                    SiteDocument.CATEGORY_PLUMBING,
                    "Nov 03, 2026",
                    "Rev 1.5.0",
                    true,
                    "Elena Rostova (PE)",
                    "PDF",
                    "PDF • 4.1 MB",
                    "",
                    "Potable booster pump distribution, rooftop detention tanks, and greywater recycling piping risers.",
                    revs4
            ));

            saveDocuments(initial);
        }
    }
}
