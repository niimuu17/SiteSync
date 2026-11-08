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
                "TSK-401",
                "Rebar Clearance Inspection - Grid 4B",
                "Check bottom steel cover before slab pour. Verify rebar chairs.",
                "Elena Rostova (Lead Eng)",
                "URGENT",
                "IN_PROGRESS",
                now - (day / 2), // overdue
                true,
                now - (2 * day),
                true
        ));

        seed.add(new TaskEntity(
                "TSK-402",
                "Conduit Pulling - Floor 3 Corridors",
                "Electrical subcontractor conduit runs and junction boxes.",
                "ElectroCorp Crew A",
                "MEDIUM",
                "TODO",
                now + (3 * day),
                true,
                now - day,
                true
        ));

        seed.add(new TaskEntity(
                "TSK-403",
                "Perimeter Fall Netting Verification",
                "Inspect safety netting along North cantilever slab edge.",
                "Safety & Compliance Lead",
                "URGENT",
                "TODO",
                now + day,
                true,
                now - day,
                true
        ));

        seed.add(new TaskEntity(
                "TSK-404",
                "Core Column Formwork Stripping",
                "Verify 7-day cylinder compressive strength test before strip.",
                "Skyline Concrete Team",
                "LOW",
                "REVIEW",
                now + (5 * day),
                true,
                now - (3 * day),
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
