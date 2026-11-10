package com.example.sitesync.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.WorkerJoinRequestEntity;
import com.example.sitesync.databinding.ItemWorkerRequestCardBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.ArrayList;
import java.util.List;

public class WorkerRequestsAdapter extends RecyclerView.Adapter<WorkerRequestsAdapter.ViewHolder> {

    public interface OnWorkerRequestActionListener {
        void onApprove(WorkerJoinRequestEntity request);
        void onReject(WorkerJoinRequestEntity request);
    }

    private final Context context;
    private final List<WorkerJoinRequestEntity> items = new ArrayList<>();
    private final OnWorkerRequestActionListener listener;

    public WorkerRequestsAdapter(Context context, OnWorkerRequestActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<WorkerJoinRequestEntity> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkerRequestCardBinding binding = ItemWorkerRequestCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkerJoinRequestEntity item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemWorkerRequestCardBinding binding;

        ViewHolder(ItemWorkerRequestCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WorkerJoinRequestEntity item) {
            binding.tvWorkerName.setText(item.getWorkerName() != null ? item.getWorkerName() : "Unnamed Worker");
            binding.tvWorkerTrade.setText(item.getWorkerTrade() != null ? item.getWorkerTrade() : "General Craft");
            binding.tvWorkerPhone.setText(item.getWorkerPhone() != null ? item.getWorkerPhone() : "--");
            binding.tvProjectCodeTag.setText("Site: " + item.getProjectCode());

            if (item.getRequestedAt() > 0) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        item.getRequestedAt(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                );
                binding.tvRequestTime.setText("Applied " + relative);
            } else {
                binding.tvRequestTime.setText("Applied today");
            }

            String status = item.getStatus() != null ? item.getStatus() : "PENDING";
            if ("PENDING".equalsIgnoreCase(status)) {
                binding.layoutActionButtons.setVisibility(View.VISIBLE);
                binding.tvStatusBadge.setVisibility(View.GONE);

                binding.btnApproveWorker.setOnClickListener(v -> {
                    HapticFeedbackUtil.confirm(context);
                    if (listener != null) {
                        listener.onApprove(item);
                    }
                });

                binding.btnRejectWorker.setOnClickListener(v -> {
                    HapticFeedbackUtil.lightClick(context, v);
                    if (listener != null) {
                        listener.onReject(item);
                    }
                });
            } else {
                binding.layoutActionButtons.setVisibility(View.GONE);
                binding.tvStatusBadge.setVisibility(View.VISIBLE);
                binding.tvStatusBadge.setText(status.toUpperCase());
                if ("APPROVED".equalsIgnoreCase(status)) {
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#10B981"));
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
                } else {
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"));
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_coral);
                }
            }
        }
    }
}