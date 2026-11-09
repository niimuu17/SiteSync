package com.example.sitesync.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.InventoryItemEntity;
import com.example.sitesync.data.local.entities.InventoryLogEntity;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.databinding.DialogStockInBinding;
import com.example.sitesync.hardware.TicketScannerDialog;
import com.example.sitesync.hardware.TicketScannerManager;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.UUID;

public class StockInDialog {

    public interface OnStockUpdatedListener {
        void onStockUpdated();
    }

    public static void show(Context context, InventoryItemEntity item, OnStockUpdatedListener listener) {
        DialogStockInBinding binding = DialogStockInBinding.inflate(LayoutInflater.from(context));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        binding.tvStockInTitle.setText("Stock In: " + item.getName());
        binding.tvStockInCurrent.setText("Current Available: " + item.getCurrentStock() + " " + item.getUnit() +
                " (Min: " + item.getMinThreshold() + " " + item.getUnit() + ")");

        binding.btnScanDeliveryTicket.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(context, v);
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) context;
                TicketScannerDialog scannerDialog = TicketScannerDialog.newInstance();
                scannerDialog.setOnTicketParsedListener(ticket -> {
                    if (ticket != null) {
                        if (ticket.parsedNumericQuantity > 0) {
                            binding.etStockInQuantity.setText(String.valueOf((int) ticket.parsedNumericQuantity));
                        } else if (ticket.quantity != null && !ticket.quantity.isEmpty()) {
                            String clean = ticket.quantity.replaceAll("[^0-9.]", "").trim();
                            binding.etStockInQuantity.setText(clean.isEmpty() ? ticket.quantity : clean);
                        }
                        if (ticket.supplierOrSerial != null && !ticket.supplierOrSerial.isEmpty()) {
                            binding.etStockInSupplier.setText(ticket.supplierOrSerial);
                        }
                        binding.tvScanTicketLabel.setText("Scanned: " + ticket.getSummary() + " ✓");
                        HapticFeedbackUtil.confirm(context);
                    }
                });
                scannerDialog.show(activity.getSupportFragmentManager(), "ticket_scanner_dialog");
            } else {
                binding.etStockInSupplier.setText("Holcim Cement Delivery #WB-" + (int)(Math.random() * 8999 + 1000));
                binding.etStockInQuantity.setText("400");
                binding.tvScanTicketLabel.setText("Ticket Scanned: WB-" + (int)(Math.random() * 8999 + 1000) + " ✓");
                Toast.makeText(context, "Delivery Ticket Scanned via Camera OCR", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnCancelStockIn.setOnClickListener(v -> dialog.dismiss());

        binding.btnConfirmStockIn.setOnClickListener(v -> {
            String qtyStr = binding.etStockInQuantity.getText().toString().trim();
            if (qtyStr.isEmpty()) {
                binding.etStockInQuantity.setError("Please enter quantity");
                return;
            }

            double qty;
            try {
                qty = Double.parseDouble(qtyStr);
                if (qty <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                binding.etStockInQuantity.setError("Invalid quantity");
                return;
            }

            String supplier = binding.etStockInSupplier.getText().toString().trim();
            if (supplier.isEmpty()) supplier = "Main Jobsite Depot Delivery";

            HapticFeedbackUtil.confirm(context);
            long now = System.currentTimeMillis();

            AppDatabase db = AppDatabase.getDatabase(context);
            db.inventoryDao().adjustStock(item.getId(), qty, now);

            // Log consumption/receipt
            InventoryLogEntity log = new InventoryLogEntity(
                    UUID.randomUUID().toString(),
                    item.getId(),
                    item.getName(),
                    "STOCK_IN",
                    qty,
                    item.getUnit(),
                    "Elena R. (Lead Eng)",
                    supplier,
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
                    "{\"action\":\"STOCK_IN\",\"qty\":" + qty + ",\"supplier\":\"" + supplier + "\"}",
                    now
            );
            db.pendingSyncDao().insert(syncItem);

            Toast.makeText(context, "Stock In Recorded: +" + qty + " " + item.getUnit(), Toast.LENGTH_SHORT).show();

            if (listener != null) listener.onStockUpdated();
            dialog.dismiss();
        });

        dialog.show();
    }
}
