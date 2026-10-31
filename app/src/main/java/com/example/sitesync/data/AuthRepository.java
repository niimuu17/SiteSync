package com.example.sitesync.data;

import android.os.Handler;
import android.os.Looper;

import com.example.sitesync.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Repository managing authentication requests and mock enterprise site credentials.
 */
public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    private static class DemoAccount {
        final String username;
        final String password;
        final String fullName;
        final String role;

        DemoAccount(String username, String password, String fullName, String role) {
            this.username = username;
            this.password = password;
            this.fullName = fullName;
            this.role = role;
        }
    }

    private final Map<String, DemoAccount> demoAccounts = new HashMap<>();

    public AuthRepository() {
        // Pre-configured formal construction roles
        demoAccounts.put("manager", new DemoAccount(
                "manager", "sitesync123", "Robert Vance (PMP)", "Project Manager"
        ));
        demoAccounts.put("engineer", new DemoAccount(
                "engineer", "sitesync123", "Elena Rostova (PE)", "Lead Field Engineer"
        ));
        demoAccounts.put("supervisor", new DemoAccount(
                "supervisor", "sitesync123", "Marcus Chen", "Site Safety Supervisor"
        ));
    }

    /**
     * Authenticates user against enterprise credentials.
     * Simulates network latency asynchronously via Handler.
     */
    public void login(final String username, final String password, final AuthCallback callback) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                String cleanUser = username != null ? username.trim().toLowerCase() : "";
                String cleanPass = password != null ? password.trim() : "";

                if (cleanUser.isEmpty()) {
                    callback.onError("Username cannot be empty");
                    return;
                }

                if (cleanPass.length() < 6) {
                    callback.onError("Password must be at least 6 characters");
                    return;
                }

                // Check demo accounts first
                if (demoAccounts.containsKey(cleanUser)) {
                    DemoAccount account = demoAccounts.get(cleanUser);
                    if (account.password.equals(cleanPass)) {
                        String token = "jwt_" + UUID.randomUUID().toString();
                        callback.onSuccess(new User(account.username, account.fullName, account.role, token));
                    } else {
                        callback.onError("Incorrect password for " + account.role + " account.");
                    }
                    return;
                }

                // Generic fallback for custom usernames during field evaluation
                String formattedName = Character.toUpperCase(cleanUser.charAt(0)) + cleanUser.substring(1);
                String token = "jwt_" + UUID.randomUUID().toString();
                callback.onSuccess(new User(cleanUser, formattedName, "Site Operations Engineer", token));
            }
        }, 700); // 700ms realistic network delay
    }
}
