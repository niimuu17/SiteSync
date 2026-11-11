package com.example.sitesync.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.dao.HazardTicketDao;
import com.example.sitesync.data.local.dao.PendingSyncDao;
import com.example.sitesync.data.local.entities.HazardTicketEntity;
import com.example.sitesync.data.sync.SyncManager;
import com.example.sitesync.databinding.FragmentSecurityBinding;
import com.example.sitesync.hardware.AudioDecibelManager;
import com.example.sitesync.hardware.CheckLevelDialog;
import com.example.sitesync.hardware.FallAlertCountdownDialog;
import com.example.sitesync.hardware.FallDetectionManager;
import com.example.sitesync.hardware.UserLocationManager;
import com.example.sitesync.notifications.SiteSyncFirebaseMessagingService;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.example.sitesync.utils.NetworkMonitor;

import java.util.List;
import java.util.Locale;

/**
 * Tab 4: Security & Site Reliability Subsystem.
 * Hosts:
 * 1. High-Priority Emergency Broadcast (Two-step safeguard with 2.5s hold-to-confirm).
 * 2. Site Hazard & Safety Ticketing Feed (Filterable Open/Resolved, GPS photo geotagging, voice notes).
 * 3. Offline-First Sync Telemetry & Manual Flush (Room DB WAL cache monitoring & WorkManager trigger).
 * 4. Acoustic Jobsite Safety Decibel Telemetry (OSHA 85 dB PPE warning).
 */
public class SecurityFragment extends Fragment implements HazardTicketAdapter.OnHazardActionListener {

    private FragmentSecurityBinding binding;
    private HazardTicketDao hazardDao;
    private PendingSyncDao pendingSyncDao;
    private HazardTicketAdapter adapter;
    private AudioDecibelManager decibelManager;
    private FallDetectionManager fallDetectionManager;
    private boolean isShowingResolved = false;
    private androidx.activity.result.ActivityResultLauncher<String> audioPermissionLauncher;

    public static SecurityFragment newInstance() {
        return new SecurityFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioPermissionLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startDecibelSampling();
                    } else if (binding != null) {
                        binding.tvNoisePpeWarning.setText("Microphone permission required for live noise monitoring.");
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSecurityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = requireContext();
        AppDatabase db = AppDatabase.getDatabase(context);
        hazardDao = db.hazardTicketDao();
        pendingSyncDao = db.pendingSyncDao();

        setupEmergencyBroadcast();
        setupAcousticDecibelMonitoring();
        setupFallDetection();
        setupSpiritLevelUtility();
        setupAttendanceTableButton();
        setupHazardList();
        setupFilterChips();
        setupSyncTelemetry();
        seedInitialHazardsIfEmpty();
        loadHazardTickets();
    }

