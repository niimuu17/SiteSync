package com.example.sitesync.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.databinding.ActivityProgressDetailBinding;
import com.example.sitesync.models.ConstructionActivity;

/**
 * Screen displaying full activity specifications, attached site photos,
 * and a chronological progress history audit trail.
 */
public class ProgressDetailActivity extends AppCompatActivity {

    private ActivityProgressDetailBinding binding;
    private ProgressRepository repository;
    private ProgressHistoryAdapter historyAdapter;
    private String activityId;
    private ConstructionActivity currentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProgressDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ProgressRepository(this);
        activityId = getIntent().getStringExtra(ProgressMonitoringActivity.EXTRA_ACTIVITY_ID);

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActivityDetails();
    }

    private void initViews() {
        historyAdapter = new ProgressHistoryAdapter();
        binding.rvHistoryTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistoryTimeline.setAdapter(historyAdapter);
    }

    private void setupListeners() {
        binding.btnBackFromDetail.setOnClickListener(v -> finish());

        binding.btnDetailUpdateProgress.setOnClickListener(v -> {
            if (currentActivity != null) {
                Intent intent = new Intent(ProgressDetailActivity.this, UpdateProgressActivity.class);
                intent.putExtra(ProgressMonitoringActivity.EXTRA_ACTIVITY_ID, currentActivity.getId());
                startActivity(intent);
            }
        });
    }

    private void loadActivityDetails() {
        currentActivity = repository.getActivityById(activityId);
        if (currentActivity == null) {
            finish();
            return;
        }

        binding.tvDetailActivityId.setText(currentActivity.getId() + " • Field Inspection Log");
        binding.tvDetailProjectName.setText(currentActivity.getProjectName());
        binding.tvDetailActivityName.setText(currentActivity.getActivityName());
        binding.tvDetailLocation.setText(currentActivity.getLocation());
        binding.tvDetailProgressPercent.setText(currentActivity.getProgressPercentage() + "%");
        binding.pbDetailProgress.setProgress(currentActivity.getProgressPercentage());
        binding.tvDetailLastUpdated.setText("Last update recorded: " + currentActivity.getLastUpdated() + " by " + currentActivity.getUpdatedBy());

        // Status badge color
        applyStatusStyle(currentActivity.getStatus(), currentActivity.getProgressPercentage());

        // Remarks
        if (currentActivity.getLatestRemarks() != null && !currentActivity.getLatestRemarks().trim().isEmpty()) {
            binding.tvDetailRemarks.setText(currentActivity.getLatestRemarks());
        } else {
            binding.tvDetailRemarks.setText("No engineering remarks documented yet.");
        }

        // Latest Photo Preview
        if (currentActivity.getLatestPhotoUri() != null && !currentActivity.getLatestPhotoUri().trim().isEmpty()) {
            try {
                binding.ivDetailPhoto.setImageURI(Uri.parse(currentActivity.getLatestPhotoUri()));
                binding.tvDetailPhotoCaption.setText("Photo attached during verified progress update.");
                binding.cardDetailPhoto.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                binding.ivDetailPhoto.setImageResource(R.drawable.ic_construction);
                binding.tvDetailPhotoCaption.setText("Attached site photo (local record).");
            }
        } else {
            binding.ivDetailPhoto.setImageResource(R.drawable.ic_construction);
            binding.tvDetailPhotoCaption.setText("Default site activity reference icon. Tap 'Update Progress' to add actual site photos.");
        }

        // History logs
        historyAdapter.setHistory(currentActivity.getProgressHistory());
    }

    private void applyStatusStyle(String status, int progress) {
        binding.tvDetailStatusBadge.setText(status);

        if (ConstructionActivity.STATUS_COMPLETED.equalsIgnoreCase(status) || progress == 100) {
            binding.tvDetailStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
            binding.tvDetailStatusBadge.setTextColor(Color.parseColor("#15803D"));
            binding.pbDetailProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
        } else if (ConstructionActivity.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
            binding.tvDetailStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
            binding.tvDetailStatusBadge.setTextColor(Color.parseColor("#B45309"));
            binding.pbDetailProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        } else {
            binding.tvDetailStatusBadge.setBackgroundColor(Color.parseColor("#F1F5F9"));
            binding.tvDetailStatusBadge.setTextColor(Color.parseColor("#475569"));
            binding.pbDetailProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
        }
    }
}
