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
import android.widget.ProgressBar;
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

/**
 * Feature: Fast Ticket & Chalan OCR Scanner Dialog.
 * CameraX live preview + ML Kit Text & Barcode recognition + Still capture & Gallery fallback.
 */
public class TicketScannerDialog extends DialogFragment {

    public interface OnTicketParsedListener {
        void onTicketParsed(TicketScannerManager.DeliveryTicket ticket);
    }

    private TicketScannerManager ticketScannerManager;
    private OnTicketParsedListener parsedListener;

    private PreviewView previewView;
    private ImageButton btnClose;
    private ImageButton btnToggleFlash;
    private MaterialButton btnSnapChalan;
    private MaterialButton btnPickFromGallery;
    private MaterialButton btnManual;
    private TextView tvOcrLiveText;
    private ProgressBar pbOcrScanning;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> galleryPickerLauncher;

    public static TicketScannerDialog newInstance() {
        return new TicketScannerDialog();
    }

    public void setOnTicketParsedListener(OnTicketParsedListener listener) {
        this.parsedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register camera permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCameraScanner();
                    } else {
                        Toast.makeText(requireContext(), "Camera permission required for OCR scanner. You can also pick from Gallery.", Toast.LENGTH_LONG).show();
                        if (tvOcrLiveText != null) {
                            tvOcrLiveText.setText("Camera permission denied. Use Gallery or Manual entry below.");
                        }
                    }
                }
        );

        // Register gallery picker launcher
        galleryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (Uri uri) -> {
                    if (uri != null && ticketScannerManager != null) {
                        if (tvOcrLiveText != null) {
                            tvOcrLiveText.setText("Analyzing selected chalan image...");
                        }
                        if (pbOcrScanning != null) {
                            pbOcrScanning.setVisibility(View.VISIBLE);
                        }
                        ticketScannerManager.processGalleryUri(uri, createTicketCallback("Gallery Chalan Parsed"));
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
        return inflater.inflate(R.layout.dialog_ticket_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.scannerPreviewView);
        btnClose = view.findViewById(R.id.btnCloseScanner);
        btnToggleFlash = view.findViewById(R.id.btnToggleFlash);
        btnSnapChalan = view.findViewById(R.id.btnSnapChalan);
        btnPickFromGallery = view.findViewById(R.id.btnPickFromGallery);
        btnManual = view.findViewById(R.id.btnManualEntry);
        tvOcrLiveText = view.findViewById(R.id.tvOcrLiveText);
        pbOcrScanning = view.findViewById(R.id.pbOcrScanning);

        ticketScannerManager = new TicketScannerManager(requireContext());

        // Live OCR detection preview listener
        ticketScannerManager.setOnLiveTextDetectedListener(snippet -> {
            if (isAdded() && tvOcrLiveText != null) {
                tvOcrLiveText.setText(snippet);
            }
        });

        // Close button
        btnClose.setOnClickListener(v -> dismiss());

        // Flashlight toggle
        btnToggleFlash.setOnClickListener(v -> {
            boolean isOn = ticketScannerManager.toggleTorch();
            HapticFeedbackUtil.lightClick(requireContext(), v);
            btnToggleFlash.setColorFilter(isOn ? Color.YELLOW : Color.WHITE);
        });

        // Snap & Parse Still Button
        btnSnapChalan.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            btnSnapChalan.setEnabled(false);
            btnSnapChalan.setText("🔍  Processing Chalan OCR...");
            if (pbOcrScanning != null) {
                pbOcrScanning.setVisibility(View.VISIBLE);
            }
            ticketScannerManager.snapAndParse(new TicketScannerManager.OnTicketScannedListener() {
                @Override
                public void onTicketParsed(TicketScannerManager.DeliveryTicket ticket) {
                    deliverResult(ticket);
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        btnSnapChalan.setEnabled(true);
                        btnSnapChalan.setText("📸  Snap & Parse Chalan");
                        if (pbOcrScanning != null) {
                            pbOcrScanning.setVisibility(View.GONE);
                        }
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        if (tvOcrLiveText != null) {
                            tvOcrLiveText.setText(error);
                        }
                    }
                }
            });
        });

        // Pick from Gallery
        btnPickFromGallery.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            galleryPickerLauncher.launch("image/*");
        });

        // Manual Entry Fallback
        btnManual.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            dismiss();
            if (parsedListener != null) {
                TicketScannerManager.DeliveryTicket manualTicket = new TicketScannerManager.DeliveryTicket();
                manualTicket.mixId = "Portland Composite Cement (PCC)";
                manualTicket.quantity = "200 Bags";
                manualTicket.parsedNumericQuantity = 200;
                manualTicket.parsedUnit = "Bags";
                manualTicket.supplierOrSerial = "Holcim / CEMEX Supply";
                parsedListener.onTicketParsed(manualTicket);
            }
        });

        // Check Camera permission and start
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCameraScanner() {
        if (previewView == null || ticketScannerManager == null) return;

        ticketScannerManager.startScanner(getViewLifecycleOwner(), previewView, createTicketCallback("Live Chalan Detected"));
    }

    private TicketScannerManager.OnTicketScannedListener createTicketCallback(String tag) {
        return new TicketScannerManager.OnTicketScannedListener() {
            @Override
            public void onTicketParsed(TicketScannerManager.DeliveryTicket ticket) {
                deliverResult(ticket);
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    if (tvOcrLiveText != null) {
                        tvOcrLiveText.setText(error);
                    }
                    if (pbOcrScanning != null) {
                        pbOcrScanning.setVisibility(View.GONE);
                    }
                    if (btnSnapChalan != null) {
                        btnSnapChalan.setEnabled(true);
                        btnSnapChalan.setText("📸  Snap & Parse Chalan");
                    }
                }
            }
        };
    }

    private void deliverResult(TicketScannerManager.DeliveryTicket ticket) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), "Chalan Parsed: " + ticket.getSummary(), Toast.LENGTH_SHORT).show();
        dismissAllowingStateLoss();
        if (parsedListener != null) {
            parsedListener.onTicketParsed(ticket);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ticketScannerManager != null) {
            ticketScannerManager.destroy();
        }
    }
}
