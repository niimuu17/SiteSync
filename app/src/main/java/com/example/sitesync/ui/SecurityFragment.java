package com.example.sitesync.ui;

import android.content.Context;
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
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.example.sitesync.utils.NetworkMonitor;

import java.util.List;

/**
 * Tab 4: Security & Site Reliability Subsystem.
 * Hosts:
 * 1. High-Priority Emergency Broadcast (Two-step safeguard with 2.5s hold-to-confirm).
 * 2. Site Hazard & Safety Ticketing Feed (Filterable Open/Resolved, GPS photo geotagging, voice notes).
 * 3. Offline-First Sync Telemetry & Manual Flush (Room DB WAL cache monitoring & WorkManager trigger).
 */
public class SecurityFragment extends Fragment implements HazardTicketAdapter.OnHazardActionListener {

    private FragmentSecurityBinding binding;
    private HazardTicketDao hazardDao;
    private PendingSyncDao pendingSyncDao;
    private HazardTicketAdapter adapter;
    private boolean isShowingResolved = false;

    public static SecurityFragment newInstance() {
        return new SecurityFragment();
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
        setupHazardList();
        setupFilterChips();
        setupSyncTelemetry();
        seedInitialHazardsIfEmpty();
        loadHazardTickets();
    }

    private void setupEmergencyBroadcast() {
        binding.incEmergency.btnTriggerEmergency.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            EmergencyConfirmationDialog dialog = EmergencyConfirmationDialog.newInstance();
            dialog.setOnEmergencyTriggeredListener((emergencyType, timestamp) -> {
                binding.incEmergency.bannerActiveEmergency.setVisibility(View.VISIBLE);
                binding.incEmergency.tvActiveEmergencyInfo.setText("ACTIVE ALERT: " + emergencyType.toUpperCase());
            });
            dialog.show(getChildFragmentManager(), "EmergencyConfirmationDialog");
        });

        binding.incEmergency.btnCancelEmergency.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            binding.incEmergency.bannerActiveEmergency.setVisibility(View.GONE);
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
        binding.tvHazardCountBadge.setText(openCount + " OPEN");
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
                binding.tvEmptyStateText.setText("Zero Resolved Hazards in Archive");
            } else {
                binding.tvEmptyStateText.setText("Zero Active Hazards • Site Perimeter Safe");
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
            binding.incSyncTelemetry.tvPendingSyncCount.setText(pending + " Operations");
            if (pending == 0) {
                binding.incSyncTelemetry.ivTelemetrySyncIcon.setImageResource(R.drawable.ic_cloud_done);
                binding.incSyncTelemetry.ivTelemetrySyncIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.status_green));
                binding.incSyncTelemetry.tvRoomDbStatusPill.setText("HEALTHY");
                binding.incSyncTelemetry.tvRoomDbStatusPill.setBackgroundResource(R.drawable.bg_badge_optimal);
            } else {
                binding.incSyncTelemetry.ivTelemetrySyncIcon.setImageResource(R.drawable.ic_cloud_sync);
                binding.incSyncTelemetry.ivTelemetrySyncIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.brand_amber));
                binding.incSyncTelemetry.tvRoomDbStatusPill.setText("PENDING (" + pending + ")");
                binding.incSyncTelemetry.tvRoomDbStatusPill.setBackgroundResource(R.drawable.bg_badge_medium);
            }
        });

        // Observe network state
        NetworkMonitor.getInstance(requireContext()).isOnline().observe(getViewLifecycleOwner(), isOnline -> {
            if (Boolean.TRUE.equals(isOnline)) {
                binding.incSyncTelemetry.tvNetworkInterface.setText("Wi-Fi / Cellular (Online)");
            } else {
                binding.incSyncTelemetry.tvNetworkInterface.setText("Offline (WAL Local Cache)");
            }
        });

        // Manual sync flush button
        binding.incSyncTelemetry.btnManualSyncFlush.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            SyncManager.enqueueOneTimeSync(requireContext());
            Toast.makeText(requireContext(), "Sync queue dispatched to cloud worker", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
