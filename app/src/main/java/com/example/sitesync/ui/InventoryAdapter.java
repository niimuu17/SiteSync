package com.example.sitesync.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.InventoryItemEntity;
import com.example.sitesync.databinding.ItemInventoryCardBinding;

import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    public interface OnInventoryActionListener {
        void onStockIn(InventoryItemEntity item);
        void onStockOut(InventoryItemEntity item);
    }

    private final Context context;
    private final List<InventoryItemEntity> items = new ArrayList<>();
    private final OnInventoryActionListener listener;

    public InventoryAdapter(Context context, OnInventoryActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<InventoryItemEntity> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInventoryCardBinding binding = ItemInventoryCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new InventoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class InventoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemInventoryCardBinding binding;

        InventoryViewHolder(ItemInventoryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(InventoryItemEntity item) {
            binding.tvMaterialName.setText(item.getName());

            String currentFormatted = formatNumber(item.getCurrentStock());
            String minFormatted = formatNumber(item.getMinThreshold());
            binding.tvStockRatio.setText("Stock: " + currentFormatted + " / " + minFormatted + " " + item.getUnit() + " min");

            int percent = item.getStockPercentage();
            binding.tvStockPercent.setText(percent + "%");
            binding.pbStockLevel.setProgress(Math.min(100, Math.max(0, percent)));

            if (percent < 30) {
                binding.tvStockStatusBadge.setText("CRITICAL");
                binding.tvStockStatusBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                binding.tvStockStatusBadge.setTextColor(Color.parseColor("#991B1B"));
                binding.pbStockLevel.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_red)));
            } else if (percent < 70) {
                binding.tvStockStatusBadge.setText("LOW STOCK");
                binding.tvStockStatusBadge.setBackgroundResource(R.drawable.bg_badge_medium);
                binding.tvStockStatusBadge.setTextColor(Color.parseColor("#92400E"));
                binding.pbStockLevel.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_amber)));
            } else {
                binding.tvStockStatusBadge.setText("OPTIMAL");
                binding.tvStockStatusBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                binding.tvStockStatusBadge.setTextColor(Color.parseColor("#065F46"));
                binding.pbStockLevel.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_green)));
            }

            binding.btnStockIn.setOnClickListener(v -> {
                if (listener != null) listener.onStockIn(item);
            });

            binding.btnStockOut.setOnClickListener(v -> {
                if (listener != null) listener.onStockOut(item);
            });
        }

        private String formatNumber(double val) {
            if (val == (long) val) {
                return String.format(java.util.Locale.US, "%d", (long) val);
            } else {
                return String.format(java.util.Locale.US, "%.1f", val);
            }
        }
    }
}
