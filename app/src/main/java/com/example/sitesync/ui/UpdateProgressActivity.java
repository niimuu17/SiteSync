package com.example.sitesync.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityUpdateProgressBinding;
import com.example.sitesync.hardware.MatchAngleCameraDialog;
import com.example.sitesync.hardware.VoiceDictationManager;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.io.File;

/**
 * Screen enabling engineers to update an activity's progress percentage,
 * lifecycle status, field remarks via "Speak Note" hands-free voice dictation,
 * and attach site verification photos via "Match Angle" comparison camera.
 */
public class UpdateProgressActivity extends AppCompatActivity {

    private ActivityUpdateProgressBinding binding;
    private ProgressRepository repository;
    private SessionManager sessionManager;
    private ConstructionActivity currentActivity;
    private String selectedPhotoUri = "";

    private VoiceDictationManager voiceDictationManager;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri.toString();
                    showPhotoPreview(uri);
                }
            });

    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startDictation();
                } else {
                    Toast.makeText(this, "Microphone permission is required to Speak Note", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchMatchAngleCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required for Match Angle", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ProgressRepository(this);
        sessionManager = new SessionManager(this);
        voiceDictationManager = new VoiceDictationManager(this);

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

        int progress = currentActivity.getProgressPercentage();
        binding.sbProgress.setProgress(progress);
        binding.tvProgressValueDisplay.setText(progress + "%");

        if (ConstructionActivity.STATUS_COMPLETED.equalsIgnoreCase(currentActivity.getStatus())) {
            binding.rbStatusCompleted.setChecked(true);
        } else if (ConstructionActivity.STATUS_PENDING.equalsIgnoreCase(currentActivity.getStatus())) {
            binding.rbStatusPending.setChecked(true);
        } else {
            binding.rbStatusInProgress.setChecked(true);
        }

        if (currentActivity.getLatestRemarks() != null) {
            binding.etRemarks.setText(currentActivity.getLatestRemarks());
        }

        if (currentActivity.getLatestPhotoUri() != null && !currentActivity.getLatestPhotoUri().isEmpty()) {
            selectedPhotoUri = currentActivity.getLatestPhotoUri();
            showPhotoPreview(Uri.parse(selectedPhotoUri));
        }
    }

    private void setupListeners() {
        binding.btnBackFromUpdate.setOnClickListener(v -> finish());

        binding.sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvProgressValueDisplay.setText(progress + "%");
                if (fromUser) {
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

        // Feature 1: "Speak Note" Hands-Free Microphone Dictation
        binding.btnDictateRemarks.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            if (voiceDictationManager.isListening()) {
                voiceDictationManager.stopListening();
                setMicUiActive(false);
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startDictation();
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                }
            }
        });

        // Feature 3: "Match Angle" Comparison Camera
        binding.btnMatchAngle.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchMatchAngleCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.btnSelectPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        binding.btnRemovePhoto.setOnClickListener(v -> {
            selectedPhotoUri = "";
            binding.layoutPhotoPreview.setVisibility(View.GONE);
            binding.ivPhotoPreview.setImageURI(null);
        });

        binding.btnSaveProgress.setOnClickListener(v -> saveProgressUpdate());
    }

    private void startDictation() {
        setMicUiActive(true);
        String initialRemarks = binding.etRemarks.getText() != null ? binding.etRemarks.getText().toString() : "";

        voiceDictationManager.startListening(new VoiceDictationManager.OnDictationListener() {
            @Override
            public void onSpeechRmsChanged(float rmsdB) {}

            @Override
            public void onPartialTranscription(String partialText) {
                if (binding.etRemarks != null) {
                    String combined = initialRemarks.isEmpty() ? partialText : initialRemarks + " " + partialText;
                    binding.etRemarks.setText(combined);
                    binding.etRemarks.setSelection(combined.length());
                }
            }

            @Override
            public void onFinalTranscription(String finalText, File audioBackupFile) {
                setMicUiActive(false);
                if (binding.etRemarks != null && !finalText.isEmpty()) {
                    String combined = initialRemarks.isEmpty() ? finalText : initialRemarks + " " + finalText;
                    binding.etRemarks.setText(combined);
                    binding.etRemarks.setSelection(combined.length());
                }
                Toast.makeText(UpdateProgressActivity.this, "Note transcribed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String userFriendlyError) {
                setMicUiActive(false);
                Toast.makeText(UpdateProgressActivity.this, userFriendlyError, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setMicUiActive(boolean active) {
        if (binding.btnDictateRemarks == null) return;
        if (active) {
            binding.btnDictateRemarks.setBackgroundResource(R.drawable.btn_mic_dictate_active);
            binding.btnDictateRemarks.setImageResource(R.drawable.ic_mic_black);
        } else {
            binding.btnDictateRemarks.setBackgroundResource(R.drawable.btn_mic_dictate);
            binding.btnDictateRemarks.setImageResource(R.drawable.ic_mic);
        }
    }

    private void launchMatchAngleCamera() {
        User user = sessionManager.getCurrentUser();
        String engineerName = user != null ? user.getFullName() : "ELENA ROSTOVA (ENG-04)";

        MatchAngleCameraDialog dialog = MatchAngleCameraDialog.newInstance(
                currentActivity.getLatestPhotoUri(),
                engineerName,
                currentActivity.getLocation()
        );

        dialog.setOnPhotoVerifiedCallback((photoFile, photoUri) -> {
            selectedPhotoUri = photoUri.toString();
            showPhotoPreview(photoUri);
        });

        dialog.show(getSupportFragmentManager(), "MatchAngleDialog");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceDictationManager != null && voiceDictationManager.isListening()) {
            voiceDictationManager.stopListening();
        }
    }
}
