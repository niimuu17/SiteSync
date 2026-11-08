package com.example.sitesync.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a construction drawing or engineering document.
 * Categorized as Architectural, Structural, Electrical, or Plumbing with revision tracking.
 */
public class SiteDocument implements Serializable {

    public static final String CATEGORY_ARCHITECTURAL = "Architectural";
    public static final String CATEGORY_STRUCTURAL = "Structural";
    public static final String CATEGORY_ELECTRICAL = "Electrical";
    public static final String CATEGORY_PLUMBING = "Plumbing";

    private final String id;
    private String name;
    private String projectName;
    private String category;
    private String uploadDate;
    private String currentRevision;
    private boolean isApproved;
    private String uploadedBy;
    private String fileFormat;
    private String fileSize;
    private String fileUri;
    private String description;
    private final List<DocumentRevision> revisionHistory;

    public SiteDocument(String id, String name, String projectName, String category,
                        String uploadDate, String currentRevision, boolean isApproved,
                        String uploadedBy, String fileFormat, String fileSize,
                        String fileUri, String description,
                        List<DocumentRevision> revisionHistory) {
        this.id = id;
        this.name = name;
        this.projectName = projectName;
        this.category = category != null ? category : CATEGORY_STRUCTURAL;
        this.uploadDate = uploadDate;
        this.currentRevision = currentRevision != null ? currentRevision : "Rev 1.0.0";
        this.isApproved = isApproved;
        this.uploadedBy = uploadedBy != null ? uploadedBy : "Field Engineer";
        this.fileFormat = fileFormat != null ? fileFormat : "PDF";
        this.fileSize = fileSize != null ? fileSize : "PDF • 3.2 MB";
        this.fileUri = fileUri != null ? fileUri : "";
        this.description = description != null ? description : "";
        this.revisionHistory = revisionHistory != null ? revisionHistory : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getCurrentRevision() {
        return currentRevision;
    }

    public void setCurrentRevision(String currentRevision) {
        this.currentRevision = currentRevision;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DocumentRevision> getRevisionHistory() {
        return revisionHistory;
    }

    public void addRevision(DocumentRevision revision) {
        if (revision != null) {
            // Newest revision at the top
            revisionHistory.add(0, revision);
            this.currentRevision = revision.getRevisionNumber();
            this.isApproved = revision.isApproved();
            this.uploadDate = revision.getUploadDate();
            this.uploadedBy = revision.getUploadedBy();
            this.fileSize = revision.getFileSize();
            if (revision.getFileUri() != null && !revision.getFileUri().isEmpty()) {
                this.fileUri = revision.getFileUri();
            }
        }
    }

    public DocumentRevision getLatestApprovedRevision() {
        for (DocumentRevision rev : revisionHistory) {
            if (rev.isApproved()) {
                return rev;
            }
        }
        return null;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("projectName", projectName);
        json.put("category", category);
        json.put("uploadDate", uploadDate);
        json.put("currentRevision", currentRevision);
        json.put("isApproved", isApproved);
        json.put("uploadedBy", uploadedBy);
        json.put("fileFormat", fileFormat);
        json.put("fileSize", fileSize);
        json.put("fileUri", fileUri);
        json.put("description", description);

        JSONArray revArray = new JSONArray();
        for (DocumentRevision rev : revisionHistory) {
            revArray.put(rev.toJSONObject());
        }
        json.put("revisionHistory", revArray);

        return json;
    }

    public static SiteDocument fromJSONObject(JSONObject json) throws JSONException {
        List<DocumentRevision> history = new ArrayList<>();
        JSONArray revArray = json.optJSONArray("revisionHistory");
        if (revArray != null) {
            for (int i = 0; i < revArray.length(); i++) {
                history.add(DocumentRevision.fromJSONObject(revArray.getJSONObject(i)));
            }
        }

        return new SiteDocument(
                json.optString("id", "DOC-" + System.currentTimeMillis()),
                json.optString("name", "Site Blueprint"),
                json.optString("projectName", "Metro Tower Phase 2"),
                json.optString("category", CATEGORY_STRUCTURAL),
                json.optString("uploadDate", "Recent"),
                json.optString("currentRevision", "Rev 1.0.0"),
                json.optBoolean("isApproved", true),
                json.optString("uploadedBy", "Field Engineer"),
                json.optString("fileFormat", "PDF"),
                json.optString("fileSize", "PDF • 3.2 MB"),
                json.optString("fileUri", ""),
                json.optString("description", ""),
                history
        );
    }
}
