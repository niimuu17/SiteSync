package com.example.sitesync.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.IssueDao;
import com.example.sitesync.data.local.IssueEntity;
import com.example.sitesync.models.IssueComment;
import com.example.sitesync.models.IssueHistoryLog;
import com.example.sitesync.models.TechnicalIssue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enterprise Single-Source-of-Truth Repository for Technical Issues & RFIs.
 * Backed by Room Database local cache and Cloud Firestore real-time snapshot sync.
 */
public class IssueRepository {

    private static final String TAG = "IssueRepository";
    private static final String COLLECTION_ISSUES = "issues";

    private final IssueDao issueDao;
    private final FirebaseFirestore firestore;

    public IssueRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.issueDao = db.issueDao();
        this.firestore = FirebaseSyncManager.getInstance(context).getFirestore();

        ensureInitialData();
        attachFirestoreSnapshotListener();
    }

    private void attachFirestoreSnapshotListener() {
        if (firestore == null) return;
        firestore.collection(COLLECTION_ISSUES)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Firestore issues listen error: " + e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            List<IssueEntity> remoteEntities = new ArrayList<>();
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String rawJson = doc.getString("rawJson");
                                if (rawJson != null && !rawJson.isEmpty()) {
                                    try {
                                        TechnicalIssue issue = TechnicalIssue.fromJSONObject(new JSONObject(rawJson));
                                        remoteEntities.add(IssueEntity.fromModel(issue));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            if (!remoteEntities.isEmpty()) {
                                issueDao.insertAll(remoteEntities);
                            }
                        });
                    }
                });
    }

    public LiveData<List<TechnicalIssue>> getAllIssuesLive() {
        return Transformations.map(issueDao.getAllLive(), entities -> {
            List<TechnicalIssue> list = new ArrayList<>();
            for (IssueEntity e : entities) {
                list.add(e.toModel());
            }
            return list;
        });
    }

    public List<TechnicalIssue> getAllIssues() {
        List<IssueEntity> entities = issueDao.getAllSync();
        List<TechnicalIssue> list = new ArrayList<>();
        for (IssueEntity e : entities) {
            list.add(e.toModel());
        }
        return list;
    }

    public TechnicalIssue getIssueById(String id) {
        if (id == null) return null;
        IssueEntity entity = issueDao.getById(id);
        return entity != null ? entity.toModel() : null;
    }

    public boolean addIssue(TechnicalIssue issue) {
        if (issue == null || issue.getId() == null) return false;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            issueDao.insertOrUpdate(IssueEntity.fromModel(issue));
        });

        if (firestore != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", issue.getId());
            data.put("title", issue.getTitle());
            data.put("projectName", issue.getProjectName());
            data.put("siteLocation", issue.getSiteLocation());
            data.put("priority", issue.getPriority());
            data.put("status", issue.getStatus());
            data.put("assignedPerson", issue.getAssignedPerson());
            data.put("createdBy", issue.getCreatedBy());
            data.put("createdDate", issue.getCreatedDate());
            try {
                data.put("rawJson", issue.toJSONObject().toString());
            } catch (Exception ignored) {
            }
            data.put("lastUpdated", System.currentTimeMillis());

            firestore.collection(COLLECTION_ISSUES)
                    .document(issue.getId())
                    .set(data);
        }
        return true;
    }

    public boolean updateIssueStatus(String issueId, String newStatus, String changedBy, String notes) {
        TechnicalIssue issue = getIssueById(issueId);
        if (issue == null) return false;

        String oldStatus = issue.getStatus();
        issue.setStatus(newStatus);
        String timestamp = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());

        IssueHistoryLog log = new IssueHistoryLog(
                "LOG-" + System.currentTimeMillis(),
                "Status: " + oldStatus + " -> " + newStatus + (notes != null ? " (" + notes + ")" : ""),
                timestamp,
                changedBy != null ? changedBy : "Site Engineer"
        );
        issue.addHistoryLog(log);

        return addIssue(issue);
    }

    public boolean addCommentToIssue(String issueId, IssueComment comment) {
        TechnicalIssue issue = getIssueById(issueId);
        if (issue == null || comment == null) return false;
        issue.addComment(comment);
        return addIssue(issue);
    }

    public boolean deleteIssue(String issueId) {
        if (issueId == null) return false;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            issueDao.deleteById(issueId);
        });
        if (firestore != null) {
            firestore.collection(COLLECTION_ISSUES).document(issueId).delete();
        }
        return true;
    }

    private void ensureInitialData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<IssueEntity> existing = issueDao.getAllSync();
            if (existing == null || existing.isEmpty()) {
                List<IssueEntity> initial = new ArrayList<>();

                // Seed 1: RFI-201
                TechnicalIssue i1 = new TechnicalIssue(
                        "RFI-201", "Level 14 HVAC Duct Clash with Structural Shear Beam",
                        "Main supply return duct route collides with shear beam SB-14C at grid intersection 6-D. Requesting mechanical reroute or beam web penetration signoff.",
                        "Metro Central Tower", "Core Tower - Level 14", TechnicalIssue.PRIORITY_HIGH,
                        TechnicalIssue.STATUS_IN_REVIEW, "Elena Rostova (PE)", "Alex Chen",
                        "Nov 06, 2026 - 10:15 AM", ""
                );
                initial.add(IssueEntity.fromModel(i1));

                // Seed 2: RFI-202
                TechnicalIssue i2 = new TechnicalIssue(
                        "RFI-202", "Basement B2 Waterproofing Membrane Membrane Tears",
                        "Minor punctures discovered along the southern cold joint retaining wall prior to backfill compaction.",
                        "Metro Central Tower", "Basement B2 Retaining Wall", TechnicalIssue.PRIORITY_MEDIUM,
                        TechnicalIssue.STATUS_ASSIGNED, "Waterproofing Subcontractor", "Alex Chen",
                        "Nov 05, 2026 - 02:40 PM", ""
                );
                initial.add(IssueEntity.fromModel(i2));

                issueDao.insertAll(initial);
            }
        });
    }
}
