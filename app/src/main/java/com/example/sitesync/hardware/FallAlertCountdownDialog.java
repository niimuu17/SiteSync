package com.example.sitesync.hardware;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

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
 */
public class FallAlertCountdownDialog extends DialogFragment {

    private DialogFallAlertBinding binding;
    private CountDownTimer countDownTimer;
    private float impactG = 3.2f;
    private OnFallAlertResolvedListener listener;

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

        // Query best available GPS coordinates (or fallback to Metro Central Tower location)
        double lat = 23.8103;
        double lng = 90.4125;
        try {
            android.location.LocationManager lm = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.location.Location loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                if (loc == null) {
                    loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                }
                if (loc != null) {
                    lat = loc.getLatitude();
                    lng = loc.getLongitude();
                }
            }
        } catch (Exception ignored) {}

        final double finalLat = lat;
        final double finalLng = lng;
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
                    String.format(Locale.US, "GPS: %.4f° N, %.4f° E", finalLat, finalLng),
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
                    "{\"type\":\"FALL_HAZARD\",\"magnitude\":" + impactG + ",\"timestamp\":" + now + "}",
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

            // Sync with Firebase Firestore
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
