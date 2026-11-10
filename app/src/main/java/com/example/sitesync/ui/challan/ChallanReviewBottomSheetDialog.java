package com.example.sitesync.ui.challan;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.network.ConstructionMarketPriceClient;
import com.example.sitesync.data.repository.InventoryRepository;
import com.example.sitesync.databinding.DialogChallanReviewBinding;
import com.example.sitesync.hardware.ChallanOcrVisionClient;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Interactive Receipt Verification Bottom Sheet Dialog.
 * Displays scanned delivery items, live Brave Search BDT market price benchmarks,
 * price inflation variance badges, and provides the "Add to Inventory" action.
 */
public class ChallanReviewBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnChallanCommittedListener {
        void onInventoryCommitted(int itemsAdded, double totalValuation);
    }

    private DialogChallanReviewBinding binding;
    private ChallanReviewAdapter adapter;
    private ChallanOcrVisionClient.ParsedChallan parsedChallan;
    private OnChallanCommittedListener commitListener;
    private final NumberFormat bdtFormat = NumberFormat.getNumberInstance(Locale.US);

    public static ChallanReviewBottomSheetDialog newInstance(ChallanOcrVisionClient.ParsedChallan challan) {
        ChallanReviewBottomSheetDialog dialog = new ChallanReviewBottomSheetDialog();
        dialog.parsedChallan = challan;
        return dialog;
    }

    public void setOnChallanCommittedListener(OnChallanCommittedListener listener) {
        this.commitListener = listener;
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
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
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
        binding = DialogChallanReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (parsedChallan == null) {
            parsedChallan = new ChallanOcrVisionClient.ParsedChallan();
        }

        setupViews();
        setupRecyclerView();
        evaluateMarketBenchmarks();
        updateSummaryStrip();
        checkReceiptDuplicateStatus();
    }

    private void setupViews() {
        // Thumbnail & Click-to-Zoom
        if (parsedChallan.imageBitmap != null) {
            binding.ivChallanThumbnail.setImageBitmap(parsedChallan.imageBitmap);
            binding.ivChallanThumbnail.setOnClickListener(v -> showFullImageDialog());
        }

        // Source badge
        if (parsedChallan.extractionSource != null) {
            binding.tvAiSourceBadge.setText(parsedChallan.extractionSource);
        }

        // Editable metadata fields
        binding.etSupplierName.setText(parsedChallan.supplierName != null ? parsedChallan.supplierName : "Direct Construction Supplier");
        binding.etChallanNumber.setText(parsedChallan.challanNumber != null ? parsedChallan.challanNumber : "CH-" + (System.currentTimeMillis() % 10000));
        binding.etChallanDate.setText(parsedChallan.date != null ? parsedChallan.date :
                new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));

        // Live validation on Challan Number to detect duplicate entries
        binding.etChallanNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkReceiptDuplicateStatus();
            }
        });

        // Commit CTA
        binding.btnAddToInventory.setOnClickListener(v -> commitChallanToInventory());

        // Cancel
        binding.btnCancelReview.setOnClickListener(v -> dismiss());
    }

    private void checkReceiptDuplicateStatus() {
        if (!isAdded() || getContext() == null || binding == null) return;

        String challanNo = binding.etChallanNumber.getText() != null ?
                binding.etChallanNumber.getText().toString().trim() : "";
        String fingerprint = parsedChallan != null ? parsedChallan.getFingerprint() : "";
        String imageHash = parsedChallan != null ? parsedChallan.imageHash : "";

        InventoryRepository repository = InventoryRepository.getInstance(requireContext());
        repository.checkDuplicateReceipt(challanNo, fingerprint, imageHash, new InventoryRepository.ActionCallback<InventoryRepository.DuplicateCheckResult>() {
            @Override
            public void onSuccess(InventoryRepository.DuplicateCheckResult result) {
                if (!isAdded() || binding == null) return;
                if (result.isDuplicate) {
                    binding.cardDuplicateWarning.setVisibility(View.VISIBLE);
                    binding.tvDuplicateDetails.setText(result.reason != null ? result.reason : "This receipt is already in the ledger.");
                    binding.btnAddToInventory.setEnabled(false);
                    binding.btnAddToInventory.setText("Already Recorded in Inventory");
                } else {
                    binding.cardDuplicateWarning.setVisibility(View.GONE);
                    List<ChallanOcrVisionClient.ChallanItem> items = adapter != null ? adapter.getItems() : parsedChallan.items;
                    binding.btnAddToInventory.setEnabled(items != null && !items.isEmpty());
                    binding.btnAddToInventory.setText("Add to Inventory");
                }
            }

            @Override
            public void onError(Exception error) {
                // If query fails, let user proceed
            }
        });
    }

    private void setupRecyclerView() {
        binding.rvReviewMaterials.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChallanReviewAdapter(parsedChallan.items, position -> {
            updateSummaryStrip();
        });
        binding.rvReviewMaterials.setAdapter(adapter);
    }

    private void evaluateMarketBenchmarks() {
        List<ChallanOcrVisionClient.ChallanItem> items = parsedChallan.items;
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            ChallanOcrVisionClient.ChallanItem item = items.get(index);
            ConstructionMarketPriceClient.evaluatePrice(item.materialName, item.unitPrice, item.unit, evaluation -> {
                if (!isAdded() || getContext() == null) return;
                item.priceEvaluation = evaluation;
                adapter.notifyItemChanged(index);
            });
        }
    }

    private void updateSummaryStrip() {
        List<ChallanOcrVisionClient.ChallanItem> items = adapter != null ? adapter.getItems() : parsedChallan.items;
        double total = 0.0;
        for (ChallanOcrVisionClient.ChallanItem item : items) {
            total += item.totalPrice;
        }

        binding.tvItemsCountBadge.setText(items.size() + " Items");
        binding.tvTotalChallanAmount.setText(String.format(Locale.US, "$ %s", bdtFormat.format(Math.round(total))));

        binding.btnAddToInventory.setEnabled(!items.isEmpty());
    }

    private void commitChallanToInventory() {
        List<ChallanOcrVisionClient.ChallanItem> finalItems = adapter.getItems();
        if (finalItems.isEmpty()) {
            Toast.makeText(requireContext(), "No items to add to inventory", Toast.LENGTH_SHORT).show();
            return;
        }

        String supplier = binding.etSupplierName.getText() != null ?
                binding.etSupplierName.getText().toString().trim() : "Supplier";
        String challanNo = binding.etChallanNumber.getText() != null ?
                binding.etChallanNumber.getText().toString().trim() : "CH-UNKNOWN";
        String challanDate = binding.etChallanDate.getText() != null ?
                binding.etChallanDate.getText().toString().trim() : null;

        binding.btnAddToInventory.setEnabled(false);
        binding.btnAddToInventory.setText("Committing to Room Ledger...");

        String fingerprint = parsedChallan != null ? parsedChallan.getFingerprint() : "";
        String imageHash = parsedChallan != null ? parsedChallan.imageHash : "";

        InventoryRepository repository = InventoryRepository.getInstance(requireContext());
        com.example.sitesync.data.SessionManager sessionManager = new com.example.sitesync.data.SessionManager(requireContext());
        String activeProject = sessionManager.getActiveProjectCode();
        if (activeProject == null || activeProject.isEmpty()) {
            activeProject = "PRJ-DHAKA-101";
        }
        repository.recordChallanDelivery(activeProject, supplier, challanNo,
                challanDate, fingerprint, imageHash,
                System.currentTimeMillis(), finalItems, new InventoryRepository.ActionCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer result) {
                        if (!isAdded()) return;
                        HapticFeedbackUtil.scanSuccess(requireContext());
                        Toast.makeText(requireContext(),
                                "Successfully committed " + result + " materials to Inventory!",
                                Toast.LENGTH_LONG).show();

                        if (commitListener != null) {
                            double totalVal = 0;
                            for (ChallanOcrVisionClient.ChallanItem ci : finalItems) totalVal += ci.totalPrice;
                            commitListener.onInventoryCommitted(result, totalVal);
                        }
                        dismiss();
                    }

                    @Override
                    public void onError(Exception error) {
                        if (!isAdded()) return;
                        HapticFeedbackUtil.errorWarning(requireContext());
                        binding.cardDuplicateWarning.setVisibility(View.VISIBLE);
                        binding.tvDuplicateDetails.setText(error.getMessage() != null ? error.getMessage() : "Double entry prevented.");
                        binding.btnAddToInventory.setEnabled(false);
                        binding.btnAddToInventory.setText("Already Recorded in Inventory");
                        Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showFullImageDialog() {
        if (parsedChallan.imageBitmap == null || !isAdded()) return;

        Dialog zoomDialog = new Dialog(requireContext());
        zoomDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ImageView iv = new ImageView(requireContext());
        iv.setImageBitmap(parsedChallan.imageBitmap);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setOnClickListener(v -> zoomDialog.dismiss());

        zoomDialog.setContentView(iv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Window window = zoomDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        zoomDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
