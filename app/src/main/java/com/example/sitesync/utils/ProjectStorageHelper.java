package com.example.sitesync.utils;

import android.content.Context;

import java.io.File;

/**
 * Ensures strict multi-project data segregation.
 * Each project maintains its own isolated directories for reports,
 * evidence photos, audio recordings, and chat media.
 */
public class ProjectStorageHelper {

    private static String sanitize(String projectCode) {
        if (projectCode == null || projectCode.trim().isEmpty()) {
            return "default_project";
        }
        return projectCode.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    public static File getProjectRoot(Context context, String projectCode) {
        File dir = new File(context.getFilesDir(), "projects/" + sanitize(projectCode));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getReportsDir(Context context, String projectCode) {
        File dir = new File(getProjectRoot(context, projectCode), "reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getEvidencePhotosDir(Context context, String projectCode) {
        File dir = new File(getProjectRoot(context, projectCode), "evidence_photos");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getChatMediaDir(Context context, String projectCode) {
        File dir = new File(getProjectRoot(context, projectCode), "chat_media");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getVoiceNotesDir(Context context, String projectCode) {
        File dir = new File(getProjectRoot(context, projectCode), "voice_notes");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
