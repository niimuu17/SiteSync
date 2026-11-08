package com.example.sitesync.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.DailyReportRepository;
import com.example.sitesync.databinding.ActivityCreateDailyReportBinding;
import com.example.sitesync.models.DailySiteReport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Screen providing an easy-to-use form to create, edit, save draft, and submit Daily Site Reports.
 */
public class CreateDailyReportActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_REPORT_ID = "extra_edit_report_id";

    private ActivityCreateDailyReportBinding binding;
    private DailyReportRepository repository;
    private String existingReportId = null;
    private DailySiteReport existingReport = null;
    private Uri selectedPhotoUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    binding.ivReportPhotoPreview.setImageURI(uri);
                    binding.layoutReportPhotoPreview.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateDailyReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new DailyReportRepository(this);
        existingReportId = getIntent().getStringExtra(EXTRA_EDIT_REPORT_ID);

        initForm();
        setupListeners();
    }

    private void initForm() {
        if (existingReportId != null) {
            existingReport = repository.getReportById(existingReportId);
        }

        if (existingReport != null) {
            // Edit Mode
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
            // Create New Mode
            String today = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            binding.etReportDate.setText(today);
        }
    }

    private void setupListeners() {
        binding.btnBackFromCreateReport.setOnClickListener(v -> finish());

        // Photo Picker
        binding.btnAttachReportPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Remove Photo
        binding.btnRemoveReportPhoto.setOnClickListener(v -> {
            selectedPhotoUri = null;
            binding.ivReportPhotoPreview.setImageURI(null);
            binding.layoutReportPhotoPreview.setVisibility(View.GONE);
        });

        // Save Draft
        binding.btnSaveDraft.setOnClickListener(v -> saveDailyReport(false));

        // Submit Report
        binding.btnSubmitReport.setOnClickListener(v -> saveDailyReport(true));
    }

    private void saveDailyReport(boolean isSubmitting) {
        String project = binding.etReportProject.getText() != null
                ? binding.etReportProject.getText().toString().trim() : "";
        String date = binding.etReportDate.getText() != null
                ? binding.etReportDate.getText().toString().trim() : "";
        String weather = binding.etReportWeather.getText() != null
                ? binding.etReportWeather.getText().toString().trim() : "";
        String completedWork = binding.etCompletedWork.getText() != null
                ? binding.etCompletedWork.getText().toString().trim() : "";
        String manpower = binding.etManpower.getText() != null
                ? binding.etManpower.getText().toString().trim() : "";
        String materials = binding.etMaterialsUsed.getText() != null
                ? binding.etMaterialsUsed.getText().toString().trim() : "";
        String equipment = binding.etEquipment.getText() != null
                ? binding.etEquipment.getText().toString().trim() : "";
        String problems = binding.etSiteProblems.getText() != null
                ? binding.etSiteProblems.getText().toString().trim() : "";
        String nextPlans = binding.etNextDayPlans.getText() != null
                ? binding.etNextDayPlans.getText().toString().trim() : "";

        // Minimal validation for Drafts
        if (project.isEmpty()) {
            binding.tilReportProject.setError("Project name is required");
            binding.etReportProject.requestFocus();
            return;
        } else {
            binding.tilReportProject.setError(null);
        }

        if (date.isEmpty()) {
            binding.tilReportDate.setError("Report date is required");
            binding.etReportDate.requestFocus();
            return;
        } else {
            binding.tilReportDate.setError(null);
        }

        // Full validation when submitting
        if (isSubmitting) {
            if (completedWork.isEmpty()) {
                binding.tilCompletedWork.setError("Please describe the work completed today");
                binding.etCompletedWork.requestFocus();
                return;
            } else {
                binding.tilCompletedWork.setError(null);
            }

            if (manpower.isEmpty()) {
                binding.tilManpower.setError("Please enter manpower deployment summary");
                binding.etManpower.requestFocus();
                return;
            } else {
                binding.tilManpower.setError(null);
            }

            if (materials.isEmpty()) {
                binding.tilMaterialsUsed.setError("Please enter materials used");
                binding.etMaterialsUsed.requestFocus();
                return;
            } else {
                binding.tilMaterialsUsed.setError(null);
            }

            if (equipment.isEmpty()) {
                binding.tilEquipment.setError("Please list machinery/equipment deployed");
                binding.etEquipment.requestFocus();
                return;
            } else {
                binding.tilEquipment.setError(null);
            }

            if (nextPlans.isEmpty()) {
                binding.tilNextDayPlans.setError("Please record next-day target activities");
                binding.etNextDayPlans.requestFocus();
                return;
            } else {
                binding.tilNextDayPlans.setError(null);
            }
        }

        String reportId;
        String createdAt;
        String submittedAt = "";

        if (existingReport != null) {
            reportId = existingReport.getId();
            createdAt = existingReport.getCreatedAt();
            submittedAt = existingReport.getSubmittedAt();
        } else {
            String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            int count = repository.getAllReports().size() + 1;
            reportId = "DSR-" + dateKey + "-" + count;
            createdAt = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
        }

        String status = isSubmitting ? DailySiteReport.STATUS_SUBMITTED : DailySiteReport.STATUS_DRAFT;
        if (isSubmitting) {
            submittedAt = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
        }

        DailySiteReport report = new DailySiteReport(
                reportId,
                project,
                date,
                "Elena Rostova (PE)",
                status,
                weather.isEmpty() ? "Sunny, 28°C" : weather,
                completedWork,
                manpower,
                materials,
                equipment,
                problems.isEmpty() ? "None reported." : problems,
                selectedPhotoUri != null ? selectedPhotoUri.toString() : null,
                nextPlans,
                createdAt,
                submittedAt
        );

        repository.saveReport(report);

        if (isSubmitting) {
            Toast.makeText(this, "Daily Site Report " + reportId + " submitted successfully!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Daily Site Report draft saved!", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
