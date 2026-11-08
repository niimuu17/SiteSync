package com.example.sitesync.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.IssueRepository;
import com.example.sitesync.databinding.ActivityCreateIssueBinding;
import com.example.sitesync.models.IssueHistoryLog;
import com.example.sitesync.models.TechnicalIssue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen allowing engineers to raise and submit a new Technical Issue / RFI.
 * Supports capturing project, location, priority, assigned person, detailed description, and photo attachments.
 */
public class CreateIssueActivity extends AppCompatActivity {

    private ActivityCreateIssueBinding binding;
    private IssueRepository repository;
    private Uri selectedPhotoUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    binding.ivIssuePhotoPreview.setImageURI(uri);
                    binding.layoutPhotoPreview.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateIssueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new IssueRepository(this);

        setupListeners();
    }

    private void setupListeners() {
        binding.btnBackFromCreateIssue.setOnClickListener(v -> finish());

        // Attach Photo Picker
        binding.btnAttachIssuePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Remove Attached Photo
        binding.btnRemoveIssuePhoto.setOnClickListener(v -> {
            selectedPhotoUri = null;
            binding.ivIssuePhotoPreview.setImageURI(null);
            binding.layoutPhotoPreview.setVisibility(View.GONE);
        });

        // Submit Button
        binding.btnSubmitIssue.setOnClickListener(v -> submitTechnicalIssue());
    }

    private void submitTechnicalIssue() {
        String title = binding.etIssueTitle.getText() != null
                ? binding.etIssueTitle.getText().toString().trim() : "";
        String project = binding.etProjectName.getText() != null
                ? binding.etProjectName.getText().toString().trim() : "";
        String location = binding.etSiteLocation.getText() != null
                ? binding.etSiteLocation.getText().toString().trim() : "";
        String assignedTo = binding.etAssignedPerson.getText() != null
                ? binding.etAssignedPerson.getText().toString().trim() : "";
        String description = binding.etDescription.getText() != null
                ? binding.etDescription.getText().toString().trim() : "";

        // Form Validation
        if (title.isEmpty()) {
            binding.tilIssueTitle.setError("Please enter an issue title or subject");
            binding.etIssueTitle.requestFocus();
            return;
        } else {
            binding.tilIssueTitle.setError(null);
        }

        if (project.isEmpty()) {
            binding.tilProjectName.setError("Please enter the project name");
            binding.etProjectName.requestFocus();
            return;
        } else {
            binding.tilProjectName.setError(null);
        }

        if (location.isEmpty()) {
            binding.tilSiteLocation.setError("Please specify the site location or grid reference");
            binding.etSiteLocation.requestFocus();
            return;
        } else {
            binding.tilSiteLocation.setError(null);
        }

        if (assignedTo.isEmpty()) {
            binding.tilAssignedPerson.setError("Please enter the assigned engineer or consultant");
            binding.etAssignedPerson.requestFocus();
            return;
        } else {
            binding.tilAssignedPerson.setError(null);
        }

        if (description.isEmpty()) {
            binding.tilDescription.setError("Please provide a description of the issue");
            binding.etDescription.requestFocus();
            return;
        } else {
            binding.tilDescription.setError(null);
        }

        // Priority Selection
        String priority = TechnicalIssue.PRIORITY_MEDIUM;
        if (binding.rbPriorityHigh.isChecked()) {
            priority = TechnicalIssue.PRIORITY_HIGH;
        } else if (binding.rbPriorityLow.isChecked()) {
            priority = TechnicalIssue.PRIORITY_LOW;
        }

        // Generate sequential RFI ID
        List<TechnicalIssue> existingIssues = repository.getAllIssues();
        String generatedId = "RFI-" + (101 + existingIssues.size());

        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());

        String initialStatus = TechnicalIssue.STATUS_ASSIGNED;

        TechnicalIssue newIssue = new TechnicalIssue(
                generatedId,
                title,
                description,
                project,
                location,
                priority,
                initialStatus,
                assignedTo,
                "Site Engineer",
                currentDate,
                selectedPhotoUri != null ? selectedPhotoUri.toString() : null
        );

        // Initial History Audit Logs
        newIssue.addHistoryLog(new IssueHistoryLog(
                "LOG-" + System.currentTimeMillis() + "-1",
                "Created RFI #" + generatedId,
                currentTime,
                "Site Engineer"
        ));
        newIssue.addHistoryLog(new IssueHistoryLog(
                "LOG-" + System.currentTimeMillis() + "-2",
                "Assigned to " + assignedTo,
                currentTime,
                "Site Engineer"
        ));

        // Save to repository
        repository.addIssue(newIssue);

        Toast.makeText(this, "RFI " + generatedId + " submitted successfully!", Toast.LENGTH_LONG).show();
        finish();
    }
}
