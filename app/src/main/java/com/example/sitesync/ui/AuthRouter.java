package com.example.sitesync.ui;

import android.content.Context;
import android.content.Intent;

import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;
import com.example.sitesync.ui.cockpit.UnifiedSiteCockpitActivity;

/**
 * Modular router utility for handling screen transitions.
 * Under Single-Module Architecture, all authenticated roles land directly in UnifiedSiteCockpitActivity.
 */
public final class AuthRouter {

    public static final String EXTRA_USER = "extra_user";
    public static final String EXTRA_ROLE = "extra_role";

    private AuthRouter() {}

    /**
     * Dispatches user directly to the Unified Command Cockpit.
     */
    public static void routeToDashboard(Context context, Role role, User user) {
        if (role == null) {
            role = Role.ENGINEER;
        }

        Intent intent = new Intent(context, UnifiedSiteCockpitActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (user != null) {
            intent.putExtra(EXTRA_USER, user);
        }
        intent.putExtra(EXTRA_ROLE, role.name());
        context.startActivity(intent);
    }
}
