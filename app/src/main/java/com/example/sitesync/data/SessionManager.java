package com.example.sitesync.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sitesync.models.User;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Manages persistent user session, Firebase state, and profile caching.
 */
public class SessionManager {
    private static final String PREF_NAME = "SiteSyncPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_UID = "uid";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_ROLE = "role";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_SAVED_USERNAME = "savedUsername";
    private static final String KEY_ACTIVE_PROJECT_ID = "activeProjectId";
    private static final String KEY_ACTIVE_PROJECT_CODE = "activeProjectCode";
    private static final String KEY_ACTIVE_PROJECT_NAME = "activeProjectName";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
    }

    public void createLoginSession(User user, boolean rememberMe) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_UID, user.getUid());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_TOKEN, user.getToken());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_PHONE, user.getPhoneNumber());
        editor.putString(KEY_COUNTRY, user.getCountry());
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);

        if (rememberMe) {
            editor.putString(KEY_SAVED_USERNAME, user.getUsername());
        } else {
            editor.remove(KEY_SAVED_USERNAME);
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        boolean firebaseLoggedIn = false;
        try {
            firebaseLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        } catch (Exception ignored) {
        }
        return pref.getBoolean(KEY_IS_LOGGED_IN, false) || firebaseLoggedIn;
    }

    public boolean isRememberMeEnabled() {
        return pref.getBoolean(KEY_REMEMBER_ME, false);
    }

    public String getSavedUsername() {
        return pref.getString(KEY_SAVED_USERNAME, "");
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }
        String uid = pref.getString(KEY_UID, "");
        String username = pref.getString(KEY_USERNAME, "engineer");
        String fullName = pref.getString(KEY_FULL_NAME, "Field Staff");
        String role = pref.getString(KEY_ROLE, "ENGINEER");
        String token = pref.getString(KEY_TOKEN, "");
        String email = pref.getString(KEY_EMAIL, "");
        String phone = pref.getString(KEY_PHONE, "");
        String country = pref.getString(KEY_COUNTRY, "");

        User user = new User(username, fullName, role, token, email, phone, country);
        user.setUid(uid);
        return user;
    }

    public void setActiveProject(long id, String code, String name) {
        setActiveProject(String.valueOf(id), code, name);
    }

    public void setActiveProject(String id, String code, String name) {
        editor.putString(KEY_ACTIVE_PROJECT_ID, id);
        editor.putString(KEY_ACTIVE_PROJECT_CODE, code);
        editor.putString(KEY_ACTIVE_PROJECT_NAME, name);
        editor.apply();
    }

    public String getActiveProjectId() {
        return pref.getString(KEY_ACTIVE_PROJECT_ID, "PRJ-MCT-2026");
    }

    public String getActiveProjectCode() {
        return pref.getString(KEY_ACTIVE_PROJECT_CODE, "MCT-2026");
    }

    public String getActiveProjectName() {
        return pref.getString(KEY_ACTIVE_PROJECT_NAME, "Metro Central Tower");
    }

    public boolean isWorkerRole() {
        User user = getCurrentUser();
        return user != null && "WORKER".equalsIgnoreCase(user.getRole());
    }

    public long getLastChatAnalysisTimestamp(String projectCode) {
        String key = "last_chat_analysis_" + (projectCode != null ? projectCode : "default");
        return pref.getLong(key, 0L);
    }

    public void setLastChatAnalysisTimestamp(String projectCode, long timestamp) {
        String key = "last_chat_analysis_" + (projectCode != null ? projectCode : "default");
        editor.putLong(key, timestamp).apply();
    }

    public String getProfilePhotoPath() {
        return pref.getString("user_profile_photo_path", null);
    }

    public void setProfilePhotoPath(String path) {
        editor.putString("user_profile_photo_path", path).apply();
    }

    public boolean isFloatingVoiceAlerterEnabled() {
        return pref.getBoolean("floating_voice_alerter_enabled", false);
    }

    public void setFloatingVoiceAlerterEnabled(boolean enabled) {
        editor.putBoolean("floating_voice_alerter_enabled", enabled).apply();
    }

    public void clearSession() {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {}

        boolean remember = isRememberMeEnabled();
        String savedUser = getSavedUsername();

        editor.clear();
        if (remember) {
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_SAVED_USERNAME, savedUser);
        }
        editor.apply();
    }
}
