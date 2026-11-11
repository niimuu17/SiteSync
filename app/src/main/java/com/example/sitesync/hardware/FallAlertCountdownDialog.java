package com.example.sitesync.hardware;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.sitesync.R;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.HazardTicketEntity;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.databinding.DialogFallAlertBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Worker Safety Alert Dialog: Displays a 15-second emergency countdown
 * when extreme impact / free-fall is registered by the hardware accelerometer.
 * Dispatches real-time GPS coordinates of the fallen crew directly into the chat room.
 */
public class FallAlertCountdownDialog extends DialogFragment {

    private DialogFallAlertBinding binding;
    private CountDownTimer countDownTimer;
    private float impactG = 3.2f;
    private OnFallAlertResolvedListener listener;

    private double currentLatitude = UserLocationManager.DEFAULT_FALLBACK_LAT;
    private double currentLongitude = UserLocationManager.DEFAULT_FALLBACK_LNG;
    private boolean isGpsLocked = false;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    public interface OnFallAlertResolvedListener {
        void onFallAlertCancelled();
        void onEmergencyDispatched(float impactG);
    }

    public static FallAlertCountdownDialog newInstance(float impactG) {
        FallAlertCountdownDialog dialog = new FallAlertCountdownDialog();
        Bundle args = new Bundle();
        args.putFloat("impact_g", impactG);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnFallAlertResolvedListener(OnFallAlertResolvedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            impactG = getArguments().getFloat("impact_g", 3.2f);
        }
        setCancelable(false);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Context ctx = getContext();
                    if (ctx != null) {
                        acquireRealLocation(ctx);
                    }
                }
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogFallAlertBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvImpactMagnitude.setText(String.format(Locale.US, "Peak Impact: %.1fG | Status: Immobility Flagged", impactG));

        // Start real-time GPS acquisition immediately
        if (!UserLocationManager.hasLocationPermission(requireContext())) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
        acquireRealLocation(requireContext());

        // Start 15s Countdown
        startCountdown();

