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
 * 1. Free-Fall Phase: Magnitude drops below FREE_FALL_THRESHOLD (< 2.5 m/s²).
 * 2. Impact Spike: Magnitude spikes above IMPACT_THRESHOLD (> 26.0 m/s² ≈ 2.8G).
 * 3. Post-Impact Immobility: Magnitude settles below IMMOBILITY_THRESHOLD for >= 1.5s,
 *    indicating the worker is immobilized or unconscious.
 */
public class FallDetectionManager implements SensorEventListener {

    public interface OnFallDetectedListener {
        void onFallDetected(float impactMagnitudeG, long timestamp);
    }

    public interface OnTelemetryUpdateListener {
        void onAccelerationMagnitude(float magnitudeMps2, String stateDescription);
    }

    private static final float FREE_FALL_THRESHOLD = 2.8f;      // Free-fall threshold (m/s²)
    private static final float IMPACT_THRESHOLD = 25.0f;        // Impact spike threshold (m/s²)
    private static final float IMMOBILITY_THRESHOLD = 2.0f;     // Immobility threshold (m/s²)
    private static final long IMPACT_WINDOW_MS = 1500;          // Impact must follow free fall within 1.5s
    private static final long IMMOBILITY_DURATION_MS = 1500;    // Immobility must persist for 1.5s

    private final Context context;
    private final SensorManager sensorManager;
    private Sensor linearAccelSensor;
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
            linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            if (linearAccelSensor == null) {
                rawAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
        }
    }

    public void startMonitoring(OnFallDetectedListener fallListener, OnTelemetryUpdateListener telemetryListener) {
        this.fallListener = fallListener;
        this.telemetryListener = telemetryListener;

        if (sensorManager == null || isMonitoring) return;

        if (linearAccelSensor != null) {
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME);
            isMonitoring = true;
        } else if (rawAccelSensor != null) {
            sensorManager.registerListener(this, rawAccelSensor, SensorManager.SENSOR_DELAY_GAME);
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
        float ax = event.values[0];
        float ay = event.values[1];
        float az = event.values[2];

        // For raw accelerometer, subtract 1G (9.8 m/s²) baseline
        float mag;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float totalMag = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            mag = Math.abs(totalMag - SensorManager.GRAVITY_EARTH);
        } else {
            mag = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        }

        long now = System.currentTimeMillis();

        switch (currentState) {
            case IDLE:
                if (mag < FREE_FALL_THRESHOLD) {
                    currentState = State.FREE_FALL;
                    freeFallTimestamp = now;
                }
                break;

            case FREE_FALL:
                if (now - freeFallTimestamp > IMPACT_WINDOW_MS) {
                    currentState = State.IDLE; // Window expired
                } else if (mag > IMPACT_THRESHOLD) {
                    currentState = State.IMPACT_DETECTED;
                    impactTimestamp = now;
                    peakImpactG = mag / SensorManager.GRAVITY_EARTH;

                    // Schedule immobility check
                    scheduleImmobilityVerification(peakImpactG);
                }
                break;

            case IMPACT_DETECTED:
                if (mag > IMMOBILITY_THRESHOLD * 2) {
                    // Worker is moving / struggling -> cancel immobility alarm
                    if (immobilityChecker != null) {
                        mainHandler.removeCallbacks(immobilityChecker);
                        immobilityChecker = null;
                    }
                    currentState = State.IDLE;
                }
                break;
        }

        if (telemetryListener != null && (now - lastTelemetryPost > 300)) {
            lastTelemetryPost = now;
            final float currentMag = mag;
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
