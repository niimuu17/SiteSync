package com.example.sitesync.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sitesync.models.IssueComment;
import com.example.sitesync.models.IssueHistoryLog;
import com.example.sitesync.models.TechnicalIssue;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Repository providing local persistence for Technical Issues & RFIs (Request for Information).
 * Uses SharedPreferences with JSON serialization for zero-dependency offline storage.
 */
public class IssueRepository {

    private static final String PREF_NAME = "SiteSync_Issue_Prefs";
    private static final String KEY_ISSUES = "key_technical_issues";

    private final SharedPreferences preferences;

    public IssueRepository(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureInitialData();
    }

    public List<TechnicalIssue> getAllIssues() {
        List<TechnicalIssue> list = new ArrayList<>();
        String json = preferences.getString(KEY_ISSUES, null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    list.add(TechnicalIssue.fromJSONObject(array.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public TechnicalIssue getIssueById(String id) {
        if (id == null) return null;
        for (TechnicalIssue issue : getAllIssues()) {
            if (id.equals(issue.getId())) {
                return issue;
            }
        }
        return null;
    }

    public List<TechnicalIssue> getIssuesByStatus(String status) {
        if (status == null || status.equalsIgnoreCase("All")) {
            return getAllIssues();
        }
        List<TechnicalIssue> filtered = new ArrayList<>();
        for (TechnicalIssue issue : getAllIssues()) {
            if (status.equalsIgnoreCase(issue.getStatus())) {
                filtered.add(issue);
            }
        }
        return filtered;
    }

    public List<TechnicalIssue> getIssuesByPriority(String priority) {
        if (priority == null || priority.equalsIgnoreCase("All")) {
            return getAllIssues();
        }
        List<TechnicalIssue> filtered = new ArrayList<>();
        for (TechnicalIssue issue : getAllIssues()) {
            if (priority.equalsIgnoreCase(issue.getPriority())) {
                filtered.add(issue);
            }
        }
        return filtered;
    }

    public List<TechnicalIssue> searchIssues(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllIssues();
        }
        String lowerQuery = query.toLowerCase().trim();
        List<TechnicalIssue> filtered = new ArrayList<>();
        for (TechnicalIssue issue : getAllIssues()) {
            boolean matches = (issue.getId() != null && issue.getId().toLowerCase().contains(lowerQuery))
                    || (issue.getTitle() != null && issue.getTitle().toLowerCase().contains(lowerQuery))
                    || (issue.getProjectName() != null && issue.getProjectName().toLowerCase().contains(lowerQuery))
                    || (issue.getSiteLocation() != null && issue.getSiteLocation().toLowerCase().contains(lowerQuery))
                    || (issue.getAssignedPerson() != null && issue.getAssignedPerson().toLowerCase().contains(lowerQuery))
                    || (issue.getPriority() != null && issue.getPriority().toLowerCase().contains(lowerQuery))
                    || (issue.getStatus() != null && issue.getStatus().toLowerCase().contains(lowerQuery));
            if (matches) {
                filtered.add(issue);
            }
        }
        return filtered;
    }

    public void addIssue(TechnicalIssue issue) {
        List<TechnicalIssue> current = getAllIssues();
        current.add(0, issue); // Add newest at top
        saveIssues(current);
    }

    public boolean updateIssue(TechnicalIssue updatedIssue) {
        List<TechnicalIssue> current = getAllIssues();
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getId().equals(updatedIssue.getId())) {
                current.set(i, updatedIssue);
                saveIssues(current);
                return true;
            }
        }
        return false;
    }

    public boolean addCommentToIssue(String issueId, IssueComment comment) {
        TechnicalIssue issue = getIssueById(issueId);
        if (issue != null) {
            issue.addComment(comment);
            String now = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
            issue.addHistoryLog(new IssueHistoryLog(
                    "LOG-" + System.currentTimeMillis(),
                    "Added comment: \"" + (comment.getMessage().length() > 30 ? comment.getMessage().substring(0, 27) + "..." : comment.getMessage()) + "\"",
                    now,
                    comment.getAuthorName()
            ));
            return updateIssue(issue);
        }
        return false;
    }

    public boolean updateIssueStatus(String issueId, String newStatus, String performedBy, String note) {
        TechnicalIssue issue = getIssueById(issueId);
        if (issue != null) {
            String oldStatus = issue.getStatus();
            issue.setStatus(newStatus);
            String now = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
            String logAction = "Status updated from " + oldStatus + " to " + newStatus;
            if (note != null && !note.trim().isEmpty()) {
                logAction += " (" + note.trim() + ")";
            }
            issue.addHistoryLog(new IssueHistoryLog(
                    "LOG-" + System.currentTimeMillis(),
                    logAction,
                    now,
                    performedBy
            ));
            return updateIssue(issue);
        }
        return false;
    }

    private void saveIssues(List<TechnicalIssue> list) {
        JSONArray array = new JSONArray();
        for (TechnicalIssue issue : list) {
            try {
                array.put(issue.toJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        preferences.edit().putString(KEY_ISSUES, array.toString()).apply();
    }

    private void ensureInitialData() {
        if (!preferences.contains(KEY_ISSUES)) {
            List<TechnicalIssue> seed = new ArrayList<>();

            // Seed Issue 1
            TechnicalIssue issue1 = new TechnicalIssue(
                    "RFI-101",
                    "HVAC Duct Clashing with Transfer Beam Girder",
                    "300mm chilled water supply duct collides with structural transfer beam TB-04 at Grid Line C-7. Awaiting MEP consultant rerouting revision or beam sleeve structural sign-off.",
                    "Metro Central Tower",
                    "Basement Level B2 - Grid Line C-7",
                    TechnicalIssue.PRIORITY_HIGH,
                    TechnicalIssue.STATUS_IN_REVIEW,
                    "Chief MEP Consultant",
                    "Alex Chen (Site Engineer)",
                    "Oct 24, 2026",
                    null
            );
            issue1.addComment(new IssueComment(
                    "C-1",
                    "David Kim",
                    "Lead Structural Engineer",
                    "Oct 24, 2026 14:15",
                    "Transfer beam coring is strictly prohibited due to post-tension cables. Duct must drop beneath beam soffit."
            ));
            issue1.addComment(new IssueComment(
                    "C-2",
                    "Alex Chen",
                    "Site Engineer",
                    "Oct 24, 2026 16:30",
                    "Verifying ceiling headroom clearance against NFPA minimum egress standard."
            ));
            issue1.addHistoryLog(new IssueHistoryLog("LOG-1", "Created RFI #101", "Oct 24, 2026 10:00", "Alex Chen"));
            issue1.addHistoryLog(new IssueHistoryLog("LOG-2", "Assigned to Chief MEP Consultant", "Oct 24, 2026 10:30", "Alex Chen"));
            issue1.addHistoryLog(new IssueHistoryLog("LOG-3", "Status changed from Assigned to In Review", "Oct 24, 2026 14:15", "David Kim"));
            seed.add(issue1);

            // Seed Issue 2
            TechnicalIssue issue2 = new TechnicalIssue(
                    "RFI-102",
                    "Rebar Spacing Discrepancy on Pile Cap B-4",
                    "Structural drawing S-104 specifies T25@100mm c/c bottom mat reinforcement, but bar bending schedule BBS-08 calls for T20@150mm. Work halted on cage tying.",
                    "Skyline Business Park",
                    "Sector 3 - Foundation Pit #4",
                    TechnicalIssue.PRIORITY_MEDIUM,
                    TechnicalIssue.STATUS_ASSIGNED,
                    "Robert Vance (PMP)",
                    "Priya Sharma (Field Engineer)",
                    "Oct 25, 2026",
                    null
            );
            issue2.addHistoryLog(new IssueHistoryLog("LOG-4", "Created RFI #102", "Oct 25, 2026 09:15", "Priya Sharma"));
            issue2.addHistoryLog(new IssueHistoryLog("LOG-5", "Assigned to Robert Vance (PMP)", "Oct 25, 2026 09:20", "Priya Sharma"));
            seed.add(issue2);

            // Seed Issue 3
            TechnicalIssue issue3 = new TechnicalIssue(
                    "RFI-103",
                    "Concrete Slump Specification Clarification for Pump Pour",
                    "Standard specification calls for 120mm +/- 25mm slump, but mobile boom pump operator advises minimum 160mm workability with superplasticizer for high-rise 45m vertical discharge.",
                    "Riverside Residences",
                    "Block B - 8th Floor Deck Slab",
                    TechnicalIssue.PRIORITY_LOW,
                    TechnicalIssue.STATUS_RESOLVED,
                    "Elena Rostova (PE)",
                    "John Doe (Site Engineer)",
                    "Oct 20, 2026",
                    null
            );
            issue3.addComment(new IssueComment(
                    "C-3",
                    "Elena Rostova",
                    "Structural Consultant",
                    "Oct 21, 2026 11:00",
                    "Approved Sikament NN dosage to achieve 160mm target slump without exceeding 0.40 W/C ratio. Test cylinders taken."
            ));
            issue3.addHistoryLog(new IssueHistoryLog("LOG-6", "Created RFI #103", "Oct 20, 2026 08:30", "John Doe"));
            issue3.addHistoryLog(new IssueHistoryLog("LOG-7", "Assigned to Elena Rostova (PE)", "Oct 20, 2026 09:00", "John Doe"));
            issue3.addHistoryLog(new IssueHistoryLog("LOG-8", "Status updated from In Review to Resolved", "Oct 21, 2026 11:00", "Elena Rostova"));
            seed.add(issue3);

            saveIssues(seed);
        }
    }
}
