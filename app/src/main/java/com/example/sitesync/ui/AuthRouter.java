package com.example.sitesync.ui;

import android.content.Context;
import android.content.Intent;

import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;

/**
 * Modular router utility for handling Role-Based Access Control (RBAC) screen transitions.
 */
public final class AuthRouter {

    public static final String EXTRA_USER = "extra_user";
    public static final String EXTRA_ROLE = "extra_role";

    private AuthRouter() {}

    /**
     * Dispatches user to their dedicated role dashboard based on selected or authenticated Role.
     */
    public static void routeToDashboard(Context context, Role role, User user) {
        if (role == null) {
            role = Role.PROJECT_MANAGER;
        }

        Class<?> targetActivity;
        switch (role) {
            case PROJECT_MANAGER:
                targetActivity = ExecutiveProgressActivity.class;
                break;
            case SITE_SUPERVISOR:
                targetActivity = SupervisorDashboardActivity.class;
                break;
            case ENGINEER:
                targetActivity = EngineerDashboardActivity.class;
                break;
            default:
                targetActivity = ExecutiveProgressActivity.class;
                break;
        }

        Intent intent = new Intent(context, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (user != null) {
            intent.putExtra(EXTRA_USER, user);
        }
        intent.putExtra(EXTRA_ROLE, role.name());
        context.startActivity(intent);
    }
}
