package com.example.sitesync.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.InspectionRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityReInspectionBinding;
import com.example.sitesync.models.InspectionChecklistItem;
import com.example.sitesync.models.SiteInspection;
import com.example.sitesync.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen enabling engineers to conduct formal re-inspections on previously failed
 * or pending inspections, re-verifying corrected checklist items, and logging audit history.
 */
public class ReInspectionActivity extends AppCompatActivity {

    private ActivityReInspectionBinding binding;
    private InspectionRepository repository;
    private SessionManager sessionManager;
    private InspectionChecklistAdapter checklistAdapter;
    private SiteInspection currentInspection;
    private String selectedPhotoUri = "";

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri.toString();
                    binding.ivReInspectPhotoPreview.setImageURI(uri);
                    binding.layoutReInspectPhotoPreview.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReInspectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new InspectionRepository(this);
        sessionManager = new SessionManager(this);

        String id = getIntent().getStringExtra(InspectionListActivity.EXTRA_INSPECTION_ID);
        currentInspection = repository.getInspectionById(id);

        if (currentInspection == null) {
            Toast.makeText(this, "Inspection record not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        binding.tvReInspectTargetType.setText(currentInspection.getInspectionType());
        binding.tvReInspectTargetLocation.setText(currentInspection.getLocation() + " • " + currentInspection.getId());

        // Clone checklist items for re-verification
        checklistAdapter = new InspectionChecklistAdapter(false);
        binding.rvReInspectChecklist.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReInspectChecklist.setAdapter(checklistAdapter);

        List<InspectionChecklistItem> cloned = new ArrayList<>();
        for (InspectionChecklistItem item : currentInspection.getChecklist()) {
            cloned.add(new InspectionChecklistItem(item.getId(), item.getTitle(), item.isPassed(), item.getNotes()));
        }
        checklistAdapter.setChecklist(cloned);

        // Pre-fill existing photo if any
        if (currentInspection.getPhotoUri() != null && !currentInspection.getPhotoUri().isEmpty()) {
            selectedPhotoUri = currentInspection.getPhotoUri();
            try {
                binding.ivReInspectPhotoPreview.setImageURI(Uri.parse(selectedPhotoUri));
                binding.layoutReInspectPhotoPreview.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {}
        }
    }

    private void setupListeners() {
        binding.btnBackFromReInspect.setOnClickListener(v -> finish());

        // Photo Picker
        binding.btnAttachReInspectPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        binding.btnRemoveReInspectPhoto.setOnClickListener(v -> {
            selectedPhotoUri = "";
            binding.layoutReInspectPhotoPreview.setVisibility(View.GONE);
        });

        // Save Re-Inspection
        binding.btnSaveReInspection.setOnClickListener(v -> handleSaveReInspection());
    }

    private void handleSaveReInspection() {
        String observations = getText(binding.etReInspectObservations);
        String remarks = getText(binding.etReInspectRemarks);

        if (TextUtils.isEmpty(observations)) {
            binding.tilReInspectObservations.setError("Please note re-inspection observations");
            return;
        } else {
            binding.tilReInspectObservations.setError(null);
        }

        // Determine Status
        String status = SiteInspection.STATUS_PASSED;
        if (binding.rbReInspectFailed.isChecked()) {
            status = SiteInspection.STATUS_FAILED;
        } else if (binding.rbReInspectPending.isChecked()) {
            status = SiteInspection.STATUS_PENDING;
        }

        User user = sessionManager.getCurrentUser();
        String engineerName = user != null ? user.getFullName() : "Field Engineer";

        boolean success = repository.conductReInspection(
                currentInspection.getId(),
                status,
                checklistAdapter.getChecklist(),
                observations,
                remarks,
                selectedPhotoUri,
                engineerName
        );

        if (success) {
            Toast.makeText(this, "Re-inspection saved: " + status, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to record re-inspection", Toast.LENGTH_SHORT).show();
        }
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }
}
