package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.InspectionRepository;
import com.example.sitesync.databinding.ActivityInspectionListBinding;
import com.example.sitesync.models.SiteInspection;

import java.util.ArrayList;
import java.util.List;

/**
 * Site Quality Control and Inspection list screen for the Engineer Module.
 * Displays inspections, pass/fail KPI telemetry, status filters, and provides re-inspection entry.
 */
public class InspectionListActivity extends AppCompatActivity implements SiteInspectionAdapter.OnInspectionActionListener {

    public static final String EXTRA_INSPECTION_ID = "extra_inspection_id";

    private ActivityInspectionListBinding binding;
    private InspectionRepository repository;
    private SiteInspectionAdapter adapter;
    private List<SiteInspection> allInspections = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInspectionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new InspectionRepository(this);

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInspections();
    }

    private void initViews() {
        adapter = new SiteInspectionAdapter(this);
        binding.rvInspections.setLayoutManager(new LinearLayoutManager(this));
        binding.rvInspections.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBackFromInspectionList.setOnClickListener(v -> finish());

        // Status Filter Chips
        binding.chipGroupInspFilter.setOnCheckedChangeListener((group, checkedId) -> applyFilter(checkedId));

        // FAB to create new inspection
        binding.fabCreateInspection.setOnClickListener(v -> {
            Intent intent = new Intent(InspectionListActivity.this, CreateInspectionActivity.class);
            startActivity(intent);
        });
    }

    private void loadInspections() {
        allInspections = repository.getAllInspections();
        updateKpiMetrics(allInspections);
        applyFilter(binding.chipGroupInspFilter.getCheckedChipId());
    }

    private void updateKpiMetrics(List<SiteInspection> list) {
        int total = list.size();
        int passed = 0;
        int failed = 0;

        for (SiteInspection item : list) {
            if (SiteInspection.STATUS_PASSED.equalsIgnoreCase(item.getStatus())) {
                passed++;
            } else if (SiteInspection.STATUS_FAILED.equalsIgnoreCase(item.getStatus())) {
                failed++;
            }
        }

        int evaluated = passed + failed;
        int passRate = evaluated > 0 ? ((passed * 100) / evaluated) : 0;

        binding.tvKpiTotalInspections.setText(String.valueOf(total));
        binding.tvKpiPassedInspections.setText(String.valueOf(passed));
        binding.tvKpiFailedInspections.setText(String.valueOf(failed));
        binding.tvKpiPassRate.setText(passRate + "%");
    }

    private void applyFilter(int chipId) {
        List<SiteInspection> filtered = new ArrayList<>();

        if (chipId == binding.chipFilterPassedInsp.getId()) {
            for (SiteInspection item : allInspections) {
                if (SiteInspection.STATUS_PASSED.equalsIgnoreCase(item.getStatus())) {
                    filtered.add(item);
                }
            }
        } else if (chipId == binding.chipFilterFailedInsp.getId()) {
            for (SiteInspection item : allInspections) {
                if (SiteInspection.STATUS_FAILED.equalsIgnoreCase(item.getStatus())) {
                    filtered.add(item);
                }
            }
        } else if (chipId == binding.chipFilterPendingInsp.getId()) {
            for (SiteInspection item : allInspections) {
                if (SiteInspection.STATUS_PENDING.equalsIgnoreCase(item.getStatus())) {
                    filtered.add(item);
                }
            }
        } else {
            filtered.addAll(allInspections);
        }

        adapter.setInspections(filtered);
        binding.layoutEmptyInspections.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDetailsClicked(SiteInspection inspection) {
        Intent intent = new Intent(this, InspectionDetailActivity.class);
        intent.putExtra(EXTRA_INSPECTION_ID, inspection.getId());
        startActivity(intent);
    }

    @Override
    public void onReInspectClicked(SiteInspection inspection) {
        Intent intent = new Intent(this, ReInspectionActivity.class);
        intent.putExtra(EXTRA_INSPECTION_ID, inspection.getId());
        startActivity(intent);
    }
}
