package com.example.sitesync.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.databinding.ItemChatBriefingCardBinding;
import com.example.sitesync.hardware.ChatIntelligenceClient;

import java.util.ArrayList;
import java.util.List;

public class ChatBriefingReportAdapter extends RecyclerView.Adapter<ChatBriefingReportAdapter.ViewHolder> {

    private final Context context;
    private final List<ChatIntelligenceClient.ImportantBriefingNotice> notices = new ArrayList<>();

    public ChatBriefingReportAdapter(Context context) {
        this.context = context;
    }

    public void setNotices(List<ChatIntelligenceClient.ImportantBriefingNotice> newNotices) {
        this.notices.clear();
        if (newNotices != null) {
            this.notices.addAll(newNotices);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBriefingCardBinding binding = ItemChatBriefingCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notices.get(position));
    }

    @Override
    public int getItemCount() {
        return notices.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatBriefingCardBinding binding;

        ViewHolder(ItemChatBriefingCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatIntelligenceClient.ImportantBriefingNotice notice) {
            binding.tvBriefingTitle.setText(notice.title);
            binding.tvBriefingDetails.setText(notice.details);
            binding.tvBriefingReporter.setText("By " + (notice.reportedBy != null ? notice.reportedBy : "Crew"));

            String category = notice.category != null ? notice.category.replace("_", " ") : "SITE NOTICE";
            binding.tvBriefingCategory.setText(category);

            String severity = notice.severity != null ? notice.severity : "INFO";
            binding.tvBriefingSeverityBadge.setText(severity.toUpperCase());

            if ("CRITICAL".equalsIgnoreCase(severity)) {
                binding.tvBriefingSeverityBadge.setTextColor(Color.parseColor("#B9382C"));
                binding.tvBriefingSeverityBadge.setBackgroundResource(R.drawable.bg_pill_alert_coral);
            } else if ("WARNING".equalsIgnoreCase(severity)) {
                binding.tvBriefingSeverityBadge.setTextColor(Color.parseColor("#D97706"));
                binding.tvBriefingSeverityBadge.setBackgroundResource(R.drawable.bg_badge_pill_teal);
            } else {
                binding.tvBriefingSeverityBadge.setTextColor(Color.parseColor("#10B981"));
                binding.tvBriefingSeverityBadge.setBackgroundResource(R.drawable.bg_badge_green);
            }

            if (notice.suggestedAction != null && !notice.suggestedAction.isEmpty()) {
                binding.tvSuggestedAction.setText("Action: " + notice.suggestedAction);
            } else {
                binding.tvSuggestedAction.setText("Action: Verify and document in daily site log.");
            }
        }
    }
}
