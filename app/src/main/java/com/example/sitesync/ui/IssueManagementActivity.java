package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.IssueRepository;
import com.example.sitesync.databinding.ActivityIssueManagementBinding;
import com.example.sitesync.models.TechnicalIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Technical Issue / RFI Management activity for the Engineer Module.
 * Displays all active site issues, priority levels, status lifecycles, and search/filtering capabilities.
 */
public class IssueManagementActivity extends AppCompatActivity implements TechnicalIssueAdapter.OnIssueClickListener {

    public static final String EXTRA_ISSUE_ID = "extra_issue_id";

    private ActivityIssueManagementBinding binding;
    private IssueRepository repository;
    private TechnicalIssueAdapter adapter;
    private int currentFilterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIssueManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new IssueRepository(this);
        currentFilterId = binding.chipAll.getId();

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        filterAndSearchIssues();
    }

    private void initViews() {
        adapter = new TechnicalIssueAdapter(this);
        binding.rvTechnicalIssues.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTechnicalIssues.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBackFromIssues.setOnClickListener(v -> finish());

        // Chip Filter Listener
        binding.chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            currentFilterId = checkedId;
            filterAndSearchIssues();
        });

        // Search TextWatcher
        binding.etSearchIssues.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterAndSearchIssues();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearchIssues.setText(""));

        // New Issue FAB
        binding.fabNewIssue.setOnClickListener(v -> {
            Intent intent = new Intent(IssueManagementActivity.this, CreateIssueActivity.class);
            startActivity(intent);
        });
    }

    private void filterAndSearchIssues() {
        List<TechnicalIssue> allIssues = repository.getAllIssues();

        // Update KPI Counters
        int total = allIssues.size();
        int highCount = 0;
        int inReviewCount = 0;
        int resolvedCount = 0;

        for (TechnicalIssue issue : allIssues) {
            if (TechnicalIssue.PRIORITY_HIGH.equalsIgnoreCase(issue.getPriority())) {
                highCount++;
            }
            if (TechnicalIssue.STATUS_IN_REVIEW.equalsIgnoreCase(issue.getStatus())) {
                inReviewCount++;
            }
            if (TechnicalIssue.STATUS_RESOLVED.equalsIgnoreCase(issue.getStatus())
                    || TechnicalIssue.STATUS_CLOSED.equalsIgnoreCase(issue.getStatus())) {
                resolvedCount++;
            }
        }

        binding.tvTotalCount.setText(String.valueOf(total));
        binding.tvHighPriorityCount.setText(String.valueOf(highCount));
        binding.tvInReviewCount.setText(String.valueOf(inReviewCount));
        binding.tvResolvedCount.setText(String.valueOf(resolvedCount));

        // Filter by selected Chip
        List<TechnicalIssue> filtered = new ArrayList<>();
        for (TechnicalIssue issue : allIssues) {
            boolean passesFilter = false;
            if (currentFilterId == binding.chipHighPriority.getId()) {
                passesFilter = TechnicalIssue.PRIORITY_HIGH.equalsIgnoreCase(issue.getPriority());
            } else if (currentFilterId == binding.chipOpen.getId()) {
                passesFilter = TechnicalIssue.STATUS_OPEN.equalsIgnoreCase(issue.getStatus())
                        || TechnicalIssue.STATUS_ASSIGNED.equalsIgnoreCase(issue.getStatus());
            } else if (currentFilterId == binding.chipInReview.getId()) {
                passesFilter = TechnicalIssue.STATUS_IN_REVIEW.equalsIgnoreCase(issue.getStatus());
            } else if (currentFilterId == binding.chipResolved.getId()) {
                passesFilter = TechnicalIssue.STATUS_RESOLVED.equalsIgnoreCase(issue.getStatus())
                        || TechnicalIssue.STATUS_CLOSED.equalsIgnoreCase(issue.getStatus());
            } else {
                passesFilter = true; // All
            }

            if (passesFilter) {
                filtered.add(issue);
            }
        }

        // Apply Search query if present
        String query = binding.etSearchIssues.getText() != null
                ? binding.etSearchIssues.getText().toString().trim().toLowerCase() : "";

        List<TechnicalIssue> displayList = new ArrayList<>();
        if (query.isEmpty()) {
            displayList.addAll(filtered);
        } else {
            for (TechnicalIssue issue : filtered) {
                boolean matches = (issue.getId() != null && issue.getId().toLowerCase().contains(query))
                        || (issue.getTitle() != null && issue.getTitle().toLowerCase().contains(query))
                        || (issue.getProjectName() != null && issue.getProjectName().toLowerCase().contains(query))
                        || (issue.getSiteLocation() != null && issue.getSiteLocation().toLowerCase().contains(query))
                        || (issue.getAssignedPerson() != null && issue.getAssignedPerson().toLowerCase().contains(query))
                        || (issue.getPriority() != null && issue.getPriority().toLowerCase().contains(query))
                        || (issue.getStatus() != null && issue.getStatus().toLowerCase().contains(query));
                if (matches) {
                    displayList.add(issue);
                }
            }
        }

        adapter.setIssues(displayList);

        if (displayList.isEmpty()) {
            binding.layoutEmptyIssues.setVisibility(View.VISIBLE);
            binding.rvTechnicalIssues.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyIssues.setVisibility(View.GONE);
            binding.rvTechnicalIssues.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onIssueClick(TechnicalIssue issue) {
        Intent intent = new Intent(this, IssueDetailActivity.class);
        intent.putExtra(EXTRA_ISSUE_ID, issue.getId());
        startActivity(intent);
    }
}
