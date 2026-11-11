package com.example.sitesync.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.entities.ProjectEntity;
import com.example.sitesync.data.repository.ProjectRepository;
import com.example.sitesync.databinding.DialogLogProgressEntryBinding;
import com.example.sitesync.hardware.ConstructionProgressEstimatorClient;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;
import java.util.UUID;

public class LogProgressBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnProgressLoggedListener {
        void onProgressUpdated(ProjectEntity updatedProject);
    }

    private DialogLogProgressEntryBinding binding;
    private ProjectRepository projectRepository;
    private SessionManager sessionManager;
    private ConstructionProgressEstimatorClient estimatorClient;
    private ProjectEntity currentProject;
    private OnProgressLoggedListener listener;

    public static LogProgressBottomSheetDialog newInstance() {
        return new LogProgressBottomSheetDialog();
    }

    public void setOnProgressLoggedListener(OnProgressLoggedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogLogProgressEntryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        projectRepository = ProjectRepository.getInstance(requireContext());
        sessionManager = new SessionManager(requireContext());
        estimatorClient = new ConstructionProgressEstimatorClient(requireContext());

        loadActiveProject();

        binding.btnCloseLogModal.setOnClickListener(v -> dismiss());
        binding.btnCancelDailyLog.setOnClickListener(v -> dismiss());
        binding.btnAnalyzeAndSaveLog.setOnClickListener(v -> analyzeAndSaveProgress());
    }

    private void loadActiveProject() {
        String pId = sessionManager.getActiveProjectId();
        projectRepository.getProjectById(pId, new ProjectRepository.ActionCallback<ProjectEntity>() {
            @Override
            public void onSuccess(ProjectEntity project) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    currentProject = project;
                    if (currentProject != null) {
                        binding.tvLogProjectSubtitle.setText(currentProject.getName() + " • " + currentProject.getProjectCode());
                        String scope = String.format(Locale.US, "Scope: %s • %.0fft Walls • %d Windows • %d Doors • %s",
                                currentProject.getBuildingType() != null ? currentProject.getBuildingType() : "Building",
                                currentProject.getWallHeightFeet() > 0 ? currentProject.getWallHeightFeet() : 10.0f,
                                currentProject.getNumWindows() > 0 ? currentProject.getNumWindows() : 4,
                                currentProject.getNumDoors() > 0 ? currentProject.getNumDoors() : 2,
                                currentProject.getFlooringType() != null ? currentProject.getFlooringType() : "Tiles");
                        binding.tvLogBaselineScope.setText(scope);
                    }
                });
            }

            @Override
            public void onError(Exception error) {}
        });
    }

    private void analyzeAndSaveProgress() {
        String logText = binding.etDailyLogInput.getText() != null ? binding.etDailyLogInput.getText().toString().trim() : "";
        if (logText.isEmpty()) {
            binding.etDailyLogInput.setError("Please describe today's work accomplishments");
            return;
        }

        if (currentProject == null) {
            currentProject = new ProjectEntity("PRJ-MCT-2026", "MCT-2026", "Metro Central Tower",
                    "Metro Urban Dev", "Skyline Infra", "Dhaka", "৳ 42.5 Crore", "Floor 3",
                    "2-Room Building", 2, 10.0f, 6, 4, "Tiles", "Standard residential scope", 68.0f);
        }

        ProgressDialog progress = new ProgressDialog(requireContext());
        progress.setMessage("AI Quantity Surveyor evaluating site progress against building scope...");
        progress.setCancelable(false);
        progress.show();

        HapticFeedbackUtil.lightClick(requireContext(), null);

        estimatorClient.estimateProgress(currentProject, logText, new ConstructionProgressEstimatorClient.OnEstimationCompletedListener() {
            @Override
            public void onSuccess(ConstructionProgressEstimatorClient.ProgressEstimationResult result) {
                if (!isAdded()) return;
                if (progress.isShowing()) progress.dismiss();

                HapticFeedbackUtil.scanSuccess(requireContext());

                // Update active project metrics
                currentProject.setOverallProgressPercent(result.overallProgressPercent);
                currentProject.setCivilProgressPercent(result.civilProgress);
                currentProject.setElectricProgressPercent(result.electricProgress);
                currentProject.setPlumbingProgressPercent(result.plumbingProgress);
                currentProject.setThaiGlassProgressPercent(result.thaiGlassProgress);
                currentProject.setTilesFlooringProgressPercent(result.tilesFlooringProgress);
                currentProject.setInteriorPlasterProgressPercent(result.interiorPlasterProgress);

                projectRepository.updateProgressMetrics(currentProject.getId(),
                        result.overallProgressPercent, result.civilProgress, result.electricProgress,
                        result.plumbingProgress, result.thaiGlassProgress, result.tilesFlooringProgress,
                        result.interiorPlasterProgress, new ProjectRepository.ActionCallback<Void>() {
                            @Override
                            public void onSuccess(Void res) {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    showAiResult(result);
                                    if (listener != null) {
                                        listener.onProgressUpdated(currentProject);
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception error) {}
                        });
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                if (progress.isShowing()) progress.dismiss();
                Toast.makeText(requireContext(), "Estimation notice: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAiResult(ConstructionProgressEstimatorClient.ProgressEstimationResult result) {
        binding.layoutAiResultContainer.setVisibility(View.VISIBLE);
        binding.tvAiModelBadge.setText("AI SURVEYOR ANALYSIS (" + result.modelUsed + ")");
        binding.tvAiEstimatedOverall.setText(String.format(Locale.US, "Estimated Overall Progress: %.1f%%", result.overallProgressPercent));

        String tradeLine = String.format(Locale.US, "Thai Glass: %.0f%% • Tiles: %.0f%% • Electric: %.0f%% • Civil: %.0f%%",
                result.thaiGlassProgress, result.tilesFlooringProgress, result.electricProgress, result.civilProgress);
        binding.tvAiTradeBreakdownText.setText(tradeLine);
        binding.tvAiCompletedSummary.setText("Done: " + result.completedTodaySummary);
        binding.tvAiRemainingSummary.setText("Pending: " + result.remainingWorkSummary);

        binding.btnAnalyzeAndSaveLog.setText("Saved to Database");
        binding.btnAnalyzeAndSaveLog.setEnabled(false);

        Toast.makeText(requireContext(), "Daily Log Recorded & Progress Updated!", Toast.LENGTH_SHORT).show();

        binding.btnCancelDailyLog.setText("Done");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
