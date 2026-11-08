package com.example.sitesync.data;

import android.os.Handler;
import android.os.Looper;

import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Repository managing authentication requests, mock credentials, and OTP simulation.
 */
public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface OtpCallback {
        void onOtpSent(String generatedOtp, String message);
        void onError(String errorMessage);
    }

    private static class DemoAccount {
        final String username;
        final String password;
        final String fullName;
        final String roleName;

        DemoAccount(String username, String password, String fullName, String roleName) {
            this.username = username;
            this.password = password;
            this.fullName = fullName;
            this.roleName = roleName;
        }
    }

    private final Map<String, DemoAccount> demoAccounts = new HashMap<>();
    private final Map<String, String> pendingOtps = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AuthRepository() {
        // Pre-configured formal construction roles
        demoAccounts.put("manager", new DemoAccount(
                "manager", "sitesync123", "Robert Vance (PMP)", Role.PROJECT_MANAGER.getDisplayName()
        ));
        demoAccounts.put("supervisor", new DemoAccount(
                "supervisor", "sitesync123", "Marcus Chen", Role.SITE_SUPERVISOR.getDisplayName()
        ));
        demoAccounts.put("engineer", new DemoAccount(
                "engineer", "sitesync123", "Elena Rostova (PE)", Role.ENGINEER.getDisplayName()
        ));
    }

    /**
     * Legacy login method for backwards compatibility.
     */
    public void login(final String username, final String password, final AuthCallback callback) {
        loginWithCredentials(username, password, Role.PROJECT_MANAGER, callback);
    }

    /**
     * Authenticates using User ID + Password for a selected role.
     */
    public void loginWithCredentials(final String username, final String password, final Role role, final AuthCallback callback) {
        mainHandler.postDelayed(() -> {
            String cleanUser = username != null ? username.trim().toLowerCase() : "";
            String cleanPass = password != null ? password.trim() : "";

            if (cleanUser.isEmpty()) {
                callback.onError("User ID cannot be empty");
                return;
            }

            if (cleanPass.length() < 6) {
                callback.onError("Password must be at least 6 characters");
                return;
            }

            // Demo accounts matching
            if (demoAccounts.containsKey(cleanUser)) {
                DemoAccount account = demoAccounts.get(cleanUser);
                if (account.password.equals(cleanPass)) {
                    String token = "jwt_" + UUID.randomUUID().toString();
                    callback.onSuccess(new User(
                            account.username,
                            account.fullName,
                            role != null ? role.getDisplayName() : account.roleName,
                            token
                    ));
                    return;
                } else {
                    callback.onError("Incorrect password for " + account.roleName);
                    return;
                }
            }

            // Custom user fallback
            String formattedName = Character.toUpperCase(cleanUser.charAt(0)) + cleanUser.substring(1);
            String token = "jwt_" + UUID.randomUUID().toString();
            callback.onSuccess(new User(
                    cleanUser,
                    formattedName,
                    role != null ? role.getDisplayName() : "Site Staff",
                    token
            ));
        }, 600);
    }

    /**
     * Registers a new user ID with RBAC profile details.
     */
    public void registerUser(final String name, final String country, final String email,
                             final String phone, final String userId, final String password,
                             final Role role, final AuthCallback callback) {
        mainHandler.postDelayed(() -> {
            String token = "jwt_" + UUID.randomUUID().toString();
            User newUser = new User(
                    userId.trim(),
                    name.trim(),
                    role != null ? role.getDisplayName() : Role.PROJECT_MANAGER.getDisplayName(),
                    token,
                    email.trim(),
                    phone.trim(),
                    country.trim()
            );

            // Register into in-memory store
            demoAccounts.put(userId.trim().toLowerCase(), new DemoAccount(
                    userId.trim(),
                    password.trim(),
                    name.trim(),
                    newUser.getRole()
            ));

            callback.onSuccess(newUser);
        }, 700);
    }

    /**
     * Generates and dispatches a mock 6-digit OTP to the phone number.
     */
    public void sendOtp(final String phone, final OtpCallback callback) {
        mainHandler.postDelayed(() -> {
            String cleanPhone = phone != null ? phone.replaceAll("[^0-9+]", "") : "";
            if (cleanPhone.length() < 8) {
                callback.onError("Please enter a valid phone number");
                return;
            }

            // Generate deterministic or random mock 6-digit OTP (e.g., 424242 or random)
            String mockOtp = "123456";
            pendingOtps.put(cleanPhone, mockOtp);

            callback.onOtpSent(mockOtp, "Security code dispatched to " + cleanPhone + ". Test OTP: " + mockOtp);
        }, 500);
    }

    /**
     * Verifies phone number OTP and creates authenticated session.
     */
    public void verifyOtpAndLogin(final String phone, final String enteredOtp, final Role role, final AuthCallback callback) {
        mainHandler.postDelayed(() -> {
            String cleanPhone = phone != null ? phone.replaceAll("[^0-9+]", "") : "";
            String cleanOtp = enteredOtp != null ? enteredOtp.trim() : "";

            if (!pendingOtps.containsKey(cleanPhone) && !"123456".equals(cleanOtp)) {
                callback.onError("No active OTP request found for this phone number. Please request a new code.");
                return;
            }

            String expectedOtp = pendingOtps.getOrDefault(cleanPhone, "123456");
            if (!expectedOtp.equals(cleanOtp) && !"123456".equals(cleanOtp)) {
                callback.onError("Invalid verification code. Please check and try again.");
                return;
            }

            // OTP verified successfully
            pendingOtps.remove(cleanPhone);
            String token = "jwt_otp_" + UUID.randomUUID().toString();
            String autoUser = "phone_" + cleanPhone.substring(Math.max(0, cleanPhone.length() - 4));
            String fullName = (role != null ? role.getDisplayName() : "Site") + " Officer";

            callback.onSuccess(new User(
                    autoUser,
                    fullName,
                    role != null ? role.getDisplayName() : Role.PROJECT_MANAGER.getDisplayName(),
                    token,
                    "",
                    cleanPhone,
                    ""
            ));
        }, 600);
    }
}
