package com.example.sitesync.ui;

import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.IssueRepository;
import com.example.sitesync.databinding.ActivityIssueDetailBinding;
import com.example.sitesync.models.IssueComment;
import com.example.sitesync.models.TechnicalIssue;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Screen displaying full details of an RFI / Technical Issue.
 * Allows updating status lifecycle, reading and posting discussion replies,
 * and reviewing full audit history.
 */
public class IssueDetailActivity extends AppCompatActivity {

    private ActivityIssueDetailBinding binding;
    private IssueRepository repository;
    private IssueCommentAdapter commentAdapter;
    private IssueHistoryLogAdapter historyAdapter;
    private String issueId;
    private TechnicalIssue currentIssue;

    private ValueEventListener rtdbCommentsListener;
    private DatabaseReference rtdbCommentsRef;
    private com.example.sitesync.hardware.VoiceDictationManager voiceDictationManager;

    private final androidx.activity.result.ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startDictation();
                } else {
                    Toast.makeText(this, "Microphone permission is required to Speak Note", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIssueDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new IssueRepository(this);
        issueId = getIntent().getStringExtra(IssueManagementActivity.EXTRA_ISSUE_ID);

        voiceDictationManager = new com.example.sitesync.hardware.VoiceDictationManager(this);

        initViews();
        setupListeners();
        setupRealtimeDiscussionStream();
    }

    private void startDictation() {
        setMicUiActive(true);
        String initialText = binding.etNewComment.getText() != null ? binding.etNewComment.getText().toString() : "";

        voiceDictationManager.startListening(new com.example.sitesync.hardware.VoiceDictationManager.OnDictationListener() {
            @Override
            public void onSpeechRmsChanged(float rmsdB) {}

            @Override
            public void onPartialTranscription(String partialText) {
                if (binding.etNewComment != null) {
                    String combined = initialText.isEmpty() ? partialText : initialText + " " + partialText;
                    binding.etNewComment.setText(combined);
                    binding.etNewComment.setSelection(combined.length());
                }
            }

            @Override
            public void onFinalTranscription(String finalText, java.io.File audioBackupFile) {
                setMicUiActive(false);
                if (binding.etNewComment != null && !finalText.isEmpty()) {
                    String combined = initialText.isEmpty() ? finalText : initialText + " " + finalText;
                    binding.etNewComment.setText(combined);
                    binding.etNewComment.setSelection(combined.length());
                }
            }

            @Override
            public void onError(String userFriendlyError) {
                setMicUiActive(false);
                Toast.makeText(IssueDetailActivity.this, userFriendlyError, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setMicUiActive(boolean active) {
        if (binding.btnDictateComment == null) return;
        if (active) {
            binding.btnDictateComment.setBackgroundResource(R.drawable.btn_mic_dictate_active);
            binding.btnDictateComment.setImageResource(R.drawable.ic_mic_black);
        } else {
            binding.btnDictateComment.setBackgroundResource(R.drawable.btn_mic_dictate);
            binding.btnDictateComment.setImageResource(R.drawable.ic_mic);
        }
    }

    private void setupRealtimeDiscussionStream() {
        if (issueId == null) return;
        try {
            rtdbCommentsRef = FirebaseDatabase.getInstance()
                    .getReference("rfi_threads")
                    .child(issueId)
                    .child("comments");

            rtdbCommentsListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists() && currentIssue != null) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String cId = child.child("commentId").getValue(String.class);
                            String author = child.child("authorName").getValue(String.class);
                            String role = child.child("authorRole").getValue(String.class);
                            String time = child.child("timestamp").getValue(String.class);
                            String msg = child.child("message").getValue(String.class);

                            if (msg != null && author != null) {
                                boolean exists = false;
                                for (IssueComment existing : currentIssue.getComments()) {
                                    if (cId != null && cId.equals(existing.getCommentId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    currentIssue.addComment(new IssueComment(cId, author, role, time, msg));
                                }
                            }
                        }
                        commentAdapter.setComments(currentIssue.getComments());
                        binding.rvDetailComments.setVisibility(View.VISIBLE);
                        binding.tvNoCommentsNotice.setVisibility(View.GONE);
                        int count = currentIssue.getComments().size();
                        binding.tvDetailCommentsCount.setText(count + (count == 1 ? " reply" : " replies"));
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Fall back to local cache gracefully
                }
            };
            rtdbCommentsRef.addValueEventListener(rtdbCommentsListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rtdbCommentsRef != null && rtdbCommentsListener != null) {
            rtdbCommentsRef.removeEventListener(rtdbCommentsListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIssueDetails();
    }

    private void initViews() {
        // Comments RecyclerView
        commentAdapter = new IssueCommentAdapter();
        binding.rvDetailComments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDetailComments.setAdapter(commentAdapter);

        // History RecyclerView
        historyAdapter = new IssueHistoryLogAdapter();
        binding.rvDetailHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDetailHistory.setAdapter(historyAdapter);
    }

    private void setupListeners() {
        binding.btnBackFromDetail.setOnClickListener(v -> finish());

        // Status update dialog
        binding.btnChangeStatus.setOnClickListener(v -> showUpdateStatusDialog());

        // Post new comment
        binding.btnSendComment.setOnClickListener(v -> postComment());

        // Speak Note mic button
        binding.btnDictateComment.setOnClickListener(v -> {
            com.example.sitesync.utils.HapticFeedbackUtil.heavyClick(this, v);
            if (voiceDictationManager.isListening()) {
                voiceDictationManager.stopListening();
                setMicUiActive(false);
            } else {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    startDictation();
                } else {
                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
                }
            }
        });
    }

    private void loadIssueDetails() {
        if (issueId == null) {
            Toast.makeText(this, "Issue not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentIssue = repository.getIssueById(issueId);
        if (currentIssue == null) {
            Toast.makeText(this, "Issue not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvDetailHeaderId.setText(currentIssue.getId());
        binding.tvDetailTitle.setText(currentIssue.getTitle());
        binding.tvDetailProject.setText(currentIssue.getProjectName());
        binding.tvDetailLocation.setText(currentIssue.getSiteLocation());
        binding.tvDetailAssignedPerson.setText(currentIssue.getAssignedPerson());
        binding.tvDetailCreatedBy.setText(currentIssue.getCreatedBy() + " • " + currentIssue.getCreatedDate());
        binding.tvDetailDescription.setText(currentIssue.getDescription());
        binding.btnChangeStatus.setText("Update Status (Current: " + currentIssue.getStatus() + ")");

        // Priority styling
        String priority = currentIssue.getPriority() != null ? currentIssue.getPriority() : TechnicalIssue.PRIORITY_MEDIUM;
        binding.tvDetailPriority.setText(priority + " Priority");
        if (TechnicalIssue.PRIORITY_HIGH.equalsIgnoreCase(priority)) {
            binding.tvDetailPriority.setBackgroundColor(Color.parseColor("#381E1E"));
            binding.tvDetailPriority.setTextColor(Color.parseColor("#EF4444"));
        } else if (TechnicalIssue.PRIORITY_LOW.equalsIgnoreCase(priority)) {
            binding.tvDetailPriority.setBackgroundColor(Color.parseColor("#1E293B"));
            binding.tvDetailPriority.setTextColor(Color.parseColor("#38BDF8"));
        } else {
            binding.tvDetailPriority.setBackgroundColor(Color.parseColor("#382B14"));
            binding.tvDetailPriority.setTextColor(Color.parseColor("#F59E0B"));
        }

        // Status styling
        String status = currentIssue.getStatus() != null ? currentIssue.getStatus() : TechnicalIssue.STATUS_OPEN;
        binding.tvDetailStatus.setText(status);
        if (TechnicalIssue.STATUS_RESOLVED.equalsIgnoreCase(status) || TechnicalIssue.STATUS_CLOSED.equalsIgnoreCase(status)) {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#123324"));
            binding.tvDetailStatus.setTextColor(Color.parseColor("#10B981"));
        } else if (TechnicalIssue.STATUS_IN_REVIEW.equalsIgnoreCase(status)) {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#1E293B"));
            binding.tvDetailStatus.setTextColor(Color.parseColor("#818CF8"));
        } else if (TechnicalIssue.STATUS_ASSIGNED.equalsIgnoreCase(status)) {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#382B14"));
            binding.tvDetailStatus.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#222631"));
            binding.tvDetailStatus.setTextColor(Color.parseColor("#94A3B8"));
        }

        // Photo Attachment handling
        if (currentIssue.getPhotoUri() != null && !currentIssue.getPhotoUri().isEmpty()) {
            binding.cardPhotoAttachment.setVisibility(View.VISIBLE);
            binding.tvNoPhotoNotice.setVisibility(View.GONE);
            binding.ivDetailPhoto.setVisibility(View.VISIBLE);
            try {
                binding.ivDetailPhoto.setImageURI(Uri.parse(currentIssue.getPhotoUri()));
            } catch (Exception e) {
                binding.ivDetailPhoto.setVisibility(View.GONE);
                binding.tvNoPhotoNotice.setText("Unable to preview attached image");
                binding.tvNoPhotoNotice.setVisibility(View.VISIBLE);
            }
        } else {
            binding.ivDetailPhoto.setVisibility(View.GONE);
            binding.tvNoPhotoNotice.setVisibility(View.VISIBLE);
        }

        // Comments list
        if (currentIssue.getComments() != null && !currentIssue.getComments().isEmpty()) {
            commentAdapter.setComments(currentIssue.getComments());
            binding.rvDetailComments.setVisibility(View.VISIBLE);
            binding.tvNoCommentsNotice.setVisibility(View.GONE);
            int count = currentIssue.getComments().size();
            binding.tvDetailCommentsCount.setText(count + (count == 1 ? " reply" : " replies"));
        } else {
            commentAdapter.setComments(null);
            binding.rvDetailComments.setVisibility(View.GONE);
            binding.tvNoCommentsNotice.setVisibility(View.VISIBLE);
            binding.tvDetailCommentsCount.setText("0 replies");
        }

        // History logs
        if (currentIssue.getHistoryLogs() != null) {
            historyAdapter.setHistoryLogs(currentIssue.getHistoryLogs());
        }
    }

    private void showUpdateStatusDialog() {
        final String[] statusOptions = {
                TechnicalIssue.STATUS_OPEN,
                TechnicalIssue.STATUS_ASSIGNED,
                TechnicalIssue.STATUS_IN_REVIEW,
                TechnicalIssue.STATUS_RESOLVED,
                TechnicalIssue.STATUS_CLOSED
        };

        int checkedItem = 0;
        for (int i = 0; i < statusOptions.length; i++) {
            if (statusOptions[i].equalsIgnoreCase(currentIssue.getStatus())) {
                checkedItem = i;
                break;
            }
        }

        final int[] selectedIndex = {checkedItem};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Status for " + currentIssue.getId())
                .setSingleChoiceItems(statusOptions, checkedItem, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newStatus = statusOptions[selectedIndex[0]];
                    if (!newStatus.equalsIgnoreCase(currentIssue.getStatus())) {
                        boolean success = repository.updateIssueStatus(
                                currentIssue.getId(),
                                newStatus,
                                "Site Engineer",
                                "Status updated via Mobile Hub"
                        );
                        if (success) {
                            Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            loadIssueDetails();
                        } else {
                            Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void postComment() {
        String message = binding.etNewComment.getText() != null
                ? binding.etNewComment.getText().toString().trim() : "";

        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a comment or clarification", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentId = "C-" + System.currentTimeMillis();
        String timestamp = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());

        IssueComment newComment = new IssueComment(
                commentId,
                "Alex Chen",
                "Site Engineer",
                timestamp,
                message
        );

        boolean success = repository.addCommentToIssue(currentIssue.getId(), newComment);
        if (success) {
            // Push to Firebase RTDB for sub-second sync across field devices
            if (rtdbCommentsRef != null) {
                java.util.Map<String, Object> rtdbPayload = new java.util.HashMap<>();
                rtdbPayload.put("commentId", commentId);
                rtdbPayload.put("authorName", newComment.getAuthorName());
                rtdbPayload.put("authorRole", newComment.getAuthorRole());
                rtdbPayload.put("timestamp", timestamp);
                rtdbPayload.put("message", message);
                rtdbCommentsRef.child(commentId).setValue(rtdbPayload);
            }
            binding.etNewComment.setText("");
            Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show();
            loadIssueDetails();
        } else {
            Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show();
        }
    }
}
