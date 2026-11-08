package com.example.sitesync.ui.cockpit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sitesync.R;
import com.example.sitesync.data.repository.UnifiedProjectRepository;
import com.example.sitesync.hardware.MatchAngleCameraDialog;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.ui.custom.CutAndFillTelemetryView;
import com.example.sitesync.ui.custom.HatchedProgressBar;
import com.example.sitesync.ui.dialogs.UpdateProgressBottomSheet;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.button.MaterialButton;

/**
 * Tab 2: Overall Construction Progress & Earthworks Telemetry
 * Hosts the 68% hero card, diagonal hatched hazard progress bar, cut & fill telemetry,
 * and launches CameraX "Match Angle" photogrammetry ghosting dialogs.
 */
public class ProgressTabFragment extends Fragment {

    private TextView tvHeroProgressPercent;
    private HatchedProgressBar hatchedProgressBar;
    private CutAndFillTelemetryView cutAndFillView;
    private ImageView btnProgressCalendar;
    private ImageView btnProgressChart;
    private MaterialButton btnUpdateMilestone;
    private MaterialButton btnMatchAngleFoundation;
    private MaterialButton btnMatchAngleColumns;

    private UnifiedProjectRepository repository;

    public static ProgressTabFragment newInstance() {
        return new ProgressTabFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = UnifiedProjectRepository.getInstance(requireContext());

        tvHeroProgressPercent = view.findViewById(R.id.tvHeroProgressPercent);
        hatchedProgressBar = view.findViewById(R.id.hatchedProgressBar);
        cutAndFillView = view.findViewById(R.id.cutAndFillView);
        btnProgressCalendar = view.findViewById(R.id.btnProgressCalendar);
        btnProgressChart = view.findViewById(R.id.btnProgressChart);
        btnUpdateMilestone = view.findViewById(R.id.btnUpdateMilestone);
        btnMatchAngleFoundation = view.findViewById(R.id.btnMatchAngleFoundation);
        btnMatchAngleColumns = view.findViewById(R.id.btnMatchAngleColumns);

        // Update overall percentage
        int overall = repository.getOverallProgressPercentage();
        if (overall > 0) {
            tvHeroProgressPercent.setText(overall + " %");
            hatchedProgressBar.setProgress(overall);
        }

        btnProgressCalendar.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            Toast.makeText(requireContext(), "Schedule Milestone Timeline: 11 Feb - 23 Mar 2026", Toast.LENGTH_SHORT).show();
        });

        btnProgressChart.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            Toast.makeText(requireContext(), "Telemetry View: Real-Time S-Curve Analysis", Toast.LENGTH_SHORT).show();
        });

        btnUpdateMilestone.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            ConstructionActivity act = new ConstructionActivity();
            act.setId("ACT-HERO");
            act.setName("Project Milestone Completion");
            act.setProgress(68);
            act.setStatus("IN PROGRESS");

            UpdateProgressBottomSheet sheet = UpdateProgressBottomSheet.newInstance(act);
            sheet.setOnProgressUpdatedListener((newProgress, status, notes) -> {
                tvHeroProgressPercent.setText(newProgress + " %");
                hatchedProgressBar.setProgress(newProgress);
            });
            sheet.show(getParentFragmentManager(), "UpdateProgressBottomSheet");
        });

        // Launch "Match Angle" Ghost Camera for Foundation
        btnMatchAngleFoundation.setOnClickListener(v -> launchMatchAngleCamera("Foundation Pour L-14"));

        // Launch "Match Angle" Ghost Camera for Column Erection
        btnMatchAngleColumns.setOnClickListener(v -> launchMatchAngleCamera("Column Erection L-14 Core"));
    }

    private void launchMatchAngleCamera(String taskLocation) {
        HapticFeedbackUtil.heavyClick(requireContext(), null);
        MatchAngleCameraDialog dialog = MatchAngleCameraDialog.newInstance(
                "", // baseline photo uri
                "ELENA ROSTOVA (ENG-04)",
                taskLocation
        );
        dialog.setOnPhotoVerifiedCallback((photoFile, photoUri) -> {
            Toast.makeText(requireContext(), "Angle Aligned & Watermarked: " + photoFile.getName(), Toast.LENGTH_LONG).show();
        });
        dialog.show(getParentFragmentManager(), "MatchAngleCameraDialog");
    }
}
