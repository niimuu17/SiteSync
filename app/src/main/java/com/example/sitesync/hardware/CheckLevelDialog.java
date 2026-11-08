package com.example.sitesync.hardware;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.sitesync.R;
import com.example.sitesync.ui.custom.SemiCircleGaugeView;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.button.MaterialButton;

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

    private SemiCircleGaugeView gaugeLevel;
    private TextView tvLevelStatus;
    private TextView tvPitchValue;
    private TextView tvRollValue;
    private TextView tvGradeValue;
    private MaterialButton btnSaveMeasurement;
    private ImageButton btnClose;

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
        return inflater.inflate(R.layout.dialog_check_level, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gaugeLevel = view.findViewById(R.id.gaugeLevel);
        tvLevelStatus = view.findViewById(R.id.tvLevelStatus);
        tvPitchValue = view.findViewById(R.id.tvPitchValue);
        tvRollValue = view.findViewById(R.id.tvRollValue);
        tvGradeValue = view.findViewById(R.id.tvGradeValue);
        btnSaveMeasurement = view.findViewById(R.id.btnSaveMeasurement);
        btnClose = view.findViewById(R.id.btnCloseCheckLevel);

        inclinometerManager = new TiltInclinometerManager(requireContext());

        inclinometerManager.startListening((pitch, roll, slope, isLevel, isPipeGrade) -> {
            latestPitch = pitch;
            latestRoll = roll;
            latestSlope = slope;
            isWithinTolerance = isLevel || isPipeGrade;

            tvPitchValue.setText(String.format(Locale.US, "%.1f°", pitch));
            tvRollValue.setText(String.format(Locale.US, "%.1f°", roll));
            tvGradeValue.setText(String.format(Locale.US, "%.1f%%", slope));

            // Map angle to progress: 0° is 100% level, 45° is 0%
            int progress = Math.max(0, 100 - (int) (Math.abs(pitch) * 2.2f));
            gaugeLevel.setProgress(progress);

            if (isLevel) {
                gaugeLevel.setSweepColor(ContextCompat.getColor(requireContext(), R.color.oxide_blue_primary));
                gaugeLevel.setTrendValue("TRUE LEVEL ▲");
                tvLevelStatus.setText("TRUE LEVEL VERIFIED (0.0° ± 0.3°)");
                tvLevelStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.oxide_blue_primary));
            } else if (isPipeGrade) {
                gaugeLevel.setSweepColor(ContextCompat.getColor(requireContext(), R.color.oxide_blue_primary));
                gaugeLevel.setTrendValue("PIPE GRADE ▲");
                tvLevelStatus.setText("PIPE GRADE VERIFIED (2.0% ± 0.2%)");
                tvLevelStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.oxide_blue_primary));
            } else {
                gaugeLevel.setSweepColor(ContextCompat.getColor(requireContext(), R.color.oxide_blue_light));
                gaugeLevel.setTrendValue(String.format(Locale.US, "%+.1f°", pitch));
                tvLevelStatus.setText("Adjust surface angle to reach tolerance");
                tvLevelStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_secondary));
            }
        });

        btnSaveMeasurement.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            String measurement = String.format(Locale.US, "Pitch: %.1f° | Roll: %.1f° | Grade: %.1f%% (%s)",
                    latestPitch, latestRoll, latestSlope, isWithinTolerance ? "PASS" : "OUT OF TOLERANCE");
            Toast.makeText(requireContext(), "Measurement saved: " + measurement, Toast.LENGTH_SHORT).show();
            dismiss();
            if (savedListener != null) {
                savedListener.onMeasurementSaved(measurement);
            }
        });

        btnClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (inclinometerManager != null) {
            inclinometerManager.stopListening();
        }
    }
}
