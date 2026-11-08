package com.example.sitesync.ui.dialogs;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.InspectionRepository;
import com.example.sitesync.hardware.CheckLevelDialog;
import com.example.sitesync.models.SiteInspection;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * High-contrast industrial bottom sheet for initiating QA & Safety checklists.
 * Integrates "Check Level" slope measurement tool directly into the workflow.
 */
public class CreateInspectionBottomSheet extends BottomSheetDialogFragment {

    public interface OnInspectionCreatedListener {
        void onInspectionCreated(SiteInspection item);
    }

    private OnInspectionCreatedListener listener;
    private String selectedDiscipline = "Structural";
    private String capturedMeasurement = "";

    private EditText etInspectionTitle;
    private MaterialButton btnDiscStructural;
    private MaterialButton btnDiscArch;
    private MaterialButton btnDiscElec;
    private MaterialButton btnDiscPlumb;
    private TextView tvCapturedSlope;
    private MaterialButton btnCheckLevelTool;
    private EditText etInspectionNotes;
    private MaterialButton btnSubmitInspection;

    public static CreateInspectionBottomSheet newInstance() {
        return new CreateInspectionBottomSheet();
    }

    public void setOnInspectionCreatedListener(OnInspectionCreatedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_inspection_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etInspectionTitle = view.findViewById(R.id.etInspectionTitle);
        btnDiscStructural = view.findViewById(R.id.btnDiscStructural);
        btnDiscArch = view.findViewById(R.id.btnDiscArch);
        btnDiscElec = view.findViewById(R.id.btnDiscElec);
        btnDiscPlumb = view.findViewById(R.id.btnDiscPlumb);
        tvCapturedSlope = view.findViewById(R.id.tvCapturedSlope);
        btnCheckLevelTool = view.findViewById(R.id.btnCheckLevelTool);
        etInspectionNotes = view.findViewById(R.id.etInspectionNotes);
        btnSubmitInspection = view.findViewById(R.id.btnSubmitInspection);

        updateDisciplineButtons();

        btnDiscStructural.setOnClickListener(v -> {
            selectedDiscipline = "Structural";
            updateDisciplineButtons();
        });
        btnDiscArch.setOnClickListener(v -> {
            selectedDiscipline = "Architectural";
            updateDisciplineButtons();
        });
        btnDiscElec.setOnClickListener(v -> {
            selectedDiscipline = "Electrical";
            updateDisciplineButtons();
        });
        btnDiscPlumb.setOnClickListener(v -> {
            selectedDiscipline = "Plumbing";
            updateDisciplineButtons();
        });

        // Launch "Check Level" slope dialog
        btnCheckLevelTool.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            CheckLevelDialog dialog = new CheckLevelDialog();
            dialog.setOnMeasurementSavedListener(formattedMeasurement -> {
                capturedMeasurement = formattedMeasurement;
                tvCapturedSlope.setText(formattedMeasurement);
                tvCapturedSlope.setTextColor(ContextCompat.getColor(requireContext(), R.color.oxide_blue_primary));
            });
            dialog.show(getParentFragmentManager(), "CheckLevelDialog");
        });

        btnSubmitInspection.setOnClickListener(v -> {
            String title = etInspectionTitle.getText() != null ? etInspectionTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                etInspectionTitle.setError("Title is required");
                return;
            }

            String notes = etInspectionNotes.getText() != null ? etInspectionNotes.getText().toString().trim() : "";
            if (!capturedMeasurement.isEmpty()) {
                notes = (notes.isEmpty() ? "" : notes + "\n") + "[SLOPE CHECK: " + capturedMeasurement + "]";
            }

            HapticFeedbackUtil.heavyClick(requireContext(), v);

            String dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            SiteInspection item = new SiteInspection(
                    UUID.randomUUID().toString(),
                    "Metro Central Tower",
                    "Building 2 • Level 14",
                    selectedDiscipline,
                    dateStr,
                    SiteInspection.STATUS_PENDING,
                    notes,
                    title,
                    "Site Engineer",
                    "",
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            InspectionRepository repo = new InspectionRepository(requireContext());
            repo.addInspection(item);

            if (listener != null) {
                listener.onInspectionCreated(item);
            }

            Toast.makeText(requireContext(), "Inspection Checklist Dispatched", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private void updateDisciplineButtons() {
        int oxideBlue = ContextCompat.getColor(requireContext(), R.color.oxide_blue_primary);
        int white = ContextCompat.getColor(requireContext(), R.color.text_on_oxide_white);
        int ashSurface = ContextCompat.getColor(requireContext(), R.color.warm_ash_surface);
        int textPrimary = ContextCompat.getColor(requireContext(), R.color.text_oxide_primary);

        boolean isStruct = "Structural".equalsIgnoreCase(selectedDiscipline);
        btnDiscStructural.setBackgroundColor(isStruct ? oxideBlue : ashSurface);
        btnDiscStructural.setTextColor(isStruct ? white : textPrimary);
        btnDiscStructural.setStrokeColorResource(R.color.stroke_oxide_blue);
        btnDiscStructural.setStrokeWidth(isStruct ? 0 : 3);

        boolean isArch = "Architectural".equalsIgnoreCase(selectedDiscipline);
        btnDiscArch.setBackgroundColor(isArch ? oxideBlue : ashSurface);
        btnDiscArch.setTextColor(isArch ? white : textPrimary);
        btnDiscArch.setStrokeColorResource(R.color.stroke_oxide_blue);
        btnDiscArch.setStrokeWidth(isArch ? 0 : 3);

        boolean isElec = "Electrical".equalsIgnoreCase(selectedDiscipline);
        btnDiscElec.setBackgroundColor(isElec ? oxideBlue : ashSurface);
        btnDiscElec.setTextColor(isElec ? white : textPrimary);
        btnDiscElec.setStrokeColorResource(R.color.stroke_oxide_blue);
        btnDiscElec.setStrokeWidth(isElec ? 0 : 3);

        boolean isPlumb = "Plumbing".equalsIgnoreCase(selectedDiscipline);
        btnDiscPlumb.setBackgroundColor(isPlumb ? oxideBlue : ashSurface);
        btnDiscPlumb.setTextColor(isPlumb ? white : textPrimary);
        btnDiscPlumb.setStrokeColorResource(R.color.stroke_oxide_blue);
        btnDiscPlumb.setStrokeWidth(isPlumb ? 0 : 3);
    }
}
