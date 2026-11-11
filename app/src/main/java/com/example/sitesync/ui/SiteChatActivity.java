package com.example.sitesync.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.dao.SiteChatMessageDao;
import com.example.sitesync.data.local.entities.SiteChatMessageEntity;
import com.example.sitesync.databinding.ActivitySiteChatBinding;
import com.example.sitesync.hardware.ChatIntelligenceClient;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.example.sitesync.utils.ProjectStorageHelper;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SiteChatActivity extends AppCompatActivity {

    private ActivitySiteChatBinding binding;
    private SessionManager sessionManager;
    private AppDatabase database;
    private SiteChatMessageDao chatDao;
    private SiteChatAdapter adapter;
    private String projectCode;
    private String projectName;
    private boolean isUrgentSelected = false;
    private ListenerRegistration firestoreListener;
    private ListenerRegistration typingListener;
    private ChatIntelligenceClient intelligenceClient;
    private Handler typingStopHandler = new Handler(Looper.getMainLooper());
    private Runnable typingStopRunnable;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private ActivityResultLauncher<String> recordAudioLauncher;

    private android.media.MediaRecorder mediaRecorder;
    private File currentVoiceFile;
    private boolean isRecordingAudio = false;
    private final Handler voiceTimerHandler = new Handler(Looper.getMainLooper());
    private int voiceSeconds = 0;
    private Runnable voiceTimerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySiteChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        database = AppDatabase.getInstance(this);
        chatDao = database.siteChatMessageDao();
        intelligenceClient = new ChatIntelligenceClient(this);

        projectCode = getIntent().getStringExtra("project_code");
        if (TextUtils.isEmpty(projectCode)) {
            projectCode = sessionManager.getActiveProjectCode();
        }
        if (TextUtils.isEmpty(projectCode)) {
            projectCode = "PRJ-DHAKA-101";
        }

        projectName = getIntent().getStringExtra("project_name");
        if (TextUtils.isEmpty(projectName)) {
            projectName = sessionManager.getActiveProjectName();
        }

        setupLaunchers();
        setupViews();
        setupRecyclerView();
        observeLocalMessages();
        setupFirestoreRealtimeSync();
        setupTypingIndicator();
        seedInitialChatIfEmpty();
    }

    private void setupLaunchers() {
        // Photo picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handlePickedPhoto(uri);
                    }
                }
        );

        // Speech-to-Text launcher
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spoken = matches.get(0);
                            String current = binding.etChatMessageInput.getText() != null ?
                                    binding.etChatMessageInput.getText().toString() : "";
                            if (!current.trim().isEmpty()) {
                                binding.etChatMessageInput.setText(current + " " + spoken);
                            } else {
                                binding.etChatMessageInput.setText(spoken);
                            }
                            binding.etChatMessageInput.setSelection(binding.etChatMessageInput.getText().length());
                        }
                    }
                }
        );

        // Audio Recording Permission Launcher
        recordAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startVoiceRecording();
                    } else {
                        Toast.makeText(this, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupViews() {
        binding.tvChatProjectTitle.setText(projectName != null ? projectName : "Site Crew Chat");
        binding.tvChatSubtitleCode.setText(projectCode + " • Live Field Room");

        binding.btnBackFromChat.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            finish();
        });

        binding.btnToggleUrgent.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            isUrgentSelected = !isUrgentSelected;
            if (isUrgentSelected) {
                binding.btnToggleUrgent.setBackgroundResource(R.drawable.bg_pill_alert_coral);
                binding.btnToggleUrgent.setColorFilter(ContextCompat.getColor(this, R.color.alert_coral_text));
            } else {
                binding.btnToggleUrgent.setBackgroundResource(R.drawable.bg_pill_forest_action);
                binding.btnToggleUrgent.setColorFilter(ContextCompat.getColor(this, R.color.text_on_dark_secondary));
            }
        });

        // Photo Attachment Button
        binding.btnAttachPhoto.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            pickImageLauncher.launch("image/*");
        });

        // Voice-to-Text Speech Dictation Button
        binding.btnVoiceToText.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            startSpeechRecognition();
        });

        // Send Message Button
        binding.btnSendChatMessage.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            sendTextMessage();
        });

        // Record Voice Note Button
        binding.btnRecordVoiceNote.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (isRecordingAudio) {
                    stopAndSendVoiceRecording();
                } else {
                    startVoiceRecording();
                }
            } else {
                recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
            }
        });

        binding.btnCancelVoiceRecording.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            cancelVoiceRecording();
        });

        binding.btnSendVoiceRecording.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            stopAndSendVoiceRecording();
        });

        // "Report on Today's Chat" LLM Intelligence Button (Manager/Supervisor only)
        if (sessionManager.isWorkerRole()) {
            binding.btnGenerateChatReport.setVisibility(View.GONE);
        } else {
            binding.btnGenerateChatReport.setVisibility(View.VISIBLE);
            binding.btnGenerateChatReport.setOnClickListener(v -> {
                HapticFeedbackUtil.heavyClick(this, v);
                generateChatIntelligenceReport();
            });
        }
    }

    private void startSpeechRecognition() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your site update or needed materials...");
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice dictation not available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startVoiceRecording() {
        try {
            File dir = new File(getCacheDir(), "voice_notes");
            if (!dir.exists()) dir.mkdirs();
            currentVoiceFile = File.createTempFile("voice_" + System.currentTimeMillis() + "_", ".m4a", dir);

            mediaRecorder = new android.media.MediaRecorder();
            mediaRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(64000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentVoiceFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecordingAudio = true;
            voiceSeconds = 0;
            binding.layoutVoiceRecordingPanel.setVisibility(View.VISIBLE);
            binding.tvVoiceRecordingTimer.setText("Recording Voice Note... 00:00");
            binding.btnRecordVoiceNote.setImageResource(R.drawable.ic_pause);

            voiceTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isRecordingAudio) {
                        voiceSeconds++;
                        int mins = voiceSeconds / 60;
                        int secs = voiceSeconds % 60;
                        binding.tvVoiceRecordingTimer.setText(
                                String.format(Locale.US, "Recording Voice Note... %02d:%02d", mins, secs)
                        );
                        voiceTimerHandler.postDelayed(this, 1000);
                    }
                }
            };
            voiceTimerHandler.postDelayed(voiceTimerRunnable, 1000);
            HapticFeedbackUtil.lightClick(this, null);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            cancelVoiceRecording();
        }
    }

    private void cancelVoiceRecording() {
        try {
            if (mediaRecorder != null) {
                if (isRecordingAudio) {
                    try { mediaRecorder.stop(); } catch (Exception ignored) {}
                }
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception ignored) {}

        isRecordingAudio = false;
        voiceTimerHandler.removeCallbacksAndMessages(null);
        if (currentVoiceFile != null && currentVoiceFile.exists()) {
            currentVoiceFile.delete();
            currentVoiceFile = null;
        }

        binding.layoutVoiceRecordingPanel.setVisibility(View.GONE);
        binding.btnRecordVoiceNote.setImageResource(R.drawable.ic_mic);
    }

    private void stopAndSendVoiceRecording() {
        if (!isRecordingAudio || currentVoiceFile == null) {
            cancelVoiceRecording();
            return;
        }

        try {
            if (mediaRecorder != null) {
                try { mediaRecorder.stop(); } catch (Exception ignored) {}
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception ignored) {}

        isRecordingAudio = false;
        voiceTimerHandler.removeCallbacksAndMessages(null);
        binding.layoutVoiceRecordingPanel.setVisibility(View.GONE);
        binding.btnRecordVoiceNote.setImageResource(R.drawable.ic_mic);

        int finalDuration = Math.max(1, voiceSeconds);
        String voicePath = currentVoiceFile.getAbsolutePath();

        User user = sessionManager.getCurrentUser();
        String senderId = user != null ? user.getUid() : "anon_user";
        String senderName = user != null ? user.getFullName() : "Site Crew";
        boolean isWorker = sessionManager.isWorkerRole();
        String role = isWorker ? "WORKER" : (user != null && user.getRole() != null ? user.getRole() : "PROJECT_MANAGER");

        SiteChatMessageEntity voiceMessage = new SiteChatMessageEntity(
                UUID.randomUUID().toString(),
                projectCode,
                senderId,
                senderName,
                role,
                "Voice Note (" + finalDuration + "s)",
                System.currentTimeMillis(),
                isUrgentSelected
        );
        voiceMessage.setMediaType("AUDIO");
        voiceMessage.setMediaUrl(voicePath);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            chatDao.insert(voiceMessage);
        });

        // Sync with Firebase Firestore
        Map<String, Object> map = new HashMap<>();
        map.put("id", voiceMessage.getId());
        map.put("projectCode", projectCode);
        map.put("senderId", senderId);
        map.put("senderName", senderName);
        map.put("senderRole", role);
        map.put("messageText", voiceMessage.getMessageText());
        map.put("timestamp", voiceMessage.getTimestamp());
        map.put("isUrgent", isUrgentSelected);
        map.put("mediaType", "AUDIO");
        map.put("mediaUrl", voicePath);

        FirebaseFirestore.getInstance()
                .collection("site_chat_messages")
                .document(voiceMessage.getId())
                .set(map)
                .addOnSuccessListener(aVoid -> {})
                .addOnFailureListener(e -> {});

        HapticFeedbackUtil.confirm(this);
        Toast.makeText(this, "Voice note sent!", Toast.LENGTH_SHORT).show();
    }

    private void handlePickedPhoto(Uri uri) {
        try {
            File destDir = ProjectStorageHelper.getChatMediaDir(this, projectCode);
            File destFile = new File(destDir, "chat_img_" + System.currentTimeMillis() + ".jpg");

            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            // Post image message
            User currentUser = sessionManager.getCurrentUser();
            String senderId = currentUser != null ? currentUser.getUsername() : "worker_me";
            String senderName = currentUser != null ? currentUser.getFullName() : "Site Worker";
            String senderRole = currentUser != null && currentUser.getRole() != null ? currentUser.getRole() : "WORKER";
            String msgId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            SiteChatMessageEntity message = new SiteChatMessageEntity(
                    msgId, projectCode, senderId, senderName, senderRole, "Attached site photo", now, isUrgentSelected
            );
            message.setMediaType("IMAGE");
            message.setMediaUrl(destFile.getAbsolutePath());

            AppDatabase.databaseWriteExecutor.execute(() -> chatDao.insert(message));
            pushMessageToFirestore(message);

            Toast.makeText(this, "Photo shared in chat", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to attach photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        User currentUser = sessionManager.getCurrentUser();
        String currentUid = (currentUser != null && currentUser.getUsername() != null) ?
                currentUser.getUsername() : "current_user";

        adapter = new SiteChatAdapter(currentUid);
        adapter.setOnMessageImageClickListener(message -> {
            if (message != null && message.getMediaUrl() != null && !message.getMediaUrl().isEmpty()) {
                ChatImageViewerDialog dialog = ChatImageViewerDialog.newInstance(message);
                dialog.show(getSupportFragmentManager(), "ChatImageViewer");
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvChatMessages.setLayoutManager(layoutManager);
        binding.rvChatMessages.setAdapter(adapter);
    }

    private void observeLocalMessages() {
        chatDao.getMessagesForProjectLive(projectCode).observe(this, messages -> {
            if (messages != null) {
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    binding.rvChatMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }

    private void setupTypingIndicator() {
        User currentUser = sessionManager.getCurrentUser();
        String currentUid = (currentUser != null && currentUser.getUsername() != null) ? currentUser.getUsername() : "me";
        String currentName = (currentUser != null) ? currentUser.getFullName() : "User";

        binding.etChatMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTypingStatus(currentUid, currentName, s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (typingStopRunnable != null) {
                    typingStopHandler.removeCallbacks(typingStopRunnable);
                }
                typingStopRunnable = () -> updateTypingStatus(currentUid, currentName, false);
                typingStopHandler.postDelayed(typingStopRunnable, 2500);
            }
        });

        // Listen for typing from other users via Firestore
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            typingListener = db.collection("projects").document(projectCode)
                    .collection("typing")
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null || snapshots == null) return;
                        String typingUser = null;
                        long now = System.currentTimeMillis();

                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (!doc.getId().equals(currentUid)) {
                                Boolean isTyping = doc.getBoolean("isTyping");
                                Long updated = doc.getLong("timestamp");
                                if (isTyping != null && isTyping && updated != null && (now - updated < 5000)) {
                                    typingUser = doc.getString("userName");
                                    break;
                                }
                            }
                        }

                        if (typingUser != null) {
                            binding.layoutTypingIndicator.setVisibility(View.VISIBLE);
                            binding.tvTypingIndicator.setText(typingUser + " is typing...");
                        } else {
                            binding.layoutTypingIndicator.setVisibility(View.GONE);
                        }
                    });
        } catch (Exception ignored) {}
    }

    private void updateTypingStatus(String uid, String name, boolean isTyping) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userName", name);
            map.put("isTyping", isTyping);
            map.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("projects").document(projectCode)
                    .collection("typing").document(uid)
                    .set(map);
        } catch (Exception ignored) {}
    }

    private void setupFirestoreRealtimeSync() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            firestoreListener = db.collection("projects").document(projectCode)
                    .collection("chat_messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .limit(100)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null || snapshots == null) return;

                        List<SiteChatMessageEntity> newFromCloud = new ArrayList<>();
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String id = dc.getDocument().getId();
                                String text = dc.getDocument().getString("messageText");
                                String sId = dc.getDocument().getString("senderId");
                                String sName = dc.getDocument().getString("senderName");
                                String sRole = dc.getDocument().getString("senderRole");
                                Long ts = dc.getDocument().getLong("timestamp");
                                Boolean urgent = dc.getDocument().getBoolean("isUrgent");
                                String mediaType = dc.getDocument().getString("mediaType");
                                String mediaUrl = dc.getDocument().getString("mediaUrl");

                                if (text != null) {
                                    SiteChatMessageEntity entity = new SiteChatMessageEntity(
                                            id, projectCode, sId != null ? sId : "",
                                            sName != null ? sName : "Crew",
                                            sRole != null ? sRole : "WORKER",
                                            text,
                                            ts != null ? ts : System.currentTimeMillis(),
                                            urgent != null && urgent
                                    );
                                    entity.setMediaType(mediaType != null ? mediaType : "TEXT");
                                    entity.setMediaUrl(mediaUrl);
                                    newFromCloud.add(entity);
                                }
                            }
                        }

                        if (!newFromCloud.isEmpty()) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                chatDao.insertAll(newFromCloud);
                            });
                        }
                    });
        } catch (Exception ignored) {
            // Firestore sync offline fallback
        }
    }

    private void sendTextMessage() {
        String text = binding.etChatMessageInput.getText() != null ?
                binding.etChatMessageInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(text)) return;

        User currentUser = sessionManager.getCurrentUser();
        String senderId = currentUser != null ? currentUser.getUsername() : "worker_me";
        String senderName = currentUser != null ? currentUser.getFullName() : "Site Worker";
        String senderRole = currentUser != null && currentUser.getRole() != null ? currentUser.getRole() : "WORKER";
        String msgId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        SiteChatMessageEntity message = new SiteChatMessageEntity(
                msgId, projectCode, senderId, senderName, senderRole, text, now, isUrgentSelected
        );
        message.setMediaType("TEXT");

        // Clear input field immediately
        binding.etChatMessageInput.setText("");
        if (isUrgentSelected) {
            binding.btnToggleUrgent.performClick();
        }

        // 1. Insert into local Room Database
        AppDatabase.databaseWriteExecutor.execute(() -> {
            chatDao.insert(message);
        });

        // 2. Push to Firestore
        pushMessageToFirestore(message);
    }

    private void pushMessageToFirestore(SiteChatMessageEntity message) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("id", message.getId());
            data.put("projectCode", projectCode);
            data.put("senderId", message.getSenderId());
            data.put("senderName", message.getSenderName());
            data.put("senderRole", message.getSenderRole());
            data.put("messageText", message.getMessageText());
            data.put("timestamp", message.getTimestamp());
            data.put("isUrgent", message.isUrgent());
            data.put("mediaType", message.getMediaType());
            data.put("mediaUrl", message.getMediaUrl());

            FirebaseFirestore.getInstance().collection("projects").document(projectCode)
                    .collection("chat_messages").document(message.getId())
                    .set(data);

            FirebaseFirestore.getInstance().collection("site_chat_messages")
                    .document(message.getId())
                    .set(data);
        } catch (Exception ignored) {}
    }

    private void generateChatIntelligenceReport() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Analyzing Chat with Multimodal AI & Benchmarking Materials...");
        progress.setCancelable(false);
        progress.show();

        long lastTimestamp = sessionManager.getLastChatAnalysisTimestamp(projectCode);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<SiteChatMessageEntity> unanalyzed = chatDao.getMessagesSince(projectCode, lastTimestamp);
            if (unanalyzed == null || unanalyzed.isEmpty()) {
                // If no messages since last timestamp, fallback to analyzing all recent project messages
                unanalyzed = chatDao.getMessagesForProject(projectCode);
            }

            final List<SiteChatMessageEntity> targetList = unanalyzed;

            intelligenceClient.analyzeChatMessages(projectCode, targetList, new ChatIntelligenceClient.OnIntelligenceCompletedListener() {
                @Override
                public void onSuccess(ChatIntelligenceClient.ChatIntelligenceResult result) {
                    runOnUiThread(() -> {
                        if (progress.isShowing()) progress.dismiss();
                        sessionManager.setLastChatAnalysisTimestamp(projectCode, System.currentTimeMillis());

                        ChatMaterialsReportBottomSheetDialog dialog = ChatMaterialsReportBottomSheetDialog.newInstance(projectCode, result);
                        dialog.show(getSupportFragmentManager(), "ChatMaterialsReportDialog");
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        if (progress.isShowing()) progress.dismiss();
                        Toast.makeText(SiteChatActivity.this, "Intelligence error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    private void seedInitialChatIfEmpty() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<SiteChatMessageEntity> existing = chatDao.getMessagesForProject(projectCode);
            if (existing == null || existing.isEmpty()) {
                long now = System.currentTimeMillis();
                List<SiteChatMessageEntity> seed = new ArrayList<>();
                seed.add(new SiteChatMessageEntity(
                        UUID.randomUUID().toString(), projectCode, "manager_1",
                        "Engr. Marcus Vance", "PROJECT_MANAGER",
                        "Good morning team. Today our priority is 2nd-floor beam shuttering and Thai glass window fitting in Section B.",
                        now - 7200000, false
                ));
                seed.add(new SiteChatMessageEntity(
                        UUID.randomUUID().toString(), projectCode, "worker_1",
                        "Rafiqul Islam", "WORKER",
                        "Received sir. 16mm rebar rings are bound and ready. We are starting plaster work in Unit 101 now.",
                        now - 3600000, false
                ));
                seed.add(new SiteChatMessageEntity(
                        UUID.randomUUID().toString(), projectCode, "worker_1",
                        "Rafiqul Islam", "WORKER",
                        "Sir, 10 bag cement lagbe ar 300 kg 16mm rod lagbe afternoon beam pouring korar jonno.",
                        now - 2400000, false
                ));
                seed.add(new SiteChatMessageEntity(
                        UUID.randomUUID().toString(), projectCode, "super_1",
                        "Alex Chen", "SITE_SUPERVISOR",
                        "Safety Reminder: Water leaking in 3rd floor plumbing shaft, valve loose. All workers on the 3rd floor scaffolding must wear full safety harnesses.",
                        now - 1800000, true
                ));
                chatDao.insertAll(seed);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
        if (typingListener != null) {
            typingListener.remove();
        }
    }
}
