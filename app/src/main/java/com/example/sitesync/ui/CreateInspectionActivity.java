package com.example.sitesync.ui;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.InspectionRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityCreateInspectionBinding;
import com.example.sitesync.models.InspectionChecklistItem;
import com.example.sitesync.models.InspectionRecord;
import com.example.sitesync.models.SiteInspection;
import com.example.sitesync.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen enabling engineers to author and submit new Quality Control inspections
 * with verification checklist items, site findings, and photo evidence.
 */
public class CreateInspectionActivity extends AppCompatActivity {

    private ActivityCreateInspectionBinding binding;
    private InspectionRepository repository;
    private SessionManager sessionManager;
    private InspectionChecklistAdapter checklistAdapter;
    private String selectedPhotoUri = "";

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri.toString();
                    binding.ivInspPhotoPreview.setImageURI(uri);
                    binding.layoutInspPhotoPreview.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateInspectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new InspectionRepository(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        checklistAdapter = new InspectionChecklistAdapter(false);
        binding.rvCreateChecklist.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCreateChecklist.setAdapter(checklistAdapter);

        // Pre-populate standard checklist items
        List<InspectionChecklistItem> defaultItems = new ArrayList<>();
        defaultItems.add(new InspectionChecklistItem("CHK-1", "Dimensions and structural member alignment match CAD blueprints", true, ""));
        defaultItems.add(new InspectionChecklistItem("CHK-2", "Material specifications and mill certificates verified", true, ""));
        defaultItems.add(new InspectionChecklistItem("CHK-3", "Surface preparation clean of debris, rust, and moisture", true, ""));
        defaultItems.add(new InspectionChecklistItem("CHK-4", "Safety clearances and temporary support bracing secured", true, ""));
        checklistAdapter.setChecklist(defaultItems);
    }

    private void setupListeners() {
        binding.btnBackFromCreateInsp.setOnClickListener(v -> finish());

        // Photo Picker
        binding.btnAttachInspPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        binding.btnRemoveInspPhoto.setOnClickListener(v -> {
            selectedPhotoUri = "";
            binding.layoutInspPhotoPreview.setVisibility(View.GONE);
        });

        // Add Custom Checklist Item Dialog
        binding.btnAddCustomCheck.setOnClickListener(v -> showAddChecklistDialog());

        // Submit Button
        binding.btnSubmitInspection.setOnClickListener(v -> handleSubmitInspection());
    }

    private void showAddChecklistDialog() {
        final EditText input = new EditText(this);
        input.setHint("e.g. Slump test within 100-140mm range");
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle("Add Quality Check")
                .setMessage("Enter a specific verification point to inspect:")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(title)) {
                        List<InspectionChecklistItem> current = checklistAdapter.getChecklist();
                        current.add(new InspectionChecklistItem("CHK-" + System.currentTimeMillis(), title, true, ""));
                        checklistAdapter.notifyItemInserted(current.size() - 1);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleSubmitInspection() {
        String project = getText(binding.etCreateInspProject);
        String location = getText(binding.etCreateInspLocation);
        String type = getText(binding.etCreateInspType);
        String observations = getText(binding.etCreateObservations);
        String remarks = getText(binding.etCreateRemarks);

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

        if (TextUtils.isEmpty(type)) {
            binding.tilCreateInspType.setError("Inspection type is required");
            return;
        } else {
            binding.tilCreateInspType.setError(null);
        }

        // Determine Status
        String status = SiteInspection.STATUS_PASSED;
        if (binding.rbCreateStatusFailed.isChecked()) {
            status = SiteInspection.STATUS_FAILED;
        } else if (binding.rbCreateStatusPending.isChecked()) {
            status = SiteInspection.STATUS_PENDING;
        }

        User user = sessionManager.getCurrentUser();
        String engineerName = user != null ? user.getFullName() : "Field Engineer";
        String timestamp = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());

        String newId = "INSP-" + (100 + repository.getAllInspections().size() + 1);

        List<InspectionChecklistItem> items = checklistAdapter.getChecklist();
        int passedCount = 0;
        for (InspectionChecklistItem it : items) {
            if (it.isPassed()) passedCount++;
        }
        String summary = "Checklist: " + passedCount + "/" + items.size() + " items verified";

        // Initial History Record
        List<InspectionRecord> history = new ArrayList<>();
        history.add(new InspectionRecord(
                "REC-" + System.currentTimeMillis(),
                1,
                timestamp,
                engineerName,
                status,
                observations,
                remarks,
                selectedPhotoUri,
                summary
        ));

        SiteInspection newInspection = new SiteInspection(
                newId,
                project,
                location,
                type,
                timestamp,
                status,
                observations,
                remarks,
                engineerName,
                selectedPhotoUri,
                items,
                history
        );

        repository.addInspection(newInspection);
        Toast.makeText(this, "Inspection logged: " + newId + " (" + status + ")", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }
}
