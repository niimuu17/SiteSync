package com.example.sitesync.hardware;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.sitesync.R;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.io.File;

/**
 * Feature: "Match Angle" Progress Comparison Camera Screen/Dialog
 * Allows field workers to align current angle against a previous baseline photo
 * and burns tamper-proof forensic telemetry (GPS, UTC time, elevation, inspector ID) onto the photo.
 */
public class MatchAngleCameraDialog extends DialogFragment {

    public interface OnPhotoVerifiedCallback {
        void onPhotoVerified(File photoFile, Uri photoUri);
    }

    private String baselinePhotoUri = "";
    private String userName = "ELENA ROSTOVA (ENG-04)";
    private String taskLocation = "Metro Central Tower";

    private CameraOverlayManager cameraManager;
    private OnPhotoVerifiedCallback callback;

    private PreviewView previewView;
    private ImageView ivGhostOverlay;
    private Slider sliderOpacity;
    private TextView tvOpacityLabel;
    private MaterialButton btnCapture;
    private ImageButton btnClose;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    public static MatchAngleCameraDialog newInstance(String baselineUri, String userName, String location) {
        MatchAngleCameraDialog dialog = new MatchAngleCameraDialog();
        dialog.baselinePhotoUri = baselineUri != null ? baselineUri : "";
        if (userName != null && !userName.isEmpty()) dialog.userName = userName;
        if (location != null && !location.isEmpty()) dialog.taskLocation = location;
        return dialog;
    }

    public void setOnPhotoVerifiedCallback(OnPhotoVerifiedCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        initCamera();
                    } else {
                        Toast.makeText(requireContext(), "Camera permission required for site photo capture", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_match_angle_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.previewView);
        ivGhostOverlay = view.findViewById(R.id.ivGhostOverlay);
        sliderOpacity = view.findViewById(R.id.sliderOpacity);
        tvOpacityLabel = view.findViewById(R.id.tvOpacityLabel);
        btnCapture = view.findViewById(R.id.btnCaptureShutter);
        btnClose = view.findViewById(R.id.btnCloseCamera);

        cameraManager = new CameraOverlayManager(requireContext());

        // Load Ghost Baseline Image if provided
        cameraManager.setGhostBaselineImage(ivGhostOverlay, baselinePhotoUri);

        // Opacity Slider Listener
        sliderOpacity.addOnChangeListener((slider, value, fromUser) -> {
            float alpha = value / 100f;
            ivGhostOverlay.setAlpha(alpha);
            tvOpacityLabel.setText("Overlay Opacity: " + (int) value + "%");
        });

        // Capture Shutter Button
        btnCapture.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            btnCapture.setEnabled(false);
            btnCapture.setText("Burning Telemetry...");

            cameraManager.captureWatermarkedPhoto(
                    taskLocation, userName,
                    new CameraOverlayManager.OnPhotoCapturedCallback() {
                        @Override
                        public void onSuccess(File capturedFile, Uri fileUri) {
                            if (!isAdded()) return;
                            HapticFeedbackUtil.scanSuccess(requireContext());
                            Toast.makeText(requireContext(), "Photo verified & forensic watermark burned", Toast.LENGTH_SHORT).show();
                            dismissAllowingStateLoss();
                            if (callback != null) {
                                callback.onPhotoVerified(capturedFile, fileUri);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (!isAdded()) return;
                            btnCapture.setEnabled(true);
                            btnCapture.setText("Capture Verified Photo");
                            Toast.makeText(requireContext(), "Camera error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        btnClose.setOnClickListener(v -> dismiss());

        // Check Camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initCamera() {
        if (previewView == null || cameraManager == null) return;
        cameraManager.startCamera(getViewLifecycleOwner(), previewView, () -> {
            // Camera ready
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraManager != null) {
            cameraManager.shutdown();
        }
    }
}
