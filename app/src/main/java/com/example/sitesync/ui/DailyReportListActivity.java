package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.DailyReportRepository;
import com.example.sitesync.data.PdfReportGenerator;
import com.example.sitesync.databinding.ActivityDailyReportListBinding;
import com.example.sitesync.models.DailySiteReport;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Daily Site Report screen for the Engineer Module.
 * Displays previous construction reports, drafts, KPI metrics, search, and quick PDF export.
 */
public class DailyReportListActivity extends AppCompatActivity implements DailySiteReportAdapter.OnReportActionListener {

    public static final String EXTRA_REPORT_ID = "extra_report_id";

    private ActivityDailyReportListBinding binding;
    private DailyReportRepository repository;
    private DailySiteReportAdapter adapter;
    private int currentFilterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDailyReportListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new DailyReportRepository(this);
        currentFilterId = binding.chipAllReports.getId();

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        filterAndSearchReports();
    }

    private void initViews() {
        adapter = new DailySiteReportAdapter(this);
        binding.rvDailyReports.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDailyReports.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBackFromReports.setOnClickListener(v -> finish());

        // Chip Filter Listener
        binding.chipGroupReportFilter.setOnCheckedChangeListener((group, checkedId) -> {
            currentFilterId = checkedId;
            filterAndSearchReports();
        });

        // Search TextWatcher
        binding.etSearchReports.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClearSearchReports.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterAndSearchReports();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearchReports.setOnClickListener(v -> binding.etSearchReports.setText(""));

        // New Daily Report FAB
        binding.fabNewDailyReport.setOnClickListener(v -> {
            Intent intent = new Intent(DailyReportListActivity.this, CreateDailyReportActivity.class);
            startActivity(intent);
        });
    }

    private void filterAndSearchReports() {
        List<DailySiteReport> allReports = repository.getAllReports();

        // Update KPI Counters
        int total = allReports.size();
        int submitted = 0;
        int drafts = 0;

        for (DailySiteReport report : allReports) {
            if (report.isSubmitted()) {
                submitted++;
            } else {
                drafts++;
            }
        }

        binding.tvTotalReportsCount.setText(String.valueOf(total));
        binding.tvSubmittedReportsCount.setText(String.valueOf(submitted));
        binding.tvDraftReportsCount.setText(String.valueOf(drafts));

        // Filter by selected Chip
        List<DailySiteReport> filtered = new ArrayList<>();
        for (DailySiteReport report : allReports) {
            boolean passesFilter = false;
            if (currentFilterId == binding.chipSubmittedReports.getId()) {
                passesFilter = report.isSubmitted();
            } else if (currentFilterId == binding.chipDraftReports.getId()) {
                passesFilter = !report.isSubmitted();
            } else {
                passesFilter = true; // All
            }

            if (passesFilter) {
                filtered.add(report);
            }
        }

        // Apply Search query
        String query = binding.etSearchReports.getText() != null
                ? binding.etSearchReports.getText().toString().trim().toLowerCase() : "";

        List<DailySiteReport> displayList = new ArrayList<>();
        if (query.isEmpty()) {
            displayList.addAll(filtered);
        } else {
            for (DailySiteReport report : filtered) {
                boolean matches = (report.getId() != null && report.getId().toLowerCase().contains(query))
                        || (report.getProjectName() != null && report.getProjectName().toLowerCase().contains(query))
                        || (report.getReportDate() != null && report.getReportDate().toLowerCase().contains(query))
                        || (report.getCompletedWork() != null && report.getCompletedWork().toLowerCase().contains(query))
                        || (report.getManpower() != null && report.getManpower().toLowerCase().contains(query))
                        || (report.getMaterialsUsed() != null && report.getMaterialsUsed().toLowerCase().contains(query))
                        || (report.getEquipment() != null && report.getEquipment().toLowerCase().contains(query))
                        || (report.getStatus() != null && report.getStatus().toLowerCase().contains(query));
                if (matches) {
                    displayList.add(report);
                }
            }
        }

        adapter.setReports(displayList);

        if (displayList.isEmpty()) {
            binding.layoutEmptyReports.setVisibility(View.VISIBLE);
            binding.rvDailyReports.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyReports.setVisibility(View.GONE);
            binding.rvDailyReports.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onReportClick(DailySiteReport report) {
        Intent intent = new Intent(this, DailyReportDetailActivity.class);
        intent.putExtra(EXTRA_REPORT_ID, report.getId());
        startActivity(intent);
    }

    @Override
    public void onSharePdfClick(DailySiteReport report) {
        PdfReportGenerator.shareReportPdf(this, report);
    }
}
