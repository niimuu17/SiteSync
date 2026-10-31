package com.example.sitesync.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sitesync.models.User;

/**
 * Manages persistent user session and remember-me credentials.
 */
public class SessionManager {
    private static final String PREF_NAME = "SiteSyncPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_ROLE = "role";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_SAVED_USERNAME = "savedUsername";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
    }

    public void createLoginSession(User user, boolean rememberMe) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_TOKEN, user.getToken());
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);

        if (rememberMe) {
            editor.putString(KEY_SAVED_USERNAME, user.getUsername());
        } else {
            editor.remove(KEY_SAVED_USERNAME);
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
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
        String username = pref.getString(KEY_USERNAME, "");
        String fullName = pref.getString(KEY_FULL_NAME, "");
        String role = pref.getString(KEY_ROLE, "Field Staff");
        String token = pref.getString(KEY_TOKEN, "");
        return new User(username, fullName, role, token);
    }

    public void clearSession() {
        boolean remember = pref.getBoolean(KEY_REMEMBER_ME, false);
        String savedUser = pref.getString(KEY_SAVED_USERNAME, "");

        editor.clear();
        if (remember) {
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_SAVED_USERNAME, savedUser);
        }
        editor.apply();
    }
}
