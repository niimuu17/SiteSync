package com.example.sitesync.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sitesync.R;
import com.example.sitesync.databinding.FragmentTaskBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

public class TaskFragment extends Fragment {

    private FragmentTaskBinding binding;

    public static TaskFragment newInstance() {
        return new TaskFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCompleteTask1.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            binding.btnCompleteTask1.setText("Completed ✓");
            binding.btnCompleteTask1.setEnabled(false);
            Toast.makeText(requireContext(), "Task Marked As Completed", Toast.LENGTH_SHORT).show();
        });

        binding.btnViewRfiDetails.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            Toast.makeText(requireContext(), "Opening RFI-2026-042 Thread...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
