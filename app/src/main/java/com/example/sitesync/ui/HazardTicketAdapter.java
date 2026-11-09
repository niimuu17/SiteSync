package com.example.sitesync.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.HazardTicketEntity;
import com.example.sitesync.databinding.ItemHazardTicketBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Jobsite Hazard Tickets.
 * Renders priority badges, location tags, and quick-resolve action buttons.
 */
public class HazardTicketAdapter extends RecyclerView.Adapter<HazardTicketAdapter.HazardViewHolder> {

    public interface OnHazardActionListener {
        void onResolveClick(HazardTicketEntity ticket);
    }

    private final List<HazardTicketEntity> allItems = new ArrayList<>();
    private final List<HazardTicketEntity> displayedItems = new ArrayList<>();
    private final OnHazardActionListener actionListener;
    private boolean showResolvedOnly = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public HazardTicketAdapter(OnHazardActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setData(List<HazardTicketEntity> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        applyFilter();
    }

    public void setFilterMode(boolean showResolved) {
        this.showResolvedOnly = showResolved;
        applyFilter();
    }

    private void applyFilter() {
        displayedItems.clear();
        for (HazardTicketEntity ticket : allItems) {
            boolean isResolved = "RESOLVED".equalsIgnoreCase(ticket.getStatus());
            if (showResolvedOnly && isResolved) {
                displayedItems.add(ticket);
            } else if (!showResolvedOnly && !isResolved) {
                displayedItems.add(ticket);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HazardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHazardTicketBinding binding = ItemHazardTicketBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HazardViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HazardViewHolder holder, int position) {
        holder.bind(displayedItems.get(position));
    }

    @Override
    public int getItemCount() {
        return displayedItems.size();
    }

    class HazardViewHolder extends RecyclerView.ViewHolder {
        private final ItemHazardTicketBinding b;

        public HazardViewHolder(ItemHazardTicketBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        public void bind(HazardTicketEntity item) {
            Context context = itemView.getContext();

            b.tvHazardCategory.setText(item.getCategory() != null ? item.getCategory().toUpperCase(Locale.ROOT) : "GENERAL HAZARD");
            b.tvHazardDescription.setText(item.getDescription());
            b.tvHazardLocation.setText("📍 " + (item.getLocationName() != null ? item.getLocationName() : "Active Site"));
            b.tvHazardTimestamp.setText(dateFormat.format(new Date(item.getReportedAt())));

            // Priority badge
            String priority = item.getPriority() != null ? item.getPriority().toUpperCase(Locale.ROOT) : "MEDIUM";
            b.tvHazardPriorityBadge.setText(priority);
            if ("URGENT".equals(priority)) {
                b.tvHazardPriorityBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                b.tvHazardPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_amber_dark));
            } else if ("MEDIUM".equals(priority)) {
                b.tvHazardPriorityBadge.setBackgroundResource(R.drawable.bg_badge_medium);
                b.tvHazardPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_amber_dark));
            } else {
                b.tvHazardPriorityBadge.setBackgroundResource(R.drawable.bg_badge_low);
                b.tvHazardPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.oxide_blue_primary));
            }

            boolean isResolved = "RESOLVED".equalsIgnoreCase(item.getStatus());
            if (isResolved) {
                b.tvHazardStatus.setText("Resolved ✓");
                b.tvHazardStatus.setTextColor(ContextCompat.getColor(context, R.color.status_green));
                b.btnResolveHazard.setVisibility(View.GONE);
                b.tvResolutionNote.setVisibility(View.VISIBLE);
                b.tvResolutionNote.setText("Resolved: " + (item.getResolutionNotes() != null && !item.getResolutionNotes().isEmpty() ?
                        item.getResolutionNotes() : "Corrective action verified by safety inspector."));
            } else {
                b.tvHazardStatus.setText("Open");
                b.tvHazardStatus.setTextColor(ContextCompat.getColor(context, R.color.text_oxide_secondary));
                b.btnResolveHazard.setVisibility(View.VISIBLE);
                b.tvResolutionNote.setVisibility(View.GONE);

                b.btnResolveHazard.setOnClickListener(v -> {
                    HapticFeedbackUtil.lightClick(context, v);
                    if (actionListener != null) {
                        actionListener.onResolveClick(item);
                    }
                });
            }
        }
    }
}
