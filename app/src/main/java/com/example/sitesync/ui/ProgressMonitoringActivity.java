package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.databinding.ActivityProgressMonitoringBinding;
import com.example.sitesync.models.ConstructionActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Site Progress Monitoring screen for the Field Engineer module.
 * Displays construction activities, progress %, status lifecycle, and quick KPI metrics.
 */
public class ProgressMonitoringActivity extends AppCompatActivity implements ConstructionActivityAdapter.OnActivityActionListener {

    public static final String EXTRA_ACTIVITY_ID = "extra_activity_id";

    private ActivityProgressMonitoringBinding binding;
    private ProgressRepository repository;
    private ConstructionActivityAdapter adapter;
    private List<ConstructionActivity> allActivities = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProgressMonitoringBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ProgressRepository(this);

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActivities();
    }

    private void initViews() {
        adapter = new ConstructionActivityAdapter(this);
        binding.rvActivities.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActivities.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBackFromMonitor.setOnClickListener(v -> finish());

        // Status Filter Chips
        binding.chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> applyFilter(checkedId));

        // FAB to add new construction activity
        binding.fabAddActivity.setOnClickListener(v -> {
            Intent intent = new Intent(ProgressMonitoringActivity.this, AddActivityActivity.class);
            startActivity(intent);
        });
    }

    private void loadActivities() {
        allActivities = repository.getAllActivities();
        updateKpiMetrics(allActivities);
        applyFilter(binding.chipGroupFilter.getCheckedChipId());
    }

    private void updateKpiMetrics(List<ConstructionActivity> list) {
        int total = list.size();
        int inProgress = 0;
        int completed = 0;
        int sumPercentage = 0;

        for (ConstructionActivity act : list) {
            sumPercentage += act.getProgressPercentage();
            if (ConstructionActivity.STATUS_COMPLETED.equalsIgnoreCase(act.getStatus())) {
                completed++;
            } else if (ConstructionActivity.STATUS_IN_PROGRESS.equalsIgnoreCase(act.getStatus())) {
                inProgress++;
            }
        }

        int avgProgress = total > 0 ? (sumPercentage / total) : 0;

        binding.tvKpiTotal.setText(String.valueOf(total));
        binding.tvKpiInProgress.setText(String.valueOf(inProgress));
        binding.tvKpiCompleted.setText(String.valueOf(completed));
        binding.tvKpiAvgProgress.setText(avgProgress + "%");
    }

    private void applyFilter(int chipId) {
        List<ConstructionActivity> filtered = new ArrayList<>();

        if (chipId == binding.chipFilterInProgress.getId()) {
            for (ConstructionActivity act : allActivities) {
                if (ConstructionActivity.STATUS_IN_PROGRESS.equalsIgnoreCase(act.getStatus())) {
                    filtered.add(act);
                }
            }
        } else if (chipId == binding.chipFilterPending.getId()) {
            for (ConstructionActivity act : allActivities) {
                if (ConstructionActivity.STATUS_PENDING.equalsIgnoreCase(act.getStatus())) {
                    filtered.add(act);
                }
            }
        } else if (chipId == binding.chipFilterCompleted.getId()) {
            for (ConstructionActivity act : allActivities) {
                if (ConstructionActivity.STATUS_COMPLETED.equalsIgnoreCase(act.getStatus())) {
                    filtered.add(act);
                }
            }
        } else {
            // All Activities
            filtered.addAll(allActivities);
        }

        adapter.setActivities(filtered);
        binding.layoutEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDetailsClicked(ConstructionActivity activity) {
        Intent intent = new Intent(this, ProgressDetailActivity.class);
        intent.putExtra(EXTRA_ACTIVITY_ID, activity.getId());
        startActivity(intent);
    }

    @Override
    public void onUpdateClicked(ConstructionActivity activity) {
        Intent intent = new Intent(this, UpdateProgressActivity.class);
        intent.putExtra(EXTRA_ACTIVITY_ID, activity.getId());
        startActivity(intent);
    }
}
