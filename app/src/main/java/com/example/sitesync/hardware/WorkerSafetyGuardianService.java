package com.example.sitesync.hardware;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.sitesync.R;
import com.example.sitesync.ui.EmergencyFallAlertActivity;
import com.example.sitesync.ui.WorkerDashboardActivity;

/**
 * Worker Safety Guardian Foreground Service:
 * Runs continuously in the background (Pocket Mode) even when SiteSync is minimized or the screen is locked.
 * Monitors raw 3-axis accelerometer telemetry for sudden free-fall, violent impact spikes, and subsequent immobility.
 * Automatically wakes up device display and launches the full-screen emergency countdown modal over keyguard.
 */
public class WorkerSafetyGuardianService extends Service {

    private static final String TAG = "WorkerSafetyGuardian";
    public static final String GUARDIAN_CHANNEL_ID = "channel_sitesync_guardian";
    public static final String EMERGENCY_CHANNEL_ID = "channel_sitesync_fall_emergency";
    private static final int NOTIFICATION_ID = 9115;
    private static final int EMERGENCY_NOTIF_ID = 9116;

    private static volatile boolean isServiceRunning = false;

    private PowerManager.WakeLock wakeLock;
    private FallDetectionManager fallDetectionManager;

    public static void startGuardian(Context context) {
        try {
            Intent intent = new Intent(context, WorkerSafetyGuardianService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Safety Guardian Service", e);
        }
    }

    public static void stopGuardian(Context context) {
        try {
            Intent intent = new Intent(context, WorkerSafetyGuardianService.class);
            context.stopService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop Safety Guardian Service", e);
        }
    }

    public static boolean isRunning() {
        return isServiceRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        Log.i(TAG, "WorkerSafetyGuardianService initialized.");

        createNotificationChannels();
        acquirePartialWakeLock();
        startForeground(NOTIFICATION_ID, buildGuardianNotification());

        // Initialize background fall detection engine
        fallDetectionManager = new FallDetectionManager(this);
        fallDetectionManager.startMonitoring(
                this::onAutonomousFallDetected,
                (magnitude, stateDescription) -> {}
        );
        Log.i(TAG, "Background Accelerometer Fall Monitoring active in Pocket Mode.");
    }

    private void acquirePartialWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SiteSync:SafetyGuardianWakeLock");
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire();
                Log.d(TAG, "Partial WakeLock acquired for background fall telemetry.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not acquire WakeLock: " + e.getMessage());
        }
    }

    private void onAutonomousFallDetected(float impactG, long timestamp) {
        Log.w(TAG, "⚠️ AUTONOMOUS BACKGROUND FALL DETECTED! Magnitude: " + impactG + "G");

        // 1. Wake up device display if screen is asleep/locked
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                @SuppressWarnings("deprecation")
                PowerManager.WakeLock screenLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "SiteSync:FallAlertWakeup"
                );
                screenLock.acquire(15000); // Keep screen on for 15s countdown
            }
        } catch (Exception e) {
            Log.w(TAG, "Screen wakeup lock error: " + e.getMessage());
        }

        // 2. Intent to launch Emergency Fall Alert Activity
        Intent alertIntent = new Intent(this, EmergencyFallAlertActivity.class);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        alertIntent.putExtra("impact_g", impactG);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                9111,
                alertIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // 3. Post Max-Priority Full-Screen Notification (wakes up lockscreen automatically on Android 10-14)
        NotificationCompat.Builder emergencyBuilder = new NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("🚨 CRITICAL: Worker Fall Detected!")
                .setContentText("Worker fall impact " + String.format(java.util.Locale.US, "%.1fG", impactG) + " registered in pocket mode.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(EMERGENCY_NOTIF_ID, emergencyBuilder.build());
        }

        // 4. Also directly launch the Activity
        try {
            startActivity(alertIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed direct startActivity: " + e.getMessage());
        }
    }

    private Notification buildGuardianNotification() {
        Intent openDashboardIntent = new Intent(this, WorkerDashboardActivity.class);
        openDashboardIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openDashboardIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, GUARDIAN_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("SiteSync Safety Guardian Active")
                .setContentText("Pocket Mode: Monitoring worker falls & jobsite hazards in background")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                // Background Service Channel
                NotificationChannel guardianChannel = new NotificationChannel(
                        GUARDIAN_CHANNEL_ID,
                        "SiteSync Safety Guardian Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                guardianChannel.setDescription("Persistent foreground monitoring of worker fall hazards in background");
                nm.createNotificationChannel(guardianChannel);

                // Critical Emergency Fall Alarm Channel
                NotificationChannel emergencyChannel = new NotificationChannel(
                        EMERGENCY_CHANNEL_ID,
                        "SiteSync Fall & Man-Down Emergency",
                        NotificationManager.IMPORTANCE_HIGH
                );
                emergencyChannel.setDescription("High-priority full-screen alerts when a sudden worker fall occurs");
                emergencyChannel.enableVibration(true);
                emergencyChannel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
                emergencyChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(emergencyChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Auto-restarts if system kills service under low memory
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        if (fallDetectionManager != null) {
            fallDetectionManager.stopMonitoring();
            fallDetectionManager = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        Log.i(TAG, "WorkerSafetyGuardianService stopped.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
