package com.example.sitesync.hardware;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Feature: Acoustic Jobsite Safety Decibel Meter Subsystem.
 * Continuous 16-bit PCM AudioRecord stream calculating Root-Mean-Square (RMS) amplitude
 * and converting to calibrated sound pressure level (dB SPL).
 * Flags hazardous acoustic environments (>85 dB OSHA threshold) requiring hearing protection PPE.
 */
public class AudioDecibelManager {

    private static final String TAG = "AudioDecibelManager";
    public static final double OSHA_HAZARD_THRESHOLD_DB = 85.0;

    public interface OnNoiseLevelListener {
        void onNoiseLevelUpdated(double decibels, boolean isHazardous);
        void onError(String errorMessage);
    }

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final Context context;
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);

    private AudioRecord audioRecord;
    private int bufferSize;
    private OnNoiseLevelListener activeListener;

    public AudioDecibelManager(Context context) {
        this.context = context.getApplicationContext();
        this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (this.bufferSize <= 0) {
            this.bufferSize = SAMPLE_RATE * 2;
        }
    }

    /**
     * Starts background microphone capture and continuous RMS dB computation.
     */
    public synchronized void startMonitoring(OnNoiseLevelListener listener) {
        if (isMonitoring.get()) {
            this.activeListener = listener;
            return;
        }

        this.activeListener = listener;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (activeListener != null) {
                activeListener.onError("Microphone permission required for decibel acoustic monitoring.");
            }
            return;
        }

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize");
                if (activeListener != null) {
                    activeListener.onError("AudioRecord initialization failed.");
                }
                return;
            }

            audioRecord.startRecording();
            isMonitoring.set(true);

            audioExecutor.execute(this::recordingLoop);

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException starting AudioRecord", e);
            if (activeListener != null) {
                activeListener.onError("Microphone access denied: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start decibel monitoring", e);
            if (activeListener != null) {
                activeListener.onError("Decibel monitoring error: " + e.getMessage());
            }
        }
    }

    private void recordingLoop() {
        short[] audioBuffer = new short[bufferSize / 2];

        while (isMonitoring.get() && audioRecord != null) {
            int readCount = audioRecord.read(audioBuffer, 0, audioBuffer.length);

            if (readCount <= 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            if (readCount > 0) {
                // Calculate RMS: sqrt( (1/N) * sum(x_i^2) )
                double sumOfSquares = 0.0;
                for (int i = 0; i < readCount; i++) {
                    sumOfSquares += (double) audioBuffer[i] * audioBuffer[i];
                }
                double rms = Math.sqrt(sumOfSquares / readCount);

                // Convert RMS to dB (Reference amplitude = 1.0)
                // Offset by +18dB baseline calibration for standard construction mic sensitivity
                double db = 0.0;
                if (rms > 1.0) {
                    db = 20.0 * Math.log10(rms) + 18.0;
                }

                // Clamp values realistically between 30 dB (whisper/quiet site) and 125 dB (jackhammer/air horn)
                final double finalDb = Math.max(32.0, Math.min(125.0, Math.round(db * 10.0) / 10.0));
                final boolean isHazardous = finalDb >= OSHA_HAZARD_THRESHOLD_DB;

                mainHandler.post(() -> {
                    if (activeListener != null && isMonitoring.get()) {
                        activeListener.onNoiseLevelUpdated(finalDb, isHazardous);
                    }
                });

                try {
                    // Sample every ~200ms to balance responsiveness and UI thread overhead
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Halts recording loop and safely releases audio hardware resources.
     */
    public synchronized void stopMonitoring() {
        isMonitoring.set(false);
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception ignored) {
            } finally {
                audioRecord = null;
            }
        }
    }

    public boolean isMonitoring() {
        return isMonitoring.get();
    }
}
