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

    public interface OnMessageImageClickListener {
        void onImageClick(SiteChatMessageEntity message);
    }

    private final String currentUserId;
    private final List<SiteChatMessageEntity> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
    private static MediaPlayer activeMediaPlayer;
    private OnMessageImageClickListener imageClickListener;

    public SiteChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public void setOnMessageImageClickListener(OnMessageImageClickListener listener) {
        this.imageClickListener = listener;
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
        holder.bind(msg, currentUserId, timeFormat, imageClickListener);
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

        public void bind(SiteChatMessageEntity msg, String currentUserId, SimpleDateFormat timeFormat, OnMessageImageClickListener imageClickListener) {
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
                binding.ivChatImageAttachment.setOnClickListener(v -> {
                    if (imageClickListener != null) {
                        imageClickListener.onImageClick(msg);
                    }
                });
            } else {
                binding.ivChatImageAttachment.setVisibility(View.GONE);
                binding.ivChatImageAttachment.setOnClickListener(null);
            }

            // Audio attachment
            if ("AUDIO".equalsIgnoreCase(msg.getMediaType()) && msg.getMediaUrl() != null && !msg.getMediaUrl().isEmpty()) {
                binding.layoutAudioAttachment.setVisibility(View.VISIBLE);
                binding.tvAudioDuration.setText("Voice Note • Tap to Play");
                binding.ivAudioPlayPause.setImageResource(R.drawable.ic_play_arrow);

                binding.layoutAudioAttachment.setOnClickListener(v -> {
                    try {
                        if (activeMediaPlayer != null && activeMediaPlayer.isPlaying()) {
                            activeMediaPlayer.stop();
                            activeMediaPlayer.release();
                            activeMediaPlayer = null;
                            binding.ivAudioPlayPause.setImageResource(R.drawable.ic_play_arrow);
                            binding.tvAudioDuration.setText("Voice Note • Tap to Play");
                            return;
                        }

                        if (activeMediaPlayer != null) {
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

                        binding.ivAudioPlayPause.setImageResource(R.drawable.ic_pause);
                        binding.tvAudioDuration.setText("Playing Voice Note...");

                        activeMediaPlayer.setOnCompletionListener(mp -> {
                            binding.ivAudioPlayPause.setImageResource(R.drawable.ic_play_arrow);
                            binding.tvAudioDuration.setText("Voice Note • Tap to Play");
                        });

                        Toast.makeText(context, "Playing voice message...", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Audio playback error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                binding.layoutAudioAttachment.setVisibility(View.GONE);
            }

            // Location / Man-Down Fall Hazard Attachment
            boolean isLocationMsg = "LOCATION".equalsIgnoreCase(msg.getMediaType()) ||
                    (msg.getMessageText() != null && (msg.getMessageText().contains("[MAN DOWN / FALL DETECTED]") ||
                            msg.getMessageText().contains("📍 Location:") ||
                            msg.getMessageText().contains("maps.google.com")));

            if (isLocationMsg) {
                binding.layoutLocationAttachment.setVisibility(View.VISIBLE);

                // Extract coordinates if present
                final double[] coords = extractCoordinates(msg.getMessageText(), msg.getMediaUrl());
                binding.tvLocationCoords.setText(String.format(Locale.US, "%.4f° N, %.4f° E", coords[0], coords[1]));

                View.OnClickListener openMapListener = v -> {
                    try {
                        Uri mapUri = Uri.parse("geo:" + coords[0] + "," + coords[1] + "?q=" + coords[0] + "," + coords[1] + "(Fallen Crew Location)");
                        android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, mapUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        mapIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(mapIntent);
                    } catch (Exception e) {
                        // Fallback to web browser Google Maps
                        try {
                            Uri webMapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + coords[0] + "," + coords[1]);
                            android.content.Intent webIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, webMapUri);
                            webIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(webIntent);
                        } catch (Exception err) {
                            Toast.makeText(context, "Cannot open map: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                binding.btnOpenGoogleMaps.setOnClickListener(openMapListener);
                binding.layoutLocationAttachment.setOnClickListener(openMapListener);
            } else {
                binding.layoutLocationAttachment.setVisibility(View.GONE);
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

        private static double[] extractCoordinates(String text, String url) {
            double defaultLat = 23.8103;
            double defaultLng = 90.4125;
            try {
                if (url != null && url.contains("query=")) {
                    String part = url.substring(url.indexOf("query=") + 6);
                    String[] split = part.split(",");
                    return new double[]{Double.parseDouble(split[0].trim()), Double.parseDouble(split[1].trim())};
                }
                if (url != null && url.contains("?q=")) {
                    String part = url.substring(url.indexOf("?q=") + 3);
                    String[] split = part.split(",");
                    return new double[]{Double.parseDouble(split[0].trim()), Double.parseDouble(split[1].trim())};
                }
                if (text != null && text.contains("📍 Location:")) {
                    String sub = text.substring(text.indexOf("📍 Location:") + 12).trim();
                    String line = sub.split("\n")[0].trim();
                    String[] split = line.split(",");
                    return new double[]{Double.parseDouble(split[0].trim()), Double.parseDouble(split[1].trim())};
                }
            } catch (Exception ignored) {}
            return new double[]{defaultLat, defaultLng};
        }
    }
}
