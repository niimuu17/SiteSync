package com.example.sitesync.ui.cockpit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sitesync.R;
import com.example.sitesync.data.AccessControl;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.hardware.CheckLevelDialog;
import com.example.sitesync.models.User;
import com.example.sitesync.ui.PdfViewerActivity;
import com.example.sitesync.ui.dialogs.CreateInspectionBottomSheet;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.button.MaterialButton;

/**
 * Tab 3: QA Checklists, Tolerance Telemetry & CAD Blueprints
 * Fast-access to approved PDF blueprints and embedded inclinometer slope verification.
 * Enforces dynamic capability-based sign-offs via AccessControl.
 */
public class QualityCadTabFragment extends Fragment {

    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterStructural;
    private MaterialButton btnFilterArch;
    private MaterialButton btnFilterMep;

    private MaterialButton btnViewBlueprint1;
    private MaterialButton btnViewBlueprint2;
    private MaterialButton btnNewInspection;
    private MaterialButton btnCheckLevelInspection;
    private MaterialButton btnSignOffRebar;

    private TextView tvMeasuredSlopeResult;
    private SessionManager sessionManager;

    public static QualityCadTabFragment newInstance() {
        return new QualityCadTabFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quality_cad_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterStructural = view.findViewById(R.id.btnFilterStructural);
        btnFilterArch = view.findViewById(R.id.btnFilterArch);
        btnFilterMep = view.findViewById(R.id.btnFilterMep);

        btnViewBlueprint1 = view.findViewById(R.id.btnViewBlueprint1);
        btnViewBlueprint2 = view.findViewById(R.id.btnViewBlueprint2);
        btnNewInspection = view.findViewById(R.id.btnNewInspection);
        btnCheckLevelInspection = view.findViewById(R.id.btnCheckLevelInspection);
        btnSignOffRebar = view.findViewById(R.id.btnSignOffRebar);

        tvMeasuredSlopeResult = view.findViewById(R.id.tvMeasuredSlopeResult);

        // Filter button clicks
        btnFilterAll.setOnClickListener(v -> highlightFilter(btnFilterAll));
        btnFilterStructural.setOnClickListener(v -> highlightFilter(btnFilterStructural));
        btnFilterArch.setOnClickListener(v -> highlightFilter(btnFilterArch));
        btnFilterMep.setOnClickListener(v -> highlightFilter(btnFilterMep));

        // View Blueprint 1 (Architectural)
        btnViewBlueprint1.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            openPdfViewer("DOC-001", "Rev 2.1");
        });

        // View Blueprint 2 (Structural)
        btnViewBlueprint2.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            openPdfViewer("DOC-002", "Rev 3.0");
        });

        // Trigger New Inspection Bottom Sheet
        btnNewInspection.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            CreateInspectionBottomSheet sheet = CreateInspectionBottomSheet.newInstance();
            sheet.show(getParentFragmentManager(), "CreateInspectionBottomSheet");
        });

        // Check Level Slope Verification embedded in QA checklist
        btnCheckLevelInspection.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            CheckLevelDialog dialog = new CheckLevelDialog();
            dialog.setOnMeasurementSavedListener(measurement -> {
                tvMeasuredSlopeResult.setText("Verified: " + measurement + " (PASSED TOLERANCE)");
                tvMeasuredSlopeResult.setTextColor(requireContext().getColor(R.color.oxide_blue_primary));
            });
            dialog.show(getParentFragmentManager(), "CheckLevelDialog");
        });

        // Sign-off inspection: Capability-checked via AccessControl
        btnSignOffRebar.setOnClickListener(v -> {
            User currentUser = sessionManager.getCurrentUser();
            AccessControl.checkOrPromptElevation(
                    requireContext(),
                    currentUser,
                    "Sign-Off Inspection",
                    "Site Supervisor / Project Manager",
                    () -> {
                        HapticFeedbackUtil.heavyClick(requireContext(), btnSignOffRebar);
                        btnSignOffRebar.setEnabled(false);
                        btnSignOffRebar.setText("SIGNED-OFF &amp; APPROVED");
                        Toast.makeText(requireContext(), "Inspection Signed-Off Successfully", Toast.LENGTH_SHORT).show();
                    }
            );
        });
    }

    private void openPdfViewer(String docId, String rev) {
        Intent intent = new Intent(requireContext(), PdfViewerActivity.class);
        intent.putExtra("extra_doc_id", docId);
        intent.putExtra(PdfViewerActivity.EXTRA_REVISION_NUMBER, rev);
        startActivity(intent);
    }

    private void highlightFilter(MaterialButton selected) {
        MaterialButton[] buttons = new MaterialButton[]{btnFilterAll, btnFilterStructural, btnFilterArch, btnFilterMep};
        int oxidePrimary = requireContext().getColor(R.color.oxide_blue_primary);
        int whiteText = requireContext().getColor(R.color.text_on_oxide_white);
        int surface = requireContext().getColor(R.color.warm_ash_surface);
        int oxideText = requireContext().getColor(R.color.text_oxide_primary);

        for (MaterialButton btn : buttons) {
            if (btn == selected) {
                btn.setBackgroundColor(oxidePrimary);
                btn.setTextColor(whiteText);
            } else {
                btn.setBackgroundColor(surface);
                btn.setTextColor(oxideText);
            }
        }
    }
}
