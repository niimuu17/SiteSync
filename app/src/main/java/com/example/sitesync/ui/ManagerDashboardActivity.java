package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityManagerDashboardBinding;
import com.example.sitesync.models.User;

public class ManagerDashboardActivity extends AppCompatActivity {

    private ActivityManagerDashboardBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        User user = (User) getIntent().getSerializableExtra(AuthRouter.EXTRA_USER);
        if (user == null) {
            user = sessionManager.getCurrentUser();
        }

        if (user != null) {
            binding.tvManagerName.setText(user.getFullName());
            binding.tvManagerUserId.setText("User ID: " + user.getUsername());
        }

        binding.btnSignOutManager.setOnClickListener(v -> confirmSignOut());
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to exit the Project Manager dashboard?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(ManagerDashboardActivity.this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
