package com.example.sitesync.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.hardware.VoiceDictationManager;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.ProgressLog;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.io.File;

/**
 * High-contrast industrial bottom sheet for fast milestone progress updates.
 * Features voice dictation ("Speak Note") and real-time Room + Firestore sync.
 */
public class UpdateProgressBottomSheet extends BottomSheetDialogFragment {

    public interface OnProgressUpdatedListener {
        void onProgressUpdated(int newProgress, String status, String notes);
    }

    private ConstructionActivity activity;
    private OnProgressUpdatedListener updateListener;
    private VoiceDictationManager voiceManager;
    private boolean isListening = false;
    private String selectedStatus = "IN PROGRESS";

    private TextView tvSheetActivityTitle;
    private TextView tvProgressPercentValue;
    private Slider sliderProgress;
    private MaterialButton btnStatusInProgress;
    private MaterialButton btnStatusDelayed;
    private MaterialButton btnStatusCompleted;
    private EditText etProgressNotes;
    private ImageButton btnVoiceProgressNotes;
    private MaterialButton btnSaveProgress;

    public static UpdateProgressBottomSheet newInstance(ConstructionActivity activity) {
        UpdateProgressBottomSheet sheet = new UpdateProgressBottomSheet();
        sheet.activity = activity;
        return sheet;
    }

    public void setOnProgressUpdatedListener(OnProgressUpdatedListener listener) {
        this.updateListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_update_progress_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSheetActivityTitle = view.findViewById(R.id.tvSheetActivityTitle);
        tvProgressPercentValue = view.findViewById(R.id.tvProgressPercentValue);
        sliderProgress = view.findViewById(R.id.sliderProgress);
        btnStatusInProgress = view.findViewById(R.id.btnStatusInProgress);
        btnStatusDelayed = view.findViewById(R.id.btnStatusDelayed);
        btnStatusCompleted = view.findViewById(R.id.btnStatusCompleted);
        etProgressNotes = view.findViewById(R.id.etProgressNotes);
        btnVoiceProgressNotes = view.findViewById(R.id.btnVoiceProgressNotes);
        btnSaveProgress = view.findViewById(R.id.btnSaveProgress);

        if (activity != null) {
            tvSheetActivityTitle.setText(activity.getName());
            int currentProg = activity.getProgress();
            sliderProgress.setValue(currentProg);
            tvProgressPercentValue.setText(currentProg + "%");
            selectedStatus = activity.getStatus() != null ? activity.getStatus() : "IN PROGRESS";
            updateStatusButtons();
        }

        sliderProgress.addOnChangeListener((slider, value, fromUser) -> {
            int intVal = Math.round(value);
            tvProgressPercentValue.setText(intVal + "%");
            if (fromUser && intVal % 10 == 0) {
                HapticFeedbackUtil.lightClick(requireContext(), slider);
            }
        });

        btnStatusInProgress.setOnClickListener(v -> {
            selectedStatus = "IN PROGRESS";
            updateStatusButtons();
        });
        btnStatusDelayed.setOnClickListener(v -> {
            selectedStatus = "DELAYED";
            updateStatusButtons();
        });
        btnStatusCompleted.setOnClickListener(v -> {
            selectedStatus = "COMPLETED";
            sliderProgress.setValue(100f);
            tvProgressPercentValue.setText("100%");
            updateStatusButtons();
        });

        setupVoiceDictation();

        btnSaveProgress.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            int newProg = Math.round(sliderProgress.getValue());
            String notes = etProgressNotes.getText() != null ? etProgressNotes.getText().toString().trim() : "";

            if (activity != null) {
                ProgressRepository repo = new ProgressRepository(requireContext());
                activity.setProgress(newProg);
                activity.setStatus(selectedStatus);
                repo.updateActivityProgress(activity.getId(), newProg, selectedStatus, notes);
            }

            if (updateListener != null) {
                updateListener.onProgressUpdated(newProg, selectedStatus, notes);
            }

            Toast.makeText(requireContext(), "Telemetry Synchronized: " + newProg + "%", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private void updateStatusButtons() {
        int amber = ContextCompat.getColor(requireContext(), R.color.accent_safety_amber);
        int dark = ContextCompat.getColor(requireContext(), R.color.dark_canvas);
        int surface = ContextCompat.getColor(requireContext(), R.color.dark_surface_elevated);
        int textSec = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        btnStatusInProgress.setBackgroundColor("IN PROGRESS".equalsIgnoreCase(selectedStatus) ? amber : surface);
        btnStatusInProgress.setTextColor("IN PROGRESS".equalsIgnoreCase(selectedStatus) ? dark : textSec);

        btnStatusDelayed.setBackgroundColor("DELAYED".equalsIgnoreCase(selectedStatus) ? amber : surface);
        btnStatusDelayed.setTextColor("DELAYED".equalsIgnoreCase(selectedStatus) ? dark : textSec);

        btnStatusCompleted.setBackgroundColor("COMPLETED".equalsIgnoreCase(selectedStatus) ? amber : surface);
        btnStatusCompleted.setTextColor("COMPLETED".equalsIgnoreCase(selectedStatus) ? dark : textSec);
    }

    private void setupVoiceDictation() {
        voiceManager = new VoiceDictationManager(requireContext());
        btnVoiceProgressNotes.setOnClickListener(v -> {
            if (!isListening) {
                btnVoiceProgressNotes.setBackgroundResource(R.drawable.btn_mic_dictate_active);
                isListening = true;
                voiceManager.startListening(new VoiceDictationManager.OnDictationListener() {
                    @Override
                    public void onSpeechRmsChanged(float rmsdB) {}

                    @Override
                    public void onPartialTranscription(String partialText) {
                        etProgressNotes.setText(partialText);
                    }

                    @Override
                    public void onFinalTranscription(String finalText, File audioBackupFile) {
                        etProgressNotes.setText(finalText);
                        btnVoiceProgressNotes.setBackgroundResource(R.drawable.btn_mic_dictate);
                        isListening = false;
                    }

                    @Override
                    public void onError(String userFriendlyError) {
                        btnVoiceProgressNotes.setBackgroundResource(R.drawable.btn_mic_dictate);
                        isListening = false;
                    }
                });
            } else {
                voiceManager.stopListening();
                btnVoiceProgressNotes.setBackgroundResource(R.drawable.btn_mic_dictate);
                isListening = false;
            }
        });
    }

    @Override
    public void onDestroy() {
        if (voiceManager != null) {
            voiceManager.destroy();
        }
        super.onDestroy();
    }
}
