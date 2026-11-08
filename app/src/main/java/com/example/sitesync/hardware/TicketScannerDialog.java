package com.example.sitesync.hardware;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.DialogFragment;

import com.example.sitesync.R;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.button.MaterialButton;

/**
 * Feature 5: Fast Ticket & Barcode Scanner Dialog
 * CameraX + ML Kit barcode recognition for concrete delivery tickets and supplier QR codes.
 */
public class TicketScannerDialog extends DialogFragment {

    public interface OnTicketParsedListener {
        void onTicketParsed(TicketScannerManager.DeliveryTicket ticket);
    }

    private TicketScannerManager ticketScannerManager;
    private OnTicketParsedListener parsedListener;

    private PreviewView previewView;
    private ImageButton btnClose;
    private MaterialButton btnManual;

    public static TicketScannerDialog newInstance() {
        return new TicketScannerDialog();
    }

    public void setOnTicketParsedListener(OnTicketParsedListener listener) {
        this.parsedListener = listener;
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
        btnManual = view.findViewById(R.id.btnManualEntry);

        ticketScannerManager = new TicketScannerManager(requireContext());

        ticketScannerManager.startScanner(this, previewView, new TicketScannerManager.OnTicketScannedListener() {
            @Override
            public void onTicketParsed(TicketScannerManager.DeliveryTicket ticket) {
                Toast.makeText(requireContext(), "Ticket Scanned: " + ticket.getSummary(), Toast.LENGTH_SHORT).show();
                dismiss();
                if (parsedListener != null) {
                    parsedListener.onTicketParsed(ticket);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        btnManual.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            dismiss();
            if (parsedListener != null) {
                TicketScannerManager.DeliveryTicket manualTicket = new TicketScannerManager.DeliveryTicket();
                manualTicket.mixId = "Ready-Mix C30/37";
                manualTicket.quantity = "7.5 m³";
                manualTicket.slump = "120 mm";
                manualTicket.supplierOrSerial = "Titan Concrete Supply Ltd.";
                parsedListener.onTicketParsed(manualTicket);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ticketScannerManager != null) {
            ticketScannerManager.stopScanner();
        }
    }
}
