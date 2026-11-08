package com.example.sitesync.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise user profile model representing an authenticated SiteSync user
 * with Role-Based Access Control (RBAC) and Multi-Factor Authentication (2FA) verification state.
 */
public class User implements Serializable {
    private String uid;
    private String username;
    private String fullName;
    private String role;
    private String token;
    private String email;
    private String phoneNumber;
    private String country;
    private boolean twoFactorEnabled;
    private long createdAt;

    public User() {
        this.uid = "";
        this.username = "";
        this.fullName = "";
        this.role = Role.ENGINEER.name();
        this.token = "";
        this.email = "";
        this.phoneNumber = "";
        this.country = "";
        this.twoFactorEnabled = false;
        this.createdAt = System.currentTimeMillis();
    }

    public User(String username, String fullName, String role, String token) {
        this(username, fullName, role, token, "", "", "");
    }

    public User(String username, String fullName, String role, String token,
                String email, String phoneNumber, String country) {
        this.uid = username != null ? username : "";
        this.username = username != null ? username : "";
        this.fullName = fullName != null ? fullName : "";
        this.role = role != null ? role : Role.ENGINEER.name();
        this.token = token != null ? token : "";
        this.email = email != null ? email : "";
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
        this.country = country != null ? country : "";
        this.twoFactorEnabled = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getUid() {
        return (uid != null && !uid.isEmpty()) ? uid : username;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", getUid());
        map.put("username", username);
        map.put("fullName", fullName);
        map.put("role", role);
        map.put("email", email);
        map.put("phoneNumber", phoneNumber);
        map.put("country", country);
        map.put("twoFactorEnabled", twoFactorEnabled);
        map.put("createdAt", createdAt);
        return map;
    }

    public static User fromFirestoreMap(String uid, Map<String, Object> map) {
        if (map == null) return null;
        User user = new User();
        user.setUid(uid);
        user.setUsername((String) map.getOrDefault("username", ""));
        user.setFullName((String) map.getOrDefault("fullName", ""));
        user.setRole((String) map.getOrDefault("role", Role.ENGINEER.name()));
        user.setEmail((String) map.getOrDefault("email", ""));
        user.setPhoneNumber((String) map.getOrDefault("phoneNumber", ""));
        user.setCountry((String) map.getOrDefault("country", ""));
        user.setTwoFactorEnabled(Boolean.TRUE.equals(map.get("twoFactorEnabled")));
        if (map.containsKey("createdAt") && map.get("createdAt") instanceof Long) {
            user.setCreatedAt((Long) map.get("createdAt"));
        }
        return user;
    }
}
