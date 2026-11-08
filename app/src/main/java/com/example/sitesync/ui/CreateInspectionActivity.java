package com.example.sitesync.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.InspectionRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityCreateInspectionBinding;
import com.example.sitesync.hardware.CheckLevelDialog;
import com.example.sitesync.hardware.MatchAngleCameraDialog;
import com.example.sitesync.hardware.VoiceDictationManager;
import com.example.sitesync.models.InspectionChecklistItem;
import com.example.sitesync.models.InspectionRecord;
import com.example.sitesync.models.SiteInspection;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen enabling engineers to author and submit new Quality Control inspections
 * with verification checklist items, "Check Level" slope/plumb telemetry,
 * "Speak Note" voice dictation, and "Match Angle" comparison camera.
 */
public class CreateInspectionActivity extends AppCompatActivity {

    private ActivityCreateInspectionBinding binding;
    private InspectionRepository repository;
    private SessionManager sessionManager;
    private InspectionChecklistAdapter checklistAdapter;
    private String selectedPhotoUri = "";

    private VoiceDictationManager voiceDictationManager;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri.toString();
                    binding.ivInspPhotoPreview.setImageURI(uri);
                    binding.layoutInspPhotoPreview.setVisibility(View.VISIBLE);
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
        binding = ActivityCreateInspectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new InspectionRepository(this);
        sessionManager = new SessionManager(this);
        voiceDictationManager = new VoiceDictationManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        checklistAdapter = new InspectionChecklistAdapter(false);
        binding.rvCreateChecklist.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCreateChecklist.setAdapter(checklistAdapter);

