package com.example.sitesync.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.InspectionRepository;
import com.example.sitesync.databinding.ActivityInspectionDetailBinding;
import com.example.sitesync.models.SiteInspection;

/**
 * Screen displaying full QA/QC Inspection details, checklist verification results,
 * photo evidence, and full history of initial inspection and re-inspection attempts.
 */
public class InspectionDetailActivity extends AppCompatActivity {

    private ActivityInspectionDetailBinding binding;
    private InspectionRepository repository;
    private InspectionChecklistAdapter checklistAdapter;
    private InspectionHistoryAdapter historyAdapter;
    private String inspectionId;
    private SiteInspection currentInspection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInspectionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new InspectionRepository(this);
        inspectionId = getIntent().getStringExtra(InspectionListActivity.EXTRA_INSPECTION_ID);

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInspectionDetails();
    }

    private void initViews() {
        // Read-only checklist
        checklistAdapter = new InspectionChecklistAdapter(true);
        binding.rvDetailChecklist.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDetailChecklist.setAdapter(checklistAdapter);

        // History timeline
        historyAdapter = new InspectionHistoryAdapter();
        binding.rvInspHistoryTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvInspHistoryTimeline.setAdapter(historyAdapter);
    }

    private void setupListeners() {
        binding.btnBackFromInspDetail.setOnClickListener(v -> finish());

        // Re-inspection triggers
        binding.btnBannerReInspect.setOnClickListener(v -> launchReInspection());
        binding.btnBottomReInspect.setOnClickListener(v -> launchReInspection());
    }

    private void launchReInspection() {
        if (currentInspection != null) {
            Intent intent = new Intent(InspectionDetailActivity.this, ReInspectionActivity.class);
            intent.putExtra(InspectionListActivity.EXTRA_INSPECTION_ID, currentInspection.getId());
            startActivity(intent);
        }
    }

    private void loadInspectionDetails() {
        currentInspection = repository.getInspectionById(inspectionId);
        if (currentInspection == null) {
            finish();
            return;
        }

        binding.tvDetailInspectionIdSubtitle.setText(currentInspection.getId() + " • QA/QC Verification Record");
        binding.tvDetailInspProject.setText(currentInspection.getProjectName());
        binding.tvDetailInspType.setText(currentInspection.getInspectionType());
        binding.tvDetailInspLocation.setText(currentInspection.getLocation());
        binding.tvDetailInspDateInspector.setText("Inspection Date: " + currentInspection.getDate() + " • Lead: " + currentInspection.getInspectorName());

        // Status badge styling
        applyStatusStyle(currentInspection.getStatus());

        // Re-inspection requirements visibility
        boolean isFailed = SiteInspection.STATUS_FAILED.equalsIgnoreCase(currentInspection.getStatus());
        binding.cardReinspectionBanner.setVisibility(isFailed ? View.VISIBLE : View.GONE);
        binding.layoutBottomReInspect.setVisibility(isFailed ? View.VISIBLE : View.GONE);

        // Checklist items
        checklistAdapter.setChecklist(currentInspection.getChecklist());

        // Observations & Remarks
        if (currentInspection.getObservations() != null && !currentInspection.getObservations().isEmpty()) {
            binding.tvDetailObservations.setText(currentInspection.getObservations());
        } else {
            binding.tvDetailObservations.setText("No specific observations documented.");
        }

        if (currentInspection.getRemarks() != null && !currentInspection.getRemarks().isEmpty()) {
            binding.tvDetailRemarks.setText(currentInspection.getRemarks());
        } else {
            binding.tvDetailRemarks.setText("No corrective measures recorded.");
        }

        // Photo display
        if (currentInspection.getPhotoUri() != null && !currentInspection.getPhotoUri().isEmpty()) {
            try {
                binding.ivDetailInspPhoto.setImageURI(Uri.parse(currentInspection.getPhotoUri()));
                binding.tvDetailInspPhotoCaption.setText("Photographic record attached during site audit.");
            } catch (Exception e) {
                binding.ivDetailInspPhoto.setImageResource(R.drawable.ic_construction);
                binding.tvDetailInspPhotoCaption.setText("Site inspection record (local reference).");
            }
        } else {
            binding.ivDetailInspPhoto.setImageResource(R.drawable.ic_construction);
            binding.tvDetailInspPhotoCaption.setText("Default inspection icon. Attach site photos during creation or re-inspection.");
        }

        // History logs
        historyAdapter.setRecords(currentInspection.getHistory());
    }

    private void applyStatusStyle(String status) {
        binding.tvDetailInspStatusBadge.setText(status);

        if (SiteInspection.STATUS_PASSED.equalsIgnoreCase(status)) {
            binding.tvDetailInspStatusBadge.setBackgroundColor(Color.parseColor("#123324"));
            binding.tvDetailInspStatusBadge.setTextColor(Color.parseColor("#10B981"));
        } else if (SiteInspection.STATUS_FAILED.equalsIgnoreCase(status)) {
            binding.tvDetailInspStatusBadge.setBackgroundColor(Color.parseColor("#381E1E"));
            binding.tvDetailInspStatusBadge.setTextColor(Color.parseColor("#EF4444"));
        } else {
            binding.tvDetailInspStatusBadge.setBackgroundColor(Color.parseColor("#382B14"));
            binding.tvDetailInspStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
        }
    }
}
