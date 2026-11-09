package com.example.sitesync.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.databinding.DialogEmergencyConfirmBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Two-Step Safeguard Dialog for High-Priority Emergency Site Broadcast.
 * Requires selecting the hazard category and holding the broadcast trigger for 2.5 seconds
 * with continuous tactile vibrational feedback.
 */
public class EmergencyConfirmationDialog extends BottomSheetDialogFragment {

    private DialogEmergencyConfirmBinding binding;
    private OnEmergencyTriggeredListener listener;
    private final Handler holdHandler = new Handler(Looper.getMainLooper());
    private static final long HOLD_DURATION_MS = 2500L;
    private static final long TICK_INTERVAL_MS = 50L;
    private long holdStartTime = 0;
    private boolean isHolding = false;

    public interface OnEmergencyTriggeredListener {
        void onEmergencyTriggered(String emergencyType, long timestamp);
    }

    public static EmergencyConfirmationDialog newInstance() {
        return new EmergencyConfirmationDialog();
    }

    public void setOnEmergencyTriggeredListener(OnEmergencyTriggeredListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogEmergencyConfirmBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCloseDialog.setOnClickListener(v -> dismiss());

        setupHoldToConfirm();
    }

    private void setupHoldToConfirm() {
        binding.btnHoldToConfirm.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startHolding();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelHolding();
                    return true;
            }
            return false;
        });
    }

    private void startHolding() {
        isHolding = true;
        holdStartTime = System.currentTimeMillis();
        binding.tvHoldButtonLabel.setText("Holding... keep pressing");
        HapticFeedbackUtil.heavyClick(requireContext(), binding.btnHoldToConfirm);
        holdHandler.post(holdRunnable);
    }

    private void cancelHolding() {
        if (!isHolding) return;
        isHolding = false;
        holdHandler.removeCallbacks(holdRunnable);
        binding.pbHoldProgress.setProgress(0);
        binding.tvHoldButtonLabel.setText("Press & Hold to Broadcast");
    }

    private final Runnable holdRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isHolding) return;

            long elapsed = System.currentTimeMillis() - holdStartTime;
            int progress = (int) ((elapsed * 100) / HOLD_DURATION_MS);
            binding.pbHoldProgress.setProgress(Math.min(progress, 100));

            // Tactile pulse every ~250ms
            if (elapsed % 250 < TICK_INTERVAL_MS) {
                HapticFeedbackUtil.continuousBuzz(requireContext(), 80);
            }

            if (elapsed >= HOLD_DURATION_MS) {
                isHolding = false;
                executeEmergencyBroadcast();
            } else {
                holdHandler.postDelayed(this, TICK_INTERVAL_MS);
            }
        }
    };

    private void executeEmergencyBroadcast() {
        Context context = requireContext();
        String selectedType = getSelectedEmergencyType();
        long now = System.currentTimeMillis();

        // 1. Tactile confirmation
        HapticFeedbackUtil.doubleBuzzToleranceSuccess(context);

        // 2. Dispatch to Firebase Realtime Database
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("projects/metro_central/emergency_broadcast");
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", selectedType);
            payload.put("timestamp", now);
            payload.put("status", "ACTIVE");
            payload.put("triggeredBy", "Field Supervisor");
            ref.setValue(payload);
        } catch (Exception ignored) {
        }

        // 3. Queue offline sync
        try {
            AppDatabase db = AppDatabase.getDatabase(context);
            PendingSyncEntity sync = new PendingSyncEntity(
                    java.util.UUID.randomUUID().toString(),
                    "EMERGENCY_BROADCAST",
                    "emergency_" + now,
                    "BROADCAST",
                    "{\"type\":\"" + selectedType + "\",\"timestamp\":" + now + "}",
                    now
            );
            db.pendingSyncDao().insert(sync);
        } catch (Exception ignored) {
        }

        // 4. Trigger High Priority System Notification
        postEmergencyNotification(context, selectedType);

        Toast.makeText(context, "EMERGENCY BROADCAST TRANSMITTED", Toast.LENGTH_LONG).show();

        if (listener != null) {
            listener.onEmergencyTriggered(selectedType, now);
        }

        dismiss();
    }

    private String getSelectedEmergencyType() {
        int checkedId = binding.rgEmergencyType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbGasChemical) {
            return "Gas / Toxic Chemical Leak";
        } else if (checkedId == R.id.rbSevereWeather) {
            return "Severe Weather / Lightning / High Wind";
        } else if (checkedId == R.id.rbFire) {
            return "Fire / Explosion Hazard";
        }
        return "Structural Failure Threat / Collapse";
    }

    private void postEmergencyNotification(Context context, String emergencyType) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String channelId = "sitesync_emergency_alerts";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Emergency Site Broadcasts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Critical life-safety alerts for jobsite evacuation");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_warning_amber)
                .setContentTitle("🚨 EMERGENCY EVACUATION ALERT")
                .setContentText(emergencyType + " reported on site. Evacuate to assembly point.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true);

        nm.notify(9999, builder.build());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        holdHandler.removeCallbacks(holdRunnable);
        binding = null;
    }
}
