package com.example.sitesync.models;

/**
 * Enumeration representing the supported user roles in SiteSync Role-Based Access Control (RBAC).
 */
public enum Role {
    PROJECT_MANAGER(
            "Project Manager",
            "Project oversight, budget allocation, schedule approvals, and milestone sign-offs."
    ),
    SITE_SUPERVISOR(
            "Site Supervisor",
            "Daily field workforce check-in, safety logs, hazard reporting, and shift monitoring."
    ),
    ENGINEER(
            "Engineer",
            "Blueprint validation, telemetry synchronization, material tests, and structural notes."
    );

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolves a Role instance safely from a role name or key.
     */
    public static Role fromString(String value) {
        if (value == null) return PROJECT_MANAGER;
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(value) || role.displayName.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return PROJECT_MANAGER;
    }
}
