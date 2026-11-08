package com.example.sitesync.models;

import java.io.Serializable;

/**
 * Model representing an authenticated SiteSync user with Role-Based Access details.
 */
public class User implements Serializable {
    private final String username;
    private final String fullName;
    private final String role;
    private final String token;
    private final String email;
    private final String phoneNumber;
    private final String country;

    public User(String username, String fullName, String role, String token) {
        this(username, fullName, role, token, "", "", "");
    }

    public User(String username, String fullName, String role, String token,
                String email, String phoneNumber, String country) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.token = token;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.country = country;
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

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCountry() {
        return country;
    }
}
