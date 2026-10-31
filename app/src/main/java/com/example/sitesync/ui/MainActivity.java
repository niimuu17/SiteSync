package com.example.sitesync.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityMainBinding;
import com.example.sitesync.models.User;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        populateUserData();
        setupListeners();
    }

    private void populateUserData() {
        User user = sessionManager.getCurrentUser();
        if (user != null) {
            binding.tvUserFullName.setText(user.getFullName());
            binding.tvUserRoleBadge.setText(user.getRole());
        }
    }

    private void setupListeners() {
        binding.btnSignOut.setOnClickListener(v -> confirmSignOut());
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to end your current site session?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    sessionManager.clearSession();
                    redirectToLogin();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
