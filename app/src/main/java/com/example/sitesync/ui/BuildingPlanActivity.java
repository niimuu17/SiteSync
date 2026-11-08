package com.example.sitesync.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.databinding.ActivityBuildingPlanBinding;

/**
 * Interactive Building Plan & Spatial Inspection View (Screen 2 in Visual Reference Spec).
 * Renders the architectural cross-section with interactive hotspot pins,
 * barcode-style progress indicator, and bottom timeline strip card.
 */
public class BuildingPlanActivity extends AppCompatActivity {

    private ActivityBuildingPlanBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBuildingPlanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        setupListeners();
    }

    private void initViews() {
        // Set Barcode Progress on Floating Card
        binding.barcodeProgressView.setProgress(53f);
        binding.barcodeProgressView.setColors(Color.parseColor("#F7A600"), Color.parseColor("#4A4C50"));

        // Set Timeline segment 1 barcode progress (all completed in white)
        binding.timelineSegment1.setProgress(100f);
        binding.timelineSegment1.setColors(Color.parseColor("#FFFFFF"), Color.parseColor("#555555"));
    }

    private void setupListeners() {
        binding.btnBackFromPlan.setOnClickListener(v -> finish());

        binding.btnPlanMenu.setOnClickListener(v ->
                Toast.makeText(this, "Building 2 Floor Plan Options & CAD Layer Filters", Toast.LENGTH_SHORT).show()
        );

        binding.cardHotspotFurnishing.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProgressDetailActivity.class);
            startActivity(intent);
        });

        binding.cardBottomTimeline.setOnClickListener(v -> {
            Intent intent = new Intent(this, DocumentManagementActivity.class);
            startActivity(intent);
        });
    }
}
