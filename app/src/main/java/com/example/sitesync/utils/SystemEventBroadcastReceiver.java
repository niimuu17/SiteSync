package com.example.sitesync.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;

/**
 * System Broadcast Receiver:
 * Listens and reacts to core OS system broadcast events:
 * - Low Battery Mode (ACTION_BATTERY_LOW / ACTION_BATTERY_OKAY): Throttles heavy sensor sampling to conserve emergency charge.
 * - Airplane Mode Transitions (ACTION_AIRPLANE_MODE_CHANGED): Switches database between real-time cloud and offline Room DB queue.
 * - Power Connected (ACTION_POWER_CONNECTED): Restores full-fidelity background telemetry.
 */
public class SystemEventBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "SystemEventReceiver";
    public static final String CHANNEL_ID = "channel_sitesync_system_events";
    private static final int NOTIF_BATTERY_ID = 401;
    private static final int NOTIF_AIRPLANE_ID = 402;

    public interface SystemEventListener {
        void onBatteryStateChanged(boolean isLowBattery);
        void onAirplaneModeChanged(boolean isAirplaneModeOn);
    }

    private static SystemEventListener listener;

    public static void setSystemEventListener(SystemEventListener eventListener) {
        listener = eventListener;
    }

    public static void register(Context context, SystemEventBroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        context.registerReceiver(receiver, filter);
        Log.i(TAG, "SystemEventBroadcastReceiver registered for battery and airplane mode events.");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        SessionManager sessionManager = new SessionManager(context);

        createNotificationChannel(context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            Log.w(TAG, "System Broadcast: Low Battery event received!");
            sessionManager.setFloatingVoiceAlerterEnabled(true); // Keep critical safety active

            NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle("SiteSync: Low Battery (<15%) Detected")
                    .setContentText("Power-saver safety mode active. Sensors throttled to preserve life-safety battery.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            if (nm != null) nm.notify(NOTIF_BATTERY_ID, nb.build());

            Toast.makeText(context, "⚠️ Low Battery: Sensor sampling throttled to preserve charge", Toast.LENGTH_LONG).show();
            if (listener != null) listener.onBatteryStateChanged(true);

        } else if (Intent.ACTION_BATTERY_OKAY.equals(action) || Intent.ACTION_POWER_CONNECTED.equals(action)) {
            Log.i(TAG, "System Broadcast: Battery normal / charging.");
            NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle("SiteSync: Power Restored")
                    .setContentText("Normal battery level. Full sensor telemetry and cloud sync resumed.")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true);
            if (nm != null) nm.notify(NOTIF_BATTERY_ID, nb.build());

            if (listener != null) listener.onBatteryStateChanged(false);

        } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean isAirplaneMode = intent.getBooleanExtra("state", false);
            Log.w(TAG, "System Broadcast: Airplane Mode changed: " + isAirplaneMode);

            String title = isAirplaneMode ? "SiteSync: Airplane Mode Active" : "SiteSync: Network Restored";
            String msg = isAirplaneMode
                    ? "Offline mode engaged. All field evidence, voice memos, and chat are queued locally in Room DB."
                    : "Connected to cellular network. Syncing pending offline queues to Firebase cloud.";

            NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);
            if (nm != null) nm.notify(NOTIF_AIRPLANE_ID, nb.build());

            Toast.makeText(context, title + " • " + (isAirplaneMode ? "Offline Queue Active" : "Cloud Sync Resumed"), Toast.LENGTH_LONG).show();
            if (listener != null) listener.onAirplaneModeChanged(isAirplaneMode);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SiteSync System & Power Events",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Monitors low battery transitions, airplane mode events, and device telemetry");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
