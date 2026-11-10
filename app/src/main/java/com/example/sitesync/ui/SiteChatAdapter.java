package com.example.sitesync.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.SiteChatMessageEntity;
import com.example.sitesync.databinding.ItemSiteChatMessageBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SiteChatAdapter extends RecyclerView.Adapter<SiteChatAdapter.ChatViewHolder> {

    private final String currentUserId;
    private final List<SiteChatMessageEntity> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
    private static MediaPlayer activeMediaPlayer;

    public SiteChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public void setMessages(List<SiteChatMessageEntity> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSiteChatMessageBinding binding = ItemSiteChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        SiteChatMessageEntity msg = messages.get(position);
        holder.bind(msg, currentUserId, timeFormat);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemSiteChatMessageBinding binding;

        public ChatViewHolder(ItemSiteChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SiteChatMessageEntity msg, String currentUserId, SimpleDateFormat timeFormat) {
            Context context = itemView.getContext();
            boolean isSelf = currentUserId.equalsIgnoreCase(msg.getSenderId()) ||
                    currentUserId.equalsIgnoreCase(msg.getSenderName());

            binding.tvChatMessageText.setText(msg.getMessageText());
            binding.tvChatMessageTime.setText(timeFormat.format(new Date(msg.getTimestamp())));

            if (msg.isUrgent()) {
                binding.tvChatUrgentBadge.setVisibility(View.VISIBLE);
            } else {
                binding.tvChatUrgentBadge.setVisibility(View.GONE);
            }

            // Image attachment
            if ("IMAGE".equalsIgnoreCase(msg.getMediaType()) && msg.getMediaUrl() != null && !msg.getMediaUrl().isEmpty()) {
                binding.ivChatImageAttachment.setVisibility(View.VISIBLE);
                File imgFile = new File(msg.getMediaUrl());
                if (imgFile.exists()) {
                    Glide.with(context).load(imgFile).into(binding.ivChatImageAttachment);
                } else {
                    Glide.with(context).load(msg.getMediaUrl()).into(binding.ivChatImageAttachment);
                }
            } else {
                binding.ivChatImageAttachment.setVisibility(View.GONE);
            }

            // Audio attachment
            if ("AUDIO".equalsIgnoreCase(msg.getMediaType()) && msg.getMediaUrl() != null && !msg.getMediaUrl().isEmpty()) {
                binding.layoutAudioAttachment.setVisibility(View.VISIBLE);
                binding.layoutAudioAttachment.setOnClickListener(v -> {
                    try {
                        if (activeMediaPlayer != null) {
                            activeMediaPlayer.stop();
                            activeMediaPlayer.release();
                            activeMediaPlayer = null;
                        }
                        activeMediaPlayer = new MediaPlayer();
                        File audioFile = new File(msg.getMediaUrl());
                        if (audioFile.exists()) {
                            activeMediaPlayer.setDataSource(audioFile.getAbsolutePath());
                        } else {
                            activeMediaPlayer.setDataSource(context, Uri.parse(msg.getMediaUrl()));
                        }
                        activeMediaPlayer.prepare();
                        activeMediaPlayer.start();
                        Toast.makeText(context, "Playing audio note...", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Audio playback error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                binding.layoutAudioAttachment.setVisibility(View.GONE);
            }

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) binding.cardMessageBubble.getLayoutParams();

            if (isSelf) {
                // Outbound Message (Self)
                lp.gravity = Gravity.END;
                binding.cardMessageBubble.setLayoutParams(lp);
                binding.cardMessageBubble.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.forest_card_dark)));
                binding.cardMessageBubble.setStrokeColor(ContextCompat.getColor(context, R.color.peach_accent));
                binding.tvChatSenderName.setText("You");
                binding.tvChatSenderName.setTextColor(ContextCompat.getColor(context, R.color.peach_accent));
                binding.tvChatMessageText.setTextColor(ContextCompat.getColor(context, R.color.text_on_dark_primary));
                binding.tvChatMessageTime.setTextColor(ContextCompat.getColor(context, R.color.text_on_dark_secondary));
                binding.tvChatSenderRoleBadge.setText(msg.getSenderRole() != null ? msg.getSenderRole() : "ME");
            } else {
                // Inbound Message (Others)
                lp.gravity = Gravity.START;
                binding.cardMessageBubble.setLayoutParams(lp);
                binding.cardMessageBubble.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.canvas_white)));
                binding.cardMessageBubble.setStrokeColor(ContextCompat.getColor(context, R.color.forest_stroke));
                binding.tvChatSenderName.setText(msg.getSenderName() != null ? msg.getSenderName() : "Site Crew");
                binding.tvChatSenderName.setTextColor(ContextCompat.getColor(context, R.color.forest_surface_deep));
                binding.tvChatMessageText.setTextColor(ContextCompat.getColor(context, R.color.forest_surface_deep));
                binding.tvChatMessageTime.setTextColor(ContextCompat.getColor(context, R.color.forest_muted));
                binding.tvChatSenderRoleBadge.setText(msg.getSenderRole() != null ? msg.getSenderRole() : "WORKER");
            }
        }
    }
}
