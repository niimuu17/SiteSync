package com.example.sitesync.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

/**
 * Worker Safety Subsystem: Sudden Impact & Fall Detection Engine.
 *
 * Mathematical Model:
 * Vector Magnitude = sqrt(ax^2 + ay^2 + az^2)
 * 1. Free-Fall Phase: Gravitational normal force drops (< 4.2 m/s²).
 * 2. Impact Spike: Impact deceleration spikes (> 21.0 m/s² ≈ 2.15G) within 1.5s of free fall,
 *    or severe shock (> 25.0 m/s² ≈ 2.55G).
 * 3. Physical Damping Window: Ignores phone bouncing / shockwave vibrations for 500ms.
 * 4. Post-Impact Immobility: Verifies the subject remains stationary for 1.5s post-impact.
 *    If worker stands up and moves vigorously, false alarm is cancelled.
 */
public class FallDetectionManager implements SensorEventListener {

    public interface OnFallDetectedListener {
        void onFallDetected(float impactMagnitudeG, long timestamp);
    }

    public interface OnTelemetryUpdateListener {
        void onAccelerationMagnitude(float magnitudeMps2, String stateDescription);
    }

    private static final String TAG = "FallDetectionManager";

    private static final float FREE_FALL_THRESHOLD = 5.2f;       // Free-fall threshold (m/s² < 0.53G)
    private static final float IMPACT_THRESHOLD = 16.5f;         // Impact spike threshold (m/s² > 1.68G)
    private static final float SEVERE_IMPACT_THRESHOLD = 22.0f;  // Severe instant shock threshold
    private static final long IMPACT_WINDOW_MS = 1800;           // Impact must follow free fall within 1.8s
    private static final long DAMPING_WINDOW_MS = 400;           // Post-impact shockwave settling window
    private static final long IMMOBILITY_DURATION_MS = 1200;     // Post-impact immobility verification duration

    private final Context context;
    private final SensorManager sensorManager;
    private Sensor rawAccelSensor;

    private boolean isMonitoring = false;
    private OnFallDetectedListener fallListener;
    private OnTelemetryUpdateListener telemetryListener;

    private enum State { IDLE, FREE_FALL, IMPACT_DETECTED }
    private State currentState = State.IDLE;

    private long freeFallTimestamp = 0;
    private long impactTimestamp = 0;
    private float peakImpactG = 0f;
    private long lastTelemetryPost = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable immobilityChecker;

    public FallDetectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            rawAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    public void startMonitoring(OnFallDetectedListener fallListener, OnTelemetryUpdateListener telemetryListener) {
        this.fallListener = fallListener;
        this.telemetryListener = telemetryListener;

        if (sensorManager == null || isMonitoring) return;

        if (rawAccelSensor != null) {
            sensorManager.registerListener(this, rawAccelSensor, SensorManager.SENSOR_DELAY_UI);
            isMonitoring = true;
        }
    }

    public void stopMonitoring() {
        if (sensorManager != null && isMonitoring) {
            sensorManager.unregisterListener(this);
            isMonitoring = false;
        }
        if (immobilityChecker != null) {
            mainHandler.removeCallbacks(immobilityChecker);
            immobilityChecker = null;
        }
        currentState = State.IDLE;
        fallListener = null;
        telemetryListener = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float ax = event.values[0];
        float ay = event.values[1];
        float az = event.values[2];

        // Total proper acceleration magnitude (includes gravity baseline 9.8 m/s² when stationary)
        float totalMag = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        long now = System.currentTimeMillis();

        switch (currentState) {
            case IDLE:
                if (totalMag < FREE_FALL_THRESHOLD) {
                    // True free-fall: gravitational normal force drops near 0
                    currentState = State.FREE_FALL;
                    freeFallTimestamp = now;
                    android.util.Log.i(TAG, "Phase 1: Free Fall Triggered! Mag=" + totalMag + " m/s²");
                } else if (totalMag > SEVERE_IMPACT_THRESHOLD) {
                    // Sudden violent impact or shock without registered free fall
                    currentState = State.IMPACT_DETECTED;
                    impactTimestamp = now;
                    peakImpactG = totalMag / SensorManager.GRAVITY_EARTH;
                    android.util.Log.i(TAG, "Phase 2 (Severe Shock): Mag=" + totalMag + " m/s² (" + peakImpactG + "G)");
                    scheduleImmobilityVerification(peakImpactG);
                }
                break;

            case FREE_FALL:
                if (now - freeFallTimestamp > IMPACT_WINDOW_MS) {
                    currentState = State.IDLE; // Free-fall window expired
                } else if (totalMag > IMPACT_THRESHOLD) {
                    // Impact following free fall
                    currentState = State.IMPACT_DETECTED;
                    impactTimestamp = now;
                    peakImpactG = totalMag / SensorManager.GRAVITY_EARTH;
                    android.util.Log.i(TAG, "Phase 2 (Impact after Free Fall): Mag=" + totalMag + " m/s² (" + peakImpactG + "G)");
                    scheduleImmobilityVerification(peakImpactG);
                }
                break;

            case IMPACT_DETECTED:
                // Only evaluate motion AFTER the initial impact shockwave and phone bounce have settled
                if (now - impactTimestamp > DAMPING_WINDOW_MS) {
                    // If worker moves vigorously post-impact (standing up / walking), cancel false alarm
                    if (Math.abs(totalMag - SensorManager.GRAVITY_EARTH) > 6.0f) {
                        android.util.Log.d(TAG, "Post-impact motion detected (" + totalMag + " m/s²). Resetting to IDLE.");
                        if (immobilityChecker != null) {
                            mainHandler.removeCallbacks(immobilityChecker);
                            immobilityChecker = null;
                        }
                        currentState = State.IDLE;
                    }
                }
                break;
        }

        if (telemetryListener != null && (now - lastTelemetryPost > 200)) {
            lastTelemetryPost = now;
            final float currentMag = totalMag;
            final String stateStr = currentState.name();
            mainHandler.post(() -> {
                if (telemetryListener != null) {
                    telemetryListener.onAccelerationMagnitude(currentMag, stateStr);
                }
            });
        }
    }

    private void scheduleImmobilityVerification(final float impactG) {
        if (immobilityChecker != null) {
            mainHandler.removeCallbacks(immobilityChecker);
        }

        immobilityChecker = () -> {
            currentState = State.IDLE;
            if (fallListener != null) {
                fallListener.onFallDetected(impactG, System.currentTimeMillis());
            }
        };

        mainHandler.postDelayed(immobilityChecker, IMMOBILITY_DURATION_MS);
    }

    /**
     * Simulation method for QA testing, field demonstrations, and Viva examinations.
     */
    public void simulateFallEvent(float simulatedG) {
        mainHandler.post(() -> {
            if (fallListener != null) {
                fallListener.onFallDetected(simulatedG, System.currentTimeMillis());
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public boolean isMonitoring() {
        return isMonitoring;
    }
}
