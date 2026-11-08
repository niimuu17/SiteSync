package com.example.sitesync.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityUpdateProgressBinding;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.User;

/**
 * Screen enabling engineers to update an activity's progress percentage,
 * lifecycle status, field remarks, and attach site verification photos.
 */
public class UpdateProgressActivity extends AppCompatActivity {

    private ActivityUpdateProgressBinding binding;
    private ProgressRepository repository;
    private SessionManager sessionManager;
    private ConstructionActivity currentActivity;
    private String selectedPhotoUri = "";

    // Photo picker launcher using modern AndroidX Activity Result API
    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri.toString();
                    showPhotoPreview(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ProgressRepository(this);
        sessionManager = new SessionManager(this);

        String activityId = getIntent().getStringExtra(ProgressMonitoringActivity.EXTRA_ACTIVITY_ID);
        currentActivity = repository.getActivityById(activityId);

        if (currentActivity == null) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateActivityData();
        setupListeners();
    }

    private void populateActivityData() {
        binding.tvUpdateActivityName.setText(currentActivity.getActivityName());
        binding.tvUpdateLocation.setText(currentActivity.getLocation() + " • " + currentActivity.getProjectName());

        // Set initial percentage
        int progress = currentActivity.getProgressPercentage();
        binding.sbProgress.setProgress(progress);
        binding.tvProgressValueDisplay.setText(progress + "%");

        // Set initial status radio button
        if (ConstructionActivity.STATUS_COMPLETED.equalsIgnoreCase(currentActivity.getStatus())) {
            binding.rbStatusCompleted.setChecked(true);
        } else if (ConstructionActivity.STATUS_PENDING.equalsIgnoreCase(currentActivity.getStatus())) {
            binding.rbStatusPending.setChecked(true);
        } else {
            binding.rbStatusInProgress.setChecked(true);
        }

        // Set initial remarks
        if (currentActivity.getLatestRemarks() != null) {
            binding.etRemarks.setText(currentActivity.getLatestRemarks());
        }

        // Set existing photo if available
        if (currentActivity.getLatestPhotoUri() != null && !currentActivity.getLatestPhotoUri().isEmpty()) {
            selectedPhotoUri = currentActivity.getLatestPhotoUri();
            showPhotoPreview(Uri.parse(selectedPhotoUri));
        }
    }

    private void setupListeners() {
        binding.btnBackFromUpdate.setOnClickListener(v -> finish());

        // SeekBar listener for percentage changes
        binding.sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvProgressValueDisplay.setText(progress + "%");

                if (fromUser) {
                    // Auto-adjust status recommendation
                    if (progress == 100) {
                        binding.rbStatusCompleted.setChecked(true);
                    } else if (progress == 0) {
                        binding.rbStatusPending.setChecked(true);
                    } else if (!binding.rbStatusInProgress.isChecked()) {
                        binding.rbStatusInProgress.setChecked(true);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Launch photo picker
        binding.btnSelectPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        // Remove photo
        binding.btnRemovePhoto.setOnClickListener(v -> {
            selectedPhotoUri = "";
            binding.layoutPhotoPreview.setVisibility(View.GONE);
            binding.ivPhotoPreview.setImageURI(null);
        });

        // Save progress update
        binding.btnSaveProgress.setOnClickListener(v -> saveProgressUpdate());
    }

    private void showPhotoPreview(Uri uri) {
        try {
            binding.ivPhotoPreview.setImageURI(uri);
            binding.layoutPhotoPreview.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            binding.layoutPhotoPreview.setVisibility(View.GONE);
        }
    }

    private void saveProgressUpdate() {
        int progress = binding.sbProgress.getProgress();

        String status = ConstructionActivity.STATUS_IN_PROGRESS;
        int checkedStatusId = binding.rgStatus.getCheckedRadioButtonId();
        if (checkedStatusId == binding.rbStatusCompleted.getId()) {
            status = ConstructionActivity.STATUS_COMPLETED;
        } else if (checkedStatusId == binding.rbStatusPending.getId()) {
            status = ConstructionActivity.STATUS_PENDING;
        }

        String remarks = binding.etRemarks.getText() != null ? binding.etRemarks.getText().toString().trim() : "";

        // Engineer identity from active session
        User currentUser = sessionManager.getCurrentUser();
        String engineerName = currentUser != null ? currentUser.getFullName() : "Field Engineer";

        boolean success = repository.updateProgress(
                currentActivity.getId(),
                progress,
                status,
                remarks,
                selectedPhotoUri,
                engineerName
        );

        if (success) {
            Toast.makeText(this, "Progress updated to " + progress + "% (" + status + ")", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update progress", Toast.LENGTH_SHORT).show();
        }
    }
}
