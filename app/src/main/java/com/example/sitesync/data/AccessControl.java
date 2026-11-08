package com.example.sitesync.data;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sitesync.R;
import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.button.MaterialButton;

/**
 * Enterprise Capability-Based Access Control (CBAC) Engine.
 * Replaces screen silos with dynamic capability checks and permission elevation.
 */
public final class AccessControl {

    private AccessControl() {}

    public static Role getRole(User user) {
        if (user == null || user.getRole() == null) {
            return Role.ENGINEER;
        }
        return Role.fromString(user.getRole());
    }

    public static boolean isProjectManager(User user) {
        return getRole(user) == Role.PROJECT_MANAGER;
    }

    public static boolean isSupervisor(User user) {
        return getRole(user) == Role.SITE_SUPERVISOR;
    }

    public static boolean isEngineer(User user) {
        return getRole(user) == Role.ENGINEER;
    }

    /**
     * Capability: Approve milestone or sign-off critical schedule adjustments.
     * Allowed: PROJECT_MANAGER
     */
    public static boolean canApproveMilestone(User user) {
        return isProjectManager(user);
    }

    /**
     * Capability: Sign-off or close QA inspection checklists.
     * Allowed: PROJECT_MANAGER, SITE_SUPERVISOR
     */
    public static boolean canSignOffInspection(User user) {
        Role role = getRole(user);
        return role == Role.PROJECT_MANAGER || role == Role.SITE_SUPERVISOR;
    }

    /**
     * Capability: Submit official Daily Site Reports.
     * Allowed: All authenticated field staff.
     */
    public static boolean canSubmitDailyReport(User user) {
        return user != null;
    }

    /**
     * Capability: Delete records, reports, or CAD revisions.
     * Allowed: PROJECT_MANAGER only.
     */
    public static boolean canDeleteRecord(User user) {
        return isProjectManager(user);
    }

    /**
     * Capability: Approve CAD blueprint revisions.
     * Allowed: PROJECT_MANAGER
     */
    public static boolean canApproveRevision(User user) {
        return isProjectManager(user);
    }

    /**
     * Capability: Trigger QA Re-inspection workflow.
     * Allowed: PROJECT_MANAGER, SITE_SUPERVISOR
     */
    public static boolean canTriggerReinspection(User user) {
        Role role = getRole(user);
        return role == Role.PROJECT_MANAGER || role == Role.SITE_SUPERVISOR;
    }

    /**
     * Capability: Update progress percentage on active tasks.
     * Allowed: All authenticated field staff.
     */
    public static boolean canUpdateProgress(User user) {
        return user != null;
    }

    /**
     * Capability: Log technical issues or RFI clarification tickets.
     * Allowed: All authenticated field staff.
     */
    public static boolean canCreateIssue(User user) {
        return user != null;
    }

    /**
     * Enforces capability check. If allowed, runs onPermitted.
     * If denied, presents industrial dark elevation prompt.
     */
    public static void checkOrPromptElevation(Context context, User user, String actionName,
                                              String requiredRole, Runnable onPermitted) {
        boolean permitted = false;
        if ("Approve Milestone".equalsIgnoreCase(actionName) || "Approve Revision".equalsIgnoreCase(actionName) || "Delete Record".equalsIgnoreCase(actionName)) {
            permitted = canApproveMilestone(user);
        } else if ("Sign-Off Inspection".equalsIgnoreCase(actionName) || "Trigger Re-inspection".equalsIgnoreCase(actionName)) {
            permitted = canSignOffInspection(user);
        } else {
            permitted = true;
        }

        if (permitted) {
            if (onPermitted != null) onPermitted.run();
        } else {
            showElevationDialog(context, actionName, requiredRole);
        }
    }

    /**
     * High-contrast industrial elevation dialog for restricted capabilities.
     */
    public static void showElevationDialog(Context context, String actionName, String requiredRole) {
        HapticFeedbackUtil.heavyClick(context, null);

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_permission_elevation, null, false);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvElevationTitle);
        TextView tvDescription = dialogView.findViewById(R.id.tvElevationDesc);
        MaterialButton btnRequestElevation = dialogView.findViewById(R.id.btnRequestElevation);
        MaterialButton btnDismiss = dialogView.findViewById(R.id.btnElevationDismiss);

        if (tvTitle != null) {
            tvTitle.setText("Sign-Off Required: " + actionName);
        }
        if (tvDescription != null) {
            tvDescription.setText("This action requires " + requiredRole + " capability credentials under jobsite compliance guidelines.");
        }

        if (btnRequestElevation != null) {
            btnRequestElevation.setOnClickListener(v -> {
                HapticFeedbackUtil.lightClick(context, v);
                Toast.makeText(context, "Elevation request dispatched to Site Supervisor / PM", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
