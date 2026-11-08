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
                binding.tvDocApprovalBadge.setBackgroundColor(Color.parseColor("#123324"));
                binding.tvDocApprovalBadge.setTextColor(Color.parseColor("#10B981"));
            } else {
                binding.tvDocApprovalBadge.setText("UNDER REVIEW");
                binding.tvDocApprovalBadge.setBackgroundColor(Color.parseColor("#382B14"));
                binding.tvDocApprovalBadge.setTextColor(Color.parseColor("#F59E0B"));
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
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#231F3D"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#A78BFA"));
            } else if (SiteDocument.CATEGORY_STRUCTURAL.equalsIgnoreCase(category)) {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#1E293B"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#38BDF8"));
            } else if (SiteDocument.CATEGORY_ELECTRICAL.equalsIgnoreCase(category)) {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#382B14"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#F59E0B"));
            } else if (SiteDocument.CATEGORY_PLUMBING.equalsIgnoreCase(category)) {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#132E2E"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#2DD4BF"));
            } else {
                binding.tvDocCategoryBadge.setBackgroundColor(Color.parseColor("#222631"));
                binding.tvDocCategoryBadge.setTextColor(Color.parseColor("#94A3B8"));
            }
        }
    }
}
