package com.example.sitesync.hardware;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.widget.Toast;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.ui.VoiceAlerterDialogActivity;

/**
 * Foreground Service hosting a Floating Voice Alerter Widget (like Messenger chatheads).
 * Remains accessible even when SiteSync is running in the background or minimized.
 * When tapped by a worker or engineer, launches the Emergency Voice Recognition engine.
 */
public class FloatingVoiceAlerterService extends Service {

    private static final String CHANNEL_ID = "channel_sitesync_voice_alerter";
    private static final int NOTIFICATION_ID = 9110;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private View cancelView;
    private WindowManager.LayoutParams cancelParams;
    private SessionManager sessionManager;

    public static void startAlerter(Context context) {
        try {
            Intent intent = new Intent(context, FloatingVoiceAlerterService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            android.util.Log.w("FloatingAlerter", "startAlerter warning: " + e.getMessage());
        }
    }

    public static void stopAlerter(Context context) {
        try {
            Intent intent = new Intent(context, FloatingVoiceAlerterService.class);
            context.stopService(intent);
        } catch (Exception ignored) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, buildForegroundNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, buildForegroundNotification());
            }
        } catch (Exception e) {
            android.util.Log.w("FloatingAlerter", "startForeground warning: " + e.getMessage());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            initFloatingOverlay();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SiteSync Voice Alerter",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background site voice emergency monitoring overlay");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildForegroundNotification() {
        Intent tapIntent = new Intent(this, VoiceAlerterDialogActivity.class);
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, tapIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Site Voice Alerter Active")
                .setContentText("Tap to speak emergency status (e.g. 'tin tolay agun lagse')")
                .setSmallIcon(R.drawable.ic_shield)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void initFloatingOverlay() {
        try {
            sessionManager = new SessionManager(this);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_voice_bubble, null);
            cancelView = LayoutInflater.from(this).inflate(R.layout.layout_floating_cancel_zone, null);

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            // Bubble Layout Params
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 20;
            params.y = 350;

            // Cancel Target Layout Params (Bottom Center)
            cancelParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            cancelParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            cancelParams.y = 120;

            cancelView.setVisibility(View.GONE);
            windowManager.addView(cancelView, cancelParams);

            ImageView ivBubble = floatingView.findViewById(R.id.ivFloatingBubble);
            ivBubble.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;
                private boolean isClick = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isClick = true;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            int deltaX = (int) (event.getRawX() - initialTouchX);
                            int deltaY = (int) (event.getRawY() - initialTouchY);
                            if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                                isClick = false;
                                if (cancelView != null && cancelView.getVisibility() != View.VISIBLE) {
                                    cancelView.setVisibility(View.VISIBLE);
                                }
                            }

                            params.x = initialX + deltaX;
                            params.y = initialY + deltaY;
                            if (windowManager != null && floatingView != null) {
                                windowManager.updateViewLayout(floatingView, params);
                            }

                            // Check if dragged down into the cancel zone (bottom 25% of screen and center horizontally)
                            boolean inCancelZone = (event.getRawY() > screenHeight - 320)
                                    && (Math.abs(event.getRawX() - (screenWidth / 2.0f)) < (screenWidth * 0.35f));
                            if (cancelView != null) {
                                if (inCancelZone) {
                                    cancelView.setScaleX(1.18f);
                                    cancelView.setScaleY(1.18f);
                                } else {
                                    cancelView.setScaleX(1.0f);
                                    cancelView.setScaleY(1.0f);
                                }
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            if (cancelView != null) {
                                cancelView.setVisibility(View.GONE);
                            }

                            if (isClick) {
                                openVoiceAlerterActivity();
                                return true;
                            }

                            // If dropped in the cancel zone -> dismiss and stop service
                            boolean droppedInCancel = (event.getRawY() > screenHeight - 320)
                                    && (Math.abs(event.getRawX() - (screenWidth / 2.0f)) < (screenWidth * 0.35f));

                            if (droppedInCancel) {
                                if (sessionManager != null) {
                                    sessionManager.setFloatingVoiceAlerterEnabled(false);
                                }
                                Toast.makeText(FloatingVoiceAlerterService.this, "Voice Alerter closed", Toast.LENGTH_SHORT).show();
                                stopSelf();
                                return true;
                            }

                            // Snap bubble to nearest screen edge (left or right)
                            int bubbleWidth = floatingView.getWidth() > 0 ? floatingView.getWidth() : 140;
                            if (params.x + (bubbleWidth / 2) < (screenWidth / 2)) {
                                params.x = 24; // Snap to left edge
                            } else {
                                params.x = screenWidth - bubbleWidth - 24; // Snap to right edge
                            }

                            // Clamp vertical bounds within visible screen
                            if (params.y < 80) params.y = 80;
                            if (params.y > screenHeight - 250) params.y = screenHeight - 250;

                            if (windowManager != null && floatingView != null) {
                                windowManager.updateViewLayout(floatingView, params);
                            }
                            return true;
                    }
                    return false;
                }
            });

            windowManager.addView(floatingView, params);
        } catch (Exception ignored) {}
    }

    private void openVoiceAlerterActivity() {
        Intent intent = new Intent(this, VoiceAlerterDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null) {
            if (floatingView != null) {
                try {
                    windowManager.removeView(floatingView);
                } catch (Exception ignored) {}
                floatingView = null;
            }
            if (cancelView != null) {
                try {
                    windowManager.removeView(cancelView);
                } catch (Exception ignored) {}
                cancelView = null;
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
