package com.example.sitesync.models;

import java.io.Serializable;

/**
 * Model representing an authenticated SiteSync user.
 */
public class User implements Serializable {
    private final String username;
    private final String fullName;
    private final String role;
    private final String token;

    public User(String username, String fullName, String role, String token) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}
