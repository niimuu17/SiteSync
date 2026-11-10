package com.example.sitesync.ui.challan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.InventoryItem;
import com.example.sitesync.data.network.MaterialImageResolver;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventoryStockAdapter extends RecyclerView.Adapter<InventoryStockAdapter.ViewHolder> {

    public interface OnStockItemActionListener {
        void onStockOut(InventoryItem item);
    }

    private final List<InventoryItem> items = new ArrayList<>();
    private final OnStockItemActionListener actionListener;
    private final NumberFormat bdtFormat = NumberFormat.getNumberInstance(Locale.US);

    public InventoryStockAdapter(OnStockItemActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setItems(List<InventoryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_stock_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = items.get(position);

        holder.tvStockItemName.setText(item.getItemName());
        holder.tvStockCategory.setText(item.getCategory() != null ? item.getCategory() : "GENERAL");

        MaterialImageResolver.loadProductIcon(holder.ivStockProductIcon,
                item.getItemName(), item.getCategory(), item.getImageUrl());

        holder.tvAvailableBalance.setText(String.format(Locale.US, "%.1f %s",
                item.getCurrentBalance(), item.getUnit()));

        double val = item.getValuation();
        holder.tvStockValuation.setText(String.format(Locale.US, "$ %s", bdtFormat.format(Math.round(val))));

        // Low stock warning
        if (item.isLowStock()) {
            holder.tvLowStockTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvLowStockTag.setVisibility(View.GONE);
        }

        // Progress level: Available vs Total Stock In
        int progress = 0;
        if (item.getTotalStockIn() > 0) {
            progress = (int) Math.round((item.getCurrentBalance() / item.getTotalStockIn()) * 100);
            if (progress < 0) progress = 0;
            if (progress > 100) progress = 100;
        }
        holder.pbStockLevel.setProgress(progress);

        holder.tvInAndOutRatio.setText(String.format(Locale.US,
                "In: %.1f | Out: %.1f %s", item.getTotalStockIn(), item.getTotalStockOut(), item.getUnit()));

        holder.btnQuickStockOut.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onStockOut(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivStockProductIcon;
        final TextView tvStockItemName;
        final TextView tvStockCategory;
        final TextView tvLowStockTag;
        final TextView tvAvailableBalance;
        final TextView tvStockValuation;
        final ProgressBar pbStockLevel;
        final TextView tvInAndOutRatio;
        final MaterialButton btnQuickStockOut;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStockProductIcon = itemView.findViewById(R.id.ivStockProductIcon);
            tvStockItemName = itemView.findViewById(R.id.tvStockItemName);
            tvStockCategory = itemView.findViewById(R.id.tvStockCategory);
            tvLowStockTag = itemView.findViewById(R.id.tvLowStockTag);
            tvAvailableBalance = itemView.findViewById(R.id.tvAvailableBalance);
            tvStockValuation = itemView.findViewById(R.id.tvStockValuation);
            pbStockLevel = itemView.findViewById(R.id.pbStockLevel);
            tvInAndOutRatio = itemView.findViewById(R.id.tvInAndOutRatio);
            btnQuickStockOut = itemView.findViewById(R.id.btnQuickStockOut);
        }
    }
}
