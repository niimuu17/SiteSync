package com.example.sitesync.hardware;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.sitesync.R;
import com.example.sitesync.databinding.DialogCheckLevelBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.Locale;

/**
 * Feature 2: "Check Level" Quick Slope & Plumb Gauge Dialog
 * Interacts in real time with TiltInclinometerManager.
 * Emits double-buzz haptic confirmation when within tolerance, turning the gauge solid safety green.
 * "Save Measurement" passes the captured angle back to the caller.
 */
public class CheckLevelDialog extends DialogFragment {

    public interface OnMeasurementSavedListener {
        void onMeasurementSaved(String formattedMeasurement);
    }

    private TiltInclinometerManager inclinometerManager;
    private OnMeasurementSavedListener savedListener;
    private DialogCheckLevelBinding binding;

    private float latestPitch = 0f;
    private float latestRoll = 0f;
    private float latestSlope = 0f;
    private boolean isWithinTolerance = false;

    public static CheckLevelDialog newInstance() {
        return new CheckLevelDialog();
    }

    public void setOnMeasurementSavedListener(OnMeasurementSavedListener listener) {
        this.savedListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogCheckLevelBinding.inflate(LayoutInflater.from(requireContext()));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setupInclinometer();

        binding.btnSaveMeasurement.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            String measurement = String.format(Locale.US, "Pitch: %.1f° | Roll: %.1f° | Grade: %.1f%% (%s)",
                    latestPitch, latestRoll, latestSlope, isWithinTolerance ? "PASS" : "OUT OF TOLERANCE");
            Toast.makeText(requireContext(), "Measurement saved: " + measurement, Toast.LENGTH_SHORT).show();
            dismiss();
            if (savedListener != null) {
                savedListener.onMeasurementSaved(measurement);
            }
        });

        binding.btnCloseCheckLevel.setOnClickListener(v -> dismiss());

        return dialog;
    }

    private void setupInclinometer() {
        inclinometerManager = new TiltInclinometerManager(requireContext());

        inclinometerManager.startListening((pitch, roll, slope, isLevel, isPipeGrade) -> {
            if (binding == null) return;

            latestPitch = pitch;
            latestRoll = roll;
            latestSlope = slope;
            isWithinTolerance = isLevel || isPipeGrade;

            binding.tvPitchValue.setText(String.format(Locale.US, "%.1f°", pitch));
            binding.tvRollValue.setText(String.format(Locale.US, "%.1f°", roll));
            binding.tvGradeValue.setText(String.format(Locale.US, "%.1f%%", slope));

            // Map angle to progress: 0° is 100% level, 45° is 0%
            int progress = Math.max(0, 100 - (int) (Math.abs(pitch) * 2.2f));
            binding.gaugeLevel.setProgress(progress);

            if (isLevel) {
                binding.gaugeLevel.setSweepColor(ContextCompat.getColor(requireContext(), R.color.status_green));
                binding.gaugeLevel.setTrendValue("TRUE LEVEL ▲");
                binding.tvLevelStatus.setText("TRUE LEVEL STABILIZED (0.0° ± 1.0°)");
                binding.tvLevelStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
            } else if (isPipeGrade) {
                binding.gaugeLevel.setSweepColor(ContextCompat.getColor(requireContext(), R.color.status_green));
                binding.gaugeLevel.setTrendValue("PIPE GRADE ▲");
                binding.tvLevelStatus.setText("PIPE GRADE VERIFIED (2.0% ± 0.2%)");
                binding.tvLevelStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
            } else {
                binding.gaugeLevel.setSweepColor(ContextCompat.getColor(requireContext(), R.color.oxide_blue_light));
                binding.gaugeLevel.setTrendValue(String.format(Locale.US, "%+.1f°", pitch));
                binding.tvLevelStatus.setText("Adjust surface angle to reach tolerance");
                binding.tvLevelStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_secondary));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (inclinometerManager != null) {
            inclinometerManager.stopListening();
        }
        binding = null;
    }
}
