package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityMainBinding;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Screen 3: Main Dashboard (Default: Progress Tab)
 * Hosts the Global App Header with 3-dot overflow project actions,
 * a 5-tab Bottom Navigation dock, and swaps between modular fragments.
 */
public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private String currentProjectTitle = "Metro Central Tower";

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

        setupTopAppBar();
        setupBottomNavigation(savedInstanceState);
    }

    private void setupTopAppBar() {
        binding.tvProjectTitle.setText(currentProjectTitle);
        binding.tvProjectSubtitle.setText("Floor Level: Level 3 (Auto)");

        binding.btnNotificationsHeader.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            Toast.makeText(this, "All systems nominal • Zero OSHA safety alerts", Toast.LENGTH_SHORT).show();
        });

        // 3-Dot Overflow Menu
        binding.ivTopMenu.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            showTopProjectMenu(v);
        });
    }

    private void showTopProjectMenu(android.view.View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.top_project_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_create_project) {
                showCreateProjectDialog();
                return true;
            } else if (id == R.id.menu_project_details) {
                showProjectDetailsModal();
                return true;
            } else if (id == R.id.menu_log_out) {
                confirmSignOut();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showCreateProjectDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        final EditText etCode = new EditText(this);
        etCode.setHint("Project Code (e.g. MCT-2026)");
        etCode.setBackgroundResource(R.drawable.bg_rounded_input);
        etCode.setPadding(32, 24, 32, 24);
        etCode.setTextColor(getColor(R.color.text_oxide_primary));
        etCode.setHintTextColor(getColor(R.color.text_oxide_muted));
        layout.addView(etCode);

        final EditText etName = new EditText(this);
        etName.setHint("Project Name (e.g. Metro Central Tower)");
        etName.setBackgroundResource(R.drawable.bg_rounded_input);
        etName.setPadding(32, 24, 32, 24);
        etName.setTextColor(getColor(R.color.text_oxide_primary));
        etName.setHintTextColor(getColor(R.color.text_oxide_muted));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 20;
        etName.setLayoutParams(lp);
        layout.addView(etName);

        new AlertDialog.Builder(this)
                .setTitle("Create New Project")
                .setView(layout)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        currentProjectTitle = name;
                        binding.tvProjectTitle.setText(currentProjectTitle);
                        Toast.makeText(this, "Active Project: " + name, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProjectDetailsModal() {
        String details = "Client: Metro Urban Development Corp.\n" +
                "General Contractor: Skyline Infrastructure Ltd.\n" +
                "Location: 40.7128° N, 74.0060° W (New York, NY)\n" +
                "Total Budget: $42.5M USD\n" +
                "Current Completion: 68% (Ahead of schedule by 2.2%)\n" +
                "Active Floor Level: Level 3 (Perimeter Concrete Columns)";

        new AlertDialog.Builder(this)
                .setTitle("Project Details: " + currentProjectTitle)
                .setMessage(details)
                .setPositiveButton("Close", null)
                .show();
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to end your active session?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    sessionManager.clearSession();
                    redirectToLogin();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation(Bundle savedInstanceState) {
        binding.bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            // Default active tab is Progress
            binding.bottomNavigationView.setSelectedItemId(R.id.nav_progress);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int id = item.getItemId();

        if (id == R.id.nav_progress) {
            selectedFragment = ProgressFragment.newInstance();
        } else if (id == R.id.nav_task) {
            selectedFragment = TaskFragment.newInstance();
        } else if (id == R.id.nav_resource) {
            selectedFragment = ResourceFragment.newInstance();
        } else if (id == R.id.nav_security) {
            selectedFragment = SecurityFragment.newInstance();
        } else if (id == R.id.nav_settings) {
            selectedFragment = SettingsFragment.newInstance();
        }

        if (selectedFragment != null) {
            HapticFeedbackUtil.lightClick(this, null);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit();
            return true;
        }

        return false;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
