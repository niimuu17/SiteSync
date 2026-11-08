package com.example.sitesync.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Model representing a comment or technical reply in an RFI / technical issue discussion thread.
 */
public class IssueComment implements Serializable {
    private final String commentId;
    private final String authorName;
    private final String authorRole;
    private final String timestamp;
    private final String message;

    public IssueComment(String commentId, String authorName, String authorRole,
                        String timestamp, String message) {
        this.commentId = commentId;
        this.authorName = authorName != null ? authorName : "Site Staff";
        this.authorRole = authorRole != null ? authorRole : "Field Engineer";
        this.timestamp = timestamp;
        this.message = message != null ? message : "";
    }

    public String getCommentId() {
        return commentId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("commentId", commentId);
        json.put("authorName", authorName);
        json.put("authorRole", authorRole);
        json.put("timestamp", timestamp);
        json.put("message", message);
        return json;
    }

    public static IssueComment fromJSONObject(JSONObject json) throws JSONException {
        return new IssueComment(
                json.optString("commentId", "COMM-" + System.currentTimeMillis()),
                json.optString("authorName", "Site Staff"),
                json.optString("authorRole", "Field Engineer"),
                json.optString("timestamp", "Recent"),
                json.optString("message", "")
        );
    }
}
