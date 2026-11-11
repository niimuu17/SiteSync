package com.example.sitesync.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.HazardTicketEntity;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.data.local.entities.SiteChatMessageEntity;
import com.example.sitesync.databinding.ActivityEmergencyFallAlertBinding;
import com.example.sitesync.hardware.UserLocationManager;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Emergency Fall Alert Activity:
 * High-visibility full-screen emergency modal that executes when a worker fall occurs,
 * EVEN IF the device is locked, in pocket, or SiteSync is running in the background.
 */
public class EmergencyFallAlertActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyFallAlert";
    private ActivityEmergencyFallAlertBinding binding;
    private CountDownTimer countDownTimer;
    private float impactG = 3.5f;

    private double currentLatitude = UserLocationManager.DEFAULT_FALLBACK_LAT;
    private double currentLongitude = UserLocationManager.DEFAULT_FALLBACK_LNG;

    private ToneGenerator sirenToneGenerator;
    private Vibrator vibrator;
    private boolean isSirenActive = false;
    private final Handler sirenHandler = new Handler(Looper.getMainLooper());
    private Runnable sirenRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn on screen and show over keyguard/lockscreen
        setupWakeupAndLockscreenFlags();

        binding = ActivityEmergencyFallAlertBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        impactG = getIntent().getFloatExtra("impact_g", 3.5f);

        setupWorkerMeta();
        acquireLocation();
        startSirenAndVibration();
        startCountdown();

        binding.btnCancelFallAlert.setOnClickListener(v -> {
            cancelAlertAndFinish();
        });

        binding.btnInstantEvacuate.setOnClickListener(v -> {
            dispatchEmergencyAndFinish();
        });
    }

    private void setupWakeupAndLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setupWorkerMeta() {
        SessionManager sessionManager = new SessionManager(this);
        User user = sessionManager.getCurrentUser();
        String name = user != null ? user.getFullName() : "Field Worker";
        String proj = sessionManager.getActiveProjectCode();
        if (proj == null || proj.isEmpty()) proj = "Metro Central Tower";

        binding.tvWorkerInfo.setText(String.format("Worker: %s • %s", name, proj));
        binding.tvImpactMagnitude.setText(String.format(Locale.US, "Peak Impact: %.1fG | Immobility Flagged", impactG));
    }

    private void acquireLocation() {
        Location cached = UserLocationManager.getInstance(this).getBestAvailableLocation();
        if (cached != null) {
            currentLatitude = cached.getLatitude();
            currentLongitude = cached.getLongitude();
            updateLocationDisplay(cached, false);
        }

        UserLocationManager.getInstance(this).fetchCurrentLocation(loc -> {
            if (loc != null && !isFinishing() && binding != null) {
                currentLatitude = loc.getLatitude();
                currentLongitude = loc.getLongitude();
                updateLocationDisplay(loc, true);
            }
        });
    }

    private void updateLocationDisplay(Location loc, boolean locked) {
        runOnUiThread(() -> {
            if (binding == null) return;
            binding.tvFallGpsStatus.setText(String.format(Locale.US, "GPS: %.5f° N, %.5f° E", loc.getLatitude(), loc.getLongitude()));
            if (locked) {
                binding.layoutGpsStatusBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                binding.ivGpsLockIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_green));
                binding.tvFallGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green));
            } else {
                binding.layoutGpsStatusBadge.setBackgroundResource(R.drawable.bg_badge_medium);
                binding.ivGpsLockIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_amber));
                binding.tvFallGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_amber));
            }
        });
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) return;
                int seconds = (int) (millisUntilFinished / 1000);
                binding.tvCountdownSeconds.setText(seconds + "s");
            }

            @Override
            public void onFinish() {
                if (binding == null) return;
                binding.tvCountdownSeconds.setText("0s");
                dispatchEmergencyAndFinish();
            }
        }.start();
    }

    private void startSirenAndVibration() {
        isSirenActive = true;
        try {
            sirenToneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            sirenRunnable = new Runnable() {
                private boolean highTone = true;
                @Override
                public void run() {
                    if (!isSirenActive) return;
                    try {
                        sirenToneGenerator.startTone(highTone ? ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK : ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 350);
                        highTone = !highTone;
                    } catch (Exception ignored) {}
                    sirenHandler.postDelayed(this, 450);
                }
            };
            sirenHandler.post(sirenRunnable);
        } catch (Exception e) {
            Log.w(TAG, "ToneGenerator error: " + e.getMessage());
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = new long[]{0, 500, 200, 500, 200, 500, 300, 800};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopSirenAndVibration() {
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

    private void cancelAlertAndFinish() {
        stopSirenAndVibration();
        if (countDownTimer != null) countDownTimer.cancel();
        Toast.makeText(this, "Fall Alert Cancelled • Worker Confirmed OK", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void dispatchEmergencyAndFinish() {
        stopSirenAndVibration();
        if (countDownTimer != null) countDownTimer.cancel();

        Toast.makeText(this, "🚨 RESCUE DISPATCHED: Safety Officer & Site Crew Notified!", Toast.LENGTH_LONG).show();

        SessionManager sessionManager = new SessionManager(this);
        String projectCode = sessionManager.getActiveProjectCode();
        if (projectCode == null || projectCode.isEmpty()) projectCode = "PRJ-DHAKA-101";

        User user = sessionManager.getCurrentUser();
        String workerName = user != null ? user.getFullName() : "Field Worker";
        String workerId = user != null ? user.getUid() : "worker_fall";

        final double finalLat = currentLatitude;
        final double finalLng = currentLongitude;
        final String finalProjectCode = projectCode;
        final String finalWorkerName = workerName;
        final String finalWorkerId = workerId;
        final float finalImpactG = impactG;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(EmergencyFallAlertActivity.this);
            String ticketId = "HAZ-FALL-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            long now = System.currentTimeMillis();

            // 1. Room DB Hazard Ticket
            HazardTicketEntity hazard = new HazardTicketEntity(
                    ticketId,
                    "Fall & Impact Hazard (Pocket / Background)",
                    "CRITICAL: Sudden Impact / Worker Fall Detected (" + String.format(Locale.US, "%.1fG", finalImpactG) + ")",
                    "URGENT",
                    "OPEN",
                    String.format(Locale.US, "GPS: %.5f° N, %.5f° E", finalLat, finalLng),
                    null,
                    "Automated Fall Guardian Service",
                    now,
                    null,
                    0L,
                    false
            );
            db.hazardTicketDao().insert(hazard);

            // 2. Room DB Chat Message with Maps Link
            String mapsUrl = String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", finalLat, finalLng);
            String chatAlertText = String.format(Locale.US,
                    "🚨 [MAN DOWN / FALL DETECTED]\nWorker: %s\nPeak Impact: %.1fG\n📍 Location: %.5f, %.5f\nTap below to open Google Maps navigation.",
                    finalWorkerName, finalImpactG, finalLat, finalLng
            );

            SiteChatMessageEntity chatMsg = new SiteChatMessageEntity(
                    UUID.randomUUID().toString(),
                    finalProjectCode,
                    finalWorkerId,
                    finalWorkerName + " (FALL SENSOR)",
                    "SAFETY_SENSOR",
                    chatAlertText,
                    now,
                    true
            );
            chatMsg.setMediaType("LOCATION");
            chatMsg.setMediaUrl(mapsUrl);
            db.siteChatMessageDao().insert(chatMsg);

            // 3. Firestore Sync (Both root collection and project subcollection)
            try {
                String cleanCode = finalProjectCode.replaceAll("[.#$\\[\\]/]", "_");
                if (cleanCode.isEmpty()) cleanCode = "metro_central";

                Map<String, Object> map = new HashMap<>();
                map.put("id", chatMsg.getId());
                map.put("projectCode", finalProjectCode);
                map.put("senderId", finalWorkerId);
                map.put("senderName", finalWorkerName + " (FALL SENSOR)");
                map.put("senderRole", "SAFETY_SENSOR");
                map.put("messageText", chatAlertText);
                map.put("timestamp", now);
                map.put("isUrgent", true);
                map.put("mediaType", "LOCATION");
                map.put("mediaUrl", mapsUrl);

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("site_chat_messages")
                        .document(chatMsg.getId())
                        .set(map);

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("projects")
                        .document(cleanCode)
                        .collection("chat_messages")
                        .document(chatMsg.getId())
                        .set(map);

                Log.i(TAG, "Dispatched fall emergency chat to Firestore.");
            } catch (Exception e) {
                Log.e(TAG, "Firestore sync error", e);
            }

            // 4. Firebase Realtime Database Broadcast
            try {
                String cleanCode = finalProjectCode.replaceAll("[.#$\\[\\]/]", "_");
                if (cleanCode.isEmpty()) cleanCode = "metro_central";

                Map<String, Object> rtdbMap = new HashMap<>();
                rtdbMap.put("action", "BROADCAST");
                rtdbMap.put("type", "Worker Fall & Impact Emergency");
                rtdbMap.put("status", "ACTIVE");
                rtdbMap.put("workerName", finalWorkerName);
                rtdbMap.put("workerId", finalWorkerId);
                rtdbMap.put("impactG", (double) finalImpactG);
                rtdbMap.put("latitude", finalLat);
                rtdbMap.put("longitude", finalLng);
                rtdbMap.put("mapsUrl", mapsUrl);
                rtdbMap.put("timestamp", now);
                rtdbMap.put("updatedAt", now);

                com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("projects")
                        .child(cleanCode)
                        .child("emergency_broadcast")
                        .setValue(rtdbMap);

                if (!"metro_central".equals(cleanCode)) {
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("projects/metro_central/emergency_broadcast")
                            .setValue(rtdbMap);
                }

                Log.i(TAG, "Dispatched fall emergency broadcast to RTDB.");
            } catch (Exception e) {
                Log.e(TAG, "RTDB broadcast error", e);
            }
        });

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSirenAndVibration();
        if (countDownTimer != null) countDownTimer.cancel();
        binding = null;
    }
}
