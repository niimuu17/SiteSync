package com.example.sitesync.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.sitesync.data.local.entities.SiteChatMessageEntity;
import com.example.sitesync.databinding.DialogChatImageViewerBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatImageViewerDialog extends DialogFragment {

    private static final String ARG_MEDIA_URL = "media_url";
    private static final String ARG_SENDER_NAME = "sender_name";
    private static final String ARG_SENDER_ROLE = "sender_role";
    private static final String ARG_TIMESTAMP = "timestamp";

    private DialogChatImageViewerBinding binding;

    public static ChatImageViewerDialog newInstance(SiteChatMessageEntity message) {
        ChatImageViewerDialog dialog = new ChatImageViewerDialog();
        Bundle args = new Bundle();
        if (message != null) {
            args.putString(ARG_MEDIA_URL, message.getMediaUrl());
            args.putString(ARG_SENDER_NAME, message.getSenderName());
            args.putString(ARG_SENDER_ROLE, message.getSenderRole());
            args.putLong(ARG_TIMESTAMP, message.getTimestamp());
        }
        dialog.setArguments(args);
        return dialog;
    }

    public static ChatImageViewerDialog newInstance(String mediaUrl, String title, String subtitle, long timestamp) {
        ChatImageViewerDialog dialog = new ChatImageViewerDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_URL, mediaUrl);
        args.putString(ARG_SENDER_NAME, title);
        args.putString(ARG_SENDER_ROLE, subtitle);
        args.putLong(ARG_TIMESTAMP, timestamp);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogChatImageViewerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }

        String mediaUrl = args.getString(ARG_MEDIA_URL, "");
        String senderName = args.getString(ARG_SENDER_NAME, "Site Crew");
        String senderRole = args.getString(ARG_SENDER_ROLE, "WORKER");
        long timestamp = args.getLong(ARG_TIMESTAMP, System.currentTimeMillis());

        binding.tvViewerSenderName.setText(senderName);
        binding.tvViewerSenderRole.setText(senderRole != null ? senderRole : "CREW");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.US);
        binding.tvViewerTimestamp.setText(sdf.format(new Date(timestamp)) + " • Site Photo");

        binding.btnCloseImageViewer.setOnClickListener(v -> dismiss());

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            File localFile = new File(mediaUrl);
            if (localFile.exists()) {
                Glide.with(this).load(localFile).into(binding.ivFullChatImage);
            } else {
                Glide.with(this).load(mediaUrl).into(binding.ivFullChatImage);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
