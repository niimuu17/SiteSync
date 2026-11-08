package com.example.sitesync.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.databinding.ItemInspectionChecklistBinding;
import com.example.sitesync.models.InspectionChecklistItem;

import java.util.ArrayList;
import java.util.List;

public class InspectionChecklistAdapter extends RecyclerView.Adapter<InspectionChecklistAdapter.ChecklistViewHolder> {

    private final List<InspectionChecklistItem> checklist = new ArrayList<>();
    private final boolean isReadOnly;

    public InspectionChecklistAdapter(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public void setChecklist(List<InspectionChecklistItem> items) {
        checklist.clear();
        if (items != null) {
            checklist.addAll(items);
        }
        notifyDataSetChanged();
    }

    public List<InspectionChecklistItem> getChecklist() {
        return checklist;
    }

    @NonNull
    @Override
    public ChecklistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInspectionChecklistBinding binding = ItemInspectionChecklistBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ChecklistViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChecklistViewHolder holder, int position) {
        holder.bind(checklist.get(position));
    }

    @Override
    public int getItemCount() {
        return checklist.size();
    }

    class ChecklistViewHolder extends RecyclerView.ViewHolder {
        private final ItemInspectionChecklistBinding binding;

        public ChecklistViewHolder(ItemInspectionChecklistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(InspectionChecklistItem item) {
            binding.tvChecklistTitle.setText(item.getTitle());

            if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                binding.tvChecklistNotes.setText(item.getNotes());
                binding.tvChecklistNotes.setVisibility(View.VISIBLE);
            } else {
                binding.tvChecklistNotes.setVisibility(View.GONE);
            }

            if (isReadOnly) {
                binding.cbChecklistItem.setVisibility(View.GONE);
                binding.ivChecklistStatusIcon.setVisibility(View.VISIBLE);
                if (item.isPassed()) {
                    binding.ivChecklistStatusIcon.setImageResource(R.drawable.ic_check_circle);
                } else {
                    binding.ivChecklistStatusIcon.setImageResource(R.drawable.ic_cancel_circle);
                }
            } else {
                binding.ivChecklistStatusIcon.setVisibility(View.GONE);
                binding.cbChecklistItem.setVisibility(View.VISIBLE);
                binding.cbChecklistItem.setChecked(item.isPassed());
                binding.cbChecklistItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.setPassed(isChecked);
                });
            }
        }
    }
}
