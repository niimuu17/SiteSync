package com.example.sitesync.ui.challan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.StockTransaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockLedgerAdapter extends RecyclerView.Adapter<StockLedgerAdapter.ViewHolder> {

    private final List<StockTransaction> transactions = new ArrayList<>();
    private final NumberFormat bdtFormat = NumberFormat.getNumberInstance(Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.US);

    public void setTransactions(List<StockTransaction> newTransactions) {
        transactions.clear();
        if (newTransactions != null) {
            transactions.addAll(newTransactions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_ledger_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StockTransaction tx = transactions.get(position);

        boolean isStockIn = tx.isStockIn();

        if (isStockIn) {
            holder.tvTxTypeBadge.setText("STOCK IN");
            holder.tvTxTypeBadge.setBackgroundResource(R.drawable.bg_badge_green);
            holder.tvTxTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_green_text));

            holder.tvTxQuantity.setText(String.format(Locale.US, "+ %.1f %s", tx.getQuantity(), tx.getUnit()));
            holder.tvTxQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_on_dark_primary));

            holder.tvChallanOrRef.setText(tx.getChallanNumber() != null && !tx.getChallanNumber().isEmpty() ?
                    tx.getChallanNumber() : "DIRECT DELIVERY");
            holder.tvTxParty.setText(tx.getSupplierOrReceiver() != null ? "Supplier: " + tx.getSupplierOrReceiver() : "Supplier");
        } else {
            holder.tvTxTypeBadge.setText("STOCK OUT");
            holder.tvTxTypeBadge.setBackgroundResource(R.drawable.bg_badge_coral);
            holder.tvTxTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.alert_coral_text));

            holder.tvTxQuantity.setText(String.format(Locale.US, "- %.1f %s", tx.getQuantity(), tx.getUnit()));
            holder.tvTxQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.alert_coral_text));

            holder.tvChallanOrRef.setText("DISPATCH / POUR");
            holder.tvTxParty.setText(tx.getSupplierOrReceiver() != null ? "Crew: " + tx.getSupplierOrReceiver() : "Site Crew");
        }

        holder.tvTxItemName.setText(tx.getItemName());
        holder.tvTxDate.setText(dateFormat.format(new Date(tx.getTimestamp())));
        holder.tvTxCost.setText(String.format(Locale.US, "৳ %s", bdtFormat.format(Math.round(tx.getTotalCost()))));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTxTypeBadge;
        final TextView tvChallanOrRef;
        final TextView tvTxDate;
        final TextView tvTxItemName;
        final TextView tvTxQuantity;
        final TextView tvTxParty;
        final TextView tvTxCost;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTxTypeBadge = itemView.findViewById(R.id.tvTxTypeBadge);
            tvChallanOrRef = itemView.findViewById(R.id.tvChallanOrRef);
            tvTxDate = itemView.findViewById(R.id.tvTxDate);
            tvTxItemName = itemView.findViewById(R.id.tvTxItemName);
            tvTxQuantity = itemView.findViewById(R.id.tvTxQuantity);
            tvTxParty = itemView.findViewById(R.id.tvTxParty);
            tvTxCost = itemView.findViewById(R.id.tvTxCost);
        }
    }
}
