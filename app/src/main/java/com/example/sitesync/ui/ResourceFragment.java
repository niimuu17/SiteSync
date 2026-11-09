package com.example.sitesync.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.InventoryItemEntity;
import com.example.sitesync.data.local.entities.PendingSyncEntity;
import com.example.sitesync.databinding.FragmentResourceBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResourceFragment extends Fragment implements InventoryAdapter.OnInventoryActionListener {

    private FragmentResourceBinding binding;
    private InventoryAdapter inventoryAdapter;
    private AppDatabase db;
    private final List<InventoryItemEntity> inventoryItems = new ArrayList<>();

    public static ResourceFragment newInstance() {
        return new ResourceFragment();
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
        db = AppDatabase.getDatabase(requireContext());

        setupRecyclerView();
        setupRequisitionActions();
        setupAddCustomItem();
        loadAndSeedInventory();
    }

    private void setupRecyclerView() {
        inventoryAdapter = new InventoryAdapter(requireContext(), this);
        binding.rvInventory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvInventory.setAdapter(inventoryAdapter);
    }

    private void setupRequisitionActions() {
        binding.layoutLowStockBanner.btnQuickRequisition.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            openRequisitionDialog();
        });
    }

    private void setupAddCustomItem() {
        binding.btnAddInventoryItem.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            showAddItemDialog();
        });
    }

    private void openRequisitionDialog() {
        CreateRequisitionBottomSheetDialog dialog = CreateRequisitionBottomSheetDialog.newInstance();
        dialog.setOnRequisitionSubmittedListener(this::loadAndSeedInventory);
        dialog.show(getParentFragmentManager(), "CREATE_REQUISITION_SHEET");
    }

    private void showAddItemDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        final EditText etName = new EditText(requireContext());
        etName.setHint("Material Name (e.g. Waterproofing Membrane)");
        etName.setBackgroundResource(R.drawable.bg_rounded_input);
        etName.setPadding(32, 24, 32, 24);
        etName.setTextColor(requireContext().getColor(R.color.text_oxide_primary));
        layout.addView(etName);

        final EditText etStock = new EditText(requireContext());
        etStock.setHint("Initial Stock Quantity (e.g. 150)");
        etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etStock.setBackgroundResource(R.drawable.bg_rounded_input);
        etStock.setPadding(32, 24, 32, 24);
        etStock.setTextColor(requireContext().getColor(R.color.text_oxide_primary));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.topMargin = 16;
        etStock.setLayoutParams(lp1);
        layout.addView(etStock);

        final EditText etMin = new EditText(requireContext());
        etMin.setHint("Minimum Safety Threshold (e.g. 50)");
        etMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etMin.setBackgroundResource(R.drawable.bg_rounded_input);
        etMin.setPadding(32, 24, 32, 24);
        etMin.setTextColor(requireContext().getColor(R.color.text_oxide_primary));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = 16;
        etMin.setLayoutParams(lp2);
        layout.addView(etMin);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Material to Inventory")
                .setView(layout)
                .setPositiveButton("Add Material", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String stockStr = etStock.getText().toString().trim();
                    String minStr = etMin.getText().toString().trim();

                    if (!name.isEmpty() && !stockStr.isEmpty() && !minStr.isEmpty()) {
                        double stock = Double.parseDouble(stockStr);
                        double min = Double.parseDouble(minStr);
                        String id = "MAT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

                        InventoryItemEntity newItem = new InventoryItemEntity(
                                id, name, "GENERAL", stock, min, "Units", System.currentTimeMillis(), false);
                        db.inventoryDao().insert(newItem);

                        // Queue sync
                        PendingSyncEntity syncItem = new PendingSyncEntity(
                                UUID.randomUUID().toString(), "INVENTORY", id, "CREATE",
                                "{\"name\":\"" + name + "\",\"stock\":" + stock + "}", System.currentTimeMillis());
                        db.pendingSyncDao().insert(syncItem);

                        Toast.makeText(requireContext(), "Material Added: " + name, Toast.LENGTH_SHORT).show();
                        loadAndSeedInventory();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadAndSeedInventory() {
        List<InventoryItemEntity> existing = db.inventoryDao().getAllInventorySync();
        if (existing == null || existing.isEmpty()) {
            seedDefaultInventory();
            existing = db.inventoryDao().getAllInventorySync();
        }

        inventoryItems.clear();
        if (existing != null) {
            inventoryItems.addAll(existing);
        }

        inventoryAdapter.setItems(inventoryItems);
        binding.tvInventoryCountBadge.setText("(" + inventoryItems.size() + " items)");

        evaluateLowStockAlert();
    }

    private void seedDefaultInventory() {
        long now = System.currentTimeMillis();
        List<InventoryItemEntity> seed = new ArrayList<>();

        seed.add(new InventoryItemEntity(
                "MAT-001",
                "Portland Cement (50kg Bags)",
                "CEMENT",
                85.0,
                500.0,
                "Bags",
                now,
                true
        ));

        seed.add(new InventoryItemEntity(
                "MAT-002",
                "Structural Rebar 16mm (Tons)",
                "STEEL",
                42.5,
                20.0,
                "Tons",
                now,
                true
        ));

        seed.add(new InventoryItemEntity(
                "MAT-003",
                "Coarse River Sand (cft)",
                "AGGREGATE",
                280.0,
                600.0,
                "cft",
                now,
                true
        ));

        seed.add(new InventoryItemEntity(
                "MAT-004",
                "Concrete Admixture Plasticizer (L)",
                "CHEMICALS",
                180.0,
                100.0,
                "Liters",
                now,
                true
        ));

        db.inventoryDao().insertAll(seed);
    }

    private void evaluateLowStockAlert() {
        InventoryItemEntity mostCritical = null;
        int lowestPercent = 100;

        for (InventoryItemEntity item : inventoryItems) {
            if (item.isLowStock()) {
                int pct = item.getStockPercentage();
                if (pct < lowestPercent) {
                    lowestPercent = pct;
                    mostCritical = item;
                }
            }
        }

        if (mostCritical != null) {
            binding.layoutLowStockBanner.layoutLowStockBanner.setVisibility(View.VISIBLE);
            binding.layoutLowStockBanner.tvLowStockAlertTitle.setText(
                    "Low Stock: " + mostCritical.getName());
            binding.layoutLowStockBanner.tvLowStockAlertMessage.setText(
                    (int)mostCritical.getCurrentStock() + " of " + (int)mostCritical.getMinThreshold() +
                    " " + mostCritical.getUnit() + " remaining (" + lowestPercent + "%)");
        } else {
            binding.layoutLowStockBanner.layoutLowStockBanner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStockIn(InventoryItemEntity item) {
        StockInDialog.show(requireContext(), item, this::loadAndSeedInventory);
    }

    @Override
    public void onStockOut(InventoryItemEntity item) {
        StockOutDialog.show(requireContext(), item, this::loadAndSeedInventory);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
