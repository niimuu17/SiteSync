package com.example.sitesync.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.RequiredMaterialItemEntity;
import com.example.sitesync.databinding.DialogChatMaterialsReportBinding;
import com.example.sitesync.hardware.ChatIntelligenceClient;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatMaterialsReportBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_INTELLIGENCE_RESULT = "arg_intelligence_result";
    private static final String ARG_PROJECT_CODE = "arg_project_code";

    private DialogChatMaterialsReportBinding binding;
    private ChatMaterialsReportAdapter adapter;
    private ChatIntelligenceClient.ChatIntelligenceResult intelligenceResult;
    private String projectCode;

    public static ChatMaterialsReportBottomSheetDialog newInstance(String projectCode,
                                                                   ChatIntelligenceClient.ChatIntelligenceResult result) {
        ChatMaterialsReportBottomSheetDialog dialog = new ChatMaterialsReportBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_CODE, projectCode);
        args.putSerializable(ARG_INTELLIGENCE_RESULT, result);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogChatMaterialsReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            projectCode = getArguments().getString(ARG_PROJECT_CODE, "PRJ-DHAKA-101");
            intelligenceResult = (ChatIntelligenceClient.ChatIntelligenceResult) getArguments().getSerializable(ARG_INTELLIGENCE_RESULT);
        }

        if (intelligenceResult == null) {
            intelligenceResult = new ChatIntelligenceClient.ChatIntelligenceResult();
        }

        binding.tvModelEngineBadge.setText("AI Extracted (" + (intelligenceResult.modelUsed != null ? intelligenceResult.modelUsed : "Multimodal AI") + ")");
        binding.btnCloseMaterialsDialog.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            dismiss();
        });

        setupRecyclerView();
        setupActions();
    }

    private void setupRecyclerView() {
        adapter = new ChatMaterialsReportAdapter(requireContext(), (selectedCount, totalCount) -> {
            binding.tvSelectedSummary.setText(String.format(Locale.US, "%d of %d Selected", selectedCount, totalCount));
            binding.btnCommitToRequiredList.setText(String.format(Locale.US, "Add Selected (%d) to Required Items", selectedCount));
            binding.btnCommitToRequiredList.setEnabled(selectedCount > 0);
        });

        binding.rvExtractedMaterials.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExtractedMaterials.setAdapter(adapter);

        List<RequiredMaterialItemEntity> materials = intelligenceResult.requiredMaterials;
        if (materials != null && !materials.isEmpty()) {
            binding.layoutEmptyMaterials.setVisibility(View.GONE);
            binding.rvExtractedMaterials.setVisibility(View.VISIBLE);
            binding.tvMaterialsCountBadge.setText(materials.size() + " DETECTED");
            adapter.setItems(materials);
        } else {
            binding.layoutEmptyMaterials.setVisibility(View.VISIBLE);
            binding.rvExtractedMaterials.setVisibility(View.GONE);
            binding.tvMaterialsCountBadge.setText("0 DETECTED");
            binding.tvSelectedSummary.setText("0 Selected");
            binding.btnCommitToRequiredList.setEnabled(false);
        }
    }

    private void setupActions() {
        binding.btnToggleSelectAll.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            adapter.toggleSelectAll();
        });

        binding.btnCommitToRequiredList.setOnClickListener(v -> {
            HapticFeedbackUtil.confirm(requireContext());
            List<RequiredMaterialItemEntity> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) return;

            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getInstance(requireContext()).requiredMaterialItemDao().insertAll(selected);
            });

            Toast.makeText(requireContext(), "Added " + selected.size() + " materials to Required Items list!", Toast.LENGTH_SHORT).show();
            binding.btnCommitToRequiredList.setEnabled(false);
            binding.btnCommitToRequiredList.setText("Committed to Required Items (" + selected.size() + ")");
        });

        binding.btnGoToBriefings.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            dismiss();
            if (getParentFragmentManager() != null) {
                ArrayList<ChatIntelligenceClient.ImportantBriefingNotice> list = new ArrayList<>(intelligenceResult.importantBriefings);
                ChatBriefingReportBottomSheetDialog dialog = ChatBriefingReportBottomSheetDialog.newInstance(list);
                dialog.show(getParentFragmentManager(), "ChatBriefingReportDialog");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
