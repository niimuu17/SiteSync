package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.databinding.ActivityExecutiveProgressBinding;

/**
 * Executive Project Progress Dashboard (Screen 1 in Visual Reference Spec).
 * Displays high-level centralized construction telemetry, striped hazard progress bar,
 * cut-and-fill elevation candlestick chart, and floating frosted bottom dock.
 */
public class ExecutiveProgressActivity extends AppCompatActivity {

    private ActivityExecutiveProgressBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExecutiveProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initTelemetry();
        setupListeners();
    }

    private void initTelemetry() {
        // Set up the custom Striped Hazard Progress Bar
        binding.stripedProgressBar.setProgress(68f);
        binding.stripedProgressBar.setDelayLabel("Delay 126");

        binding.tvWorkProgressPercent.setText("68");
        binding.tvStartDate.setText("11/02/2026");
        binding.tvEndDate.setText("23/03/2026");
    }

    private void setupListeners() {
        // Switch to Screen 2: Building Plan
        binding.btnGoBuildingPlan.setOnClickListener(v -> {
            Intent intent = new Intent(ExecutiveProgressActivity.this, BuildingPlanActivity.class);
            startActivity(intent);
        });

        // Switch to Screen 3: Work Areas
        binding.btnGoWorkAreas.setOnClickListener(v -> {
            Intent intent = new Intent(ExecutiveProgressActivity.this, WorkAreasActivity.class);
            startActivity(intent);
        });

        // Notifications & Grid buttons
        binding.btnNotifications.setOnClickListener(v ->
                Toast.makeText(this, "All construction systems operational (2 notifications)", Toast.LENGTH_SHORT).show()
        );

        binding.btnModuleGrid.setOnClickListener(v -> {
            Intent intent = new Intent(this, EngineerDashboardActivity.class);
            startActivity(intent);
        });

        // Toggles
        binding.btnToggleCalendar.setOnClickListener(v ->
                Toast.makeText(this, "Switched to Milestone Calendar View", Toast.LENGTH_SHORT).show()
        );

        binding.btnToggleChart.setOnClickListener(v ->
                Toast.makeText(this, "Switched to Cumulative Burn-down Chart", Toast.LENGTH_SHORT).show()
        );

        binding.btnOpenCutFillDetail.setOnClickListener(v ->
                Toast.makeText(this, "Earthwork Telemetry: 224,400 cu yd net balance verified", Toast.LENGTH_SHORT).show()
        );

        // Bottom Dock Interactions
        binding.dockHome.setOnClickListener(v -> {
            // Already at home
        });

        binding.dockCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(this, DailyReportListActivity.class);
            startActivity(intent);
        });

        binding.dockPie.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProgressMonitoringActivity.class);
            startActivity(intent);
        });

        binding.dockAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkAreasActivity.class);
            startActivity(intent);
        });

        binding.dockProfile.setOnClickListener(v -> {
            Toast.makeText(this, "SiteSync Profile & Role Permissions verified", Toast.LENGTH_SHORT).show();
        });
    }
}
