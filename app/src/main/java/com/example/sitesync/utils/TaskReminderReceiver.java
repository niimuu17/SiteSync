package com.example.sitesync.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.sitesync.R;
import com.example.sitesync.ui.MainActivity;

public class TaskReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "sitesync_task_reminders";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_IS_OVERDUE = "extra_is_overdue";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        boolean isOverdue = intent.getBooleanExtra(EXTRA_IS_OVERDUE, false);

        if (taskTitle == null || taskTitle.isEmpty()) {
            taskTitle = "Site Task Reminder";
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for construction task milestones and deadlines");
            manager.createNotificationChannel(channel);
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId != null ? taskId.hashCode() : 101,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = isOverdue ? "⚠️ Overdue Task Alert!" : "📅 Upcoming Task Deadline";
        String message = isOverdue
                ? "Task [" + taskTitle + "] is now overdue! Please review and update status."
                : "Task [" + taskTitle + "] is due in 24 hours.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(taskId != null ? taskId.hashCode() : (int) System.currentTimeMillis(), builder.build());
    }
}
