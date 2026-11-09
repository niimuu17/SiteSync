package com.example.sitesync.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.sitesync.R;
import com.example.sitesync.ui.MainActivity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) Service for Critical Jobsite Hazards & Evacuation Alerts.
 *
 * Configures an urgent Android Notification Channel with max priority, custom vibration,
 * and audio override to alert workers in loud construction environments.
 */
public class SiteSyncFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "SiteSyncFCM";
    public static final String CHANNEL_ID_CRITICAL_HAZARDS = "sitesync_critical_hazard_fcm";
    public static final String CHANNEL_NAME = "Critical Hazard & Evacuation Push";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Device Registration Token: " + token);
        // Automatically subscribe device to universal site safety channels
        subscribeToSiteSafetyChannels();
    }

    public static void subscribeToSiteSafetyChannels() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("critical_hazards")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Subscribed to FCM topic: critical_hazards");
                        }
                    });

            FirebaseMessaging.getInstance().subscribeToTopic("site_evacuation")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Subscribed to FCM topic: site_evacuation");
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "FCM subscription deferred: " + e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM Message Received from: " + remoteMessage.getFrom());

        String title = "🚨 CRITICAL JOBSITE HAZARD";
        String body = "Evacuate danger zone immediately.";
        String hazardType = "GENERAL_SAFETY";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body")) body = data.get("body");
            if (data.containsKey("hazardType")) hazardType = data.get("hazardType");
        }

        postCriticalNotification(this, title, body, hazardType);
    }

    /**
     * Dispatches a high-priority heads-up notification with custom siren vibration pattern.
     */
    public static void postCriticalNotification(Context context, String title, String body, String hazardType) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        createHazardNotificationChannel(context, nm);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("OPEN_TAB", "SECURITY");
        intent.putExtra("HAZARD_TYPE", hazardType);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_CRITICAL_HAZARDS)
                .setSmallIcon(R.drawable.ic_warning_amber)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 200, 500, 200, 500})
                .setContentIntent(pendingIntent);

        nm.notify((int) (System.currentTimeMillis() % 100000), builder.build());
    }

    public static void createHazardNotificationChannel(Context context, NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_CRITICAL_HAZARDS,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Critical life-safety hazard notifications and evacuation dispatches.");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
            channel.enableLights(true);
            channel.setLightColor(0xFFFF0000); // Red strobe

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            channel.setSound(soundUri, audioAttributes);

            nm.createNotificationChannel(channel);
        }
    }
}
