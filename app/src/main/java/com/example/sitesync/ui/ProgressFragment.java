package com.example.sitesync.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.entities.ProjectEntity;
import com.example.sitesync.data.repository.ProjectRepository;
import com.example.sitesync.databinding.FragmentProgressBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.Locale;

public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;
    private ProjectRepository projectRepository;
    private SessionManager sessionManager;
    private ProjectEntity activeProject;

    public static ProgressFragment newInstance() {
        return new ProgressFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        projectRepository = ProjectRepository.getInstance(requireContext());
        sessionManager = new SessionManager(requireContext());

        setupMiniCards();
        observeActiveProject();
    }

    private void setupMiniCards() {
        // Add / Edit Project Plan Button
        binding.btnAddProjectPlan.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            ProjectPlanBottomSheetDialog dialog = ProjectPlanBottomSheetDialog.newInstance(activeProject);
            dialog.setOnProjectSavedListener(updated -> {
                bindProjectData(updated);
            });
            dialog.show(getChildFragmentManager(), "ProjectPlanDialog");
        });

        // Mini Card 1: Log Daily Progress Entry (AI LLM Evaluator)
        binding.cardMiniLogProgress.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            LogProgressBottomSheetDialog dialog = LogProgressBottomSheetDialog.newInstance();
            dialog.setOnProgressLoggedListener(updated -> {
                bindProjectData(updated);
            });
            dialog.show(getChildFragmentManager(), "LogProgressDialog");
        });

        // Mini Card 2: Geotagged Site Evidence Camera
        binding.cardMiniEvidenceCamera.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            SiteEvidenceBottomSheetDialog dialog = SiteEvidenceBottomSheetDialog.newInstance();
            dialog.show(getChildFragmentManager(), "SiteEvidenceDialog");
        });

        // Mini Card 3: Employee Attendance Table
        binding.cardMiniAttendanceTable.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            startActivity(new android.content.Intent(requireContext(), AttendanceTableActivity.class));
        });

        // Mini Card 4: Audit Trail & Report Export
        binding.cardMiniAuditExport.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            ReportExportBottomSheetDialog dialog = ReportExportBottomSheetDialog.newInstance();
            dialog.show(getChildFragmentManager(), "ReportExportDialog");
        });
    }

    private void observeActiveProject() {
        String pId = sessionManager.getActiveProjectId();
        projectRepository.getProjectByIdLive(pId).observe(getViewLifecycleOwner(), project -> {
            if (project != null) {
                bindProjectData(project);
            } else {
                // Fetch first available project if none active
                projectRepository.getRecentProjectsLive(1).observe(getViewLifecycleOwner(), list -> {
                    if (list != null && !list.isEmpty()) {
                        bindProjectData(list.get(0));
                    }
                });
            }
        });
    }

    private void bindProjectData(ProjectEntity project) {
        if (binding == null || project == null) return;
        activeProject = project;

        binding.tvProgressProjectHeader.setText(project.getName());

        int overall = Math.round(project.getOverallProgressPercent());
        binding.tvProgressPercent.setText(overall + "%");
        binding.hatchedProgressBar.setProgress(overall);

        // Bangladesh trade breakdown percentages
        binding.tvTradeCivilPercent.setText(String.format(Locale.US, "%.0f%%", project.getCivilProgressPercent()));
        binding.tvTradeThaiGlassPercent.setText(String.format(Locale.US, "%.0f%%", project.getThaiGlassProgressPercent()));
        binding.tvTradeTilesPercent.setText(String.format(Locale.US, "%.0f%%", project.getTilesFlooringProgressPercent()));
        binding.tvTradeElectricPercent.setText(String.format(Locale.US, "%.0f%%", project.getElectricProgressPercent()));
        binding.tvTradePlumbingPercent.setText(String.format(Locale.US, "%.0f%%", project.getPlumbingProgressPercent()));
        binding.tvTradePlasterPercent.setText(String.format(Locale.US, "%.0f%%", project.getInteriorPlasterProgressPercent()));

        String planSummary = String.format(Locale.US, "Plan: %s • %.0fft Walls • %d Windows • %d Doors • %s",
                project.getBuildingType() != null ? project.getBuildingType() : "Building Scope",
                project.getWallHeightFeet() > 0 ? project.getWallHeightFeet() : 10.0f,
                project.getNumWindows() > 0 ? project.getNumWindows() : 4,
                project.getNumDoors() > 0 ? project.getNumDoors() : 2,
                project.getFlooringType() != null ? project.getFlooringType() : "Tiles");
        binding.tvProgressPlanSummaryBrief.setText(planSummary);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
