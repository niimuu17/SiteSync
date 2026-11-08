package com.example.sitesync.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.DocumentDao;
import com.example.sitesync.data.local.DocumentEntity;
import com.example.sitesync.models.DocumentRevision;
import com.example.sitesync.models.SiteDocument;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Single-Source-of-Truth Repository for CAD Blueprints & Drawings.
 * Backed by Room Database local cache and Cloud Firestore real-time snapshot sync.
 */
public class DocumentRepository {

    private static final String TAG = "DocumentRepository";
    private static final String COLLECTION_DOCUMENTS = "documents";

    private final DocumentDao documentDao;
    private final FirebaseFirestore firestore;

    public DocumentRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.documentDao = db.documentDao();
        this.firestore = FirebaseSyncManager.getInstance(context).getFirestore();

        ensureInitialData();
        attachFirestoreSnapshotListener();
    }

    private void attachFirestoreSnapshotListener() {
        if (firestore == null) return;
        firestore.collection(COLLECTION_DOCUMENTS)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Firestore documents listen error: " + e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            List<DocumentEntity> remoteEntities = new ArrayList<>();
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String rawJson = doc.getString("rawJson");
                                if (rawJson != null && !rawJson.isEmpty()) {
                                    try {
                                        SiteDocument siteDoc = SiteDocument.fromJSONObject(new JSONObject(rawJson));
                                        remoteEntities.add(DocumentEntity.fromModel(siteDoc));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            if (!remoteEntities.isEmpty()) {
                                documentDao.insertAll(remoteEntities);
                            }
                        });
                    }
                });
    }

    public LiveData<List<SiteDocument>> getAllDocumentsLive() {
        return Transformations.map(documentDao.getAllLive(), entities -> {
            List<SiteDocument> list = new ArrayList<>();
            for (DocumentEntity e : entities) {
                list.add(e.toModel());
            }
            return list;
        });
    }

    public List<SiteDocument> getAllDocuments() {
        List<DocumentEntity> entities = documentDao.getAllSync();
        List<SiteDocument> list = new ArrayList<>();
        for (DocumentEntity e : entities) {
            list.add(e.toModel());
        }
        return list;
    }

    public SiteDocument getDocumentById(String id) {
        if (id == null) return null;
        DocumentEntity entity = documentDao.getById(id);
        return entity != null ? entity.toModel() : null;
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
        List<SiteDocument> base = getDocumentsByCategory(category);
        if (query == null || query.trim().isEmpty()) {
            return base;
        }
        String lower = query.trim().toLowerCase();
        List<SiteDocument> filtered = new ArrayList<>();
        for (SiteDocument doc : base) {
            if ((doc.getName() != null && doc.getName().toLowerCase().contains(lower)) ||
                (doc.getProjectName() != null && doc.getProjectName().toLowerCase().contains(lower)) ||
                (doc.getCurrentRevision() != null && doc.getCurrentRevision().toLowerCase().contains(lower))) {
                filtered.add(doc);
            }
        }
        return filtered;
    }

    public boolean addRevisionToDocument(String documentId, DocumentRevision revision) {
        return uploadNewRevision(documentId, revision);
    }

    public boolean addDocument(SiteDocument document) {
        if (document == null || document.getId() == null) return false;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            documentDao.insertOrUpdate(DocumentEntity.fromModel(document));
        });

        if (firestore != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", document.getId());
            data.put("name", document.getName());
            data.put("projectName", document.getProjectName());
            data.put("category", document.getCategory());
            data.put("uploadDate", document.getUploadDate());
            data.put("currentRevision", document.getCurrentRevision());
            data.put("isApproved", document.isApproved());
            data.put("uploadedBy", document.getUploadedBy());
            try {
                data.put("rawJson", document.toJSONObject().toString());
            } catch (Exception ignored) {
            }
            data.put("lastUpdated", System.currentTimeMillis());

            firestore.collection(COLLECTION_DOCUMENTS)
                    .document(document.getId())
                    .set(data);
        }
        return true;
    }

    public boolean uploadNewRevision(String documentId, DocumentRevision revision) {
        SiteDocument document = getDocumentById(documentId);
        if (document == null || revision == null) return false;
        document.addRevision(revision);
        return addDocument(document);
    }

    public boolean deleteDocument(String id) {
        if (id == null) return false;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            documentDao.deleteById(id);
        });
        if (firestore != null) {
            firestore.collection(COLLECTION_DOCUMENTS).document(id).delete();
        }
        return true;
    }

    private void ensureInitialData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<DocumentEntity> existing = documentDao.getAllSync();
            if (existing == null || existing.isEmpty()) {
                List<DocumentEntity> initial = new ArrayList<>();

                // Seed 1: Architectural
                List<DocumentRevision> revs1 = new ArrayList<>();
                revs1.add(new DocumentRevision("REV-1", "Rev 2.1", "Nov 06, 2026", "Alex Chen (PE)", true, "Approved curtain wall facade integration", "PDF • 4.8 MB", ""));
                SiteDocument doc1 = new SiteDocument(
                        "DOC-001", "Tower Envelope & Curtain Wall Details", "Metro Central Tower",
                        SiteDocument.CATEGORY_ARCHITECTURAL, "Nov 06, 2026", "Rev 2.1",
                        true, "Alex Chen (PE)", "PDF", "PDF • 4.8 MB", "",
                        "Full architectural facade detail with double glazed thermal performance data.",
                        revs1
                );
                initial.add(DocumentEntity.fromModel(doc1));

                // Seed 2: Structural
                List<DocumentRevision> revs2 = new ArrayList<>();
                revs2.add(new DocumentRevision("REV-2", "Rev 4.8.2", "Nov 04, 2026", "Structural Engineering Consultant", true, "Latest Approved Revision for Level 14 columns", "PDF • 8.1 MB", ""));
                SiteDocument doc2 = new SiteDocument(
                        "DOC-002", "Core Wall Reinforcement Layout", "Metro Central Tower",
                        SiteDocument.CATEGORY_STRUCTURAL, "Nov 04, 2026", "Rev 4.8.2",
                        true, "Elena Rostova (PE)", "PDF", "PDF • 8.1 MB", "",
                        "Detailed rebar schedule and coupler splice elevations for shear core.",
                        revs2
                );
                initial.add(DocumentEntity.fromModel(doc2));

                documentDao.insertAll(initial);
            }
        });
    }
}
