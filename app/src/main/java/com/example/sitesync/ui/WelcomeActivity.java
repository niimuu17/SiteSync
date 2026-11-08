package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityWelcomeBinding;
import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;

/**
 * Welcome screen presenting the 3 primary construction site roles.
 * Selecting a role forwards it via Intent to the Auth screen.
 */
public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Check if an authenticated session already exists
        if (sessionManager.isLoggedIn()) {
            User currentUser = sessionManager.getCurrentUser();
            Role savedRole = Role.fromString(currentUser != null ? currentUser.getRole() : null);
            AuthRouter.routeToDashboard(this, savedRole, currentUser);
            finish();
            return;
        }

        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRoleClickListeners();
    }

    private void setupRoleClickListeners() {
        // Project Manager selection
        binding.cardProjectManager.setOnClickListener(v -> selectRole(Role.PROJECT_MANAGER));
        binding.btnSelectManager.setOnClickListener(v -> selectRole(Role.PROJECT_MANAGER));

        // Site Supervisor selection
        binding.cardSiteSupervisor.setOnClickListener(v -> selectRole(Role.SITE_SUPERVISOR));
        binding.btnSelectSupervisor.setOnClickListener(v -> selectRole(Role.SITE_SUPERVISOR));

        // Field Engineer selection
        binding.cardEngineer.setOnClickListener(v -> selectRole(Role.ENGINEER));
        binding.btnSelectEngineer.setOnClickListener(v -> selectRole(Role.ENGINEER));

        // Quick Direct Access into Unified Cockpit
        if (binding.tvDirectLogin != null) {
            binding.tvDirectLogin.setOnClickListener(v -> {
                User demoUser = new User("engineer", "Elena Rostova", Role.ENGINEER.name(), "TOKEN-DEMO-01", "elena@sitesync.io", "+1-555-0192", "US");
                sessionManager.createLoginSession(demoUser, true);
                AuthRouter.routeToDashboard(this, Role.ENGINEER, demoUser);
                finish();
            });
        }
    }

    /**
     * Passes the selected Role enum to the authentication screen.
     */
    private void selectRole(Role role) {
        Intent intent = new Intent(WelcomeActivity.this, AuthActivity.class);
        intent.putExtra(AuthActivity.EXTRA_ROLE, role.name());
        startActivity(intent);
    }
}
