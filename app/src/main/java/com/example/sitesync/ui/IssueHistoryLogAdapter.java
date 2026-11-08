package com.example.sitesync.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemIssueHistoryBinding;
import com.example.sitesync.models.IssueHistoryLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering audit trail history logs for an RFI / Technical Issue.
 */
public class IssueHistoryLogAdapter extends RecyclerView.Adapter<IssueHistoryLogAdapter.HistoryViewHolder> {

    private final List<IssueHistoryLog> historyLogs = new ArrayList<>();

    public void setHistoryLogs(List<IssueHistoryLog> list) {
        historyLogs.clear();
        if (list != null) {
            historyLogs.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemIssueHistoryBinding binding = ItemIssueHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(historyLogs.get(position));
    }

    @Override
    public int getItemCount() {
        return historyLogs.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemIssueHistoryBinding binding;

        public HistoryViewHolder(@NonNull ItemIssueHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(IssueHistoryLog log) {
            binding.tvHistoryAction.setText(log.getAction());
            binding.tvHistoryTimestamp.setText(log.getTimestamp());
            binding.tvHistoryPerformedBy.setText(log.getPerformedBy());
        }
    }
}
