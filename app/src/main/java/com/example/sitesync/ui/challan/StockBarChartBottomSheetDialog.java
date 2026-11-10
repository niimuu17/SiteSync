package com.example.sitesync.ui.challan;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.InventoryItem;
import com.example.sitesync.data.network.MaterialImageResolver;
import com.example.sitesync.databinding.DialogStockBarChartBinding;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bottom sheet dialog presenting the complete material balance horizontal bar chart
 * and detailed quantity ledger for all site materials.
 */
public class StockBarChartBottomSheetDialog extends BottomSheetDialogFragment {

    private DialogStockBarChartBinding binding;
    private List<InventoryItem> items = new ArrayList<>();
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

    public static StockBarChartBottomSheetDialog newInstance(List<InventoryItem> items) {
        StockBarChartBottomSheetDialog dialog = new StockBarChartBottomSheetDialog();
        if (items != null) {
            dialog.items = new ArrayList<>(items);
        }
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogStockBarChartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCloseBarChartDialog.setOnClickListener(v -> dismiss());
        binding.btnDoneBarChart.setOnClickListener(v -> dismiss());

        setupHorizontalBarChart();
        populateDetailedLedger();
    }

    private void setupHorizontalBarChart() {
        HorizontalBarChart chart = binding.barChartStockBalances;
        if (items.isEmpty()) {
            chart.clear();
            return;
        }

        // Dynamically size chart height based on item count
        int calculatedHeightDp = Math.max(260, items.size() * 52);
        float density = getResources().getDisplayMetrics().density;
        chart.getLayoutParams().height = (int) (calculatedHeightDp * density);
        chart.requestLayout();

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            entries.add(new BarEntry(i, (float) item.getCurrentBalance()));
            // Abbreviate very long titles for axis readability
            String shortName = item.getItemName();
            if (shortName.length() > 22) {
                shortName = shortName.substring(0, 20) + "…";
            }
            labels.add(shortName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Available Balance");
        dataSet.setColor(Color.parseColor("#F5C29B")); // Soft Peach Accent
        dataSet.setValueTextColor(Color.parseColor("#F9F5EB")); // On-dark primary
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return numberFormat.format(Math.round(value));
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chart.setData(barData);

        // Chart styling
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setFitBars(true);

        // In HorizontalBarChart, XAxis is the vertical category axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size());
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(Color.parseColor("#F9F5EB"));
        xAxis.setTextSize(10f);

        chart.getAxisLeft().setTextColor(Color.parseColor("#89A19A"));
        chart.getAxisLeft().setTextSize(9f);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setSpaceTop(25f);
        chart.getAxisLeft().setGridColor(Color.parseColor("#35574F"));

        chart.getAxisRight().setEnabled(false);
        chart.setExtraRightOffset(36f);

        chart.animateY(800);
        chart.invalidate();
    }

    private void populateDetailedLedger() {
        LinearLayout container = binding.layoutStockDetailsList;
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        float density = getResources().getDisplayMetrics().density;

        for (InventoryItem item : items) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = (int) (10 * density);
            card.setLayoutParams(cardParams);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.forest_surface_deep));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.forest_stroke));
            card.setStrokeWidth((int) (1 * density));
            card.setRadius(14 * density);
            card.setCardElevation(0);

            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (14 * density);
            content.setPadding(pad, pad, pad, pad);

            // Row 1: Squircle Icon + Title + Status Badge
            LinearLayout row1 = new LinearLayout(requireContext());
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Icon card
            MaterialCardView iconCard = new MaterialCardView(requireContext());
            int iconBoxSize = (int) (38 * density);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconBoxSize, iconBoxSize);
            iconParams.rightMargin = (int) (10 * density);
            iconCard.setLayoutParams(iconParams);
            iconCard.setRadius(10 * density);
            iconCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.forest_card_dark));
            iconCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.forest_stroke));
            iconCard.setStrokeWidth((int) (1 * density));
            iconCard.setCardElevation(0);

            ImageView ivIcon = new ImageView(requireContext());
            ivIcon.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iconCard.addView(ivIcon);
            row1.addView(iconCard);

            MaterialImageResolver.loadProductIcon(ivIcon, item.getItemName(), item.getCategory(), item.getImageUrl());

            // Name + Category
            LinearLayout nameBox = new LinearLayout(requireContext());
            nameBox.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            nameBox.setLayoutParams(nameParams);

            TextView tvName = new TextView(requireContext());
            tvName.setText(item.getItemName());
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_dark_primary));
            tvName.setTextSize(13);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            nameBox.addView(tvName);

            TextView tvCat = new TextView(requireContext());
            tvCat.setText(item.getCategory());
            tvCat.setTextColor(ContextCompat.getColor(requireContext(), R.color.peach_accent));
            tvCat.setTextSize(10);
            tvCat.setTypeface(null, android.graphics.Typeface.BOLD);
            nameBox.addView(tvCat);

            row1.addView(nameBox);

            // Health Badge
            TextView tvBadge = new TextView(requireContext());
            tvBadge.setTextSize(10);
            tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            tvBadge.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
            if (item.isLowStock()) {
                tvBadge.setText("Low Stock");
                tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_coral_text));
                tvBadge.setBackgroundResource(R.drawable.bg_badge_coral);
            } else {
                tvBadge.setText("Healthy");
                tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_green_text));
                tvBadge.setBackgroundResource(R.drawable.bg_badge_green);
            }
            row1.addView(tvBadge);
            content.addView(row1);

            // Row 2: Balance numbers
            LinearLayout row2 = new LinearLayout(requireContext());
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams row2Params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            row2Params.topMargin = (int) (10 * density);
            row2.setLayoutParams(row2Params);

            TextView tvBal = new TextView(requireContext());
            tvBal.setText(String.format(Locale.US, "Remaining: %.1f %s", item.getCurrentBalance(), item.getUnit()));
            tvBal.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_dark_primary));
            tvBal.setTextSize(12);
            tvBal.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams balParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tvBal.setLayoutParams(balParams);
            row2.addView(tvBal);

            TextView tvLedger = new TextView(requireContext());
            tvLedger.setText(String.format(Locale.US, "In: %.1f | Out: %.1f %s", item.getTotalStockIn(), item.getTotalStockOut(), item.getUnit()));
            tvLedger.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_dark_muted));
            tvLedger.setTextSize(11);
            row2.addView(tvLedger);

            content.addView(row2);
            card.addView(content);
            container.addView(card);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
