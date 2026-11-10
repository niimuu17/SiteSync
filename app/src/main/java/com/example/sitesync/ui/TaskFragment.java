package com.example.sitesync.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.data.local.entities.TaskEntity;
import com.example.sitesync.databinding.FragmentTaskBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskFragment extends Fragment implements TaskAdapter.OnTaskActionListener {

    private FragmentTaskBinding binding;
    private TaskAdapter taskAdapter;
    private AppDatabase db;
    private List<TaskEntity> allTasks = new ArrayList<>();
    private String currentFilter = "ALL";

    public static TaskFragment newInstance() {
        return new TaskFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getDatabase(requireContext());

        setupRecyclerView();
        setupFilterChips();
        setupFab();
        loadAndSeedTasks();
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(requireContext(), this);
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(taskAdapter);
    }

    private void setupFilterChips() {
        binding.chipGroupTaskFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            HapticFeedbackUtil.lightClick(requireContext(), null);

            if (checkedId == R.id.chipFilterTodo) {
                currentFilter = "TODO";
            } else if (checkedId == R.id.chipFilterInProgress) {
                currentFilter = "IN_PROGRESS";
            } else if (checkedId == R.id.chipFilterReview) {
                currentFilter = "REVIEW";
            } else if (checkedId == R.id.chipFilterCompleted) {
                currentFilter = "COMPLETED";
            } else {
                currentFilter = "ALL";
            }
            applyCurrentFilter();
        });
    }

    private void setupFab() {
        binding.fabCreateTask.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            CreateTaskBottomSheetDialog dialog = CreateTaskBottomSheetDialog.newInstance();
            dialog.setOnTaskCreatedListener(task -> {
                loadAndSeedTasks();
            });
            dialog.show(getParentFragmentManager(), "CREATE_TASK_SHEET");
        });
    }

    private void loadAndSeedTasks() {
        List<TaskEntity> existing = db.taskDao().getAllTasksSync();
        if (existing == null || existing.isEmpty()) {
            seedDefaultTasks();
            existing = db.taskDao().getAllTasksSync();
        }

        allTasks.clear();
        if (existing != null) {
            allTasks.addAll(existing);
        }

        applyCurrentFilter();
    }

    private void seedDefaultTasks() {
        long now = System.currentTimeMillis();
        long day = 24L * 60 * 60 * 1000;

        List<TaskEntity> seed = new ArrayList<>();
        seed.add(new TaskEntity(
                "TSK-BD-501",
                "Bending & Binding Rebar Rings (10mm Ring / Churi for Beams)",
                "Bend 10mm deformed rebar into square stirrup rings for 2nd floor lintel and roof beams with 6-inch pitch spacing.",
                "Habib Mistry (Lead Rod Binder)",
                "URGENT",
                "IN_PROGRESS",
                now - (day / 3),
                true,
                now - (2 * day),
                true
        ));

        seed.add(new TaskEntity(
                "TSK-BD-502",
                "Wall Plastering (1:4 Cement Sand Ratio) - 2nd Floor Unit A",
                "Apply 12mm thick sand cement plaster on brick masonry walls. Ensure straight spirit level plumb line finish.",
                "Jalal Uddin (Head Raj Mistry)",
                "MEDIUM",
                "TODO",
                now + (2 * day),
                true,
                now - day,
                true
        ));

        seed.add(new TaskEntity(
                "TSK-BD-503",
                "Thai Aluminum Sliding Window Fitting & Glass Sealant",
                "Fit heavy-grade bronze Thai aluminum window frames, install 5mm tinted glass panels, and apply silicone weather sealant.",
                "Alamgir Hossain (Thai Fabricator)",
                "HIGH",
                "IN_PROGRESS",
                now + day,
                true,
                now - day,
                true
        ));

        seed.add(new TaskEntity(
                "TSK-BD-504",
                "Floor Porcelain Tiles Setting (600x600mm) with Grouting",
                "Lay 24x24 inch mirror-polished porcelain floor tiles with 2mm tile leveling spacers and epoxy waterproof grout.",
                "Sumon Mia (Master Tile Setter)",
                "MEDIUM",
                "REVIEW",
                now + (3 * day),
                true,
                now - (2 * day),
                true
        ));

        seed.add(new TaskEntity(
                "TSK-BD-505",
                "Concealed Electrical Conduit Wall Chipping & Box Fixing",
                "Cut wall grooves for 25mm PVC electrical conduits, fix 8-module metal gang boxes at 4-foot height, and run draw wires.",
                "Mizanur Rahman (Senior Electrician)",
                "HIGH",
                "TODO",
                now + (4 * day),
                true,
                now - day,
                true
        ));

        seed.add(new TaskEntity(
                "TSK-BD-506",
                "Roof Slab Concrete Water Ponding & Curing Inspection",
                "Construct 2-inch cement mortar bunds around roof slab perimeter and maintain 14 continuous days of clean water ponding.",
                "Rafiqul Islam (Site Supervisor)",
                "LOW",
                "COMPLETED",
                now - day,
                true,
                now - (7 * day),
                true
        ));

        db.taskDao().insertAll(seed);
    }

    private void applyCurrentFilter() {
        List<TaskEntity> filtered = new ArrayList<>();
        int activeCount = 0;

        for (TaskEntity task : allTasks) {
            if (!"COMPLETED".equals(task.getStatus())) {
                activeCount++;
            }
            if ("ALL".equals(currentFilter) || currentFilter.equals(task.getStatus())) {
                filtered.add(task);
            }
        }

        taskAdapter.setTasks(filtered);
        binding.tvTaskCountBadge.setText("(" + filtered.size() + ")");
        binding.tvMilestonesBadge.setText(activeCount + " ACTIVE");

        if (filtered.isEmpty()) {
            binding.layoutEmptyTasks.setVisibility(View.VISIBLE);
            binding.rvTasks.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyTasks.setVisibility(View.GONE);
            binding.rvTasks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStatusChanged(TaskEntity task, String newStatus) {
        HapticFeedbackUtil.confirm(requireContext());
        db.taskDao().updateTaskStatus(task.getId(), newStatus);

        // Queue pending update for sync
        PendingSyncEntity syncItem = new PendingSyncEntity(
                UUID.randomUUID().toString(),
                "TASK",
                task.getId(),
                "UPDATE",
                "{\"status\":\"" + newStatus + "\"}",
                System.currentTimeMillis()
        );
        db.pendingSyncDao().insert(syncItem);

        Toast.makeText(requireContext(), "Status Updated: " + newStatus, Toast.LENGTH_SHORT).show();
        loadAndSeedTasks();
    }

    @Override
    public void onNotesClicked(TaskEntity task) {
        HapticFeedbackUtil.lightClick(requireContext(), null);

        String message = "Description: " + task.getDescription() + "\n\n" +
                "Assigned to: " + task.getAssignedTo() + "\n" +
                "Priority: " + task.getPriority() + "\n" +
                "Current Status: " + task.getStatus() + "\n" +
                "Sync State: Local Cache (Room DB)";

        new AlertDialog.Builder(requireContext())
                .setTitle(task.getTitle())
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