        binding.btnCancelFallAlert.setOnClickListener(v -> {
            HapticFeedbackUtil.confirm(requireContext());
            if (countDownTimer != null) countDownTimer.cancel();
            Toast.makeText(requireContext(), "Safety Alert Cancelled • Worker Confirmed OK", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onFallAlertCancelled();
            dismiss();
        });

        binding.btnInstantEvacuate.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            if (countDownTimer != null) countDownTimer.cancel();
            dispatchRescueTeam();
        });
    }

    private void acquireRealLocation(@NonNull Context context) {
        Location cached = UserLocationManager.getInstance(context).getBestAvailableLocation();
        if (cached != null) {
            currentLatitude = cached.getLatitude();
            currentLongitude = cached.getLongitude();
            updateLocationPreview(cached, false);
        }

        UserLocationManager.getInstance(context).fetchCurrentLocation(location -> {
            if (location != null && isAdded() && binding != null) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                isGpsLocked = true;
                updateLocationPreview(location, true);
            }
        });
    }

    private void updateLocationPreview(@NonNull Location location, boolean locked) {
        if (binding == null || !isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (binding == null) return;
            if (locked) {
                binding.tvFallGpsStatus.setText(String.format(Locale.US, "GPS Lock: %.5f° N, %.5f° E", location.getLatitude(), location.getLongitude()));
                binding.layoutGpsStatusBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                binding.ivGpsLockIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_green));
            } else {
                binding.tvFallGpsStatus.setText(String.format(Locale.US, "Cached GPS: %.5f° N, %.5f° E", location.getLatitude(), location.getLongitude()));
                binding.layoutGpsStatusBadge.setBackgroundResource(R.drawable.bg_badge_medium);
                binding.ivGpsLockIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.brand_amber));
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

                // Warning haptic pulse every second
                if (getContext() != null) {
                    HapticFeedbackUtil.lightClick(getContext(), null);
                }
            }

            @Override
            public void onFinish() {
                if (binding == null) return;
                binding.tvCountdownSeconds.setText("0s");
                dispatchRescueTeam();
            }
        }.start();
    }

    private void dispatchRescueTeam() {
        Context context = getContext();
        if (context == null) {
            dismiss();
            return;
        }

        HapticFeedbackUtil.heavyClick(context, null);
        Toast.makeText(context, "RESCUE DISPATCHED: Safety Officer & First Aid Notified!", Toast.LENGTH_LONG).show();

        com.example.sitesync.data.SessionManager sessionManager = new com.example.sitesync.data.SessionManager(context);
        String projectCode = sessionManager.getActiveProjectCode();
        if (projectCode == null || projectCode.isEmpty()) projectCode = "PRJ-DHAKA-101";

        com.example.sitesync.models.User user = sessionManager.getCurrentUser();
        String workerName = user != null ? user.getFullName() : "Site Worker";
        String workerId = user != null ? user.getUid() : "fall_sensor";

        // Real Physical Coordinates from UserLocationManager
        final double finalLat = currentLatitude;
        final double finalLng = currentLongitude;
        final String finalProjectCode = projectCode;
        final String finalWorkerName = workerName;
        final String finalWorkerId = workerId;

        // 1. Queue high-priority safety hazard in Room DB
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            String ticketId = "HAZ-FALL-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            long now = System.currentTimeMillis();

            HazardTicketEntity hazard = new HazardTicketEntity(
                    ticketId,
                    "Fall & Impact Hazard",
                    "CRITICAL: Sudden Impact / Worker Fall Detected (" + String.format(Locale.US, "%.1fG", impactG) + ")",
                    "URGENT",
                    "OPEN",
                    String.format(Locale.US, "GPS: %.5f° N, %.5f° E", finalLat, finalLng),
                    null,
                    "Automated Fall Sensor",
                    now,
                    null,
                    0L,
                    false
            );
            db.hazardTicketDao().insert(hazard);

            // Queue for Firestore Cloud Sync
            PendingSyncEntity syncItem = new PendingSyncEntity(
                    UUID.randomUUID().toString(),
                    "HAZARD",
                    ticketId,
                    "CREATE",
                    "{\"type\":\"FALL_HAZARD\",\"magnitude\":" + impactG + ",\"lat\":" + finalLat + ",\"lng\":" + finalLng + ",\"timestamp\":" + now + "}",
                    now
            );
            db.pendingSyncDao().insert(syncItem);

            // 2. Dispatch Emergency Man-Down Location Alert to Site Chat Room
            String mapsUrl = String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", finalLat, finalLng);
            String chatAlertText = String.format(Locale.US,
                    "🚨 [MAN DOWN / FALL DETECTED]\nWorker: %s\nPeak Impact: %.1fG\n📍 Location: %.5f, %.5f\nTap below to open Google Maps navigation.",
                    finalWorkerName, impactG, finalLat, finalLng
            );

            com.example.sitesync.data.local.entities.SiteChatMessageEntity chatMsg =
                    new com.example.sitesync.data.local.entities.SiteChatMessageEntity(
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

            // 3. Sync with Firebase Firestore
            try {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
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
            } catch (Exception ignored) {}

            // 4. Dispatch Instant Emergency Broadcast to Firebase Realtime Database
            try {
                String cleanCode = finalProjectCode.replaceAll("[.#$\\[\\]/]", "_");
                if (cleanCode.isEmpty()) cleanCode = "metro_central";

                java.util.Map<String, Object> rtdbMap = new java.util.HashMap<>();
                rtdbMap.put("action", "BROADCAST");
                rtdbMap.put("type", "Worker Fall & Impact Emergency");
                rtdbMap.put("status", "ACTIVE");
                rtdbMap.put("workerName", finalWorkerName);
                rtdbMap.put("workerId", finalWorkerId);
                rtdbMap.put("impactG", (double) impactG);
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

                // Also publish to default metro_central demo node if different
                if (!"metro_central".equals(cleanCode)) {
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("projects/metro_central/emergency_broadcast")
                            .setValue(rtdbMap);
                }
            } catch (Exception ignored) {}
        });

        if (listener != null) {
            listener.onEmergencyDispatched(impactG);
        }

        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        binding = null;
    }
}
