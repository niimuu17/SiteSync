package com.example.sitesync.ui.challan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.network.ConstructionMarketPriceClient;
import com.example.sitesync.data.network.MaterialImageResolver;
import com.example.sitesync.hardware.ChallanOcrVisionClient;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChallanReviewAdapter extends RecyclerView.Adapter<ChallanReviewAdapter.ViewHolder> {

    public interface OnItemRemovedListener {
        void onItemRemoved(int position);
    }

    private final List<ChallanOcrVisionClient.ChallanItem> items = new ArrayList<>();
    private final OnItemRemovedListener removeListener;
    private final NumberFormat bdtFormat;

    public ChallanReviewAdapter(List<ChallanOcrVisionClient.ChallanItem> initialItems,
                                OnItemRemovedListener removeListener) {
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
        this.removeListener = removeListener;
        this.bdtFormat = NumberFormat.getNumberInstance(Locale.US);
    }

    public void updateItems(List<ChallanOcrVisionClient.ChallanItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<ChallanOcrVisionClient.ChallanItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challan_review_material, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChallanOcrVisionClient.ChallanItem item = items.get(position);

        holder.tvMaterialName.setText(item.materialName);
        holder.tvCategoryBadge.setText(item.category != null ? item.category : "GENERAL");

        MaterialImageResolver.loadProductIcon(holder.ivReviewProductIcon,
                item.materialName, item.category, item.imageUrl);

        String qtyText = String.format(Locale.US, "%.1f %s", item.quantity, item.unit);
        holder.tvQuantityAndUnit.setText(qtyText);

        String rateText = String.format(Locale.US, "$ %s / %s",
                bdtFormat.format(Math.round(item.unitPrice)), item.unit);
        holder.tvBilledRate.setText(rateText);

        String subtotalText = String.format(Locale.US, "$ %s",
                bdtFormat.format(Math.round(item.totalPrice)));
        holder.tvItemSubtotal.setText(subtotalText);

        // Bind Market Benchmark & Status
        if (item.priceEvaluation != null) {
            String benchText = String.format(Locale.US, "Market Benchmark: $ %s",
                    bdtFormat.format(Math.round(item.priceEvaluation.benchmarkPrice)));
            holder.tvMarketBenchmark.setText(benchText);

            holder.tvVarianceBadge.setVisibility(View.VISIBLE);
            holder.tvVarianceBadge.setText(item.priceEvaluation.statusSummary);

            if (item.priceEvaluation.status == ConstructionMarketPriceClient.PriceStatus.OVER_BILLED) {
                holder.tvVarianceBadge.setBackgroundResource(R.drawable.bg_badge_coral);
                holder.tvVarianceBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.alert_coral_text));
            } else if (item.priceEvaluation.status == ConstructionMarketPriceClient.PriceStatus.DISCOUNTED_BULK) {
                holder.tvVarianceBadge.setBackgroundResource(R.drawable.bg_badge_green);
                holder.tvVarianceBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_green_text));
            } else {
                holder.tvVarianceBadge.setBackgroundResource(R.drawable.bg_badge_green);
                holder.tvVarianceBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_green_text));
            }
        } else {
            holder.tvMarketBenchmark.setText("Analyzing Live Benchmark...");
            holder.tvVarianceBadge.setVisibility(View.GONE);
        }

        holder.btnRemoveItem.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && removeListener != null) {
                items.remove(pos);
                notifyItemRemoved(pos);
                removeListener.onItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivReviewProductIcon;
        final TextView tvMaterialName;
        final TextView tvCategoryBadge;
        final TextView tvQuantityAndUnit;
        final TextView tvBilledRate;
        final TextView tvItemSubtotal;
        final TextView tvMarketBenchmark;
        final TextView tvVarianceBadge;
        final ImageView btnRemoveItem;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReviewProductIcon = itemView.findViewById(R.id.ivReviewProductIcon);
            tvMaterialName = itemView.findViewById(R.id.tvMaterialName);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvQuantityAndUnit = itemView.findViewById(R.id.tvQuantityAndUnit);
            tvBilledRate = itemView.findViewById(R.id.tvBilledRate);
            tvItemSubtotal = itemView.findViewById(R.id.tvItemSubtotal);
            tvMarketBenchmark = itemView.findViewById(R.id.tvMarketBenchmark);
            tvVarianceBadge = itemView.findViewById(R.id.tvVarianceBadge);
            btnRemoveItem = itemView.findViewById(R.id.btnRemoveItem);
        }
    }
}
