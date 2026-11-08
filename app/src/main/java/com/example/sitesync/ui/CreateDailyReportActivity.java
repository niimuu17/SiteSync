package com.example.sitesync.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.DailyReportRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityCreateDailyReportBinding;
import com.example.sitesync.hardware.MatchAngleCameraDialog;
import com.example.sitesync.hardware.TicketScannerDialog;
import com.example.sitesync.hardware.TicketScannerManager;
import com.example.sitesync.hardware.VoiceDictationManager;
import com.example.sitesync.models.DailySiteReport;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Screen providing an industrial form to compile 5-section Daily Site Reports (DSR),
 * featuring ML Kit delivery ticket scanning, "Speak Note" hands-free voice dictation,
 * and "Match Angle" photo evidence capture.
 */
public class CreateDailyReportActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_REPORT_ID = "extra_edit_report_id";

    private ActivityCreateDailyReportBinding binding;
    private DailyReportRepository repository;
    private SessionManager sessionManager;
    private String existingReportId = null;
    private DailySiteReport existingReport = null;
    private Uri selectedPhotoUri = null;

    private VoiceDictationManager voiceDictationManager;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    binding.ivReportPhotoPreview.setImageURI(uri);
                    binding.layoutReportPhotoPreview.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchTicketScanner();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan tickets", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> matchAnglePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchMatchAngleCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required for Match Angle", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateDailyReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new DailyReportRepository(this);
        sessionManager = new SessionManager(this);
        voiceDictationManager = new VoiceDictationManager(this);
        existingReportId = getIntent().getStringExtra(EXTRA_EDIT_REPORT_ID);

        initForm();
        setupListeners();
    }

    private void initForm() {
        if (existingReportId != null) {
            existingReport = repository.getReportById(existingReportId);
        }

        if (existingReport != null) {
            binding.tvFormHeaderTitle.setText("Edit Daily Site Report");
            binding.tvFormHeaderSubtitle.setText("Editing log: " + existingReport.getId());
            binding.etReportProject.setText(existingReport.getProjectName());
            binding.etReportDate.setText(existingReport.getReportDate());
            binding.etReportWeather.setText(existingReport.getWeather());
            binding.etCompletedWork.setText(existingReport.getCompletedWork());
            binding.etManpower.setText(existingReport.getManpower());
            binding.etMaterialsUsed.setText(existingReport.getMaterialsUsed());
            binding.etEquipment.setText(existingReport.getEquipment());
            binding.etSiteProblems.setText(existingReport.getSiteProblems());
            binding.etNextDayPlans.setText(existingReport.getNextDayPlans());

            if (existingReport.getPhotoUri() != null && !existingReport.getPhotoUri().isEmpty()) {
                selectedPhotoUri = Uri.parse(existingReport.getPhotoUri());
                try {
                    binding.ivReportPhotoPreview.setImageURI(selectedPhotoUri);
                    binding.layoutReportPhotoPreview.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    binding.layoutReportPhotoPreview.setVisibility(View.GONE);
                }
            }
        } else {
            String today = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            binding.etReportDate.setText(today);
            binding.etManpower.setText("18 Masons, 12 Barbenders, 4 Electricians, 2 Operators");
            binding.etMaterialsUsed.setText("42.5 m³ C35 Ready-Mix Concrete, 6.2 Tons Rebar #6");
            binding.etEquipment.setText("1 Tower Crane #2, 1 Concrete Boom Pump, 4 Vibrator Units");
            binding.etNextDayPlans.setText("Complete Level 4 column pours and begin beam rebar layout.");
        }
    }

    private void setupListeners() {
        binding.btnBackFromCreateReport.setOnClickListener(v -> finish());

        // Feature 5: [ SCAN DELIVERY TICKET ]
        binding.btnScanDeliveryTicket.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchTicketScanner();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Feature 1: "Speak Note" Hands-Free Microphone Dictation
        binding.btnDictateWork.setOnClickListener(v -> {
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
        binding.btnMatchAngleReport.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchMatchAngleCamera();
            } else {
                matchAnglePermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.btnAttachReportPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        binding.btnRemoveReportPhoto.setOnClickListener(v -> {
            selectedPhotoUri = null;
            binding.ivReportPhotoPreview.setImageURI(null);
            binding.layoutReportPhotoPreview.setVisibility(View.GONE);
        });

        binding.btnSaveDraft.setOnClickListener(v -> saveReport(true));
        binding.btnSubmitReport.setOnClickListener(v -> saveReport(false));
    }

    private void launchTicketScanner() {
        TicketScannerDialog dialog = TicketScannerDialog.newInstance();
        dialog.setOnTicketParsedListener(ticket -> {
            // Auto-populate material fields
            String current = binding.etMaterialsUsed.getText() != null ? binding.etMaterialsUsed.getText().toString().trim() : "";
            String ticketSummary = String.format("%s (%s, Slump: %s, Supplier: %s)",
                    ticket.mixId, ticket.quantity, ticket.slump, ticket.supplierOrSerial);

            String updated = current.isEmpty() ? ticketSummary : current + "\n• " + ticketSummary;
            binding.etMaterialsUsed.setText(updated);
            Toast.makeText(this, "Delivery ticket data auto-populated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "TicketScannerDialog");
    }

    private void launchMatchAngleCamera() {
        User user = sessionManager.getCurrentUser();
        String engineerName = user != null ? user.getFullName() : "ELENA ROSTOVA (ENG-04)";
        String location = binding.etReportProject.getText() != null ? binding.etReportProject.getText().toString() : "Metro Central Tower";

        MatchAngleCameraDialog dialog = MatchAngleCameraDialog.newInstance("", engineerName, location);
        dialog.setOnPhotoVerifiedCallback((photoFile, photoUri) -> {
            selectedPhotoUri = photoUri;
            binding.ivReportPhotoPreview.setImageURI(photoUri);
            binding.layoutReportPhotoPreview.setVisibility(View.VISIBLE);
        });
        dialog.show(getSupportFragmentManager(), "MatchAngleDialog");
    }

    private void startDictation() {
        setMicUiActive(true);
        String initialText = binding.etCompletedWork.getText() != null ? binding.etCompletedWork.getText().toString() : "";

        voiceDictationManager.startListening(new VoiceDictationManager.OnDictationListener() {
            @Override
            public void onSpeechRmsChanged(float rmsdB) {}

            @Override
            public void onPartialTranscription(String partialText) {
                if (binding.etCompletedWork != null) {
                    String combined = initialText.isEmpty() ? partialText : initialText + " " + partialText;
                    binding.etCompletedWork.setText(combined);
                    binding.etCompletedWork.setSelection(combined.length());
                }
            }

            @Override
            public void onFinalTranscription(String finalText, File audioBackupFile) {
                setMicUiActive(false);
                if (binding.etCompletedWork != null && !finalText.isEmpty()) {
                    String combined = initialText.isEmpty() ? finalText : initialText + " " + finalText;
                    binding.etCompletedWork.setText(combined);
                    binding.etCompletedWork.setSelection(combined.length());
                }
                Toast.makeText(CreateDailyReportActivity.this, "Voice note recorded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String userFriendlyError) {
                setMicUiActive(false);
                Toast.makeText(CreateDailyReportActivity.this, userFriendlyError, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setMicUiActive(boolean active) {
        if (binding.btnDictateWork == null) return;
        if (active) {
            binding.btnDictateWork.setBackgroundResource(R.drawable.btn_mic_dictate_active);
            binding.btnDictateWork.setImageResource(R.drawable.ic_mic_black);
        } else {
            binding.btnDictateWork.setBackgroundResource(R.drawable.btn_mic_dictate);
            binding.btnDictateWork.setImageResource(R.drawable.ic_mic);
        }
    }

    private void saveReport(boolean isDraft) {
        String project = binding.etReportProject.getText() != null ? binding.etReportProject.getText().toString().trim() : "";
        String date = binding.etReportDate.getText() != null ? binding.etReportDate.getText().toString().trim() : "";
        String weather = binding.etReportWeather.getText() != null ? binding.etReportWeather.getText().toString().trim() : "";
        String completedWork = binding.etCompletedWork.getText() != null ? binding.etCompletedWork.getText().toString().trim() : "";
        String manpower = binding.etManpower.getText() != null ? binding.etManpower.getText().toString().trim() : "";
        String materials = binding.etMaterialsUsed.getText() != null ? binding.etMaterialsUsed.getText().toString().trim() : "";
        String equipment = binding.etEquipment.getText() != null ? binding.etEquipment.getText().toString().trim() : "";
        String problems = binding.etSiteProblems.getText() != null ? binding.etSiteProblems.getText().toString().trim() : "";
        String nextDay = binding.etNextDayPlans.getText() != null ? binding.etNextDayPlans.getText().toString().trim() : "";

        if (TextUtils.isEmpty(project)) {
            binding.tilReportProject.setError("Project Name is required");
            return;
        } else {
            binding.tilReportProject.setError(null);
        }

        if (TextUtils.isEmpty(date)) {
            binding.tilReportDate.setError("Date is required");
            return;
        } else {
            binding.tilReportDate.setError(null);
        }

        if (!isDraft && TextUtils.isEmpty(completedWork)) {
            binding.tilCompletedWork.setError("Work Completed cannot be empty for official submission");
            return;
        } else {
            binding.tilCompletedWork.setError(null);
        }

        String reportId = existingReport != null ? existingReport.getId() : "DSR-" + (System.currentTimeMillis() % 100000);
        String status = isDraft ? DailySiteReport.STATUS_DRAFT : DailySiteReport.STATUS_SUBMITTED;
        String photoUriString = selectedPhotoUri != null ? selectedPhotoUri.toString() : "";

        User currentUser = sessionManager.getCurrentUser();
        String preparedBy = currentUser != null ? currentUser.getFullName() : "Lead Site Engineer";
        String nowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        DailySiteReport report = new DailySiteReport(
                reportId, project, date, preparedBy,
                status, weather, completedWork,
                manpower, materials, equipment, problems,
                photoUriString, nextDay,
                existingReport != null ? existingReport.getCreatedAt() : nowStr,
                !isDraft ? nowStr : ""
        );

        repository.saveReport(report);
        String msg = isDraft ? "Report saved as Draft (" + reportId + ")" : "Report submitted to PM (" + reportId + ")";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceDictationManager != null && voiceDictationManager.isListening()) {
            voiceDictationManager.stopListening();
        }
    }
}
