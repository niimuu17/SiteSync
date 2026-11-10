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

import com.example.sitesync.databinding.DialogChatBriefingReportBinding;
import com.example.sitesync.hardware.ChatIntelligenceClient;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class ChatBriefingReportBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_BRIEFINGS = "arg_briefings";

    private DialogChatBriefingReportBinding binding;
    private ChatBriefingReportAdapter adapter;
    private ArrayList<ChatIntelligenceClient.ImportantBriefingNotice> briefings;

    public static ChatBriefingReportBottomSheetDialog newInstance(ArrayList<ChatIntelligenceClient.ImportantBriefingNotice> list) {
        ChatBriefingReportBottomSheetDialog dialog = new ChatBriefingReportBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_BRIEFINGS, list);
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
        binding = DialogChatBriefingReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            briefings = (ArrayList<ChatIntelligenceClient.ImportantBriefingNotice>) getArguments().getSerializable(ARG_BRIEFINGS);
        }

        binding.btnCloseBriefingDialog.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            dismiss();
        });

        binding.btnDoneBriefing.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            dismiss();
        });

        adapter = new ChatBriefingReportAdapter(requireContext());
        binding.rvBriefingCards.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBriefingCards.setAdapter(adapter);

        if (briefings != null && !briefings.isEmpty()) {
            binding.layoutEmptyBriefings.setVisibility(View.GONE);
            binding.rvBriefingCards.setVisibility(View.VISIBLE);
            binding.tvBriefingsCountBadge.setText(briefings.size() + " ALERT" + (briefings.size() > 1 ? "S" : ""));
            adapter.setNotices(briefings);
        } else {
            binding.layoutEmptyBriefings.setVisibility(View.VISIBLE);
            binding.rvBriefingCards.setVisibility(View.GONE);
            binding.tvBriefingsCountBadge.setText("0 ALERTS");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
