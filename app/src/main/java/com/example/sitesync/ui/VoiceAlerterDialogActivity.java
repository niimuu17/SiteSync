package com.example.sitesync.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.SiteChatMessageEntity;
import com.example.sitesync.databinding.ActivityVoiceAlerterDialogBinding;
import com.example.sitesync.hardware.EmergencySpeechClassifier;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class VoiceAlerterDialogActivity extends AppCompatActivity {

    private ActivityVoiceAlerterDialogBinding binding;
    private EmergencySpeechClassifier speechClassifier;
    private SessionManager sessionManager;
    private ActivityResultLauncher<Intent> speechLauncher;

    private ToneGenerator sirenToneGenerator;
    private Vibrator vibrator;
    private boolean isSirenActive = false;
    private Handler sirenHandler = new Handler(Looper.getMainLooper());
    private Runnable sirenRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoiceAlerterDialogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        speechClassifier = new EmergencySpeechClassifier(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setupSpeechLauncher();
        setupViews();

        // Check if an initial phrase was passed via intent (e.g. for testing)
        String testPhrase = getIntent().getStringExtra("test_speech");
        if (testPhrase != null && !testPhrase.isEmpty()) {
            evaluateSpokenUtterance(testPhrase);
        } else {
            // Auto-trigger microphone on launch
            startListening();
        }
    }

    private void setupSpeechLauncher() {
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spoken = matches.get(0);
                            evaluateSpokenUtterance(spoken);
                        } else {
                            binding.tvSpokenTranscript.setText("No speech recognized. Tap mic to retry.");
                        }
                    } else {
                        binding.tvSpokenTranscript.setText("Tap mic to speak emergency...");
                    }
                }
        );
    }

    private void setupViews() {
        binding.btnVoiceMicTrigger.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            startListening();
        });

        binding.btnDismissVoiceAlerter.setOnClickListener(v -> {
            stopSiren();
            finish();
        });

        binding.btnSilenceSiren.setOnClickListener(v -> {
            stopSiren();
            binding.btnSilenceSiren.setVisibility(View.GONE);
            binding.tvEvaluationStatus.setText("Siren Silenced • Alert broadcast recorded.");
        });
    }

    private void startListening() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD");
            intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{"bn-BD", "en-US"});
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak in Bengali or English (e.g. '3 ta led bulb lagbe')");
            speechLauncher.launch(intent);
            binding.tvSpokenTranscript.setText("Listening (Bengali / English)...");
            binding.tvEvaluationStatus.setText("Microphone active");
        } catch (Exception e) {
            binding.tvSpokenTranscript.setText("Voice recognition error: " + e.getMessage());
        }
    }

    private void evaluateSpokenUtterance(String text) {
        binding.tvSpokenTranscript.setText("\"" + text + "\"");
        binding.tvEvaluationStatus.setText("AI Classifier Evaluating Spoken Threat Level...");

        speechClassifier.classifySpeech(text, classification -> {
            if (classification.isEmergency) {
                // TRIGGER EMERGENCY ALARM & SIREN
                triggerEmergencyAlarm(classification);
            } else if (classification.isMaterialRequest) {
                // DISPATCH MATERIAL REQUISITION TO SITE CHAT ROOM
                handleMaterialRequest(classification);
            } else {
                // Casual chatter - also post message to chat room as requested
                handleCasualChatter(classification);
            }
        });
    }

    private void handleMaterialRequest(EmergencySpeechClassifier.EmergencyClassification result) {
        binding.tvEvaluationStatus.setText("MATERIAL REQUISITION SENT TO CHAT ROOM");
        binding.tvEvaluationStatus.setTextColor(Color.parseColor("#F5C29B"));
        binding.tvSpokenTranscript.setTextColor(Color.parseColor("#F5C29B"));

        postVoiceMessageToChat("[MATERIAL REQUEST] " + result.rawSpokenText, false);

        Toast.makeText(this, "Material Request sent to Site Chat: \"" + result.rawSpokenText + "\"", Toast.LENGTH_LONG).show();
    }

    private void handleCasualChatter(EmergencySpeechClassifier.EmergencyClassification result) {
        binding.tvEvaluationStatus.setText("Voice Update Dispatched to Site Chat (" + result.modelUsed + ")");
        binding.tvEvaluationStatus.setTextColor(Color.parseColor("#10B981"));
        binding.tvSpokenTranscript.setTextColor(Color.parseColor("#F5C29B"));

        postVoiceMessageToChat("[VOICE NOTE] " + result.rawSpokenText, false);

        Toast.makeText(this, "Message sent to Site Chat Room: \"" + result.rawSpokenText + "\"", Toast.LENGTH_SHORT).show();
    }

    private void triggerEmergencyAlarm(EmergencySpeechClassifier.EmergencyClassification result) {
        isSirenActive = true;
        binding.tvEvaluationStatus.setText("ALERT ACTIVATED: " + result.alertHeadline);
        binding.tvEvaluationStatus.setTextColor(Color.parseColor("#EF4444"));
        binding.tvSpokenTranscript.setTextColor(Color.parseColor("#EF4444"));
        binding.btnSilenceSiren.setVisibility(View.VISIBLE);

        // 1. Play Bold Siren Tone
        startBoldSiren();

        // 2. Heavy SOS Vibration Pattern
        startEmergencyVibration();

        // 3. Post Urgent Message to Site Crew Chat
        postEmergencyToChat(result);

        Toast.makeText(this, "EMERGENCY BROADCAST TRIGGERED: " + result.alertHeadline, Toast.LENGTH_LONG).show();
    }

    private void postVoiceMessageToChat(String messageText, boolean isUrgent) {
        String pCode = sessionManager.getActiveProjectCode();
        if (pCode == null || pCode.isEmpty()) pCode = "PRJ-DHAKA-101";

        User currentUser = sessionManager.getCurrentUser();
        String senderId = currentUser != null ? currentUser.getUsername() : "voice_alerter";
        String senderName = currentUser != null ? currentUser.getFullName() : "Voice Alerter";
        String senderRole = currentUser != null && currentUser.getRole() != null ? currentUser.getRole() : "WORKER";
        String msgId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        SiteChatMessageEntity message = new SiteChatMessageEntity(
                msgId, pCode, senderId, senderName, senderRole, messageText, now, isUrgent
        );
        message.setMediaType("TEXT");

        // 1. Local Room DB Cache
        String finalPCode = pCode;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getInstance(VoiceAlerterDialogActivity.this).siteChatMessageDao().insert(message);
        });

        // 2. Realtime Cloud Sync via Firestore
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("id", msgId);
            data.put("projectCode", finalPCode);
            data.put("senderId", senderId);
            data.put("senderName", senderName);
            data.put("senderRole", senderRole);
            data.put("messageText", messageText);
            data.put("timestamp", now);
            data.put("isUrgent", isUrgent);
            data.put("mediaType", "TEXT");

            FirebaseFirestore.getInstance().collection("projects").document(finalPCode)
                    .collection("chat_messages").document(msgId)
                    .set(data);
        } catch (Exception ignored) {}
    }

    private void startBoldSiren() {
        try {
            if (sirenToneGenerator == null) {
                sirenToneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            }
            sirenRunnable = new Runnable() {
                private boolean high = true;
                @Override
                public void run() {
                    if (!isSirenActive) return;
                    try {
                        sirenToneGenerator.startTone(high ? ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK : ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 400);
                        high = !high;
                    } catch (Exception ignored) {}
                    sirenHandler.postDelayed(this, 500);
                }
            };
            sirenHandler.post(sirenRunnable);
        } catch (Exception ignored) {}
    }

    private void startEmergencyVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = new long[]{0, 500, 200, 500, 200, 500, 300, 800};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // 0 = repeat
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopSiren() {
        isSirenActive = false;
        if (sirenHandler != null && sirenRunnable != null) {
            sirenHandler.removeCallbacks(sirenRunnable);
        }
        if (sirenToneGenerator != null) {
            try {
                sirenToneGenerator.stopTone();
                sirenToneGenerator.release();
            } catch (Exception ignored) {}
            sirenToneGenerator = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void postEmergencyToChat(EmergencySpeechClassifier.EmergencyClassification result) {
        String pCode = sessionManager.getActiveProjectCode();
        if (pCode == null || pCode.isEmpty()) pCode = "PRJ-DHAKA-101";

        User currentUser = sessionManager.getCurrentUser();
        String senderId = currentUser != null ? currentUser.getUsername() : "voice_alerter";
        String senderName = currentUser != null ? currentUser.getFullName() : "Voice Alerter";
        String senderRole = currentUser != null && currentUser.getRole() != null ? currentUser.getRole() : "WORKER";
        String msgId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        String alertText = "[CRITICAL EMERGENCY - " + result.emergencyType + "] Spoken Report: \""
                + result.rawSpokenText + "\" - Immediate site response required!";

        SiteChatMessageEntity message = new SiteChatMessageEntity(
                msgId, pCode, senderId, senderName, senderRole, alertText, now, true
        );
        message.setMediaType("TEXT");

        // 1. Room DB
        String finalPCode = pCode;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getInstance(VoiceAlerterDialogActivity.this).siteChatMessageDao().insert(message);
        });

        // 2. Firestore
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("id", msgId);
            data.put("projectCode", finalPCode);
            data.put("senderId", senderId);
            data.put("senderName", senderName);
            data.put("senderRole", senderRole);
            data.put("messageText", alertText);
            data.put("timestamp", now);
            data.put("isUrgent", true);
            data.put("mediaType", "TEXT");

            FirebaseFirestore.getInstance().collection("projects").document(finalPCode)
                    .collection("chat_messages").document(msgId)
                    .set(data);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSiren();
    }
}
