package com.example.sitesync.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemDocumentRevisionBinding;
import com.example.sitesync.models.DocumentRevision;

import java.util.ArrayList;
import java.util.List;

public class DocumentRevisionAdapter extends RecyclerView.Adapter<DocumentRevisionAdapter.RevisionViewHolder> {

    public interface OnRevisionActionListener {
        void onViewRevision(DocumentRevision revision);
        void onDownloadRevision(DocumentRevision revision);
    }

    private final List<DocumentRevision> revisionList = new ArrayList<>();
    private final OnRevisionActionListener actionListener;

    public DocumentRevisionAdapter(OnRevisionActionListener listener) {
        this.actionListener = listener;
    }

    public void setRevisions(List<DocumentRevision> list) {
        revisionList.clear();
        if (list != null) {
            revisionList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RevisionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDocumentRevisionBinding binding = ItemDocumentRevisionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new RevisionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RevisionViewHolder holder, int position) {
        holder.bind(revisionList.get(position));
    }

    @Override
    public int getItemCount() {
        return revisionList.size();
    }

    class RevisionViewHolder extends RecyclerView.ViewHolder {
        private final ItemDocumentRevisionBinding binding;

        public RevisionViewHolder(ItemDocumentRevisionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DocumentRevision revision) {
            binding.tvRevisionNumberTitle.setText("Revision: " + revision.getRevisionNumber());
            binding.tvRevisionDateAuthor.setText(revision.getUploadDate() + " • " + revision.getUploadedBy());
            binding.tvRevisionChangeNotes.setText("Notes: " + revision.getChangeNotes());
            binding.tvRevisionFileSize.setText(revision.getFileSize());

            if (revision.isApproved()) {
                binding.tvRevisionStatusBadge.setText("LATEST APPROVED");
                binding.tvRevisionStatusBadge.setBackgroundColor(Color.parseColor("#123324"));
                binding.tvRevisionStatusBadge.setTextColor(Color.parseColor("#10B981"));
                binding.viewRevTimelineDot.setBackgroundColor(Color.parseColor("#10B981"));
            } else {
                binding.tvRevisionStatusBadge.setText("SUPERSEDED");
                binding.tvRevisionStatusBadge.setBackgroundColor(Color.parseColor("#222631"));
                binding.tvRevisionStatusBadge.setTextColor(Color.parseColor("#94A3B8"));
                binding.viewRevTimelineDot.setBackgroundColor(Color.parseColor("#64748B"));
            }

            binding.btnRevItemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onViewRevision(revision);
            });
            binding.btnRevItemDownload.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDownloadRevision(revision);
            });
        }
    }
}
