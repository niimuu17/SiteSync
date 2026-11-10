package com.example.sitesync.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.TaskEntity;
import com.example.sitesync.databinding.ItemTaskCardBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onStatusChanged(TaskEntity task, String newStatus);
        void onNotesClicked(TaskEntity task);
    }

    private final Context context;
    private final List<TaskEntity> taskList = new ArrayList<>();
    private final OnTaskActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.US);

    public TaskAdapter(Context context, OnTaskActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTasks(List<TaskEntity> tasks) {
        this.taskList.clear();
        if (tasks != null) {
            this.taskList.addAll(tasks);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskCardBinding binding = ItemTaskCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(taskList.get(position));
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskCardBinding binding;

        TaskViewHolder(ItemTaskCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TaskEntity task) {
            binding.tvTaskTitle.setText(task.getTitle());
            binding.tvTaskDescription.setText(task.getDescription());
            binding.tvAssignee.setText(task.getAssignedTo() != null ? task.getAssignedTo() : "Elena Rostova");

            // Priority Badge Styling
            String priority = task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM";
            binding.tvPriorityBadge.setText(priority);
            if ("URGENT".equals(priority)) {
                binding.tvPriorityBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                binding.tvPriorityBadge.setTextColor(Color.parseColor("#991B1B"));
            } else if ("LOW".equals(priority)) {
                binding.tvPriorityBadge.setBackgroundResource(R.drawable.bg_badge_low);
                binding.tvPriorityBadge.setTextColor(Color.parseColor("#1E293B"));
            } else {
                binding.tvPriorityBadge.setBackgroundResource(R.drawable.bg_badge_medium);
                binding.tvPriorityBadge.setTextColor(Color.parseColor("#92400E"));
            }

            // Due Date & Overdue Calculation
            long now = System.currentTimeMillis();
            long due = task.getDueDate();
            if (due > 0) {
                String dateStr = dateFormat.format(new Date(due));
                if (now > due && !"COMPLETED".equals(task.getStatus())) {
                    binding.tvDueDate.setText("Overdue (" + dateStr + ")");
                    binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.status_red));
                } else {
                    long diffDays = Math.max(0, (due - now) / (24L * 60 * 60 * 1000));
                    binding.tvDueDate.setText(dateStr + " (" + diffDays + "d left)");
                    binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.text_oxide_secondary));
                }
            } else {
                binding.tvDueDate.setText("Flexible");
                binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.text_oxide_secondary));
            }

            // Status Spinner Setup
            String[] statuses = new String[]{"To-Do", "In-Progress", "Under Review", "Completed"};
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    context, R.layout.item_spinner_task_status, statuses);
            spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            binding.spinnerStatus.setAdapter(spinnerAdapter);

            int selectedIndex = 0;
            String status = task.getStatus();
            if ("IN_PROGRESS".equals(status)) selectedIndex = 1;
            else if ("REVIEW".equals(status)) selectedIndex = 2;
            else if ("COMPLETED".equals(status)) selectedIndex = 3;

            binding.spinnerStatus.setSelection(selectedIndex, false);

            final int initialIndex = selectedIndex;
            binding.spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    if (pos != initialIndex && listener != null) {
                        String newStatus = "TODO";
                        if (pos == 1) newStatus = "IN_PROGRESS";
                        else if (pos == 2) newStatus = "REVIEW";
                        else if (pos == 3) newStatus = "COMPLETED";

                        task.setStatus(newStatus);
                        listener.onStatusChanged(task, newStatus);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // View Notes Click
            binding.btnViewNotes.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotesClicked(task);
                }
            });
        }
    }
}
