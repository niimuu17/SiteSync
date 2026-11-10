package com.example.sitesync.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.HazardTicketEntity;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.databinding.DialogResolveHazardBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.UUID;

/**
 * Bottom Sheet Dialog to record corrective action and mark a hazard as resolved.
 */
public class ResolveHazardDialog extends BottomSheetDialogFragment {

    private DialogResolveHazardBinding binding;
    private HazardTicketEntity ticket;
    private OnHazardResolvedListener listener;

    public interface OnHazardResolvedListener {
        void onHazardResolved(HazardTicketEntity ticket);
    }

    public static ResolveHazardDialog newInstance(HazardTicketEntity ticket) {
        ResolveHazardDialog dialog = new ResolveHazardDialog();
        dialog.ticket = ticket;
        return dialog;
    }

    public void setOnHazardResolvedListener(OnHazardResolvedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogResolveHazardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ticket != null) {
            binding.tvResolveTargetSummary.setText("Ticket #" + ticket.getId() + " • " + ticket.getCategory() + " (" + ticket.getLocationName() + ")");
        }

        binding.btnConfirmResolve.setOnClickListener(v -> {
            String corrective = binding.etCorrectiveAction.getText().toString().trim();
            if (corrective.isEmpty()) {
                binding.etCorrectiveAction.setError("Please describe corrective action taken");
                binding.etCorrectiveAction.requestFocus();
                return;
            }

            if (ticket != null) {
                ticket.setStatus("RESOLVED");
                ticket.setResolutionNotes(corrective);
                ticket.setResolvedAt(System.currentTimeMillis());

                // Update Room
                AppDatabase db = AppDatabase.getDatabase(requireContext());
                db.hazardTicketDao().update(ticket);

                // Queue Sync
                PendingSyncEntity sync = new PendingSyncEntity(
                        UUID.randomUUID().toString(),
                        "HAZARD",
                        ticket.getId(),
                        "UPDATE",
                        "{\"id\":\"" + ticket.getId() + "\",\"status\":\"RESOLVED\",\"action\":\"" + corrective + "\"}",
                        System.currentTimeMillis()
                );
                db.pendingSyncDao().insert(sync);

                HapticFeedbackUtil.confirm(requireContext());
                Toast.makeText(requireContext(), "Hazard resolved", Toast.LENGTH_SHORT).show();

                if (listener != null) {
                    listener.onHazardResolved(ticket);
                }
            }

            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
