package com.example.sitesync.ui.common;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.hardware.BarometerElevationManager;
import com.example.sitesync.utils.HapticFeedbackUtil;

/**
 * Sticky ContextBannerView with Automatic Floor Detector telemetry.
 * Displays project location, active floor level with blue elevator icon,
 * and allows instant manual override for field engineers.
 */
public class ContextBannerView extends FrameLayout {

    public interface OnFloorSelectedListener {
        void onFloorSelected(int floorLevel, boolean isManual);
    }

    private TextView tvLocationTitle;
    private TextView tvFloorLevel;
    private ImageView ivElevatorIcon;
    private ImageView ivChevron;

    private String projectName = "Metro Central Tower";
    private int currentFloor = 3;
    private boolean isManualOverride = false;
    private BarometerElevationManager barometerManager;
    private OnFloorSelectedListener floorListener;

    public ContextBannerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ContextBannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ContextBannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // High-contrast Warm Ash Outlined Card Surface
        setBackgroundResource(R.drawable.bg_card_outlined);
        setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(lp);

        // 4-Square Building Icon (Oxide Blue)
        ivElevatorIcon = new ImageView(context);
        ivElevatorIcon.setImageResource(R.drawable.ic_grid);
        ivElevatorIcon.setColorFilter(ContextCompat.getColor(context, R.color.oxide_blue_primary));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        iconLp.setMarginEnd(dpToPx(12));
        ivElevatorIcon.setLayoutParams(iconLp);
        container.addView(ivElevatorIcon);

        // Text Group (Location + Floor)
        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
        textGroup.setLayoutParams(textLp);

        tvLocationTitle = new TextView(context);
        tvLocationTitle.setText(projectName);
        tvLocationTitle.setTextColor(ContextCompat.getColor(context, R.color.text_oxide_primary));
        tvLocationTitle.setTextSize(14f);
        tvLocationTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textGroup.addView(tvLocationTitle);

        tvFloorLevel = new TextView(context);
        tvFloorLevel.setText("Floor Level: Level " + currentFloor + " (Auto)");
        tvFloorLevel.setTextColor(ContextCompat.getColor(context, R.color.text_oxide_secondary));
        tvFloorLevel.setTextSize(12f);
        textGroup.addView(tvFloorLevel);

        container.addView(textGroup);

        // Angled action trigger arrow (Oxide Blue)
        ivChevron = new ImageView(context);
        ivChevron.setImageResource(R.drawable.ic_arrow_diagonal);
        ivChevron.setColorFilter(ContextCompat.getColor(context, R.color.oxide_blue_primary));
        LinearLayout.LayoutParams chevLp = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
        ivChevron.setLayoutParams(chevLp);
        container.addView(ivChevron);

        addView(container);

        setOnClickListener(v -> showFloorOverrideDialog(context));
    }

    public void setProjectName(String name) {
        this.projectName = name != null ? name : "Metro Central Tower";
        if (tvLocationTitle != null) {
            tvLocationTitle.setText(this.projectName);
        }
    }

    public void setFloorLevel(int floor, boolean manual) {
        this.currentFloor = floor;
        this.isManualOverride = manual;
        if (tvFloorLevel != null) {
            String suffix = manual ? " (Manual Lock)" : " (Auto)";
            tvFloorLevel.setText("Floor Level: Level " + floor + suffix);
        }
    }

    public void attachBarometerManager(BarometerElevationManager manager, OnFloorSelectedListener listener) {
        this.barometerManager = manager;
        this.floorListener = listener;

        if (manager != null) {
            manager.startListening(new BarometerElevationManager.OnFloorLevelChangeListener() {
                @Override
                public void onFloorChanged(int computedFloor, float elevationDeltaMeters, float currentPressureHpa) {
                    setFloorLevel(computedFloor, false);
                    if (floorListener != null) {
                        floorListener.onFloorSelected(computedFloor, false);
                    }
                }

                @Override
                public void onRawElevationUpdate(float elevationDeltaMeters, float pressureHpa) {
                    // Raw pressure telemetry
                }
            });
        }
    }

    private void showFloorOverrideDialog(Context context) {
        HapticFeedbackUtil.heavyClick(context, this);

        String[] floors = new String[]{
                "Auto Floor Detector (Barometer)",
                "Basement -2 (Sub-Structure)",
                "Basement -1 (Foundation)",
                "Ground Level (Level 1)",
                "Floor Level: Level 2",
                "Floor Level: Level 3",
                "Floor Level: Level 4",
                "Floor Level: Level 5",
                "Floor Level: Level 6",
                "Floor Level: Level 14 (Tower Core)",
                "Roof Level (Mechanical Deck)"
        };

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Select Floor Level")
                .setItems(floors, (dialog, which) -> {
                    if (which == 0) {
                        isManualOverride = false;
                        if (barometerManager != null) {
                            barometerManager.setManualFloorOverride(false, 1);
                        }
                        setFloorLevel(currentFloor, false);
                    } else {
                        isManualOverride = true;
                        int chosenFloor = which - 2; // maps to -1, 0, 1, 2...
                        if (chosenFloor <= 0) chosenFloor = Math.max(1, which);
                        if (which >= 3) chosenFloor = which - 2;
                        if (which == 9) chosenFloor = 14;

                        setFloorLevel(chosenFloor, true);
                        if (barometerManager != null) {
                            barometerManager.setManualFloorOverride(true, chosenFloor);
                        }
                        if (floorListener != null) {
                            floorListener.onFloorSelected(chosenFloor, true);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
