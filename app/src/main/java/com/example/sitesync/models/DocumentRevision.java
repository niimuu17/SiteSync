package com.example.sitesync.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Model representing a specific version / revision of a construction drawing or document.
 */
public class DocumentRevision implements Serializable {
    private final String revisionId;
    private final String revisionNumber;
    private final String uploadDate;
    private final String uploadedBy;
    private final boolean isApproved;
    private final String changeNotes;
    private final String fileSize;
    private final String fileUri;

    public DocumentRevision(String revisionId, String revisionNumber, String uploadDate,
                            String uploadedBy, boolean isApproved, String changeNotes,
                            String fileSize, String fileUri) {
        this.revisionId = revisionId;
        this.revisionNumber = revisionNumber;
        this.uploadDate = uploadDate;
        this.uploadedBy = uploadedBy != null ? uploadedBy : "Field Engineer";
        this.isApproved = isApproved;
        this.changeNotes = changeNotes != null ? changeNotes : "";
        this.fileSize = fileSize != null ? fileSize : "PDF • 2.5 MB";
        this.fileUri = fileUri != null ? fileUri : "";
    }

    public String getRevisionId() {
        return revisionId;
    }

    public String getRevisionNumber() {
        return revisionNumber;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public String getChangeNotes() {
        return changeNotes;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getFileUri() {
        return fileUri;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("revisionId", revisionId);
        json.put("revisionNumber", revisionNumber);
        json.put("uploadDate", uploadDate);
        json.put("uploadedBy", uploadedBy);
        json.put("isApproved", isApproved);
        json.put("changeNotes", changeNotes);
        json.put("fileSize", fileSize);
        json.put("fileUri", fileUri);
        return json;
    }

    public static DocumentRevision fromJSONObject(JSONObject json) throws JSONException {
        return new DocumentRevision(
                json.optString("revisionId", "REV-" + System.currentTimeMillis()),
                json.optString("revisionNumber", "Rev 1.0.0"),
                json.optString("uploadDate", "Recent"),
                json.optString("uploadedBy", "Field Engineer"),
                json.optBoolean("isApproved", true),
                json.optString("changeNotes", ""),
                json.optString("fileSize", "PDF • 2.5 MB"),
                json.optString("fileUri", "")
        );
    }
}
