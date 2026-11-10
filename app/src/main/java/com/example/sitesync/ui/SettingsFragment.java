package com.example.sitesync.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.DialogSignOutConfirmBinding;
import com.example.sitesync.databinding.FragmentSettingsBinding;
import com.example.sitesync.hardware.CheckLevelDialog;
import com.example.sitesync.hardware.FloatingVoiceAlerterService;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SessionManager sessionManager;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateSwitchState()
        );
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
            binding.tvSettingsUserRole.setText(user.getRole() != null ? user.getRole() : "PROJECT MANAGER");
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                binding.tvSettingsUserEmail.setText(user.getEmail());
            }
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                binding.tvSettingsUserPhone.setText(user.getPhoneNumber());
            }
        }

        // Active Project Info
        String projectCode = sessionManager.getActiveProjectCode();
        String projectName = sessionManager.getActiveProjectName();
        binding.tvSettingsProjectCodeBadge.setText(projectCode != null ? projectCode : "PRJ-DHAKA-101");
        binding.tvSettingsProjectName.setText(projectName != null ? projectName : "Metro Central Tower, Uttara Sector 4");

        // Edit Project Scope Plan
        binding.btnSettingsEditProjectPlan.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            ProjectPlanBottomSheetDialog dialog = ProjectPlanBottomSheetDialog.newInstance(null);
            dialog.show(getParentFragmentManager(), "ProjectPlanDialog");
        });

        // Switch Project (Launch Project Hub)
        binding.btnSettingsSwitchHub.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            startActivity(new Intent(requireContext(), ProjectHubActivity.class));
            requireActivity().finish();
        });

        // Calibrate Sensors
        binding.rowCalibrateSensors.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            CheckLevelDialog dialog = CheckLevelDialog.newInstance();
            dialog.show(getParentFragmentManager(), "CheckLevelDialog");
        });

        // Floating Voice Alerter Switch & Test
        binding.switchFloatingVoiceAlerter.setOnCheckedChangeListener((btn, isChecked) -> {
            if (!btn.isPressed()) return; // user interaction only
            HapticFeedbackUtil.confirm(requireContext());
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
                    Toast.makeText(requireContext(), "Permission required to display floating bubble over apps", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + requireContext().getPackageName())
                    );
                    overlayPermissionLauncher.launch(intent);
                    btn.setChecked(false);
                    return;
                }
                FloatingVoiceAlerterService.startAlerter(requireContext());
                sessionManager.setFloatingVoiceAlerterEnabled(true);
                Toast.makeText(requireContext(), "Floating Voice Alerter Active!", Toast.LENGTH_SHORT).show();
            } else {
                FloatingVoiceAlerterService.stopAlerter(requireContext());
                sessionManager.setFloatingVoiceAlerterEnabled(false);
                Toast.makeText(requireContext(), "Floating Voice Alerter Stopped", Toast.LENGTH_SHORT).show();
            }
        });

        binding.rowTestVoiceAlerter.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            startActivity(new Intent(requireContext(), VoiceAlerterDialogActivity.class));
        });

        // App Tour Tips
        binding.cardSettingsTourTips.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            AppTourTipsDialog dialog = AppTourTipsDialog.newInstance();
            dialog.show(getParentFragmentManager(), "AppTourTipsDialog");
        });

        binding.btnSettingsSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            confirmSignOut();
        });
    }

    private void confirmSignOut() {
        DialogSignOutConfirmBinding dialogBinding = DialogSignOutConfirmBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogBinding.btnCancelSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            dialog.dismiss();
        });

        dialogBinding.btnConfirmSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            dialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            sessionManager.clearSession();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchState();
    }

    private void updateSwitchState() {
        if (binding == null || getContext() == null) return;
        boolean hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(requireContext());
        boolean isEnabled = sessionManager.isFloatingVoiceAlerterEnabled() && hasPermission;
        binding.switchFloatingVoiceAlerter.setChecked(isEnabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
