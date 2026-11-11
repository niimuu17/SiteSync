package com.example.sitesync.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.dao.WorkerAttendanceDao;
import com.example.sitesync.data.local.entities.TaskEntity;
import com.example.sitesync.data.local.entities.WorkerAttendanceEntity;
import com.example.sitesync.databinding.ActivityWorkerDashboardBinding;
import com.example.sitesync.hardware.AudioDecibelManager;
import com.example.sitesync.hardware.FallAlertCountdownDialog;
import com.example.sitesync.hardware.FallDetectionManager;
import com.example.sitesync.hardware.FloatingVoiceAlerterService;
import com.example.sitesync.hardware.UserLocationManager;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class WorkerDashboardActivity extends AppCompatActivity {

    private ActivityWorkerDashboardBinding binding;
    private SessionManager sessionManager;
    private AppDatabase database;
    private WorkerAttendanceDao attendanceDao;
    private WorkerAttendanceEntity currentAttendance;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private LiveData<WorkerAttendanceEntity> attendanceLiveData;
    private ActivityResultLauncher<String> workerPhotoPickerLauncher;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private AudioDecibelManager decibelManager;
    private FallDetectionManager fallDetectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        database = AppDatabase.getInstance(this);
        attendanceDao = database.workerAttendanceDao();

        workerPhotoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveAndApplyWorkerPhoto(uri);
                    }
                }
        );

        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                        FloatingVoiceAlerterService.startAlerter(this);
                        sessionManager.setFloatingVoiceAlerterEnabled(true);
                    }
                    updateVoiceAlerterBubbleUi();
                }
        );

        setupHeaderAndUserInfo();
        setupSmartAttendance();
        setupQuickActions();
        setupVoiceAlerter();
        setupDecibelMeter();
        setupFallDetector();
        setupAssignedTasks();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || binding == null) return;
        int scrollY = intent.getIntExtra("scroll_y", 0);
        if (scrollY > 0) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (binding != null) {
                    binding.workerNestedScrollView.smoothScrollTo(0, scrollY);
                }
            }, 300);
        }
        if (intent.getBooleanExtra("simulate_fall", false)) {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    triggerFallAlertCountdown(3.5f), 300);
        }
    }

    private void setupHeaderAndUserInfo() {
        String pCode = sessionManager.getActiveProjectCode();
        if (pCode == null || pCode.isEmpty()) {
            pCode = "PRJ-DHAKA-101";
        }
        binding.tvWorkerProjectCode.setText(pCode);

        User user = sessionManager.getCurrentUser();
        if (user != null) {
            binding.tvWorkerGreetingName.setText("Welcome, " + user.getFullName());
            binding.tvWorkerTradeRole.setText("Site Worker • " + sessionManager.getActiveProjectName());
        }

        binding.layoutWorkerBackToHub.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            startActivity(new Intent(this, ProjectHubActivity.class));
            finish();
        });

        binding.tvWorkerProjectCode.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            startActivity(new Intent(this, ProjectHubActivity.class));
            finish();
        });

        binding.btnWorkerSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            com.example.sitesync.databinding.DialogSignOutConfirmBinding dialogBinding =
                    com.example.sitesync.databinding.DialogSignOutConfirmBinding.inflate(getLayoutInflater());
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogBinding.getRoot())
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            dialogBinding.btnCancelSignOut.setOnClickListener(cv -> {
                HapticFeedbackUtil.lightClick(this, cv);
                dialog.dismiss();
            });

            dialogBinding.btnConfirmSignOut.setOnClickListener(cv -> {
                HapticFeedbackUtil.heavyClick(this, cv);
                dialog.dismiss();
                sessionManager.clearSession();
                FloatingVoiceAlerterService.stopAlerter(WorkerDashboardActivity.this);
                Intent intent = new Intent(WorkerDashboardActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });

            dialog.show();
        });

        binding.btnWorkerChatHeader.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            openChatRoom();
        });

        binding.btnWorkerProfilePhoto.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            workerPhotoPickerLauncher.launch("image/*");
        });
        loadWorkerProfilePhoto();
    }

    private void setupSmartAttendance() {
        User user = sessionManager.getCurrentUser();
        String workerId = (user != null && user.getUsername() != null) ? user.getUsername() : "worker_default";

        attendanceLiveData = attendanceDao.getLatestAttendanceForWorkerLive(workerId);
        attendanceLiveData.observe(this, attendance -> {
            currentAttendance = attendance;
            if (attendance != null && attendance.isClockedIn()) {
                updateUiForClockedIn(attendance);
            } else {
                updateUiForClockedOut();
            }
        });

        android.location.Location bestLoc = UserLocationManager.getInstance(this).getBestAvailableLocation();
        if (bestLoc != null) {
            binding.tvGpsLocationInfo.setText(String.format(Locale.US, "GPS: %.4f° N, %.4f° E • Geofence Verified", bestLoc.getLatitude(), bestLoc.getLongitude()));
        }

        binding.btnAttendancePunch.setOnClickListener(v -> {
            HapticFeedbackUtil.confirm(this);
            toggleAttendancePunch();
        });
    }

    private void toggleAttendancePunch() {
        User user = sessionManager.getCurrentUser();
        String workerId = (user != null && user.getUsername() != null) ? user.getUsername() : "worker_default";
        String workerName = (user != null) ? user.getFullName() : "Site Worker";
        String pCode = sessionManager.getActiveProjectCode();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (currentAttendance != null && currentAttendance.isClockedIn()) {
                // Clock Out
                long now = System.currentTimeMillis();
                attendanceDao.clockOut(currentAttendance.getId(), now);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Shift Ended. Attendance recorded.", Toast.LENGTH_SHORT).show();
                    stopShiftTimer();
                });
            } else {
                // Clock In
                String id = UUID.randomUUID().toString();
                long now = System.currentTimeMillis();
                android.location.Location userLoc = com.example.sitesync.hardware.UserLocationManager.getInstance(this).getBestAvailableLocation();
                double siteLat = userLoc != null ? userLoc.getLatitude() : sessionManager.getLastKnownLatitude();
                double siteLng = userLoc != null ? userLoc.getLongitude() : sessionManager.getLastKnownLongitude();
                WorkerAttendanceEntity newAttendance = new WorkerAttendanceEntity(
                        id, workerId, workerName, pCode, now, siteLat, siteLng
                );
                attendanceDao.insert(newAttendance);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Clocked In successfully! Geofence verified.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUiForClockedIn(WorkerAttendanceEntity attendance) {
        binding.tvAttendanceStatusBadge.setText("ON DUTY");
        binding.tvAttendanceStatusBadge.setTextColor(Color.parseColor("#10B981"));
        binding.tvAttendanceStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
        binding.tvClockInTime.setText(sdf.format(new Date(attendance.getClockInTime())));

        binding.btnAttendancePunch.setText("Clock Out (End Shift)");
        binding.btnAttendancePunch.setBackgroundColor(Color.parseColor("#E53935"));
        binding.btnAttendancePunch.setTextColor(Color.WHITE);
        binding.btnAttendancePunch.setIconResource(R.drawable.ic_close);
        binding.btnAttendancePunch.setIconTintResource(R.color.white);

        startShiftTimer(attendance.getClockInTime());
    }

    private void updateUiForClockedOut() {
        stopShiftTimer();
        binding.tvAttendanceStatusBadge.setText("NOT CLOCKED IN");
        binding.tvAttendanceStatusBadge.setTextColor(Color.parseColor("#B8C9C3"));
        binding.tvAttendanceStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill_teal);

        binding.tvClockInTime.setText("--:-- AM");
        binding.tvShiftDuration.setText("0h 00m");

        binding.btnAttendancePunch.setText("Clock In (GPS Punch)");
        binding.btnAttendancePunch.setBackgroundColor(getColor(R.color.peach_accent));
        binding.btnAttendancePunch.setTextColor(getColor(R.color.forest_surface_deep));
        binding.btnAttendancePunch.setIconResource(R.drawable.ic_check_circle);
        binding.btnAttendancePunch.setIconTint(getColorStateList(R.color.forest_surface_deep));
    }

    private void startShiftTimer(long startTime) {
        stopShiftTimer();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long diff = System.currentTimeMillis() - startTime;
                if (diff < 0) diff = 0;
                long hours = diff / (1000 * 60 * 60);
                long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
                binding.tvShiftDuration.setText(String.format(Locale.US, "%dh %02dm", hours, minutes));
                timerHandler.postDelayed(this, 30000); // refresh every 30s
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopShiftTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void setupQuickActions() {
        // Daily Progress Log (AI Evaluator)
        binding.cardWorkerLogWork.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            LogProgressBottomSheetDialog dialog = LogProgressBottomSheetDialog.newInstance();
            dialog.show(getSupportFragmentManager(), "LogProgressDialog");
        });

        // Geotagged Photo Evidence
        binding.cardWorkerSitePhoto.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            SiteEvidenceBottomSheetDialog dialog = SiteEvidenceBottomSheetDialog.newInstance();
            dialog.show(getSupportFragmentManager(), "SiteEvidenceDialog");
        });

        // Chat Room
        binding.cardWorkerChat.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            openChatRoom();
        });

        // Safety Hazard Alert
        binding.cardWorkerHazardAlert.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            ReportHazardBottomSheetDialog dialog = ReportHazardBottomSheetDialog.newInstance();
            dialog.show(getSupportFragmentManager(), "ReportHazardDialog");
        });
    }

    private void openChatRoom() {
        Intent intent = new Intent(this, SiteChatActivity.class);
        intent.putExtra("project_code", sessionManager.getActiveProjectCode());
        intent.putExtra("project_name", sessionManager.getActiveProjectName());
        startActivity(intent);
    }

    private void setupAssignedTasks() {
        binding.rvWorkerTasks.setLayoutManager(new LinearLayoutManager(this));

        long now = System.currentTimeMillis();
        long day = 24L * 60 * 60 * 1000;
        List<TaskEntity> workerTasks = new ArrayList<>();
        workerTasks.add(new TaskEntity("WT-101", "Bending & Binding Rebar Rings (10mm Ring / Churi for Beams)",
                "Bend 10mm deformed rebar into square stirrup rings for lintel beam.", "Me", "HIGH", "IN_PROGRESS", now + day, true, now, true));
        workerTasks.add(new TaskEntity("WT-102", "Wall Plastering (1:4 Cement Sand) - 2nd Floor Unit A",
                "Apply 12mm plaster with spirit level plumb line finish.", "Me", "MEDIUM", "TODO", now + (2 * day), true, now, true));
        workerTasks.add(new TaskEntity("WT-103", "Porcelain Floor Tiles Setting (600x600mm)",
                "Lay 24x24 inch porcelain tiles with leveling spacers.", "Me", "MEDIUM", "TODO", now + (3 * day), true, now, true));

        TaskAdapter taskAdapter = new TaskAdapter(this, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onStatusChanged(TaskEntity task, String newStatus) {
                HapticFeedbackUtil.confirm(WorkerDashboardActivity.this);
                Toast.makeText(WorkerDashboardActivity.this, "Status updated: " + newStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNotesClicked(TaskEntity task) {
                HapticFeedbackUtil.lightClick(WorkerDashboardActivity.this, null);
                Toast.makeText(WorkerDashboardActivity.this, task.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });
        taskAdapter.setTasks(workerTasks);
        binding.rvWorkerTasks.setAdapter(taskAdapter);
        binding.tvWorkerTaskCount.setText(workerTasks.size() + " ASSIGNED");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWorkerProfilePhoto();

        // Auto-start Floating Voice Alerter if overlay permission is granted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            FloatingVoiceAlerterService.startAlerter(this);
            sessionManager.setFloatingVoiceAlerterEnabled(true);
        } else if (!sessionManager.isOverlayPermissionPrompted()) {
            sessionManager.setOverlayPermissionPrompted(true);
            new AlertDialog.Builder(this)
                    .setTitle("Enable Site Voice Alerter")
                    .setMessage("SiteSync includes a floating emergency voice bubble for instant hands-free alerts (e.g. 'tin tolay agun lagse'). Enable 'Display over other apps' to keep it accessible.")
                    .setPositiveButton("Enable", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        overlayPermissionLauncher.launch(intent);
                    })
                    .setNegativeButton("Later", null)
                    .show();
        }

        updateVoiceAlerterBubbleUi();
        startDecibelSampling();
        startFallMonitoring();
        UserLocationManager.getInstance(this).fetchCurrentLocation(loc -> {});
        UserLocationManager.getInstance(this).startPassiveLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (decibelManager != null) {
            decibelManager.stopMonitoring();
        }
        if (fallDetectionManager != null) {
            fallDetectionManager.stopMonitoring();
        }
        UserLocationManager.getInstance(this).stopPassiveLocationUpdates();
    }

    private void setupVoiceAlerter() {
        binding.cardVoiceEmergencyAlerter.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            startActivity(new Intent(this, VoiceAlerterDialogActivity.class));
        });

        binding.btnTriggerVoiceAlerter.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            startActivity(new Intent(this, VoiceAlerterDialogActivity.class));
        });

        binding.btnToggleFloatingBubble.setOnClickListener(v -> {
            HapticFeedbackUtil.confirm(this);
            toggleFloatingBubbleService();
        });

        updateVoiceAlerterBubbleUi();
    }

    private void toggleFloatingBubbleService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Enable 'Display over other apps' to use floating voice bubble", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            overlayPermissionLauncher.launch(intent);
            return;
        }

        boolean currentActive = sessionManager.isFloatingVoiceAlerterEnabled();
        if (currentActive) {
            FloatingVoiceAlerterService.stopAlerter(this);
            sessionManager.setFloatingVoiceAlerterEnabled(false);
            Toast.makeText(this, "Floating Voice Bubble Disabled", Toast.LENGTH_SHORT).show();
        } else {
            FloatingVoiceAlerterService.startAlerter(this);
            sessionManager.setFloatingVoiceAlerterEnabled(true);
            Toast.makeText(this, "Floating Voice Bubble Active!", Toast.LENGTH_SHORT).show();
        }
        updateVoiceAlerterBubbleUi();
    }

    private void updateVoiceAlerterBubbleUi() {
        boolean hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        boolean enabled = sessionManager.isFloatingVoiceAlerterEnabled() && hasPermission;

        if (enabled) {
            binding.tvVoiceAlerterBubbleBadge.setText("ACTIVE");
            binding.tvVoiceAlerterBubbleBadge.setBackgroundResource(R.drawable.bg_badge_green);
            binding.tvVoiceAlerterBubbleBadge.setTextColor(Color.parseColor("#10B981"));
            binding.btnToggleFloatingBubble.setText("Disable Bubble");
        } else {
            binding.tvVoiceAlerterBubbleBadge.setText("OFF");
            binding.tvVoiceAlerterBubbleBadge.setBackgroundResource(R.drawable.bg_badge_pill_teal);
            binding.tvVoiceAlerterBubbleBadge.setTextColor(Color.parseColor("#B8C9C3"));
            binding.btnToggleFloatingBubble.setText(hasPermission ? "Start Bubble" : "Grant Permission");
        }
    }

    private void saveAndApplyWorkerPhoto(Uri uri) {
        try {
            File photoFile = new File(getFilesDir(), "worker_profile_photo.jpg");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(photoFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            sessionManager.setProfilePhotoPath(photoFile.getAbsolutePath());
            loadWorkerProfilePhoto();
            Toast.makeText(this, "Worker photo updated successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to update photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWorkerProfilePhoto() {
        String path = sessionManager.getProfilePhotoPath();
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                binding.ivWorkerAvatar.setImageTintList(null);
                binding.ivWorkerAvatar.setImageURI(null);
                binding.ivWorkerAvatar.setImageURI(Uri.fromFile(file));
                return;
            }
        }
        binding.ivWorkerAvatar.setImageResource(R.drawable.ic_person);
        binding.ivWorkerAvatar.setImageTintList(getColorStateList(R.color.forest_surface_deep));
    }

    private void setupDecibelMeter() {
        decibelManager = new AudioDecibelManager(this);
    }

    private void startDecibelSampling() {
        if (decibelManager == null) return;
        decibelManager.startMonitoring(new AudioDecibelManager.OnNoiseLevelListener() {
            @Override
            public void onNoiseLevelUpdated(double decibels, boolean isHazardous) {
                if (binding == null) return;
                binding.pbWorkerDecibelLevel.setProgress((int) decibels);
                if (isHazardous) {
                    binding.tvWorkerNoiseStatusBadge.setText(String.format(Locale.US, "%.0f dB (Hazard)", decibels));
                    binding.tvWorkerNoiseStatusBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                    binding.tvWorkerNoiseStatusBadge.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_red));
                    binding.pbWorkerDecibelLevel.setProgressTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_red)));
                    binding.tvWorkerNoiseWarning.setText("Hazardous noise (>85 dB). Hearing protection mandatory.");
                    binding.tvWorkerNoiseWarning.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_red));
                } else {
                    binding.tvWorkerNoiseStatusBadge.setText(String.format(Locale.US, "%.0f dB (Safe)", decibels));
                    binding.tvWorkerNoiseStatusBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                    binding.tvWorkerNoiseStatusBadge.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_green));
                    binding.pbWorkerDecibelLevel.setProgressTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_green)));
                    binding.tvWorkerNoiseWarning.setText("Nominal background sound level. Hearing protection mandatory above 85 dB.");
                    binding.tvWorkerNoiseWarning.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.forest_muted));
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (binding != null) {
                    binding.tvWorkerNoiseWarning.setText("Acoustic Sensor: Nominal background calibration active.");
                }
            }
        });
    }

    private void setupFallDetector() {
        fallDetectionManager = new FallDetectionManager(this);
        binding.btnWorkerSimulateFall.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            triggerFallAlertCountdown(3.5f);
        });
    }

    private void triggerFallAlertCountdown(float impactG) {
        FallAlertCountdownDialog dialog = FallAlertCountdownDialog.newInstance(impactG);
        dialog.setOnFallAlertResolvedListener(new FallAlertCountdownDialog.OnFallAlertResolvedListener() {
            @Override
            public void onFallAlertCancelled() {
                if (binding != null) {
                    binding.tvWorkerFallSensorBadge.setText("ACTIVE (OK)");
                    binding.tvWorkerFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                    binding.tvWorkerFallSensorBadge.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_green));
                }
            }

            @Override
            public void onEmergencyDispatched(float impactG) {
                if (binding != null) {
                    binding.tvWorkerFallSensorBadge.setText("ALERT SENT");
                    binding.tvWorkerFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                    binding.tvWorkerFallSensorBadge.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_red));
                }
            }
        });
        dialog.show(getSupportFragmentManager(), "WorkerFallAlertCountdownDialog");
    }

    private void startFallMonitoring() {
        if (fallDetectionManager == null) return;
        fallDetectionManager.startMonitoring(
                (impactG, timestamp) -> triggerFallAlertCountdown(impactG),
                (magnitude, stateDescription) -> {
                    if (binding == null) return;
                    float gForce = magnitude / 9.8f;
                    if ("IMPACT_DETECTED".equals(stateDescription)) {
                        binding.tvWorkerFallSensorBadge.setText(String.format(Locale.US, "IMPACT (%.1fG)", gForce));
                        binding.tvWorkerFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                        binding.tvWorkerFallSensorBadge.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_red));
                    } else if ("FREE_FALL".equals(stateDescription)) {
                        binding.tvWorkerFallSensorBadge.setText("FREE-FALL");
                        binding.tvWorkerFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                        binding.tvWorkerFallSensorBadge.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.brand_amber));
                    } else {
                        binding.tvWorkerFallSensorBadge.setText(String.format(Locale.US, "ACTIVE (%.1fG)", gForce));
                        binding.tvWorkerFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                        binding.tvWorkerFallSensorBadge.setTextColor(ContextCompat.getColor(WorkerDashboardActivity.this, R.color.status_green));
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopShiftTimer();
        if (decibelManager != null) {
            decibelManager.stopMonitoring();
        }
        if (fallDetectionManager != null) {
            fallDetectionManager.stopMonitoring();
        }
        UserLocationManager.getInstance(this).stopPassiveLocationUpdates();
    }
}
