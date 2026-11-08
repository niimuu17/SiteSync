package com.example.sitesync.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemDailySiteReportBinding;
import com.example.sitesync.models.DailySiteReport;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for rendering Daily Site Reports in the engineer hub.
 * Supports viewing details and quick PDF generation & sharing with Project Managers.
 */
public class DailySiteReportAdapter extends RecyclerView.Adapter<DailySiteReportAdapter.ReportViewHolder> {

    public interface OnReportActionListener {
        void onReportClick(DailySiteReport report);
        void onSharePdfClick(DailySiteReport report);
    }

    private final List<DailySiteReport> reportList = new ArrayList<>();
    private final OnReportActionListener listener;

    public DailySiteReportAdapter(OnReportActionListener listener) {
        this.listener = listener;
    }

    public void setReports(List<DailySiteReport> list) {
        reportList.clear();
        if (list != null) {
            reportList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDailySiteReportBinding binding = ItemDailySiteReportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(reportList.get(position));
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private final ItemDailySiteReportBinding binding;

        public ReportViewHolder(@NonNull ItemDailySiteReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DailySiteReport report) {
            binding.tvReportId.setText(report.getId());
            binding.tvReportDate.setText(report.getReportDate());
            binding.tvReportProjectName.setText(report.getProjectName());
            binding.tvReportWorkPreview.setText(report.getCompletedWork());
            binding.tvReportWeather.setText(report.getWeather());
            binding.tvReportManpowerSummary.setText(report.getManpower());
            binding.tvReportAuthor.setText("By: " + report.getPreparedBy());

            // Status Badge Styling
            if (report.isSubmitted()) {
                binding.tvReportStatusBadge.setText("Submitted");
                binding.tvReportStatusBadge.setBackgroundColor(Color.parseColor("#123324"));
                binding.tvReportStatusBadge.setTextColor(Color.parseColor("#10B981"));
            } else {
                binding.tvReportStatusBadge.setText("Draft");
                binding.tvReportStatusBadge.setBackgroundColor(Color.parseColor("#382B14"));
                binding.tvReportStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
            }

            // Quick Share PDF
            binding.btnQuickSharePdf.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSharePdfClick(report);
                }
            });

            // Card Click
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(report);
                }
            });
        }
    }
}
