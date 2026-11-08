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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIssueDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new IssueRepository(this);
        issueId = getIntent().getStringExtra(IssueManagementActivity.EXTRA_ISSUE_ID);

        initViews();
        setupListeners();
        setupRealtimeDiscussionStream();
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
            binding.tvDetailPriority.setBackgroundColor(Color.parseColor("#FEE2E2"));
            binding.tvDetailPriority.setTextColor(Color.parseColor("#DC2626"));
        } else if (TechnicalIssue.PRIORITY_LOW.equalsIgnoreCase(priority)) {
            binding.tvDetailPriority.setBackgroundColor(Color.parseColor("#DBEAFE"));
            binding.tvDetailPriority.setTextColor(Color.parseColor("#2563EB"));
        } else {
            binding.tvDetailPriority.setBackgroundColor(Color.parseColor("#FEF3C7"));
            binding.tvDetailPriority.setTextColor(Color.parseColor("#D97706"));
        }

        // Status styling
        String status = currentIssue.getStatus() != null ? currentIssue.getStatus() : TechnicalIssue.STATUS_OPEN;
        binding.tvDetailStatus.setText(status);
        if (TechnicalIssue.STATUS_RESOLVED.equalsIgnoreCase(status) || TechnicalIssue.STATUS_CLOSED.equalsIgnoreCase(status)) {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#DCFCE7"));
            binding.tvDetailStatus.setTextColor(Color.parseColor("#16A34A"));
        } else if (TechnicalIssue.STATUS_IN_REVIEW.equalsIgnoreCase(status)) {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#E0E7FF"));
            binding.tvDetailStatus.setTextColor(Color.parseColor("#4F46E5"));
        } else if (TechnicalIssue.STATUS_ASSIGNED.equalsIgnoreCase(status)) {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            binding.tvDetailStatus.setTextColor(Color.parseColor("#D97706"));
        } else {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#F1F5F9"));
            binding.tvDetailStatus.setTextColor(Color.parseColor("#475569"));
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
