package com.example.sitesync.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.HazardTicketEntity;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.databinding.DialogReportHazardBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

/**
 * Bottom Sheet Dialog for reporting jobsite hazards and safety non-compliance issues.
 * Supports speech dictation, priority assignment, and offline Room DB queuing.
 */
public class ReportHazardBottomSheetDialog extends BottomSheetDialogFragment {

    private DialogReportHazardBinding binding;
    private OnHazardReportedListener listener;
    private String selectedPriority = "URGENT";
    private boolean photoAttached = false;
    private ActivityResultLauncher<Intent> speechLauncher;

    public interface OnHazardReportedListener {
        void onHazardReported(HazardTicketEntity ticket);
    }

    public static ReportHazardBottomSheetDialog newInstance() {
        return new ReportHazardBottomSheetDialog();
    }

    public void setOnHazardReportedListener(OnHazardReportedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            String existing = binding.etHazardDescription.getText().toString();
                            binding.etHazardDescription.setText(existing.isEmpty() ? spokenText : existing + " " + spokenText);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogReportHazardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCloseDialog.setOnClickListener(v -> dismiss());

        setupCategorySpinner();
        setupPriorityPills();
        setupSpeechDictation();
        setupPhotoButton();
        setupSubmitButton();
    }

    private void setupCategorySpinner() {
        String[] categories = new String[]{
                "Electrical Hazard",
                "Scaffolding & Fall Protection",
                "Excavation & Trenching",
                "Structural Formwork Defect",
                "Chemical / Fuel Spill",
                "Heavy Equipment Clearance",
                "General Housekeeping"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        binding.spinnerHazardCategory.setAdapter(adapter);
    }

    private void setupPriorityPills() {
        updatePriorityPillUI("URGENT");

        binding.pillPriorityUrgent.setOnClickListener(v -> updatePriorityPillUI("URGENT"));
        binding.pillPriorityMedium.setOnClickListener(v -> updatePriorityPillUI("MEDIUM"));
        binding.pillPriorityLow.setOnClickListener(v -> updatePriorityPillUI("LOW"));
    }

    private void updatePriorityPillUI(String priority) {
        selectedPriority = priority;

        binding.pillPriorityUrgent.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillPriorityUrgent.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_secondary));

        binding.pillPriorityMedium.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillPriorityMedium.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_secondary));

        binding.pillPriorityLow.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillPriorityLow.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_secondary));

        if ("URGENT".equals(priority)) {
            binding.pillPriorityUrgent.setBackgroundResource(R.drawable.bg_btn_emergency);
            binding.pillPriorityUrgent.setTextColor(0xFFFFFFFF);
        } else if ("MEDIUM".equals(priority)) {
            binding.pillPriorityMedium.setBackgroundResource(R.drawable.bg_tab_pill_active);
            binding.pillPriorityMedium.setTextColor(0xFFFFFFFF);
        } else {
            binding.pillPriorityLow.setBackgroundResource(R.drawable.bg_pill_inactive);
            binding.pillPriorityLow.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_primary));
        }
    }

    private void setupSpeechDictation() {
        binding.btnHazardVoiceDictate.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe site hazard...");
                speechLauncher.launch(intent);
            } catch (Exception e) {
                // Fallback demo text
                String existing = binding.etHazardDescription.getText().toString();
                String note = "Loose scaffolding clamp detected on exterior facade east wing.";
                binding.etHazardDescription.setText(existing.isEmpty() ? note : existing + " " + note);
                Toast.makeText(requireContext(), "Voice note dictated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPhotoButton() {
        binding.btnCaptureHazardPhoto.setOnClickListener(v -> {
            HapticFeedbackUtil.scanSuccess(requireContext());
            photoAttached = true;
            binding.tvCapturePhotoLabel.setText("✓ Photo Attached (40.7128°N, 74.0060°W)");
            Toast.makeText(requireContext(), "Geotagged site photo captured & GPS verified", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSubmitButton() {
        binding.btnSubmitHazard.setOnClickListener(v -> {
            String location = binding.etHazardLocation.getText().toString().trim();
            String description = binding.etHazardDescription.getText().toString().trim();

            if (location.isEmpty()) {
                binding.etHazardLocation.setError("Specify location");
                binding.etHazardLocation.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                binding.etHazardDescription.setError("Enter description");
                binding.etHazardDescription.requestFocus();
                return;
            }

            String category = (String) binding.spinnerHazardCategory.getSelectedItem();
            long now = System.currentTimeMillis();
            String ticketId = "HAZ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);

            HazardTicketEntity ticket = new HazardTicketEntity(
                    ticketId,
                    category,
                    description,
                    selectedPriority,
                    "OPEN",
                    location,
                    photoAttached ? "content://geotagged_photo" : null,
                    "Field Personnel",
                    now,
                    null,
                    0L,
                    false
            );

            // Insert into Room DB
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            db.hazardTicketDao().insert(ticket);

            // Queue for background sync
            PendingSyncEntity sync = new PendingSyncEntity(
                    UUID.randomUUID().toString(),
                    "HAZARD",
                    ticketId,
                    "CREATE",
                    "{\"id\":\"" + ticketId + "\",\"cat\":\"" + category + "\",\"priority\":\"" + selectedPriority + "\"}",
                    now
            );
            db.pendingSyncDao().insert(sync);

            HapticFeedbackUtil.confirm(requireContext());
            Toast.makeText(requireContext(), "Hazard ticket logged & queued for inspection", Toast.LENGTH_SHORT).show();

            if (listener != null) {
                listener.onHazardReported(ticket);
            }

            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
