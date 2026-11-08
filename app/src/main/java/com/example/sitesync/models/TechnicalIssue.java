package com.example.sitesync.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a Technical Issue / RFI (Request for Information) on a construction project.
 */
public class TechnicalIssue implements Serializable {
    public static final String PRIORITY_LOW = "Low";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_HIGH = "High";

    public static final String STATUS_OPEN = "Open";
    public static final String STATUS_ASSIGNED = "Assigned";
    public static final String STATUS_IN_REVIEW = "In Review";
    public static final String STATUS_RESOLVED = "Resolved";
    public static final String STATUS_CLOSED = "Closed";

    private String id;
    private String title;
    private String description;
    private String projectName;
    private String siteLocation;
    private String priority;
    private String status;
    private String assignedPerson;
    private String createdBy;
    private String createdDate;
    private String photoUri;
    private List<IssueComment> comments;
    private List<IssueHistoryLog> historyLogs;

    public TechnicalIssue() {
        this.comments = new ArrayList<>();
        this.historyLogs = new ArrayList<>();
    }

    public TechnicalIssue(String id, String title, String description, String projectName,
                          String siteLocation, String priority, String status,
                          String assignedPerson, String createdBy, String createdDate,
                          String photoUri) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.projectName = projectName;
        this.siteLocation = siteLocation;
        this.priority = priority != null ? priority : PRIORITY_MEDIUM;
        this.status = status != null ? status : STATUS_OPEN;
        this.assignedPerson = assignedPerson != null ? assignedPerson : "Unassigned";
        this.createdBy = createdBy != null ? createdBy : "Site Engineer";
        this.createdDate = createdDate;
        this.photoUri = photoUri;
        this.comments = new ArrayList<>();
        this.historyLogs = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSiteLocation() {
        return siteLocation;
    }

    public void setSiteLocation(String siteLocation) {
        this.siteLocation = siteLocation;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedPerson() {
        return assignedPerson;
    }

    public void setAssignedPerson(String assignedPerson) {
        this.assignedPerson = assignedPerson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public List<IssueComment> getComments() {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        return comments;
    }

    public void setComments(List<IssueComment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
    }

    public void addComment(IssueComment comment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(comment);
    }

    public List<IssueHistoryLog> getHistoryLogs() {
        if (historyLogs == null) {
            historyLogs = new ArrayList<>();
        }
        return historyLogs;
    }

    public void setHistoryLogs(List<IssueHistoryLog> historyLogs) {
        this.historyLogs = historyLogs != null ? historyLogs : new ArrayList<>();
    }

    public void addHistoryLog(IssueHistoryLog log) {
        if (historyLogs == null) {
            historyLogs = new ArrayList<>();
        }
        historyLogs.add(log);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("description", description);
        json.put("projectName", projectName);
        json.put("siteLocation", siteLocation);
        json.put("priority", priority);
        json.put("status", status);
        json.put("assignedPerson", assignedPerson);
        json.put("createdBy", createdBy);
        json.put("createdDate", createdDate);
        json.put("photoUri", photoUri != null ? photoUri : "");

        JSONArray commentsArray = new JSONArray();
        if (comments != null) {
            for (IssueComment comment : comments) {
                commentsArray.put(comment.toJSONObject());
            }
        }
        json.put("comments", commentsArray);

        JSONArray historyArray = new JSONArray();
        if (historyLogs != null) {
            for (IssueHistoryLog log : historyLogs) {
                historyArray.put(log.toJSONObject());
            }
        }
        json.put("historyLogs", historyArray);

        return json;
    }

    public static TechnicalIssue fromJSONObject(JSONObject json) throws JSONException {
        TechnicalIssue issue = new TechnicalIssue();
        issue.setId(json.optString("id", ""));
        issue.setTitle(json.optString("title", ""));
        issue.setDescription(json.optString("description", ""));
        issue.setProjectName(json.optString("projectName", ""));
        issue.setSiteLocation(json.optString("siteLocation", ""));
        issue.setPriority(json.optString("priority", PRIORITY_MEDIUM));
        issue.setStatus(json.optString("status", STATUS_OPEN));
        issue.setAssignedPerson(json.optString("assignedPerson", "Unassigned"));
        issue.setCreatedBy(json.optString("createdBy", "Site Engineer"));
        issue.setCreatedDate(json.optString("createdDate", ""));
        issue.setPhotoUri(json.optString("photoUri", null));

        if (json.has("comments")) {
            JSONArray commentsArray = json.getJSONArray("comments");
            List<IssueComment> commentsList = new ArrayList<>();
            for (int i = 0; i < commentsArray.length(); i++) {
                commentsList.add(IssueComment.fromJSONObject(commentsArray.getJSONObject(i)));
            }
            issue.setComments(commentsList);
        }

        if (json.has("historyLogs")) {
            JSONArray historyArray = json.getJSONArray("historyLogs");
            List<IssueHistoryLog> historyList = new ArrayList<>();
            for (int i = 0; i < historyArray.length(); i++) {
                historyList.add(IssueHistoryLog.fromJSONObject(historyArray.getJSONObject(i)));
            }
            issue.setHistoryLogs(historyList);
        }

        return issue;
    }
}
