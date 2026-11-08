package com.example.sitesync.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemSiteInspectionBinding;
import com.example.sitesync.models.SiteInspection;

import java.util.ArrayList;
import java.util.List;

public class SiteInspectionAdapter extends RecyclerView.Adapter<SiteInspectionAdapter.InspectionViewHolder> {

    public interface OnInspectionActionListener {
        void onDetailsClicked(SiteInspection inspection);
        void onReInspectClicked(SiteInspection inspection);
    }

    private final List<SiteInspection> inspectionList = new ArrayList<>();
    private final OnInspectionActionListener actionListener;

    public SiteInspectionAdapter(OnInspectionActionListener listener) {
        this.actionListener = listener;
    }

    public void setInspections(List<SiteInspection> list) {
        inspectionList.clear();
        if (list != null) {
            inspectionList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InspectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSiteInspectionBinding binding = ItemSiteInspectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new InspectionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InspectionViewHolder holder, int position) {
        holder.bind(inspectionList.get(position));
    }

    @Override
    public int getItemCount() {
        return inspectionList.size();
    }

    class InspectionViewHolder extends RecyclerView.ViewHolder {
        private final ItemSiteInspectionBinding binding;

        public InspectionViewHolder(ItemSiteInspectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SiteInspection inspection) {
            binding.tvInspCardProject.setText(inspection.getProjectName());
            binding.tvInspCardType.setText(inspection.getInspectionType());
            binding.tvInspCardLocation.setText(inspection.getLocation());

            int passed = inspection.getPassedChecklistCount();
            int total = inspection.getTotalChecklistCount();
            binding.tvInspCardChecklistSummary.setText("Checklist: " + passed + "/" + total + " Verified");

            binding.tvInspCardDateInspector.setText(inspection.getDate() + " • " + inspection.getInspectorName());

            // Status Styling
            applyStatusStyle(inspection.getStatus());

            // Re-inspection Needed indicator & button
            boolean isFailed = SiteInspection.STATUS_FAILED.equalsIgnoreCase(inspection.getStatus());
            binding.tvInspCardReinspectionBadge.setVisibility(isFailed ? View.VISIBLE : View.GONE);
            binding.btnInspCardReInspect.setVisibility(isFailed ? View.VISIBLE : View.GONE);

            // Listeners
            itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDetailsClicked(inspection);
            });
            binding.btnInspCardDetails.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDetailsClicked(inspection);
            });
            binding.btnInspCardReInspect.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onReInspectClicked(inspection);
            });
        }

        private void applyStatusStyle(String status) {
            binding.tvInspCardStatusBadge.setText(status);

            if (SiteInspection.STATUS_PASSED.equalsIgnoreCase(status)) {
                binding.tvInspCardStatusBadge.setBackgroundColor(Color.parseColor("#123324"));
                binding.tvInspCardStatusBadge.setTextColor(Color.parseColor("#10B981"));
            } else if (SiteInspection.STATUS_FAILED.equalsIgnoreCase(status)) {
                binding.tvInspCardStatusBadge.setBackgroundColor(Color.parseColor("#381E1E"));
                binding.tvInspCardStatusBadge.setTextColor(Color.parseColor("#EF4444"));
            } else {
                binding.tvInspCardStatusBadge.setBackgroundColor(Color.parseColor("#382B14"));
                binding.tvInspCardStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
            }
        }
    }
}