        // Standard pre-populated construction checks
        List<InspectionChecklistItem> defaultItems = new ArrayList<>();
        defaultItems.add(new InspectionChecklistItem("CHK-1", "Dimensions and structural member alignment match CAD blueprints", true, ""));
        defaultItems.add(new InspectionChecklistItem("CHK-2", "Material specifications and mill certificates verified", true, ""));
        defaultItems.add(new InspectionChecklistItem("CHK-3", "Surface preparation clean of debris, rust, and moisture", true, ""));
        defaultItems.add(new InspectionChecklistItem("CHK-4", "Safety clearances and temporary support bracing secured", true, ""));
        checklistAdapter.setChecklist(defaultItems);
    }

    private void setupListeners() {
        binding.btnBackFromCreateInsp.setOnClickListener(v -> finish());

        // Feature 2: "Check Level" Quick Slope & Plumb Gauge
        binding.btnCheckLevelTool.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            CheckLevelDialog checkLevelDialog = CheckLevelDialog.newInstance();
            checkLevelDialog.setOnMeasurementSavedListener(measurement -> {
                // Paste captured angle directly into active checklist
                checklistAdapter.addChecklistItem(new InspectionChecklistItem(
                        "CHK-" + System.currentTimeMillis(),
                        "Check Level Telemetry: " + measurement,
                        true,
                        "Captured with Tilt Sensor"
                ));
            });
            checkLevelDialog.show(getSupportFragmentManager(), "CheckLevelDialog");
        });

        // Feature 1: "Speak Note" Hands-Free Microphone Dictation
        binding.btnDictateObservations.setOnClickListener(v -> {
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
        binding.btnMatchAngleInsp.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchMatchAngleCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.btnAttachInspPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        binding.btnRemoveInspPhoto.setOnClickListener(v -> {
            selectedPhotoUri = "";
            binding.layoutInspPhotoPreview.setVisibility(View.GONE);
        });

        binding.btnAddCustomCheck.setOnClickListener(v -> showAddChecklistDialog());
        binding.btnSubmitInspection.setOnClickListener(v -> handleSubmitInspection());
    }

    private void startDictation() {
        setMicUiActive(true);
        String initialObs = binding.etCreateObservations.getText() != null ? binding.etCreateObservations.getText().toString() : "";

        voiceDictationManager.startListening(new VoiceDictationManager.OnDictationListener() {
            @Override
            public void onSpeechRmsChanged(float rmsdB) {}

            @Override
            public void onPartialTranscription(String partialText) {
                if (binding.etCreateObservations != null) {
                    String combined = initialObs.isEmpty() ? partialText : initialObs + " " + partialText;
                    binding.etCreateObservations.setText(combined);
                    binding.etCreateObservations.setSelection(combined.length());
                }
            }

            @Override
            public void onFinalTranscription(String finalText, File audioBackupFile) {
                setMicUiActive(false);
                if (binding.etCreateObservations != null && !finalText.isEmpty()) {
                    String combined = initialObs.isEmpty() ? finalText : initialObs + " " + finalText;
                    binding.etCreateObservations.setText(combined);
                    binding.etCreateObservations.setSelection(combined.length());
                }
                Toast.makeText(CreateInspectionActivity.this, "Observations transcribed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String userFriendlyError) {
                setMicUiActive(false);
                Toast.makeText(CreateInspectionActivity.this, userFriendlyError, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setMicUiActive(boolean active) {
        if (binding.btnDictateObservations == null) return;
        if (active) {
            binding.btnDictateObservations.setBackgroundResource(R.drawable.btn_mic_dictate_active);
            binding.btnDictateObservations.setImageResource(R.drawable.ic_mic_black);
        } else {
            binding.btnDictateObservations.setBackgroundResource(R.drawable.btn_mic_dictate);
            binding.btnDictateObservations.setImageResource(R.drawable.ic_mic);
        }
    }

    private void launchMatchAngleCamera() {
        User user = sessionManager.getCurrentUser();
        String engineerName = user != null ? user.getFullName() : "ELENA ROSTOVA (ENG-04)";
        String location = binding.etCreateInspLocation.getText() != null ? binding.etCreateInspLocation.getText().toString() : "Metro Central Tower";

        MatchAngleCameraDialog dialog = MatchAngleCameraDialog.newInstance("", engineerName, location);
        dialog.setOnPhotoVerifiedCallback((photoFile, photoUri) -> {
            selectedPhotoUri = photoUri.toString();
            binding.ivInspPhotoPreview.setImageURI(photoUri);
            binding.layoutInspPhotoPreview.setVisibility(View.VISIBLE);
        });
        dialog.show(getSupportFragmentManager(), "MatchAngleDialog");
    }

    private void showAddChecklistDialog() {
        final EditText input = new EditText(this);
        input.setHint("e.g. Slump test within 100-140mm range");
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle("Add Verification Item")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(title)) {
                        checklistAdapter.addChecklistItem(new InspectionChecklistItem(
                                "CHK-" + System.currentTimeMillis(), title, true, ""
                        ));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleSubmitInspection() {
        String project = binding.etCreateInspProject.getText() != null ? binding.etCreateInspProject.getText().toString().trim() : "";
        String location = binding.etCreateInspLocation.getText() != null ? binding.etCreateInspLocation.getText().toString().trim() : "";
        String title = binding.etCreateInspType.getText() != null ? binding.etCreateInspType.getText().toString().trim() : "";

        if (TextUtils.isEmpty(project)) {
            binding.tilCreateInspProject.setError("Project name is required");
            return;
        } else {
            binding.tilCreateInspProject.setError(null);
        }

        if (TextUtils.isEmpty(location)) {
            binding.tilCreateInspLocation.setError("Location is required");
            return;
        } else {
            binding.tilCreateInspLocation.setError(null);
        }

        if (TextUtils.isEmpty(title)) {
            binding.tilCreateInspType.setError("Inspection title is required");
            return;
        } else {
            binding.tilCreateInspType.setError(null);
        }

        String status = SiteInspection.STATUS_PASSED;
        int checkedStatusId = binding.rgCreateStatus.getCheckedRadioButtonId();
        if (checkedStatusId == binding.rbCreateStatusFailed.getId()) {
            status = SiteInspection.STATUS_FAILED;
        } else if (checkedStatusId == binding.rbCreateStatusPending.getId()) {
            status = SiteInspection.STATUS_PENDING;
        }

        String observations = binding.etCreateObservations.getText() != null ? binding.etCreateObservations.getText().toString().trim() : "";
        String remarks = binding.etCreateRemarks.getText() != null ? binding.etCreateRemarks.getText().toString().trim() : "";
        String combinedRemarks = observations;
        if (!TextUtils.isEmpty(remarks)) {
            combinedRemarks = combinedRemarks.isEmpty() ? remarks : combinedRemarks + "\nCorrective: " + remarks;
        }

        User user = sessionManager.getCurrentUser();
        String inspectorName = user != null ? user.getFullName() : "Site Quality Engineer";
        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

        String newId = "INSP-" + (System.currentTimeMillis() % 10000);

        List<InspectionRecord> records = new ArrayList<>();
        records.add(new InspectionRecord("ATT-1", 1, date, inspectorName, status, observations, remarks, selectedPhotoUri, ""));

        SiteInspection newInspection = new SiteInspection(
                newId, project, location, title,
                date, status, observations, remarks,
                inspectorName, selectedPhotoUri,
                new ArrayList<>(checklistAdapter.getChecklist()),
                records
        );

        boolean success = repository.addInspection(newInspection);
        if (success) {
            Toast.makeText(this, "Inspection " + newId + " recorded as " + status, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to record inspection", Toast.LENGTH_SHORT).show();
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
