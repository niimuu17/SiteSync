package com.example.sitesync.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.DocumentRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityUploadDocumentBinding;
import com.example.sitesync.models.DocumentRevision;
import com.example.sitesync.models.SiteDocument;
import com.example.sitesync.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen enabling authorized users to upload drawings and engineering documents,
 * or upload successive revisions to existing drawings.
 */
public class UploadDocumentActivity extends AppCompatActivity {

    private ActivityUploadDocumentBinding binding;
    private DocumentRepository repository;
    private SessionManager sessionManager;
    private String existingDocId = null;
    private SiteDocument existingDocument = null;
    private String selectedFileUri = "";

    // File picker launcher using AndroidX Activity Result contract for PDF or documents
    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri.toString();
                    String displayName = uri.getLastPathSegment();
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = "Engineering_Submittal.pdf";
                    }
                    binding.tvSelectedFileName.setText(displayName);
                    binding.tvSelectedFileSize.setText("PDF Document attached");
                    binding.layoutSelectedFileInfo.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new DocumentRepository(this);
        sessionManager = new SessionManager(this);

        existingDocId = getIntent().getStringExtra(DocumentManagementActivity.EXTRA_DOC_ID);
        if (existingDocId != null) {
            existingDocument = repository.getDocumentById(existingDocId);
            configureRevisionMode();
        }

        setupListeners();
    }

    private void configureRevisionMode() {
        if (existingDocument != null) {
            binding.tvUploadHeaderTitle.setText("Add Drawing Revision");
            binding.etUploadDocName.setText(existingDocument.getName());
            binding.etUploadDocName.setEnabled(false);
            binding.etUploadProject.setText(existingDocument.getProjectName());
            binding.etUploadProject.setEnabled(false);
            binding.rgUploadCategory.setVisibility(View.GONE);

            // Suggest incremented revision
            binding.etUploadRevision.setText("Rev " + existingDocument.getRevisionHistory().size() + ".1.0");
            binding.btnSubmitUpload.setText("Register New Revision");
        }
    }

    private void setupListeners() {
        binding.btnBackFromUpload.setOnClickListener(v -> finish());

        // File picker button
        binding.btnPickDocumentFile.setOnClickListener(v -> filePickerLauncher.launch("application/pdf"));

        // Remove attached file
        binding.btnRemoveSelectedFile.setOnClickListener(v -> {
            selectedFileUri = "";
            binding.layoutSelectedFileInfo.setVisibility(View.GONE);
        });

        // Submit Button
        binding.btnSubmitUpload.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String docName = getText(binding.etUploadDocName);
        String project = getText(binding.etUploadProject);
        String revision = getText(binding.etUploadRevision);
        String notes = getText(binding.etUploadNotes);

        if (TextUtils.isEmpty(docName)) {
            binding.tilUploadDocName.setError("Document name cannot be empty");
            return;
        } else {
            binding.tilUploadDocName.setError(null);
        }

        if (TextUtils.isEmpty(revision)) {
            binding.tilUploadRevision.setError("Revision number cannot be empty");
            return;
        } else {
            binding.tilUploadRevision.setError(null);
        }

        User user = sessionManager.getCurrentUser();
        String engineerName = user != null ? user.getFullName() : "Field Engineer";
        String timestamp = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());

        if (existingDocument != null) {
            // Adding a new revision to an existing document
            DocumentRevision newRev = new DocumentRevision(
                    "REV-" + System.currentTimeMillis(),
                    revision,
                    timestamp,
                    engineerName,
                    true,
                    notes.isEmpty() ? "Verified submittal revision" : notes,
                    "PDF • 4.8 MB",
                    selectedFileUri
            );
            repository.addRevisionToDocument(existingDocument.getId(), newRev);
            Toast.makeText(this, "Revision " + revision + " uploaded successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // Uploading a brand new document
            String category = SiteDocument.CATEGORY_STRUCTURAL;
            int checkedId = binding.rgUploadCategory.getCheckedRadioButtonId();
            if (checkedId == binding.rbCatArchitectural.getId()) {
                category = SiteDocument.CATEGORY_ARCHITECTURAL;
            } else if (checkedId == binding.rbCatElectrical.getId()) {
                category = SiteDocument.CATEGORY_ELECTRICAL;
            } else if (checkedId == binding.rbCatPlumbing.getId()) {
                category = SiteDocument.CATEGORY_PLUMBING;
            }

            String newId = "DOC-" + category.substring(0, 3).toUpperCase() + "-" + (100 + repository.getAllDocuments().size() + 1);

            List<DocumentRevision> revs = new ArrayList<>();
            revs.add(new DocumentRevision(
                    "REV-" + System.currentTimeMillis(),
                    revision,
                    timestamp,
                    engineerName,
                    true,
                    notes.isEmpty() ? "Initial approved design issue" : notes,
                    "PDF • 4.2 MB",
                    selectedFileUri
            ));

            SiteDocument newDoc = new SiteDocument(
                    newId,
                    docName,
                    project,
                    category,
                    timestamp,
                    revision,
                    true,
                    engineerName,
                    "PDF",
                    "PDF • 4.2 MB",
                    selectedFileUri,
                    notes.isEmpty() ? "Construction drawing plan" : notes,
                    revs
            );

            repository.addDocument(newDoc);
            Toast.makeText(this, "Drawing uploaded: " + docName + " (" + revision + ")", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }
}
