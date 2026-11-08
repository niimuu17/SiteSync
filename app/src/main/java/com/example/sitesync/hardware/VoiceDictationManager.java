package com.example.sitesync.hardware;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.example.sitesync.utils.HapticFeedbackUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Feature 1: "Speak Note" Hands-Free Voice Dictation
 * Encapsulates Android SpeechRecognizer + Parallel MediaRecorder (16 kHz AAC mono)
 * Automatically finalizes after 1.8s silence or tap, provides RMS dB telemetry and haptics.
 */
public class VoiceDictationManager {

    private static final String TAG = "VoiceDictationManager";
    private static final long SILENCE_THRESHOLD_MS = 1800; // 1.8 seconds pause detection

    public interface OnDictationListener {
        void onSpeechRmsChanged(float rmsdB);
        void onPartialTranscription(String partialText);
        void onFinalTranscription(String finalText, File audioBackupFile);
        void onError(String userFriendlyError);
    }

    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private MediaRecorder mediaRecorder;
    private File currentAudioBackupFile;
    private OnDictationListener activeListener;
    private boolean isListening = false;
    private String accumulatedFinalText = "";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable silenceTimeoutRunnable = () -> {
        Log.d(TAG, "Silence threshold reached (1.8s). Finalizing dictation.");
        stopListening();
    };

    public VoiceDictationManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void startListening(OnDictationListener listener) {
        if (isListening) {
            stopListening();
        }

        this.activeListener = listener;
        this.accumulatedFinalText = "";

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (activeListener != null) {
                activeListener.onError("Speech recognition service is not available on this device.");
            }
            return;
        }

        HapticFeedbackUtil.heavyClick(context, null);

        // 1. Prepare Audio Backup File (.m4a)
        File audioDir = new File(context.getCacheDir(), "voice_notes");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        currentAudioBackupFile = new File(audioDir, "note_" + System.currentTimeMillis() + ".m4a");

        // 2. Initialize MediaRecorder (16kHz AAC mono)
        try {
            initMediaRecorder(currentAudioBackupFile);
            mediaRecorder.start();
        } catch (Exception e) {
            Log.w(TAG, "MediaRecorder initialization failed, continuing speech recognition: " + e.getMessage());
        }

        // 3. Initialize SpeechRecognizer
        mainHandler.post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                }
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        Log.d(TAG, "Microphone ready for speech input");
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        resetSilenceTimer();
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                        if (activeListener != null) {
                            activeListener.onSpeechRmsChanged(rmsdB);
                        }
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {}

                    @Override
                    public void onEndOfSpeech() {
                        // Keep listening until 1.8s silence or manual stop
                        resetSilenceTimer();
                    }

                    @Override
                    public void onError(int error) {
                        String msg;
                        switch (error) {
                            case SpeechRecognizer.ERROR_NO_MATCH:
                                msg = "No speech detected. Please speak clearly into the mic.";
                                break;
                            case SpeechRecognizer.ERROR_AUDIO:
                                msg = "Audio recording error. Check microphone.";
                                break;
                            case SpeechRecognizer.ERROR_NETWORK:
                            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                                msg = "Offline speech model in use.";
                                break;
                            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                msg = "Microphone permission required for Speak Note.";
                                break;
                            default:
                                msg = "Voice recognition paused.";
                        }
                        if (activeListener != null && isListening) {
                            activeListener.onError(msg);
                        }
                        stopListening();
                    }

                    @Override
                    public void onResults(Bundle results) {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            accumulatedFinalText = matches.get(0);
                        }
                        finishDictation();
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            String partial = matches.get(0);
                            accumulatedFinalText = partial;
                            if (activeListener != null) {
                                activeListener.onPartialTranscription(partial);
                            }
                            resetSilenceTimer();
                        }
                    }

                    @Override
                    public void onEvent(int eventType, Bundle params) {}
                });

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

                speechRecognizer.startListening(intent);
                isListening = true;
                resetSilenceTimer();

            } catch (Exception e) {
                if (activeListener != null) {
                    activeListener.onError("Failed to start voice dictation: " + e.getMessage());
                }
                stopListening();
            }
        });
    }

    private void resetSilenceTimer() {
        mainHandler.removeCallbacks(silenceTimeoutRunnable);
        mainHandler.postDelayed(silenceTimeoutRunnable, SILENCE_THRESHOLD_MS);
    }

    private void initMediaRecorder(File outputFile) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = new MediaRecorder(context);
        } else {
            mediaRecorder = new MediaRecorder();
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(16000); // 16 kHz
        mediaRecorder.setAudioChannels(1);         // Mono
        mediaRecorder.setAudioEncodingBitRate(32000);
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
        mediaRecorder.prepare();
    }

    public synchronized void stopListening() {
        if (!isListening) return;
        isListening = false;
        mainHandler.removeCallbacks(silenceTimeoutRunnable);

        mainHandler.post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.stopListening();
                }
            } catch (Exception ignored) {
            }
        });

        finishDictation();
    }

    private synchronized void finishDictation() {
        // Release MediaRecorder
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception ignored) {
            }
            try {
                mediaRecorder.release();
            } catch (Exception ignored) {
            }
            mediaRecorder = null;
        }

        // Release SpeechRecognizer
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }
            speechRecognizer = null;
        }

        HapticFeedbackUtil.heavyClick(context, null);

        if (activeListener != null) {
            String finalText = accumulatedFinalText != null ? accumulatedFinalText.trim() : "";
            activeListener.onFinalTranscription(finalText, currentAudioBackupFile);
            activeListener = null;
        }
    }

    public boolean isListening() {
        return isListening;
    }
}
