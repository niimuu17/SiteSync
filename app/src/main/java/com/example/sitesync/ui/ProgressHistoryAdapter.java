package com.example.sitesync.ui;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemProgressHistoryBinding;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.ProgressLog;

import java.util.ArrayList;
import java.util.List;

public class ProgressHistoryAdapter extends RecyclerView.Adapter<ProgressHistoryAdapter.HistoryViewHolder> {

    private final List<ProgressLog> historyList = new ArrayList<>();

    public void setHistory(List<ProgressLog> list) {
        historyList.clear();
        if (list != null) {
            historyList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProgressHistoryBinding binding = ItemProgressHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(historyList.get(position));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemProgressHistoryBinding binding;

        public HistoryViewHolder(ItemProgressHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProgressLog log) {
            binding.tvHistoryPercentage.setText(log.getPercentage() + "% Complete");
            binding.tvHistoryStatus.setText(log.getStatus());
            binding.tvHistoryTimestamp.setText(log.getTimestamp() + " • " + log.getUpdatedBy());

            if (log.getRemarks() != null && !log.getRemarks().trim().isEmpty()) {
                binding.tvHistoryRemarks.setText(log.getRemarks());
                binding.tvHistoryRemarks.setVisibility(View.VISIBLE);
            } else {
                binding.tvHistoryRemarks.setText("No specific remarks provided.");
                binding.tvHistoryRemarks.setVisibility(View.VISIBLE);
            }

            // Status Badge Coloring
            if (ConstructionActivity.STATUS_COMPLETED.equalsIgnoreCase(log.getStatus())) {
                binding.tvHistoryStatus.setBackgroundColor(Color.parseColor("#DCFCE7"));
                binding.tvHistoryStatus.setTextColor(Color.parseColor("#15803D"));
            } else if (ConstructionActivity.STATUS_IN_PROGRESS.equalsIgnoreCase(log.getStatus())) {
                binding.tvHistoryStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
                binding.tvHistoryStatus.setTextColor(Color.parseColor("#B45309"));
            } else {
                binding.tvHistoryStatus.setBackgroundColor(Color.parseColor("#F1F5F9"));
                binding.tvHistoryStatus.setTextColor(Color.parseColor("#475569"));
            }

            // Attached Photo Preview
            if (log.getPhotoUri() != null && !log.getPhotoUri().trim().isEmpty()) {
                try {
                    binding.ivHistoryPhoto.setImageURI(Uri.parse(log.getPhotoUri()));
                    binding.ivHistoryPhoto.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    binding.ivHistoryPhoto.setVisibility(View.GONE);
                }
            } else {
                binding.ivHistoryPhoto.setVisibility(View.GONE);
            }
        }
    }
}
