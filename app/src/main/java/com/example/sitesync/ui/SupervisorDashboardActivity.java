package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivitySupervisorDashboardBinding;
import com.example.sitesync.models.User;

public class SupervisorDashboardActivity extends AppCompatActivity {

    private ActivitySupervisorDashboardBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySupervisorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        User user = (User) getIntent().getSerializableExtra(AuthRouter.EXTRA_USER);
        if (user == null) {
            user = sessionManager.getCurrentUser();
        }

        if (user != null) {
            binding.tvSupervisorName.setText(user.getFullName());
            binding.tvSupervisorUserId.setText("User ID: " + user.getUsername());
        }

        binding.btnSignOutSupervisor.setOnClickListener(v -> confirmSignOut());
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to exit the Site Supervisor dashboard?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(SupervisorDashboardActivity.this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
