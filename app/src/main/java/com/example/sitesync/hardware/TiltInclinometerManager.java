package com.example.sitesync.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import com.example.sitesync.utils.HapticFeedbackUtil;

/**
 * Feature 2: "Check Level" Quick Slope & Plumb Gauge
 * Uses TYPE_ROTATION_VECTOR (with fallback to ACCELEROMETER/MAGNETOMETER)
 * Applies alpha=0.15 low-pass smoothing filter, calculates Pitch, Roll, and Slope %
 * Detects true level (0.0° ± 0.3°) and pipe grade (2.0% ± 0.2%) with double-buzz haptic confirmation.
 */
public class TiltInclinometerManager implements SensorEventListener {

    public interface OnInclinometerListener {
        void onAngleChanged(float pitchDegrees, float rollDegrees, float slopePercent, boolean isLevel, boolean isPipeGrade);
    }

    private static final float ALPHA = 0.15f; // Low-pass filter constant
    private static final float LEVEL_TOLERANCE_DEG = 0.3f;
    private static final float PIPE_GRADE_TARGET = 2.0f;
    private static final float PIPE_GRADE_TOLERANCE = 0.2f;

    private final Context context;
    private final SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelSensor;
    private Sensor magSensor;

    private boolean isRegistered = false;
    private OnInclinometerListener listener;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] lastAccel = new float[3];
    private final float[] lastMag = new float[3];
    private boolean hasAccel = false;
    private boolean hasMag = false;

    private float filteredPitch = 0f;
    private float filteredRoll = 0f;
    private boolean prevInTolerance = false;
    private long lastHapticTimestamp = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public TiltInclinometerManager(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotationSensor == null) {
                accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            }
        }
    }

    public void startListening(OnInclinometerListener listener) {
        this.listener = listener;
        if (sensorManager == null || isRegistered) return;

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            isRegistered = true;
        } else if (accelSensor != null) {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
            if (magSensor != null) {
                sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI);
            }
            isRegistered = true;
        }
    }

    public void stopListening() {
        if (sensorManager != null && isRegistered) {
            sensorManager.unregisterListener(this);
            isRegistered = false;
        }
        listener = null;
        prevInTolerance = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            processOrientation(orientationAngles[1], orientationAngles[2]);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccel, 0, 3);
            hasAccel = true;
            if (hasMag) {
                computeFallbackOrientation();
            } else {
                // Approximate pitch from accel Y/Z
                float rawPitch = (float) Math.atan2(lastAccel[1], lastAccel[2]);
                float rawRoll = (float) Math.atan2(-lastAccel[0], Math.sqrt(lastAccel[1] * lastAccel[1] + lastAccel[2] * lastAccel[2]));
                processOrientation(rawPitch, rawRoll);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMag, 0, 3);
            hasMag = true;
            if (hasAccel) {
                computeFallbackOrientation();
            }
        }
    }

    private void computeFallbackOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccel, lastMag)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            processOrientation(orientationAngles[1], orientationAngles[2]);
        }
    }

    private void processOrientation(float rawPitchRad, float rawRollRad) {
        // Convert to degrees
        float rawPitchDeg = (float) Math.toDegrees(rawPitchRad);
        float rawRollDeg = (float) Math.toDegrees(rawRollRad);

        // Low-pass filter: theta_filtered = alpha * theta_raw + (1 - alpha) * theta_prev
        filteredPitch = (ALPHA * rawPitchDeg) + ((1f - ALPHA) * filteredPitch);
        filteredRoll = (ALPHA * rawRollDeg) + ((1f - ALPHA) * filteredRoll);

        // Slope percentage = |tan(theta_p)| * 100
        double pitchRad = Math.toRadians(filteredPitch);
        float slopePercent = (float) Math.abs(Math.tan(pitchRad)) * 100f;
        if (Float.isNaN(slopePercent) || Float.isInfinite(slopePercent)) {
            slopePercent = 999.9f;
        }

        // Tolerances:
        // True level: 0.0° ± 0.3°
        boolean isLevel = Math.abs(filteredPitch) <= LEVEL_TOLERANCE_DEG && Math.abs(filteredRoll) <= LEVEL_TOLERANCE_DEG;
        // Pipe grade: 2.0% ± 0.2%
        boolean isPipeGrade = Math.abs(slopePercent - PIPE_GRADE_TARGET) <= PIPE_GRADE_TOLERANCE;

        boolean nowInTolerance = isLevel || isPipeGrade;
        long now = System.currentTimeMillis();
        if (nowInTolerance && !prevInTolerance && (now - lastHapticTimestamp > 1200)) {
            lastHapticTimestamp = now;
            HapticFeedbackUtil.doubleBuzzToleranceSuccess(context);
        }
        prevInTolerance = nowInTolerance;

        final float finalPitch = filteredPitch;
        final float finalRoll = filteredRoll;
        final float finalSlope = slopePercent;
        final boolean finalLevel = isLevel;
        final boolean finalPipe = isPipeGrade;

        mainHandler.post(() -> {
            if (listener != null) {
                listener.onAngleChanged(finalPitch, finalRoll, finalSlope, finalLevel, finalPipe);
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public boolean isAvailable() {
        return rotationSensor != null || accelSensor != null;
    }
}
