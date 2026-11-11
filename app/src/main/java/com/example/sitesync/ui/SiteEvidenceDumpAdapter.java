package com.example.sitesync.ui;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sitesync.data.local.entities.SiteEvidencePhotoEntity;
import com.example.sitesync.databinding.ItemSiteEvidenceDumpCardBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SiteEvidenceDumpAdapter extends RecyclerView.Adapter<SiteEvidenceDumpAdapter.DumpViewHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClick(SiteEvidencePhotoEntity photo);
    }

    private final Context context;
    private final OnPhotoClickListener listener;
    private final List<SiteEvidencePhotoEntity> photos = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM • hh:mm a", Locale.US);

    public SiteEvidenceDumpAdapter(Context context, OnPhotoClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setPhotos(List<SiteEvidencePhotoEntity> newPhotos) {
        photos.clear();
        if (newPhotos != null) {
            photos.addAll(newPhotos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DumpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSiteEvidenceDumpCardBinding binding = ItemSiteEvidenceDumpCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new DumpViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DumpViewHolder holder, int position) {
        SiteEvidencePhotoEntity photo = photos.get(position);
        holder.bind(photo, context, dateFormat, listener);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class DumpViewHolder extends RecyclerView.ViewHolder {
        private final ItemSiteEvidenceDumpCardBinding binding;

        public DumpViewHolder(ItemSiteEvidenceDumpCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SiteEvidencePhotoEntity photo, Context context, SimpleDateFormat dateFormat, OnPhotoClickListener listener) {
            binding.tvDumpWorkerName.setText(photo.getCapturedBy() != null ? photo.getCapturedBy() : "Crew Member");
            binding.tvDumpTimestamp.setText(dateFormat.format(new Date(photo.getTimestamp())));
            binding.tvDumpGpsBadge.setText(photo.getLocationLabel() != null ? photo.getLocationLabel() : "GPS Stamped");

            if (photo.getLocalFilePath() != null && !photo.getLocalFilePath().isEmpty()) {
                File file = new File(photo.getLocalFilePath());
                if (file.exists()) {
                    Glide.with(context).load(file).into(binding.ivDumpThumbnail);
                } else if (photo.getCloudStorageUrl() != null) {
                    Glide.with(context).load(photo.getCloudStorageUrl()).into(binding.ivDumpThumbnail);
                }
            } else if (photo.getCloudStorageUrl() != null) {
                Glide.with(context).load(photo.getCloudStorageUrl()).into(binding.ivDumpThumbnail);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoClick(photo);
                }
            });
        }
    }
}