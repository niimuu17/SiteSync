package com.example.sitesync.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.InventoryItemEntity;
import com.example.sitesync.data.local.entities.InventoryLogEntity;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.databinding.DialogStockOutBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.UUID;

public class StockOutDialog {

    public interface OnStockUpdatedListener {
        void onStockUpdated();
    }

    public static void show(Context context, InventoryItemEntity item, OnStockUpdatedListener listener) {
        DialogStockOutBinding binding = DialogStockOutBinding.inflate(LayoutInflater.from(context));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        boolean isEquipment = "EQUIPMENT".equalsIgnoreCase(item.getCategory()) || "SAFETY_GEAR".equalsIgnoreCase(item.getCategory());
        binding.tvStockOutTitle.setText((isEquipment ? "Check-Out / Deploy: " : "Stock Out: ") + item.getName());
        binding.tvStockOutCurrent.setText("Available in Depot: " + (int) item.getCurrentStock() + " " + item.getUnit());
        if (isEquipment) {
            binding.etStockOutQuantity.setText("1");
        }

        binding.btnCancelStockOut.setOnClickListener(v -> dialog.dismiss());

        binding.btnConfirmStockOut.setOnClickListener(v -> {
            String qtyStr = binding.etStockOutQuantity.getText().toString().trim();
            if (qtyStr.isEmpty()) {
                binding.etStockOutQuantity.setError("Please enter quantity");
                return;
            }

            double qty;
            try {
                qty = Double.parseDouble(qtyStr);
                if (qty <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                binding.etStockOutQuantity.setError("Invalid quantity");
                return;
            }

            if (qty > item.getCurrentStock()) {
                binding.etStockOutQuantity.setError("Cannot deduct more than available (" + item.getCurrentStock() + ")");
                return;
            }

            String location = binding.etStockOutLocation.getText().toString().trim();
            if (location.isEmpty()) location = "Level 3 Perimeter Pour";

            HapticFeedbackUtil.confirm(context);
            long now = System.currentTimeMillis();

            AppDatabase db = AppDatabase.getDatabase(context);
            db.inventoryDao().adjustStock(item.getId(), -qty, now);

            // Log consumption
            InventoryLogEntity log = new InventoryLogEntity(
                    UUID.randomUUID().toString(),
                    item.getId(),
                    item.getName(),
                    "STOCK_OUT",
                    qty,
                    item.getUnit(),
                    "Marco D. (Foreman)",
                    location,
                    now,
                    false
            );
            db.inventoryLogDao().insert(log);

            // Cloud sync queue
            PendingSyncEntity syncItem = new PendingSyncEntity(
                    UUID.randomUUID().toString(),
                    "INVENTORY",
                    item.getId(),
                    "UPDATE",
                    "{\"action\":\"STOCK_OUT\",\"qty\":" + qty + ",\"location\":\"" + location + "\"}",
                    now
            );
            db.pendingSyncDao().insert(syncItem);

            Toast.makeText(context, "Stock Out Recorded: -" + qty + " " + item.getUnit(), Toast.LENGTH_SHORT).show();

            if (listener != null) listener.onStockUpdated();
            dialog.dismiss();
        });

        dialog.show();
    }
}
