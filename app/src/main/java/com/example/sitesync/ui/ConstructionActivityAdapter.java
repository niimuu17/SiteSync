package com.example.sitesync.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemConstructionActivityBinding;
import com.example.sitesync.models.ConstructionActivity;

import java.util.ArrayList;
import java.util.List;

public class ConstructionActivityAdapter extends RecyclerView.Adapter<ConstructionActivityAdapter.ActivityViewHolder> {

    public interface OnActivityActionListener {
        void onDetailsClicked(ConstructionActivity activity);
        void onUpdateClicked(ConstructionActivity activity);
    }

    private final List<ConstructionActivity> activityList = new ArrayList<>();
    private final OnActivityActionListener actionListener;

    public ConstructionActivityAdapter(OnActivityActionListener listener) {
        this.actionListener = listener;
    }

    public void setActivities(List<ConstructionActivity> list) {
        activityList.clear();
        if (list != null) {
            activityList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConstructionActivityBinding binding = ItemConstructionActivityBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ActivityViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        holder.bind(activityList.get(position));
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final ItemConstructionActivityBinding binding;

        public ActivityViewHolder(ItemConstructionActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ConstructionActivity activity) {
            binding.tvCardProjectName.setText(activity.getProjectName());
            binding.tvCardActivityName.setText(activity.getActivityName());
            binding.tvCardLocation.setText(activity.getLocation());
            binding.tvCardProgressPercent.setText(activity.getProgressPercentage() + "%");
            binding.pbCardProgress.setProgress(activity.getProgressPercentage());
            binding.tvCardLastUpdated.setText("Updated: " + activity.getLastUpdated() + " • " + activity.getUpdatedBy());

            // Status Badge Coloring
            applyStatusStyle(activity.getStatus(), activity.getProgressPercentage());

            // Action listeners
            itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDetailsClicked(activity);
            });
            binding.btnCardDetails.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDetailsClicked(activity);
            });
            binding.btnCardUpdate.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onUpdateClicked(activity);
            });
        }

        private void applyStatusStyle(String status, int progress) {
            binding.tvCardStatusBadge.setText(status);

            if (ConstructionActivity.STATUS_COMPLETED.equalsIgnoreCase(status) || progress == 100) {
                binding.tvCardStatusBadge.setBackgroundColor(Color.parseColor("#123324"));
                binding.tvCardStatusBadge.setTextColor(Color.parseColor("#10B981"));
                binding.pbCardProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
            } else if (ConstructionActivity.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                binding.tvCardStatusBadge.setBackgroundColor(Color.parseColor("#382B14"));
                binding.tvCardStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
                binding.pbCardProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            } else {
                binding.tvCardStatusBadge.setBackgroundColor(Color.parseColor("#222631"));
                binding.tvCardStatusBadge.setTextColor(Color.parseColor("#94A3B8"));
                binding.pbCardProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#2E3442")));
            }
        }
    }
}
