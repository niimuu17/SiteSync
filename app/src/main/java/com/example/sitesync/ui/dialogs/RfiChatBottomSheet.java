package com.example.sitesync.ui.dialogs;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.IssueRepository;
import com.example.sitesync.hardware.VoiceDictationManager;
import com.example.sitesync.models.IssueComment;
import com.example.sitesync.models.TechnicalIssue;
import com.example.sitesync.ui.IssueCommentAdapter;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * High-contrast industrial bottom sheet for live RFI / Technical Issue collaboration.
 * Embeds hands-free voice dictation ("Speak Note") and real-time Room + Firestore sync.
 */
public class RfiChatBottomSheet extends BottomSheetDialogFragment {

    private TechnicalIssue issue;
    private IssueRepository issueRepository;
    private VoiceDictationManager voiceManager;
    private boolean isListening = false;
    private IssueCommentAdapter commentAdapter;
    private final List<IssueComment> commentList = new ArrayList<>();

    private TextView tvRfiTicketId;
    private TextView tvRfiStatusBadge;
    private TextView tvRfiTitle;
    private RecyclerView rvRfiChatThread;
    private EditText etChatMessage;
    private ImageButton btnVoiceChat;
    private ImageButton btnSendChat;

    public static RfiChatBottomSheet newInstance(TechnicalIssue issue) {
        RfiChatBottomSheet sheet = new RfiChatBottomSheet();
        sheet.issue = issue;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_rfi_chat_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        issueRepository = new IssueRepository(requireContext());

        tvRfiTicketId = view.findViewById(R.id.tvRfiTicketId);
        tvRfiStatusBadge = view.findViewById(R.id.tvRfiStatusBadge);
        tvRfiTitle = view.findViewById(R.id.tvRfiTitle);
        rvRfiChatThread = view.findViewById(R.id.rvRfiChatThread);
        etChatMessage = view.findViewById(R.id.etChatMessage);
        btnVoiceChat = view.findViewById(R.id.btnVoiceChat);
        btnSendChat = view.findViewById(R.id.btnSendChat);

        rvRfiChatThread.setLayoutManager(new LinearLayoutManager(requireContext()));
        commentAdapter = new IssueCommentAdapter(commentList);
        rvRfiChatThread.setAdapter(commentAdapter);

        if (issue != null) {
            tvRfiTicketId.setText(issue.getId());
            tvRfiStatusBadge.setText(issue.getStatus());
            tvRfiTitle.setText(issue.getTitle());

            if (issue.getComments() != null) {
                commentList.clear();
                commentList.addAll(issue.getComments());
                commentAdapter.notifyDataSetChanged();
                if (!commentList.isEmpty()) {
                    rvRfiChatThread.scrollToPosition(commentList.size() - 1);
                }
            }
        }

        setupVoiceDictation();

        btnSendChat.setOnClickListener(v -> sendComment());
    }

    private void sendComment() {
        String text = etChatMessage.getText() != null ? etChatMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        HapticFeedbackUtil.lightClick(requireContext(), btnSendChat);

        String timestamp = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
        IssueComment comment = new IssueComment(
                UUID.randomUUID().toString(),
                "Field Staff",
                "Engineer",
                timestamp,
                text
        );

        commentList.add(comment);
        commentAdapter.notifyItemInserted(commentList.size() - 1);
        rvRfiChatThread.scrollToPosition(commentList.size() - 1);
        etChatMessage.setText("");

        if (issue != null) {
            issueRepository.addCommentToIssue(issue.getId(), comment);
        }
    }

    private void setupVoiceDictation() {
        voiceManager = new VoiceDictationManager(requireContext());
        btnVoiceChat.setOnClickListener(v -> {
            if (!isListening) {
                btnVoiceChat.setBackgroundResource(R.drawable.btn_mic_dictate_active);
                isListening = true;
                voiceManager.startListening(new VoiceDictationManager.OnDictationListener() {
                    @Override
                    public void onSpeechRmsChanged(float rmsdB) {}

                    @Override
                    public void onPartialTranscription(String partialText) {
                        etChatMessage.setText(partialText);
                    }

                    @Override
                    public void onFinalTranscription(String finalText, File audioBackupFile) {
                        etChatMessage.setText(finalText);
                        btnVoiceChat.setBackgroundResource(R.drawable.btn_mic_dictate);
                        isListening = false;
                    }

                    @Override
                    public void onError(String userFriendlyError) {
                        btnVoiceChat.setBackgroundResource(R.drawable.btn_mic_dictate);
                        isListening = false;
                    }
                });
            } else {
                voiceManager.stopListening();
                btnVoiceChat.setBackgroundResource(R.drawable.btn_mic_dictate);
                isListening = false;
            }
        });
    }

    @Override
    public void onDestroy() {
        if (voiceManager != null) {
            voiceManager.destroy();
        }
        super.onDestroy();
    }
}
