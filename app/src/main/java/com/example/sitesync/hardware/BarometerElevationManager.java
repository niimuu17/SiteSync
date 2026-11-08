package com.example.sitesync.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Feature 4: Automatic Floor Detector (Barometer Sensor)
 * Uses Sensor.TYPE_PRESSURE and hypsometric altimetry:
 * h = 44330 * (1 - (P / P0)^(1 / 5.255))
 * Floor Level = round((h - h_ground) / 3.3m)
 * Automatically notifies when stable vertical shift (>3.0m for >10s) is confirmed.
 */
public class BarometerElevationManager implements SensorEventListener {

    private static final String TAG = "BarometerManager";
    private static final float FLOOR_HEIGHT_METERS = 3.3f;
    private static final float STABLE_SHIFT_METERS = 3.0f;
    private static final long STABILITY_WINDOW_MS = 10000; // 10 seconds

    public interface OnFloorLevelChangeListener {
        void onFloorChanged(int computedFloor, float elevationDeltaMeters, float currentPressureHpa);
        void onRawElevationUpdate(float elevationDeltaMeters, float pressureHpa);
    }

    private final Context context;
    private final SensorManager sensorManager;
    private Sensor pressureSensor;

    private float baselinePressureHpa = 1013.25f; // P0 ground baseline
    private boolean isBaselineSet = false;
    private boolean isRegistered = false;

    private int activeFloor = 1;
    private int candidateFloor = 1;
    private long candidateStableStartTime = 0;
    private boolean isManualOverride = false;
    private int manualFloor = 1;

    private OnFloorLevelChangeListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public BarometerElevationManager(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        }
    }

    public void startListening(OnFloorLevelChangeListener listener) {
        this.listener = listener;
        if (sensorManager == null || pressureSensor == null || isRegistered) return;

        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isRegistered = true;
    }

    public void stopListening() {
        if (sensorManager != null && isRegistered) {
            sensorManager.unregisterListener(this);
            isRegistered = false;
        }
    }

    public void calibrateGroundZero(float groundPressureHpa) {
        this.baselinePressureHpa = groundPressureHpa;
        this.isBaselineSet = true;
        Log.d(TAG, "Calibrated ground zero P0 = " + groundPressureHpa + " hPa");
    }

    public void setManualFloorOverride(boolean override, int floor) {
        this.isManualOverride = override;
        this.manualFloor = floor;
        if (override && listener != null) {
            listener.onFloorChanged(floor, (floor - 1) * FLOOR_HEIGHT_METERS, baselinePressureHpa);
        }
    }

    public boolean isManualOverride() {
        return isManualOverride;
    }

    public int getActiveFloor() {
        return isManualOverride ? manualFloor : activeFloor;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_PRESSURE) return;

        float currentPressure = event.values[0];

        if (!isBaselineSet) {
            baselinePressureHpa = currentPressure;
            isBaselineSet = true;
        }

        // Hypsometric formula: h = 44330 * (1 - (P / P0)^(1 / 5.255))
        double pressureRatio = currentPressure / baselinePressureHpa;
        float elevationDeltaMeters = (float) (44330.0 * (1.0 - Math.pow(pressureRatio, (1.0 / 5.255))));

        // Discrete floor index: round(h / 3.3) + 1 (Base Floor = 1)
        int computedFloorIndex = Math.max(1, Math.round(elevationDeltaMeters / FLOOR_HEIGHT_METERS) + 1);

        mainHandler.post(() -> {
            if (listener != null) {
                listener.onRawElevationUpdate(elevationDeltaMeters, currentPressure);
            }
        });

        if (isManualOverride) return;

        long now = System.currentTimeMillis();

        if (computedFloorIndex != activeFloor) {
            if (computedFloorIndex != candidateFloor) {
                candidateFloor = computedFloorIndex;
                candidateStableStartTime = now;
            } else {
                // Check if candidate shift has been stable for > 10 seconds (> 3.0m shift)
                float shiftMagnitude = Math.abs((candidateFloor - activeFloor) * FLOOR_HEIGHT_METERS);
                if (shiftMagnitude >= STABLE_SHIFT_METERS && (now - candidateStableStartTime >= STABILITY_WINDOW_MS)) {
                    activeFloor = candidateFloor;
                    Log.d(TAG, "Stable floor shift confirmed: Level " + activeFloor + " (" + elevationDeltaMeters + "m)");

                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onFloorChanged(activeFloor, elevationDeltaMeters, currentPressure);
                        }
                    });
                }
            }
        } else {
            candidateFloor = activeFloor;
            candidateStableStartTime = now;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public boolean isAvailable() {
        return pressureSensor != null;
    }
}
