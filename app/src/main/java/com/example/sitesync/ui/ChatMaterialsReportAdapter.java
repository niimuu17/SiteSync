package com.example.sitesync.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sitesync.R;
import com.example.sitesync.data.network.MaterialImageResolver;
import com.example.sitesync.data.local.entities.RequiredMaterialItemEntity;
import com.example.sitesync.databinding.ItemChatExtractedMaterialBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatMaterialsReportAdapter extends RecyclerView.Adapter<ChatMaterialsReportAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int totalCount);
    }

    private final Context context;
    private final List<RequiredMaterialItemEntity> items = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private final OnSelectionChangedListener listener;

    public ChatMaterialsReportAdapter(Context context, OnSelectionChangedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<RequiredMaterialItemEntity> newItems) {
        this.items.clear();
        this.selectedIds.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
            for (RequiredMaterialItemEntity item : newItems) {
                selectedIds.add(item.getId());
            }
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedIds.size(), items.size());
        }
    }

    public void toggleSelectAll() {
        if (selectedIds.size() == items.size()) {
            selectedIds.clear();
        } else {
            for (RequiredMaterialItemEntity item : items) {
                selectedIds.add(item.getId());
            }
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedIds.size(), items.size());
        }
    }

    public List<RequiredMaterialItemEntity> getSelectedItems() {
        List<RequiredMaterialItemEntity> result = new ArrayList<>();
        for (RequiredMaterialItemEntity item : items) {
            if (selectedIds.contains(item.getId())) {
                result.add(item);
            }
        }
        return result;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatExtractedMaterialBinding binding = ItemChatExtractedMaterialBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatExtractedMaterialBinding binding;

        ViewHolder(ItemChatExtractedMaterialBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RequiredMaterialItemEntity item) {
            binding.tvMaterialName.setText(item.getItemName());
            binding.tvMaterialCategoryBadge.setText(item.getCategory() != null ? item.getCategory() : "GENERAL");
            binding.tvMaterialQuantity.setText(String.format(Locale.US, "%.1f %s", item.getQuantity(), item.getUnit()));
            binding.tvRequestedBy.setText("• " + (item.getRequestedBy() != null ? item.getRequestedBy() : "Crew"));

            if (item.getRawSentence() != null && !item.getRawSentence().isEmpty()) {
                binding.tvRawSentence.setVisibility(android.view.View.VISIBLE);
                binding.tvRawSentence.setText("\"" + item.getRawSentence().trim() + "\"");
            } else {
                binding.tvRawSentence.setVisibility(android.view.View.GONE);
            }

            // Thumbnail Image: Enterprise Multi-Tier Product Icon Resolver
            MaterialImageResolver.loadProductIcon(binding.ivMaterialThumbnail, item.getItemName(), item.getCategory(), item.getImageUrl());

            binding.cbSelectMaterial.setOnCheckedChangeListener(null);
            binding.cbSelectMaterial.setChecked(selectedIds.contains(item.getId()));
            binding.cbSelectMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedIds.add(item.getId());
                } else {
                    selectedIds.remove(item.getId());
                }
                if (listener != null) {
                    listener.onSelectionChanged(selectedIds.size(), items.size());
                }
            });

            itemView.setOnClickListener(v -> binding.cbSelectMaterial.toggle());
        }
    }
}
