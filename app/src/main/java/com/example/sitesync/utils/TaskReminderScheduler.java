package com.example.sitesync.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.sitesync.data.local.entities.TaskEntity;

public class TaskReminderScheduler {

    private static final String TAG = "TaskReminderScheduler";

    public static void scheduleReminders(Context context, TaskEntity task) {
        if (task == null || task.getDueDate() <= 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long now = System.currentTimeMillis();
        long dueTime = task.getDueDate();

        // 1. 24 Hours Prior Notification
        long time24hBefore = dueTime - (24L * 60 * 60 * 1000);
        if (time24hBefore > now) {
            Intent intent24h = new Intent(context, TaskReminderReceiver.class);
            intent24h.putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.getId() + "_24h");
            intent24h.putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.getTitle());
            intent24h.putExtra(TaskReminderReceiver.EXTRA_IS_OVERDUE, false);

            PendingIntent pi24h = PendingIntent.getBroadcast(
                    context,
                    (task.getId() + "_24h").hashCode(),
                    intent24h,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time24hBefore, pi24h);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, time24hBefore, pi24h);
                }
                Log.d(TAG, "Scheduled 24h reminder for task: " + task.getTitle());
            } catch (SecurityException se) {
                Log.w(TAG, "Cannot schedule exact alarm without permission", se);
            }
        }

        // 2. Exact Deadline / Overdue Notification
        if (dueTime > now) {
            Intent intentExact = new Intent(context, TaskReminderReceiver.class);
            intentExact.putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.getId() + "_due");
            intentExact.putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.getTitle());
            intentExact.putExtra(TaskReminderReceiver.EXTRA_IS_OVERDUE, true);

            PendingIntent piExact = PendingIntent.getBroadcast(
                    context,
                    (task.getId() + "_due").hashCode(),
                    intentExact,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueTime, piExact);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, dueTime, piExact);
                }
                Log.d(TAG, "Scheduled deadline alert for task: " + task.getTitle());
            } catch (SecurityException se) {
                Log.w(TAG, "Cannot schedule exact alarm without permission", se);
            }
        }
    }

    public static void cancelReminders(Context context, String taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || taskId == null) return;

        // Cancel 24h intent
        Intent intent24h = new Intent(context, TaskReminderReceiver.class);
        PendingIntent pi24h = PendingIntent.getBroadcast(
                context,
                (taskId + "_24h").hashCode(),
                intent24h,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pi24h != null) {
            alarmManager.cancel(pi24h);
            pi24h.cancel();
        }

        // Cancel exact intent
        Intent intentExact = new Intent(context, TaskReminderReceiver.class);
        PendingIntent piExact = PendingIntent.getBroadcast(
                context,
                (taskId + "_due").hashCode(),
                intentExact,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (piExact != null) {
            alarmManager.cancel(piExact);
            piExact.cancel();
        }
    }
}