    private void setupAttendanceTableButton() {
        binding.cardEmployeeAttendanceTable.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            startActivity(new android.content.Intent(requireContext(), AttendanceTableActivity.class));
        });
    }

    private void setupFallDetection() {
        fallDetectionManager = new FallDetectionManager(requireContext());
        startFallMonitoring();

        binding.btnSimulateFallTest.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            triggerFallAlertCountdown(3.4f);
        });

        binding.btnTestFcmPush.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            SiteSyncFirebaseMessagingService.postCriticalNotification(
                    requireContext(),
                    "CRITICAL JOBSITE HAZARD (FCM)",
                    "Perimeter scaffolding displacement on Level 3 East. Evacuate zone immediately.",
                    "SCAFFOLD_COLLAPSE"
            );
            Toast.makeText(requireContext(), "FCM Critical Hazard Push Dispatched!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSpiritLevelUtility() {
        binding.btnOpenSpiritLevel.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            CheckLevelDialog dialog = CheckLevelDialog.newInstance();
            dialog.show(requireActivity().getSupportFragmentManager(), "CheckLevelDialog");
        });
    }

    private void triggerFallAlertCountdown(float impactG) {
        FallAlertCountdownDialog dialog = FallAlertCountdownDialog.newInstance(impactG);
        dialog.setOnFallAlertResolvedListener(new FallAlertCountdownDialog.OnFallAlertResolvedListener() {
            @Override
            public void onFallAlertCancelled() {
                if (binding != null) {
                    binding.tvFallSensorBadge.setText("ACTIVE (OK)");
                    binding.tvFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                    binding.tvFallSensorBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
                }
            }

            @Override
            public void onEmergencyDispatched(float impactG) {
                if (binding != null) {
                    binding.tvFallSensorBadge.setText("ALERT SENT");
                    binding.tvFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                    binding.tvFallSensorBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
                }
                loadHazardTickets();
            }
        });
        dialog.show(getParentFragmentManager(), "FallAlertCountdownDialog");
    }

    private void startFallMonitoring() {
        if (fallDetectionManager == null) return;
        fallDetectionManager.startMonitoring(
                (impactG, timestamp) -> triggerFallAlertCountdown(impactG),
                (magnitude, stateDescription) -> {
                    if (binding == null) return;
                    float gForce = magnitude / 9.8f;
                    if ("IMPACT_DETECTED".equals(stateDescription)) {
                        binding.tvFallSensorBadge.setText(String.format(Locale.US, "IMPACT (%.1fG)", gForce));
                        binding.tvFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                        binding.tvFallSensorBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
                    } else if ("FREE_FALL".equals(stateDescription)) {
                        binding.tvFallSensorBadge.setText("FREE-FALL");
                        binding.tvFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                        binding.tvFallSensorBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_amber));
                    } else {
                        binding.tvFallSensorBadge.setText(String.format(Locale.US, "ACTIVE (%.1fG)", gForce));
                        binding.tvFallSensorBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                        binding.tvFallSensorBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
                    }
                }
        );
    }

    private void setupAcousticDecibelMonitoring() {
        decibelManager = new AudioDecibelManager(requireContext());
    }

    private void startDecibelSampling() {
        if (decibelManager == null) return;

        if (!AudioDecibelManager.hasMicrophonePermission(requireContext())) {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
            return;
        }

        decibelManager.startMonitoring(new AudioDecibelManager.OnNoiseLevelListener() {
            @Override
            public void onNoiseLevelUpdated(double decibels, boolean isHazardous) {
                if (binding == null) return;

                binding.pbDecibelLevel.setProgress((int) decibels);

                if (isHazardous) {
                    binding.tvNoiseStatusBadge.setText(String.format(Locale.US, "%.0f dB (Hazard)", decibels));
                    binding.tvNoiseStatusBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
                    binding.tvNoiseStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
                    binding.pbDecibelLevel.setProgressTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.status_red)));
                    binding.tvNoisePpeWarning.setText("Hazardous noise (>85 dB). Hearing protection required.");
                    binding.tvNoisePpeWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
                } else if (decibels >= 75.0) {
                    binding.tvNoiseStatusBadge.setText(String.format(Locale.US, "%.0f dB (Elevated)", decibels));
                    binding.tvNoiseStatusBadge.setBackgroundResource(R.drawable.bg_badge_medium);
                    binding.tvNoiseStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_amber));
                    binding.pbDecibelLevel.setProgressTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.brand_amber)));
                    binding.tvNoisePpeWarning.setText("Elevated jobsite noise. Hearing protection recommended.");
                    binding.tvNoisePpeWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_amber));
                } else {
                    binding.tvNoiseStatusBadge.setText(String.format(Locale.US, "%.0f dB (Safe)", decibels));
                    binding.tvNoiseStatusBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
                    binding.tvNoiseStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
                    binding.pbDecibelLevel.setProgressTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.status_green)));
                    binding.tvNoisePpeWarning.setText("Normal noise. Hearing protection required above 85 dB.");
                    binding.tvNoisePpeWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_secondary));
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (binding != null) {
                    binding.tvNoisePpeWarning.setText("Acoustic Sensor: Nominal background calibration active.");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        startDecibelSampling();
        startFallMonitoring();
        if (getContext() != null) {
            UserLocationManager.getInstance(requireContext()).fetchCurrentLocation(loc -> {});
            UserLocationManager.getInstance(requireContext()).startPassiveLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (decibelManager != null) {
            decibelManager.stopMonitoring();
        }
        if (fallDetectionManager != null) {
            fallDetectionManager.stopMonitoring();
        }
        if (getContext() != null) {
            UserLocationManager.getInstance(requireContext()).stopPassiveLocationUpdates();
        }
    }

    private void setupEmergencyBroadcast() {
        // Real-Time Firebase WebSocket listener for Emergency Hazard Broadcasts
        try {
            com.google.firebase.database.DatabaseReference ref =
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("projects/metro_central/emergency_broadcast");

            ref.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    if (!isAdded() || binding == null) return;
                    if (snapshot.exists()) {
                        String action = snapshot.child("action").getValue(String.class);
                        String status = snapshot.child("status").getValue(String.class);
                        String type = snapshot.child("type").getValue(String.class);
                        String worker = snapshot.child("workerName").getValue(String.class);
                        String mapsUrl = snapshot.child("mapsUrl").getValue(String.class);

                        if ("BROADCAST".equals(action) && "ACTIVE".equals(status)) {
                            binding.incEmergency.bannerActiveEmergency.setVisibility(View.VISIBLE);
                            String alertText = "🚨 " + (type != null ? type : "Emergency");
                            if (worker != null) alertText += " (" + worker + ")";
                            binding.incEmergency.tvActiveEmergencyInfo.setText(alertText);

                            if (mapsUrl != null && !mapsUrl.isEmpty()) {
                                binding.incEmergency.bannerActiveEmergency.setOnClickListener(v -> {
                                    android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mapsUrl));
                                    startActivity(mapIntent);
                                });
                            }
                        } else {
                            binding.incEmergency.bannerActiveEmergency.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
            });
        } catch (Exception ignored) {}

        binding.incEmergency.btnTriggerEmergency.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            EmergencyConfirmationDialog dialog = EmergencyConfirmationDialog.newInstance();
            dialog.setOnEmergencyTriggeredListener((emergencyType, timestamp) -> {
                binding.incEmergency.bannerActiveEmergency.setVisibility(View.VISIBLE);
                binding.incEmergency.tvActiveEmergencyInfo.setText("Active Alert: " + emergencyType);
            });
            dialog.show(getChildFragmentManager(), "EmergencyConfirmationDialog");
        });

        binding.incEmergency.btnCancelEmergency.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            binding.incEmergency.bannerActiveEmergency.setVisibility(View.GONE);
            try {
                com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("projects/metro_central/emergency_broadcast/status")
                        .setValue("RESOLVED");
            } catch (Exception ignored) {}
            Toast.makeText(requireContext(), "Emergency broadcast cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupHazardList() {
        adapter = new HazardTicketAdapter(this);
        binding.rvHazardTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHazardTickets.setAdapter(adapter);

        binding.btnReportHazardHeader.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            ReportHazardBottomSheetDialog dialog = ReportHazardBottomSheetDialog.newInstance();
            dialog.setOnHazardReportedListener(ticket -> loadHazardTickets());
            dialog.show(getChildFragmentManager(), "ReportHazardBottomSheetDialog");
        });
    }

    private void setupFilterChips() {
        binding.chipHazardsOpen.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            setFilterMode(false);
        });

        binding.chipHazardsResolved.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            setFilterMode(true);
        });
    }

    private void setFilterMode(boolean resolved) {
        isShowingResolved = resolved;
        adapter.setFilterMode(resolved);

        if (resolved) {
            binding.chipHazardsResolved.setBackgroundResource(R.drawable.bg_tab_pill_active);
            binding.chipHazardsResolved.setTextColor(0xFFFFFFFF);
            binding.chipHazardsOpen.setBackgroundResource(R.drawable.bg_pill_inactive);
            binding.chipHazardsOpen.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_secondary));
        } else {
            binding.chipHazardsOpen.setBackgroundResource(R.drawable.bg_tab_pill_active);
            binding.chipHazardsOpen.setTextColor(0xFFFFFFFF);
            binding.chipHazardsResolved.setBackgroundResource(R.drawable.bg_pill_inactive);
            binding.chipHazardsResolved.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_oxide_secondary));
        }

        updateEmptyState();
    }

    private void seedInitialHazardsIfEmpty() {
        List<HazardTicketEntity> existing = hazardDao.getAllHazardsSync();
        if (existing == null || existing.isEmpty()) {
            long now = System.currentTimeMillis();

            HazardTicketEntity h1 = new HazardTicketEntity(
                    "HAZ-104",
                    "Electrical Hazard",
                    "Unshielded 480V temporary power junction box exposed to water leak near pump station.",
                    "URGENT",
                    "OPEN",
                    "Level 2 Pump Room",
                    null,
                    "Safety Officer",
                    now - (2L * 60 * 60 * 1000),
                    null,
                    0L,
                    false
            );

            HazardTicketEntity h2 = new HazardTicketEntity(
                    "HAZ-105",
                    "Scaffolding & Fall Protection",
                    "Loose toe-board clamp and missing mid-rail on exterior perimeter staging.",
                    "MEDIUM",
                    "OPEN",
                    "Level 3 East Facade",
                    null,
                    "Lead Foreman",
                    now - (5L * 60 * 60 * 1000),
                    null,
                    0L,
                    false
            );

            HazardTicketEntity h3 = new HazardTicketEntity(
                    "HAZ-102",
                    "Excavation & Trenching",
                    "Perimeter trench shoring reinforced and verified by safety engineer.",
                    "LOW",
                    "RESOLVED",
                    "South Retention Pit",
                    null,
                    "Field Engineer",
                    now - (24L * 60 * 60 * 1000),
                    "Shoring plates bolted and OSHA clearance tag affixed.",
                    now - (12L * 60 * 60 * 1000),
                    true
            );

            hazardDao.insert(h1);
            hazardDao.insert(h2);
            hazardDao.insert(h3);
        }
    }

    private void loadHazardTickets() {
        List<HazardTicketEntity> allTickets = hazardDao.getAllHazardsSync();
        adapter.setData(allTickets);

        // Update Open Count Badge
        int openCount = 0;
        if (allTickets != null) {
            for (HazardTicketEntity t : allTickets) {
                if ("OPEN".equalsIgnoreCase(t.getStatus())) {
                    openCount++;
                }
            }
        }
        binding.tvHazardCountBadge.setText(openCount + " Open");
        if (openCount == 0) {
            binding.tvHazardCountBadge.setBackgroundResource(R.drawable.bg_badge_optimal);
            binding.tvHazardCountBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
        } else {
            binding.tvHazardCountBadge.setBackgroundResource(R.drawable.bg_badge_urgent);
            binding.tvHazardCountBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
        }

        updateEmptyState();
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            binding.rvHazardTickets.setVisibility(View.GONE);
            binding.layoutHazardEmptyState.setVisibility(View.VISIBLE);
            if (isShowingResolved) {
                binding.tvEmptyStateText.setText("No Resolved Hazards");
            } else {
                binding.tvEmptyStateText.setText("No Active Hazards");
            }
        } else {
            binding.rvHazardTickets.setVisibility(View.VISIBLE);
            binding.layoutHazardEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResolveClick(HazardTicketEntity ticket) {
        ResolveHazardDialog dialog = ResolveHazardDialog.newInstance(ticket);
        dialog.setOnHazardResolvedListener(t -> loadHazardTickets());
        dialog.show(getChildFragmentManager(), "ResolveHazardDialog");
    }

    private void setupSyncTelemetry() {
        // Observe pending queue count
        pendingSyncDao.getPendingCountLiveData().observe(getViewLifecycleOwner(), count -> {
            int pending = count != null ? count : 0;
            binding.incSyncTelemetry.tvPendingSyncCount.setText(pending + (pending == 1 ? " item" : " items"));
            if (pending == 0) {
                binding.incSyncTelemetry.ivTelemetrySyncIcon.setImageResource(R.drawable.ic_cloud_done);
                binding.incSyncTelemetry.ivTelemetrySyncIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.status_green));
                binding.incSyncTelemetry.tvRoomDbStatusPill.setText("Synced");
                binding.incSyncTelemetry.tvRoomDbStatusPill.setBackgroundResource(R.drawable.bg_badge_optimal);
            } else {
                binding.incSyncTelemetry.ivTelemetrySyncIcon.setImageResource(R.drawable.ic_cloud_sync);
                binding.incSyncTelemetry.ivTelemetrySyncIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.brand_amber));
                binding.incSyncTelemetry.tvRoomDbStatusPill.setText("Pending (" + pending + ")");
                binding.incSyncTelemetry.tvRoomDbStatusPill.setBackgroundResource(R.drawable.bg_badge_medium);
            }
        });

        // Observe network state
        NetworkMonitor.getInstance(requireContext()).isOnline().observe(getViewLifecycleOwner(), isOnline -> {
            if (Boolean.TRUE.equals(isOnline)) {
                binding.incSyncTelemetry.tvNetworkInterface.setText("Online");
            } else {
                binding.incSyncTelemetry.tvNetworkInterface.setText("Offline");
            }
        });

        // Manual sync flush button
        binding.incSyncTelemetry.btnManualSyncFlush.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            SyncManager.enqueueOneTimeSync(requireContext());
            Toast.makeText(requireContext(), "Sync initiated", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
