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
 * Continuous 16-bit PCM AudioRecord stream calculating Root-Mean-Square (RMS) amplitude,
 * converting to calibrated sound pressure level (dB SPL) with fast-attack / slow-decay smoothing.
 * Flags hazardous acoustic environments (>= 85.0 dB OSHA threshold) requiring hearing protection PPE.
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

    // Standard acoustic reference calibration for raw MEMS mobile microphones without AGC.
    // Quiet indoor room ambient (RMS ~ 400): ~40 - 44 dB SPL (Safe)
    // Normal conversation (RMS ~ 2000): ~56 - 65 dB SPL (Safe / Elevated)
    // Heavy machinery / shouting / power drill (RMS > 15000): >= 85 dB SPL (OSHA PPE Hazard Alert)
    private static final double CALIBRATION_OFFSET_DB = -10.0;
    private static final double MIN_DB_SPL = 35.0;
    private static final double MAX_DB_SPL = 115.0;

    private final Context context;
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);

    private AudioRecord audioRecord;
    private int bufferSize;
    private OnNoiseLevelListener activeListener;

    private double smoothedDb = 42.0; // Nominal quiet indoor baseline

    public AudioDecibelManager(Context context) {
        this.context = context.getApplicationContext();
        this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (this.bufferSize <= 0) {
            this.bufferSize = SAMPLE_RATE * 2;
        }
    }

    public static boolean hasMicrophonePermission(Context context) {
        if (context == null) return false;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Starts background microphone capture and continuous RMS dB computation.
     */
    public synchronized void startMonitoring(OnNoiseLevelListener listener) {
        this.activeListener = listener;

        if (isMonitoring.get()) {
            return;
        }

        if (!hasMicrophonePermission(context)) {
            if (activeListener != null) {
                activeListener.onError("Microphone permission required for decibel acoustic monitoring.");
            }
            return;
        }

        try {
            // Attempt VOICE_RECOGNITION source first (bypasses aggressive AGC distortion on Android)
            int audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
            try {
                audioRecord = new AudioRecord(
                        audioSource,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize
                );
            } catch (Exception e) {
                audioRecord = null;
            }

            if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "VOICE_RECOGNITION source unavailable, falling back to MIC source");
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize
                );
            }

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize with all sources");
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
        long lastUiPostTime = 0;
        long lastLogTime = 0;

        while (isMonitoring.get() && audioRecord != null) {
            int readCount = audioRecord.read(audioBuffer, 0, audioBuffer.length);

            if (readCount <= 0) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            // Calculate RMS: sqrt( (1/N) * sum(x_i^2) )
            double sumOfSquares = 0.0;
            for (int i = 0; i < readCount; i++) {
                sumOfSquares += (double) audioBuffer[i] * audioBuffer[i];
            }
            double rms = Math.sqrt(sumOfSquares / readCount);

            // Calibrated Sound Pressure Level (dB SPL)
            double instantDb;
            if (rms > 2.0) {
                instantDb = 20.0 * Math.log10(rms) + CALIBRATION_OFFSET_DB;
            } else {
                instantDb = MIN_DB_SPL;
            }

            // Clamp instant value
            instantDb = Math.max(MIN_DB_SPL, Math.min(MAX_DB_SPL, instantDb));

            // Fast-attack (alpha=0.35) for impact sounds, Smooth-decay (alpha=0.08) for stable readings
            if (instantDb > smoothedDb) {
                smoothedDb = 0.35 * instantDb + 0.65 * smoothedDb;
            } else {
                smoothedDb = 0.08 * instantDb + 0.92 * smoothedDb;
            }

            long now = System.currentTimeMillis();
            if (now - lastLogTime >= 2000) {
                lastLogTime = now;
                Log.d(TAG, String.format(java.util.Locale.US, "Acoustic Telemetry: RMS=%.1f -> Instant=%.1fdB -> Smoothed=%.1fdB", rms, instantDb, smoothedDb));
            }

            // Update UI at ~350ms intervals (prevents main thread event starvation)
            if (now - lastUiPostTime >= 350) {
                lastUiPostTime = now;
                final double finalDb = Math.round(smoothedDb * 10.0) / 10.0;
                final boolean isHazardous = finalDb >= OSHA_HAZARD_THRESHOLD_DB;

                mainHandler.post(() -> {
                    if (activeListener != null && isMonitoring.get()) {
                        activeListener.onNoiseLevelUpdated(finalDb, isHazardous);
                    }
                });
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
