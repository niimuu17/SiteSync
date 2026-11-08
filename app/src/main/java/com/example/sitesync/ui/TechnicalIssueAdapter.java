package com.example.sitesync.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemTechnicalIssueBinding;
import com.example.sitesync.models.TechnicalIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering Technical Issues & RFIs in the engineer dashboard and tracking screen.
 */
public class TechnicalIssueAdapter extends RecyclerView.Adapter<TechnicalIssueAdapter.IssueViewHolder> {

    public interface OnIssueClickListener {
        void onIssueClick(TechnicalIssue issue);
    }

    private final List<TechnicalIssue> issueList = new ArrayList<>();
    private final OnIssueClickListener listener;

    public TechnicalIssueAdapter(OnIssueClickListener listener) {
        this.listener = listener;
    }

    public void setIssues(List<TechnicalIssue> list) {
        issueList.clear();
        if (list != null) {
            issueList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IssueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTechnicalIssueBinding binding = ItemTechnicalIssueBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new IssueViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull IssueViewHolder holder, int position) {
        holder.bind(issueList.get(position));
    }

    @Override
    public int getItemCount() {
        return issueList.size();
    }

    class IssueViewHolder extends RecyclerView.ViewHolder {
        private final ItemTechnicalIssueBinding binding;

        public IssueViewHolder(@NonNull ItemTechnicalIssueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TechnicalIssue issue) {
            binding.tvIssueId.setText(issue.getId());
            binding.tvIssueTitle.setText(issue.getTitle());
            binding.tvIssueDescription.setText(issue.getDescription());
            binding.tvProjectName.setText(issue.getProjectName());
            binding.tvSiteLocation.setText(issue.getSiteLocation());
            binding.tvAssignedPerson.setText(issue.getAssignedPerson());

            int commentCount = issue.getComments() != null ? issue.getComments().size() : 0;
            binding.tvCommentsCount.setText(commentCount + (commentCount == 1 ? " reply" : " replies"));

            // Priority Badge Styling
            String priority = issue.getPriority() != null ? issue.getPriority() : TechnicalIssue.PRIORITY_MEDIUM;
            binding.tvPriorityBadge.setText(priority);
            if (TechnicalIssue.PRIORITY_HIGH.equalsIgnoreCase(priority)) {
                binding.tvPriorityBadge.setBackgroundColor(Color.parseColor("#381E1E"));
                binding.tvPriorityBadge.setTextColor(Color.parseColor("#EF4444"));
            } else if (TechnicalIssue.PRIORITY_LOW.equalsIgnoreCase(priority)) {
                binding.tvPriorityBadge.setBackgroundColor(Color.parseColor("#1E293B"));
                binding.tvPriorityBadge.setTextColor(Color.parseColor("#38BDF8"));
            } else {
                binding.tvPriorityBadge.setBackgroundColor(Color.parseColor("#382B14"));
                binding.tvPriorityBadge.setTextColor(Color.parseColor("#F59E0B"));
            }

            // Status Badge Styling
            String status = issue.getStatus() != null ? issue.getStatus() : TechnicalIssue.STATUS_OPEN;
            binding.tvStatusBadge.setText(status);
            if (TechnicalIssue.STATUS_RESOLVED.equalsIgnoreCase(status) || TechnicalIssue.STATUS_CLOSED.equalsIgnoreCase(status)) {
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#123324"));
                binding.tvStatusBadge.setTextColor(Color.parseColor("#10B981"));
            } else if (TechnicalIssue.STATUS_IN_REVIEW.equalsIgnoreCase(status)) {
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#1E293B"));
                binding.tvStatusBadge.setTextColor(Color.parseColor("#818CF8"));
            } else if (TechnicalIssue.STATUS_ASSIGNED.equalsIgnoreCase(status)) {
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#382B14"));
                binding.tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
            } else {
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#222631"));
                binding.tvStatusBadge.setTextColor(Color.parseColor("#94A3B8"));
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onIssueClick(issue);
                }
            });
        }
    }
}
