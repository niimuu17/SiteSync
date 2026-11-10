package com.example.sitesync.ui.challan;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.InventoryItem;
import com.example.sitesync.data.local.entities.StockTransaction;
import com.example.sitesync.data.repository.InventoryRepository;
import com.example.sitesync.databinding.DialogRecordStockOutBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class RecordStockOutBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnStockOutCommittedListener {
        void onStockOutCommitted(StockTransaction transaction);
    }

    private DialogRecordStockOutBinding binding;
    private InventoryItem inventoryItem;
    private OnStockOutCommittedListener listener;

    public static RecordStockOutBottomSheetDialog newInstance(InventoryItem item) {
        RecordStockOutBottomSheetDialog dialog = new RecordStockOutBottomSheetDialog();
        dialog.inventoryItem = item;
        return dialog;
    }

    public void setOnStockOutCommittedListener(OnStockOutCommittedListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            View bottomSheet = ((BottomSheetDialog) dialog).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            View bottomSheet = ((BottomSheetDialog) d).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogRecordStockOutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (inventoryItem != null) {
            binding.tvOutMaterialName.setText(inventoryItem.getItemName());
            binding.tvOutAvailableBalance.setText(String.format(Locale.US,
                    "Available Balance: %.1f %s", inventoryItem.getCurrentBalance(), inventoryItem.getUnit()));
        }

        binding.btnCancelStockOut.setOnClickListener(v -> dismiss());
        binding.btnConfirmStockOut.setOnClickListener(v -> commitStockOut());
    }

    private void commitStockOut() {
        if (inventoryItem == null) {
            dismiss();
            return;
        }

        String qtyStr = binding.etOutQuantity.getText() != null ? binding.etOutQuantity.getText().toString().trim() : "";
        if (qtyStr.isEmpty()) {
            binding.etOutQuantity.setError("Please enter quantity");
            return;
        }

        double qty;
        try {
            qty = Double.parseDouble(qtyStr);
            if (qty <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            binding.etOutQuantity.setError("Invalid quantity");
            return;
        }

        if (qty > inventoryItem.getCurrentBalance()) {
            binding.etOutQuantity.setError("Exceeds current balance (" + inventoryItem.getCurrentBalance() + ")");
            return;
        }

        String purpose = binding.etOutPurpose.getText() != null ?
                binding.etOutPurpose.getText().toString().trim() : "Site Pouring / Usage";
        String receiver = binding.etOutReceiver.getText() != null ?
                binding.etOutReceiver.getText().toString().trim() : "Site Work Crew";

        binding.btnConfirmStockOut.setEnabled(false);

        InventoryRepository.getInstance(requireContext()).recordStockOut(
                inventoryItem.getId(),
                "Metro Central Tower - Site A",
                qty,
                receiver,
                purpose,
                new InventoryRepository.ActionCallback<StockTransaction>() {
                    @Override
                    public void onSuccess(StockTransaction result) {
                        if (!isAdded()) return;
                        HapticFeedbackUtil.confirm(requireContext());
                        Toast.makeText(requireContext(),
                                "Stock-out recorded: -" + qty + " " + inventoryItem.getUnit(),
                                Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onStockOutCommitted(result);
                        }
                        dismiss();
                    }

                    @Override
                    public void onError(Exception error) {
                        if (!isAdded()) return;
                        binding.btnConfirmStockOut.setEnabled(true);
                        Toast.makeText(requireContext(), "Failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
