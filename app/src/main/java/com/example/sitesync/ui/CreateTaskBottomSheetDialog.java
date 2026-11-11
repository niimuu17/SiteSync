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
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.data.local.entities.TaskEntity;
import com.example.sitesync.databinding.DialogCreateTaskBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.example.sitesync.utils.TaskReminderScheduler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CreateTaskBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnTaskCreatedListener {
        void onTaskCreated(TaskEntity task);
    }

    private DialogCreateTaskBinding binding;
    private OnTaskCreatedListener listener;
    private long selectedDeadlineMillis = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000); // 3 days default
    private String selectedPriority = "MEDIUM";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    private ActivityResultLauncher<Intent> speechLauncher;

    public static CreateTaskBottomSheetDialog newInstance() {
        return new CreateTaskBottomSheetDialog();
    }

    public void setOnTaskCreatedListener(OnTaskCreatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> spoken = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (spoken != null && !spoken.isEmpty()) {
                            String existing = binding.etTaskDescription.getText().toString().trim();
                            if (!existing.isEmpty()) {
                                binding.etTaskDescription.setText(existing + " " + spoken.get(0));
                            } else {
                                binding.etTaskDescription.setText(spoken.get(0));
                            }
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogCreateTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Pre-fill deadline
        binding.tvSelectedDeadline.setText(dateFormat.format(new Date(selectedDeadlineMillis)));

        setupPriorityPills();
        setupAssigneeSpinner();
        setupDeadlinePicker();
        setupSpeechDictation();
        setupActionButtons();
    }

    private void setupPriorityPills() {
        binding.pillPriorityLow.setOnClickListener(v -> setPriority("LOW"));
        binding.pillPriorityMedium.setOnClickListener(v -> setPriority("MEDIUM"));
        binding.pillPriorityUrgent.setOnClickListener(v -> setPriority("URGENT"));
    }

    private void setPriority(String priority) {
        selectedPriority = priority;
        HapticFeedbackUtil.lightClick(requireContext(), null);

        // Reset all pills
        binding.pillPriorityLow.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillPriorityLow.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_primary));

        binding.pillPriorityMedium.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillPriorityMedium.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_primary));

        binding.pillPriorityUrgent.setBackgroundResource(R.drawable.bg_pill_inactive);
        binding.pillPriorityUrgent.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_primary));

        if ("LOW".equals(priority)) {
            binding.pillPriorityLow.setBackgroundResource(R.drawable.bg_tab_pill_active);
            binding.pillPriorityLow.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_oxide_white));
        } else if ("URGENT".equals(priority)) {
            binding.pillPriorityUrgent.setBackgroundResource(R.drawable.bg_badge_urgent);
            binding.pillPriorityUrgent.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
        } else {
            binding.pillPriorityMedium.setBackgroundResource(R.drawable.bg_tab_pill_active);
            binding.pillPriorityMedium.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_oxide_white));
        }
    }

    private void setupAssigneeSpinner() {
        String[] members = new String[]{
                "Elena Rostova (Lead Eng)",
                "Marco Rossi (Supervisor)",
                "ElectroCorp Crew A",
                "Skyline Concrete Team",
                "Steel Fixers Squad",
                "Safety & Compliance Lead"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), R.layout.item_spinner_task_status, members);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.spinnerAssignee.setAdapter(adapter);
    }

    private void setupDeadlinePicker() {
        binding.layoutPickDeadline.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Task Deadline")
                    .setSelection(selectedDeadlineMillis)
                    .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    selectedDeadlineMillis = selection;
                    binding.tvSelectedDeadline.setText(dateFormat.format(new Date(selectedDeadlineMillis)));
                }
            });

            picker.show(getParentFragmentManager(), "TASK_DEADLINE_PICKER");
        });
    }

    private void setupSpeechDictation() {
        binding.btnMicDictate.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak task details...");
                speechLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Speech recognition unavailable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActionButtons() {
        binding.btnCancelCreateTask.setOnClickListener(v -> dismiss());

        binding.btnSaveTask.setOnClickListener(v -> {
            String title = binding.etTaskTitle.getText().toString().trim();
            String description = binding.etTaskDescription.getText().toString().trim();
            String assignee = (String) binding.spinnerAssignee.getSelectedItem();

            if (title.isEmpty()) {
                binding.etTaskTitle.setError("Please enter a task title");
                binding.etTaskTitle.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                description = "General field deliverable for " + title;
            }

            HapticFeedbackUtil.confirm(requireContext());

            String taskId = "TSK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            long now = System.currentTimeMillis();
            com.example.sitesync.data.SessionManager sessionManager = new com.example.sitesync.data.SessionManager(requireContext());
            String pCode = sessionManager.getActiveProjectCode();
            if (pCode == null || pCode.isEmpty()) pCode = "PRJ-DHAKA-101";

            TaskEntity task = new TaskEntity(
                    taskId,
                    pCode,
                    title,
                    description,
                    assignee,
                    selectedPriority,
                    "TODO",
                    selectedDeadlineMillis,
                    true,
                    now,
                    false
            );

            // Persist locally in Room
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            db.taskDao().insert(task);

            // Queue for cloud sync
            PendingSyncEntity syncItem = new PendingSyncEntity(
                    UUID.randomUUID().toString(),
                    "TASK",
                    taskId,
                    "CREATE",
                    "{\"title\":\"" + title + "\",\"priority\":\"" + selectedPriority + "\"}",
                    now
            );
            db.pendingSyncDao().insert(syncItem);

            // Schedule push reminder alarm
            TaskReminderScheduler.scheduleReminders(requireContext(), task);

            Toast.makeText(requireContext(), "Task Scheduled: " + title, Toast.LENGTH_SHORT).show();

            if (listener != null) {
                listener.onTaskCreated(task);
            }

            dismiss();
        });
    }
}
