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
        Toast.makeText(context, "🚨 RESCUE DISPATCHED: Safety Officer & First Aid Notified!", Toast.LENGTH_LONG).show();

        // Queue high-priority safety hazard in Room DB
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
                    "Sector 4 - Active Pour Perimeter",
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
