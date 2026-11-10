package com.example.sitesync.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.databinding.DialogRequiredMaterialsListBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class RequiredMaterialsListBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_PROJECT_CODE = "arg_project_code";

    private DialogRequiredMaterialsListBinding binding;
    private ChatMaterialsReportAdapter adapter;
    private String projectCode;

    public static RequiredMaterialsListBottomSheetDialog newInstance(String projectCode) {
        RequiredMaterialsListBottomSheetDialog dialog = new RequiredMaterialsListBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_CODE, projectCode);
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
        binding = DialogRequiredMaterialsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            projectCode = getArguments().getString(ARG_PROJECT_CODE);
        }
        if (projectCode == null || projectCode.isEmpty()) {
            SessionManager sm = new SessionManager(requireContext());
            projectCode = sm.getActiveProjectCode();
        }
        if (projectCode == null || projectCode.isEmpty()) {
            projectCode = "PRJ-DHAKA-101";
        }

        binding.btnCloseRequiredList.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            dismiss();
        });

        binding.btnDoneRequiredList.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            dismiss();
        });

        adapter = new ChatMaterialsReportAdapter(requireContext(), (selectedCount, totalCount) -> {});
        binding.rvRequiredMaterials.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRequiredMaterials.setAdapter(adapter);

        AppDatabase.getInstance(requireContext()).requiredMaterialItemDao()
                .getPendingItemsForProjectLive(projectCode)
                .observe(getViewLifecycleOwner(), items -> {
                    if (items != null && !items.isEmpty()) {
                        binding.layoutEmptyRequiredMaterials.setVisibility(View.GONE);
                        binding.rvRequiredMaterials.setVisibility(View.VISIBLE);
                        binding.tvRequiredCountBadge.setText(items.size() + " PENDING");
                        adapter.setItems(items);
                    } else {
                        binding.layoutEmptyRequiredMaterials.setVisibility(View.VISIBLE);
                        binding.rvRequiredMaterials.setVisibility(View.GONE);
                        binding.tvRequiredCountBadge.setText("0 PENDING");
                        adapter.setItems(null);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
