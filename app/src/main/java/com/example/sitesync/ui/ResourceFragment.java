package com.example.sitesync.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.entities.InventoryItem;
import com.example.sitesync.data.local.entities.StockTransaction;
import com.example.sitesync.data.repository.InventoryRepository;
import com.example.sitesync.databinding.FragmentResourceBinding;
import com.example.sitesync.hardware.ChallanOcrVisionClient;
import com.example.sitesync.ui.challan.ChallanReviewBottomSheetDialog;
import com.example.sitesync.ui.challan.InventoryStockAdapter;
import com.example.sitesync.ui.challan.RecordStockOutBottomSheetDialog;
import com.example.sitesync.ui.challan.StockBarChartBottomSheetDialog;
import com.example.sitesync.ui.challan.StockLedgerAdapter;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Enterprise Single Unified Inventory & Delivery Dashboard.
 * Re-engineered from the ground up to support multimodal Challan OCR,
 * Brave Search market benchmarking, dynamic stock balances, and MPAndroidChart analytics.
 */
public class ResourceFragment extends Fragment implements InventoryStockAdapter.OnStockItemActionListener {

    private FragmentResourceBinding binding;
    private InventoryRepository repository;
    private SessionManager sessionManager;
    private ChallanOcrVisionClient visionClient;
    private String activeProjectCode;

    private InventoryStockAdapter stockAdapter;
    private StockLedgerAdapter ledgerAdapter;

    private List<InventoryItem> currentInventory = new ArrayList<>();
    private List<StockTransaction> allTransactions = new ArrayList<>();

    private Uri cameraCaptureUri;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private final NumberFormat bdtFormat = NumberFormat.getNumberInstance(Locale.US);

