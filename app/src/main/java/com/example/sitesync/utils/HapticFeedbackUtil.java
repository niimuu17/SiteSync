package com.example.sitesync.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Enterprise Industrial Haptic Feedback Utility
 * Delivers crisp, tactile vibrational cues for field workers wearing heavy gloves.
 */
public class HapticFeedbackUtil {

    /**
     * Light click pulse for tab selections and quick toggles.
     */
    public static void lightClick(Context context, View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        vibrate(context, 20);
    }

    /**
     * Heavy click pulse for button presses and speech activation.
     */
    public static void heavyClick(Context context, View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        vibrate(context, 50);
    }

    /**
     * Double buzz haptic confirmation for "Check Level" snap within target tolerance.
     * Waveform pattern: 0ms delay, 80ms buzz, 50ms pause, 80ms buzz.
     */
    public static void doubleBuzzToleranceSuccess(Context context) {
        if (context == null) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    long[] timings = new long[]{0, 80, 50, 80};
                    int[] amplitudes = new int[]{0, 255, 0, 255};
                    VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
                    vibrator.vibrate(effect);
                } else {
                    vibrator.vibrate(new long[]{0, 80, 50, 80}, -1);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Success pulse for delivery ticket scan and photo capture.
     */
    public static void scanSuccess(Context context) {
        if (context == null) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void vibrate(Context context, long durationMs) {
        if (context == null) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(durationMs);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
