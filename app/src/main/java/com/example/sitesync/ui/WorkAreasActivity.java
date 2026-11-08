package com.example.sitesync.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.databinding.ActivityWorkAreasBinding;

/**
 * Work Areas & Discipline Telemetry Screen (Screen 3 in Visual Reference Spec).
 * Displays full-bleed construction framework background, horizontal segmented tabs,
 * and 2x2 grid with dynamic corner-arc progress gauges.
 */
public class WorkAreasActivity extends AppCompatActivity {

    private ActivityWorkAreasBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkAreasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initGauges();
        setupListeners();
    }

    private void initGauges() {
        // 1. Foundation: 85% (Amber)
        binding.arcFoundation.setProgress(85f);
        binding.arcFoundation.setProgressColor(Color.parseColor("#F7A600"));

        // 2. Structural Work: 62% (Amber)
        binding.arcStructural.setProgress(62f);
        binding.arcStructural.setProgressColor(Color.parseColor("#F7A600"));

        // 3. Electrical: 40% (Amber)
        binding.arcElectrical.setProgress(40f);
        binding.arcElectrical.setProgressColor(Color.parseColor("#F7A600"));

        // 4. Finishing: 18% (Alert Terracotta / Coral Red)
        binding.arcFinishing.setProgress(18f);
        binding.arcFinishing.setProgressColor(Color.parseColor("#E0532B"));
    }

    private void setupListeners() {
        binding.btnBackFromWorkAreas.setOnClickListener(v -> finish());

        binding.btnWorkAreasMenu.setOnClickListener(v ->
                Toast.makeText(this, "Discipline Telemetry Settings & Sensor Sync", Toast.LENGTH_SHORT).show()
        );

        // Segmented Tabs
        binding.tabProgress.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProgressMonitoringActivity.class);
            startActivity(intent);
        });

        binding.tabTimeline.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuildingPlanActivity.class);
            startActivity(intent);
        });

        binding.tabDaily.setOnClickListener(v -> {
            Intent intent = new Intent(this, DailyReportListActivity.class);
            startActivity(intent);
        });

        // 2x2 Grid Cards -> Open Respective Sub-Systems
        binding.cardTelemetryFoundation.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProgressMonitoringActivity.class);
            startActivity(intent);
        });

        binding.cardTelemetryStructural.setOnClickListener(v -> {
            Intent intent = new Intent(this, InspectionListActivity.class);
            startActivity(intent);
        });

        binding.cardTelemetryElectrical.setOnClickListener(v -> {
            Intent intent = new Intent(this, DocumentManagementActivity.class);
            startActivity(intent);
        });

        binding.cardTelemetryFinishing.setOnClickListener(v -> {
            Intent intent = new Intent(this, IssueManagementActivity.class);
            startActivity(intent);
        });
    }
}
