package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.FragmentSettingsBinding;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SessionManager sessionManager;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        User user = sessionManager.getCurrentUser();
        if (user != null) {
            binding.tvSettingsUserFullName.setText(user.getFullName());
            binding.tvSettingsUserRole.setText(user.getRole() + " • Metro Central Tower");
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                binding.tvSettingsUserEmail.setText(user.getEmail());
            }
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                binding.tvSettingsUserPhone.setText(user.getPhoneNumber());
            }
        }

        binding.btnSettingsSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            confirmSignOut();
        });
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to end your active session?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    sessionManager.clearSession();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
