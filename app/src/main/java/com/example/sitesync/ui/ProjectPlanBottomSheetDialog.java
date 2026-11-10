package com.example.sitesync.ui;

import android.app.Dialog;
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
import com.example.sitesync.data.local.entities.ProjectEntity;
import com.example.sitesync.data.repository.ProjectRepository;
import com.example.sitesync.databinding.DialogCreateProjectWithPlanBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.UUID;

public class ProjectPlanBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnProjectSavedListener {
        void onProjectSaved(ProjectEntity project);
    }

    private DialogCreateProjectWithPlanBinding binding;
    private ProjectRepository repository;
    private ProjectEntity existingProject;
    private OnProjectSavedListener listener;

    public static ProjectPlanBottomSheetDialog newInstance(@Nullable ProjectEntity existing) {
        ProjectPlanBottomSheetDialog dialog = new ProjectPlanBottomSheetDialog();
        Bundle args = new Bundle();
        if (existing != null) {
            args.putSerializable("project", existing);
        }
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnProjectSavedListener(OnProjectSavedListener listener) {
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
        binding = DialogCreateProjectWithPlanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = ProjectRepository.getInstance(requireContext());

        if (getArguments() != null && getArguments().containsKey("project")) {
            existingProject = (ProjectEntity) getArguments().getSerializable("project");
        }

        if (existingProject != null) {
            populateExistingData();
        }

        binding.btnCloseProjectPlanModal.setOnClickListener(v -> dismiss());
        binding.btnCancelProjectPlan.setOnClickListener(v -> dismiss());
        binding.btnSubmitProjectPlan.setOnClickListener(v -> saveProjectPlan());
    }

    private void populateExistingData() {
        binding.etPlanProjectCode.setText(existingProject.getProjectCode());
        binding.etPlanProjectName.setText(existingProject.getName());
        binding.etPlanClientLocation.setText(existingProject.getClientName() + " • " + existingProject.getLocation());
        binding.etPlanBuildingType.setText(existingProject.getBuildingType());
        binding.etPlanTotalRooms.setText(String.valueOf(existingProject.getTotalRooms()));
        binding.etPlanWallHeight.setText(String.valueOf(existingProject.getWallHeightFeet()));
        binding.etPlanNumWindows.setText(String.valueOf(existingProject.getNumWindows()));
        binding.etPlanNumDoors.setText(String.valueOf(existingProject.getNumDoors()));
        binding.etPlanAdditionalScope.setText(existingProject.getPlanDetailsJson());

        String floor = existingProject.getFlooringType();
        if (floor != null && floor.toLowerCase().contains("mosaic")) {
            binding.rbMosaic.setChecked(true);
        } else if (floor != null && floor.toLowerCase().contains("concrete")) {
            binding.rbConcrete.setChecked(true);
        } else {
            binding.rbTiles.setChecked(true);
        }

        binding.btnSubmitProjectPlan.setText("Update Project Plan");
    }

    private void saveProjectPlan() {
        String code = binding.etPlanProjectCode.getText() != null ? binding.etPlanProjectCode.getText().toString().trim() : "";
        String name = binding.etPlanProjectName.getText() != null ? binding.etPlanProjectName.getText().toString().trim() : "";
        String clientLoc = binding.etPlanClientLocation.getText() != null ? binding.etPlanClientLocation.getText().toString().trim() : "";
        String buildingType = binding.etPlanBuildingType.getText() != null ? binding.etPlanBuildingType.getText().toString().trim() : "";
        String roomsStr = binding.etPlanTotalRooms.getText() != null ? binding.etPlanTotalRooms.getText().toString().trim() : "2";
        String heightStr = binding.etPlanWallHeight.getText() != null ? binding.etPlanWallHeight.getText().toString().trim() : "10.0";
        String windowsStr = binding.etPlanNumWindows.getText() != null ? binding.etPlanNumWindows.getText().toString().trim() : "4";
        String doorsStr = binding.etPlanNumDoors.getText() != null ? binding.etPlanNumDoors.getText().toString().trim() : "2";
        String scopeDetails = binding.etPlanAdditionalScope.getText() != null ? binding.etPlanAdditionalScope.getText().toString().trim() : "";

        if (name.isEmpty()) {
            binding.etPlanProjectName.setError("Project Name is mandatory");
            return;
        }

        if (buildingType.isEmpty()) {
            binding.etPlanBuildingType.setError("Building scope is mandatory (e.g. 2-Room Building)");
            return;
        }

        int rooms = 2;
        try { rooms = Integer.parseInt(roomsStr); } catch (Exception ignored) {}

        float wallHeight = 10.0f;
        try { wallHeight = Float.parseFloat(heightStr); } catch (Exception ignored) {}

        int windows = 4;
        try { windows = Integer.parseInt(windowsStr); } catch (Exception ignored) {}

        int doors = 2;
        try { doors = Integer.parseInt(doorsStr); } catch (Exception ignored) {}

        String flooring = "Porcelain Floor Tiles";
        if (binding.rbMosaic.isChecked()) flooring = "Polished Mosaic Flooring";
        if (binding.rbConcrete.isChecked()) flooring = "Hardener Concrete Flooring";

        String pId = existingProject != null ? existingProject.getId() : "PRJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        if (code.isEmpty()) code = "PRJ-" + pId.substring(4, 8);

        ProjectEntity project = existingProject != null ? existingProject : new ProjectEntity();
        project.setId(pId);
        project.setProjectCode(code);
        project.setName(name);
        project.setClientName(clientLoc.contains("•") ? clientLoc.split("•")[0].trim() : clientLoc);
        project.setLocation(clientLoc.contains("•") ? clientLoc.split("•")[1].trim() : "Dhaka, Bangladesh");
        project.setBuildingType(buildingType);
        project.setTotalRooms(rooms);
        project.setWallHeightFeet(wallHeight);
        project.setNumWindows(windows);
        project.setNumDoors(doors);
        project.setFlooringType(flooring);
        project.setPlanDetailsJson(scopeDetails);
        project.setLastOpenedAt(System.currentTimeMillis());

        HapticFeedbackUtil.confirm(requireContext());
        repository.saveProject(project, new ProjectRepository.ActionCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Project Plan Initialized: " + project.getName(), Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onProjectSaved(project);
                    }
                    dismiss();
                });
            }

            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to save: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
