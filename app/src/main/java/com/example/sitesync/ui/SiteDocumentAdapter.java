package com.example.sitesync.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemSiteDocumentBinding;
import com.example.sitesync.models.SiteDocument;

import java.util.ArrayList;
import java.util.List;

public class SiteDocumentAdapter extends RecyclerView.Adapter<SiteDocumentAdapter.DocumentViewHolder> {

    public interface OnDocumentActionListener {
        void onViewClicked(SiteDocument document);
        void onDownloadClicked(SiteDocument document);
        void onRevisionsClicked(SiteDocument document);
    }

    private final List<SiteDocument> documentList = new ArrayList<>();
    private final OnDocumentActionListener actionListener;

    public SiteDocumentAdapter(OnDocumentActionListener listener) {
        this.actionListener = listener;
    }

    public void setDocuments(List<SiteDocument> list) {
        documentList.clear();
        if (list != null) {
            documentList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSiteDocumentBinding binding = ItemSiteDocumentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new DocumentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        holder.bind(documentList.get(position));
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final ItemSiteDocumentBinding binding;

        public DocumentViewHolder(ItemSiteDocumentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SiteDocument document) {
            binding.tvDocName.setText(document.getName());
            binding.tvDocProjectAndDate.setText(document.getProjectName() + " • Uploaded: " + document.getUploadDate() + " by " + document.getUploadedBy());
            binding.tvDocFileSize.setText(document.getFileSize());
            binding.tvDocRevisionBadge.setText(document.getCurrentRevision());

            // Category Badge & Color
            applyCategoryStyle(document.getCategory());

            // Approval Status Badge
            if (document.isApproved()) {
                binding.tvDocApprovalBadge.setText("LATEST APPROVED");
                binding.tvDocApprovalBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
                binding.tvDocApprovalBadge.setTextColor(Color.parseColor("#15803D"));
            } else {
                binding.tvDocApprovalBadge.setText("UNDER REVIEW");
                binding.tvDocApprovalBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
                binding.tvDocApprovalBadge.setTextColor(Color.parseColor("#B45309"));
            }

            // Click Handlers
            itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onRevisionsClicked(document);
            });
            binding.btnDocView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onViewClicked(document);
            });
            binding.btnDocDownload.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDownloadClicked(document);
            });
            binding.btnDocRevisions.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onRevisionsClicked(document);
            });
        }

        private void applyCategoryStyle(String category) {
            binding.tvDocCategoryBadge.setText(category);

            if (SiteDocument.CATEGORY_ARCHITECTURAL.equalsIgnoreCase(category)) {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#EDE9FE"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#6D28D9"));
            } else if (SiteDocument.CATEGORY_STRUCTURAL.equalsIgnoreCase(category)) {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#DBEAFE"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#1D4ED8"));
            } else if (SiteDocument.CATEGORY_ELECTRICAL.equalsIgnoreCase(category)) {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#B45309"));
            } else if (SiteDocument.CATEGORY_PLUMBING.equalsIgnoreCase(category)) {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#CCFBF1"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#0F766E"));
            } else {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#F1F5F9"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#475569"));
            }
        }
    }
}
