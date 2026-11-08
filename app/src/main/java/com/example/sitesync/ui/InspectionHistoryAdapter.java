package com.example.sitesync.ui;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemInspectionRecordBinding;
import com.example.sitesync.models.InspectionRecord;
import com.example.sitesync.models.SiteInspection;

import java.util.ArrayList;
import java.util.List;

public class InspectionHistoryAdapter extends RecyclerView.Adapter<InspectionHistoryAdapter.RecordViewHolder> {

    private final List<InspectionRecord> recordList = new ArrayList<>();

    public void setRecords(List<InspectionRecord> list) {
        recordList.clear();
        if (list != null) {
            recordList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInspectionRecordBinding binding = ItemInspectionRecordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new RecordViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        holder.bind(recordList.get(position));
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        private final ItemInspectionRecordBinding binding;

        public RecordViewHolder(ItemInspectionRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(InspectionRecord record) {
            if (record.getAttemptNumber() == 1) {
                binding.tvRecordAttemptTitle.setText("Attempt #1 • Initial Site Inspection");
            } else {
                binding.tvRecordAttemptTitle.setText("Attempt #" + record.getAttemptNumber() + " • Formal Re-Inspection");
            }

            binding.tvRecordStatusBadge.setText(record.getStatus());
            binding.tvRecordDateInspector.setText(record.getDate() + " • " + record.getInspectorName());
            binding.tvRecordChecklistSummary.setText(record.getChecklistSummary());

            if (record.getObservations() != null && !record.getObservations().isEmpty()) {
                binding.tvRecordObservations.setText("Observations: " + record.getObservations());
                binding.tvRecordObservations.setVisibility(View.VISIBLE);
            } else {
                binding.tvRecordObservations.setVisibility(View.GONE);
            }

            if (record.getRemarks() != null && !record.getRemarks().isEmpty()) {
                binding.tvRecordRemarks.setText("Remarks: " + record.getRemarks());
                binding.tvRecordRemarks.setVisibility(View.VISIBLE);
            } else {
                binding.tvRecordRemarks.setVisibility(View.GONE);
            }

            // Status Badge Coloring
            if (SiteInspection.STATUS_PASSED.equalsIgnoreCase(record.getStatus())) {
                binding.tvRecordStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
                binding.tvRecordStatusBadge.setTextColor(Color.parseColor("#15803D"));
                binding.viewTimelineDot.setBackgroundColor(Color.parseColor("#16A34A"));
            } else if (SiteInspection.STATUS_FAILED.equalsIgnoreCase(record.getStatus())) {
                binding.tvRecordStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"));
                binding.tvRecordStatusBadge.setTextColor(Color.parseColor("#B91C1C"));
                binding.viewTimelineDot.setBackgroundColor(Color.parseColor("#DC2626"));
            } else {
                binding.tvRecordStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
                binding.tvRecordStatusBadge.setTextColor(Color.parseColor("#B45309"));
                binding.viewTimelineDot.setBackgroundColor(Color.parseColor("#F59E0B"));
            }

            // Attached Photo Preview
            if (record.getPhotoUri() != null && !record.getPhotoUri().isEmpty()) {
                try {
                    binding.ivRecordPhoto.setImageURI(Uri.parse(record.getPhotoUri()));
                    binding.ivRecordPhoto.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    binding.ivRecordPhoto.setVisibility(View.GONE);
                }
            } else {
                binding.ivRecordPhoto.setVisibility(View.GONE);
            }
        }
    }
}
