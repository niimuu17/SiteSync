package com.example.sitesync.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.databinding.DialogCreateRequisitionBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CreateRequisitionBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnRequisitionSubmittedListener {
        void onRequisitionSubmitted();
    }

    private DialogCreateRequisitionBinding binding;
    private OnRequisitionSubmittedListener listener;
    private long selectedNeedByMillis = System.currentTimeMillis() + (2L * 24 * 60 * 60 * 1000);
    private String selectedUrgency = "Standard";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    public static CreateRequisitionBottomSheetDialog newInstance() {
        return new CreateRequisitionBottomSheetDialog();
    }

    public void setOnRequisitionSubmittedListener(OnRequisitionSubmittedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogCreateRequisitionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvReqNeedByDate.setText(dateFormat.format(new Date(selectedNeedByMillis)));

        setupSpinners();
        setupDatePicker();
        setupUrgencyPills();
        setupActions();
    }

    private void setupSpinners() {
        String[] materials = new String[]{
                "Portland Cement (50kg Bags)",
                "Structural Rebar 16mm (Tons)",
                "Coarse River Sand (cft)",
                "Crushed Aggregate 20mm (Tons)",
                "Concrete Admixture Plasticizer (Liters)"
        };
        ArrayAdapter<String> matAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, materials);
        binding.spinnerReqMaterial.setAdapter(matAdapter);

        String[] units = new String[]{"Bags", "Tons", "cft", "Liters", "Bundles"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, units);
        binding.spinnerReqUnit.setAdapter(unitAdapter);
    }

    private void setupDatePicker() {
        binding.layoutReqNeedByDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Delivery Need-By Date")
                    .setSelection(selectedNeedByMillis)
                    .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    selectedNeedByMillis = selection;
                    binding.tvReqNeedByDate.setText(dateFormat.format(new Date(selectedNeedByMillis)));
                }
            });

            picker.show(getParentFragmentManager(), "REQ_DATE_PICKER");
        });
    }

    private void setupUrgencyPills() {
        binding.pillUrgencyStandard.setOnClickListener(v -> setUrgency("Standard"));
        binding.pillUrgencyHigh.setOnClickListener(v -> setUrgency("High Priority"));
        binding.pillUrgencyEmergency.setOnClickListener(v -> setUrgency("Jobsite Emergency"));
    }

    private void setUrgency(String urgency) {
        selectedUrgency = urgency;
        HapticFeedbackUtil.lightClick(requireContext(), null);

        binding.pillUrgencyStandard.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillUrgencyStandard.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_primary));

        binding.pillUrgencyHigh.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillUrgencyHigh.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_primary));

        binding.pillUrgencyEmergency.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillUrgencyEmergency.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_primary));

        if ("Standard".equals(urgency)) {
            binding.pillUrgencyStandard.setBackgroundResource(R.drawable.bg_tab_pill_active);
            binding.pillUrgencyStandard.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_oxide_white));
        } else if ("High Priority".equals(urgency)) {
            binding.pillUrgencyHigh.setBackgroundResource(R.drawable.bg_badge_medium);
            binding.pillUrgencyHigh.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_amber_dark));
        } else {
            binding.pillUrgencyEmergency.setBackgroundResource(R.drawable.bg_badge_urgent);
            binding.pillUrgencyEmergency.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
        }
    }

    private void setupActions() {
        binding.btnCancelRequisition.setOnClickListener(v -> dismiss());

        binding.btnSubmitRequisition.setOnClickListener(v -> {
            String qtyStr = binding.etReqQuantity.getText().toString().trim();
            if (qtyStr.isEmpty()) {
                binding.etReqQuantity.setError("Please enter quantity");
                binding.etReqQuantity.requestFocus();
                return;
            }

            String material = (String) binding.spinnerReqMaterial.getSelectedItem();
            String unit = (String) binding.spinnerReqUnit.getSelectedItem();
            String notes = binding.etReqJustification.getText().toString().trim();

            HapticFeedbackUtil.confirm(requireContext());

            // Queue requisition for cloud sync
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            PendingSyncEntity syncItem = new PendingSyncEntity(
                    UUID.randomUUID().toString(),
                    "REQUISITION",
                    "REQ-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                    "CREATE",
                    "{\"material\":\"" + material + "\",\"qty\":" + qtyStr + ",\"" + unit + "\",\"urgency\":\"" + selectedUrgency + "\"}",
                    System.currentTimeMillis()
            );
            db.pendingSyncDao().insert(syncItem);

            Toast.makeText(requireContext(), "Requisition Submitted for Approval: " + qtyStr + " " + unit + " " + material, Toast.LENGTH_LONG).show();

            if (listener != null) listener.onRequisitionSubmitted();
            dismiss();
        });
    }
}