    public static ResourceFragment newInstance() {
        return new ResourceFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Activity Result Handlers for Camera and Gallery
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraCaptureUri != null) {
                        processChallanImage(cameraCaptureUri);
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processChallanImage(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentResourceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        activeProjectCode = sessionManager.getActiveProjectCode();
        if (activeProjectCode == null || activeProjectCode.isEmpty()) {
            activeProjectCode = "PRJ-DHAKA-101";
        }

        repository = InventoryRepository.getInstance(requireContext());
        visionClient = new ChallanOcrVisionClient(requireContext());

        setupAdapters();
        setupActionButtons();
        setupFilterChips();
        observeDatabaseData();

        // Seed initial Bangladesh construction materials if Room database is empty for this project
        repository.seedDefaultMaterialsForProjectIfEmpty(activeProjectCode, new InventoryRepository.ActionCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean seeded) {
                // Observers will automatically refresh UI
            }

            @Override
            public void onError(Exception error) {
                // Ignore
            }
        });
    }

    private void setupAdapters() {
        stockAdapter = new InventoryStockAdapter(this);
        binding.rvActiveStockItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvActiveStockItems.setAdapter(stockAdapter);

        ledgerAdapter = new StockLedgerAdapter();
        binding.rvMovementLedger.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMovementLedger.setAdapter(ledgerAdapter);
    }

    private void setupActionButtons() {
        binding.btnScanChallanOcr.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            showChallanSourceDialog();
        });

        binding.btnRecordStockOut.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            showQuickStockOutPicker();
        });

        binding.btnViewAllStocksChart.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            showCompleteStockBarChart();
        });

        binding.btnRequiredMaterialsMenu.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            RequiredMaterialsListBottomSheetDialog dialog = RequiredMaterialsListBottomSheetDialog.newInstance(activeProjectCode);
            dialog.show(getChildFragmentManager(), "RequiredMaterialsListDialog");
        });

        binding.btnExportExcelInventory.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            exportInventoryToExcel();
        });
    }

    private void exportInventoryToExcel() {
        if (currentInventory == null || currentInventory.isEmpty()) {
            Toast.makeText(requireContext(), "No inventory items to export", Toast.LENGTH_SHORT).show();
            return;
        }

        File reportsDir = com.example.sitesync.utils.ProjectStorageHelper.getReportsDir(requireContext(), activeProjectCode);

        File csvFile = new File(reportsDir, "SiteSync_Inventory_Ledger.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("ID,Item Name,Category,Unit,Current Balance,Avg Unit Price (BDT),Total Valuation (BDT),Total Stock In,Total Stock Out,Reorder Level,Status\n");
            for (InventoryItem item : currentInventory) {
                String status = item.isLowStock() ? "REORDER_ALERT" : "IN_STOCK";
                writer.write(String.format(Locale.US, "%d,\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                        item.getId(),
                        item.getItemName() != null ? item.getItemName().replace("\"", "\"\"") : "",
                        item.getCategory() != null ? item.getCategory() : "",
                        item.getUnit() != null ? item.getUnit() : "",
                        item.getCurrentBalance(),
                        item.getAvgUnitPrice(),
                        item.getValuation(),
                        item.getTotalStockIn(),
                        item.getTotalStockOut(),
                        item.getReorderLevel(),
                        status));
            }

            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", csvFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "SiteSync Construction Material Inventory Ledger");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Export Inventory as Excel Sheet"));
            Toast.makeText(requireContext(), "Opening Excel Export...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to export inventory: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterChips() {
        binding.chipGroupLedgerFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterStockIn) {
                filterLedger(StockTransaction.TYPE_STOCK_IN);
            } else if (checkedId == R.id.chipFilterStockOut) {
                filterLedger(StockTransaction.TYPE_STOCK_OUT);
            } else {
                ledgerAdapter.setTransactions(allTransactions);
            }
        });
    }

    private void filterLedger(String type) {
        List<StockTransaction> filtered = new ArrayList<>();
        for (StockTransaction tx : allTransactions) {
            if (type.equalsIgnoreCase(tx.getType())) {
                filtered.add(tx);
            }
        }
        ledgerAdapter.setTransactions(filtered);
    }

    private void observeDatabaseData() {
        String pCode = activeProjectCode != null ? activeProjectCode : "PRJ-DHAKA-101";

        repository.getItemsForProject(pCode).observe(getViewLifecycleOwner(), items -> {
            if (items == null) return;
            currentInventory = items;
            stockAdapter.setItems(items);
            binding.tvTotalItemsTracked.setText(items.size() + " Materials");

            updateValuationHeader();
            updatePieChart();
        });

        repository.getTransactionsForProject(pCode).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null) return;
            allTransactions = transactions;

            int checkedChip = binding.chipGroupLedgerFilter.getCheckedChipId();
            if (checkedChip == R.id.chipFilterStockIn) {
                filterLedger(StockTransaction.TYPE_STOCK_IN);
            } else if (checkedChip == R.id.chipFilterStockOut) {
                filterLedger(StockTransaction.TYPE_STOCK_OUT);
            } else {
                ledgerAdapter.setTransactions(transactions);
            }

            updateValuationHeader();
        });

        // Observe required items count to update badge on Required Items button
        repository.getPendingRequiredMaterials(pCode).observe(getViewLifecycleOwner(), requiredItems -> {
            int count = (requiredItems != null) ? requiredItems.size() : 0;
            if (binding.btnRequiredMaterialsMenu != null) {
                if (count > 0) {
                    binding.btnRequiredMaterialsMenu.setText("Required (" + count + ")");
                } else {
                    binding.btnRequiredMaterialsMenu.setText("Required Items");
                }
            }
        });
    }

    private void updateValuationHeader() {
        double totalValuation = 0.0;
        int lowStockCount = 0;

        for (InventoryItem item : currentInventory) {
            totalValuation += item.getValuation();
            if (item.isLowStock()) {
                lowStockCount++;
            }
        }

        binding.tvTotalInventoryValuation.setText(String.format(Locale.US, "$ %s", bdtFormat.format(Math.round(totalValuation))));

        if (lowStockCount > 0) {
            binding.tvLowStockAlertBadge.setText(lowStockCount + " Items Low Stock");
            binding.tvLowStockAlertBadge.setBackgroundResource(R.drawable.bg_badge_coral);
            binding.tvLowStockAlertBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_coral_text));
        } else {
            binding.tvLowStockAlertBadge.setText("All Stocks Healthy");
            binding.tvLowStockAlertBadge.setBackgroundResource(R.drawable.bg_badge_green);
            binding.tvLowStockAlertBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_green_text));
        }

        double totalInflow = 0.0;
        double totalOutflow = 0.0;
        for (StockTransaction tx : allTransactions) {
            if (tx.isStockIn()) {
                totalInflow += tx.getTotalCost();
            } else {
                totalOutflow += tx.getTotalCost();
            }
        }

        binding.tvTotalPurchases.setText(String.format(Locale.US, "Delivered: $ %s", bdtFormat.format(Math.round(totalInflow))));
        binding.tvTotalDispatches.setText(String.format(Locale.US, "Site Pouring: $ %s", bdtFormat.format(Math.round(totalOutflow))));
    }

    private void updatePieChart() {
        PieChart pie = binding.pieChartValuation;
        LinearLayout legendContainer = binding.layoutPieLegend;
        legendContainer.removeAllViews();

        if (currentInventory.isEmpty()) {
            pie.clear();
            binding.tvPieChartSubtitle.setText("0 Materials Tracked");
            return;
        }

        // Sort items descending by valuation
        List<InventoryItem> sorted = new ArrayList<>(currentInventory);
        Collections.sort(sorted, (a, b) -> Double.compare(b.getValuation(), a.getValuation()));

        double totalValuation = 0;
        for (InventoryItem it : sorted) {
            totalValuation += it.getValuation();
        }

        binding.tvPieChartSubtitle.setText(String.format(Locale.US, "Top %d of %d Materials", Math.min(4, sorted.size()), sorted.size()));

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int[] palette = new int[]{
                Color.parseColor("#F5C29B"), // Soft Peach
                Color.parseColor("#89A19A"), // Muted Teal
                Color.parseColor("#34524A"), // Forest Pill
                Color.parseColor("#E8AF84"), // Dark Peach
                Color.parseColor("#526B64")  // Sage Green
        };

        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < sorted.size(); i++) {
            InventoryItem item = sorted.get(i);
            double val = item.getValuation();
            if (val > 0) {
                entries.add(new PieEntry((float) val, ""));
                int color = palette[i % palette.length];
                colors.add(color);

                // Add to right legend if within top 4
                if (i < 4) {
                    addLegendItem(legendContainer, item, color, totalValuation, density);
                }
            }
        }

        if (sorted.size() > 4) {
            TextView tvMore = new TextView(requireContext());
            tvMore.setText(String.format(Locale.US, "+%d more materials (Tap View All)", sorted.size() - 4));
            tvMore.setTextColor(ContextCompat.getColor(requireContext(), R.color.peach_accent));
            tvMore.setTextSize(11);
            tvMore.setTypeface(null, android.graphics.Typeface.BOLD);
            tvMore.setPadding(0, (int) (4 * density), 0, 0);
            tvMore.setOnClickListener(v -> showCompleteStockBarChart());
            legendContainer.addView(tvMore);
        }

        if (entries.isEmpty()) {
            pie.clear();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2.5f);
        dataSet.setDrawValues(false); // Clean donut without overlapping text on slices

        PieData pieData = new PieData(dataSet);
        pie.setData(pieData);
        pie.setDrawEntryLabels(false); // NO OVERLAPPING ENTRY LABELS!
        pie.getDescription().setEnabled(false);
        pie.getLegend().setEnabled(false);

        // Donut Center
        pie.setDrawHoleEnabled(true);
        pie.setHoleColor(Color.parseColor("#2B443E"));
        pie.setHoleRadius(56f);
        pie.setTransparentCircleRadius(62f);
        pie.setTransparentCircleColor(Color.parseColor("#3D5E56"));

        // Donut Center Text: Formatted total
        String centerText;
        if (totalValuation >= 1_000_000) {
            centerText = String.format(Locale.US, "$%.2fM\nTotal", totalValuation / 1_000_000.0);
        } else if (totalValuation >= 1_000) {
            centerText = String.format(Locale.US, "$%.0fK\nTotal", totalValuation / 1_000.0);
        } else {
            centerText = String.format(Locale.US, "$%.0f\nTotal", totalValuation);
        }
        pie.setCenterText(centerText);
        pie.setCenterTextColor(Color.parseColor("#F9F5EB"));
        pie.setCenterTextSize(11f);

        pie.animateY(600);
        pie.invalidate();
    }

    private void addLegendItem(LinearLayout container, InventoryItem item, int color, double totalValuation, float density) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = (int) (6 * density);
        row.setLayoutParams(rowParams);

        // Color indicator pill
        View colorDot = new View(requireContext());
        int dotSize = (int) (10 * density);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotParams.rightMargin = (int) (8 * density);
        colorDot.setLayoutParams(dotParams);
        android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
        dotBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        dotBg.setCornerRadius(3 * density);
        dotBg.setColor(color);
        colorDot.setBackground(dotBg);
        row.addView(colorDot);

        // Name and percent/valuation column
        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(colParams);

        TextView tvName = new TextView(requireContext());
        tvName.setText(item.getItemName());
        tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_dark_primary));
        tvName.setTextSize(11);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setSingleLine(true);
        tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textCol.addView(tvName);

        double pct = totalValuation > 0 ? (item.getValuation() / totalValuation * 100.0) : 0;
        TextView tvSub = new TextView(requireContext());
        tvSub.setText(String.format(Locale.US, "%.1f%% • $ %s", pct, bdtFormat.format(Math.round(item.getValuation()))));
        tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_dark_muted));
        tvSub.setTextSize(10);
        textCol.addView(tvSub);

        row.addView(textCol);
        container.addView(row);
    }

    public void showCompleteStockBarChart() {
        if (currentInventory == null || currentInventory.isEmpty()) {
            if (repository != null) {
                repository.getAllItems().observe(getViewLifecycleOwner(), items -> {
                    if (items != null && !items.isEmpty() && getChildFragmentManager().findFragmentByTag("StockBarChartDialog") == null) {
                        currentInventory = items;
                        StockBarChartBottomSheetDialog dialog = StockBarChartBottomSheetDialog.newInstance(currentInventory);
                        dialog.show(getChildFragmentManager(), "StockBarChartDialog");
                    }
                });
            }
            return;
        }
        StockBarChartBottomSheetDialog dialog = StockBarChartBottomSheetDialog.newInstance(currentInventory);
        dialog.show(getChildFragmentManager(), "StockBarChartDialog");
    }

    private void showChallanSourceDialog() {
        String[] options = {
                "Take Photo of Challan",
                "Choose from Gallery",
                "Simulate Verified Challan (Shah Cement & BSRM Rebar)"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Delivery Challan Scanner")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchCameraCapture();
                    } else if (which == 1) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        simulateDemoChallan();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchCameraCapture() {
        try {
            File photoFile = File.createTempFile("challan_", ".jpg", requireContext().getCacheDir());
            cameraCaptureUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(cameraCaptureUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not create image file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processChallanImage(Uri uri) {
        ProgressDialog progress = new ProgressDialog(requireContext());
        progress.setMessage("Analyzing Challan with Gemini Vision AI & Benchmarking BDT Market Rates...");
        progress.setCancelable(false);
        progress.show();

        visionClient.processChallanUri(uri, new ChallanOcrVisionClient.OnChallanParsedListener() {
            @Override
            public void onSuccess(ChallanOcrVisionClient.ParsedChallan challan) {
                if (progress.isShowing()) progress.dismiss();
                if (!isAdded()) return;

                if (!challan.isValidReceipt || challan.items == null || challan.items.isEmpty()) {
                    showInvalidReceiptWarningDialog(challan.rejectionReason);
                    return;
                }

                // Deduplication Guard: Check if receipt has already been entered
                repository.checkDuplicateReceipt(challan.challanNumber, challan.getFingerprint(), challan.imageHash,
                        new InventoryRepository.ActionCallback<InventoryRepository.DuplicateCheckResult>() {
                            @Override
                            public void onSuccess(InventoryRepository.DuplicateCheckResult dupResult) {
                                if (!isAdded()) return;
                                if (dupResult.isDuplicate) {
                                    showDuplicateReceiptWarningDialog(challan, dupResult);
                                } else {
                                    openChallanReviewSheet(challan);
                                }
                            }

                            @Override
                            public void onError(Exception error) {
                                if (!isAdded()) return;
                                openChallanReviewSheet(challan);
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                if (progress.isShowing()) progress.dismiss();
                if (!isAdded()) return;
                showInvalidReceiptWarningDialog("Scan error: " + errorMessage);
            }
        });
    }

    void showDuplicateReceiptWarningDialog(ChallanOcrVisionClient.ParsedChallan challan,
                                           InventoryRepository.DuplicateCheckResult dupResult) {
        if (!isAdded() || getContext() == null) return;
        HapticFeedbackUtil.errorWarning(requireContext());

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_duplicate_receipt_warning, null);
        TextView tvChallanNo = dialogView.findViewById(R.id.tvDuplicateChallanNumber);
        TextView tvSupplier = dialogView.findViewById(R.id.tvDuplicateSupplier);
        TextView tvDate = dialogView.findViewById(R.id.tvDuplicateDate);
        TextView tvMsg = dialogView.findViewById(R.id.tvDuplicateMessage);
        View btnViewInLedger = dialogView.findViewById(R.id.btnViewInLedger);
        View btnDismiss = dialogView.findViewById(R.id.btnDismissDuplicate);

        if (challan != null && challan.challanNumber != null && !challan.challanNumber.isEmpty()) {
            tvChallanNo.setText(challan.challanNumber);
        } else if (dupResult.matchedChallanNumber != null) {
            tvChallanNo.setText(dupResult.matchedChallanNumber);
        }

        if (challan != null && challan.supplierName != null && !challan.supplierName.isEmpty()) {
            tvSupplier.setText(challan.supplierName);
        } else if (dupResult.matchedSupplier != null) {
            tvSupplier.setText(dupResult.matchedSupplier);
        }

        if (dupResult.matchedRecordedAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US);
            tvDate.setText(sdf.format(new Date(dupResult.matchedRecordedAt)));
        }

        if (dupResult.reason != null) {
            tvMsg.setText(dupResult.reason + "\n\nDouble entry is blocked to preserve inventory accuracy.");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnViewInLedger.setOnClickListener(v -> {
            dialog.dismiss();
            binding.chipFilterStockIn.setChecked(true);
            filterLedger(StockTransaction.TYPE_STOCK_IN);
            Toast.makeText(requireContext(), "Showing recorded delivery in ledger", Toast.LENGTH_SHORT).show();
        });

        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    void showInvalidReceiptWarningDialog(String rejectionReason) {
        if (!isAdded() || getContext() == null) return;
        HapticFeedbackUtil.errorWarning(requireContext());

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_invalid_receipt_warning, null);
        TextView tvReason = dialogView.findViewById(R.id.tvWarningReason);
        View btnDismiss = dialogView.findViewById(R.id.btnDismissWarning);
        View btnTryAgain = dialogView.findViewById(R.id.btnTryAgainScan);

        if (rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            tvReason.setText(rejectionReason);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnDismiss.setOnClickListener(v -> dialog.dismiss());
        btnTryAgain.setOnClickListener(v -> {
            dialog.dismiss();
            showChallanSourceDialog();
        });

        dialog.show();
    }

    public void openDemoChallanReview() {
        simulateDemoChallan();
    }

    private void simulateDemoChallan() {
        ChallanOcrVisionClient.ParsedChallan demo = new ChallanOcrVisionClient.ParsedChallan();
        demo.supplierName = "Meghna Cement & Steel Mills Ltd";
        demo.challanNumber = "CH-94812";
        demo.date = "2026-11-10";
        demo.extractionSource = "Google Gemini 1.5 Flash Vision";

        if (getActivity() != null && getActivity().getIntent() != null) {
            String overrideChallan = getActivity().getIntent().getStringExtra("demo_challan_no");
            if (overrideChallan != null && !overrideChallan.trim().isEmpty()) {
                demo.challanNumber = overrideChallan.trim();
            }
        }

        demo.items.add(new ChallanOcrVisionClient.ChallanItem("Shah Cement Special", "CEMENT", 120.0, "BAG", 560.0, 67200.0));
        demo.items.add(new ChallanOcrVisionClient.ChallanItem("BSRM 500W 16mm Rebar", "STEEL", 4.5, "TON", 92000.0, 414000.0));
        demo.items.add(new ChallanOcrVisionClient.ChallanItem("Sylhet Sand (2.5 FM Coarse)", "AGGREGATE_SAND", 400.0, "CFT", 48.0, 19200.0));

        boolean forceReview = getActivity() != null && getActivity().getIntent() != null &&
                getActivity().getIntent().getBooleanExtra("force_review", false);

        if (forceReview) {
            openChallanReviewSheet(demo);
            return;
        }

        repository.checkDuplicateReceipt(demo.challanNumber, demo.getFingerprint(), demo.imageHash,
                new InventoryRepository.ActionCallback<InventoryRepository.DuplicateCheckResult>() {
                    @Override
                    public void onSuccess(InventoryRepository.DuplicateCheckResult dupResult) {
                        if (!isAdded()) return;
                        if (dupResult.isDuplicate) {
                            showDuplicateReceiptWarningDialog(demo, dupResult);
                        } else {
                            openChallanReviewSheet(demo);
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        if (!isAdded()) return;
                        openChallanReviewSheet(demo);
                    }
                });
    }

    private void openChallanReviewSheet(ChallanOcrVisionClient.ParsedChallan challan) {
        ChallanReviewBottomSheetDialog dialog = ChallanReviewBottomSheetDialog.newInstance(challan);
        dialog.setOnChallanCommittedListener((itemsAdded, totalValuation) -> {
            HapticFeedbackUtil.scanSuccess(requireContext());
        });
        dialog.show(getChildFragmentManager(), "ChallanReviewSheet");
    }

    private void showQuickStockOutPicker() {
        if (currentInventory.isEmpty()) {
            Toast.makeText(requireContext(), "No materials currently in inventory", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] itemNames = new String[currentInventory.size()];
        for (int i = 0; i < currentInventory.size(); i++) {
            InventoryItem it = currentInventory.get(i);
            itemNames[i] = it.getItemName() + " (" + it.getCurrentBalance() + " " + it.getUnit() + " avail)";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Material to Stock Out")
                .setItems(itemNames, (dialog, which) -> {
                    InventoryItem selected = currentInventory.get(which);
                    onStockOut(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onStockOut(InventoryItem item) {
        RecordStockOutBottomSheetDialog dialog = RecordStockOutBottomSheetDialog.newInstance(item);
        dialog.setOnStockOutCommittedListener(transaction -> {
            HapticFeedbackUtil.confirm(requireContext());
        });
        dialog.show(getChildFragmentManager(), "StockOutSheet");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
